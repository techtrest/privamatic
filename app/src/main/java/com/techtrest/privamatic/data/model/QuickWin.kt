package com.techtrest.privamatic.data.model

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.AdUnits
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.ui.graphics.vector.ImageVector
import com.techtrest.privamatic.R

enum class QuickWinType(
    @StringRes val title: Int,
    @StringRes val description: Int,
    val timeEstimate: String,
    val icon: ImageVector,
    @StringRes val instructions: Int,
    val actionType: ActionType? = null,
    @StringRes val actionLabel: Int? = null
) {
    // ===== SYSTEM SERVICE REVOCATIONS =====

    REVOKE_NOTIFICATION_LISTENERS(
        title = R.string.quick_win_revoke_notification_listeners_title,
        description = R.string.quick_win_revoke_notification_listeners_description,
        timeEstimate = "1 minute",
        icon = Icons.Default.NotificationsOff,
        actionType = ActionType.NOTIFICATION_LISTENER,
        actionLabel = R.string.quick_win_revoke_notification_listeners_action,
        instructions = R.string.quick_win_revoke_notification_listeners_instructions
    ),

    REVOKE_ACCESSIBILITY_SERVICES(
        title = R.string.quick_win_revoke_accessibility_services_title,
        description = R.string.quick_win_revoke_accessibility_services_description,
        timeEstimate = "1 minute",
        icon = Icons.Default.Accessibility,
        actionType = ActionType.ACCESSIBILITY_SETTINGS,
        actionLabel = R.string.quick_win_revoke_accessibility_services_action,
        instructions = R.string.quick_win_revoke_accessibility_services_instructions
    ),

    REVOKE_DEVICE_ADMINS(
        title = R.string.quick_win_revoke_device_admins_title,
        description = R.string.quick_win_revoke_device_admins_description,
        timeEstimate = "1 minute",
        icon = Icons.Default.Security,
        actionType = ActionType.DEVICE_ADMIN_SETTINGS,
        actionLabel = R.string.quick_win_revoke_device_admins_action,
        instructions = R.string.quick_win_revoke_device_admins_instructions
    ),

    // ===== SETTINGS TOGGLES =====

    DISABLE_WIFI_SCANNING(
        title = R.string.quick_win_disable_wifi_scanning_title,
        description = R.string.quick_win_disable_wifi_scanning_description,
        timeEstimate = "30 seconds",
        icon = Icons.Default.LocationOff,
        actionType = ActionType.LOCATION_SETTINGS,
        actionLabel = R.string.quick_win_disable_wifi_scanning_action,
        instructions = R.string.quick_win_disable_wifi_scanning_instructions
    ),

    DISABLE_ADVERTISING_ID(
        title = R.string.quick_win_disable_advertising_id_title,
        description = R.string.quick_win_disable_advertising_id_description,
        timeEstimate = "30 seconds",
        icon = Icons.Default.AdUnits,
        actionType = ActionType.PRIVACY_SETTINGS,
        actionLabel = R.string.quick_win_disable_advertising_id_action,
        instructions = R.string.quick_win_disable_advertising_id_instructions
    ),

    ENABLE_PRIVATE_DNS(
        title = R.string.quick_win_enable_private_dns_title,
        description = R.string.quick_win_enable_private_dns_description,
        timeEstimate = "1 minute",
        icon = Icons.Default.Language,
        actionType = ActionType.NETWORK_SETTINGS,
        actionLabel = R.string.quick_win_enable_private_dns_action,
        instructions = R.string.quick_win_enable_private_dns_instructions
    ),

    DISABLE_DEVELOPER_OPTIONS(
        title = R.string.quick_win_disable_developer_options_title,
        description = R.string.quick_win_disable_developer_options_description,
        timeEstimate = "30 seconds",
        icon = Icons.Default.Code,
        actionType = ActionType.DEVELOPER_SETTINGS,
        actionLabel = R.string.quick_win_disable_developer_options_action,
        instructions = R.string.quick_win_disable_developer_options_instructions
    ),

    // ===== DEFAULT APP REPLACEMENTS =====

    REPLACE_BROWSER(
        title = R.string.quick_win_replace_browser_title,
        description = R.string.quick_win_replace_browser_description,
        timeEstimate = "2 minutes",
        icon = Icons.Default.Explore,
        actionType = ActionType.DEFAULT_APPS_SETTINGS,
        actionLabel = R.string.quick_win_replace_browser_action,
        instructions = R.string.quick_win_replace_browser_instructions
    ),

    REPLACE_KEYBOARD(
        title = R.string.quick_win_replace_keyboard_title,
        description = R.string.quick_win_replace_keyboard_description,
        timeEstimate = "2 minutes",
        icon = Icons.Default.Keyboard,
        actionType = ActionType.DEFAULT_APPS_SETTINGS,
        actionLabel = R.string.quick_win_replace_keyboard_action,
        instructions = R.string.quick_win_replace_keyboard_instructions
    ),

    REPLACE_DEFAULT_SMS(
        title = R.string.quick_win_replace_default_sms_title,
        description = R.string.quick_win_replace_default_sms_description,
        timeEstimate = "2 minutes",
        icon = Icons.Default.Sms,
        actionType = ActionType.DEFAULT_APPS_SETTINGS,
        actionLabel = R.string.quick_win_replace_default_sms_action,
        instructions = R.string.quick_win_replace_default_sms_instructions
    ),

    REPLACE_DEFAULT_EMAIL(
        title = R.string.quick_win_replace_default_email_title,
        description = R.string.quick_win_replace_default_email_description,
        timeEstimate = "2 minutes",
        icon = Icons.Default.Email,
        actionType = ActionType.DEFAULT_APPS_SETTINGS,
        actionLabel = R.string.quick_win_replace_default_email_action,
        instructions = R.string.quick_win_replace_default_email_instructions
    ),

    REPLACE_DEFAULT_LAUNCHER(
        title = R.string.quick_win_replace_default_launcher_title,
        description = R.string.quick_win_replace_default_launcher_description,
        timeEstimate = "2 minutes",
        icon = Icons.Default.Home,
        actionType = ActionType.DEFAULT_APPS_SETTINGS,
        actionLabel = R.string.quick_win_replace_default_launcher_action,
        instructions = R.string.quick_win_replace_default_launcher_instructions
    ),

    // ===== APP UNINSTALLS =====

    UNINSTALL_APP(
        title = R.string.quick_win_uninstall_app_title,
        description = R.string.quick_win_uninstall_app_description,
        timeEstimate = "30 seconds",
        icon = Icons.Default.Delete,
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = R.string.quick_win_uninstall_app_action,
        instructions = R.string.quick_win_uninstall_app_instructions
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
    fun displayTitle(context: Context): String = when {
        currentAppName != null -> when (type) {
            QuickWinType.UNINSTALL_APP -> context.getString(R.string.fmt_uninstall_app, currentAppName)
            QuickWinType.REPLACE_BROWSER,
            QuickWinType.REPLACE_KEYBOARD,
            QuickWinType.REPLACE_DEFAULT_SMS,
            QuickWinType.REPLACE_DEFAULT_EMAIL,
            QuickWinType.REPLACE_DEFAULT_LAUNCHER -> context.getString(R.string.fmt_replace_app, currentAppName)
            else -> context.getString(type.title)
        }
        else -> context.getString(type.title)
    }

    /**
     * Get impact (point value) from the related PrivacyCheck
     * This ensures synchronization across all displays - the PrivacyCheck enum is the single source of truth
     */
    val impact: Int
        get() = relatedCheck?.pointDeduction ?: 0
}
