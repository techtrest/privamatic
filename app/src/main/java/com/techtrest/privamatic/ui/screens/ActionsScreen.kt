package com.techtrest.privamatic.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.techtrest.privamatic.R
import com.techtrest.privamatic.data.maintenance.filterDismissed
import com.techtrest.privamatic.data.maintenance.onlyDismissed
import com.techtrest.privamatic.data.model.QuickWin
import com.techtrest.privamatic.data.model.QuickWinType
import com.techtrest.privamatic.data.model.ManualCheckState
import com.techtrest.privamatic.data.model.ManualCheckType
import com.techtrest.privamatic.data.model.PrivacyTip
import com.techtrest.privamatic.ui.components.getProgressColor
import com.techtrest.privamatic.ui.components.getStatusText

@Composable
fun ActionsScreen(
    allQuickWins: List<QuickWin>,
    checkStates: List<ManualCheckState>,
    dismissedCheckNames: Set<String>,
    currentTip: PrivacyTip?,
    onNavigateToGuide: (ManualCheckType) -> Unit,
    onMarkCheckDone: (ManualCheckType) -> Unit,
    onQuickWinSelected: (QuickWin) -> Unit,
    onRestoreQuickWin: (QuickWin) -> Unit,
    onNextTip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Active Quick Wins (not dismissed)
    val activeQuickWins = remember(allQuickWins, dismissedCheckNames) {
        allQuickWins.filterDismissed(dismissedCheckNames)
    }

    // Dismissed Quick Wins (still detected but user chose to hide)
    val dismissedQuickWins = remember(allQuickWins, dismissedCheckNames) {
        allQuickWins.onlyDismissed(dismissedCheckNames)
    }

    var showDismissed by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(dismissedQuickWins.isEmpty()) {
        if (dismissedQuickWins.isEmpty()) {
            showDismissed = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // 1. Quick Wins Section - with horizontal padding
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // Header row: title left, "Dismissed (X)" toggle right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.label_actions_quick_wins),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (dismissedQuickWins.isNotEmpty()) {
                    TextButton(onClick = { showDismissed = !showDismissed }) {
                        Text(
                            text = if (showDismissed) {
                                pluralStringResource(
                                    R.plurals.plural_dismissed_hide_count,
                                    dismissedQuickWins.size,
                                    dismissedQuickWins.size
                                )
                            } else {
                                pluralStringResource(
                                    R.plurals.plural_dismissed_count,
                                    dismissedQuickWins.size,
                                    dismissedQuickWins.size
                                )
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3-column grid of active Quick Win tiles, or All Done state
            if (activeQuickWins.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    activeQuickWins.chunked(3).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { quickWin ->
                                QuickWinCompactTile(
                                    quickWin = quickWin,
                                    onClick = { onQuickWinSelected(quickWin) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                QuickWinAllDoneCard()
            }

            // Dismissed Quick Wins — vertical list below the grid
            AnimatedVisibility(
                visible = showDismissed && dismissedQuickWins.isNotEmpty(),
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    dismissedQuickWins.forEach { quickWin ->
                        DismissedQuickWinRow(
                            quickWin = quickWin,
                            onRestore = { onRestoreQuickWin(quickWin) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Manual Checks Section - edge-to-edge
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Section title with padding
            Text(
                text = stringResource(R.string.label_actions_manual_checks),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp)
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

        // 3. Privacy Tip — contextual education card
        if (currentTip != null) {
            Spacer(modifier = Modifier.height(16.dp))
            PrivacyTipCard(
                tip = currentTip,
                onNextTip = onNextTip,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Contextual privacy tip card with lightbulb icon.
 * Subtle design — informational, not action-demanding.
 */
@Composable
private fun PrivacyTipCard(
    tip: PrivacyTip,
    onNextTip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tip.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onNextTip) {
                    Text(
                        text = stringResource(R.string.label_actions_next_tip),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = tip.content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Full-width row for a dismissed Quick Win, shown in the vertical list below the scroll row.
 * Outlined with no fill to visually distinguish from active items.
 */
@Composable
private fun DismissedQuickWinRow(
    quickWin: QuickWin,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = Color.Transparent
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = quickWin.type.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = quickWin.displayTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRestore) {
                Text(
                    text = stringResource(R.string.label_actions_restore),
                    style = MaterialTheme.typography.labelMedium
                )
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
    val shortName = when (quickWin.type) {
        QuickWinType.REVOKE_NOTIFICATION_LISTENERS -> stringResource(R.string.quick_win_revoke_notification_listeners_short)
        QuickWinType.REVOKE_ACCESSIBILITY_SERVICES -> stringResource(R.string.quick_win_revoke_accessibility_services_short)
        QuickWinType.REVOKE_DEVICE_ADMINS -> stringResource(R.string.quick_win_revoke_device_admins_short)
        QuickWinType.DISABLE_WIFI_SCANNING -> stringResource(R.string.quick_win_disable_wifi_scanning_short)
        QuickWinType.DISABLE_ADVERTISING_ID -> stringResource(R.string.quick_win_disable_advertising_id_short)
        QuickWinType.ENABLE_PRIVATE_DNS -> stringResource(R.string.quick_win_enable_private_dns_short)
        QuickWinType.DISABLE_DEVELOPER_OPTIONS -> stringResource(R.string.quick_win_disable_developer_options_short)
        QuickWinType.REPLACE_BROWSER -> quickWin.currentAppName ?: stringResource(R.string.quick_win_replace_browser_short)
        QuickWinType.REPLACE_KEYBOARD -> quickWin.currentAppName ?: stringResource(R.string.quick_win_replace_keyboard_short)
        QuickWinType.REPLACE_DEFAULT_SMS -> quickWin.currentAppName ?: stringResource(R.string.quick_win_replace_default_sms_short)
        QuickWinType.REPLACE_DEFAULT_EMAIL -> quickWin.currentAppName ?: stringResource(R.string.quick_win_replace_default_email_short)
        QuickWinType.REPLACE_DEFAULT_LAUNCHER -> quickWin.currentAppName ?: stringResource(R.string.quick_win_replace_default_launcher_short)
        QuickWinType.UNINSTALL_APP -> quickWin.currentAppName ?: stringResource(R.string.quick_win_uninstall_app_short)
    }

    Card(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
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
            Icon(
                imageVector = quickWin.type.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = shortName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = pluralStringResource(R.plurals.plural_quick_win_points, quickWin.impact, quickWin.impact),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Full-width card shown when no active Quick Wins remain.
 * Dismissed wins are accessed via the "Dismissed (X)" button in the section header.
 */
@Composable
private fun QuickWinAllDoneCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.label_all_done),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.label_actions_great_work),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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

                // Custom progress bar (fixes dot bug at 0% progress)
                val progressValue = checkState.fillPercentage.coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(8.dp))
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
