package com.techtrest.privacywidget.data.scanner.checks

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.techtrest.privacywidget.data.model.PrivacyCheck
import com.techtrest.privacywidget.data.model.PrivacyIssue

class InstalledAppsChecker(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    // ===== GOOGLE APPS (MAJOR - 1.5 points each) =====

    fun checkGoogleChrome(): PrivacyIssue {
        return checkAppInstalled(
            check = PrivacyCheck.GOOGLE_CHROME,
            packageName = "com.android.chrome",
            pointDeduction = 1
        )
    }

    fun checkGmailInstalled(): PrivacyIssue {
        return checkAppInstalled(
            check = PrivacyCheck.GMAIL_APP,
            packageName = "com.google.android.gm",
            pointDeduction = 1
        )
    }

    fun checkGoogleMapsInstalled(): PrivacyIssue {
        return checkAppInstalled(
            check = PrivacyCheck.GOOGLE_MAPS,
            packageName = "com.google.android.apps.maps",
            pointDeduction = 1
        )
    }

    fun checkGooglePhotosInstalled(): PrivacyIssue {
        return checkAppInstalled(
            check = PrivacyCheck.GOOGLE_PHOTOS,
            packageName = "com.google.android.apps.photos",
            pointDeduction = 1
        )
    }

    fun checkGoogleDriveInstalled(): PrivacyIssue {
        return checkAppInstalled(
            check = PrivacyCheck.GOOGLE_DRIVE,
            packageName = "com.google.android.apps.docs",
            pointDeduction = 1
        )
    }

    // ===== GOOGLE APPS (MINOR - 0.5 points each) =====

    fun checkYouTubeInstalled(): PrivacyIssue {
        return checkAppInstalled(
            check = PrivacyCheck.YOUTUBE,
            packageName = "com.google.android.youtube",
            pointDeduction = 0
        )
    }

    fun checkGoogleCalendarInstalled(): PrivacyIssue {
        return checkAppInstalled(
            check = PrivacyCheck.GOOGLE_CALENDAR,
            packageName = "com.google.android.calendar",
            pointDeduction = 0
        )
    }

    fun checkGoogleKeepInstalled(): PrivacyIssue {
        return checkAppInstalled(
            check = PrivacyCheck.GOOGLE_KEEP,
            packageName = "com.google.android.keep",
            pointDeduction = 0
        )
    }

    fun checkGoogleCameraInstalled(): PrivacyIssue {
        return checkAppInstalled(
            check = PrivacyCheck.GOOGLE_CAMERA,
            packageName = "com.google.android.GoogleCamera",
            pointDeduction = 0
        )
    }

    fun checkGoogleDocsInstalled(): PrivacyIssue {
        return checkAppInstalled(
            check = PrivacyCheck.GOOGLE_DOCS,
            packageName = "com.google.android.apps.docs.editors.docs",
            pointDeduction = 0
        )
    }

    // ===== META/FACEBOOK APPS (1.5 points each) =====

    fun checkFacebookInstalled(): PrivacyIssue {
        return checkAppInstalled(
            check = PrivacyCheck.FACEBOOK_APP,
            packageName = "com.facebook.katana",
            pointDeduction = 1
        )
    }

    fun checkInstagramInstalled(): PrivacyIssue {
        return checkAppInstalled(
            check = PrivacyCheck.INSTAGRAM_APP,
            packageName = "com.instagram.android",
            pointDeduction = 1
        )
    }

    fun checkWhatsAppInstalled(): PrivacyIssue {
        return checkAppInstalled(
            check = PrivacyCheck.WHATSAPP_APP,
            packageName = "com.whatsapp",
            pointDeduction = 1
        )
    }

    fun checkMessengerInstalled(): PrivacyIssue {
        return checkAppInstalled(
            check = PrivacyCheck.MESSENGER_APP,
            packageName = "com.facebook.orca",
            pointDeduction = 1
        )
    }

    // ===== MICROSOFT APPS =====

    fun checkEdgeInstalled(): PrivacyIssue {
        return checkAppInstalled(
            check = PrivacyCheck.EDGE_APP,
            packageName = "com.microsoft.emmx",
            pointDeduction = 1
        )
    }

    fun checkOutlookInstalled(): PrivacyIssue {
        return checkAppInstalled(
            check = PrivacyCheck.OUTLOOK_APP,
            packageName = "com.microsoft.office.outlook",
            pointDeduction = 1
        )
    }

    fun checkOneDriveInstalled(): PrivacyIssue {
        return checkAppInstalled(
            check = PrivacyCheck.ONEDRIVE_APP,
            packageName = "com.microsoft.skydrive",
            pointDeduction = 0
        )
    }

    // ===== AMAZON APPS =====

    fun checkAmazonShoppingInstalled(): PrivacyIssue {
        return checkAppInstalled(
            check = PrivacyCheck.AMAZON_SHOPPING,
            packageName = "com.amazon.mShop.android.shopping",
            pointDeduction = 1
        )
    }

    fun checkPrimeVideoInstalled(): PrivacyIssue {
        return checkAppInstalled(
            check = PrivacyCheck.PRIME_VIDEO,
            packageName = "com.amazon.avod.thirdpartyclient",
            pointDeduction = 0
        )
    }

    // ===== AI/LLM APPS =====

    fun checkChatGPTInstalled(): PrivacyIssue {
        return checkAppInstalled(
            check = PrivacyCheck.CHATGPT_APP,
            packageName = "com.openai.chatgpt",
            pointDeduction = 1
        )
    }

    fun checkGoogleGeminiInstalled(): PrivacyIssue {
        // Google Gemini/Bard - check multiple possible package names
        val possiblePackages = listOf(
            "com.google.android.apps.bard",
            "com.google.android.apps.gemini"
        )

        for (pkg in possiblePackages) {
            if (isAppInstalled(pkg)) {
                return PrivacyIssue(
                    check = PrivacyCheck.GOOGLE_GEMINI,
                    isSecure = false,
                    currentStatus = "Installed and enabled",
                    technicalDetails = "Package: $pkg",
                    customPointDeduction = 1
                )
            }
        }

        return PrivacyIssue(
            check = PrivacyCheck.GOOGLE_GEMINI,
            isSecure = true,
            currentStatus = "Not installed or disabled",
            technicalDetails = "Checked: ${possiblePackages.joinToString(", ")}",
            customPointDeduction = 0
        )
    }

    fun checkMicrosoftCopilotInstalled(): PrivacyIssue {
        return checkAppInstalled(
            check = PrivacyCheck.MICROSOFT_COPILOT,
            packageName = "com.microsoft.copilot",
            pointDeduction = 1
        )
    }

    fun checkClaudeInstalled(): PrivacyIssue {
        return checkAppInstalled(
            check = PrivacyCheck.CLAUDE_APP,
            packageName = "com.anthropic.claude",
            pointDeduction = 1
        )
    }

    fun checkPerplexityInstalled(): PrivacyIssue {
        return checkAppInstalled(
            check = PrivacyCheck.PERPLEXITY_APP,
            packageName = "ai.perplexity.app.android",
            pointDeduction = 0
        )
    }

    fun checkMetaAIInstalled(): PrivacyIssue {
        return checkAppInstalled(
            check = PrivacyCheck.META_AI,
            packageName = "com.meta.ai",
            pointDeduction = 1
        )
    }

    // ===== SOCIAL MEDIA =====

    fun checkTikTokInstalled(): PrivacyIssue {
        return checkAppInstalled(
            check = PrivacyCheck.TIKTOK_APP,
            packageName = "com.zhiliaoapp.musically",
            pointDeduction = 1
        )
    }

    fun checkTwitterInstalled(): PrivacyIssue {
        return checkAppInstalled(
            check = PrivacyCheck.TWITTER_APP,
            packageName = "com.twitter.android",
            pointDeduction = 1
        )
    }

    fun checkRedditInstalled(): PrivacyIssue {
        return checkAppInstalled(
            check = PrivacyCheck.REDDIT_APP,
            packageName = "com.reddit.frontpage",
            pointDeduction = 0
        )
    }

    // ===== HELPER METHODS =====

    /**
     * Generic method to check if an app is installed
     */
    private fun checkAppInstalled(
        check: PrivacyCheck,
        packageName: String,
        pointDeduction: Int
    ): PrivacyIssue {
        return try {
            val isInstalled = isAppInstalled(packageName)

            PrivacyIssue(
                check = check,
                isSecure = !isInstalled,
                currentStatus = if (isInstalled) "Installed" else "Not installed",
                technicalDetails = "Package: $packageName",
                customPointDeduction = if (isInstalled) pointDeduction else 0
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking ${check.displayName}", e)
            PrivacyIssue(
                check = check,
                isSecure = true,
                currentStatus = "Unable to determine",
                technicalDetails = "Error: ${e.message}",
                customPointDeduction = 0
            )
        }
    }

    /**
     * Helper function to check if an app is installed AND enabled
     * Only returns true if the app is both installed and enabled
     */
    private fun isAppInstalled(packageName: String): Boolean {
        Log.d(TAG, "Checking for package: $packageName")

        // Method 1: Try getPackageInfo
        try {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA)
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val isEnabled = appInfo.enabled

            Log.d(TAG, "✓ Package found: $packageName (Enabled: $isEnabled)")

            if (!isEnabled) {
                Log.d(TAG, "⚠ Package is DISABLED - not counting as installed")
                return false
            }
            return true
        } catch (e: PackageManager.NameNotFoundException) {
            // Package not found, continue to next method
        } catch (e: Exception) {
            Log.w(TAG, "Error checking package $packageName: ${e.message}")
        }

        // Method 2: Try getApplicationInfo
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val isEnabled = appInfo.enabled

            Log.d(TAG, "✓ Package found via getApplicationInfo: $packageName (Enabled: $isEnabled)")

            if (!isEnabled) {
                Log.d(TAG, "⚠ Package is DISABLED - not counting as installed")
                return false
            }
            return true
        } catch (e: PackageManager.NameNotFoundException) {
            // Package not found
        } catch (e: Exception) {
            Log.w(TAG, "Error checking package $packageName: ${e.message}")
        }

        Log.d(TAG, "✗ Package NOT found or DISABLED: $packageName")
        return false
    }

    companion object {
        private const val TAG = "InstalledAppsChecker"
    }
}
