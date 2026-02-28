package com.techtrest.privacywidget.data.scanner

import com.techtrest.privacywidget.data.model.PrivacyIssue
import com.techtrest.privacywidget.data.model.PrivacyScore

object PrivacyScoreCalculator {

    private const val MAX_SCORE = 100
    private const val MAX_MANUAL_CHECK_POINTS = 20

    /**
     * Calculates the privacy score based on detected issues and manual check completion.
     *
     * Starts from a maximum score of 100 and deducts points for each insecure issue.
     * Adds points for completed (non-overdue) manual checks.
     * The score is floored at 0 and cannot go negative.
     *
     * @param issues List of privacy issues detected by scanners
     * @param manualCheckPoints Points earned from completed manual checks (0-15)
     * @return Calculated privacy score with all issues and timestamp
     */
    fun calculateScore(issues: List<PrivacyIssue>, manualCheckPoints: Int = 0): PrivacyScore {
        val totalDeductions = issues
            .filter { !it.isSecure }
            .sumOf { it.pointDeduction }

        // Calculate deductions from incomplete manual checks
        val manualCheckDeductions = MAX_MANUAL_CHECK_POINTS - manualCheckPoints

        // Start with MAX_SCORE (100), subtract both privacy issues and manual check deductions
        // Fresh install: 100 - 0 - 15 = 85/100 (all checks incomplete)
        // All completed: 100 - 0 - 0 = 100/100 (full points)
        val score = (MAX_SCORE - totalDeductions - manualCheckDeductions).coerceIn(0, MAX_SCORE)

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

    enum class ScoreRating(val displayName: String, val subtitle: String, val scoreRange: String) {
        EXCELLENT("Excellent", "Strong protection", "85-100"),
        GOOD("Good", "Good, with minor issues", "70-84"),
        FAIR("Fair", "Room for improvement", "50-69"),
        POOR("Needs Attention", "Several issues detected", "30-49"),
        CRITICAL("Critical", "Immediate action required", "0-29")
    }
}
