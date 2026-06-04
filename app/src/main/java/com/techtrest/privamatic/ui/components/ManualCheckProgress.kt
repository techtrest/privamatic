package com.techtrest.privamatic.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.techtrest.privamatic.Amber
import com.techtrest.privamatic.data.model.ManualCheckState

/**
 * Progress bar colour for a manual check based on how close it is to its review window.
 * Overdue checks (>= 1f) and freshly completed checks (< 0.86f) both use primary; the
 * intermediate bands warn with tertiary then Amber as the review date approaches.
 */
@Composable
fun getProgressColor(checkState: ManualCheckState): Color {
    return when {
        checkState.fillPercentage >= 0.96f && checkState.fillPercentage < 1f -> MaterialTheme.colorScheme.tertiary
        checkState.fillPercentage >= 0.86f && checkState.fillPercentage < 0.96f -> Amber
        else -> MaterialTheme.colorScheme.primary
    }
}

/**
 * Human-readable status text describing how long until a manual check is due for review.
 */
fun getStatusText(checkState: ManualCheckState): String {
    return when {
        checkState.isOverdue -> "Review needed"
        checkState.daysRemaining == 1 -> "1 day remaining"
        else -> "${checkState.daysRemaining} days remaining"
    }
}
