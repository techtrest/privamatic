package com.techtrest.privacywidget.data.scanner.checks

import android.content.Context
import android.provider.Settings
import com.techtrest.privacywidget.data.model.PrivacyCheck
import com.techtrest.privacywidget.data.model.PrivacyIssue

class DeveloperSettingsChecker(private val context: Context) {

    fun checkUsbDebugging(): PrivacyIssue {
        return try {
            val isEnabled = Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.ADB_ENABLED,
                0
            ) == 1

            PrivacyIssue(
                check = PrivacyCheck.USB_DEBUGGING,
                isSecure = !isEnabled,
                currentStatus = if (isEnabled) "Enabled" else "Disabled",
                technicalDetails = "Checked Settings.Global.ADB_ENABLED"
            )
        } catch (e: Exception) {
            PrivacyIssue(
                check = PrivacyCheck.USB_DEBUGGING,
                isSecure = true, // Assume secure if unable to check
                currentStatus = "Unable to determine",
                technicalDetails = "Error: ${e.message}"
            )
        }
    }

    fun checkDeveloperOptions(): PrivacyIssue {
        return try {
            val isEnabled = Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                0
            ) == 1

            PrivacyIssue(
                check = PrivacyCheck.DEVELOPER_OPTIONS,
                isSecure = !isEnabled,
                currentStatus = if (isEnabled) "Enabled" else "Disabled",
                technicalDetails = "Checked Settings.Global.DEVELOPMENT_SETTINGS_ENABLED"
            )
        } catch (e: Exception) {
            PrivacyIssue(
                check = PrivacyCheck.DEVELOPER_OPTIONS,
                isSecure = true, // Assume secure if unable to check
                currentStatus = "Unable to determine",
                technicalDetails = "Error: ${e.message}"
            )
        }
    }
}
