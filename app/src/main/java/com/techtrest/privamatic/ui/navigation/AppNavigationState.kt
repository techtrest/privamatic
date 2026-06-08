package com.techtrest.privamatic.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Security
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import com.techtrest.privamatic.R

enum class DetailsSubTab(
    @StringRes val label: Int,
    val icon: ImageVector
) {
    SURVEILLANCE(
        label = R.string.details_sub_tab_surveillance,
        icon = Icons.Default.RemoveRedEye
    ),
    SECURITY(
        label = R.string.details_sub_tab_security,
        icon = Icons.Default.Security
    )
}

enum class DetailsTab(@StringRes val label: Int) {
    CHECKS(R.string.details_tab_checks),
    APPS(R.string.details_tab_apps)
}

class AppNavigationState(
    initialTab: NavigationTab = NavigationTab.DASHBOARD,
    initialDetailsSubTab: DetailsSubTab = DetailsSubTab.SURVEILLANCE,
    initialDetailsTab: DetailsTab = DetailsTab.CHECKS
) {
    var selectedTab by mutableStateOf(initialTab)
        private set

    var selectedDetailsSubTab by mutableStateOf(initialDetailsSubTab)
        private set

    var selectedDetailsTab by mutableStateOf(initialDetailsTab)
        private set

    var isDrawerOpen by mutableStateOf(false)
        private set

    fun selectTab(tab: NavigationTab) {
        selectedTab = tab
    }

    fun selectDetailsSubTab(subTab: DetailsSubTab) {
        selectedDetailsSubTab = subTab
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
}

@Composable
fun rememberAppNavigationState(
    initialTab: NavigationTab = NavigationTab.DASHBOARD,
    initialDetailsSubTab: DetailsSubTab = DetailsSubTab.SURVEILLANCE,
    initialDetailsTab: DetailsTab = DetailsTab.CHECKS
): AppNavigationState {
    return remember {
        AppNavigationState(initialTab, initialDetailsSubTab, initialDetailsTab)
    }
}
