package com.techtrest.privamatic

import com.techtrest.privamatic.data.TrustedAppsAdjuster
import com.techtrest.privamatic.data.model.PrivacyCheck
import com.techtrest.privamatic.data.model.PrivacyIssue
import com.techtrest.privamatic.data.scanner.PrivacyScoreCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class TrustedAppsAdjusterTest {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Builds a raw scan issue the way the checkers do: capped per-package deduction. */
    private fun flaggedIssue(check: PrivacyCheck, packages: List<String>) = PrivacyIssue(
        check = check,
        isSecure = false,
        currentStatus = "Detected",
        customPointDeduction = check.cappedDeductionFor(packages.size),
        flaggedPackages = packages
    )

    private fun adjustedDeduction(issue: PrivacyIssue, trusted: Set<String>): Int {
        val raw = PrivacyScoreCalculator.calculateScore(listOf(issue))
        val adjusted = TrustedAppsAdjuster.computeAdjustedScore(raw, trusted)
        return adjusted.issues.single().pointDeduction
    }

    // -------------------------------------------------------------------------
    // H1 regression: trusting an app must never increase a deduction
    // -------------------------------------------------------------------------

    @Test
    fun oldTargetSdk_trustingOneOfTenCappedApps_keepsCapAt5() {
        // 10 apps × 1 pt capped at 5; trusting one leaves 9 untrusted — still capped at 5,
        // never the uncapped 9 that made the score drop for trusting an app.
        val packages = (1..10).map { "com.old.app$it" }
        val issue = flaggedIssue(PrivacyCheck.OLD_TARGET_SDK, packages)
        assertEquals(5, adjustedDeduction(issue, setOf("com.old.app1")))
    }

    @Test
    fun oldTargetSdk_trustingBelowCap_deductsPerUntrustedApp() {
        // 3 apps, 1 trusted → 2 untrusted × 1 pt, under the cap
        val packages = listOf("com.old.a", "com.old.b", "com.old.c")
        val issue = flaggedIssue(PrivacyCheck.OLD_TARGET_SDK, packages)
        assertEquals(2, adjustedDeduction(issue, setOf("com.old.a")))
    }

    @Test
    fun oldTargetSdk_allTrusted_deductsZero() {
        val packages = listOf("com.old.a", "com.old.b")
        val issue = flaggedIssue(PrivacyCheck.OLD_TARGET_SDK, packages)
        assertEquals(0, adjustedDeduction(issue, packages.toSet()))
    }

    // -------------------------------------------------------------------------
    // M3 regression: untrusting restores the raw deduction without a rescan
    // -------------------------------------------------------------------------

    @Test
    fun oldTargetSdk_emptyTrustedSet_returnsRawCappedDeduction() {
        // The raw score now contains ALL flagged apps, so adjusting with an empty
        // trusted set (i.e. after untrusting everything) restores the full capped value.
        val packages = (1..7).map { "com.old.app$it" }
        val issue = flaggedIssue(PrivacyCheck.OLD_TARGET_SDK, packages)
        assertEquals(5, adjustedDeduction(issue, emptySet()))
    }

    // -------------------------------------------------------------------------
    // Uncapped checks keep their existing per-package behaviour
    // -------------------------------------------------------------------------

    @Test
    fun backgroundLocation_uncapped_trustingOneDeductsPerRemainingApp() {
        // 3 apps × 4 pts = 12 raw; trusting one → 2 × 4 = 8 (no cap on this check)
        val packages = listOf("com.loc.a", "com.loc.b", "com.loc.c")
        val issue = flaggedIssue(PrivacyCheck.BACKGROUND_LOCATION_APPS, packages)
        assertEquals(8, adjustedDeduction(issue, setOf("com.loc.a")))
    }

    @Test
    fun cappedDeductionFor_matchesCheckerAndAdjusterExpectations() {
        assertEquals(3, PrivacyCheck.OLD_TARGET_SDK.cappedDeductionFor(3))
        assertEquals(5, PrivacyCheck.OLD_TARGET_SDK.cappedDeductionFor(5))
        assertEquals(5, PrivacyCheck.OLD_TARGET_SDK.cappedDeductionFor(12))
        assertEquals(20, PrivacyCheck.BACKGROUND_LOCATION_APPS.cappedDeductionFor(5))
    }
}
