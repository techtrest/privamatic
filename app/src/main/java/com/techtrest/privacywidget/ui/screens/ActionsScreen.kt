package com.techtrest.privacywidget.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.techtrest.privacywidget.data.QuickWinsDetector
import com.techtrest.privacywidget.data.model.PrivacyScore
import com.techtrest.privacywidget.data.model.QuickWin
import com.techtrest.privacywidget.data.model.QuickWinType
import com.techtrest.privacywidget.data.model.ManualCheckState
import com.techtrest.privacywidget.data.model.ManualCheckType
import com.techtrest.privacywidget.ui.components.InstructionsDialog
import com.techtrest.privacywidget.ui.components.ManualCheckCard
import com.techtrest.privacywidget.ui.components.PrivacyWins
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun ActionsScreen(
    privacyScore: PrivacyScore,
    checkStates: List<ManualCheckState>,
    onNavigateToGuide: (ManualCheckType) -> Unit,
    onMarkCheckDone: (ManualCheckType) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Quick Wins state (only actionable privacy settings)
    val quickWins = remember(privacyScore) {
        QuickWinsDetector.detectQuickWins(privacyScore)
    }

    var selectedQuickWin by remember { mutableStateOf<QuickWin?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Status & Notifications Tile
        StatusNotificationsTile(
            checkStates = checkStates,
            quickWinsCount = quickWins.size
        )

        // 2. Regular Maintenance Section
        Text(
            text = "Regular Maintenance",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Review these privacy settings regularly:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Manual Check Cards
        checkStates.forEach { checkState ->
            ManualCheckCard(
                checkState = checkState,
                onViewGuide = { onNavigateToGuide(checkState.type) },
                onMarkDone = { onMarkCheckDone(checkState.type) }
            )
        }

        // 3. Quick Wins Section Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Section Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Quick Wins",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (quickWins.isNotEmpty()) "${quickWins.size} improvements available" else "All complete!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (quickWins.isNotEmpty()) {
                    // Quick Wins List
                    quickWins.forEachIndexed { index, quickWin ->
                        QuickWinItem(
                            quickWin = quickWin,
                            onClick = { selectedQuickWin = quickWin },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (index < quickWins.size - 1) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                } else {
                    // All Complete Celebration
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "All Quick Wins Complete!",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "You've completed all easy privacy improvements. Great job!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 4. Privacy Wins Section
        PrivacyWins(
            privacyScore = privacyScore,
            initialExpanded = true
        )
    }

    // Show Instructions Dialog
    selectedQuickWin?.let { quickWin ->
        InstructionsDialog(
            quickWin = quickWin,
            onDismiss = { selectedQuickWin = null }
        )
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
 * Adaptive status tile that changes based on actionable items state.
 * Shows different messages and colors based on priority:
 * 1. Manual checks overdue (red/urgent)
 * 2. Manual checks due soon (yellow/warning)
 * 3. Quick wins available (blue/informational)
 * 4. Everything complete (green/success)
 *
 * @param checkStates List of manual check states
 * @param quickWinsCount Number of available quick wins
 * @param modifier Modifier for the card
 */
@Composable
private fun StatusNotificationsTile(
    checkStates: List<ManualCheckState>,
    quickWinsCount: Int,
    modifier: Modifier = Modifier
) {
    val overdueCount = checkStates.count { it.isOverdue }
    val dueSoonCount = checkStates.count { !it.isOverdue && it.daysRemaining <= 7 }
    val overduePoints = checkStates.filter { it.isOverdue }.sumOf { it.type.pointValue }
    val hasOverdueChecks = overdueCount > 0
    val hasDueSoon = dueSoonCount > 0
    val hasQuickWins = quickWinsCount > 0

    // Calculate next review date (earliest non-overdue check)
    val nextReviewDate = remember(checkStates) {
        checkStates
            .filter { !it.isOverdue && it.lastCompletedTimestamp > 0 }
            .minByOrNull { it.daysRemaining }
            ?.let { state ->
                val completedDate = Instant.ofEpochMilli(state.lastCompletedTimestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                completedDate.plusDays(state.type.periodDays.toLong())
                    .format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
            }
    }

    // Determine tile state based on priority
    val tileState = when {
        hasOverdueChecks -> TileState.OVERDUE
        hasDueSoon -> TileState.DUE_SOON
        hasQuickWins -> TileState.QUICK_WINS_AVAILABLE
        else -> TileState.ALL_COMPLETE
    }

    // Get appropriate content based on state
    val (icon, title, description, containerColor, contentColor) = when (tileState) {
        TileState.OVERDUE -> {
            val pointText = if (overduePoints > 0) "$overduePoints points" else "points"
            TileContent(
                icon = Icons.Default.ErrorOutline,
                title = "Manual Checks Overdue",
                description = "Your privacy score has been reduced by $pointText. Complete reviews to restore your score.",
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        }
        TileState.DUE_SOON -> {
            val daysText = if (dueSoonCount == 1) {
                val state = checkStates.first { !it.isOverdue && it.daysRemaining <= 7 }
                if (state.daysRemaining == 1) "1 day" else "${state.daysRemaining} days"
            } else {
                "7 days"
            }
            TileContent(
                icon = Icons.Default.Warning,
                title = "Manual Checks Due Soon",
                description = "$dueSoonCount ${if (dueSoonCount == 1) "review" else "reviews"} needed in $daysText to maintain your +15 point bonus.",
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
        TileState.QUICK_WINS_AVAILABLE -> {
            TileContent(
                icon = Icons.Default.TipsAndUpdates,
                title = "Quick Wins Available",
                description = "$quickWinsCount easy privacy ${if (quickWinsCount == 1) "improvement is" else "improvements are"} ready to boost your score.",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        TileState.ALL_COMPLETE -> {
            val nextReviewText = nextReviewDate?.let { "Next manual review: $it" } ?: "Keep up the great work!"
            TileContent(
                icon = Icons.Default.CheckCircle,
                title = "All Caught Up!",
                description = "Great privacy hygiene! $nextReviewText",
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(32.dp)
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor
                )
            }
        }
    }
}

/**
 * States for the status notifications tile.
 */
private enum class TileState {
    OVERDUE,
    DUE_SOON,
    QUICK_WINS_AVAILABLE,
    ALL_COMPLETE
}

/**
 * Data class holding tile content.
 */
private data class TileContent(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val description: String,
    val containerColor: Color,
    val contentColor: Color
)
