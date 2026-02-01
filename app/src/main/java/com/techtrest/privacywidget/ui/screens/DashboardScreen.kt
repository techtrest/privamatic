package com.techtrest.privacywidget.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.techtrest.privacywidget.data.QuickWinsDetector
import com.techtrest.privacywidget.data.model.PrivacyCategory
import com.techtrest.privacywidget.data.model.PrivacyScore
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
    modifier: Modifier = Modifier
) {
    // Quick Wins state
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

        // 2. Summary Cards (no header)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tracking Issues (renamed from "Surveillance") - FIRST
            val trackingIssuesCount = privacyScore.issues.count { issue ->
                issue.check !in PrivacyCategory.SYSTEM_SECURITY.checks && !issue.isSecure
            }
            val trackingSubtitle = if (trackingIssuesCount == 0) {
                "Protected"
            } else if (trackingIssuesCount == 1) {
                "1 Issue"
            } else {
                "$trackingIssuesCount Issues"
            }

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
            val securityIssuesCount = privacyScore.issues.count { issue ->
                issue.check in PrivacyCategory.SYSTEM_SECURITY.checks && !issue.isSecure
            }
            val securitySubtitle = if (securityIssuesCount == 0) {
                "No Issues"
            } else if (securityIssuesCount == 1) {
                "1 Issue"
            } else {
                "$securityIssuesCount Issues"
            }

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

            // Quick Wins (with actual count) - THIRD
            val quickWinsSubtitle = when (quickWins.size) {
                0 -> "All Done!"
                1 -> "1 Available"
                else -> "${quickWins.size} Available"
            }

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

        // 3. Device Info Card
        DeviceInfoCard()

        Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
