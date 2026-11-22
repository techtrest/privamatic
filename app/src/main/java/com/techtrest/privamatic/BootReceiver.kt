package com.techtrest.privamatic

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.techtrest.privamatic.BuildConfig

/**
 * Receives BOOT_COMPLETED and restores the periodic update alarm.
 *
 * AlarmManager schedules do not survive a reboot, so we must re-register
 * them here. When the widget alarm had to be re-armed we also fire an
 * immediate update so the widget shows fresh data rather than waiting 6 hours.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        if (BuildConfig.DEBUG) Log.d(TAG, "Boot completed – restoring widget alarm and requesting immediate update")

        // Snapshot recording must resume regardless of whether a widget is placed
        SnapshotReceiver.ensureSnapshotAlarm(context, this)

        // Android 15+ also sends BOOT_COMPLETED on app starts after a force-stop. Re-arm only
        // when a widget is placed and its alarm is gone (a real reboot clears it); re-arming a
        // live alarm would push its next fire 6 hours out. Refresh only when we re-armed, so a
        // BOOT_COMPLETED on a normal start adds no scan.
        if (PrivacyWidgetProvider.rearmPeriodicUpdatesIfPlaced(context)) {
            PrivacyWidgetProvider.requestImmediateUpdate(context)
        } else if (BuildConfig.DEBUG) {
            Log.d(TAG, "Widget alarm already set or no widget placed, left alone")
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
