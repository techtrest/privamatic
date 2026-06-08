package com.techtrest.privamatic.data.scanner.checks

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.provider.Telephony
import android.util.Log
import com.techtrest.privamatic.BuildConfig
import com.techtrest.privamatic.data.model.PrivacyCheck
import com.techtrest.privamatic.data.model.PrivacyIssue
import com.techtrest.privamatic.data.util.PackageManagerUtil

class DefaultAppsChecker(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    /**
     * Check default browser app with three-tier detection
     * Privacy-invasive: Chrome, Edge, Opera, UC Browser (-3), Samsung Internet (-2)
     * Privacy-friendly: Brave, Firefox, DuckDuckGo, etc. (0)
     * Unknown: Everything else (0, displayed as unknown)
     */
    fun checkDefaultBrowser(): PrivacyIssue {
        return try {
            // URI passed only to PackageManager.resolveActivity() — no network request is made
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.example.com"))

            // Try MATCH_DEFAULT_ONLY first
            var resolveInfo = packageManager.resolveActivity(browserIntent, PackageManager.MATCH_DEFAULT_ONLY)
            var packageName = resolveInfo?.activityInfo?.packageName

            // If no default, try without the flag to see what's available
            if (packageName == null || packageName == "android") {
                resolveInfo = packageManager.resolveActivity(browserIntent, 0)
                val fallbackPackage = resolveInfo?.activityInfo?.packageName

                // If we got a real app (not the chooser), use it
                if (fallbackPackage != null && fallbackPackage != "android") {
                    packageName = fallbackPackage
                }
            }

            val finalPackage = packageName ?: "none"

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
                val appName = PackageManagerUtil.getAppName(packageManager, finalPackage)
                PrivacyIssue(
                    check = PrivacyCheck.DEFAULT_BROWSER,
                    isSecure = true,
                    currentStatus = "Using $appName (unknown, 0 pts)",
                    technicalDetails = "Package: $finalPackage",
                    customPointDeduction = 0
                )
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Error checking default browser", e)
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
                val appName = PackageManagerUtil.getAppName(packageManager, defaultSmsPackage)
                PrivacyIssue(
                    check = PrivacyCheck.DEFAULT_SMS,
                    isSecure = true,
                    currentStatus = "Using $appName (unknown, 0 pts)",
                    technicalDetails = "Package: $defaultSmsPackage",
                    customPointDeduction = 0
                )
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Error checking default SMS", e)
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

            // Privacy-friendly keyboards (allowlist)
            val friendly = when {
                currentKeyboard.contains("florisboard", ignoreCase = true) || currentKeyboard.contains("dev.patrickgold.florisboard", ignoreCase = true) -> "FlorisBoard"
                currentKeyboard.contains("heliboard", ignoreCase = true) || currentKeyboard.contains("helium314.keyboard", ignoreCase = true) -> "HeliBoard"
                currentKeyboard.contains("anysoftkeyboard", ignoreCase = true) -> "AnySoftKeyboard"
                currentKeyboard.contains("simplekeyboard", ignoreCase = true) || currentKeyboard.contains("rkr.simplekeyboard.inputmethod", ignoreCase = true) -> "Simple Keyboard"
                currentKeyboard.contains("futo", ignoreCase = true) || currentKeyboard.contains("org.futo.inputmethod.latin", ignoreCase = true) -> "FUTO Keyboard"
                currentKeyboard.contains("unexpected", ignoreCase = true) && currentKeyboard.contains("keyboard") || currentKeyboard.contains("juloo.keyboard2", ignoreCase = true) -> "Unexpected Keyboard"
                currentKeyboard.contains("openboard", ignoreCase = true) || currentKeyboard.contains("org.dslul.openboard.inputmethod.latin", ignoreCase = true) -> "OpenBoard"
                currentKeyboard.contains("android.inputmethod.latin", ignoreCase = true) || currentKeyboard.contains("com.android.inputmethod", ignoreCase = true) -> "AOSP Keyboard"
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
            val keyboardPackage = currentKeyboard.substringBefore('/')
            val appName = PackageManagerUtil.getAppName(packageManager, keyboardPackage)
            return PrivacyIssue(
                check = PrivacyCheck.DEFAULT_KEYBOARD,
                isSecure = false,
                currentStatus = "Using $appName",
                technicalDetails = "IME: $currentKeyboard",
                customPointDeduction = 3,
                flaggedPackages = listOf(keyboardPackage)
            )
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Error checking default keyboard", e)
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
                packageName.contains("net.thunderbird.android", ignoreCase = true) -> "Thunderbird"
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
                val appName = PackageManagerUtil.getAppName(packageManager, packageName)
                PrivacyIssue(
                    check = PrivacyCheck.DEFAULT_EMAIL,
                    isSecure = true,
                    currentStatus = "Using $appName (unknown, 0 pts)",
                    technicalDetails = "Package: $packageName",
                    customPointDeduction = 0
                )
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Error checking default email", e)
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
                packageName.contains("neolauncher", ignoreCase = true) -> "Neo Launcher"
                packageName.contains("de.mm20.launcher2", ignoreCase = true) -> "Kvaesitso"
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
                val appName = PackageManagerUtil.getAppName(packageManager, packageName)
                PrivacyIssue(
                    check = PrivacyCheck.DEFAULT_LAUNCHER,
                    isSecure = true,
                    currentStatus = "Using $appName (unknown, 0 pts)",
                    technicalDetails = "Package: $packageName",
                    customPointDeduction = 0
                )
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Error checking default launcher", e)
            PrivacyIssue(
                check = PrivacyCheck.DEFAULT_LAUNCHER,
                isSecure = true,
                currentStatus = "Unable to determine default launcher",
                technicalDetails = "Error: ${e.message}",
                customPointDeduction = 0
            )
        }
    }

    companion object {
        private const val TAG = "DefaultAppsChecker"
    }
}
