package com.techtrest.privamatic

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receives ACTION_MY_PACKAGE_REPLACED and restores the midnight snapshot alarm.
 *
 * AlarmManager schedules are cleared when the app is updated, so non-widget
 * users would otherwise lose daily score history recording until they next
 * open the app.
 */
class PackageReplacedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        if (BuildConfig.DEBUG) Log.d(TAG, "Package replaced – restoring midnight snapshot alarm")

        SnapshotReceiver.scheduleMidnightSnapshot(context)
    }

    companion object {
        private const val TAG = "PackageReplacedReceiver"
    }
}
