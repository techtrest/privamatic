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
    SDK(R.string.details_tab_sdk),
    BREAKDOWN(R.string.tab_breakdown)
}

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

    fun selectTab(tab: NavigationTab) {
        selectedTab = tab
    }

    fun selectDetailsTab(tab: DetailsTab) {
        selectedDetailsTab = tab
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
