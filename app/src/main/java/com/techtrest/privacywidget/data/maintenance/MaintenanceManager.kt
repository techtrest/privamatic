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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
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
     *
     * ADVERTISING_ID_CHECK is hidden once completed until it becomes overdue again,
     * since it only needs to be verified once every 180 days. All other check types
     * are always included regardless of completion state.
     */
    fun getCheckStates(): Flow<List<ManualCheckState>> {
        return context.maintenanceDataStore.data.map { preferences ->
            ManualCheckType.entries
                .map { type -> calculateCheckState(type, preferences) }
                .filter { state ->
                    state.type != ManualCheckType.ADVERTISING_ID_CHECK ||
                        state.lastCompletedTimestamp == 0L ||
                        state.isOverdue
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
     * Uses calendar days (midnight-based) rather than 24-hour periods.
     */
    private fun calculateCheckState(
        type: ManualCheckType,
        preferences: Preferences
    ): ManualCheckState {
        val key = longPreferencesKey("manual_check_${type.name}_timestamp")
        val lastCompletedTimestamp = preferences[key] ?: 0L

        val currentDate = LocalDate.now()
        val daysSinceCompleted = if (lastCompletedTimestamp == 0L) {
            type.periodDays // If never completed, treat as overdue
        } else {
            // Convert timestamp to LocalDate and calculate calendar days between
            val lastCompletedDate = Instant.ofEpochMilli(lastCompletedTimestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            ChronoUnit.DAYS.between(lastCompletedDate, currentDate).toInt()
        }

        val daysRemaining = max(0, type.periodDays - daysSinceCompleted)

        // Calculate fill percentage (0.0 = just completed, 1.0 = due/overdue)
        // Explicitly handle 0 days to ensure exactly 0.0f with no floating point errors
        val fillPercentage = if (daysSinceCompleted == 0) {
            0f
        } else {
            (daysSinceCompleted.toFloat() / type.periodDays.toFloat()).coerceIn(0f, 1f)
        }

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
     * Reads all check states directly from DataStore rather than via getCheckStates(),
     * because getCheckStates() applies a UI visibility filter that hides completed
     * ADVERTISING_ID_CHECK entries — which would incorrectly drop their earned points.
     */
    fun getTotalPoints(): Flow<Int> {
        return context.maintenanceDataStore.data.map { preferences ->
            ManualCheckType.entries
                .map { type -> calculateCheckState(type, preferences) }
                .filter {
                    if (it.type == ManualCheckType.ADVERTISING_ID_CHECK)
                        it.lastCompletedTimestamp != 0L
                    else
                        !it.isOverdue
                }
                .sumOf { it.type.pointValue }
        }
    }
}
