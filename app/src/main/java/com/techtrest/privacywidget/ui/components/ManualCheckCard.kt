package com.techtrest.privacywidget.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.clip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.techtrest.privacywidget.data.model.ManualCheckState

/**
 * Collapsible card component for manual privacy checks (Roomba-style).
 * Shows progress bar and time until next review is due.
 *
 * @param checkState Current state of the manual check
 * @param onViewGuide Callback when "View Step-by-Step Guide" is clicked
 * @param onMarkDone Callback when "Mark as Done" is clicked
 * @param modifier Modifier for the card
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualCheckCard(
    checkState: ManualCheckState,
    onViewGuide: () -> Unit,
    onMarkDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        onClick = { isExpanded = !isExpanded },
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Collapsed Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Icon and Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = checkState.type.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = checkState.type.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Expand/Collapse Icon
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Custom progress bar without rounded caps to avoid visual dot
            // Fill direction: Left-to-right as time passes toward deadline
            // - 0% (just completed): Completely grey, no green visible
            // - 50% (halfway to deadline): Half green (left side), half grey (right side)
            // - 100% (due/overdue): Completely green from left to right
            val progressColor = getProgressColor(checkState)
            val progressValue = checkState.fillPercentage.coerceIn(0f, 1f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (progressValue > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressValue)
                            .background(progressColor)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Status Text
            Text(
                text = getStatusText(checkState),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Expanded Content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    // Description
                    Text(
                        text = checkState.type.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onViewGuide,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("View Guide")
                        }

                        Button(
                            onClick = onMarkDone,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Mark as Done")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Get progress bar color based on check state.
 *
 * Color scheme:
 * - 0-85% full (>7 days): Green (primary)
 * - 86-95% full (4-7 days): Amber
 * - 96-99% full (1-3 days): Orange (tertiary)
 * - 100% full (overdue): Green (full bar = action needed)
 */
@Composable
private fun getProgressColor(checkState: ManualCheckState): Color {
    return when {
        checkState.fillPercentage >= 1f -> MaterialTheme.colorScheme.primary
        checkState.fillPercentage >= 0.96f -> MaterialTheme.colorScheme.tertiary
        checkState.fillPercentage >= 0.86f -> Color(0xFFFFA726) // Amber
        else -> MaterialTheme.colorScheme.primary
    }
}

/**
 * Get status text based on days remaining.
 */
private fun getStatusText(checkState: ManualCheckState): String {
    return when {
        checkState.isOverdue -> "Review needed"
        checkState.daysRemaining == 1 -> "1 day remaining"
        else -> "${checkState.daysRemaining} days remaining"
    }
}
