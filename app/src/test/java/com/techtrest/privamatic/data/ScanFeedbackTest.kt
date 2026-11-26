package com.techtrest.privamatic.data

import com.techtrest.privamatic.data.model.ManualCheckType
import com.techtrest.privamatic.data.model.PrivacyCheck
import com.techtrest.privamatic.data.model.PrivacyIssue
import com.techtrest.privamatic.data.model.PrivacyScore
import com.techtrest.privamatic.data.scanner.PrivacyScoreCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanFeedbackTest {

    private val lock = PrivacyCheck.SCREEN_LOCK
    private val dns = PrivacyCheck.PRIVATE_DNS
    private val vpn = PrivacyCheck.VPN_CONNECTION
    private val usb = PrivacyCheck.USB_DEBUGGING

    private fun fail(check: PrivacyCheck, points: Int) =
        PrivacyIssue(check, isSecure = false, currentStatus = "", customPointDeduction = points)

    private fun pass(check: PrivacyCheck) = PrivacyIssue(check, isSecure = true, currentStatus = "")

    private fun unknown(check: PrivacyCheck) = PrivacyIssue(
        check, isSecure = false, currentStatus = "", customPointDeduction = 0, isUnknown = true
    )

    private fun score(vararg issues: PrivacyIssue, manual: Int = 15): PrivacyScore =
        PrivacyScoreCalculator.calculateScore(issues.toList(), manual)

    private fun between(
        before: PrivacyScore,
        after: PrivacyScore,
        trigger: UserScan = UserScan.Rescan
    ) = ScanFeedback.between(before, after, trigger)

    // Better

    @Test
    fun `one fix names the check`() = assertEquals(
        ScanOutcome.CheckFixed(lock, 11, 94),
        between(score(fail(lock, 11), fail(dns, 6)), score(pass(lock), fail(dns, 6)))
    )

    @Test
    fun `several fixes are counted`() = assertEquals(
        ScanOutcome.ChecksFixed(2, 17, 100),
        between(score(fail(lock, 11), fail(dns, 6)), score(pass(lock), pass(dns)))
    )

    @Test
    fun `a partial improvement is not called fixed`() = assertEquals(
        ScanOutcome.CheckImproved(dns, 3, 97),
        between(score(fail(dns, 6)), score(fail(dns, 3)))
    )

    @Test
    fun `a fix together with a partial improvement reports only the points`() = assertEquals(
        ScanOutcome.Gain(14, 97, hasLosses = false),
        between(score(fail(lock, 11), fail(dns, 6)), score(pass(lock), fail(dns, 3)))
    )

    @Test
    fun `fail to unknown is better but not fixed`() = assertEquals(
        ScanOutcome.CheckImproved(usb, 4, 100),
        between(score(fail(usb, 4)), score(unknown(usb)))
    )

    @Test
    fun `a gain with a loss names neither`() = assertEquals(
        ScanOutcome.Gain(8, 97, hasLosses = true),
        between(score(fail(lock, 11), pass(dns)), score(pass(lock), fail(dns, 3)))
    )

    // Worse

    @Test
    fun `one worse check is named`() = assertEquals(
        ScanOutcome.Loss(dns, 3, 97),
        between(score(pass(dns)), score(fail(dns, 3)))
    )

    @Test
    fun `several worse checks are not named`() = assertEquals(
        ScanOutcome.Loss(null, 9, 91),
        between(score(pass(dns), pass(vpn)), score(fail(dns, 6), fail(vpn, 3)))
    )

    @Test
    fun `a net loss with something better names neither`() = assertEquals(
        ScanOutcome.Loss(null, 3, 94),
        between(score(pass(dns), fail(vpn, 3)), score(fail(dns, 6), pass(vpn)))
    )

    @Test
    fun `unknown to fail is worse`() = assertEquals(
        ScanOutcome.Loss(usb, 4, 96),
        between(score(unknown(usb)), score(fail(usb, 4)))
    )

    @Test
    fun `a review falling due is an unnamed loss`() = assertEquals(
        ScanOutcome.Loss(null, 5, 95),
        between(score(pass(dns)), score(pass(dns), manual = 10))
    )

    @Test
    fun `Ad ID reset shows the points it costs`() = assertEquals(
        ScanOutcome.Loss(PrivacyCheck.ADVERTISING_ID, 5, 95),
        between(
            score(pass(PrivacyCheck.ADVERTISING_ID)),
            score(fail(PrivacyCheck.ADVERTISING_ID, 5)),
            UserScan.AdIdReset
        )
    )

    // Net 0

    @Test
    fun `mixed net 0 counts both sides`() = assertEquals(
        ScanOutcome.Mixed(better = 1, worse = 1, exact = true, score = 97),
        between(score(fail(dns, 3), pass(vpn)), score(pass(dns), fail(vpn, 3)))
    )

    @Test
    fun `mixed net 0 with partial changes is not exact`() = assertEquals(
        ScanOutcome.Mixed(better = 1, worse = 1, exact = false, score = 91),
        between(score(fail(dns, 6), fail(vpn, 3)), score(fail(dns, 3), fail(vpn, 6)))
    )

    @Test
    fun `mixed net 0 wins over not fixed yet`() = assertEquals(
        ScanOutcome.Mixed(better = 1, worse = 1, exact = true, score = 86),
        between(
            score(fail(lock, 11), fail(dns, 3), pass(vpn)),
            score(fail(lock, 11), pass(dns), fail(vpn, 3)),
            UserScan.CheckAgain(lock)
        )
    )

    @Test
    fun `no change after a rescan`() = assertEquals(
        ScanOutcome.NoChange(94),
        between(score(fail(dns, 6)), score(fail(dns, 6)))
    )

    @Test
    fun `no change after a fix deep link is not fixed yet`() = assertEquals(
        ScanOutcome.NotFixedYet(dns, 94),
        between(score(fail(dns, 6)), score(fail(dns, 6)), UserScan.FixReturn(dns))
    )

    @Test
    fun `no change after check again is not fixed yet`() = assertEquals(
        ScanOutcome.NotFixedYet(dns, 94),
        between(score(fail(dns, 6)), score(fail(dns, 6)), UserScan.CheckAgain(dns))
    )

    @Test
    fun `no change back from a manual review's settings is no change`() = assertEquals(
        ScanOutcome.NoChange(94),
        between(score(fail(dns, 6)), score(fail(dns, 6)), UserScan.FixReturn(null))
    )

    @Test
    fun `a check that cost nothing is never not fixed yet`() = assertEquals(
        ScanOutcome.NoChange(100),
        between(score(fail(dns, 0)), score(fail(dns, 0)), UserScan.FixReturn(dns))
    )

    // Manual reviews and Ad ID

    @Test
    fun `manual review gain`() = assertEquals(
        ScanOutcome.ReviewDone(ManualCheckType.CAMERA_MIC_ACCESS, 5, 95),
        between(
            score(manual = 5), score(manual = 10),
            UserScan.ManualDone(ManualCheckType.CAMERA_MIC_ACCESS)
        )
    )

    @Test
    fun `a review that was not due earns no points`() = assertEquals(
        ScanOutcome.ReviewDone(ManualCheckType.UNUSED_APPS, null, 100),
        between(score(), score(), UserScan.ManualDone(ManualCheckType.UNUSED_APPS))
    )

    @Test
    fun `a review plus a fix reports only the points`() = assertEquals(
        ScanOutcome.Gain(9, 100, hasLosses = false),
        between(
            score(fail(PrivacyCheck.BACKGROUND_LOCATION_APPS, 4), manual = 10),
            score(pass(PrivacyCheck.BACKGROUND_LOCATION_APPS)),
            UserScan.ManualDone(ManualCheckType.LOCATION_ALWAYS_ON)
        )
    )

    @Test
    fun `two reviews merged into one scan are not named after one`() = assertEquals(
        ScanOutcome.Gain(10, 100, hasLosses = false),
        between(score(manual = 5), score(), UserScan.ManualDone(ManualCheckType.UNUSED_APPS))
    )

    @Test
    fun `Ad ID confirm`() = assertEquals(
        ScanOutcome.AdIdConfirmed(5, 100),
        between(
            score(fail(PrivacyCheck.ADVERTISING_ID, 5)),
            score(pass(PrivacyCheck.ADVERTISING_ID)),
            UserScan.AdIdConfirmed
        )
    )

    @Test
    fun `an Ad ID check that can't be read after confirming is improved, not confirmed`() = assertEquals(
        ScanOutcome.CheckImproved(PrivacyCheck.ADVERTISING_ID, 5, 100),
        between(
            score(fail(PrivacyCheck.ADVERTISING_ID, 5)),
            score(unknown(PrivacyCheck.ADVERTISING_ID)),
            UserScan.AdIdConfirmed
        )
    )

    @Test
    fun `an Ad ID check that still costs points after confirming is improved, not confirmed`() = assertEquals(
        ScanOutcome.CheckImproved(PrivacyCheck.ADVERTISING_ID, 3, 98),
        between(
            score(fail(PrivacyCheck.ADVERTISING_ID, 5)),
            score(fail(PrivacyCheck.ADVERTISING_ID, 2)),
            UserScan.AdIdConfirmed
        )
    )

    // A check on one side only

    @Test
    fun `a check that disappears is points only, never 0 checks fixed`() = assertEquals(
        ScanOutcome.Gain(6, 100, hasLosses = false),
        between(score(fail(dns, 6)), score())
    )

    @Test
    fun `a check that disappears together with a fix doesn't lend its points to it`() = assertEquals(
        ScanOutcome.Gain(17, 100, hasLosses = false),
        between(score(fail(lock, 11), fail(dns, 6)), score(pass(lock)))
    )

    @Test
    fun `two checks that disappear are not called fixed`() = assertEquals(
        ScanOutcome.Gain(17, 100, hasLosses = false),
        between(score(fail(lock, 11), fail(dns, 6)), score())
    )

    @Test
    fun `a failing check that appears is worse, also when another improves by the same points`() = assertEquals(
        ScanOutcome.Mixed(better = 1, worse = 1, exact = false, score = 94),
        between(score(fail(dns, 6)), score(fail(dns, 3), fail(vpn, 3)))
    )

    @Test
    fun `a failing check that appears while another is fixed for the same points is a new issue`() = assertEquals(
        ScanOutcome.Mixed(better = 1, worse = 1, exact = true, score = 97),
        between(score(fail(dns, 3)), score(pass(dns), fail(vpn, 3)))
    )

    @Test
    fun `a failing check that appears is named as the loss`() = assertEquals(
        ScanOutcome.Loss(vpn, 3, 97),
        between(score(pass(dns)), score(pass(dns), fail(vpn, 3)))
    )

    @Test
    fun `a check that disappears while another gets worse is neither fixed nor unchanged`() = assertEquals(
        ScanOutcome.Mixed(better = 1, worse = 1, exact = false, score = 97),
        between(score(fail(vpn, 3), pass(dns)), score(fail(dns, 3)))
    )

    @Test
    fun `a check that disappears below the clamp is no change`() = assertEquals(
        ScanOutcome.NoChange(0),
        between(score(fail(lock, 140), fail(dns, 6)), score(fail(lock, 140)), UserScan.CheckAgain(dns))
    )

    @Test
    fun `an Ad ID check that disappears is not called confirmed`() = assertEquals(
        ScanOutcome.Gain(5, 100, hasLosses = false),
        between(score(fail(PrivacyCheck.ADVERTISING_ID, 5)), score(), UserScan.AdIdConfirmed)
    )

    @Test
    fun `checks that cost nothing may come and go`() = assertEquals(
        ScanOutcome.NoChange(94),
        between(score(fail(dns, 6), pass(vpn)), score(fail(dns, 6), unknown(usb)))
    )

    @Test
    fun `no combination is ever called 0 checks fixed`() {
        // Absent, pass, unknown, 3 or 6 points: every state of two checks on both sides
        fun states(check: PrivacyCheck) =
            listOf(null, pass(check), unknown(check), fail(check, 3), fail(check, 6))
        val scores = states(dns).flatMap { a ->
            states(vpn).flatMap { b ->
                listOf(10, 15).map { manual -> score(*listOfNotNull(a, b).toTypedArray(), manual = manual) }
            }
        }
        val triggers = listOf(UserScan.Rescan, UserScan.CheckAgain(dns), UserScan.AdIdConfirmed)
        for (before in scores) for (after in scores) for (trigger in triggers) {
            val outcome = between(before, after, trigger)
            if (outcome is ScanOutcome.ChecksFixed) assertTrue("$outcome", outcome.count >= 2)
            if (outcome is ScanOutcome.CheckFixed) {
                assertTrue("$outcome", after.issues.any { it.check == outcome.check && it.isSecure })
            }
            if (after.score == before.score) {
                val costs = { s: PrivacyScore -> s.issues.filter { it.pointDeduction > 0 }.associate { it.check to it.pointDeduction } }
                if (costs(before) != costs(after)) assertTrue("$outcome", outcome is ScanOutcome.Mixed)
            }
        }
    }

    // Clamp at 0

    @Test
    fun `a fix below the clamp is no change, not +0 and not not-fixed-yet`() = assertEquals(
        ScanOutcome.NoChange(0),
        between(
            score(fail(lock, 80), fail(dns, 60)), score(fail(lock, 80), fail(dns, 30)),
            UserScan.CheckAgain(dns)
        )
    )

    @Test
    fun `a review below the clamp is no change`() = assertEquals(
        ScanOutcome.NoChange(0),
        between(
            score(fail(lock, 140), manual = 5), score(fail(lock, 140), manual = 10),
            UserScan.ManualDone(ManualCheckType.UNUSED_APPS)
        )
    )

    @Test
    fun `leaving the clamp shows the shown points only`() = assertEquals(
        ScanOutcome.CheckFixed(dns, 20, 20),
        between(score(fail(lock, 80), fail(dns, 60)), score(fail(lock, 80), pass(dns)))
    )

    // Pending requests

    private val first = score(fail(lock, 11))
    private val second = score(pass(lock))

    @Test
    fun `before survives a cancelled scan`() {
        val pending = ScanFeedback.merge(null, UserScan.FixReturn(lock), first)
        val merged = ScanFeedback.merge(pending, UserScan.CheckAgain(lock), second)
        assertEquals(PendingUserScan(first, UserScan.CheckAgain(lock)), merged)
    }

    @Test
    fun `a plain rescan keeps the cancelled scan's trigger`() {
        val done = UserScan.ManualDone(ManualCheckType.UNUSED_APPS)
        val merged = ScanFeedback.merge(PendingUserScan(first, done), UserScan.Rescan, second)
        assertEquals(PendingUserScan(first, done), merged)
    }

    @Test
    fun `a scan nobody asked for keeps the pending one`() {
        val pending = PendingUserScan(first, UserScan.Rescan)
        assertEquals(pending, ScanFeedback.merge(pending, null, second))
    }

    @Test
    fun `nothing to compare with means no feedback`() {
        assertNull(ScanFeedback.merge(null, UserScan.Rescan, null))
        assertNull(ScanFeedback.merge(null, null, first))
    }
}
