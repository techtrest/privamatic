package com.techtrest.privamatic.data.model

import androidx.annotation.StringRes
import com.techtrest.privamatic.R

enum class PrivacyCheck(
    @StringRes val displayName: Int,
    val pointDeduction: Int,
    @StringRes val description: Int,
    @StringRes val recommendation: Int,
    val actionType: ActionType? = null,
    @StringRes val actionLabel: Int? = null,
    val packageName: String? = null,
    val isInformational: Boolean = false
) {
    // ===== SYSTEM SECURITY =====
    SCREEN_LOCK(
        displayName = R.string.privacy_check_screen_lock_name,
        pointDeduction = 11,
        description = R.string.privacy_check_screen_lock_description,
        recommendation = R.string.privacy_check_screen_lock_recommendation
    ),
    DEVICE_ENCRYPTION(
        displayName = R.string.privacy_check_device_encryption_name,
        pointDeduction = 10,
        description = R.string.privacy_check_device_encryption_description,
        recommendation = R.string.privacy_check_device_encryption_recommendation
    ),
    BIOMETRIC_AUTH(
        displayName = R.string.privacy_check_biometric_auth_name,
        pointDeduction = 2,
        description = R.string.privacy_check_biometric_auth_description,
        recommendation = R.string.privacy_check_biometric_auth_recommendation
    ),
    USB_DEBUGGING(
        displayName = R.string.privacy_check_usb_debugging_name,
        pointDeduction = 4,
        description = R.string.privacy_check_usb_debugging_description,
        recommendation = R.string.privacy_check_usb_debugging_recommendation
    ),
    DEVELOPER_OPTIONS(
        displayName = R.string.privacy_check_developer_options_name,
        pointDeduction = 1,
        description = R.string.privacy_check_developer_options_description,
        recommendation = R.string.privacy_check_developer_options_recommendation
    ),
    NOTIFICATION_LISTENER(
        displayName = R.string.privacy_check_notification_listener_name,
        pointDeduction = 5,
        description = R.string.privacy_check_notification_listener_description,
        recommendation = R.string.privacy_check_notification_listener_recommendation,
        actionType = ActionType.NOTIFICATION_LISTENER,
        actionLabel = R.string.privacy_check_notification_listener_action
    ),
    ACCESSIBILITY_SERVICE(
        displayName = R.string.privacy_check_accessibility_service_name,
        pointDeduction = 5,
        description = R.string.privacy_check_accessibility_service_description,
        recommendation = R.string.privacy_check_accessibility_service_recommendation,
        actionType = ActionType.ACCESSIBILITY_SETTINGS,
        actionLabel = R.string.privacy_check_accessibility_service_action
    ),
    DEVICE_ADMIN(
        displayName = R.string.privacy_check_device_admin_name,
        pointDeduction = 3,
        description = R.string.privacy_check_device_admin_description,
        recommendation = R.string.privacy_check_device_admin_recommendation,
        actionType = ActionType.DEVICE_ADMIN_SETTINGS,
        actionLabel = R.string.privacy_check_device_admin_action
    ),
    BACKGROUND_LOCATION_APPS(
        displayName = R.string.privacy_check_background_location_apps_name,
        pointDeduction = 4,
        description = R.string.privacy_check_background_location_apps_description,
        recommendation = R.string.privacy_check_background_location_apps_recommendation,
        actionType = ActionType.LOCATION_SETTINGS,
        actionLabel = R.string.privacy_check_background_location_apps_action
    ),

    // ===== NETWORK & TRACKING PRIVACY =====
    VPN_CONNECTION(
        displayName = R.string.privacy_check_vpn_connection_name,
        pointDeduction = 7,
        description = R.string.privacy_check_vpn_connection_description,
        recommendation = R.string.privacy_check_vpn_connection_recommendation
    ),
    PRIVATE_DNS(
        displayName = R.string.privacy_check_private_dns_name,
        pointDeduction = 6,
        description = R.string.privacy_check_private_dns_description,
        recommendation = R.string.privacy_check_private_dns_recommendation
    ),
    ADVERTISING_ID(
        displayName = R.string.privacy_check_advertising_id_name,
        pointDeduction = 5,
        description = R.string.privacy_check_advertising_id_description,
        recommendation = R.string.privacy_check_advertising_id_recommendation
    ),
    WIFI_SCANNING(
        displayName = R.string.privacy_check_wifi_scanning_name,
        pointDeduction = 1,
        description = R.string.privacy_check_wifi_scanning_description,
        recommendation = R.string.privacy_check_wifi_scanning_recommendation
    ),

    // ===== GOOGLE SERVICES =====
    FIND_MY_DEVICE(
        displayName = R.string.privacy_check_find_my_device_name,
        pointDeduction = 0,
        description = R.string.privacy_check_find_my_device_description,
        recommendation = R.string.privacy_check_find_my_device_recommendation
    ),
    GOOGLE_PLAY_SERVICES(
        displayName = R.string.privacy_check_google_play_services_name,
        pointDeduction = 8,
        description = R.string.privacy_check_google_play_services_description,
        recommendation = R.string.privacy_check_google_play_services_recommendation,
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = R.string.privacy_check_google_play_services_action,
        packageName = "com.google.android.gms"
    ),

    // ===== DEFAULT APPS =====
    DEFAULT_BROWSER(
        displayName = R.string.privacy_check_default_browser_name,
        pointDeduction = 3,
        description = R.string.privacy_check_default_browser_description,
        recommendation = R.string.privacy_check_default_browser_recommendation,
        actionType = ActionType.DEFAULT_APPS_SETTINGS,
        actionLabel = R.string.privacy_check_default_browser_action
    ),
    DEFAULT_KEYBOARD(
        displayName = R.string.privacy_check_default_keyboard_name,
        pointDeduction = 3,
        description = R.string.privacy_check_default_keyboard_description,
        recommendation = R.string.privacy_check_default_keyboard_recommendation,
        actionType = ActionType.DEFAULT_APPS_SETTINGS,
        actionLabel = R.string.privacy_check_default_keyboard_action
    ),
    DEFAULT_SMS(
        displayName = R.string.privacy_check_default_sms_name,
        pointDeduction = 2,
        description = R.string.privacy_check_default_sms_description,
        recommendation = R.string.privacy_check_default_sms_recommendation,
        actionType = ActionType.DEFAULT_APPS_SETTINGS,
        actionLabel = R.string.privacy_check_default_sms_action
    ),
    DEFAULT_EMAIL(
        displayName = R.string.privacy_check_default_email_name,
        pointDeduction = 2,
        description = R.string.privacy_check_default_email_description,
        recommendation = R.string.privacy_check_default_email_recommendation,
        actionType = ActionType.DEFAULT_APPS_SETTINGS,
        actionLabel = R.string.privacy_check_default_email_action
    ),
    DEFAULT_LAUNCHER(
        displayName = R.string.privacy_check_default_launcher_name,
        pointDeduction = 2,
        description = R.string.privacy_check_default_launcher_description,
        recommendation = R.string.privacy_check_default_launcher_recommendation,
        actionType = ActionType.DEFAULT_APPS_SETTINGS,
        actionLabel = R.string.privacy_check_default_launcher_action
    ),

    // ===== GOOGLE APPS (MAJOR) =====
    GOOGLE_CHROME(
        displayName = R.string.privacy_check_google_chrome_name,
        pointDeduction = 1,
        description = R.string.privacy_check_google_chrome_description,
        recommendation = R.string.privacy_check_google_chrome_recommendation,
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = R.string.privacy_check_google_chrome_action,
        packageName = PackageNames.CHROME
    ),
    GMAIL_APP(
        displayName = R.string.privacy_check_gmail_app_name,
        pointDeduction = 1,
        description = R.string.privacy_check_gmail_app_description,
        recommendation = R.string.privacy_check_gmail_app_recommendation,
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = R.string.privacy_check_gmail_app_action,
        packageName = PackageNames.GMAIL
    ),
    GOOGLE_MAPS(
        displayName = R.string.privacy_check_google_maps_name,
        pointDeduction = 1,
        description = R.string.privacy_check_google_maps_description,
        recommendation = R.string.privacy_check_google_maps_recommendation,
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = R.string.privacy_check_google_maps_action,
        packageName = PackageNames.GOOGLE_MAPS
    ),
    GOOGLE_PHOTOS(
        displayName = R.string.privacy_check_google_photos_name,
        pointDeduction = 1,
        description = R.string.privacy_check_google_photos_description,
        recommendation = R.string.privacy_check_google_photos_recommendation,
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = R.string.privacy_check_google_photos_action,
        packageName = PackageNames.GOOGLE_PHOTOS
    ),
    GOOGLE_DRIVE(
        displayName = R.string.privacy_check_google_drive_name,
        pointDeduction = 1,
        description = R.string.privacy_check_google_drive_description,
        recommendation = R.string.privacy_check_google_drive_recommendation,
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = R.string.privacy_check_google_drive_action,
        packageName = PackageNames.GOOGLE_DRIVE
    ),

    // ===== GOOGLE APPS (MINOR) =====
    YOUTUBE(
        displayName = R.string.privacy_check_youtube_name,
        pointDeduction = 1,
        description = R.string.privacy_check_youtube_description,
        recommendation = R.string.privacy_check_youtube_recommendation,
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = R.string.privacy_check_youtube_action,
        packageName = PackageNames.YOUTUBE
    ),
    GOOGLE_CALENDAR(
        displayName = R.string.privacy_check_google_calendar_name,
        pointDeduction = 1,
        description = R.string.privacy_check_google_calendar_description,
        recommendation = R.string.privacy_check_google_calendar_recommendation,
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = R.string.privacy_check_google_calendar_action,
        packageName = PackageNames.GOOGLE_CALENDAR
    ),
    GOOGLE_KEEP(
        displayName = R.string.privacy_check_google_keep_name,
        pointDeduction = 1,
        description = R.string.privacy_check_google_keep_description,
        recommendation = R.string.privacy_check_google_keep_recommendation,
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = R.string.privacy_check_google_keep_action,
        packageName = PackageNames.GOOGLE_KEEP
    ),
    GOOGLE_CAMERA(
        displayName = R.string.privacy_check_google_camera_name,
        pointDeduction = 1,
        description = R.string.privacy_check_google_camera_description,
        recommendation = R.string.privacy_check_google_camera_recommendation,
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = R.string.privacy_check_google_camera_action,
        packageName = PackageNames.GOOGLE_CAMERA
    ),
    GOOGLE_DOCS(
        displayName = R.string.privacy_check_google_docs_name,
        pointDeduction = 1,
        description = R.string.privacy_check_google_docs_description,
        recommendation = R.string.privacy_check_google_docs_recommendation,
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = R.string.privacy_check_google_docs_action,
        packageName = PackageNames.GOOGLE_DOCS
    ),

    // ===== META/FACEBOOK APPS =====
    FACEBOOK_APP(
        displayName = R.string.privacy_check_facebook_app_name,
        pointDeduction = 1,
        description = R.string.privacy_check_facebook_app_description,
        recommendation = R.string.privacy_check_facebook_app_recommendation,
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = R.string.privacy_check_facebook_app_action,
        packageName = PackageNames.FACEBOOK
    ),
    INSTAGRAM_APP(
        displayName = R.string.privacy_check_instagram_app_name,
        pointDeduction = 1,
        description = R.string.privacy_check_instagram_app_description,
        recommendation = R.string.privacy_check_instagram_app_recommendation,
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = R.string.privacy_check_instagram_app_action,
        packageName = PackageNames.INSTAGRAM
    ),
    WHATSAPP_APP(
        displayName = R.string.privacy_check_whatsapp_app_name,
        pointDeduction = 1,
        description = R.string.privacy_check_whatsapp_app_description,
        recommendation = R.string.privacy_check_whatsapp_app_recommendation,
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = R.string.privacy_check_whatsapp_app_action,
        packageName = PackageNames.WHATSAPP
    ),
    MESSENGER_APP(
        displayName = R.string.privacy_check_messenger_app_name,
        pointDeduction = 1,
        description = R.string.privacy_check_messenger_app_description,
        recommendation = R.string.privacy_check_messenger_app_recommendation,
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = R.string.privacy_check_messenger_app_action,
        packageName = PackageNames.MESSENGER
    ),

    // ===== MICROSOFT APPS =====
    EDGE_APP(
        displayName = R.string.privacy_check_edge_app_name,
        pointDeduction = 1,
        description = R.string.privacy_check_edge_app_description,
        recommendation = R.string.privacy_check_edge_app_recommendation,
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = R.string.privacy_check_edge_app_action,
        packageName = PackageNames.EDGE
    ),
    OUTLOOK_APP(
        displayName = R.string.privacy_check_outlook_app_name,
        pointDeduction = 1,
        description = R.string.privacy_check_outlook_app_description,
        recommendation = R.string.privacy_check_outlook_app_recommendation,
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = R.string.privacy_check_outlook_app_action,
        packageName = PackageNames.OUTLOOK
    ),
    ONEDRIVE_APP(
        displayName = R.string.privacy_check_onedrive_app_name,
        pointDeduction = 1,
        description = R.string.privacy_check_onedrive_app_description,
        recommendation = R.string.privacy_check_onedrive_app_recommendation,
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = R.string.privacy_check_onedrive_app_action,
        packageName = PackageNames.ONEDRIVE
    ),

    // ===== AMAZON APPS =====
    AMAZON_SHOPPING(
        displayName = R.string.privacy_check_amazon_shopping_name,
        pointDeduction = 1,
        description = R.string.privacy_check_amazon_shopping_description,
        recommendation = R.string.privacy_check_amazon_shopping_recommendation,
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = R.string.privacy_check_amazon_shopping_action,
        packageName = PackageNames.AMAZON_SHOPPING
    ),
    PRIME_VIDEO(
        displayName = R.string.privacy_check_prime_video_name,
        pointDeduction = 1,
        description = R.string.privacy_check_prime_video_description,
        recommendation = R.string.privacy_check_prime_video_recommendation,
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = R.string.privacy_check_prime_video_action,
        packageName = PackageNames.PRIME_VIDEO
    ),

    // ===== AI/LLM APPS =====
    CHATGPT_APP(
        displayName = R.string.privacy_check_chatgpt_app_name,
        pointDeduction = 0,
        description = R.string.privacy_check_ai_app_description,
        recommendation = R.string.privacy_check_ai_app_recommendation,
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = R.string.privacy_check_chatgpt_app_action,
        packageName = PackageNames.CHATGPT,
        isInformational = true
    ),
    GOOGLE_GEMINI(
        displayName = R.string.privacy_check_google_gemini_name,
        pointDeduction = 0,
        description = R.string.privacy_check_ai_app_description,
        recommendation = R.string.privacy_check_ai_app_recommendation,
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = R.string.privacy_check_google_gemini_action,
        packageName = PackageNames.GEMINI,
        isInformational = true
    ),
    MICROSOFT_COPILOT(
        displayName = R.string.privacy_check_microsoft_copilot_name,
        pointDeduction = 0,
        description = R.string.privacy_check_ai_app_description,
        recommendation = R.string.privacy_check_ai_app_recommendation,
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = R.string.privacy_check_microsoft_copilot_action,
        packageName = PackageNames.COPILOT,
        isInformational = true
    ),
    CLAUDE_APP(
        displayName = R.string.privacy_check_claude_app_name,
        pointDeduction = 0,
        description = R.string.privacy_check_ai_app_description,
        recommendation = R.string.privacy_check_ai_app_recommendation,
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = R.string.privacy_check_claude_app_action,
        packageName = PackageNames.CLAUDE,
        isInformational = true
    ),
    PERPLEXITY_APP(
        displayName = R.string.privacy_check_perplexity_app_name,
        pointDeduction = 0,
        description = R.string.privacy_check_ai_app_description,
        recommendation = R.string.privacy_check_ai_app_recommendation,
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = R.string.privacy_check_perplexity_app_action,
        packageName = PackageNames.PERPLEXITY,
        isInformational = true
    ),
    META_AI(
        displayName = R.string.privacy_check_meta_ai_name,
        pointDeduction = 0,
        description = R.string.privacy_check_ai_app_description,
        recommendation = R.string.privacy_check_ai_app_recommendation,
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = R.string.privacy_check_meta_ai_action,
        packageName = PackageNames.META_AI,
        isInformational = true
    ),

    // ===== SOCIAL MEDIA =====
    TIKTOK_APP(
        displayName = R.string.privacy_check_tiktok_app_name,
        pointDeduction = 1,
        description = R.string.privacy_check_tiktok_app_description,
        recommendation = R.string.privacy_check_tiktok_app_recommendation,
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = R.string.privacy_check_tiktok_app_action,
        packageName = PackageNames.TIKTOK
    ),
    TWITTER_APP(
        displayName = R.string.privacy_check_twitter_app_name,
        pointDeduction = 1,
        description = R.string.privacy_check_twitter_app_description,
        recommendation = R.string.privacy_check_twitter_app_recommendation,
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = R.string.privacy_check_twitter_app_action,
        packageName = PackageNames.TWITTER
    ),
    REDDIT_APP(
        displayName = R.string.privacy_check_reddit_app_name,
        pointDeduction = 1,
        description = R.string.privacy_check_reddit_app_description,
        recommendation = R.string.privacy_check_reddit_app_recommendation,
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = R.string.privacy_check_reddit_app_action,
        packageName = PackageNames.REDDIT
    ),

    // ===== LEGACY (kept for compatibility) =====
    DEFAULT_ASSISTANT(
        displayName = R.string.privacy_check_default_assistant_name,
        pointDeduction = 0,
        description = R.string.privacy_check_default_assistant_description,
        recommendation = R.string.privacy_check_default_assistant_recommendation
    )
}
