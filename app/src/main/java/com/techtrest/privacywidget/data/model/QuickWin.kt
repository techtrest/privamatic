package com.techtrest.privacywidget.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.AdUnits
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.ui.graphics.vector.ImageVector

enum class QuickWinType(
    val title: String,
    val description: String,
    val timeEstimate: String,
    val icon: ImageVector,
    val instructions: String,
    val actionType: ActionType? = null,
    val actionLabel: String? = null
) {
    // ===== SYSTEM SERVICE REVOCATIONS =====

    REVOKE_NOTIFICATION_LISTENERS(
        title = "Revoke Notification Listeners",
        description = "Non-system apps can read ALL your notifications",
        timeEstimate = "1 minute",
        icon = Icons.Default.NotificationsOff,
        actionType = ActionType.NOTIFICATION_LISTENER,
        actionLabel = "Open Notification Access",
        instructions = """
            Apps with notification listener access can read ALL your notifications, including 2FA codes and private messages.

            Steps:
            1. Open Settings app
            2. Go to 'Apps' → 'Special app access'
            3. Tap 'Notification access'
            4. Review the list and disable access for non-essential apps

            After completing this, rescan to see your improved score!
        """.trimIndent()
    ),

    REVOKE_ACCESSIBILITY_SERVICES(
        title = "Revoke Accessibility Services",
        description = "Non-essential apps can control your entire device",
        timeEstimate = "1 minute",
        icon = Icons.Default.Accessibility,
        actionType = ActionType.ACCESSIBILITY_SETTINGS,
        actionLabel = "Open Accessibility Settings",
        instructions = """
            Apps with accessibility service access can control your entire device and potentially log keystrokes.

            Steps:
            1. Open Settings app
            2. Go to 'Accessibility'
            3. Review 'Downloaded apps' or 'Installed services'
            4. Disable any services you don't actively need

            Note: Keep accessibility services that you genuinely use (password managers, screen readers).

            After completing this, rescan to see your improved score!
        """.trimIndent()
    ),

    REVOKE_DEVICE_ADMINS(
        title = "Revoke Device Administrators",
        description = "Non-system apps have elevated device control",
        timeEstimate = "1 minute",
        icon = Icons.Default.Security,
        actionType = ActionType.DEVICE_ADMIN_SETTINGS,
        actionLabel = "Open Security Settings",
        instructions = """
            Apps with device administrator privileges have elevated control over your device.

            Steps:
            1. Open Settings app
            2. Go to 'Security' → 'Device admin apps'
            3. Review the list of device administrators
            4. Deactivate any that you don't recognize or need

            After completing this, rescan to see your improved score!
        """.trimIndent()
    ),

    // ===== SETTINGS TOGGLES =====

    DISABLE_WIFI_SCANNING(
        title = "Disable Background Wi-Fi Scanning",
        description = "Prevents location tracking via Wi-Fi networks",
        timeEstimate = "30 seconds",
        icon = Icons.Default.LocationOff,
        actionType = ActionType.LOCATION_SETTINGS,
        actionLabel = "Open Location Settings",
        instructions = """
            Background Wi-Fi scanning allows apps to track your location even when Wi-Fi is off.

            Steps:
            1. Open Settings app
            2. Go to 'Location'
            3. Tap 'Location services' or 'Wi-Fi and Bluetooth scanning'
            4. Disable 'Wi-Fi scanning'

            After completing this, rescan to see your improved score!
        """.trimIndent()
    ),

    DISABLE_ADVERTISING_ID(
        title = "Disable Advertising ID",
        description = "Prevents cross-app tracking by advertisers",
        timeEstimate = "30 seconds",
        icon = Icons.Default.AdUnits,
        actionType = ActionType.PRIVACY_SETTINGS,
        actionLabel = "Open Privacy Settings",
        instructions = """
            This prevents apps from building an advertising profile and tracking you across different apps.

            Steps:
            1. Open Settings app
            2. Scroll to 'Privacy' or 'Google'
            3. Tap 'Ads'
            4. Tap 'Delete Advertising ID' or 'Opt out of Ads Personalization'
            5. Confirm

            After completing this, rescan to see your improved score!
        """.trimIndent()
    ),

    ENABLE_PRIVATE_DNS(
        title = "Enable Private DNS",
        description = "Blocks tracking and malicious domains at network level",
        timeEstimate = "1 minute",
        icon = Icons.Default.Language,
        actionType = ActionType.NETWORK_SETTINGS,
        actionLabel = "Open Network Settings",
        instructions = """
            Private DNS blocks tracking and malicious domains at the network level for all apps.

            Recommended providers:
            • Quad9 (dns.quad9.net)
              Privacy-focused, blocks malware, no logging

            • AdGuard DNS (dns.adguard-dns.com)
              Blocks ads and trackers

            Steps:
            1. Open Settings app
            2. Go to 'Network & Internet'
            3. Tap 'Private DNS'
            4. Select 'Private DNS provider hostname'
            5. Enter: dns.quad9.net
            6. Tap 'Save'

            After completing this, rescan to see your improved score!
        """.trimIndent()
    ),

    DISABLE_FIND_MY_DEVICE(
        title = "Disable Find My Device",
        description = "Reduces Google's device tracking",
        timeEstimate = "30 seconds",
        icon = Icons.Default.PhoneAndroid,
        actionType = ActionType.SECURITY_SETTINGS,
        actionLabel = "Open Security Settings",
        instructions = """
            While useful for theft protection, Find My Device constantly tracks your location.

            Steps:
            1. Open Settings app
            2. Go to 'Security'
            3. Tap 'Find My Device'
            4. Turn off 'Find My Device'
            5. Confirm

            Note: This trades security for privacy. Only disable if privacy is your priority.

            After completing this, rescan to see your improved score!
        """.trimIndent()
    ),

    // ===== DEFAULT APP REPLACEMENTS =====

    REPLACE_BROWSER(
        title = "Replace Browser",
        description = "Switch to privacy-focused browser",
        timeEstimate = "2 minutes",
        icon = Icons.Default.Explore,
        actionType = ActionType.DEFAULT_APPS_SETTINGS,
        actionLabel = "Open Default Apps",
        instructions = """
            Privacy-focused browsers block trackers and don't sync your browsing history to cloud servers.

            Recommended alternatives:
            • Brave (Play Store) - Blocks trackers by default
            • Firefox (F-Droid and Play Store) - Privacy-focused, customizable

            Or explore more options:
            → Privacy Guides: privacyguides.org/browsers

            Steps:
            1. Open F-Droid or Play Store
            2. Search 'Brave' or 'Firefox'
            3. Install browser
            4. Open the new browser
            5. When prompted, set as default browser
               (Or: Settings → Apps → Default apps → Browser)

            After completing this, rescan to see your improved score!
        """.trimIndent()
    ),

    REPLACE_KEYBOARD(
        title = "Replace Keyboard",
        description = "Switch to privacy-focused keyboard",
        timeEstimate = "2 minutes",
        icon = Icons.Default.Keyboard,
        actionType = ActionType.DEFAULT_APPS_SETTINGS,
        actionLabel = "Open Default Apps",
        instructions = """
            Your current keyboard may send your typing data to cloud servers for processing.

            Recommended alternatives:
            • FUTO Keyboard (Play Store)
              Best privacy + features, offline voice typing
            • AnySoftKeyboard (F-Droid & Play Store)
              Mature, highly customizable

            Or explore more options:
            → F-Droid: Browse privacy keyboards
            → Privacy Guides: privacyguides.org/android

            Steps:
            1. Open F-Droid or Play Store
            2. Search 'privacy keyboard'
            3. Install keyboard
            4. Open Settings → System → Languages & Input
            5. Tap 'On-screen keyboard' or 'Virtual keyboard'
            6. Select your new keyboard as default

            After completing this, rescan to see your improved score!
        """.trimIndent()
    ),

    REPLACE_DEFAULT_SMS(
        title = "Replace SMS App",
        description = "Switch to privacy-focused messaging app",
        timeEstimate = "2 minutes",
        icon = Icons.Default.Sms,
        actionType = ActionType.DEFAULT_APPS_SETTINGS,
        actionLabel = "Open Default Apps",
        instructions = """
            Your current SMS app may collect your messaging data and contacts.

            Recommended alternatives:
            • Fossify Messages (F-Droid & Play Store)
              Open source, privacy-focused, no ads
            • QKSMS (F-Droid)
              Lightweight and private

            Steps:
            1. Install a privacy-focused messaging app
            2. Open Settings → Apps → Default apps
            3. Tap 'SMS app'
            4. Select your new messaging app

            After completing this, rescan to see your improved score!
        """.trimIndent()
    ),

    REPLACE_DEFAULT_EMAIL(
        title = "Replace Email App",
        description = "Switch to privacy-focused email client",
        timeEstimate = "2 minutes",
        icon = Icons.Default.Email,
        actionType = ActionType.DEFAULT_APPS_SETTINGS,
        actionLabel = "Open Default Apps",
        instructions = """
            Your current email app may scan your emails for advertising and data collection.

            Recommended alternatives:
            • K-9 Mail (F-Droid & Play Store) - Open source
            • FairEmail (F-Droid & Play Store) - Privacy-focused
            • ProtonMail (Play Store) - Encrypted email

            Steps:
            1. Install a privacy-focused email app
            2. Open Settings → Apps → Default apps
            3. Tap 'Email app' (if available)
            4. Select your new email app

            After completing this, rescan to see your improved score!
        """.trimIndent()
    ),

    REPLACE_DEFAULT_LAUNCHER(
        title = "Replace Launcher",
        description = "Switch to privacy-focused launcher",
        timeEstimate = "2 minutes",
        icon = Icons.Default.Home,
        actionType = ActionType.DEFAULT_APPS_SETTINGS,
        actionLabel = "Open Default Apps",
        instructions = """
            Your current launcher may collect usage patterns, app habits, and behavioral data.

            Recommended alternatives:
            • Lawnchair (F-Droid & Play Store) - Modern and customizable
            • KISS Launcher (F-Droid & Play Store) - Lightweight
            • Neo Launcher (F-Droid) - Feature-rich

            Steps:
            1. Install a privacy-focused launcher
            2. Open Settings → Apps → Default apps
            3. Tap 'Home app'
            4. Select your new launcher

            After completing this, rescan to see your improved score!
        """.trimIndent()
    ),

    // ===== APP UNINSTALLS =====

    UNINSTALL_APP(
        title = "Uninstall App",
        description = "Remove a privacy-invasive app from your device",
        timeEstimate = "30 seconds",
        icon = Icons.Default.Delete,
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = "Open App Info",
        instructions = """
            You can improve your privacy by uninstalling or disabling this app.

            Steps:
            1. Tap 'Open App Settings' below
            2. Tap 'Uninstall' or 'Disable'
            3. Confirm

            If the app came pre-installed, you may only be able to disable it rather than fully uninstall it.

            After completing this, rescan to see your improved score!
        """.trimIndent()
    )
}

data class QuickWin(
    val type: QuickWinType,
    val relatedCheck: PrivacyCheck?,
    val currentAppName: String? = null
) {
    /**
     * Get dynamic title based on current app name
     * e.g., "Replace SwiftKey" instead of generic "Replace Keyboard"
     * e.g., "Uninstall Chrome" instead of generic "Uninstall App"
     */
    val displayTitle: String
        get() = when {
            currentAppName != null -> when (type) {
                QuickWinType.UNINSTALL_APP -> "Uninstall $currentAppName"
                QuickWinType.REPLACE_BROWSER,
                QuickWinType.REPLACE_KEYBOARD,
                QuickWinType.REPLACE_DEFAULT_SMS,
                QuickWinType.REPLACE_DEFAULT_EMAIL,
                QuickWinType.REPLACE_DEFAULT_LAUNCHER -> "Replace $currentAppName"
                else -> type.title
            }
            else -> type.title
        }

    /**
     * Get impact (point value) from the related PrivacyCheck
     * This ensures synchronization across all displays - the PrivacyCheck enum is the single source of truth
     */
    val impact: Int
        get() = relatedCheck?.pointDeduction ?: 0
}
