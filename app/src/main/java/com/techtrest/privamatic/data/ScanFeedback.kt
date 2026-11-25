package com.techtrest.privamatic.data

import com.techtrest.privamatic.data.model.ManualCheckType
import com.techtrest.privamatic.data.model.PrivacyCheck
import com.techtrest.privamatic.data.model.PrivacyIssue
import com.techtrest.privamatic.data.model.PrivacyScore

/** A scan the user asked for; its result is reported in a snackbar. App-start scans have none. */
sealed interface UserScan {
    /** Back from a settings page. [check] is the check its button fixes; null for a manual review. */
    data class FixReturn(val check: PrivacyCheck?) : UserScan
    data class CheckAgain(val check: PrivacyCheck) : UserScan
    data object Rescan : UserScan
    data class ManualDone(val type: ManualCheckType) : UserScan
    data object AdIdConfirmed : UserScan
    data object AdIdReset : UserScan
}

/** What a user scan changed. Every [score] is the shown (clamped) score; points are never 0. */
sealed interface ScanOutcome {
    val score: Int

    /** One check no longer costs anything. */
    data class CheckFixed(val check: PrivacyCheck, val points: Int, override val score: Int) : ScanOutcome
    data class ChecksFixed(val count: Int, val points: Int, override val score: Int) : ScanOutcome

    /** One check costs less but isn't fixed (partial, or it can't be read any more). */
    data class CheckImproved(val check: PrivacyCheck, val points: Int, override val score: Int) : ScanOutcome

    /** [points] is null when the review earned nothing (it wasn't due). */
    data class ReviewDone(val type: ManualCheckType, val points: Int?, override val score: Int) : ScanOutcome
    data class AdIdConfirmed(val points: Int?, override val score: Int) : ScanOutcome

    /** The score rose, but no single name fits. [hasLosses]: something also got worse. */
    data class Gain(val points: Int, override val score: Int, val hasLosses: Boolean) : ScanOutcome

    /** [check] is set when it is the only thing that changed. */
    data class Loss(val check: PrivacyCheck?, val points: Int, override val score: Int) : ScanOutcome

    /** Changes that cancel out. [exact]: every better check is fixed, every worse one is new. */
    data class Mixed(val better: Int, val worse: Int, val exact: Boolean, override val score: Int) : ScanOutcome
    data class NotFixedYet(val check: PrivacyCheck, override val score: Int) : ScanOutcome
    data class NoChange(override val score: Int) : ScanOutcome
}

/**
 * A user scan that hasn't finished. [before] is the raw score of the result on screen when it
 * was asked for. It is trust-adjusted only when compared, with the same trusted apps as the new
 * result, so trusting an app while a scan runs is never reported as a fix.
 */
data class PendingUserScan(val before: PrivacyScore, val trigger: UserScan)

object ScanFeedback {

    /**
     * Compares two trust-adjusted scores held in memory. Never reads score_history: the widget
     * writes that file too. A check is better when its deduction went down, worse when it went up;
     * a check only one side has costs nothing on the other.
     */
    fun between(before: PrivacyScore, after: PrivacyScore, trigger: UserScan): ScanOutcome {
        val points = after.score - before.score
        val beforeIssues = before.issues.associateBy { it.check }
        val afterIssues = after.issues.associateBy { it.check }
        val changes = (beforeIssues.keys + afterIssues.keys).map { check ->
            Change(check, beforeIssues[check]?.pointDeduction ?: 0, afterIssues[check])
        }
        val better = changes.filter { it.after < it.before }
        val worse = changes.filter { it.after > it.before }
        val manual = after.manualCheckPoints - before.manualCheckPoints
        val betterCount = better.size + if (manual > 0) 1 else 0
        val worseCount = worse.size + if (manual < 0) 1 else 0

        return when {
            points > 0 -> gain(better, worseCount, manual, points, after.score, trigger)
            points < 0 -> ScanOutcome.Loss(
                check = worse.singleOrNull()?.check?.takeIf { betterCount == 0 && worseCount == 1 },
                points = -points,
                score = after.score
            )
            betterCount > 0 && worseCount > 0 -> ScanOutcome.Mixed(
                better = betterCount,
                worse = worseCount,
                exact = manual == 0 && better.all { it.isFixed } && worse.all { it.before == 0 },
                score = after.score
            )
            // Only one side changed but the shown score didn't move: both are clamped at 0.
            betterCount > 0 || worseCount > 0 -> ScanOutcome.NoChange(after.score)
            else -> unchanged(beforeIssues, after.score, trigger)
        }
    }

    private fun gain(
        better: List<Change>,
        worseCount: Int,
        manual: Int,
        points: Int,
        score: Int,
        trigger: UserScan
    ): ScanOutcome {
        val single = better.singleOrNull()
        return when {
            // Nothing shows that a check which disappeared was fixed: its points only
            better.any { it.isGone } -> ScanOutcome.Gain(points, score, hasLosses = worseCount > 0)
            worseCount > 0 -> ScanOutcome.Gain(points, score, hasLosses = true)
            manual > 0 ->
                if (better.isEmpty() && trigger is UserScan.ManualDone && manual == trigger.type.pointValue) {
                    ScanOutcome.ReviewDone(trigger.type, points, score)
                } else {
                    ScanOutcome.Gain(points, score, hasLosses = false)
                }
            trigger == UserScan.AdIdConfirmed && single?.check == PrivacyCheck.ADVERTISING_ID ->
                ScanOutcome.AdIdConfirmed(points, score)
            single != null && single.isFixed -> ScanOutcome.CheckFixed(single.check, points, score)
            single != null -> ScanOutcome.CheckImproved(single.check, points, score)
            better.isNotEmpty() && better.all { it.isFixed } -> ScanOutcome.ChecksFixed(better.size, points, score)
            else -> ScanOutcome.Gain(points, score, hasLosses = false)
        }
    }

    /** "Not fixed yet" only for a check that costs points: a 0-point row has nothing to earn. */
    private fun unchanged(
        issues: Map<PrivacyCheck, PrivacyIssue>,
        score: Int,
        trigger: UserScan
    ): ScanOutcome {
        fun notFixedOrNoChange(check: PrivacyCheck?): ScanOutcome =
            if (check != null && (issues[check]?.pointDeduction ?: 0) > 0) {
                ScanOutcome.NotFixedYet(check, score)
            } else {
                ScanOutcome.NoChange(score)
            }
        return when (trigger) {
            is UserScan.FixReturn -> notFixedOrNoChange(trigger.check)
            is UserScan.CheckAgain -> notFixedOrNoChange(trigger.check)
            is UserScan.ManualDone -> ScanOutcome.ReviewDone(trigger.type, null, score)
            UserScan.AdIdConfirmed -> ScanOutcome.AdIdConfirmed(null, score)
            UserScan.Rescan, UserScan.AdIdReset -> ScanOutcome.NoChange(score)
        }
    }

    /**
     * A new scan cancels a running one. The earliest [PendingUserScan.before] is kept, so a
     * double tap can't lose the gain; a plain rescan doesn't replace a more telling trigger.
     */
    fun merge(pending: PendingUserScan?, trigger: UserScan?, current: PrivacyScore?): PendingUserScan? =
        when {
            trigger == null -> pending
            pending == null -> current?.let { PendingUserScan(it, trigger) }
            trigger == UserScan.Rescan -> pending
            else -> pending.copy(trigger = trigger)
        }

    private class Change(val check: PrivacyCheck, val before: Int, new: PrivacyIssue?) {
        val after = new?.pointDeduction ?: 0
        val isGone = new == null
        val isFixed = new != null && after == 0 && !new.isUnknown
    }
}
