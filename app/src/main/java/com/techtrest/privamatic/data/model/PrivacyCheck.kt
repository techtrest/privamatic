package com.techtrest.privamatic.data.model

enum class PrivacyCheck(
    val displayName: String,
    val pointDeduction: Int,
    val description: String,
    val recommendation: String,
    val actionType: ActionType? = null,
    val actionLabel: String? = null,
    val packageName: String? = null,
    val isInformational: Boolean = false
) {
    // ===== SYSTEM SECURITY =====
    SCREEN_LOCK(
        displayName = "Screen Lock",
        pointDeduction = 11,
        description = "Device should have a screen lock enabled (PIN, pattern, password, or biometric)",
        recommendation = "Enable a screen lock in Settings > Security > Screen lock to protect your device from unauthorized access."
    ),
    DEVICE_ENCRYPTION(
        displayName = "Device Encryption",
        pointDeduction = 10,
        description = "Device storage should be encrypted to protect your data if the device is lost or stolen",
        recommendation = "Enable device encryption in Settings > Security > Encryption (typically enabled by default on modern devices)."
    ),
    BIOMETRIC_AUTH(
        displayName = "Biometric Authentication",
        pointDeduction = 2,
        description = "Biometric authentication should be enabled for added security",
        recommendation = "Set up fingerprint or face unlock in Settings > Security > Biometrics for convenient and secure authentication."
    ),
    USB_DEBUGGING(
        displayName = "USB Debugging",
        pointDeduction = 4,
        description = "USB debugging should be disabled for security",
        recommendation = "Disable USB debugging in Settings > Developer options to prevent unauthorized access via USB connection."
    ),
    DEVELOPER_OPTIONS(
        displayName = "Developer Options",
        pointDeduction = 1,
        description = "Developer options should be disabled on personal devices",
        recommendation = "Disable developer options in Settings > System > Developer options to reduce security risks."
    ),
    NOTIFICATION_LISTENER(
        displayName = "Notification Listener Access",
        pointDeduction = 5,
        description = "Apps with notification listener permission can read ALL notifications including 2FA codes and messages",
        recommendation = "Review and revoke notification access for non-essential apps in Settings > Apps > Special app access > Notification access.",
        actionType = ActionType.NOTIFICATION_LISTENER,
        actionLabel = "Manage Permissions"
    ),
    ACCESSIBILITY_SERVICE(
        displayName = "Accessibility Service Access",
        pointDeduction = 5,
        description = "Apps with accessibility services can control your entire device and potentially log your keystrokes",
        recommendation = "Disable accessibility services for apps that don't legitimately need them in Settings > Accessibility.",
        actionType = ActionType.ACCESSIBILITY_SETTINGS,
        actionLabel = "Manage Services"
    ),
    DEVICE_ADMIN(
        displayName = "Device Administrator Apps",
        pointDeduction = 3,
        description = "Apps with device admin privileges have elevated control over your device",
        recommendation = "Review and remove device admin access for non-essential apps in Settings > Security > Device admin apps.",
        actionType = ActionType.DEVICE_ADMIN_SETTINGS,
        actionLabel = "Manage Admins"
    ),
    BACKGROUND_LOCATION_APPS(
        displayName = "Background Location Access",
        pointDeduction = 4,
        description = "Apps with background location permission can track your precise location at all times, even when not in use",
        recommendation = "Review and restrict background location permissions in Settings > Location > App permissions. Only grant background location to apps that genuinely need it.",
        actionType = ActionType.LOCATION_SETTINGS,
        actionLabel = "Manage Permissions"
    ),

    // ===== NETWORK & TRACKING PRIVACY =====
    VPN_CONNECTION(
        displayName = "VPN Protection",
        pointDeduction = 7,
        description = "VPN should be active to protect network traffic",
        recommendation = "Enable a VPN connection to encrypt your internet traffic and protect your privacy online."
    ),
    PRIVATE_DNS(
        displayName = "Private DNS",
        pointDeduction = 6,
        description = "Private DNS encrypts your web lookups so your internet provider can't see which sites you visit",
        recommendation = "Configure Private DNS in Settings > Network & internet > Private DNS. Recommended: dns.quad9.net (privacy-focused, blocks malware) or dns.adguard-dns.com (blocks ads and trackers)."
    ),
    ADVERTISING_ID(
        displayName = "Advertising ID",
        pointDeduction = 5,
        description = "Advertising ID enables cross-app tracking by advertisers",
        recommendation = "Disable or reset your Advertising ID in Settings > Privacy > Ads."
    ),
    WIFI_SCANNING(
        displayName = "Background Wi-Fi Scanning",
        pointDeduction = 1,
        description = "Background Wi-Fi/Bluetooth scanning can track your location",
        recommendation = "Disable Wi-Fi and Bluetooth scanning in Settings > Location > Wi-Fi and Bluetooth scanning to prevent location tracking."
    ),

    // ===== GOOGLE SERVICES =====
    FIND_MY_DEVICE(
        displayName = "Find My Device",
        pointDeduction = 0,
        description = "Google's Find My Device / Find Hub tracks device location — a security feature with a privacy cost. On Android 14+ the enabled state cannot be programmatically detected.",
        recommendation = "Disable in Settings > Security > Find My Device (or Find Hub on Android 14+) if privacy is a priority over theft protection."
    ),
    GOOGLE_PLAY_SERVICES(
        displayName = "Google Play Services",
        pointDeduction = 8,
        description = "Google Play Services has deep access to your device and enables extensive tracking and data collection across all apps",
        recommendation = "Consider using GrapheneOS to run Google Play Services with limited access, significantly reducing its reach on your device.",
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = "Open App Settings",
        packageName = "com.google.android.gms"
    ),

    // ===== DEFAULT APPS =====
    DEFAULT_BROWSER(
        displayName = "Default Browser",
        pointDeduction = 3,
        description = "Privacy-invasive browser detected as default",
        recommendation = "Switch to a privacy-respecting browser like Brave, Firefox, or DuckDuckGo Browser. Available on Google Play Store.",
        actionType = ActionType.DEFAULT_APPS_SETTINGS,
        actionLabel = "Change Default"
    ),
    DEFAULT_KEYBOARD(
        displayName = "Default Keyboard",
        pointDeduction = 3,
        description = "Privacy-invasive keyboard detected as default",
        recommendation = "Switch to FUTO Keyboard (Play Store) for best privacy + features with offline voice typing, or AnySoftKeyboard (F-Droid & Play Store) for a mature, highly customizable option.",
        actionType = ActionType.DEFAULT_APPS_SETTINGS,
        actionLabel = "Change Default"
    ),
    DEFAULT_SMS(
        displayName = "Default SMS/Messaging",
        pointDeduction = 2,
        description = "Privacy-invasive messaging app detected as default",
        recommendation = "Switch to Fossify Messages - available on F-Droid and Play Store. Open source, privacy-focused, no ads.",
        actionType = ActionType.DEFAULT_APPS_SETTINGS,
        actionLabel = "Change Default"
    ),
    DEFAULT_EMAIL(
        displayName = "Default Email",
        pointDeduction = 2,
        description = "Privacy-invasive email app detected as default",
        recommendation = "Switch to a privacy-respecting email client like K-9 Mail, FairEmail, or ProtonMail. Available on Google Play Store.",
        actionType = ActionType.DEFAULT_APPS_SETTINGS,
        actionLabel = "Change Default"
    ),
    DEFAULT_LAUNCHER(
        displayName = "Default Launcher",
        pointDeduction = 2,
        description = "Privacy-invasive launcher detected as default",
        recommendation = "Switch to a privacy-respecting launcher like Lawnchair, KISS Launcher, or Neo Launcher. Available on Google Play Store.",
        actionType = ActionType.DEFAULT_APPS_SETTINGS,
        actionLabel = "Change Default"
    ),

    // ===== GOOGLE APPS (MAJOR) =====
    GOOGLE_CHROME(
        displayName = "Chrome",
        pointDeduction = 1,
        description = "Google Chrome app is installed on your device",
        recommendation = "Consider uninstalling Chrome and switching to Brave, Firefox, or DuckDuckGo Browser.",
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = "Open App Settings",
        packageName = PackageNames.CHROME
    ),
    GMAIL_APP(
        displayName = "Gmail",
        pointDeduction = 1,
        description = "Gmail app is installed on your device",
        recommendation = "Consider uninstalling Gmail and switching to K-9 Mail, FairEmail, or ProtonMail.",
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = "Open App Settings",
        packageName = PackageNames.GMAIL
    ),
    GOOGLE_MAPS(
        displayName = "Google Maps",
        pointDeduction = 1,
        description = "Google Maps app is installed on your device",
        recommendation = "Consider uninstalling Google Maps and switching to OsmAnd or Organic Maps.",
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = "Open App Settings",
        packageName = PackageNames.GOOGLE_MAPS
    ),
    GOOGLE_PHOTOS(
        displayName = "Google Photos",
        pointDeduction = 1,
        description = "Google Photos app is installed on your device",
        recommendation = "Consider uninstalling Google Photos and switching to Simple Gallery or Aves Libre.",
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = "Open App Settings",
        packageName = PackageNames.GOOGLE_PHOTOS
    ),
    GOOGLE_DRIVE(
        displayName = "Google Drive",
        pointDeduction = 1,
        description = "Google Drive app is installed on your device",
        recommendation = "Consider uninstalling Google Drive and switching to Nextcloud or Cryptomator.",
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = "Open App Settings",
        packageName = PackageNames.GOOGLE_DRIVE
    ),

    // ===== GOOGLE APPS (MINOR) =====
    YOUTUBE(
        displayName = "YouTube",
        pointDeduction = 1,
        description = "YouTube app is installed on your device",
        recommendation = "Consider using NewPipe or LibreTube as privacy-friendly YouTube alternatives.",
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = "Open App Settings",
        packageName = PackageNames.YOUTUBE
    ),
    GOOGLE_CALENDAR(
        displayName = "Google Calendar",
        pointDeduction = 1,
        description = "Google Calendar app is installed on your device",
        recommendation = "Consider uninstalling Google Calendar and switching to Simple Calendar or an offline calendar app.",
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = "Open App Settings",
        packageName = PackageNames.GOOGLE_CALENDAR
    ),
    GOOGLE_KEEP(
        displayName = "Google Keep",
        pointDeduction = 1,
        description = "Google Keep app is installed on your device",
        recommendation = "Consider uninstalling Google Keep and switching to Simple Notes or Standard Notes.",
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = "Open App Settings",
        packageName = PackageNames.GOOGLE_KEEP
    ),
    GOOGLE_CAMERA(
        displayName = "Google Camera",
        pointDeduction = 1,
        description = "Google Camera app is installed on your device",
        recommendation = "Consider uninstalling Google Camera and switching to Open Camera or Simple Camera.",
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = "Open App Settings",
        packageName = PackageNames.GOOGLE_CAMERA
    ),
    GOOGLE_DOCS(
        displayName = "Google Docs",
        pointDeduction = 1,
        description = "Google Docs app is installed on your device",
        recommendation = "Consider uninstalling Google Docs and switching to Collabora Office or an offline editor.",
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = "Open App Settings",
        packageName = PackageNames.GOOGLE_DOCS
    ),

    // ===== META/FACEBOOK APPS =====
    FACEBOOK_APP(
        displayName = "Facebook",
        pointDeduction = 1,
        description = "Facebook app is installed on your device",
        recommendation = "Consider uninstalling the Facebook app and using the mobile website if needed.",
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = "Open App Settings",
        packageName = PackageNames.FACEBOOK
    ),
    INSTAGRAM_APP(
        displayName = "Instagram",
        pointDeduction = 1,
        description = "Instagram app is installed on your device",
        recommendation = "Consider uninstalling the Instagram app and using the mobile website if needed, or switching to Pixelfed.",
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = "Open App Settings",
        packageName = PackageNames.INSTAGRAM
    ),
    WHATSAPP_APP(
        displayName = "WhatsApp",
        pointDeduction = 1,
        description = "WhatsApp is installed on your device",
        recommendation = "Consider uninstalling WhatsApp and switching to Signal, Session, or SimpleX Chat.",
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = "Open App Settings",
        packageName = PackageNames.WHATSAPP
    ),
    MESSENGER_APP(
        displayName = "Messenger",
        pointDeduction = 1,
        description = "Facebook Messenger app is installed on your device",
        recommendation = "Consider uninstalling Messenger and switching to Signal or another privacy-focused messaging app.",
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = "Open App Settings",
        packageName = PackageNames.MESSENGER
    ),

    // ===== MICROSOFT APPS =====
    EDGE_APP(
        displayName = "Microsoft Edge",
        pointDeduction = 1,
        description = "Microsoft Edge app is installed on your device",
        recommendation = "Consider uninstalling Edge and switching to Brave, Firefox, or DuckDuckGo Browser.",
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = "Open App Settings",
        packageName = PackageNames.EDGE
    ),
    OUTLOOK_APP(
        displayName = "Outlook",
        pointDeduction = 1,
        description = "Microsoft Outlook app is installed on your device",
        recommendation = "Consider uninstalling Outlook and switching to K-9 Mail, FairEmail, or ProtonMail.",
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = "Open App Settings",
        packageName = PackageNames.OUTLOOK
    ),
    ONEDRIVE_APP(
        displayName = "OneDrive",
        pointDeduction = 1,
        description = "Microsoft OneDrive app is installed on your device",
        recommendation = "Consider uninstalling OneDrive and switching to Nextcloud or Cryptomator.",
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = "Open App Settings",
        packageName = PackageNames.ONEDRIVE
    ),

    // ===== AMAZON APPS =====
    AMAZON_SHOPPING(
        displayName = "Amazon Shopping",
        pointDeduction = 1,
        description = "Amazon Shopping app is installed on your device",
        recommendation = "Consider uninstalling and using the mobile website for better privacy.",
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = "Open App Settings",
        packageName = PackageNames.AMAZON_SHOPPING
    ),
    PRIME_VIDEO(
        displayName = "Prime Video",
        pointDeduction = 1,
        description = "Amazon Prime Video app is installed on your device",
        recommendation = "Consider uninstalling Prime Video and using the mobile website instead. Streaming apps collect viewing habits and device data.",
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = "Open App Settings",
        packageName = PackageNames.PRIME_VIDEO
    ),

    // ===== AI/LLM APPS =====
    CHATGPT_APP(
        displayName = "ChatGPT",
        pointDeduction = 0,
        description = "AI apps process conversations on cloud servers — be mindful of what you share",
        recommendation = "Avoid sharing passwords, financial details, or personal information in AI conversations. Review your chat history and data settings in the app.",
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = "Open App Settings",
        packageName = PackageNames.CHATGPT,
        isInformational = true
    ),
    GOOGLE_GEMINI(
        displayName = "Google Gemini",
        pointDeduction = 0,
        description = "AI apps process conversations on cloud servers — be mindful of what you share",
        recommendation = "Avoid sharing passwords, financial details, or personal information in AI conversations. Review your chat history and data settings in the app.",
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = "Open App Settings",
        packageName = PackageNames.GEMINI,
        isInformational = true
    ),
    MICROSOFT_COPILOT(
        displayName = "Microsoft Copilot",
        pointDeduction = 0,
        description = "AI apps process conversations on cloud servers — be mindful of what you share",
        recommendation = "Avoid sharing passwords, financial details, or personal information in AI conversations. Review your chat history and data settings in the app.",
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = "Open App Settings",
        packageName = PackageNames.COPILOT,
        isInformational = true
    ),
    CLAUDE_APP(
        displayName = "Claude",
        pointDeduction = 0,
        description = "AI apps process conversations on cloud servers — be mindful of what you share",
        recommendation = "Avoid sharing passwords, financial details, or personal information in AI conversations. Review your chat history and data settings in the app.",
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = "Open App Settings",
        packageName = PackageNames.CLAUDE,
        isInformational = true
    ),
    PERPLEXITY_APP(
        displayName = "Perplexity",
        pointDeduction = 0,
        description = "AI apps process conversations on cloud servers — be mindful of what you share",
        recommendation = "Avoid sharing passwords, financial details, or personal information in AI conversations. Review your chat history and data settings in the app.",
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = "Open App Settings",
        packageName = PackageNames.PERPLEXITY,
        isInformational = true
    ),
    META_AI(
        displayName = "Meta AI",
        pointDeduction = 0,
        description = "AI apps process conversations on cloud servers — be mindful of what you share",
        recommendation = "Avoid sharing passwords, financial details, or personal information in AI conversations. Review your chat history and data settings in the app.",
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = "Open App Settings",
        packageName = PackageNames.META_AI,
        isInformational = true
    ),

    // ===== SOCIAL MEDIA =====
    TIKTOK_APP(
        displayName = "TikTok",
        pointDeduction = 1,
        description = "TikTok app is installed on your device",
        recommendation = "Consider uninstalling TikTok due to extensive data collection practices.",
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = "Open App Settings",
        packageName = PackageNames.TIKTOK
    ),
    TWITTER_APP(
        displayName = "Twitter/X",
        pointDeduction = 1,
        description = "Twitter/X app is installed on your device",
        recommendation = "Consider using the mobile website instead of the app for better privacy.",
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = "Open App Settings",
        packageName = PackageNames.TWITTER
    ),
    REDDIT_APP(
        displayName = "Reddit",
        pointDeduction = 1,
        description = "Reddit app is installed on your device",
        recommendation = "Consider using privacy-focused Reddit clients like Infinity or Slide.",
        actionType = ActionType.OPEN_APP_SETTINGS,
        actionLabel = "Open App Settings",
        packageName = PackageNames.REDDIT
    ),

    // ===== LEGACY (kept for compatibility) =====
    DEFAULT_ASSISTANT(
        displayName = "System Assistant/Search",
        pointDeduction = 0,
        description = "Voice assistants can collect and process your conversations and search history",
        recommendation = "Review your assistant settings in Settings > Apps and disable Google Assistant or Alexa if you don't use them."
    )
}
