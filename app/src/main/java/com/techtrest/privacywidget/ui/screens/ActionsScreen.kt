package com.techtrest.privacywidget.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.techtrest.privacywidget.data.QuickWinsDetector
import com.techtrest.privacywidget.data.model.PrivacyScore
import com.techtrest.privacywidget.data.model.QuickWin
import com.techtrest.privacywidget.data.model.QuickWinType
import com.techtrest.privacywidget.data.model.ManualCheckState
import com.techtrest.privacywidget.data.model.ManualCheckType
import com.techtrest.privacywidget.ui.components.InstructionsDialog
import kotlinx.coroutines.launch
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
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Quick Wins state (only actionable privacy settings)
    val quickWins = remember(privacyScore) {
        QuickWinsDetector.detectQuickWins(privacyScore)
    }

    var selectedQuickWin by remember { mutableStateOf<QuickWin?>(null) }

    // Calculate estimated scroll position for Quick Wins section
    // Hero (160dp) + spacing (16dp) + title (28dp) + spacing (16dp) + manual checks (76dp each) + spacing (16dp)
    val quickWinsScrollTarget = remember(checkStates.size) {
        with(density) {
            (160.dp + 16.dp + 28.dp + 16.dp + (76 * checkStates.size).dp + 16.dp).toPx().toInt()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Section - Actions Overview
        ActionsOverviewHero(
            quickWinsCount = quickWins.size,
            checkStates = checkStates,
            onScrollToQuickWins = {
                coroutineScope.launch {
                    scrollState.animateScrollTo(
                        value = quickWinsScrollTarget,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }
            }
        )

        // 2. Manual Checks Section - Compact Format
        Text(
            text = "Manual Checks",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        checkStates.forEach { checkState ->
            CompactManualCheckItem(
                checkState = checkState,
                onClick = { onNavigateToGuide(checkState.type) }
            )
        }

        // 3. Quick Wins Section
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
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
        }
    }

    // Show Instructions Dialog
    selectedQuickWin?.let { quickWin ->
        InstructionsDialog(
            quickWin = quickWin,
            onDismiss = { selectedQuickWin = null }
        )
    }
}

/**
 * Fixed-size hero section showing Actions Overview.
 * Displays Quick Wins count, Manual Checks status, and adaptive messaging.
 * Includes scroll anchor to jump to Quick Wins section.
 */
@Composable
private fun ActionsOverviewHero(
    quickWinsCount: Int,
    checkStates: List<ManualCheckState>,
    onScrollToQuickWins: () -> Unit,
    modifier: Modifier = Modifier
) {
    val overdueCount = checkStates.count { it.isOverdue }
    val dueSoonCount = checkStates.count { !it.isOverdue && it.daysRemaining <= 7 }

    // Determine state priority
    val hasOverdueChecks = overdueCount > 0
    val hasDueSoon = dueSoonCount > 0
    val hasQuickWins = quickWinsCount > 0

    // Get manual checks status text
    val checksStatusText = when {
        hasOverdueChecks -> "$overdueCount overdue"
        hasDueSoon -> "$dueSoonCount due soon"
        else -> "All complete"
    }

    // Get adaptive message and colors
    val (message, containerColor, contentColor) = when {
        hasOverdueChecks -> Triple(
            "Complete overdue checks to restore your privacy score.",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        hasDueSoon -> Triple(
            "Stay on top of reviews to maintain your +15 point bonus.",
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        hasQuickWins -> Triple(
            "Complete Quick Wins to boost your privacy score.",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        else -> Triple(
            "Great privacy hygiene! Keep up the excellent work.",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Actions Overview",
                    style = MaterialTheme.typography.titleLarge,
                    color = contentColor
                )
            }

            // Status Summary
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Quick Wins line with scroll anchor
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Quick Wins: $quickWinsCount available",
                        style = MaterialTheme.typography.bodyLarge,
                        color = contentColor
                    )
                    if (quickWinsCount > 0) {
                        IconButton(
                            onClick = onScrollToQuickWins,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Scroll to Quick Wins",
                                tint = contentColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Manual Checks status
                Text(
                    text = "Manual Checks: $checksStatusText",
                    style = MaterialTheme.typography.bodyLarge,
                    color = contentColor
                )
            }

            // Adaptive message
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor
            )
        }
    }
}

/**
 * Compact one-line manual check item.
 * Shows: Icon | Title | Progress Bar | Status
 */
@Composable
private fun CompactManualCheckItem(
    checkState: ManualCheckState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progressColor = getProgressColor(checkState)
    val statusText = getStatusText(checkState)

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon
            Icon(
                imageVector = checkState.type.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            // Title
            Text(
                text = checkState.type.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            // Progress Bar
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                val progressValue = checkState.fillPercentage.coerceIn(0f, 1f)
                if (progressValue > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressValue)
                            .background(progressColor)
                    )
                }
            }

            // Status
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(80.dp)
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
        checkState.isOverdue -> "Due now"
        checkState.daysRemaining == 1 -> "1 day"
        checkState.daysRemaining <= 90 -> "${checkState.daysRemaining} days"
        else -> "${checkState.daysRemaining} days"
    }
}
