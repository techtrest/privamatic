package com.techtrest.privacywidget.ui.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import com.techtrest.privacywidget.data.model.ActionType

/**
 * Helper for creating and launching Android settings intents.
 * Includes fallback handling for intents that may not resolve on all devices.
 */
object IntentHelper {

    /**
     * Launch settings intent for a privacy check action.
     */
    fun launchActionIntent(
        context: Context,
        actionType: ActionType,
        packageName: String? = null
    ) {
        val intent = createIntent(actionType, packageName)
        if (intent == null) {
            showToast(context, "Unable to open settings")
            return
        }

        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            val fallback = createFallbackIntent(actionType)
            if (fallback != null) {
                try {
                    context.startActivity(fallback)
                    showToast(context, "Opening general settings (specific page not available)")
                } catch (e2: ActivityNotFoundException) {
                    showToast(context, "Unable to open settings on this device")
                }
            } else {
                showToast(context, "Unable to open settings on this device")
            }
        }
    }

    private fun createIntent(actionType: ActionType, packageName: String?): Intent? {
        return when (actionType) {
            ActionType.OPEN_APP_SETTINGS -> {
                if (packageName == null) return null
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }

            ActionType.NOTIFICATION_LISTENER -> {
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }

            ActionType.ACCESSIBILITY_SETTINGS -> {
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }

            ActionType.DEVICE_ADMIN_SETTINGS -> {
                Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }

            ActionType.DEFAULT_APPS_SETTINGS -> {
                Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }

            ActionType.PRIVACY_SETTINGS -> {
                Intent(Settings.ACTION_PRIVACY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }

            ActionType.NETWORK_SETTINGS -> {
                Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }

            ActionType.LOCATION_SETTINGS -> {
                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }

            ActionType.SECURITY_SETTINGS -> {
                Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
        }
    }

    private fun createFallbackIntent(actionType: ActionType): Intent? {
        return when (actionType) {
            ActionType.OPEN_APP_SETTINGS,
            ActionType.NOTIFICATION_LISTENER,
            ActionType.ACCESSIBILITY_SETTINGS,
            ActionType.DEVICE_ADMIN_SETTINGS,
            ActionType.PRIVACY_SETTINGS,
            ActionType.NETWORK_SETTINGS,
            ActionType.LOCATION_SETTINGS,
            ActionType.SECURITY_SETTINGS -> {
                Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }

            ActionType.DEFAULT_APPS_SETTINGS -> {
                Intent(Settings.ACTION_APPLICATION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
        }
    }

    private fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
