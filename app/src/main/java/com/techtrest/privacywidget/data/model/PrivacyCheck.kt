package com.techtrest.privacywidget.data.model

enum class PrivacyCheck(
    val displayName: String,
    val pointDeduction: Int,
    val description: String,
    val recommendation: String
) {
    // ===== SYSTEM SECURITY =====
    SCREEN_LOCK(
        displayName = "Screen Lock",
        pointDeduction = 15,
        description = "Device should have a screen lock enabled (PIN, pattern, password, or biometric)",
        recommendation = "Enable a screen lock in Settings > Security > Screen lock to protect your device from unauthorized access."
    ),
    DEVICE_ENCRYPTION(
        displayName = "Device Encryption",
        pointDeduction = 10,
        description = "Device storage should be encrypted to protect data at rest",
        recommendation = "Enable device encryption in Settings > Security > Encryption (typically enabled by default on modern devices)."
    ),
    BIOMETRIC_AUTH(
        displayName = "Biometric Authentication",
        pointDeduction = 3,
        description = "Biometric authentication should be enabled for added security",
        recommendation = "Set up fingerprint or face unlock in Settings > Security > Biometrics for convenient and secure authentication."
    ),
    USB_DEBUGGING(
        displayName = "USB Debugging",
        pointDeduction = 5,
        description = "USB debugging should be disabled for security",
        recommendation = "Disable USB debugging in Settings > Developer options to prevent unauthorized access via USB connection."
    ),
    DEVELOPER_OPTIONS(
        displayName = "Developer Options",
        pointDeduction = 3,
        description = "Developer options should be disabled on production devices",
        recommendation = "Disable developer options in Settings > System > Developer options to reduce security risks."
    ),
    NOTIFICATION_LISTENER(
        displayName = "Notification Listener Access",
        pointDeduction = 7,
        description = "Apps with notification listener permission can read ALL notifications including 2FA codes and messages",
        recommendation = "Review and revoke notification access for non-essential apps in Settings > Apps > Special app access > Notification access."
    ),
    ACCESSIBILITY_SERVICE(
        displayName = "Accessibility Service Abuse",
        pointDeduction = 7,
        description = "Apps with accessibility services can control your entire device and potentially keylog",
        recommendation = "Disable accessibility services for apps that don't legitimately need them in Settings > Accessibility."
    ),
    DEVICE_ADMIN(
        displayName = "Device Administrator Apps",
        pointDeduction = 5,
        description = "Apps with device admin privileges have elevated control over your device",
        recommendation = "Review and remove device admin access for non-essential apps in Settings > Security > Device admin apps."
    ),

    // ===== NETWORK & TRACKING PRIVACY =====
    VPN_CONNECTION(
        displayName = "VPN Protection",
        pointDeduction = 5,
        description = "VPN should be active to protect network traffic",
        recommendation = "Enable a VPN connection to encrypt your internet traffic and protect your privacy online."
    ),
    ALWAYS_ON_VPN(
        displayName = "Always-On VPN",
        pointDeduction = 2,
        description = "When using VPN, Always-On VPN prevents accidental unencrypted connections",
        recommendation = "Enable Always-On VPN in Settings > Network & internet > VPN > [Your VPN] > Always-on VPN."
    ),
    PRIVATE_DNS(
        displayName = "Private DNS",
        pointDeduction = 7,
        description = "Private DNS (DNS over TLS/HTTPS) should be configured",
        recommendation = "Configure Private DNS in Settings > Network & internet > Private DNS. Recommended: dns.quad9.net (privacy-focused, blocks malware) or dns.adguard-dns.com (blocks ads and trackers)."
    ),
    ADVERTISING_ID(
        displayName = "Advertising ID",
        pointDeduction = 7,
        description = "Advertising ID enables cross-app tracking by advertisers",
        recommendation = "Disable or reset your Advertising ID in Settings > Privacy > Ads."
    ),
    WIFI_SCANNING(
        displayName = "Background Wi-Fi Scanning",
        pointDeduction = 2,
        description = "Background Wi-Fi/Bluetooth scanning can track your location",
        recommendation = "Disable Wi-Fi and Bluetooth scanning in Settings > Location > Wi-Fi and Bluetooth scanning to prevent location tracking."
    ),

    // ===== GOOGLE SERVICES =====
    FIND_MY_DEVICE(
        displayName = "Find My Device",
        pointDeduction = 2,
        description = "Google's Find My Device tracks device location (security feature but privacy cost)",
        recommendation = "Disable Find My Device in Settings > Security > Find My Device if privacy is priority over theft protection."
    ),

    // ===== DEFAULT APPS =====
    DEFAULT_BROWSER(
        displayName = "Default Browser",
        pointDeduction = 3,
        description = "Privacy-invasive browser detected as default",
        recommendation = "Switch to a privacy-respecting browser like Brave, Firefox, or DuckDuckGo Browser. Available on Google Play Store."
    ),
    DEFAULT_KEYBOARD(
        displayName = "Default Keyboard",
        pointDeduction = 3,
        description = "Privacy-invasive keyboard detected as default",
        recommendation = "Switch to FUTO Keyboard (Play Store) for best privacy + features with offline voice typing, or AnySoftKeyboard (F-Droid & Play Store) for a mature, highly customizable option."
    ),
    DEFAULT_SMS(
        displayName = "Default SMS/Messaging",
        pointDeduction = 2,
        description = "Privacy-invasive messaging app detected as default",
        recommendation = "Switch to Fossify Messages - available on F-Droid and Play Store. Open source, privacy-focused, no ads."
    ),
    DEFAULT_EMAIL(
        displayName = "Default Email",
        pointDeduction = 2,
        description = "Privacy-invasive email app detected as default",
        recommendation = "Switch to a privacy-respecting email client like K-9 Mail, FairEmail, or ProtonMail. Available on Google Play Store."
    ),
    DEFAULT_LAUNCHER(
        displayName = "Default Launcher",
        pointDeduction = 2,
        description = "Privacy-invasive launcher detected",
        recommendation = "Switch to a privacy-respecting launcher like Lawnchair, KISS Launcher, or Neo Launcher. Available on Google Play Store."
    ),

    // ===== GOOGLE APPS (MAJOR) =====
    GOOGLE_CHROME(
        displayName = "Chrome",
        pointDeduction = 1,
        description = "Google Chrome app is installed on device",
        recommendation = "Uninstall Chrome and switch to Brave, Firefox, or DuckDuckGo Browser."
    ),
    GMAIL_APP(
        displayName = "Gmail",
        pointDeduction = 1,
        description = "Gmail app is installed on device",
        recommendation = "Uninstall Gmail and switch to K-9 Mail, FairEmail, or ProtonMail."
    ),
    GOOGLE_MAPS(
        displayName = "Google Maps",
        pointDeduction = 1,
        description = "Google Maps app is installed on device",
        recommendation = "Uninstall Google Maps and switch to OsmAnd or Organic Maps."
    ),
    GOOGLE_PHOTOS(
        displayName = "Google Photos",
        pointDeduction = 1,
        description = "Google Photos app is installed on device",
        recommendation = "Uninstall Google Photos and switch to Simple Gallery or Aves Libre."
    ),
    GOOGLE_DRIVE(
        displayName = "Google Drive",
        pointDeduction = 1,
        description = "Google Drive app is installed on device",
        recommendation = "Uninstall Google Drive and switch to Nextcloud or Cryptomator."
    ),

    // ===== GOOGLE APPS (MINOR) =====
    YOUTUBE(
        displayName = "YouTube",
        pointDeduction = 0,
        description = "YouTube app is installed on device",
        recommendation = "Consider using NewPipe or LibreTube as privacy-friendly YouTube alternatives."
    ),
    GOOGLE_CALENDAR(
        displayName = "Google Calendar",
        pointDeduction = 0,
        description = "Google Calendar app is installed on device",
        recommendation = "Uninstall Google Calendar and switch to Simple Calendar or offline calendar apps."
    ),
    GOOGLE_KEEP(
        displayName = "Google Keep",
        pointDeduction = 0,
        description = "Google Keep app is installed on device",
        recommendation = "Uninstall Google Keep and switch to Simple Notes or Standard Notes."
    ),
    GOOGLE_CAMERA(
        displayName = "Google Camera",
        pointDeduction = 0,
        description = "Google Camera app is installed on device",
        recommendation = "Uninstall Google Camera and switch to Open Camera or Simple Camera."
    ),
    GOOGLE_DOCS(
        displayName = "Google Docs",
        pointDeduction = 0,
        description = "Google Docs app is installed on device",
        recommendation = "Uninstall Google Docs and switch to Collabora Office or offline editors."
    ),

    // ===== META/FACEBOOK APPS =====
    FACEBOOK_APP(
        displayName = "Facebook",
        pointDeduction = 1,
        description = "Facebook app is installed on device",
        recommendation = "Uninstall Facebook app and use the mobile website if needed, or quit Facebook entirely."
    ),
    INSTAGRAM_APP(
        displayName = "Instagram",
        pointDeduction = 1,
        description = "Instagram app is installed on device",
        recommendation = "Uninstall Instagram app and use the mobile website if needed, or consider alternatives."
    ),
    WHATSAPP_APP(
        displayName = "WhatsApp",
        pointDeduction = 1,
        description = "WhatsApp (owned by Meta) is installed on device",
        recommendation = "Uninstall WhatsApp and switch to Signal, Session, or SimpleX Chat."
    ),
    MESSENGER_APP(
        displayName = "Messenger",
        pointDeduction = 1,
        description = "Facebook Messenger app is installed on device",
        recommendation = "Uninstall Messenger and use Signal or other privacy-focused messaging apps."
    ),

    // ===== MICROSOFT APPS =====
    EDGE_APP(
        displayName = "Microsoft Edge",
        pointDeduction = 1,
        description = "Microsoft Edge app is installed on device",
        recommendation = "Uninstall Edge and switch to Brave, Firefox, or DuckDuckGo Browser."
    ),
    OUTLOOK_APP(
        displayName = "Outlook",
        pointDeduction = 1,
        description = "Microsoft Outlook app is installed on device",
        recommendation = "Uninstall Outlook and switch to K-9 Mail, FairEmail, or ProtonMail."
    ),
    ONEDRIVE_APP(
        displayName = "OneDrive",
        pointDeduction = 0,
        description = "Microsoft OneDrive app is installed on device",
        recommendation = "Uninstall OneDrive and switch to Nextcloud or Cryptomator."
    ),

    // ===== AMAZON APPS =====
    AMAZON_SHOPPING(
        displayName = "Amazon Shopping",
        pointDeduction = 1,
        description = "Amazon Shopping app is installed on device",
        recommendation = "Consider uninstalling and using the mobile website for better privacy."
    ),
    PRIME_VIDEO(
        displayName = "Prime Video",
        pointDeduction = 0,
        description = "Amazon Prime Video app is installed on device",
        recommendation = "Be aware that streaming apps collect viewing habits and device data."
    ),

    // ===== AI/LLM APPS =====
    CHATGPT_APP(
        displayName = "ChatGPT",
        pointDeduction = 1,
        description = "OpenAI ChatGPT app is installed on device",
        recommendation = "Be aware that AI apps send your conversations to cloud servers for processing."
    ),
    GOOGLE_GEMINI(
        displayName = "Google Gemini/Bard",
        pointDeduction = 1,
        description = "Google's AI assistant app is installed on device",
        recommendation = "Be aware that AI apps send your conversations to cloud servers for processing."
    ),
    MICROSOFT_COPILOT(
        displayName = "Microsoft Copilot",
        pointDeduction = 1,
        description = "Microsoft Copilot AI app is installed on device",
        recommendation = "Be aware that AI apps send your conversations to cloud servers for processing."
    ),
    CLAUDE_APP(
        displayName = "Claude",
        pointDeduction = 1,
        description = "Anthropic Claude app is installed on device",
        recommendation = "Be aware that AI apps send your conversations to cloud servers for processing."
    ),
    PERPLEXITY_APP(
        displayName = "Perplexity",
        pointDeduction = 0,
        description = "Perplexity AI app is installed on device",
        recommendation = "Be aware that AI apps send your searches to cloud servers for processing."
    ),
    META_AI(
        displayName = "Meta AI",
        pointDeduction = 1,
        description = "Meta AI app is installed on device",
        recommendation = "Be aware that AI apps send your conversations to cloud servers for processing."
    ),

    // ===== SOCIAL MEDIA =====
    TIKTOK_APP(
        displayName = "TikTok",
        pointDeduction = 1,
        description = "TikTok app is installed on device",
        recommendation = "Consider uninstalling TikTok due to extensive data collection practices."
    ),
    TWITTER_APP(
        displayName = "Twitter/X",
        pointDeduction = 1,
        description = "Twitter/X app is installed on device",
        recommendation = "Consider using the mobile website instead of the app for better privacy."
    ),
    REDDIT_APP(
        displayName = "Reddit",
        pointDeduction = 0,
        description = "Official Reddit app is installed on device",
        recommendation = "Consider using privacy-focused Reddit clients like Infinity or Slide."
    ),

    // ===== LEGACY (kept for compatibility) =====
    DEFAULT_ASSISTANT(
        displayName = "System Assistant/Search",
        pointDeduction = 0,
        description = "System assistant status",
        recommendation = "Disable Google Assistant/Alexa in Settings > Apps if enabled."
    )
}
