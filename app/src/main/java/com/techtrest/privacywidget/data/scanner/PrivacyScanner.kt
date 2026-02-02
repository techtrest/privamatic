package com.techtrest.privacywidget.data.scanner

import android.content.Context
import com.techtrest.privacywidget.data.maintenance.MaintenanceManager
import com.techtrest.privacywidget.data.model.PrivacyIssue
import com.techtrest.privacywidget.data.model.PrivacyScore
import com.techtrest.privacywidget.data.scanner.checks.DefaultAppsChecker
import com.techtrest.privacywidget.data.scanner.checks.DeveloperSettingsChecker
import com.techtrest.privacywidget.data.scanner.checks.GooglePlayChecker
import com.techtrest.privacywidget.data.scanner.checks.GoogleServicesChecker
import com.techtrest.privacywidget.data.scanner.checks.InstalledAppsChecker
import com.techtrest.privacywidget.data.scanner.checks.NetworkSecurityChecker
import com.techtrest.privacywidget.data.scanner.checks.SecuritySettingsChecker
import com.techtrest.privacywidget.data.scanner.checks.SystemServicesChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class PrivacyScanner(private val context: Context) {

    private val securityChecker = SecuritySettingsChecker(context)
    private val systemServicesChecker = SystemServicesChecker(context)
    private val developerChecker = DeveloperSettingsChecker(context)
    private val networkChecker = NetworkSecurityChecker(context)
    private val playChecker = GooglePlayChecker(context)
    private val googleServicesChecker = GoogleServicesChecker(context)
    private val defaultAppsChecker = DefaultAppsChecker(context)
    private val installedAppsChecker = InstalledAppsChecker(context)
    private val maintenanceManager = MaintenanceManager(context)

    suspend fun performCompleteScan(): PrivacyScore = withContext(Dispatchers.IO) {
        val issues = mutableListOf<PrivacyIssue>()

        // ===== SYSTEM SECURITY CHECKS =====
        issues.add(securityChecker.checkScreenLock())
        issues.add(systemServicesChecker.checkDeviceEncryption())
        issues.add(securityChecker.checkBiometricAuth())
        issues.add(developerChecker.checkUsbDebugging())
        issues.add(developerChecker.checkDeveloperOptions())
        issues.add(systemServicesChecker.checkNotificationListeners())
        issues.add(systemServicesChecker.checkAccessibilityServices())
        issues.add(systemServicesChecker.checkDeviceAdminApps())

        // ===== NETWORK & TRACKING PRIVACY CHECKS =====
        issues.add(networkChecker.checkVpnConnection())
        issues.add(networkChecker.checkAlwaysOnVpn())
        issues.add(networkChecker.checkPrivateDns())
        issues.add(networkChecker.checkAdvertisingId())
        issues.add(playChecker.checkWifiScanning())

        // ===== GOOGLE SERVICES CHECKS =====
        issues.add(googleServicesChecker.checkFindMyDevice())

        // ===== DEFAULT APPS CHECKS =====
        issues.add(defaultAppsChecker.checkDefaultBrowser())
        issues.add(defaultAppsChecker.checkDefaultKeyboard())
        issues.add(defaultAppsChecker.checkDefaultSms())
        issues.add(defaultAppsChecker.checkDefaultEmail())
        issues.add(defaultAppsChecker.checkDefaultLauncher())

        // ===== GOOGLE APPS CHECKS (MAJOR) =====
        issues.add(installedAppsChecker.checkGoogleChrome())
        issues.add(installedAppsChecker.checkGmailInstalled())
        issues.add(installedAppsChecker.checkGoogleMapsInstalled())
        issues.add(installedAppsChecker.checkGooglePhotosInstalled())
        issues.add(installedAppsChecker.checkGoogleDriveInstalled())

        // ===== GOOGLE APPS CHECKS (MINOR) =====
        issues.add(installedAppsChecker.checkYouTubeInstalled())
        issues.add(installedAppsChecker.checkGoogleCalendarInstalled())
        issues.add(installedAppsChecker.checkGoogleKeepInstalled())
        issues.add(installedAppsChecker.checkGoogleCameraInstalled())
        issues.add(installedAppsChecker.checkGoogleDocsInstalled())

        // ===== META/FACEBOOK APPS CHECKS =====
        issues.add(installedAppsChecker.checkFacebookInstalled())
        issues.add(installedAppsChecker.checkInstagramInstalled())
        issues.add(installedAppsChecker.checkWhatsAppInstalled())
        issues.add(installedAppsChecker.checkMessengerInstalled())

        // ===== MICROSOFT APPS CHECKS =====
        issues.add(installedAppsChecker.checkEdgeInstalled())
        issues.add(installedAppsChecker.checkOutlookInstalled())
        issues.add(installedAppsChecker.checkOneDriveInstalled())

        // ===== AMAZON APPS CHECKS =====
        issues.add(installedAppsChecker.checkAmazonShoppingInstalled())
        issues.add(installedAppsChecker.checkPrimeVideoInstalled())

        // ===== AI/LLM APPS CHECKS =====
        issues.add(installedAppsChecker.checkChatGPTInstalled())
        issues.add(installedAppsChecker.checkGoogleGeminiInstalled())
        issues.add(installedAppsChecker.checkMicrosoftCopilotInstalled())
        issues.add(installedAppsChecker.checkClaudeInstalled())
        issues.add(installedAppsChecker.checkPerplexityInstalled())
        issues.add(installedAppsChecker.checkMetaAIInstalled())

        // ===== SOCIAL MEDIA APPS CHECKS =====
        issues.add(installedAppsChecker.checkTikTokInstalled())
        issues.add(installedAppsChecker.checkTwitterInstalled())
        issues.add(installedAppsChecker.checkRedditInstalled())

        // Get manual check points
        val manualCheckPoints = maintenanceManager.getTotalPoints().first()

        // Calculate final score
        PrivacyScoreCalculator.calculateScore(issues, manualCheckPoints)
    }
}
