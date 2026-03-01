package com.techtrest.privamatic

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.techtrest.privamatic.BuildConfig

/**
 * Receives BOOT_COMPLETED and restores the periodic update alarm.
 *
 * AlarmManager schedules do not survive a reboot, so we must re-register
 * them here. We also fire an immediate update so the widget shows fresh
 * data as soon as the device boots rather than waiting 6 hours.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        if (BuildConfig.DEBUG) Log.d(TAG, "Boot completed – restoring widget alarm and requesting immediate update")

        // Only bother if at least one widget is actually placed on the home screen
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(
            ComponentName(context, PrivacyWidgetProvider::class.java)
        )
        if (ids.isEmpty()) return

        PrivacyWidgetProvider.schedulePeriodicUpdates(context)
        PrivacyWidgetProvider.requestImmediateUpdate(context)
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
