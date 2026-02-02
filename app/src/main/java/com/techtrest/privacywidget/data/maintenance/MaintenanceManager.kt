package com.techtrest.privacywidget.data.maintenance

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.techtrest.privacywidget.data.model.ManualCheckState
import com.techtrest.privacywidget.data.model.ManualCheckType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import kotlin.math.max

// Top-level DataStore singleton
private val Context.maintenanceDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "maintenance_prefs"
)

/**
 * Manages manual privacy check maintenance system using DataStore for persistence.
 */
class MaintenanceManager(private val context: Context) {

    /**
     * Mark a manual check as completed.
     * Stores current timestamp and awards points.
     */
    suspend fun markCheckCompleted(type: ManualCheckType) {
        val key = longPreferencesKey("manual_check_${type.name}_timestamp")
        context.maintenanceDataStore.edit { preferences ->
            preferences[key] = System.currentTimeMillis()
        }
    }

    /**
     * Get current states for all manual checks.
     * Calculates days remaining, fill percentage, and overdue status.
     */
    fun getCheckStates(): Flow<List<ManualCheckState>> {
        return context.maintenanceDataStore.data.map { preferences ->
            ManualCheckType.entries.map { type ->
                calculateCheckState(type, preferences)
            }
        }
    }

    /**
     * Get state for a specific check type.
     */
    fun getCheckState(type: ManualCheckType): Flow<ManualCheckState> {
        return context.maintenanceDataStore.data.map { preferences ->
            calculateCheckState(type, preferences)
        }
    }

    /**
     * Calculate the current state for a manual check.
     */
    private fun calculateCheckState(
        type: ManualCheckType,
        preferences: Preferences
    ): ManualCheckState {
        val key = longPreferencesKey("manual_check_${type.name}_timestamp")
        val lastCompletedTimestamp = preferences[key] ?: 0L

        val currentTime = System.currentTimeMillis()
        val daysSinceCompleted = if (lastCompletedTimestamp == 0L) {
            type.periodDays // If never completed, treat as overdue
        } else {
            TimeUnit.MILLISECONDS.toDays(currentTime - lastCompletedTimestamp).toInt()
        }

        val daysRemaining = max(0, type.periodDays - daysSinceCompleted)
        val fillPercentage = (daysSinceCompleted.toFloat() / type.periodDays.toFloat()).coerceIn(0f, 1f)
        val isOverdue = fillPercentage >= 1f

        return ManualCheckState(
            type = type,
            lastCompletedTimestamp = lastCompletedTimestamp,
            daysRemaining = daysRemaining,
            fillPercentage = fillPercentage,
            isOverdue = isOverdue
        )
    }

    /**
     * Get count of overdue checks.
     */
    fun getOverdueCount(): Flow<Int> {
        return getCheckStates().map { states ->
            states.count { it.isOverdue }
        }
    }

    /**
     * Get count of checks due soon (≤7 days but not overdue).
     */
    fun getDueSoonCount(): Flow<Int> {
        return getCheckStates().map { states ->
            states.count { !it.isOverdue && it.daysRemaining <= 7 }
        }
    }

    /**
     * Get total points from non-overdue checks.
     */
    fun getTotalPoints(): Flow<Int> {
        return getCheckStates().map { states ->
            states.filter { !it.isOverdue }.sumOf { it.type.pointValue }
        }
    }
}
