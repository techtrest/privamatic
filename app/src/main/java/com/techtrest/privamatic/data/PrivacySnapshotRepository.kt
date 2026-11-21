package com.techtrest.privamatic.data

import android.content.Context
import android.util.Log
import com.techtrest.privamatic.BuildConfig
import com.techtrest.privamatic.data.model.CheckDeduction
import com.techtrest.privamatic.data.model.PrivacyScore
import com.techtrest.privamatic.data.model.PrivacySnapshot
import kotlinx.coroutines.CancellationException
import java.util.Calendar

enum class HistoryFilter { WEEK, MONTH, THREE_MONTHS, SIX_MONTHS, YEAR, ALL }

class PrivacySnapshotRepository internal constructor(
    private val dao: PrivacySnapshotDao,
    private val clock: () -> Long
) {
    constructor(context: Context) :
        this(PrivacyDatabase.getInstance(context).snapshotDao(), System::currentTimeMillis)

    /**
     * The one place a finished scan records history: the app scan (PrivacyViewModel), the widget
     * scan (PrivacyWidgetProvider) and the nightly safety-net scan (SnapshotReceiver) all call it
     * with the trust-adjusted score. A failed write is logged and swallowed, so the caller's own
     * work (the score on screen, the widget) goes on.
     */
    suspend fun recordScan(adjustedScore: PrivacyScore) {
        try {
            recordSnapshot(adjustedScore.score, deductionsOf(adjustedScore))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Recording today's history point failed", e)
        }
    }

    /** Sets today's point (local calendar day); a later scan the same day replaces it. */
    internal suspend fun recordSnapshot(score: Int, deductions: List<CheckDeduction>) {
        val now = clock()
        val dayStart = midnightOf(now)
        // The next local midnight, not dayStart + 24 h: DST days have 23 or 25 hours.
        val dayEnd = Calendar.getInstance().apply {
            timeInMillis = dayStart
            add(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis
        dao.setPointForDay(
            dayStart,
            dayEnd,
            PrivacySnapshot(
                timestamp = now,
                score = score,
                deductionsJson = CheckDeduction.encode(deductions)
            )
        )
    }

    suspend fun getSnapshots(filter: HistoryFilter): List<PrivacySnapshot> {
        if (filter == HistoryFilter.ALL) return dao.getAllSnapshots()
        val since = System.currentTimeMillis() - filterDays(filter) * DAY_MS
        return dao.getSnapshotsSince(since)
    }

    suspend fun getEarliestTimestamp(): Long? = dao.getEarliestTimestamp()

    /** Any snapshot at all, whatever the selected range. */
    suspend fun hasAnySnapshot(): Boolean = dao.getEarliestTimestamp() != null

    suspend fun clearAll() = dao.deleteAll()

    private fun filterDays(filter: HistoryFilter): Long = when (filter) {
        HistoryFilter.WEEK -> 7L
        HistoryFilter.MONTH -> 30L
        HistoryFilter.THREE_MONTHS -> 90L
        HistoryFilter.SIX_MONTHS -> 180L
        HistoryFilter.YEAR -> 365L
        HistoryFilter.ALL -> 0L
    }

    companion object {
        private const val TAG = "PrivacySnapshotRepo"
        private const val DAY_MS = 24L * 60 * 60 * 1000

        internal fun deductionsOf(adjustedScore: PrivacyScore): List<CheckDeduction> =
            adjustedScore.issues
                .filter { it.pointDeduction > 0 }
                .map { CheckDeduction(it.check.name, it.pointDeduction) }

        private fun midnightOf(ms: Long): Long = Calendar.getInstance().apply {
            timeInMillis = ms
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
