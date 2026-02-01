package com.techtrest.privacywidget.data.scanner.checks

import android.app.KeyguardManager
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.techtrest.privacywidget.data.model.PrivacyCheck
import com.techtrest.privacywidget.data.model.PrivacyIssue

class SecuritySettingsChecker(private val context: Context) {

    fun checkScreenLock(): PrivacyIssue {
        return try {
            val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            val isSecure = keyguardManager.isDeviceSecure

            PrivacyIssue(
                check = PrivacyCheck.SCREEN_LOCK,
                isSecure = isSecure,
                currentStatus = if (isSecure) "Enabled" else "Disabled",
                technicalDetails = "Checked using KeyguardManager.isDeviceSecure"
            )
        } catch (e: Exception) {
            PrivacyIssue(
                check = PrivacyCheck.SCREEN_LOCK,
                isSecure = false,
                currentStatus = "Unable to determine",
                technicalDetails = "Error: ${e.message}"
            )
        }
    }

    fun checkBiometricAuth(): PrivacyIssue {
        return try {
            val hasBiometric = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                checkBiometricAPI30Plus()
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                checkBiometricAPI29()
            } else {
                // For SDK 26-28, biometric check is not reliably available
                return PrivacyIssue(
                    check = PrivacyCheck.BIOMETRIC_AUTH,
                    isSecure = true, // Don't penalize on older devices
                    currentStatus = "Not available on Android < 10",
                    technicalDetails = "BiometricManager API requires Android 10+"
                )
            }

            PrivacyIssue(
                check = PrivacyCheck.BIOMETRIC_AUTH,
                isSecure = hasBiometric,
                currentStatus = if (hasBiometric) "Available and enrolled" else "Not enrolled",
                technicalDetails = "Checked using BiometricManager (SDK ${Build.VERSION.SDK_INT})"
            )
        } catch (e: Exception) {
            PrivacyIssue(
                check = PrivacyCheck.BIOMETRIC_AUTH,
                isSecure = true, // Don't penalize on error
                currentStatus = "Unable to determine",
                technicalDetails = "Error: ${e.message}"
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun checkBiometricAPI29(): Boolean {
        val biometricManager = context.getSystemService(Context.BIOMETRIC_SERVICE) as BiometricManager
        val canAuthenticate = biometricManager.canAuthenticate()
        return canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun checkBiometricAPI30Plus(): Boolean {
        val biometricManager = context.getSystemService(Context.BIOMETRIC_SERVICE) as BiometricManager
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        return canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS
    }
}
