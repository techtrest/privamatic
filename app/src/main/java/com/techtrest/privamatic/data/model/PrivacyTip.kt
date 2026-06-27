package com.techtrest.privamatic.data.model

/**
 * Priority layers for privacy tips.
 * Selection follows ISSUE_AWARE → REINFORCING → EDUCATIONAL order.
 */
enum class TipLayer {
    /** Shown when the related PrivacyCheck is currently insecure. Most actionable. */
    ISSUE_AWARE,
    /** Shown when the related PrivacyCheck is secure. Reinforces good behavior. */
    REINFORCING,
    /** General privacy knowledge. Always available regardless of posture. */
    EDUCATIONAL
}

/**
 * A contextual privacy tip displayed in the Actions tab.
 *
 * @param id Unique identifier for history tracking
 * @param title Short headline for the tip
 * @param content Educational body text
 * @param layer Determines when this tip is eligible to show
 * @param relatedCheck Optional PrivacyCheck that gates visibility based on secure/insecure state
 */
data class PrivacyTip(
    val id: String,
    val title: String,
    val content: String,
    val layer: TipLayer,
    val relatedCheck: PrivacyCheck? = null
)

/**
 * All 45 privacy tips organized by layer.
 * Tips with a relatedCheck are contextual — they only show when the check
 * matches the expected state for their layer (insecure for ISSUE_AWARE, secure for REINFORCING).
 */
object PrivacyTips {

    // ===== LAYER 1: ISSUE_AWARE (show when related check is insecure) =====

    private val issueAwareTips = listOf(
        PrivacyTip(
            id = "usb_debugging",
            title = "Why USB Debugging Matters",
            content = "USB Debugging allows connected computers to access your device data. It\u2019s essential for development but best left off when you\u2019re not actively using it.",
            layer = TipLayer.ISSUE_AWARE,
            relatedCheck = PrivacyCheck.USB_DEBUGGING
        ),
        PrivacyTip(
            id = "developer_options",
            title = "Developer Options Exposure",
            content = "Developer Options are powerful tools for debugging, but they expose settings that can weaken security. Toggle it off when you don\u2019t need it \u2014 you can always re-enable it.",
            layer = TipLayer.ISSUE_AWARE,
            relatedCheck = PrivacyCheck.DEVELOPER_OPTIONS
        ),
        PrivacyTip(
            id = "no_vpn",
            title = "Browsing Without a VPN",
            content = "Without a VPN, your internet provider sees every domain you connect to. Some providers sell this data to advertisers. A VPN keeps your browsing between you and the VPN provider.",
            layer = TipLayer.ISSUE_AWARE,
            relatedCheck = PrivacyCheck.VPN_CONNECTION
        ),
        PrivacyTip(
            id = "ad_id",
            title = "Your Advertising ID",
            content = "Your Advertising ID lets ad networks build a profile across every app you use. Deleting it doesn\u2019t break any apps \u2014 they just lose the ability to connect your activity.",
            layer = TipLayer.ISSUE_AWARE,
            relatedCheck = PrivacyCheck.ADVERTISING_ID
        ),
        PrivacyTip(
            id = "no_private_dns",
            title = "Unencrypted DNS",
            content = "Without Private DNS, your DNS lookups are sent unencrypted to your provider. Enabling it takes 30 seconds and blocks known trackers across all your apps automatically.",
            layer = TipLayer.ISSUE_AWARE,
            relatedCheck = PrivacyCheck.PRIVATE_DNS
        ),
        PrivacyTip(
            id = "google_maps",
            title = "Google Maps Trade-off",
            content = "Google Maps is excellent, but it logs every search, route, and visit to your Google account. If you mainly need offline navigation, Organic Maps offers that without any tracking.",
            layer = TipLayer.ISSUE_AWARE,
            relatedCheck = PrivacyCheck.GOOGLE_MAPS
        ),
        PrivacyTip(
            id = "notification_listener",
            title = "Notification Access Risks",
            content = "Apps with notification access can read all your notifications \u2014 messages, banking alerts, verification codes. Worth reviewing whether each app truly needs this level of access.",
            layer = TipLayer.ISSUE_AWARE,
            relatedCheck = PrivacyCheck.NOTIFICATION_LISTENER
        ),
        PrivacyTip(
            id = "no_biometric",
            title = "Adding Biometrics",
            content = "Adding biometric authentication makes it faster to unlock your device, which means you\u2019re more likely to keep a strong PIN. Convenience and security working together.",
            layer = TipLayer.ISSUE_AWARE,
            relatedCheck = PrivacyCheck.BIOMETRIC_AUTH
        ),
        PrivacyTip(
            id = "wifi_scanning",
            title = "Background WiFi Scanning",
            content = "Background WiFi scanning lets Google collect nearby network names even when WiFi is off. Disabling it has no noticeable impact on daily use.",
            layer = TipLayer.ISSUE_AWARE,
            relatedCheck = PrivacyCheck.WIFI_SCANNING
        ),
        PrivacyTip(
            id = "find_my_device",
            title = "Find My Device Trade-off",
            content = "Find My Device is useful if your phone is lost, but it requires Google to know your device\u2019s location. If you\u2019re on GrapheneOS without Play Services, this isn\u2019t functional anyway.",
            layer = TipLayer.ISSUE_AWARE,
            relatedCheck = PrivacyCheck.FIND_MY_DEVICE
        ),
        PrivacyTip(
            id = "chrome_default",
            title = "Chrome as Default Browser",
            content = "Chrome syncs browsing data with your Google account by default. Firefox or Brave offer similar speed with better default privacy settings \u2014 and your bookmarks can be imported.",
            layer = TipLayer.ISSUE_AWARE,
            relatedCheck = PrivacyCheck.DEFAULT_BROWSER
        ),
        PrivacyTip(
            id = "gboard_default",
            title = "Keyboard Privacy",
            content = "Gboard sends typing data to Google to improve predictions. Privacy-focused keyboards like HeliBoard or FlorisBoard work offline with no data collection.",
            layer = TipLayer.ISSUE_AWARE,
            relatedCheck = PrivacyCheck.DEFAULT_KEYBOARD
        )
    )

    // ===== LAYER 2: REINFORCING (show when related check is secure) =====

    private val reinforcingTips = listOf(
        PrivacyTip(
            id = "screen_lock_good",
            title = "Your Screen Lock Strength",
            content = "Your screen lock is your first defense. A 6-digit PIN takes around 11 hours to brute force, while a 4-digit PIN takes just 7 minutes. Length matters more than complexity.",
            layer = TipLayer.REINFORCING,
            relatedCheck = PrivacyCheck.SCREEN_LOCK
        ),
        PrivacyTip(
            id = "encryption_good",
            title = "How Encryption Protects You",
            content = "With encryption enabled, your phone\u2019s data is unreadable without your PIN when powered off \u2014 even with physical access to the storage chip.",
            layer = TipLayer.REINFORCING,
            relatedCheck = PrivacyCheck.DEVICE_ENCRYPTION
        ),
        PrivacyTip(
            id = "biometric_good",
            title = "Your Biometric Security",
            content = "Your biometric data stays in your device\u2019s secure enclave. It never leaves the chip, isn\u2019t sent to any server, and can\u2019t be extracted even by the OS itself. For higher-risk situations — such as border crossings or legal proceedings — a strong PIN offers additional protection, as it cannot be physically compelled.",
            layer = TipLayer.REINFORCING,
            relatedCheck = PrivacyCheck.BIOMETRIC_AUTH
        ),
        PrivacyTip(
            id = "private_dns_good",
            title = "Your DNS Protection",
            content = "Your Private DNS works across every app \u2014 even ones that try to use their own tracking domains. It\u2019s one of the most effective single settings on your device.",
            layer = TipLayer.REINFORCING,
            relatedCheck = PrivacyCheck.PRIVATE_DNS
        ),
        PrivacyTip(
            id = "vpn_good",
            title = "Your VPN Protection",
            content = "Your VPN encrypts all traffic leaving your device. On public WiFi, others on the network only see encrypted data going to one server \u2014 nothing useful.",
            layer = TipLayer.REINFORCING,
            relatedCheck = PrivacyCheck.VPN_CONNECTION
        ),
        PrivacyTip(
            id = "ad_id_good",
            title = "Ad Tracking Blocked",
            content = "Without an Advertising ID, apps can still show ads, but they can\u2019t easily build a cross-app profile of your interests and behavior. The ads just become less targeted.",
            layer = TipLayer.REINFORCING,
            relatedCheck = PrivacyCheck.ADVERTISING_ID
        ),
        PrivacyTip(
            id = "no_google_apps",
            title = "Google-Free Benefits",
            content = "Running without Google apps means significantly less background data collection. Most Google services have capable alternatives \u2014 it\u2019s about finding what works for you.",
            layer = TipLayer.REINFORCING,
            relatedCheck = PrivacyCheck.GOOGLE_CHROME
        )
    )

    // ===== LAYER 3: EDUCATIONAL (always available) =====

    private val educationalTips = listOf(
        PrivacyTip(
            id = "photo_metadata",
            title = "Hidden Photo Data",
            content = "Photos contain hidden metadata \u2014 GPS coordinates, timestamp, device model. Before sharing sensitive photos, strip this data using apps like Scrambled Exif. It takes one tap.",
            layer = TipLayer.EDUCATIONAL
        ),
        PrivacyTip(
            id = "public_wifi",
            title = "Public WiFi Risks",
            content = "Public WiFi is easy to impersonate. Anyone can create a network called \u2018Airport_Free_WiFi\u2019. A VPN makes this a non-issue since all your traffic is encrypted regardless.",
            layer = TipLayer.EDUCATIONAL
        ),
        PrivacyTip(
            id = "browser_fingerprinting",
            title = "Browser Fingerprinting",
            content = "Browser fingerprinting identifies you by combining your screen size, fonts, timezone, and GPU into a unique signature. This works even without cookies. Firefox has the best built-in protections.",
            layer = TipLayer.EDUCATIONAL
        ),
        PrivacyTip(
            id = "sms_2fa",
            title = "SMS Two-Factor Weakness",
            content = "SMS-based two-factor authentication is better than nothing, but vulnerable to SIM swap attacks. App-based authenticators like Aegis or Google Authenticator are significantly safer.",
            layer = TipLayer.EDUCATIONAL
        ),
        PrivacyTip(
            id = "free_vpns",
            title = "The Cost of Free VPNs",
            content = "Free VPNs need revenue \u2014 most sell your browsing data or inject ads. Privacy-respecting VPNs like Mullvad or Proton VPN cost a few euros per month and have audited no-log policies.",
            layer = TipLayer.EDUCATIONAL
        ),
        PrivacyTip(
            id = "wifi_probes",
            title = "WiFi Probe Requests",
            content = "Your phone broadcasts WiFi probe requests \u2014 searching for known networks by name. This can reveal places you\u2019ve been. Disabling auto-join for old networks reduces this leakage.",
            layer = TipLayer.EDUCATIONAL
        ),
        PrivacyTip(
            id = "clipboard",
            title = "Clipboard Privacy",
            content = "Clipboard data is accessible to any foreground app on older Android versions. Use your password manager\u2019s autofill instead of copying passwords \u2014 it\u2019s both safer and faster.",
            layer = TipLayer.EDUCATIONAL
        ),
        PrivacyTip(
            id = "device_name",
            title = "Your Device Name",
            content = "Your device name is visible to anyone nearby scanning Bluetooth or WiFi Direct. Setting it to something generic like \u2018Phone\u2019 prevents it from identifying you personally.",
            layer = TipLayer.EDUCATIONAL
        ),
        PrivacyTip(
            id = "delete_account_first",
            title = "Uninstall Isn\u2019t Enough",
            content = "Deleting an app removes it from your device but not from the company\u2019s servers. Use \u2018Delete Account\u2019 in the app first, then uninstall. GDPR gives EU residents the legal right to request deletion.",
            layer = TipLayer.EDUCATIONAL
        ),
        PrivacyTip(
            id = "email_privacy",
            title = "Email Isn\u2019t Private",
            content = "Email is not private by default \u2014 it\u2019s sent in plain text between servers. For sensitive communication, use Signal or encrypted email like Proton Mail. Regular email is fine for newsletters.",
            layer = TipLayer.EDUCATIONAL
        ),
        PrivacyTip(
            id = "data_brokers",
            title = "Data Brokers",
            content = "Data brokers compile profiles from public records, purchases, and app data, then sell them. Services like Mine or yourdigitalrights.org help you discover and request deletion of your data.",
            layer = TipLayer.EDUCATIONAL
        ),
        PrivacyTip(
            id = "incognito_myth",
            title = "Incognito Mode Limits",
            content = "Incognito mode only hides browsing from other people using your device. Your ISP, employer, and the websites themselves still see everything. A VPN addresses the ISP part.",
            layer = TipLayer.EDUCATIONAL
        ),
        PrivacyTip(
            id = "permission_review",
            title = "Revoke Unused Permissions",
            content = "App permissions on Android are revocable at any time. Granting camera access to scan a QR code doesn\u2019t mean the app needs it forever \u2014 review and revoke after one-time use.",
            layer = TipLayer.EDUCATIONAL
        ),
        PrivacyTip(
            id = "search_engines",
            title = "Private Search Engines",
            content = "Search engines log every query tied to your account or IP. DuckDuckGo and Startpage provide Google-quality results without building a search profile. You can always fall back to Google for specific queries.",
            layer = TipLayer.EDUCATIONAL
        ),
        PrivacyTip(
            id = "email_trackers",
            title = "Email Tracking Pixels",
            content = "Email trackers \u2014 invisible pixels embedded in marketing emails \u2014 report when and where you opened them. Disabling \u2018auto-load images\u2019 in your email app blocks most of these.",
            layer = TipLayer.EDUCATIONAL
        ),
        PrivacyTip(
            id = "cloud_backups",
            title = "Cloud Backup Trade-offs",
            content = "Cloud backups are convenient but mean your data exists on someone else\u2019s server. If you use cloud backup, ensure it\u2019s encrypted. GrapheneOS\u2019s Seedvault offers encrypted local backups.",
            layer = TipLayer.EDUCATIONAL
        ),
        PrivacyTip(
            id = "location_uniqueness",
            title = "Location Data Sensitivity",
            content = "Location data is among the most sensitive information your phone generates. Even anonymized location datasets have been de-anonymized \u2014 the path between your home and work is unique to you.",
            layer = TipLayer.EDUCATIONAL
        ),
        PrivacyTip(
            id = "app_updates",
            title = "Keep Apps Updated",
            content = "App updates aren\u2019t just about new features \u2014 they patch security vulnerabilities. Keeping apps updated is one of the simplest and most effective security practices.",
            layer = TipLayer.EDUCATIONAL
        ),
        PrivacyTip(
            id = "qr_codes",
            title = "QR Code Awareness",
            content = "QR codes can link to malicious websites just like any URL. Before scanning unknown QR codes in public places, consider that someone may have placed a sticker over the original.",
            layer = TipLayer.EDUCATIONAL
        ),
        PrivacyTip(
            id = "phone_sensors",
            title = "Sensor Access",
            content = "Your phone\u2019s sensors \u2014 accelerometer, gyroscope, barometer \u2014 can be accessed by any app without special permission. Research has shown these can be used for crude keystroke detection.",
            layer = TipLayer.EDUCATIONAL
        ),
        PrivacyTip(
            id = "email_aliases",
            title = "Email Aliases",
            content = "Using email aliases (like SimpleLogin or addy.io) means each service gets a unique address. If one gets breached or sold, you disable that alias instead of changing your actual email everywhere.",
            layer = TipLayer.EDUCATIONAL
        ),
        PrivacyTip(
            id = "auto_revoke",
            title = "Auto-Revoke Permissions",
            content = "Android\u2019s auto-revoke automatically removes permissions from apps you haven\u2019t used in months. This is on by default since Android 11 \u2014 one of the better automatic privacy features.",
            layer = TipLayer.EDUCATIONAL
        ),
        PrivacyTip(
            id = "payment_tracking",
            title = "Payment Privacy",
            content = "Paying with a card creates a permanent record of where you were and what you bought. Cash or privacy-focused payment methods break this tracking chain for sensitive purchases.",
            layer = TipLayer.EDUCATIONAL
        ),
        PrivacyTip(
            id = "contacts_upload",
            title = "Contacts Access Risks",
            content = "Social media apps often request contacts access during setup. This uploads your entire address book \u2014 including phone numbers of people who never consented to sharing their data.",
            layer = TipLayer.EDUCATIONAL
        ),
        PrivacyTip(
            id = "smart_home",
            title = "Smart Home Privacy",
            content = "Smart home devices process some audio in the cloud. If you use them, review and regularly delete stored voice recordings in the manufacturer\u2019s app.",
            layer = TipLayer.EDUCATIONAL
        ),
        PrivacyTip(
            id = "router_dns",
            title = "Router-Level DNS",
            content = "Your router\u2019s DNS settings affect every device on your network. Setting your router\u2019s DNS to a privacy-respecting provider (Quad9, NextDNS) protects all devices at once \u2014 including smart TVs and IoT.",
            layer = TipLayer.EDUCATIONAL
        )
    )

    /** All tips across all layers. */
    val all: List<PrivacyTip> = issueAwareTips + reinforcingTips + educationalTips
}
