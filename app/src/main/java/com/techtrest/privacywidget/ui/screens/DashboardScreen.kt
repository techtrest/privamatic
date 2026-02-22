package com.techtrest.privacywidget.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.techtrest.privacywidget.data.QuickWinsDetector
import com.techtrest.privacywidget.data.maintenance.MaintenanceManager
import com.techtrest.privacywidget.data.maintenance.filterDismissed
import com.techtrest.privacywidget.data.model.PrivacyCategory
import com.techtrest.privacywidget.data.model.PrivacyScore
import com.techtrest.privacywidget.data.model.ScoreHistory
import com.techtrest.privacywidget.data.model.getSecurityIssuesCount
import com.techtrest.privacywidget.data.model.getTrackingIssuesCount
import com.techtrest.privacywidget.ui.components.DeviceInfoCard
import com.techtrest.privacywidget.ui.components.ScoreCard
import com.techtrest.privacywidget.ui.components.SummaryCard
import com.techtrest.privacywidget.ui.navigation.AppNavigationState
import com.techtrest.privacywidget.ui.navigation.NavigationTab

@Composable
fun DashboardScreen(
    privacyScore: PrivacyScore,
    scoreHistory: ScoreHistory?,
    navigationState: AppNavigationState,
    dismissedCheckNames: Set<String>,
    onRefresh: () -> Unit = {},
    isRefreshing: Boolean = false,
    onNavigateToManualChecks: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val maintenanceManager = remember { MaintenanceManager(context) }

    // Manual checks state
    val checkStates by maintenanceManager.getCheckStates().collectAsState(initial = emptyList())
    val overdueCount = remember(checkStates) { checkStates.count { it.isOverdue } }

    // Quick Wins state — filtered by dismissals so Dashboard count matches Actions tab
    val activeQuickWins = remember(privacyScore, dismissedCheckNames) {
        QuickWinsDetector.detectQuickWins(privacyScore).filterDismissed(dismissedCheckNames)
    }

    // Total actionable items (active Quick Wins + overdue Manual Checks)
    val totalActions = remember(activeQuickWins, overdueCount) {
        activeQuickWins.size + overdueCount
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
                .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // 1. Score Card
        ScoreCard(privacyScore = privacyScore)

        // 2. Summary Cards (no header)
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
                    navigationState.selectTab(NavigationTab.DETAILS)
                },
                modifier = Modifier.weight(1f)
            )

            // Actions (Quick Wins + overdue Manual Checks) - THIRD
            val actionsSubtitle = getIssueCountSubtitle(
                count = totalActions,
                zeroText = "All Done!",
                singleText = "1 Total",
                pluralSuffix = "Total"
            )

            SummaryCard(
                title = "Actions",
                subtitle = actionsSubtitle,
                icon = Icons.Default.TipsAndUpdates,
                onClick = {
                    navigationState.selectTab(NavigationTab.ACTIONS)
                },
                modifier = Modifier.weight(1f)
            )
        }

        // 3. Device Info Card
        DeviceInfoCard(scoreHistory = scoreHistory)
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
