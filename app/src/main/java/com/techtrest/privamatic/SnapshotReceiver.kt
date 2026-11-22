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
import com.techtrest.privamatic.data.scanner.PrivacyScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

/**
 * The daily safety-net snapshot: every finished scan already sets today's history point
 * (PrivacySnapshotRepository.recordScan); this alarm adds a scan on days with no other one.
 *
 * One one-shot alarm, armed for the next 22:00 local time with setAndAllowWhileIdle: inexact,
 * needs no exact-alarm permission, and may fire in Doze. Android 12+ caps its window at one hour,
 * so it lands between 22:00 and 23:00. Each delivery arms the next night's alarm. Re-armed after
 * 22:00 (boot, force-stop) on a day with no point, it fires at once instead; still one a night.
 *
 * Runs a full scan independently — does not depend on ViewModel or any UI state.
 * Uses the same scan → TrustedAppsAdjuster pattern as PrivacyWidgetProvider.
 */
class SnapshotReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != PrivacyWidgetProvider.ACTION_MIDNIGHT_SNAPSHOT) return

        // The alarm that just fired is gone; arm tomorrow's first, so a failed scan can't lose it.
        // This also replaces the old daily repeating alarm on the first night after an update.
        scheduleNextSnapshot(context)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val scanner = PrivacyScanner(context)
                val rawScore = scanner.performCompleteScan()
                val trusted = TrustedAppsRepository(context).trustedPackages.first()
                val adjustedScore = TrustedAppsAdjuster.computeAdjustedScore(rawScore, trusted)
                PrivacySnapshotRepository(context).recordScan(adjustedScore)
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.e(TAG, "Safety-net snapshot failed", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "SnapshotReceiver"
        private const val ALARM_REQUEST_CODE = 1002

        /** Local hour the safety-net alarm is armed for; it may land up to an hour later. */
        internal const val SAFETY_NET_HOUR = 22

        /**
         * App start, boot and app update: arms the safety-net alarm only when none is pending.
         * Re-arming a pending one would cancel it, so an app start after 22:00 and before it
         * fired would lose that night's scan. A force-stop or reboot removes the alarm and its
         * PendingIntent; the alarm's own delivery re-arms before anything else.
         *
         * Armed after 22:00 on a day with no history point, it fires now instead (see
         * [armTriggerAt]): a reboot or force-stop before tonight's alarm fired would otherwise
         * skip that night's scan. Today's point is read off the main thread, so tomorrow's alarm
         * is armed first and replaced only if needed; a receiver passes itself to stay alive.
         */
        fun ensureSnapshotAlarm(context: Context, receiver: BroadcastReceiver? = null) {
            if (!needsArm(isAlarmPending(context))) {
                if (BuildConfig.DEBUG) Log.d(TAG, "Snapshot alarm already pending, left alone")
                return
            }
            val now = System.currentTimeMillis()
            scheduleAt(context, nextTriggerAt(now))
            if (!isPastTonightsHour(now)) return

            val appContext = context.applicationContext
            val pendingResult = receiver?.goAsync()
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                try {
                    val todayHasPoint = PrivacySnapshotRepository(appContext).hasPointToday()
                    val triggerAt = armTriggerAt(now, todayHasPoint)
                    if (triggerAt != nextTriggerAt(now)) scheduleAt(appContext, triggerAt)
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) Log.e(TAG, "Checking today's point failed; alarm stays at tomorrow", e)
                } finally {
                    pendingResult?.finish()
                }
            }
        }

        internal fun needsArm(alarmPending: Boolean): Boolean = !alarmPending

        /** The next [SAFETY_NET_HOUR]:00 local time strictly after [now]. */
        internal fun nextTriggerAt(now: Long): Long = Calendar.getInstance().apply {
            timeInMillis = todayAt(now, SAFETY_NET_HOUR)
            if (timeInMillis <= now) add(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis

        private fun todayAt(now: Long, hour: Int): Long = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        /**
         * When app start, boot or update arms a missing alarm: after [SAFETY_NET_HOUR]:00 on a day
         * with no point, tonight's alarm was lost (reboot or force-stop before it fired), so it
         * fires now; otherwise at the next [SAFETY_NET_HOUR]:00. Its delivery then arms tomorrow.
         */
        internal fun armTriggerAt(now: Long, todayHasPoint: Boolean): Long =
            if (isPastTonightsHour(now) && !todayHasPoint) now else nextTriggerAt(now)

        /** True from today's [SAFETY_NET_HOUR]:00 local time until midnight. */
        internal fun isPastTonightsHour(now: Long): Boolean = todayAt(now, SAFETY_NET_HOUR) <= now

        /** Arms (or replaces) the one safety-net alarm for the next 22:00. */
        private fun scheduleNextSnapshot(context: Context) =
            scheduleAt(context, nextTriggerAt(System.currentTimeMillis()))

        private fun scheduleAt(context: Context, triggerAt: Long) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                buildPendingIntent(context)
            )
            if (BuildConfig.DEBUG) Log.d(TAG, "Snapshot alarm scheduled for ${Date(triggerAt)}")
        }

        /** True while the alarm's PendingIntent exists, the same test as the widget alarm. */
        private fun isAlarmPending(context: Context): Boolean =
            PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                buildIntent(context),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            ) != null

        private fun buildIntent(context: Context): Intent =
            Intent(context, SnapshotReceiver::class.java).apply {
                action = PrivacyWidgetProvider.ACTION_MIDNIGHT_SNAPSHOT
            }

        private fun buildPendingIntent(context: Context): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                buildIntent(context),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
    }
}
