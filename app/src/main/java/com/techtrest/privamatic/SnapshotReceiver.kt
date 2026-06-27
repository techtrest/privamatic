package com.techtrest.privamatic

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.techtrest.privamatic.data.PrivacySnapshotRepository
import com.techtrest.privamatic.data.TrustedAppsAdjuster
import com.techtrest.privamatic.data.TrustedAppsRepository
import com.techtrest.privamatic.data.model.CheckDeduction
import com.techtrest.privamatic.data.scanner.PrivacyScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Receives ACTION_MIDNIGHT_SNAPSHOT at 23:50 each day and records a privacy
 * score snapshot to the local Room database.
 *
 * Runs a full scan independently — does not depend on ViewModel or any UI state.
 * Uses the same scan → TrustedAppsAdjuster pattern as PrivacyWidgetProvider.
 */
class SnapshotReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != PrivacyWidgetProvider.ACTION_MIDNIGHT_SNAPSHOT) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val scanner = PrivacyScanner(context)
                val rawScore = scanner.performCompleteScan()
                val trusted = TrustedAppsRepository(context).trustedPackages.first()
                val adjustedScore = TrustedAppsAdjuster.computeAdjustedScore(rawScore, trusted)

                val deductions = adjustedScore.issues
                    .filter { it.pointDeduction > 0 }
                    .map { CheckDeduction(it.check.name, it.pointDeduction) }

                PrivacySnapshotRepository(context).recordSnapshot(
                    score = adjustedScore.score,
                    deductions = deductions
                )
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.e(TAG, "Midnight snapshot failed", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "SnapshotReceiver"
        private const val ALARM_REQUEST_CODE = 1002

        /**
         * Schedules a daily RTC_WAKEUP alarm at 23:50.
         * Safe to call multiple times — subsequent calls update the existing alarm.
         */
        fun scheduleMidnightSnapshot(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 50)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                buildPendingIntent(context)
            )

            if (BuildConfig.DEBUG) Log.d(TAG, "Midnight snapshot alarm scheduled for ${calendar.time}")
        }

        fun cancelMidnightSnapshot(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(buildPendingIntent(context))
        }

        private fun buildPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, SnapshotReceiver::class.java).apply {
                action = PrivacyWidgetProvider.ACTION_MIDNIGHT_SNAPSHOT
            }
            return PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
