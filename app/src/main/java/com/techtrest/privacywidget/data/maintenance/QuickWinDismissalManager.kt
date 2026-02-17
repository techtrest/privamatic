package com.techtrest.privacywidget.data.maintenance

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.techtrest.privacywidget.data.model.PrivacyCheck
import com.techtrest.privacywidget.data.model.QuickWin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dismissalDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "quick_win_dismissals"
)

private const val KEY_PREFIX = "dismissed_"

/**
 * Manages Quick Win dismissals using DataStore for persistence.
 * Dismissals are keyed by PrivacyCheck name so they auto-clear
 * when the underlying condition changes (e.g., app reinstalled).
 */
class QuickWinDismissalManager(private val context: Context) {

    /**
     * Dismiss a Quick Win so it no longer appears in the active list.
     */
    suspend fun dismiss(quickWin: QuickWin) {
        val checkName = quickWin.relatedCheck?.name ?: return
        val key = booleanPreferencesKey("$KEY_PREFIX$checkName")
        context.dismissalDataStore.edit { preferences ->
            preferences[key] = true
        }
    }

    /**
     * Restore a previously dismissed Quick Win to the active list.
     */
    suspend fun restore(quickWin: QuickWin) {
        val checkName = quickWin.relatedCheck?.name ?: return
        val key = booleanPreferencesKey("$KEY_PREFIX$checkName")
        context.dismissalDataStore.edit { preferences ->
            preferences.remove(key)
        }
    }

    /**
     * Observe the set of dismissed PrivacyCheck names.
     * Quick Wins whose relatedCheck is in this set should be filtered from the active list.
     */
    fun getDismissedCheckNames(): Flow<Set<String>> {
        return context.dismissalDataStore.data.map { preferences ->
            preferences.asMap()
                .filter { (key, value) -> key.name.startsWith(KEY_PREFIX) && value == true }
                .map { (key, _) -> key.name.removePrefix(KEY_PREFIX) }
                .toSet()
        }
    }
}

/**
 * Filter out dismissed Quick Wins from an active list.
 * A Quick Win is dismissed if its relatedCheck name is in the dismissed set.
 */
fun List<QuickWin>.filterDismissed(dismissedCheckNames: Set<String>): List<QuickWin> {
    return filter { it.relatedCheck?.name !in dismissedCheckNames }
}

/**
 * Return only dismissed Quick Wins from a full list.
 * Used to populate the restore UI.
 */
fun List<QuickWin>.onlyDismissed(dismissedCheckNames: Set<String>): List<QuickWin> {
    return filter { it.relatedCheck?.name in dismissedCheckNames }
}
