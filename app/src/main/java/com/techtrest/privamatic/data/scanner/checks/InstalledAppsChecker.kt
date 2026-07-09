package com.techtrest.privamatic.data.scanner.checks

import android.Manifest
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.techtrest.privamatic.BuildConfig
import com.techtrest.privamatic.data.model.PackageNames
import com.techtrest.privamatic.data.model.PrivacyCheck
import com.techtrest.privamatic.data.model.PrivacyIssue
import com.techtrest.privamatic.data.util.PackageManagerUtil

class InstalledAppsChecker(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    // ===== GOOGLE APPS (MAJOR) =====

    fun checkGoogleChrome() = checkAppInstalled(PrivacyCheck.GOOGLE_CHROME)
    fun checkGmailInstalled() = checkAppInstalled(PrivacyCheck.GMAIL_APP)
    fun checkGoogleMapsInstalled() = checkAppInstalled(PrivacyCheck.GOOGLE_MAPS)
    fun checkGooglePhotosInstalled() = checkAppInstalled(PrivacyCheck.GOOGLE_PHOTOS)
    fun checkGoogleDriveInstalled() = checkAppInstalled(PrivacyCheck.GOOGLE_DRIVE)

    // ===== GOOGLE APPS (MINOR) =====

    fun checkYouTubeInstalled() = checkAppInstalled(PrivacyCheck.YOUTUBE)
    fun checkGoogleCalendarInstalled() = checkAppInstalled(PrivacyCheck.GOOGLE_CALENDAR)
    fun checkGoogleKeepInstalled() = checkAppInstalled(PrivacyCheck.GOOGLE_KEEP)
    fun checkGoogleCameraInstalled() = checkAppInstalled(PrivacyCheck.GOOGLE_CAMERA)
    fun checkGoogleDocsInstalled() = checkAppInstalled(PrivacyCheck.GOOGLE_DOCS)

    // ===== META/FACEBOOK APPS =====

    fun checkFacebookInstalled() = checkAppInstalled(PrivacyCheck.FACEBOOK_APP)
    fun checkInstagramInstalled() = checkAppInstalled(PrivacyCheck.INSTAGRAM_APP)
    fun checkWhatsAppInstalled() = checkAppInstalled(PrivacyCheck.WHATSAPP_APP)
    fun checkMessengerInstalled() = checkAppInstalled(PrivacyCheck.MESSENGER_APP)

    // ===== MICROSOFT APPS =====

    fun checkEdgeInstalled() = checkAppInstalled(PrivacyCheck.EDGE_APP)
    fun checkOutlookInstalled() = checkAppInstalled(PrivacyCheck.OUTLOOK_APP)
    fun checkOneDriveInstalled() = checkAppInstalled(PrivacyCheck.ONEDRIVE_APP)

    // ===== AMAZON APPS =====

    fun checkAmazonShoppingInstalled() = checkAppInstalled(PrivacyCheck.AMAZON_SHOPPING)
    fun checkPrimeVideoInstalled() = checkAppInstalled(PrivacyCheck.PRIME_VIDEO)

    // ===== AI/LLM APPS =====

    fun checkChatGPTInstalled() = checkAppInstalled(PrivacyCheck.CHATGPT_APP)

    fun checkGoogleGeminiInstalled(): PrivacyIssue {
        // Google Gemini/Bard - check multiple possible package names
        val possiblePackages = listOf(
            PackageNames.GEMINI,
            PackageNames.GEMINI_ALT
        )

        for (pkg in possiblePackages) {
            if (isAppInstalled(pkg)) {
                return PrivacyIssue(
                    check = PrivacyCheck.GOOGLE_GEMINI,
                    isSecure = false,
                    currentStatus = "Installed and enabled",
                    technicalDetails = "Package: $pkg"
                )
            }
        }

        return PrivacyIssue(
            check = PrivacyCheck.GOOGLE_GEMINI,
            isSecure = true,
            currentStatus = "Not installed or disabled",
            technicalDetails = "Checked: ${possiblePackages.joinToString(", ")}"
        )
    }

    fun checkMicrosoftCopilotInstalled() = checkAppInstalled(PrivacyCheck.MICROSOFT_COPILOT)
    fun checkClaudeInstalled() = checkAppInstalled(PrivacyCheck.CLAUDE_APP)
    fun checkPerplexityInstalled() = checkAppInstalled(PrivacyCheck.PERPLEXITY_APP)
    fun checkMetaAIInstalled() = checkAppInstalled(PrivacyCheck.META_AI)

    // ===== SOCIAL MEDIA =====

    fun checkTikTokInstalled() = checkAppInstalled(PrivacyCheck.TIKTOK_APP)
    fun checkTwitterInstalled() = checkAppInstalled(PrivacyCheck.TWITTER_APP)
    fun checkRedditInstalled() = checkAppInstalled(PrivacyCheck.REDDIT_APP)

    // ===== SYSTEM PERMISSION CHECKS =====

    /**
     * Check for third-party apps with ACCESS_BACKGROUND_LOCATION granted.
     * Only runs on Android 11+ (API 30) where the permission model was tightened.
     */
    fun checkBackgroundLocationApps(): PrivacyIssue {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return PrivacyIssue(
                check = PrivacyCheck.BACKGROUND_LOCATION_APPS,
                isSecure = true,
                currentStatus = "Not applicable (Android < 11)",
                technicalDetails = "Background location permission model requires Android 11+"
            )
        }

        return try {
            @Suppress("DEPRECATION")
            val packages = packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            val appsWithBgLocation = mutableListOf<String>()

            for (packageInfo in packages) {
                val requestedPermissions = packageInfo.requestedPermissions ?: continue
                val requestedPermissionsFlags = packageInfo.requestedPermissionsFlags ?: continue

                val bgLocationIndex = requestedPermissions.indexOfFirst {
                    it == Manifest.permission.ACCESS_BACKGROUND_LOCATION
                }

                if (bgLocationIndex >= 0 &&
                    (requestedPermissionsFlags[bgLocationIndex] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0 &&
                    !PackageManagerUtil.isSystemApp(packageManager, packageInfo.packageName)
                ) {
                    appsWithBgLocation.add(packageInfo.packageName)
                }
            }

            if (appsWithBgLocation.isEmpty()) {
                PrivacyIssue(
                    check = PrivacyCheck.BACKGROUND_LOCATION_APPS,
                    isSecure = true,
                    currentStatus = "No third-party apps have background location",
                    technicalDetails = "Checked ${packages.size} installed packages"
                )
            } else {
                val count = appsWithBgLocation.size
                val appNames = appsWithBgLocation.map { PackageManagerUtil.getAppName(packageManager, it) }
                val pointDeduction = PrivacyCheck.BACKGROUND_LOCATION_APPS.cappedDeductionFor(count)
                val statusText = when {
                    count <= 3 -> "$count app(s) have background location: ${appNames.joinToString(", ")}"
                    else -> {
                        val shown = appNames.take(3).joinToString(", ")
                        val remaining = count - 3
                        "$count app(s) have background location: $shown and $remaining more"
                    }
                }

                PrivacyIssue(
                    check = PrivacyCheck.BACKGROUND_LOCATION_APPS,
                    isSecure = false,
                    currentStatus = statusText,
                    technicalDetails = "Packages: ${appsWithBgLocation.joinToString(", ")}",
                    customPointDeduction = pointDeduction,
                    flaggedPackages = appsWithBgLocation
                )
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Error checking background location apps", e)
            PrivacyIssue(
                check = PrivacyCheck.BACKGROUND_LOCATION_APPS,
                isSecure = true,
                currentStatus = "Unable to determine",
                technicalDetails = "Error: ${e.message}"
            )
        }
    }

    // ===== OLD TARGET SDK CHECK =====

    fun checkOldTargetSdkApps(): PrivacyIssue {
        return try {
            @Suppress("DEPRECATION")
            val packages = packageManager.getInstalledPackages(0)

            // Trust filtering deliberately NOT applied here — the raw result must list
            // all non-system old-SDK apps so TrustedAppsAdjuster (the single owner of
            // trust logic) can recompute deductions when apps are trusted or untrusted.
            val oldSdkApps = packages.filter { packageInfo ->
                val targetSdk = packageInfo.applicationInfo?.targetSdkVersion ?: return@filter false
                targetSdk <= OLD_SDK_THRESHOLD &&
                    !PackageManagerUtil.isSystemApp(packageManager, packageInfo.packageName)
            }

            if (oldSdkApps.isEmpty()) {
                PrivacyIssue(
                    check = PrivacyCheck.OLD_TARGET_SDK,
                    isSecure = true,
                    currentStatus = "All apps target modern Android versions",
                    technicalDetails = "Checked ${packages.size} installed packages"
                )
            } else {
                val count = oldSdkApps.size
                val appNames = oldSdkApps.map {
                    PackageManagerUtil.getAppName(packageManager, it.packageName)
                }
                val flaggedPackages = oldSdkApps.map { it.packageName }
                val pointDeduction = PrivacyCheck.OLD_TARGET_SDK.cappedDeductionFor(count)

                val statusText = when {
                    count <= 3 -> "$count app(s) target old Android versions: ${appNames.joinToString(", ")}"
                    else -> {
                        val shown = appNames.take(3).joinToString(", ")
                        val remaining = count - 3
                        "$count app(s) target old Android versions: $shown and $remaining more"
                    }
                }

                PrivacyIssue(
                    check = PrivacyCheck.OLD_TARGET_SDK,
                    isSecure = false,
                    currentStatus = statusText,
                    technicalDetails = "Apps targeting API ≤ $OLD_SDK_THRESHOLD: ${flaggedPackages.joinToString(", ")}",
                    customPointDeduction = pointDeduction,
                    flaggedPackages = flaggedPackages
                )
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Error checking old target SDK apps", e)
            PrivacyIssue(
                check = PrivacyCheck.OLD_TARGET_SDK,
                isSecure = true,
                currentStatus = "Unable to determine",
                technicalDetails = "Error: ${e.message}"
            )
        }
    }

    // ===== HELPER METHODS =====

    /**
     * Check if an app is installed using package name and point deduction from the PrivacyCheck enum.
     */
    private fun checkAppInstalled(check: PrivacyCheck): PrivacyIssue {
        val packageName = check.packageName
            ?: return PrivacyIssue(
                check = check,
                isSecure = true,
                currentStatus = "Unable to determine",
                technicalDetails = "No package name configured"
            )

        return try {
            val isInstalled = isAppInstalled(packageName)
            val isSystem = isInstalled && PackageManagerUtil.isSystemApp(packageManager, packageName)

            PrivacyIssue(
                check = check,
                isSecure = !isInstalled,
                isSystemApp = isSystem,
                currentStatus = if (isInstalled) "Installed" else "Not installed",
                technicalDetails = "Package: $packageName"
            )
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Error checking ${context.getString(check.displayName)}", e)
            PrivacyIssue(
                check = check,
                isSecure = true,
                currentStatus = "Unable to determine",
                technicalDetails = "Error: ${e.message}"
            )
        }
    }

    /**
     * Helper function to check if an app is installed AND enabled.
     * Only returns true if the app is both installed and enabled.
     */
    private fun isAppInstalled(packageName: String): Boolean {
        // Method 1: Try getPackageInfo
        try {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA)
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            if (!appInfo.enabled) return false
            return true
        } catch (e: PackageManager.NameNotFoundException) {
            // Package not found, continue to next method
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "Error checking package $packageName: ${e.message}")
        }

        // Method 2: Try getApplicationInfo
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            if (!appInfo.enabled) return false
            return true
        } catch (e: PackageManager.NameNotFoundException) {
            // Package not found
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "Error checking package $packageName: ${e.message}")
        }

        return false
    }

    companion object {
        private const val TAG = "InstalledAppsChecker"
        private const val OLD_SDK_THRESHOLD = 22
    }
}
