package com.techtrest.privamatic.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.ui.graphics.vector.ImageVector
import com.techtrest.privamatic.R

enum class NavigationTab(
    @StringRes val title: Int,
    val icon: ImageVector,
    @StringRes val label: Int
) {
    DASHBOARD(
        title = R.string.nav_tab_dashboard,
        icon = Icons.Default.Dashboard,
        label = R.string.nav_tab_dashboard
    ),
    ACTIONS(
        title = R.string.nav_tab_actions,
        icon = Icons.Default.Bolt,
        label = R.string.nav_tab_actions
    ),
    DETAILS(
        title = R.string.nav_tab_details,
        icon = Icons.AutoMirrored.Filled.List,
        label = R.string.nav_tab_details
    )
}
