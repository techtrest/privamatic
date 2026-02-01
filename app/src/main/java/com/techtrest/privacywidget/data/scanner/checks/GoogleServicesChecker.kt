package com.techtrest.privacywidget.data.scanner.checks

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.techtrest.privacywidget.data.model.PrivacyCheck
import com.techtrest.privacywidget.data.model.PrivacyIssue

class GoogleServicesChecker(private val context: Context) {

    /**
     * Check if Google's Find My Device is enabled
     */
    fun checkFindMyDevice(): PrivacyIssue {
        return try {
            // Check through multiple methods as implementation varies by device/Android version
            val findMyDeviceEnabled = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
                    // Try to check through system settings
                    try {
                        val value = Settings.Secure.getInt(
                            context.contentResolver,
                            "find_my_device_enabled",
                            -1
                        )
                        value == 1
                    } catch (e: Exception) {
                        // Setting might not exist, try alternative
                        checkFindMyDeviceAlternative()
                    }
                }
                else -> {
                    // Older versions
                    checkFindMyDeviceAlternative()
                }
            }

            PrivacyIssue(
                check = PrivacyCheck.FIND_MY_DEVICE,
                isSecure = !findMyDeviceEnabled,
                currentStatus = if (findMyDeviceEnabled) "Enabled" else "Disabled",
                technicalDetails = "Checked via system settings (SDK ${Build.VERSION.SDK_INT})"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Find My Device", e)
            PrivacyIssue(
                check = PrivacyCheck.FIND_MY_DEVICE,
                isSecure = true, // Don't penalize on error
                currentStatus = "Unable to determine",
                technicalDetails = "Error: ${e.message}"
            )
        }
    }

    /**
     * Alternative method to check Find My Device
     * Checks if Google Play Services device admin is active
     */
    private fun checkFindMyDeviceAlternative(): Boolean {
        return try {
            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE)
                as? android.app.admin.DevicePolicyManager

            val activeAdmins = devicePolicyManager?.activeAdmins

            // Check if any Google Play Services component is a device admin
            activeAdmins?.any { component ->
                component.packageName.contains("google", ignoreCase = true) ||
                component.packageName.contains("gms", ignoreCase = true)
            } ?: false
        } catch (e: Exception) {
            Log.w(TAG, "Alternative Find My Device check failed", e)
            false
        }
    }

    companion object {
        private const val TAG = "GoogleServicesChecker"
    }
}
