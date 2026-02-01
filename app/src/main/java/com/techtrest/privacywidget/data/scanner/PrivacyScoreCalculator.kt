package com.techtrest.privacywidget.data.scanner

import com.techtrest.privacywidget.data.model.PrivacyIssue
import com.techtrest.privacywidget.data.model.PrivacyScore
import kotlin.math.max

object PrivacyScoreCalculator {

    private const val MAX_SCORE = 100

    /**
     * Calculates the privacy score based on detected issues.
     *
     * Starts from a maximum score of 100 and deducts points for each insecure issue.
     * The score is floored at 0 and cannot go negative.
     *
     * @param issues List of privacy issues detected by scanners
     * @return Calculated privacy score with all issues and timestamp
     */
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

    /**
     * Determines the privacy rating category based on the numeric score.
     *
     * Score ranges:
     * - 85-100: Excellent
     * - 70-84: Good
     * - 50-69: Fair
     * - 30-49: Poor
     * - 0-29: Critical
     *
     * @param score The privacy score value (0-100)
     * @return The corresponding rating category
     */
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
