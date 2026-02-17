package com.techtrest.privacywidget.data.maintenance

import android.content.Context
import android.content.SharedPreferences

private const val PREFS_NAME = "privacy_tip_history"
private const val KEY_PREFIX = "tip_shown_"

/** Tips shown within this window are considered recent and won't repeat. */
private const val COOLDOWN_MILLIS = 24L * 60 * 60 * 1000 // 24 hours

/**
 * Tracks which privacy tips have been shown recently using SharedPreferences.
 * Prevents the same tip from repeating within a cooldown window.
 */
class PrivacyTipHistory(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Returns the set of tip IDs that have been shown within the cooldown window.
     */
    fun getRecentlyShownIds(): Set<String> {
        val cutoff = System.currentTimeMillis() - COOLDOWN_MILLIS
        return prefs.all
            .filter { (key, value) ->
                key.startsWith(KEY_PREFIX) && value is Long && value > cutoff
            }
            .map { (key, _) -> key.removePrefix(KEY_PREFIX) }
            .toSet()
    }

    /**
     * Record that a tip was shown now.
     */
    fun markShown(tipId: String) {
        prefs.edit()
            .putLong("$KEY_PREFIX$tipId", System.currentTimeMillis())
            .apply()
    }
}
