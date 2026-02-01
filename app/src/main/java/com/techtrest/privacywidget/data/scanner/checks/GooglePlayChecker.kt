package com.techtrest.privacywidget.data.scanner.checks

import android.content.Context
import android.net.wifi.WifiManager
import com.techtrest.privacywidget.data.model.PrivacyCheck
import com.techtrest.privacywidget.data.model.PrivacyIssue

class GooglePlayChecker(private val context: Context) {

    fun checkWifiScanning(): PrivacyIssue {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val isScanAlwaysEnabled = wifiManager.isScanAlwaysAvailable

            PrivacyIssue(
                check = PrivacyCheck.WIFI_SCANNING,
                isSecure = !isScanAlwaysEnabled,
                currentStatus = if (isScanAlwaysEnabled) "Enabled" else "Disabled",
                technicalDetails = "Background Wi-Fi scanning allows location tracking even when Wi-Fi is off"
            )
        } catch (e: Exception) {
            PrivacyIssue(
                check = PrivacyCheck.WIFI_SCANNING,
                isSecure = true, // Don't penalize on error
                currentStatus = "Unable to determine",
                technicalDetails = "Error: ${e.message}"
            )
        }
    }
}
