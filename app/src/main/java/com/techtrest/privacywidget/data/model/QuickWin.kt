package com.techtrest.privacywidget.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdUnits
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.ui.graphics.vector.ImageVector

enum class QuickWinType(
    val title: String,
    val description: String,
    val timeEstimate: String,
    val icon: ImageVector,
    val instructions: String
) {
    DISABLE_WIFI_SCANNING(
        title = "Disable Background Wi-Fi Scanning",
        description = "Prevents location tracking via Wi-Fi networks",
        timeEstimate = "30 seconds",
        icon = Icons.Default.LocationOff,
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

    REPLACE_BROWSER(
        title = "Replace Browser",
        description = "Switch to privacy-focused browser",
        timeEstimate = "2 minutes",
        icon = Icons.Default.Explore,
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
    )
}

data class QuickWin(
    val type: QuickWinType,
    val relatedCheck: PrivacyCheck,
    val currentAppName: String? = null
) {
    /**
     * Get dynamic title based on current app name
     * e.g., "Replace SwiftKey" instead of generic "Replace Keyboard"
     */
    val displayTitle: String
        get() = when {
            currentAppName != null && (type == QuickWinType.REPLACE_KEYBOARD || type == QuickWinType.REPLACE_BROWSER) -> {
                "Replace $currentAppName"
            }
            else -> type.title
        }

    /**
     * Get impact (point value) from the related PrivacyCheck
     * This ensures synchronization across all displays - the PrivacyCheck enum is the single source of truth
     */
    val impact: Int
        get() = relatedCheck.pointDeduction
}
