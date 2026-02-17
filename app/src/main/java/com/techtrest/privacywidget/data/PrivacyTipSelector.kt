package com.techtrest.privacywidget.data

import com.techtrest.privacywidget.data.model.PrivacyScore
import com.techtrest.privacywidget.data.model.PrivacyTip
import com.techtrest.privacywidget.data.model.PrivacyTips
import com.techtrest.privacywidget.data.model.TipLayer

/**
 * Selects contextual privacy tips based on the user's current privacy posture.
 * Priority: ISSUE_AWARE (most actionable) → REINFORCING → EDUCATIONAL.
 * Avoids recently shown tips when possible.
 */
object PrivacyTipSelector {

    /**
     * Select a single tip appropriate for the current privacy state.
     *
     * @param privacyScore Current scan results used to determine which checks are secure/insecure
     * @param recentlyShownIds Tip IDs that were shown within the cooldown window
     * @return A contextual tip, or null if no tips are available (shouldn't happen with 45 tips)
     */
    fun selectTip(
        privacyScore: PrivacyScore,
        recentlyShownIds: Set<String>
    ): PrivacyTip? {
        val insecureChecks = privacyScore.issues
            .filter { !it.isSecure }
            .map { it.check }
            .toSet()

        val secureChecks = privacyScore.issues
            .filter { it.isSecure }
            .map { it.check }
            .toSet()

        val eligible = PrivacyTips.all.filter { tip ->
            isTipEligible(tip, insecureChecks, secureChecks)
        }

        // Try to find an unshown tip in priority order
        val unshown = eligible.filter { it.id !in recentlyShownIds }
        val picked = pickByPriority(unshown)
        if (picked != null) return picked

        // All eligible tips recently shown — reset window and pick from full eligible pool
        return pickByPriority(eligible)
    }

    /**
     * Check whether a tip is eligible given the current posture.
     */
    private fun isTipEligible(
        tip: PrivacyTip,
        insecureChecks: Set<com.techtrest.privacywidget.data.model.PrivacyCheck>,
        secureChecks: Set<com.techtrest.privacywidget.data.model.PrivacyCheck>
    ): Boolean {
        return when (tip.layer) {
            TipLayer.ISSUE_AWARE -> tip.relatedCheck in insecureChecks
            TipLayer.REINFORCING -> tip.relatedCheck in secureChecks
            TipLayer.EDUCATIONAL -> true
        }
    }

    /**
     * Pick the first available tip following layer priority.
     * Within each layer, select randomly for variety.
     */
    private fun pickByPriority(tips: List<PrivacyTip>): PrivacyTip? {
        val layerOrder = listOf(TipLayer.ISSUE_AWARE, TipLayer.REINFORCING, TipLayer.EDUCATIONAL)
        for (layer in layerOrder) {
            val candidates = tips.filter { it.layer == layer }
            if (candidates.isNotEmpty()) return candidates.random()
        }
        return null
    }
}
