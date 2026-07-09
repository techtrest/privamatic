package com.techtrest.privamatic.data.scanner.checks

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager
import com.techtrest.privamatic.BuildConfig
import com.techtrest.privamatic.data.model.PrivacyCheck
import com.techtrest.privamatic.data.model.PrivacyIssue
import com.techtrest.privamatic.data.util.PackageManagerUtil

class SystemServicesChecker(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    /**
     * Check if device storage is encrypted
     */
    fun checkDeviceEncryption(): PrivacyIssue {
        return try {
            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
                ?: return PrivacyIssue(
                    check = PrivacyCheck.DEVICE_ENCRYPTION,
                    isSecure = true,
                    currentStatus = "Unable to determine",
                    technicalDetails = "Device policy service not available on this device"
                )
            val encryptionStatus = devicePolicyManager.storageEncryptionStatus
            val isEncrypted = encryptionStatus == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE ||
                    encryptionStatus == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY ||
                    encryptionStatus == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER

            PrivacyIssue(
                check = PrivacyCheck.DEVICE_ENCRYPTION,
                isSecure = isEncrypted,
                currentStatus = if (isEncrypted) "Enabled" else "Not encrypted",
                technicalDetails = "Checked using DevicePolicyManager (SDK ${Build.VERSION.SDK_INT})"
            )
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Error checking device encryption", e)
            PrivacyIssue(
                check = PrivacyCheck.DEVICE_ENCRYPTION,
                isSecure = true, // Don't penalize on error
                currentStatus = "Unable to determine",
                technicalDetails = "Error: ${e.message}"
            )
        }
    }

    /**
     * Check which apps have notification listener access
     * This is a critical privacy risk - these apps can read ALL notifications
     */
    fun checkNotificationListeners(): PrivacyIssue {
        return try {
            val enabledListeners = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: ""

            if (enabledListeners.isEmpty()) {
                return PrivacyIssue(
                    check = PrivacyCheck.NOTIFICATION_LISTENER,
                    isSecure = true,
                    currentStatus = "No notification listeners enabled",
                    technicalDetails = "Checked enabled_notification_listeners setting"
                )
            }

            // Parse the enabled listeners
            val listenerPackages = enabledListeners.split(":")
                .map { it.substringBefore("/") }
                .filter { it.isNotEmpty() }
                .distinct()

            // Filter out system apps
            val nonSystemListeners = listenerPackages.filter { pkg ->
                !PackageManagerUtil.isSystemApp(packageManager, pkg)
            }

            if (nonSystemListeners.isEmpty()) {
                PrivacyIssue(
                    check = PrivacyCheck.NOTIFICATION_LISTENER,
                    isSecure = true,
                    currentStatus = "Only system apps have notification access",
                    technicalDetails = "System apps: ${listenerPackages.size}"
                )
            } else {
                val appNames = nonSystemListeners.map { PackageManagerUtil.getAppName(packageManager, it) }
                val pointDeduction = PrivacyCheck.NOTIFICATION_LISTENER.cappedDeductionFor(nonSystemListeners.size)

                PrivacyIssue(
                    check = PrivacyCheck.NOTIFICATION_LISTENER,
                    isSecure = false,
                    currentStatus = "${nonSystemListeners.size} non-system app(s) have notification access: ${appNames.joinToString(", ")}",
                    technicalDetails = "Packages: ${nonSystemListeners.joinToString(", ")}",
                    customPointDeduction = pointDeduction,
                    flaggedPackages = nonSystemListeners
                )
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Error checking notification listeners", e)
            PrivacyIssue(
                check = PrivacyCheck.NOTIFICATION_LISTENER,
                isSecure = true, // Don't penalize on error
                currentStatus = "Unable to determine",
                technicalDetails = "Error: ${e.message}"
            )
        }
    }

    /**
     * Check which apps have accessibility services enabled
     * Whitelist legitimate apps like password managers
     */
    fun checkAccessibilityServices(): PrivacyIssue {
        return try {
            val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
                ?: return PrivacyIssue(
                    check = PrivacyCheck.ACCESSIBILITY_SERVICE,
                    isSecure = true,
                    currentStatus = "Unable to determine",
                    technicalDetails = "Accessibility service not available on this device"
                )
            val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)

            if (enabledServices.isEmpty()) {
                return PrivacyIssue(
                    check = PrivacyCheck.ACCESSIBILITY_SERVICE,
                    isSecure = true,
                    currentStatus = "No accessibility services enabled",
                    technicalDetails = "Checked AccessibilityManager"
                )
            }

            // Whitelist of exact package names for legitimate password managers.
            // System accessibility tools (TalkBack, Samsung accessibility, etc.) are
            // deliberately not listed here — PackageManagerUtil.isSystemApp() below
            // already excludes them without relying on string matching.
            val whitelist = setOf(
                "com.x8bit.bitwarden", // Bitwarden
                "com.bitwarden.authenticator", // Bitwarden Authenticator
                "com.kunzisoft.keepass.libre", // KeePassDX (F-Droid)
                "com.kunzisoft.keepass.free", // KeePassDX (Play, free)
                "com.kunzisoft.keepass.pro", // KeePassDX (Play, pro)
                "com.android.keepass", // KeePassDroid
                "keepass2android.keepass2android", // Keepass2Android
                "keepass2android.keepass2android_nonet", // Keepass2Android offline
                "org.pwsafe.android", // Password Safe
                "com.lastpass.lpandroid", // LastPass
                "com.onepassword.android", // 1Password (current package)
                "com.agilebits.onepassword", // 1Password (legacy package)
                "com.dashlane", // Dashlane
                "org.thoughtcrime.securesms" // Signal
            )

            val servicePackages = enabledServices.map { it.resolveInfo.serviceInfo.packageName }.distinct()

            // Filter out whitelisted and system apps
            val suspiciousServices = servicePackages.filter { pkg ->
                !PackageManagerUtil.isSystemApp(packageManager, pkg) && pkg !in whitelist
            }

            if (suspiciousServices.isEmpty()) {
                PrivacyIssue(
                    check = PrivacyCheck.ACCESSIBILITY_SERVICE,
                    isSecure = true,
                    currentStatus = "Only legitimate apps have accessibility access",
                    technicalDetails = "Total services: ${servicePackages.size}, all whitelisted or system"
                )
            } else {
                val appNames = suspiciousServices.map { PackageManagerUtil.getAppName(packageManager, it) }
                val pointDeduction = PrivacyCheck.ACCESSIBILITY_SERVICE.cappedDeductionFor(suspiciousServices.size)

                PrivacyIssue(
                    check = PrivacyCheck.ACCESSIBILITY_SERVICE,
                    isSecure = false,
                    currentStatus = "${suspiciousServices.size} app(s) have accessibility access: ${appNames.joinToString(", ")}",
                    technicalDetails = "Packages: ${suspiciousServices.joinToString(", ")}",
                    customPointDeduction = pointDeduction,
                    flaggedPackages = suspiciousServices
                )
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Error checking accessibility services", e)
            PrivacyIssue(
                check = PrivacyCheck.ACCESSIBILITY_SERVICE,
                isSecure = true, // Don't penalize on error
                currentStatus = "Unable to determine",
                technicalDetails = "Error: ${e.message}"
            )
        }
    }

    /**
     * Check which apps have device administrator privileges
     */
    fun checkDeviceAdminApps(): PrivacyIssue {
        return try {
            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
                ?: return PrivacyIssue(
                    check = PrivacyCheck.DEVICE_ADMIN,
                    isSecure = true,
                    currentStatus = "Unable to determine",
                    technicalDetails = "Device policy service not available on this device"
                )
            val activeAdmins = devicePolicyManager.activeAdmins

            if (activeAdmins.isNullOrEmpty()) {
                return PrivacyIssue(
                    check = PrivacyCheck.DEVICE_ADMIN,
                    isSecure = true,
                    currentStatus = "No device admin apps",
                    technicalDetails = "Checked DevicePolicyManager"
                )
            }

            // Filter out system and work profile admins
            val nonSystemAdmins = activeAdmins.filter { component ->
                !PackageManagerUtil.isSystemApp(packageManager, component.packageName)
            }

            if (nonSystemAdmins.isEmpty()) {
                PrivacyIssue(
                    check = PrivacyCheck.DEVICE_ADMIN,
                    isSecure = true,
                    currentStatus = "Only system apps have device admin",
                    technicalDetails = "Total admins: ${activeAdmins.size}, all system"
                )
            } else {
                val appNames = nonSystemAdmins.map { PackageManagerUtil.getAppName(packageManager, it.packageName) }
                val pointDeduction = PrivacyCheck.DEVICE_ADMIN.cappedDeductionFor(nonSystemAdmins.size)
                val adminPackages = nonSystemAdmins.map { it.packageName }

                PrivacyIssue(
                    check = PrivacyCheck.DEVICE_ADMIN,
                    isSecure = false,
                    currentStatus = "${nonSystemAdmins.size} non-system app(s) have device admin: ${appNames.joinToString(", ")}",
                    technicalDetails = "Packages: ${adminPackages.joinToString(", ")}",
                    customPointDeduction = pointDeduction,
                    flaggedPackages = adminPackages
                )
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Error checking device admin apps", e)
            PrivacyIssue(
                check = PrivacyCheck.DEVICE_ADMIN,
                isSecure = true, // Don't penalize on error
                currentStatus = "Unable to determine",
                technicalDetails = "Error: ${e.message}"
            )
        }
    }

    companion object {
        private const val TAG = "SystemServicesChecker"
    }
}
