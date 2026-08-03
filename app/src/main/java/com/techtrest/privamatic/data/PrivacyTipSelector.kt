package com.techtrest.privamatic.data

import com.techtrest.privamatic.data.model.PrivacyScore
import com.techtrest.privamatic.data.model.PrivacyTip
import com.techtrest.privamatic.data.model.PrivacyTips
import com.techtrest.privamatic.data.model.TipLayer

/**
 * Selects contextual privacy tips based on the user's current privacy posture.
 * Layers are weighted rather than ranked: ISSUE_AWARE (most actionable) appears
 * most often, then REINFORCING, then EDUCATIONAL — but no layer is ever locked out.
 * Avoids recently shown tips when possible.
 */
object PrivacyTipSelector {

    /** Relative pick weight per tip, by layer. Higher = shown more often. */
    private val LAYER_WEIGHTS = mapOf(
        TipLayer.ISSUE_AWARE to 3,
        TipLayer.REINFORCING to 2,
        TipLayer.EDUCATIONAL to 1
    )

    /**
     * Select a single tip appropriate for the current privacy state.
     *
     * @param privacyScore Current scan results used to determine which checks are secure/insecure
     * @param recentlyShownIds Tip IDs that were shown within the cooldown window
     * @param excludeId ID of the tip currently on screen, never re-selected unless it is the
     *   only eligible tip. Guarantees "Next tip" always visibly changes the tip.
     * @return A contextual tip, or null if no tips are available (shouldn't happen with 45 tips)
     */
    fun selectTip(
        privacyScore: PrivacyScore,
        recentlyShownIds: Set<String>,
        excludeId: String? = null
    ): PrivacyTip? {
        val insecureChecks = privacyScore.issues
            .filter { !it.isSecure }
            .map { it.check }
            .toSet()

        val secureChecks = privacyScore.issues
            .filter { it.isSecure }
            .map { it.check }
            .toSet()

        val allEligible = PrivacyTips.all.filter { tip ->
            isTipEligible(tip, insecureChecks, secureChecks)
        }

        // Drop the tip already on screen, unless it is the only thing we could show
        val eligible = allEligible.filterNot { it.id == excludeId }
            .ifEmpty { allEligible }

        // Try to find an unshown tip first
        val unshown = eligible.filter { it.id !in recentlyShownIds }
        val picked = pickWeighted(unshown)
        if (picked != null) return picked

        // All eligible tips recently shown — reset window and pick from full eligible pool
        return pickWeighted(eligible)
    }

    /**
     * Check whether a tip is eligible given the current posture.
     */
    private fun isTipEligible(
        tip: PrivacyTip,
        insecureChecks: Set<com.techtrest.privamatic.data.model.PrivacyCheck>,
        secureChecks: Set<com.techtrest.privamatic.data.model.PrivacyCheck>
    ): Boolean {
        return when (tip.layer) {
            TipLayer.ISSUE_AWARE -> tip.relatedCheck in insecureChecks
            TipLayer.REINFORCING -> tip.relatedCheck in secureChecks
            TipLayer.EDUCATIONAL -> true
        }
    }

    /**
     * Pick a random tip from a single pool spanning all layers, biased by [LAYER_WEIGHTS].
     * Unlike a strict layer waterfall, this keeps EDUCATIONAL tips in rotation even when
     * ISSUE_AWARE or REINFORCING tips are available.
     */
    private fun pickWeighted(tips: List<PrivacyTip>): PrivacyTip? {
        if (tips.isEmpty()) return null
        val pool = tips.flatMap { tip -> List(LAYER_WEIGHTS[tip.layer] ?: 1) { tip } }
        return pool.random()
    }
}
