package com.techtrest.privacywidget.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Security
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector

enum class DetailsSubTab(
    val label: String,
    val icon: ImageVector
) {
    SURVEILLANCE(
        label = "Surveillance",
        icon = Icons.Default.RemoveRedEye
    ),
    SECURITY(
        label = "Security",
        icon = Icons.Default.Security
    )
}

class AppNavigationState(
    initialTab: NavigationTab = NavigationTab.DASHBOARD,
    initialDetailsSubTab: DetailsSubTab = DetailsSubTab.SURVEILLANCE
) {
    var selectedTab by mutableStateOf(initialTab)
        private set

    var selectedDetailsSubTab by mutableStateOf(initialDetailsSubTab)
        private set

    var isDrawerOpen by mutableStateOf(false)
        private set

    fun selectTab(tab: NavigationTab) {
        selectedTab = tab
    }

    fun selectDetailsSubTab(subTab: DetailsSubTab) {
        selectedDetailsSubTab = subTab
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
    initialDetailsSubTab: DetailsSubTab = DetailsSubTab.SURVEILLANCE
): AppNavigationState {
    return remember {
        AppNavigationState(initialTab, initialDetailsSubTab)
    }
}
