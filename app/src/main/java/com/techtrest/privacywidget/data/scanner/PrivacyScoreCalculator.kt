package com.techtrest.privacywidget.data.scanner

import com.techtrest.privacywidget.data.model.PrivacyIssue
import com.techtrest.privacywidget.data.model.PrivacyScore
import kotlin.math.max

object PrivacyScoreCalculator {

    private const val MAX_SCORE = 100

    fun calculateScore(issues: List<PrivacyIssue>): PrivacyScore {
        val totalDeductions = issues
            .filter { !it.isSecure }
            .sumOf { it.pointDeduction }

        // Floor at 0 - score cannot go below 0
        val score = max(0, MAX_SCORE - totalDeductions)

        return PrivacyScore(
            score = score,
            maxScore = MAX_SCORE,
            issues = issues,
            scanTimestamp = System.currentTimeMillis()
        )
    }

    fun getScoreRating(score: Int): ScoreRating {
        return when {
            score >= 85 -> ScoreRating.EXCELLENT
            score >= 70 -> ScoreRating.GOOD
            score >= 50 -> ScoreRating.FAIR
            score >= 30 -> ScoreRating.POOR
            else -> ScoreRating.CRITICAL
        }
    }

    enum class ScoreRating(val displayName: String, val description: String) {
        EXCELLENT("Excellent Privacy", "Strong privacy protection"),
        GOOD("Good Privacy", "Good privacy with minor issues"),
        FAIR("Fair Privacy", "Fair privacy, room for improvement"),
        POOR("Poor Privacy", "Poor privacy, needs attention"),
        CRITICAL("Critical Privacy Issues", "Critical issues need fixing")
    }
}
