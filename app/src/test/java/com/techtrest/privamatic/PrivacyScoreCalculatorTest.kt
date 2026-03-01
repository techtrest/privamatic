package com.techtrest.privamatic

import com.techtrest.privamatic.data.model.PrivacyCheck
import com.techtrest.privamatic.data.model.PrivacyIssue
import com.techtrest.privamatic.data.model.PrivacyScore
import com.techtrest.privamatic.data.model.getSecurityIssuesCount
import com.techtrest.privamatic.data.model.getTrackingIssuesCount
import com.techtrest.privamatic.data.scanner.PrivacyScoreCalculator
import com.techtrest.privamatic.data.scanner.PrivacyScoreCalculator.ScoreRating
import org.junit.Assert.assertEquals
import org.junit.Test

class PrivacyScoreCalculatorTest {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun insecureIssue(
        check: PrivacyCheck,
        customPointDeduction: Int? = null
    ) = PrivacyIssue(
        check = check,
        isSecure = false,
        currentStatus = "Not configured",
        customPointDeduction = customPointDeduction
    )

    private fun secureIssue(check: PrivacyCheck) = PrivacyIssue(
        check = check,
        isSecure = true,
        currentStatus = "Configured"
    )

    // -------------------------------------------------------------------------
    // calculateScore()
    // -------------------------------------------------------------------------

    @Test
    fun calculateScore_noIssuesNoManualPoints_returns85() {
        val result = PrivacyScoreCalculator.calculateScore(emptyList())
        assertEquals(85, result.score)
    }

    @Test
    fun calculateScore_noIssuesFullManualPoints_returns100() {
        val result = PrivacyScoreCalculator.calculateScore(emptyList(), manualCheckPoints = 15)
        assertEquals(100, result.score)
    }

    @Test
    fun calculateScore_deductionsExceed100_floorsAtZero() {
        // 86-point custom deduction + 15 manual check deductions = 101 total → clamped to 0
        val issues = listOf(insecureIssue(PrivacyCheck.SCREEN_LOCK, customPointDeduction = 86))
        val result = PrivacyScoreCalculator.calculateScore(issues, manualCheckPoints = 0)
        assertEquals(0, result.score)
    }

    @Test
    fun calculateScore_manualPointsExceedMax_ceilsAt100() {
        // manualCheckPoints > 15 produces negative manualCheckDeductions, pushing raw score
        // above 100; coerceIn must clamp it back to 100.
        val result = PrivacyScoreCalculator.calculateScore(emptyList(), manualCheckPoints = 200)
        assertEquals(100, result.score)
    }

    @Test
    fun calculateScore_onlyInsecureIssuesContributeDeductions() {
        val issues = listOf(
            secureIssue(PrivacyCheck.VPN_CONNECTION),  // isSecure — 0 deduction
            insecureIssue(PrivacyCheck.PRIVATE_DNS)    // 6 deduction
        )
        // 100 - 6 (issues) - 15 (manual) = 79
        val result = PrivacyScoreCalculator.calculateScore(issues)
        assertEquals(79, result.score)
    }

    @Test
    fun calculateScore_manualCheckPointsDefaultsToZero() {
        // Calling without second parameter must behave identically to manualCheckPoints = 0
        val withDefault = PrivacyScoreCalculator.calculateScore(emptyList())
        val withExplicitZero = PrivacyScoreCalculator.calculateScore(emptyList(), manualCheckPoints = 0)
        assertEquals(withExplicitZero.score, withDefault.score)
        assertEquals(85, withDefault.score)
    }

    @Test
    fun calculateScore_customPointDeductionOverridesCheckDefault() {
        // VPN_CONNECTION.pointDeduction == 7; override with 3
        val issues = listOf(insecureIssue(PrivacyCheck.VPN_CONNECTION, customPointDeduction = 3))
        // 100 - 3 (custom) - 15 (manual) = 82
        val result = PrivacyScoreCalculator.calculateScore(issues)
        assertEquals(82, result.score)
    }

    // -------------------------------------------------------------------------
    // getScoreRating()
    // -------------------------------------------------------------------------

    @Test
    fun getScoreRating_100_returnsExcellent() {
        assertEquals(ScoreRating.EXCELLENT, PrivacyScoreCalculator.getScoreRating(100))
    }

    @Test
    fun getScoreRating_85_returnsExcellent() {
        assertEquals(ScoreRating.EXCELLENT, PrivacyScoreCalculator.getScoreRating(85))
    }

    @Test
    fun getScoreRating_84_returnsGood() {
        assertEquals(ScoreRating.GOOD, PrivacyScoreCalculator.getScoreRating(84))
    }

    @Test
    fun getScoreRating_70_returnsGood() {
        assertEquals(ScoreRating.GOOD, PrivacyScoreCalculator.getScoreRating(70))
    }

    @Test
    fun getScoreRating_69_returnsFair() {
        assertEquals(ScoreRating.FAIR, PrivacyScoreCalculator.getScoreRating(69))
    }

    @Test
    fun getScoreRating_50_returnsFair() {
        assertEquals(ScoreRating.FAIR, PrivacyScoreCalculator.getScoreRating(50))
    }

    @Test
    fun getScoreRating_49_returnsPoor() {
        assertEquals(ScoreRating.POOR, PrivacyScoreCalculator.getScoreRating(49))
    }

    @Test
    fun getScoreRating_30_returnsPoor() {
        assertEquals(ScoreRating.POOR, PrivacyScoreCalculator.getScoreRating(30))
    }

    @Test
    fun getScoreRating_29_returnsCritical() {
        assertEquals(ScoreRating.CRITICAL, PrivacyScoreCalculator.getScoreRating(29))
    }

    @Test
    fun getScoreRating_0_returnsCritical() {
        assertEquals(ScoreRating.CRITICAL, PrivacyScoreCalculator.getScoreRating(0))
    }

    // -------------------------------------------------------------------------
    // PrivacyScore properties
    // -------------------------------------------------------------------------

    @Test
    fun totalDeductions_sumsOnlyInsecureIssueDeductions() {
        val issues = listOf(
            insecureIssue(PrivacyCheck.VPN_CONNECTION),  // pointDeduction = 7
            secureIssue(PrivacyCheck.SCREEN_LOCK),        // pointDeduction = 0 (secure)
            insecureIssue(PrivacyCheck.PRIVATE_DNS)       // pointDeduction = 6
        )
        val privacyScore = PrivacyScore(score = 72, maxScore = 100, issues = issues)
        assertEquals(13, privacyScore.totalDeductions)
    }

    @Test
    fun secureIssues_filtersCorrectly() {
        val secure = secureIssue(PrivacyCheck.VPN_CONNECTION)
        val insecure = insecureIssue(PrivacyCheck.SCREEN_LOCK)
        val privacyScore = PrivacyScore(score = 85, maxScore = 100, issues = listOf(secure, insecure))
        assertEquals(listOf(secure), privacyScore.secureIssues)
    }

    @Test
    fun insecureIssues_filtersCorrectly() {
        val secure = secureIssue(PrivacyCheck.VPN_CONNECTION)
        val insecure = insecureIssue(PrivacyCheck.SCREEN_LOCK)
        val privacyScore = PrivacyScore(score = 85, maxScore = 100, issues = listOf(secure, insecure))
        assertEquals(listOf(insecure), privacyScore.insecureIssues)
    }

    @Test
    fun scorePercentage_calculatesCorrectly() {
        val privacyScore = PrivacyScore(score = 50, maxScore = 100, issues = emptyList())
        assertEquals(50.0f, privacyScore.scorePercentage, 0.001f)
    }

    @Test
    fun getTrackingIssuesCount_excludesSystemSecurityAndInformationalChecks() {
        val issues = listOf(
            insecureIssue(PrivacyCheck.SCREEN_LOCK),    // system security — excluded
            insecureIssue(PrivacyCheck.VPN_CONNECTION), // tracking — included
            insecureIssue(PrivacyCheck.CHATGPT_APP),    // informational — excluded
            insecureIssue(PrivacyCheck.FACEBOOK_APP)    // tracking app — included
        )
        val privacyScore = PrivacyScore(score = 60, maxScore = 100, issues = issues)
        assertEquals(2, privacyScore.getTrackingIssuesCount())
    }

    @Test
    fun getSecurityIssuesCount_includesOnlyInsecureSystemSecurityChecks() {
        val issues = listOf(
            insecureIssue(PrivacyCheck.SCREEN_LOCK),       // system security, insecure — counted
            insecureIssue(PrivacyCheck.USB_DEBUGGING),     // system security, insecure — counted
            insecureIssue(PrivacyCheck.VPN_CONNECTION),    // not system security — excluded
            secureIssue(PrivacyCheck.DEVICE_ENCRYPTION)   // system security but secure — excluded
        )
        val privacyScore = PrivacyScore(score = 60, maxScore = 100, issues = issues)
        assertEquals(2, privacyScore.getSecurityIssuesCount())
    }
}
