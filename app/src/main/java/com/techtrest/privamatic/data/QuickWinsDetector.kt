package com.techtrest.privamatic.data

import android.content.Context
import com.techtrest.privamatic.data.model.ActionType
import com.techtrest.privamatic.data.model.PrivacyCheck
import com.techtrest.privamatic.data.model.PrivacyScore
import com.techtrest.privamatic.data.model.QuickWin
import com.techtrest.privamatic.data.model.QuickWinType

/**
 * Detects available quick wins based on the current privacy score and manual check states.
 * Only returns quick wins for improvements that are NOT already completed.
 */
object QuickWinsDetector {

    /**
     * Installed app checks eligible for Quick Wins: have a package name,
     * use OPEN_APP_SETTINGS action, and have a non-zero point deduction.
     */
    private val installedAppChecks = PrivacyCheck.entries.filter {
        it.actionType == ActionType.OPEN_APP_SETTINGS && it.pointDeduction > 0
    }

    /**
     * Analyzes the privacy score and returns a list of available quick wins,
     * sorted by impact (highest first).
     * Only includes wins where the related check is currently insecure.
     */
    fun detectQuickWins(privacyScore: PrivacyScore, context: Context): List<QuickWin> {
        val quickWins = mutableListOf<QuickWin>()

        // System service revocations (high impact)
        checkNotificationListeners(privacyScore)?.let { quickWins.add(it) }
        checkAccessibilityServices(privacyScore)?.let { quickWins.add(it) }
        checkDeviceAdmins(privacyScore)?.let { quickWins.add(it) }

        // Settings toggles
        checkWifiScanning(privacyScore)?.let { quickWins.add(it) }
        checkAdvertisingId(privacyScore)?.let { quickWins.add(it) }
        checkPrivateDns(privacyScore)?.let { quickWins.add(it) }
        checkDeveloperOptions(privacyScore)?.let { quickWins.add(it) }

        // Default app replacements
        checkDefaultBrowser(privacyScore)?.let { quickWins.add(it) }
        checkDefaultKeyboard(privacyScore)?.let { quickWins.add(it) }
        checkDefaultSms(privacyScore)?.let { quickWins.add(it) }
        checkDefaultEmail(privacyScore)?.let { quickWins.add(it) }
        checkDefaultLauncher(privacyScore)?.let { quickWins.add(it) }

        // Installed app uninstalls
        quickWins.addAll(checkInstalledApps(privacyScore, context))

        return quickWins.sortedByDescending { it.impact }
    }

    // ===== SYSTEM SERVICE REVOCATIONS =====

    private fun checkNotificationListeners(privacyScore: PrivacyScore): QuickWin? {
        val issue = privacyScore.issues.find { it.check == PrivacyCheck.NOTIFICATION_LISTENER }
        return if (issue != null && !issue.isSecure) {
            QuickWin(
                type = QuickWinType.REVOKE_NOTIFICATION_LISTENERS,
                relatedCheck = PrivacyCheck.NOTIFICATION_LISTENER
            )
        } else null
    }

    private fun checkAccessibilityServices(privacyScore: PrivacyScore): QuickWin? {
        val issue = privacyScore.issues.find { it.check == PrivacyCheck.ACCESSIBILITY_SERVICE }
        return if (issue != null && !issue.isSecure) {
            QuickWin(
                type = QuickWinType.REVOKE_ACCESSIBILITY_SERVICES,
                relatedCheck = PrivacyCheck.ACCESSIBILITY_SERVICE
            )
        } else null
    }

    private fun checkDeviceAdmins(privacyScore: PrivacyScore): QuickWin? {
        val issue = privacyScore.issues.find { it.check == PrivacyCheck.DEVICE_ADMIN }
        return if (issue != null && !issue.isSecure) {
            QuickWin(
                type = QuickWinType.REVOKE_DEVICE_ADMINS,
                relatedCheck = PrivacyCheck.DEVICE_ADMIN
            )
        } else null
    }

    // ===== SETTINGS TOGGLES =====

    private fun checkWifiScanning(privacyScore: PrivacyScore): QuickWin? {
        val issue = privacyScore.issues.find { it.check == PrivacyCheck.WIFI_SCANNING }
        return if (issue != null && !issue.isSecure) {
            QuickWin(
                type = QuickWinType.DISABLE_WIFI_SCANNING,
                relatedCheck = PrivacyCheck.WIFI_SCANNING
            )
        } else null
    }

    private fun checkAdvertisingId(privacyScore: PrivacyScore): QuickWin? {
        val issue = privacyScore.issues.find { it.check == PrivacyCheck.ADVERTISING_ID }
        return if (issue != null && !issue.isSecure) {
            QuickWin(
                type = QuickWinType.DISABLE_ADVERTISING_ID,
                relatedCheck = PrivacyCheck.ADVERTISING_ID
            )
        } else null
    }

    private fun checkPrivateDns(privacyScore: PrivacyScore): QuickWin? {
        val issue = privacyScore.issues.find { it.check == PrivacyCheck.PRIVATE_DNS }
        return if (issue != null && !issue.isSecure) {
            QuickWin(
                type = QuickWinType.ENABLE_PRIVATE_DNS,
                relatedCheck = PrivacyCheck.PRIVATE_DNS
            )
        } else null
    }

    private fun checkDeveloperOptions(privacyScore: PrivacyScore): QuickWin? {
        val issue = privacyScore.issues.find { it.check == PrivacyCheck.DEVELOPER_OPTIONS }
        return if (issue != null && !issue.isSecure) {
            QuickWin(
                type = QuickWinType.DISABLE_DEVELOPER_OPTIONS,
                relatedCheck = PrivacyCheck.DEVELOPER_OPTIONS
            )
        } else null
    }

    // ===== DEFAULT APP REPLACEMENTS =====

    private fun checkDefaultBrowser(privacyScore: PrivacyScore): QuickWin? {
        val issue = privacyScore.issues.find { it.check == PrivacyCheck.DEFAULT_BROWSER }
        return if (issue != null && !issue.isSecure) {
            QuickWin(
                type = QuickWinType.REPLACE_BROWSER,
                relatedCheck = PrivacyCheck.DEFAULT_BROWSER,
                currentAppName = extractAppName(issue.currentStatus)
            )
        } else null
    }

    private fun checkDefaultKeyboard(privacyScore: PrivacyScore): QuickWin? {
        val issue = privacyScore.issues.find { it.check == PrivacyCheck.DEFAULT_KEYBOARD }
        return if (issue != null && !issue.isSecure) {
            QuickWin(
                type = QuickWinType.REPLACE_KEYBOARD,
                relatedCheck = PrivacyCheck.DEFAULT_KEYBOARD,
                currentAppName = extractAppName(issue.currentStatus)
            )
        } else null
    }

    private fun checkDefaultSms(privacyScore: PrivacyScore): QuickWin? {
        val issue = privacyScore.issues.find { it.check == PrivacyCheck.DEFAULT_SMS }
        return if (issue != null && !issue.isSecure) {
            QuickWin(
                type = QuickWinType.REPLACE_DEFAULT_SMS,
                relatedCheck = PrivacyCheck.DEFAULT_SMS,
                currentAppName = extractAppName(issue.currentStatus)
            )
        } else null
    }

    private fun checkDefaultEmail(privacyScore: PrivacyScore): QuickWin? {
        val issue = privacyScore.issues.find { it.check == PrivacyCheck.DEFAULT_EMAIL }
        return if (issue != null && !issue.isSecure) {
            QuickWin(
                type = QuickWinType.REPLACE_DEFAULT_EMAIL,
                relatedCheck = PrivacyCheck.DEFAULT_EMAIL,
                currentAppName = extractAppName(issue.currentStatus)
            )
        } else null
    }

    private fun checkDefaultLauncher(privacyScore: PrivacyScore): QuickWin? {
        val issue = privacyScore.issues.find { it.check == PrivacyCheck.DEFAULT_LAUNCHER }
        return if (issue != null && !issue.isSecure) {
            QuickWin(
                type = QuickWinType.REPLACE_DEFAULT_LAUNCHER,
                relatedCheck = PrivacyCheck.DEFAULT_LAUNCHER,
                currentAppName = extractAppName(issue.currentStatus)
            )
        } else null
    }

    // ===== INSTALLED APP UNINSTALLS =====

    private fun checkInstalledApps(privacyScore: PrivacyScore, context: Context): List<QuickWin> {
        return installedAppChecks.mapNotNull { check ->
            val issue = privacyScore.issues.find { it.check == check }
            if (issue != null && !issue.isSecure && !issue.isSystemApp) {
                QuickWin(
                    type = QuickWinType.UNINSTALL_APP,
                    relatedCheck = check,
                    currentAppName = context.getString(check.displayName)
                )
            } else null
        }
    }

    /**
     * Extract app name from status string
     * e.g., "Using SwiftKey" -> "SwiftKey"
     * e.g., "Using Chrome" -> "Chrome"
     */
    private fun extractAppName(status: String): String? {
        return status.removePrefix("Using ").takeIf { it != status }
    }
}
