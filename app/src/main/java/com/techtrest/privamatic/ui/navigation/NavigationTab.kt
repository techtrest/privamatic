package com.techtrest.privamatic.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.ui.graphics.vector.ImageVector

enum class NavigationTab(
    val title: String,
    val icon: ImageVector,
    val label: String
) {
    DASHBOARD(
        title = "Dashboard",
        icon = Icons.Default.Dashboard,
        label = "Dashboard"
    ),
    ACTIONS(
        title = "Actions",
        icon = Icons.Default.Bolt,
        label = "Actions"
    ),
    DETAILS(
        title = "Details",
        icon = Icons.AutoMirrored.Filled.List,
        label = "Details"
    )
}
