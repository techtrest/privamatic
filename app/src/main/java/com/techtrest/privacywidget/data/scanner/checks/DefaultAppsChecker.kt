package com.techtrest.privacywidget.data.scanner.checks

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.provider.Telephony
import android.util.Log
import android.view.inputmethod.InputMethodManager
import com.techtrest.privacywidget.data.model.PrivacyCheck
import com.techtrest.privacywidget.data.model.PrivacyIssue

class DefaultAppsChecker(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    init {
        // Log all installed packages at initialization for debugging
        logAllInstalledPackages()
    }

    private fun logAllInstalledPackages() {
        try {
            Log.d(TAG, "=== LISTING ALL INSTALLED PACKAGES ===")
            val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            Log.d(TAG, "Total installed applications: ${installedApps.size}")

            // Log all Google packages
            val googlePackages = installedApps.filter {
                it.packageName.contains("google", ignoreCase = true)
            }.sortedBy { it.packageName }

            Log.d(TAG, "=== GOOGLE PACKAGES (${googlePackages.size}) ===")
            googlePackages.forEach { app ->
                val appName = packageManager.getApplicationLabel(app).toString()
                Log.d(TAG, "  ${app.packageName} | $appName")
            }

            // Log all packages containing "maps"
            val mapsPackages = installedApps.filter {
                it.packageName.contains("maps", ignoreCase = true)
            }
            Log.d(TAG, "=== MAPS-RELATED PACKAGES (${mapsPackages.size}) ===")
            mapsPackages.forEach { app ->
                val appName = packageManager.getApplicationLabel(app).toString()
                Log.d(TAG, "  ${app.packageName} | $appName")
            }

            // Log all packages containing "camera"
            val cameraPackages = installedApps.filter {
                it.packageName.contains("camera", ignoreCase = true)
            }
            Log.d(TAG, "=== CAMERA-RELATED PACKAGES (${cameraPackages.size}) ===")
            cameraPackages.forEach { app ->
                val appName = packageManager.getApplicationLabel(app).toString()
                Log.d(TAG, "  ${app.packageName} | $appName")
            }

            // Log all packages containing "photo"
            val photoPackages = installedApps.filter {
                it.packageName.contains("photo", ignoreCase = true)
            }
            Log.d(TAG, "=== PHOTO-RELATED PACKAGES (${photoPackages.size}) ===")
            photoPackages.forEach { app ->
                val appName = packageManager.getApplicationLabel(app).toString()
                Log.d(TAG, "  ${app.packageName} | $appName")
            }

            Log.d(TAG, "=== END PACKAGE LIST ===")
        } catch (e: Exception) {
            Log.e(TAG, "Error logging installed packages", e)
        }
    }

    /**
     * Check default browser app with three-tier detection
     * Privacy-invasive: Chrome, Edge, Opera, UC Browser (-3), Samsung Internet (-2)
     * Privacy-friendly: Brave, Firefox, DuckDuckGo, etc. (0)
     * Unknown: Everything else (0, displayed as unknown)
     */
    fun checkDefaultBrowser(): PrivacyIssue {
        return try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.example.com"))

            // Try MATCH_DEFAULT_ONLY first
            var resolveInfo = packageManager.resolveActivity(browserIntent, PackageManager.MATCH_DEFAULT_ONLY)
            var packageName = resolveInfo?.activityInfo?.packageName

            Log.d(TAG, "Default browser (MATCH_DEFAULT_ONLY): $packageName")

            // If no default, try without the flag to see what's available
            if (packageName == null || packageName == "android") {
                resolveInfo = packageManager.resolveActivity(browserIntent, 0)
                val fallbackPackage = resolveInfo?.activityInfo?.packageName
                Log.d(TAG, "Fallback browser resolver: $fallbackPackage")

                // If we got a real app (not the chooser), use it
                if (fallbackPackage != null && fallbackPackage != "android") {
                    packageName = fallbackPackage
                }
            }

            val finalPackage = packageName ?: "none"
            Log.d(TAG, "Final browser package: $finalPackage")

            // Privacy-invasive browsers
            val invasive = when {
                finalPackage.contains("chrome", ignoreCase = true) -> Triple(3, "Chrome", false)
                finalPackage.contains("edge", ignoreCase = true) && finalPackage.contains("microsoft") -> Triple(3, "Microsoft Edge", false)
                finalPackage.contains("opera", ignoreCase = true) -> Triple(3, "Opera", false)
                finalPackage.contains("ucbrowser", ignoreCase = true) || finalPackage.contains("uc.browser", ignoreCase = true) -> Triple(3, "UC Browser", false)
                finalPackage.contains("sec.android.app.sbrowser", ignoreCase = true) -> Triple(2, "Samsung Internet", false)
                else -> null
            }

            if (invasive != null) {
                val (points, name, _) = invasive
                return PrivacyIssue(
                    check = PrivacyCheck.DEFAULT_BROWSER,
                    isSecure = false,
                    currentStatus = "Using $name",
                    technicalDetails = "Package: $finalPackage",
                    customPointDeduction = points
                )
            }

            // Privacy-friendly browsers
            val friendly = when {
                finalPackage.contains("brave", ignoreCase = true) -> "Brave"
                finalPackage.contains("firefox", ignoreCase = true) -> "Firefox"
                finalPackage.contains("focus", ignoreCase = true) -> "Firefox Focus"
                finalPackage.contains("duckduckgo", ignoreCase = true) -> "DuckDuckGo Browser"
                finalPackage.contains("vanadium", ignoreCase = true) -> "Vanadium"
                finalPackage.contains("cromite", ignoreCase = true) -> "Cromite"
                finalPackage.contains("mull", ignoreCase = true) -> "Mull"
                finalPackage.contains("tor", ignoreCase = true) && finalPackage.contains("browser") -> "Tor Browser"
                else -> null
            }

            if (friendly != null) {
                return PrivacyIssue(
                    check = PrivacyCheck.DEFAULT_BROWSER,
                    isSecure = true,
                    currentStatus = "Using $friendly",
                    technicalDetails = "Package: $finalPackage",
                    customPointDeduction = 0
                )
            }

            // No default or unknown browser
            if (finalPackage == "android" || finalPackage == "none") {
                PrivacyIssue(
                    check = PrivacyCheck.DEFAULT_BROWSER,
                    isSecure = true,
                    currentStatus = "No default browser set",
                    technicalDetails = "Package: $finalPackage",
                    customPointDeduction = 0
                )
            } else {
                // Unknown browser - don't penalize
                val appName = getAppName(finalPackage)
                PrivacyIssue(
                    check = PrivacyCheck.DEFAULT_BROWSER,
                    isSecure = true,
                    currentStatus = "Using $appName (unknown, 0 pts)",
                    technicalDetails = "Package: $finalPackage",
                    customPointDeduction = 0
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking default browser", e)
            PrivacyIssue(
                check = PrivacyCheck.DEFAULT_BROWSER,
                isSecure = true,
                currentStatus = "Unable to determine default browser",
                technicalDetails = "Error: ${e.message}",
                customPointDeduction = 0
            )
        }
    }

    /**
     * Check default SMS/Messaging app with three-tier detection
     * Privacy-invasive: Google Messages (-2), Facebook Messenger (-3), WhatsApp (-3), Samsung Messages (-2)
     * Privacy-friendly: Signal, QKSMS, etc. (0)
     * Unknown: Everything else (0, displayed as unknown)
     */
    fun checkDefaultSms(): PrivacyIssue {
        return try {
            val defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(context) ?: "none"
            Log.d(TAG, "Default SMS package: $defaultSmsPackage")

            // Privacy-invasive messaging apps
            val invasive = when {
                defaultSmsPackage.contains("google.android.apps.messaging", ignoreCase = true) -> Triple(2, "Google Messages", false)
                defaultSmsPackage.contains("facebook.orca", ignoreCase = true) -> Triple(3, "Facebook Messenger", false)
                defaultSmsPackage.contains("whatsapp", ignoreCase = true) -> Triple(3, "WhatsApp", false)
                defaultSmsPackage.contains("sec.android.messaging", ignoreCase = true) -> Triple(2, "Samsung Messages", false)
                else -> null
            }

            if (invasive != null) {
                val (points, name, _) = invasive
                return PrivacyIssue(
                    check = PrivacyCheck.DEFAULT_SMS,
                    isSecure = false,
                    currentStatus = "Using $name",
                    technicalDetails = "Package: $defaultSmsPackage",
                    customPointDeduction = points
                )
            }

            // Privacy-friendly messaging apps
            val friendly = when {
                defaultSmsPackage.contains("fossify", ignoreCase = true) && defaultSmsPackage.contains("messages", ignoreCase = true) -> "Fossify Messages"
                defaultSmsPackage.contains("signal", ignoreCase = true) || defaultSmsPackage.contains("securesms", ignoreCase = true) -> "Signal"
                defaultSmsPackage.contains("molly", ignoreCase = true) -> "Molly"
                defaultSmsPackage.contains("asms", ignoreCase = true) -> "aSMS"
                defaultSmsPackage.contains("qksms", ignoreCase = true) -> "QKSMS"
                defaultSmsPackage.contains("simplex", ignoreCase = true) -> "SimpleX Chat"
                defaultSmsPackage.contains("partisan", ignoreCase = true) -> "Partisan SMS"
                defaultSmsPackage.contains("silence", ignoreCase = true) -> "Silence"
                else -> null
            }

            if (friendly != null) {
                return PrivacyIssue(
                    check = PrivacyCheck.DEFAULT_SMS,
                    isSecure = true,
                    currentStatus = "Using $friendly",
                    technicalDetails = "Package: $defaultSmsPackage",
                    customPointDeduction = 0
                )
            }

            // No default or unknown
            if (defaultSmsPackage == "none") {
                PrivacyIssue(
                    check = PrivacyCheck.DEFAULT_SMS,
                    isSecure = true,
                    currentStatus = "No default SMS app set",
                    technicalDetails = "Package: $defaultSmsPackage",
                    customPointDeduction = 0
                )
            } else {
                val appName = getAppName(defaultSmsPackage)
                PrivacyIssue(
                    check = PrivacyCheck.DEFAULT_SMS,
                    isSecure = true,
                    currentStatus = "Using $appName (unknown, 0 pts)",
                    technicalDetails = "Package: $defaultSmsPackage",
                    customPointDeduction = 0
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking default SMS", e)
            PrivacyIssue(
                check = PrivacyCheck.DEFAULT_SMS,
                isSecure = true,
                currentStatus = "Unable to determine default SMS app",
                technicalDetails = "Error: ${e.message}",
                customPointDeduction = 0
            )
        }
    }

    /**
     * Check default keyboard with allowlist approach
     * Privacy-friendly (allowlist): OpenBoard, FlorisBoard, AnySoftKeyboard, HeliBoard, Simple Keyboard, FUTO, Unexpected Keyboard (0 pts)
     * Everything else: Insecure (-3 pts)
     *
     * This catches ALL non-privacy keyboards including: Gboard, SwiftKey, Samsung, Xiaomi, Huawei, OnePlus, Oppo, Vivo, and any OEM keyboard
     */
    fun checkDefaultKeyboard(): PrivacyIssue {
        return try {
            val currentKeyboard = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: "none"
            Log.d(TAG, "Default keyboard IME: $currentKeyboard")

            // Privacy-friendly keyboards (allowlist)
            val friendly = when {
                currentKeyboard.contains("florisboard", ignoreCase = true) || currentKeyboard.contains("dev.patrickgold.florisboard", ignoreCase = true) -> "FlorisBoard"
                currentKeyboard.contains("heliboard", ignoreCase = true) || currentKeyboard.contains("helium314.keyboard", ignoreCase = true) -> "HeliBoard"
                currentKeyboard.contains("anysoftkeyboard", ignoreCase = true) -> "AnySoftKeyboard"
                currentKeyboard.contains("simplekeyboard", ignoreCase = true) || currentKeyboard.contains("rkr.simplekeyboard.inputmethod", ignoreCase = true) -> "Simple Keyboard"
                currentKeyboard.contains("futo", ignoreCase = true) || currentKeyboard.contains("org.futo.inputmethod.latin", ignoreCase = true) -> "FUTO Keyboard"
                currentKeyboard.contains("unexpected", ignoreCase = true) && currentKeyboard.contains("keyboard") || currentKeyboard.contains("juloo.keyboard2", ignoreCase = true) -> "Unexpected Keyboard"
                currentKeyboard.contains("openboard", ignoreCase = true) || currentKeyboard.contains("org.dslul.openboard.inputmethod.latin", ignoreCase = true) -> "OpenBoard"
                else -> null
            }

            if (friendly != null) {
                return PrivacyIssue(
                    check = PrivacyCheck.DEFAULT_KEYBOARD,
                    isSecure = true,
                    currentStatus = "Using $friendly",
                    technicalDetails = "IME: $currentKeyboard",
                    customPointDeduction = 0
                )
            }

            // No default keyboard
            if (currentKeyboard == "none") {
                return PrivacyIssue(
                    check = PrivacyCheck.DEFAULT_KEYBOARD,
                    isSecure = true,
                    currentStatus = "No default keyboard set",
                    technicalDetails = "IME: $currentKeyboard",
                    customPointDeduction = 0
                )
            }

            // Everything else is non-privacy-friendly (including Gboard, SwiftKey, Samsung, Xiaomi, Huawei, etc.)
            val appName = getAppName(currentKeyboard.substringBefore('/'))
            return PrivacyIssue(
                check = PrivacyCheck.DEFAULT_KEYBOARD,
                isSecure = false,
                currentStatus = "Using $appName",
                technicalDetails = "IME: $currentKeyboard",
                customPointDeduction = 3
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking default keyboard", e)
            PrivacyIssue(
                check = PrivacyCheck.DEFAULT_KEYBOARD,
                isSecure = true,
                currentStatus = "Unable to determine default keyboard",
                technicalDetails = "Error: ${e.message}",
                customPointDeduction = 0
            )
        }
    }

    /**
     * Check default email app with three-tier detection
     * Privacy-invasive: Gmail (-2), Outlook (-2), Yahoo Mail (-2), Samsung Email (-1)
     * Privacy-friendly: K-9 Mail, FairEmail, ProtonMail, etc. (0)
     * Unknown: Everything else (0, displayed as unknown)
     */
    fun checkDefaultEmail(): PrivacyIssue {
        return try {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
            }

            val resolveInfo = packageManager.resolveActivity(emailIntent, PackageManager.MATCH_DEFAULT_ONLY)
            val packageName = resolveInfo?.activityInfo?.packageName ?: "none"

            Log.d(TAG, "Default email package: $packageName")

            // Privacy-invasive email apps
            val invasive = when {
                packageName.contains("google.android.gm", ignoreCase = true) -> Triple(2, "Gmail", false)
                packageName.contains("microsoft.office.outlook", ignoreCase = true) -> Triple(2, "Outlook", false)
                packageName.contains("yahoo.mobile", ignoreCase = true) -> Triple(2, "Yahoo Mail", false)
                packageName.contains("sec.android.email", ignoreCase = true) -> Triple(1, "Samsung Email", false)
                else -> null
            }

            if (invasive != null) {
                val (points, name, _) = invasive
                return PrivacyIssue(
                    check = PrivacyCheck.DEFAULT_EMAIL,
                    isSecure = false,
                    currentStatus = "Using $name",
                    technicalDetails = "Package: $packageName",
                    customPointDeduction = points
                )
            }

            // Privacy-friendly email apps
            val friendly = when {
                packageName.contains("fsck.k9", ignoreCase = true) -> "K-9 Mail"
                packageName.contains("faircode.email", ignoreCase = true) -> "FairEmail"
                packageName.contains("protonmail", ignoreCase = true) -> "ProtonMail"
                packageName.contains("tutanota", ignoreCase = true) -> "Tutanota"
                packageName.contains("simple.mail", ignoreCase = true) -> "Simple Mail"
                else -> null
            }

            if (friendly != null) {
                return PrivacyIssue(
                    check = PrivacyCheck.DEFAULT_EMAIL,
                    isSecure = true,
                    currentStatus = "Using $friendly",
                    technicalDetails = "Package: $packageName",
                    customPointDeduction = 0
                )
            }

            // No default or unknown
            if (packageName == "android" || packageName == "none") {
                PrivacyIssue(
                    check = PrivacyCheck.DEFAULT_EMAIL,
                    isSecure = true,
                    currentStatus = "No default email app set",
                    technicalDetails = "Package: $packageName",
                    customPointDeduction = 0
                )
            } else {
                val appName = getAppName(packageName)
                PrivacyIssue(
                    check = PrivacyCheck.DEFAULT_EMAIL,
                    isSecure = true,
                    currentStatus = "Using $appName (unknown, 0 pts)",
                    technicalDetails = "Package: $packageName",
                    customPointDeduction = 0
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking default email", e)
            PrivacyIssue(
                check = PrivacyCheck.DEFAULT_EMAIL,
                isSecure = true,
                currentStatus = "Unable to determine default email app",
                technicalDetails = "Error: ${e.message}",
                customPointDeduction = 0
            )
        }
    }

    /**
     * Check default launcher with three-tier detection
     * Privacy-invasive: Nova Launcher (-2), Microsoft Launcher (-2), Samsung/Xiaomi with ads (-2)
     * Privacy-friendly: Lawnchair, KISS Launcher, etc. (0)
     * Unknown: Everything else (0, displayed as unknown)
     */
    fun checkDefaultLauncher(): PrivacyIssue {
        return try {
            val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }

            val resolveInfo = packageManager.resolveActivity(launcherIntent, PackageManager.MATCH_DEFAULT_ONLY)
            val packageName = resolveInfo?.activityInfo?.packageName ?: "none"

            Log.d(TAG, "Default launcher package: $packageName")

            // Privacy-invasive launchers
            val invasive = when {
                packageName.contains("teslacoilsw.launcher", ignoreCase = true) -> Triple(2, "Nova Launcher", false)
                packageName.contains("microsoft.launcher", ignoreCase = true) -> Triple(2, "Microsoft Launcher", false)
                packageName.contains("sec.android.app.launcher", ignoreCase = true) -> Triple(2, "Samsung Launcher", false)
                packageName.contains("miui.home", ignoreCase = true) -> Triple(2, "Xiaomi Launcher", false)
                else -> null
            }

            if (invasive != null) {
                val (points, name, _) = invasive
                return PrivacyIssue(
                    check = PrivacyCheck.DEFAULT_LAUNCHER,
                    isSecure = false,
                    currentStatus = "Using $name",
                    technicalDetails = "Package: $packageName",
                    customPointDeduction = points
                )
            }

            // Privacy-friendly launchers
            val friendly = when {
                packageName.contains("lawnchair", ignoreCase = true) -> "Lawnchair"
                packageName.contains("kiss", ignoreCase = true) && packageName.contains("launcher") -> "KISS Launcher"
                packageName.contains("niagara", ignoreCase = true) -> "Niagara Launcher"
                packageName.contains("neolauncher", ignoreCase = true) -> "Neo Launcher"
                packageName.contains("olauncher", ignoreCase = true) -> "Olauncher"
                packageName.contains("grapheneos", ignoreCase = true) && packageName.contains("launcher") -> "GrapheneOS Launcher"
                packageName.contains("launcher3", ignoreCase = true) && packageName.contains("android") -> "AOSP Launcher"
                else -> null
            }

            if (friendly != null) {
                return PrivacyIssue(
                    check = PrivacyCheck.DEFAULT_LAUNCHER,
                    isSecure = true,
                    currentStatus = "Using $friendly",
                    technicalDetails = "Package: $packageName",
                    customPointDeduction = 0
                )
            }

            // No default or unknown
            if (packageName == "android" || packageName == "none") {
                PrivacyIssue(
                    check = PrivacyCheck.DEFAULT_LAUNCHER,
                    isSecure = true,
                    currentStatus = "No default launcher set",
                    technicalDetails = "Package: $packageName",
                    customPointDeduction = 0
                )
            } else {
                val appName = getAppName(packageName)
                PrivacyIssue(
                    check = PrivacyCheck.DEFAULT_LAUNCHER,
                    isSecure = true,
                    currentStatus = "Using $appName (unknown, 0 pts)",
                    technicalDetails = "Package: $packageName",
                    customPointDeduction = 0
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking default launcher", e)
            PrivacyIssue(
                check = PrivacyCheck.DEFAULT_LAUNCHER,
                isSecure = true,
                currentStatus = "Unable to determine default launcher",
                technicalDetails = "Error: ${e.message}",
                customPointDeduction = 0
            )
        }
    }

    /**
     * Check if Gmail app is installed
     * Gmail installed: -10 points
     * Not installed: 0 points
     */
    fun checkGmailInstalled(): PrivacyIssue {
        return try {
            val isInstalled = isAppInstalled("com.google.android.gm")

            PrivacyIssue(
                check = PrivacyCheck.GMAIL_APP,
                isSecure = !isInstalled,
                currentStatus = if (isInstalled) "Installed" else "Not installed",
                technicalDetails = "Package: com.google.android.gm",
                customPointDeduction = if (isInstalled) 10 else 0
            )
        } catch (e: Exception) {
            PrivacyIssue(
                check = PrivacyCheck.GMAIL_APP,
                isSecure = true,
                currentStatus = "Unable to determine",
                technicalDetails = "Error: ${e.message}",
                customPointDeduction = 0
            )
        }
    }

    /**
     * Check if Google Photos is installed
     * Google Photos installed: -10 points
     * Not installed: 0 points
     */
    fun checkGooglePhotosInstalled(): PrivacyIssue {
        return try {
            val isInstalled = isAppInstalled("com.google.android.apps.photos")

            PrivacyIssue(
                check = PrivacyCheck.GOOGLE_PHOTOS,
                isSecure = !isInstalled,
                currentStatus = if (isInstalled) "Installed" else "Not installed",
                technicalDetails = "Package: com.google.android.apps.photos",
                customPointDeduction = if (isInstalled) 10 else 0
            )
        } catch (e: Exception) {
            PrivacyIssue(
                check = PrivacyCheck.GOOGLE_PHOTOS,
                isSecure = true,
                currentStatus = "Unable to determine",
                technicalDetails = "Error: ${e.message}",
                customPointDeduction = 0
            )
        }
    }

    /**
     * Check if Google Camera is installed
     * Google Camera installed: -5 points
     * Not installed: 0 points
     */
    fun checkGoogleCameraInstalled(): PrivacyIssue {
        return try {
            val isInstalled = isAppInstalled("com.google.android.GoogleCamera")

            PrivacyIssue(
                check = PrivacyCheck.GOOGLE_CAMERA,
                isSecure = !isInstalled,
                currentStatus = if (isInstalled) "Installed" else "Not installed",
                technicalDetails = "Package: com.google.android.GoogleCamera",
                customPointDeduction = if (isInstalled) 5 else 0
            )
        } catch (e: Exception) {
            PrivacyIssue(
                check = PrivacyCheck.GOOGLE_CAMERA,
                isSecure = true,
                currentStatus = "Unable to determine",
                technicalDetails = "Error: ${e.message}",
                customPointDeduction = 0
            )
        }
    }

    /**
     * Check if Google Maps is installed
     * Google Maps installed: -10 points
     * Not installed: 0 points
     */
    fun checkGoogleMapsInstalled(): PrivacyIssue {
        return try {
            val isInstalled = isAppInstalled("com.google.android.apps.maps")

            PrivacyIssue(
                check = PrivacyCheck.GOOGLE_MAPS,
                isSecure = !isInstalled,
                currentStatus = if (isInstalled) "Installed" else "Not installed",
                technicalDetails = "Package: com.google.android.apps.maps",
                customPointDeduction = if (isInstalled) 10 else 0
            )
        } catch (e: Exception) {
            PrivacyIssue(
                check = PrivacyCheck.GOOGLE_MAPS,
                isSecure = true,
                currentStatus = "Unable to determine",
                technicalDetails = "Error: ${e.message}",
                customPointDeduction = 0
            )
        }
    }

    /**
     * Check system assistant/search
     * Google Assistant, Alexa: -10 points
     * Everything else: 0 points
     */
    fun checkDefaultAssistant(): PrivacyIssue {
        return try {
            val assistantPackage = Settings.Secure.getString(context.contentResolver, "assistant") ?:
                                   Settings.Secure.getString(context.contentResolver, "voice_interaction_service") ?: "none"
            Log.d(TAG, "Default assistant package: $assistantPackage")

            val (pointDeduction, appName, isSecure) = when {
                assistantPackage.contains("google", ignoreCase = true) -> Triple(10, "Google Assistant", false)
                assistantPackage.contains("alexa", ignoreCase = true) -> Triple(10, "Amazon Alexa", false)
                assistantPackage == "none" -> Triple(0, "No assistant configured", true)
                else -> Triple(0, "Assistant: ${getAppName(assistantPackage)}", true)
            }

            Log.d(TAG, "Assistant check result: $appName (${if (isSecure) "secure" else "insecure"}, -$pointDeduction pts)")

            PrivacyIssue(
                check = PrivacyCheck.DEFAULT_ASSISTANT,
                isSecure = isSecure,
                currentStatus = appName,
                technicalDetails = "Package: $assistantPackage",
                customPointDeduction = pointDeduction
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking default assistant", e)
            PrivacyIssue(
                check = PrivacyCheck.DEFAULT_ASSISTANT,
                isSecure = true,
                currentStatus = "Unable to determine assistant app",
                technicalDetails = "Error: ${e.message}",
                customPointDeduction = 0
            )
        }
    }

    /**
     * Helper function to check if an app is installed AND enabled
     * Only returns true if the app is both installed and enabled
     * Uses multiple detection methods for compatibility with GrapheneOS and other custom ROMs
     */
    private fun isAppInstalled(packageName: String): Boolean {
        Log.d(TAG, "")
        Log.d(TAG, "============================================")
        Log.d(TAG, "SEARCHING FOR PACKAGE: '$packageName'")
        Log.d(TAG, "============================================")

        // Method 1: Try getPackageInfo with GET_META_DATA
        try {
            @Suppress("DEPRECATION")
            val pkgInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA)
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val isEnabled = appInfo.enabled

            Log.d(TAG, "✓ Package '$packageName' found via getPackageInfo")
            Log.d(TAG, "  Version: ${pkgInfo.versionName} (${pkgInfo.versionCode})")
            Log.d(TAG, "  Enabled: $isEnabled")

            if (!isEnabled) {
                Log.d(TAG, "⚠ Package is DISABLED - not counting as installed")
                return false
            }
            return true
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d(TAG, "✗ Method 1 (getPackageInfo): Package '$packageName' not found")
        } catch (e: Exception) {
            Log.w(TAG, "✗ Method 1 (getPackageInfo): Error - ${e.message}")
        }

        // Method 2: Try getApplicationInfo
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val isEnabled = appInfo.enabled

            Log.d(TAG, "✓ Package '$packageName' found via getApplicationInfo")
            Log.d(TAG, "  App label: ${packageManager.getApplicationLabel(appInfo)}")
            Log.d(TAG, "  Enabled: $isEnabled")

            if (!isEnabled) {
                Log.d(TAG, "⚠ Package is DISABLED - not counting as installed")
                return false
            }
            return true
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d(TAG, "✗ Method 2 (getApplicationInfo): Package '$packageName' not found")
        } catch (e: Exception) {
            Log.w(TAG, "✗ Method 2 (getApplicationInfo): Error - ${e.message}")
        }

        // Method 3: Check in installed applications list (exact match)
        try {
            val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            Log.d(TAG, "Method 3: Searching through ${installedApps.size} installed apps...")

            val exactMatch = installedApps.find { it.packageName == packageName }
            if (exactMatch != null) {
                val isEnabled = exactMatch.enabled

                Log.d(TAG, "✓ Exact match found for '$packageName'")
                Log.d(TAG, "  App label: ${packageManager.getApplicationLabel(exactMatch)}")
                Log.d(TAG, "  Enabled: $isEnabled")

                if (!isEnabled) {
                    Log.d(TAG, "⚠ Package is DISABLED - not counting as installed")
                    return false
                }
                return true
            }

            // Show similar package names for debugging
            val similarPackages = installedApps.filter {
                it.packageName.contains(packageName.substringAfterLast('.'), ignoreCase = true) ||
                it.packageName.contains("maps", ignoreCase = true) ||
                it.packageName.contains("camera", ignoreCase = true)
            }

            if (similarPackages.isNotEmpty()) {
                Log.d(TAG, "✗ No exact match, but found ${similarPackages.size} similar packages:")
                similarPackages.take(5).forEach { app ->
                    val appName = packageManager.getApplicationLabel(app).toString()
                    Log.d(TAG, "    - ${app.packageName} | $appName | Enabled: ${app.enabled}")
                }
            } else {
                Log.d(TAG, "✗ Method 3: No exact or similar matches found")
            }

        } catch (e: Exception) {
            Log.w(TAG, "✗ Method 3 (installed apps list): Error - ${e.message}")
        }

        Log.d(TAG, "⚠ FINAL RESULT: Package '$packageName' NOT FOUND or DISABLED")
        Log.d(TAG, "============================================")
        Log.d(TAG, "")
        return false
    }

    companion object {
        private const val TAG = "DefaultAppsChecker"
    }

    /**
     * Helper function to get app name from package name
     */
    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }
}
