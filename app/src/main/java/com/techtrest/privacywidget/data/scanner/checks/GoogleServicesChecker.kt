package com.techtrest.privacywidget.data.scanner.checks

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.techtrest.privacywidget.data.model.PrivacyCheck
import com.techtrest.privacywidget.data.model.PrivacyIssue

class GoogleServicesChecker(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    /**
     * Check Google Play Services installation state.
     * Stage 1: Not installed → isSecure = true
     * Stage 2: MicroG present → isSecure = true (privacy-friendly replacement)
     * Stage 3: FLAG_SYSTEM or FLAG_UPDATED_SYSTEM_APP set → isSecure = false
     * Stage 4: Sandboxed (no system flags) → isSecure = true
     */
    fun checkGooglePlayServices(): PrivacyIssue {
        return try {
            val appInfo = try {
                packageManager.getApplicationInfo(GMS_PACKAGE, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }

            val isMicroG = try {
                packageManager.getApplicationInfo(MICROG_PACKAGE, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }

            val hasSystemPrivileges = appInfo != null &&
                ((appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0 ||
                    (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0)

            when {
                appInfo == null -> PrivacyIssue(
                    check = PrivacyCheck.GOOGLE_PLAY_SERVICES,
                    isSecure = true,
                    currentStatus = "Not installed",
                    technicalDetails = "$GMS_PACKAGE is not installed"
                )
                isMicroG -> PrivacyIssue(
                    check = PrivacyCheck.GOOGLE_PLAY_SERVICES,
                    isSecure = true,
                    currentStatus = "MicroG installed (privacy-friendly replacement)",
                    technicalDetails = "MicroG is an open-source Google Play Services replacement"
                )
                hasSystemPrivileges -> PrivacyIssue(
                    check = PrivacyCheck.GOOGLE_PLAY_SERVICES,
                    isSecure = false,
                    isSystemApp = true,
                    currentStatus = "Installed with full system privileges",
                    technicalDetails = "Google Play Services has deep system access and telemetry"
                )
                else -> PrivacyIssue(
                    check = PrivacyCheck.GOOGLE_PLAY_SERVICES,
                    isSecure = true,
                    currentStatus = "Installed (sandboxed)",
                    technicalDetails = "Running without system privileges - reduced privacy risk"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Google Play Services", e)
            PrivacyIssue(
                check = PrivacyCheck.GOOGLE_PLAY_SERVICES,
                isSecure = true,
                currentStatus = "Unable to determine",
                technicalDetails = "Error: ${e.message}"
            )
        }
    }

    /**
     * Check if Google's Find My Device is active by looking for its package.
     * com.google.android.apps.adm installed and enabled = Find My Device is active.
     */
    fun checkFindMyDevice(): PrivacyIssue {
        return try {
            val appInfo = try {
                context.packageManager.getApplicationInfo(FIND_MY_DEVICE_PACKAGE, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }

            val isActive = appInfo != null && appInfo.enabled

            PrivacyIssue(
                check = PrivacyCheck.FIND_MY_DEVICE,
                isSecure = !isActive,
                currentStatus = if (isActive) "Enabled" else "Not installed / Not applicable",
                technicalDetails = if (isActive)
                    "$FIND_MY_DEVICE_PACKAGE is installed and enabled"
                else
                    "$FIND_MY_DEVICE_PACKAGE is not installed or is disabled"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Find My Device", e)
            PrivacyIssue(
                check = PrivacyCheck.FIND_MY_DEVICE,
                isSecure = true,
                currentStatus = "Unable to determine",
                technicalDetails = "Error: ${e.message}"
            )
        }
    }

    companion object {
        private const val TAG = "GoogleServicesChecker"
        private const val GMS_PACKAGE = "com.google.android.gms"
        private const val MICROG_PACKAGE = "org.microg.gms.droidguard"
        private const val FIND_MY_DEVICE_PACKAGE = "com.google.android.apps.adm"
    }
}
