package com.techtrest.privacywidget.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.techtrest.privacywidget.data.QuickWinsDetector
import com.techtrest.privacywidget.data.model.PrivacyScore
import com.techtrest.privacywidget.data.model.QuickWin
import com.techtrest.privacywidget.data.model.QuickWinType
import com.techtrest.privacywidget.data.model.ManualCheckState
import com.techtrest.privacywidget.data.model.ManualCheckType

@Composable
fun ActionsScreen(
    privacyScore: PrivacyScore,
    checkStates: List<ManualCheckState>,
    onNavigateToGuide: (ManualCheckType) -> Unit,
    onMarkCheckDone: (ManualCheckType) -> Unit,
    onQuickWinSelected: (QuickWin) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Quick Wins state (only actionable privacy settings)
    val quickWins = remember(privacyScore) {
        QuickWinsDetector.detectQuickWins(privacyScore)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // 1. Quick Wins Section
        Text(
            text = "Quick Wins",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Horizontal scroller with compact tiles
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 0.dp)
        ) {
            if (quickWins.isNotEmpty()) {
                items(quickWins) { quickWin ->
                    QuickWinCompactTile(
                        quickWin = quickWin,
                        onClick = { onQuickWinSelected(quickWin) }
                    )
                }
            } else {
                item {
                    QuickWinAllDoneTile()
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 2. Manual Checks Section
        Text(
            text = "Manual Checks",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Edge-to-edge rows with no spacing between them
        checkStates.forEach { checkState ->
            ManualCheckGarminRow(
                checkState = checkState,
                onClick = { onNavigateToGuide(checkState.type) }
            )
        }
    }
}

@Composable
private fun QuickWinItem(
    quickWin: QuickWin,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = quickWin.type.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = quickWin.displayTitle,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Impact Badge
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "+${quickWin.impact} points",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        // Time Estimate
                        Text(
                            text = quickWin.type.timeEstimate,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = quickWin.type.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Show Instructions")
            }
        }
    }
}

/**
 * Compact square tile for Quick Wins horizontal scroller.
 * Shows icon at top, short name, and point value.
 */
@Composable
private fun QuickWinCompactTile(
    quickWin: QuickWin,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .width(120.dp)
            .height(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon at top
            Icon(
                imageVector = quickWin.type.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Short name (1-2 words)
            val shortName = when (quickWin.type) {
                QuickWinType.DISABLE_WIFI_SCANNING -> "Wi-Fi Scan"
                QuickWinType.DISABLE_ADVERTISING_ID -> "Ad ID"
                QuickWinType.ENABLE_PRIVATE_DNS -> "DNS"
                QuickWinType.DISABLE_FIND_MY_DEVICE -> "Find Device"
                QuickWinType.REPLACE_BROWSER -> quickWin.currentAppName ?: "Browser"
                QuickWinType.REPLACE_KEYBOARD -> quickWin.currentAppName ?: "Keyboard"
            }

            Text(
                text = shortName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Point value
            Text(
                text = "+${quickWin.impact}pts",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * "All Done" tile shown when no Quick Wins are available.
 */
@Composable
private fun QuickWinAllDoneTile(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(120.dp)
            .height(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "All Done!",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Great work",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Garmin-style full-width row for Manual Checks.
 * Icon vertically centered on left, title/progress/status on right.
 * No card elevation, subtle press state, edge-to-edge layout.
 */
@Composable
private fun ManualCheckGarminRow(
    checkState: ManualCheckState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progressColor = getProgressColor(checkState)
    val statusText = getStatusText(checkState)

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon - vertically centered on left with start padding
            Icon(
                imageVector = checkState.type.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(32.dp)
                    .padding(start = 16.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Content - title, progress, status
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Title
                Text(
                    text = checkState.type.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Progress bar
                LinearProgressIndicator(
                    progress = { checkState.fillPercentage.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = progressColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                // Status text
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Get progress bar color based on check state.
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

