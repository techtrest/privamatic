package com.techtrest.privacywidget.data

import com.techtrest.privacywidget.data.model.PrivacyCheck
import com.techtrest.privacywidget.data.model.PrivacyScore
import com.techtrest.privacywidget.data.model.QuickWin
import com.techtrest.privacywidget.data.model.QuickWinType

/**
 * Detects available quick wins based on the current privacy score.
 * Only returns quick wins for improvements that are NOT already completed.
 */
object QuickWinsDetector {

    /**
     * Analyzes the privacy score and returns a list of available quick wins.
     * Only includes wins where the related check is currently insecure.
     */
    fun detectQuickWins(privacyScore: PrivacyScore): List<QuickWin> {
        val quickWins = mutableListOf<QuickWin>()

        // Check each potential quick win
        checkWifiScanning(privacyScore)?.let { quickWins.add(it) }
        checkAdvertisingId(privacyScore)?.let { quickWins.add(it) }
        checkPrivateDns(privacyScore)?.let { quickWins.add(it) }
        checkFindMyDevice(privacyScore)?.let { quickWins.add(it) }
        checkDefaultBrowser(privacyScore)?.let { quickWins.add(it) }
        checkDefaultKeyboard(privacyScore)?.let { quickWins.add(it) }

        return quickWins
    }

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

    private fun checkFindMyDevice(privacyScore: PrivacyScore): QuickWin? {
        val issue = privacyScore.issues.find { it.check == PrivacyCheck.FIND_MY_DEVICE }
        return if (issue != null && !issue.isSecure) {
            QuickWin(
                type = QuickWinType.DISABLE_FIND_MY_DEVICE,
                relatedCheck = PrivacyCheck.FIND_MY_DEVICE
            )
        } else null
    }

    private fun checkDefaultBrowser(privacyScore: PrivacyScore): QuickWin? {
        val issue = privacyScore.issues.find { it.check == PrivacyCheck.DEFAULT_BROWSER }
        return if (issue != null && !issue.isSecure) {
            // Extract browser name from currentStatus (e.g., "Using Chrome" -> "Chrome")
            val browserName = extractAppName(issue.currentStatus)
            QuickWin(
                type = QuickWinType.REPLACE_BROWSER,
                relatedCheck = PrivacyCheck.DEFAULT_BROWSER,
                currentAppName = browserName
            )
        } else null
    }

    private fun checkDefaultKeyboard(privacyScore: PrivacyScore): QuickWin? {
        val issue = privacyScore.issues.find { it.check == PrivacyCheck.DEFAULT_KEYBOARD }
        return if (issue != null && !issue.isSecure) {
            // Extract keyboard name from currentStatus (e.g., "Using SwiftKey" -> "SwiftKey")
            val keyboardName = extractAppName(issue.currentStatus)
            QuickWin(
                type = QuickWinType.REPLACE_KEYBOARD,
                relatedCheck = PrivacyCheck.DEFAULT_KEYBOARD,
                currentAppName = keyboardName
            )
        } else null
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
