package com.techtrest.privacywidget.ui.screens

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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.techtrest.privacywidget.data.QuickWinsDetector
import com.techtrest.privacywidget.data.maintenance.MaintenanceManager
import com.techtrest.privacywidget.data.model.PrivacyCategory
import com.techtrest.privacywidget.data.model.PrivacyScore
import com.techtrest.privacywidget.data.model.getSecurityIssuesCount
import com.techtrest.privacywidget.data.model.getTrackingIssuesCount
import com.techtrest.privacywidget.ui.components.DeviceInfoCard
import com.techtrest.privacywidget.ui.components.ScoreCard
import com.techtrest.privacywidget.ui.components.SummaryCard
import com.techtrest.privacywidget.ui.navigation.AppNavigationState
import com.techtrest.privacywidget.ui.navigation.DetailsSubTab
import com.techtrest.privacywidget.ui.navigation.NavigationTab

@Composable
fun DashboardScreen(
    privacyScore: PrivacyScore,
    navigationState: AppNavigationState,
    onRefresh: () -> Unit = {},
    isRefreshing: Boolean = false,
    onNavigateToManualChecks: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val maintenanceManager = remember { MaintenanceManager(context) }

    // Manual checks state
    val checkStates by maintenanceManager.getCheckStates().collectAsState(initial = emptyList())
    val overdueCount = checkStates.count { it.isOverdue }
    val dueSoonCount = checkStates.count { !it.isOverdue && it.daysRemaining <= 7 }
    val overduePoints = checkStates.filter { it.isOverdue }.sumOf { it.type.pointValue }
    val hasOverdueChecks = overdueCount > 0
    val hasDueSoon = dueSoonCount > 0
    val showReminderCard = hasOverdueChecks || hasDueSoon

    // Quick Wins state (only actionable privacy settings, no manual checks)
    val quickWins = remember(privacyScore) {
        QuickWinsDetector.detectQuickWins(privacyScore)
    }

    // Swipe refresh state
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isRefreshing)

    SwipeRefresh(
        state = swipeRefreshState,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // 1. Score Card
        ScoreCard(privacyScore = privacyScore)

        // 2. Maintenance Reminder Card (if checks overdue or due soon)
        if (showReminderCard) {
            MaintenanceReminderCard(
                hasOverdueChecks = hasOverdueChecks,
                overdueCount = overdueCount,
                overduePoints = overduePoints,
                dueSoonCount = dueSoonCount,
                onNavigateToManualChecks = onNavigateToManualChecks
            )
        }

        // 3. Summary Cards (no header)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tracking Issues (renamed from "Surveillance") - FIRST
            val trackingIssuesCount = privacyScore.getTrackingIssuesCount()
            val trackingSubtitle = getIssueCountSubtitle(
                count = trackingIssuesCount,
                zeroText = "Protected"
            )

            SummaryCard(
                title = "Tracking",
                subtitle = trackingSubtitle,
                icon = Icons.Default.RemoveRedEye,
                onClick = {
                    navigationState.selectDetailsSubTab(DetailsSubTab.SURVEILLANCE)
                    navigationState.selectTab(NavigationTab.DETAILS)
                },
                modifier = Modifier.weight(1f)
            )

            // Security Issues (System Security category only) - SECOND
            val securityIssuesCount = privacyScore.getSecurityIssuesCount()
            val securitySubtitle = getIssueCountSubtitle(
                count = securityIssuesCount,
                zeroText = "No Issues"
            )

            SummaryCard(
                title = "Security",
                subtitle = securitySubtitle,
                icon = Icons.Default.Security,
                onClick = {
                    navigationState.selectDetailsSubTab(DetailsSubTab.SECURITY)
                    navigationState.selectTab(NavigationTab.DETAILS)
                },
                modifier = Modifier.weight(1f)
            )

            // Quick Wins (already includes manual check wins) - THIRD
            val quickWinsSubtitle = getIssueCountSubtitle(
                count = quickWins.size,
                zeroText = "All Done!",
                singleText = "1 Available",
                pluralSuffix = "Available"
            )

            SummaryCard(
                title = "Quick Wins",
                subtitle = quickWinsSubtitle,
                icon = Icons.Default.TipsAndUpdates,
                onClick = {
                    navigationState.selectTab(NavigationTab.ACTIONS)
                },
                modifier = Modifier.weight(1f)
            )
        }

        // 4. Device Info Card
        DeviceInfoCard()

        Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Card reminding user about manual privacy checks.
 * Shows when checks are overdue OR have ≤7 days remaining.
 * Context-aware messaging based on fresh install vs overdue checks.
 */
@Composable
private fun MaintenanceReminderCard(
    hasOverdueChecks: Boolean,
    overdueCount: Int,
    overduePoints: Int,
    dueSoonCount: Int,
    onNavigateToManualChecks: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Determine if this is a fresh install (all checks overdue with max points possible)
    val isFreshInstall = overdueCount == 3 && overduePoints == 15

    val (title, description) = when {
        isFreshInstall -> {
            "Privacy Reviews Available" to "Complete 3 initial privacy reviews to earn +15 points"
        }
        hasOverdueChecks -> {
            "Maintenance Overdue" to "Complete $overdueCount overdue privacy ${if (overdueCount == 1) "review" else "reviews"} to restore +$overduePoints points"
        }
        else -> {
            "Maintenance Due Soon" to "$dueSoonCount privacy ${if (dueSoonCount == 1) "check needs" else "checks need"} review within the next week"
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (hasOverdueChecks) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = if (hasOverdueChecks) {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    },
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (hasOverdueChecks) {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    }
                )
            }

            // Description
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = if (hasOverdueChecks) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                }
            )

            // Action Button
            OutlinedButton(
                onClick = onNavigateToManualChecks,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View Manual Checks")
            }
        }
    }
}

/**
 * Generates a subtitle string based on count with customizable text.
 *
 * @param count The count to display
 * @param zeroText Text to show when count is 0 (default: "No Issues")
 * @param singleText Text to show when count is 1 (default: "1 Issue")
 * @param pluralSuffix Suffix for plural counts (default: "Issues")
 * @return Formatted subtitle string
 */
private fun getIssueCountSubtitle(
    count: Int,
    zeroText: String = "No Issues",
    singleText: String = "1 Issue",
    pluralSuffix: String = "Issues"
): String = when (count) {
    0 -> zeroText
    1 -> singleText
    else -> "$count $pluralSuffix"
}
