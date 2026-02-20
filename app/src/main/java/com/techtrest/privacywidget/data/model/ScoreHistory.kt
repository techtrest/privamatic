package com.techtrest.privacywidget.data.model

/**
 * Tracks the current score against today's daily baseline, enabling a cumulative
 * day-over-day change indicator (e.g. "↑16 pts" if the score rose from 75 to 91 today).
 *
 * [dailyBaselineScore] is the score recorded at the start of the current calendar day.
 * On the very first scan ever it equals [currentScore] (scoreDelta = 0 until something changes).
 * On subsequent days it is the previous day's closing score.
 *
 * [dailyBaselineTimestamp] is the midnight timestamp of the day the baseline was set.
 * [lastUpdateTimestamp] is the wall-clock time of the most recent [recordScore] call.
 */
data class ScoreHistory(
    val currentScore: Int,
    val dailyBaselineScore: Int?,
    val dailyBaselineTimestamp: Long,
    val lastUpdateTimestamp: Long
) {
    /**
     * Cumulative points gained or lost since the daily baseline.
     * Positive means improvement, negative means regression.
     */
    val scoreDelta: Int?
        get() = dailyBaselineScore?.let { currentScore - it }
}
