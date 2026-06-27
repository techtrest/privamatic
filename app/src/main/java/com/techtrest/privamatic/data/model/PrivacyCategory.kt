package com.techtrest.privamatic.data.model

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.ui.graphics.vector.ImageVector
import com.techtrest.privamatic.R

enum class PrivacyCategory(
    @StringRes val displayName: Int,
    val icon: ImageVector,
    val checks: List<PrivacyCheck>
) {
    SYSTEM_SECURITY(
        displayName = R.string.privacy_category_system_security_name,
        icon = Icons.Filled.Security,
        checks = listOf(
            PrivacyCheck.SCREEN_LOCK,
            PrivacyCheck.DEVICE_ENCRYPTION,
            PrivacyCheck.BIOMETRIC_AUTH,
            PrivacyCheck.USB_DEBUGGING,
            PrivacyCheck.DEVELOPER_OPTIONS,
            PrivacyCheck.NOTIFICATION_LISTENER,
            PrivacyCheck.ACCESSIBILITY_SERVICE,
            PrivacyCheck.DEVICE_ADMIN,
            PrivacyCheck.BACKGROUND_LOCATION_APPS,
            PrivacyCheck.SECURITY_PATCH,
            PrivacyCheck.OLD_TARGET_SDK
        )
    ),
    NETWORK_PRIVACY(
        displayName = R.string.privacy_category_network_privacy_name,
        icon = Icons.Filled.Language,
        checks = listOf(
            PrivacyCheck.VPN_CONNECTION,
            PrivacyCheck.PRIVATE_DNS,
            PrivacyCheck.ADVERTISING_ID,
            PrivacyCheck.WIFI_SCANNING
        )
    ),
    GOOGLE_SERVICES(
        displayName = R.string.privacy_category_google_services_name,
        icon = Icons.Filled.Cloud,
        checks = listOf(
            PrivacyCheck.FIND_MY_DEVICE,
            PrivacyCheck.GOOGLE_PLAY_SERVICES
        )
    ),
    DEFAULT_APPS(
        displayName = R.string.privacy_category_default_apps_name,
        icon = Icons.Filled.Apps,
        checks = listOf(
            PrivacyCheck.DEFAULT_BROWSER,
            PrivacyCheck.DEFAULT_KEYBOARD,
            PrivacyCheck.DEFAULT_SMS,
            PrivacyCheck.DEFAULT_EMAIL,
            PrivacyCheck.DEFAULT_LAUNCHER
        )
    ),
    GOOGLE_APPS(
        displayName = R.string.privacy_category_google_apps_name,
        icon = Icons.Filled.Business,
        checks = listOf(
            PrivacyCheck.GOOGLE_CHROME,
            PrivacyCheck.GMAIL_APP,
            PrivacyCheck.GOOGLE_MAPS,
            PrivacyCheck.GOOGLE_PHOTOS,
            PrivacyCheck.GOOGLE_DRIVE,
            PrivacyCheck.YOUTUBE,
            PrivacyCheck.GOOGLE_CALENDAR,
            PrivacyCheck.GOOGLE_KEEP,
            PrivacyCheck.GOOGLE_CAMERA,
            PrivacyCheck.GOOGLE_DOCS
        )
    ),
    META_FACEBOOK_APPS(
        displayName = R.string.privacy_category_meta_facebook_name,
        icon = Icons.Filled.People,
        checks = listOf(
            PrivacyCheck.FACEBOOK_APP,
            PrivacyCheck.INSTAGRAM_APP,
            PrivacyCheck.WHATSAPP_APP,
            PrivacyCheck.MESSENGER_APP
        )
    ),
    MICROSOFT_APPS(
        displayName = R.string.privacy_category_microsoft_name,
        icon = Icons.Filled.Computer,
        checks = listOf(
            PrivacyCheck.EDGE_APP,
            PrivacyCheck.OUTLOOK_APP,
            PrivacyCheck.ONEDRIVE_APP
        )
    ),
    AI_AND_OTHER_APPS(
        displayName = R.string.privacy_category_ai_other_name,
        icon = Icons.Filled.SmartToy,
        checks = listOf(
            PrivacyCheck.CHATGPT_APP,
            PrivacyCheck.GOOGLE_GEMINI,
            PrivacyCheck.MICROSOFT_COPILOT,
            PrivacyCheck.CLAUDE_APP,
            PrivacyCheck.PERPLEXITY_APP,
            PrivacyCheck.META_AI,
            PrivacyCheck.AMAZON_SHOPPING,
            PrivacyCheck.PRIME_VIDEO,
            PrivacyCheck.TIKTOK_APP,
            PrivacyCheck.TWITTER_APP,
            PrivacyCheck.REDDIT_APP
        )
    );

    companion object {
        /**
         * Optimized Set for O(1) lookup performance when checking system security category membership.
         */
        val systemSecurityChecksSet: Set<PrivacyCheck> = SYSTEM_SECURITY.checks.toSet()

        /**
         * Get the category for a specific privacy check
         */
        fun getCategoryForCheck(check: PrivacyCheck): PrivacyCategory? {
            return entries.firstOrNull { category ->
                check in category.checks
            }
        }

        /**
         * Get the number of issues and total checks for a category
         * @return Pair<issuesCount, totalCount>
         */
        fun getIssuesCount(category: PrivacyCategory, privacyScore: PrivacyScore): Pair<Int, Int> {
            val categoryIssues = privacyScore.issues.filter { issue ->
                issue.check in category.checks
            }

            val issuesCount = categoryIssues.count { !it.isSecure && !it.check.isInformational }
            val totalCount = categoryIssues.size

            return Pair(issuesCount, totalCount)
        }

        /**
         * Get all issues for a specific category
         */
        fun getIssuesForCategory(category: PrivacyCategory, privacyScore: PrivacyScore): List<PrivacyIssue> {
            return privacyScore.issues.filter { issue ->
                issue.check in category.checks
            }
        }
    }
}
