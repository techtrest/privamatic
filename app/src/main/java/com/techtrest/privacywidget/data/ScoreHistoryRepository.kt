package com.techtrest.privacywidget.data

import android.content.Context
import com.techtrest.privacywidget.data.model.ScoreHistory
import java.util.Calendar

/**
 * Persists daily cumulative score history using SharedPreferences.
 *
 * Recording logic:
 * - First scan ever: seed both currentScore and dailyBaselineScore with the initial value
 *   so scoreDelta = 0 until the score actually changes.
 * - Same calendar day: update currentScore; daily baseline is preserved so the
 *   cumulative delta for today keeps accumulating (e.g. 75→83→91 shows ↑16).
 * - New calendar day: promote the previous currentScore to dailyBaselineScore,
 *   set dailyBaselineTimestamp to today's midnight, save the new score.
 */
class ScoreHistoryRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun recordScore(newScore: Int): ScoreHistory {
        val savedCurrent = savedCurrentScore
        val now = System.currentTimeMillis()
        val todayMidnight = midnightOf(now)

        return when {
            savedCurrent == null -> {
                // First scan ever — seed the baseline with the initial score so that
                // same-day changes (e.g. after a privacy setting is changed) produce a
                // non-zero scoreDelta immediately, without requiring a prior day's record.
                prefs.edit()
                    .putInt(KEY_CURRENT_SCORE, newScore)
                    .putInt(KEY_DAILY_BASELINE_SCORE, newScore)
                    .putLong(KEY_DAILY_BASELINE_TIMESTAMP, todayMidnight)
                    .putLong(KEY_LAST_UPDATE_TIMESTAMP, now)
                    .apply()
                ScoreHistory(
                    currentScore = newScore,
                    dailyBaselineScore = newScore,
                    dailyBaselineTimestamp = todayMidnight,
                    lastUpdateTimestamp = now
                )
            }
            isSameDay(savedDailyBaselineTimestamp, now) -> {
                // Same calendar day — keep baseline, update current score only
                prefs.edit()
                    .putInt(KEY_CURRENT_SCORE, newScore)
                    .putLong(KEY_LAST_UPDATE_TIMESTAMP, now)
                    .apply()
                ScoreHistory(
                    currentScore = newScore,
                    dailyBaselineScore = savedDailyBaselineScore,
                    dailyBaselineTimestamp = savedDailyBaselineTimestamp,
                    lastUpdateTimestamp = now
                )
            }
            else -> {
                // New calendar day — yesterday's closing score becomes today's baseline
                prefs.edit()
                    .putInt(KEY_DAILY_BASELINE_SCORE, savedCurrent)
                    .putInt(KEY_CURRENT_SCORE, newScore)
                    .putLong(KEY_DAILY_BASELINE_TIMESTAMP, todayMidnight)
                    .putLong(KEY_LAST_UPDATE_TIMESTAMP, now)
                    .apply()
                ScoreHistory(
                    currentScore = newScore,
                    dailyBaselineScore = savedCurrent,
                    dailyBaselineTimestamp = todayMidnight,
                    lastUpdateTimestamp = now
                )
            }
        }
    }

    private val savedCurrentScore: Int?
        get() = if (prefs.contains(KEY_CURRENT_SCORE)) prefs.getInt(KEY_CURRENT_SCORE, 0) else null

    private val savedDailyBaselineScore: Int?
        get() = if (prefs.contains(KEY_DAILY_BASELINE_SCORE)) prefs.getInt(KEY_DAILY_BASELINE_SCORE, 0) else null

    private val savedDailyBaselineTimestamp: Long
        get() = prefs.getLong(KEY_DAILY_BASELINE_TIMESTAMP, 0L)

    companion object {
        private const val PREFS_NAME = "score_history"
        private const val KEY_CURRENT_SCORE = "current_score"
        private const val KEY_DAILY_BASELINE_SCORE = "daily_baseline_score"
        private const val KEY_DAILY_BASELINE_TIMESTAMP = "daily_baseline_timestamp"
        private const val KEY_LAST_UPDATE_TIMESTAMP = "last_update_timestamp"

        /** Returns the Unix timestamp (ms) of midnight at the start of the day containing [ms]. */
        private fun midnightOf(ms: Long): Long = Calendar.getInstance().apply {
            timeInMillis = ms
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        /** True when [baselineMs] and [nowMs] fall on the same calendar day. */
        private fun isSameDay(baselineMs: Long, nowMs: Long): Boolean =
            midnightOf(baselineMs) == midnightOf(nowMs)
    }
}
