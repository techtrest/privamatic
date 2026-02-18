package com.techtrest.privacywidget.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

@Composable
fun PrivacyNavigationDrawer(
    drawerState: DrawerState,
    onScoringSystemClick: () -> Unit,
    onAboutClick: () -> Unit,
    content: @Composable () -> Unit
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                windowInsets = WindowInsets(0, 0, 0, 0)
            ) {
                // Branded Header
                NavigationDrawerHeader()

                Spacer(modifier = Modifier.height(12.dp))

                // Main Navigation Items
                NavigationDrawerItem(
                    label = {
                        Text(text = "Scoring System")
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Dashboard,
                            contentDescription = null
                        )
                    },
                    selected = false,
                    onClick = onScoringSystemClick,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                // Divider before About section
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                // About Section
                NavigationDrawerItem(
                    label = {
                        Text(text = "About")
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null
                        )
                    },
                    selected = false,
                    onClick = onAboutClick,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        },
        content = content
    )
}

@Composable
private fun NavigationDrawerHeader() {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("PRIVA")
                        }
                        withStyle(SpanStyle(fontWeight = FontWeight.Normal)) {
                            append("matic")
                        }
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = "Device Privacy Auditor",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                )
            }
        }
    }
}
