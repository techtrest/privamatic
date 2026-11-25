package com.techtrest.privamatic.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.techtrest.privamatic.R

enum class DetailsTab(@StringRes val label: Int) {
    CHECKS(R.string.details_tab_checks),
    APPS(R.string.details_tab_apps),
    SDK(R.string.details_tab_sdk)
}

/** Alternate views of the Checks tab, switched by the header toggle and persisted. */
enum class ChecksView { LIST, BREAKDOWN }

class AppNavigationState(
    initialTab: NavigationTab = NavigationTab.DASHBOARD,
    initialDetailsTab: DetailsTab = DetailsTab.CHECKS
) {
    var selectedTab by mutableStateOf(initialTab)
        private set

    var selectedDetailsTab by mutableStateOf(initialDetailsTab)
        private set

    var isDrawerOpen by mutableStateOf(false)
        private set

    var showHistoryScreen by mutableStateOf(false)
        private set

    /**
     * One-shot: Details > Checks shows its Breakdown view, and keeps it and clears this once the
     * pager has settled there. Going anywhere else first drops it, so it can't change a later visit.
     */
    var isBreakdownRequested by mutableStateOf(false)
        private set

    fun openBreakdown() {
        selectDetailsTab(DetailsTab.CHECKS)
        selectTab(NavigationTab.DETAILS)
        isBreakdownRequested = true
    }

    fun onBreakdownShown() {
        isBreakdownRequested = false
    }

    fun selectTab(tab: NavigationTab) {
        selectedTab = tab
        if (tab != NavigationTab.DETAILS) isBreakdownRequested = false
    }

    fun selectDetailsTab(tab: DetailsTab) {
        selectedDetailsTab = tab
        if (tab != DetailsTab.CHECKS) isBreakdownRequested = false
    }

    fun openDrawer() {
        isDrawerOpen = true
    }

    fun closeDrawer() {
        isDrawerOpen = false
    }

    fun openHistory() {
        showHistoryScreen = true
    }

    fun closeHistory() {
        showHistoryScreen = false
    }
}

@Composable
fun rememberAppNavigationState(
    initialTab: NavigationTab = NavigationTab.DASHBOARD,
    initialDetailsTab: DetailsTab = DetailsTab.CHECKS
): AppNavigationState {
    return remember {
        AppNavigationState(initialTab, initialDetailsTab)
    }
}
