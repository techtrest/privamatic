package com.techtrest.privacywidget.ui.screens.guides

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Full-screen guide for reviewing and removing unused apps.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnusedAppsGuide(
    onBackClick: () -> Unit,
    onMarkDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Unused Apps Review",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                windowInsets = WindowInsets.statusBars
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Why This Matters
            Text(
                text = "Why This Matters",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Unused apps are a hidden privacy and security risk. They still have all their permissions, continue receiving data access, may have unpatched vulnerabilities, and increase your attack surface. Apps you installed months or years ago might have been updated with new tracking features or sold to different companies. Regular cleanups reduce these risks.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // How to Check
            Text(
                text = "How to Check",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            StepText(number = 1, text = "Open Settings on your device")
            StepText(number = 2, text = "Navigate to Apps or Application Manager")
            StepText(number = 3, text = "Look for a sort option and select \"Last used\" or \"Unused apps\"")
            StepText(number = 4, text = "Scroll through the list and identify apps you haven't opened in months")
            StepText(number = 5, text = "For each unused app, ask: \"Will I actually use this again?\"")
            StepText(number = 6, text = "Uninstall apps you don't need - you can always reinstall later if needed")

            Spacer(modifier = Modifier.height(8.dp))

            // What to Look For
            Text(
                text = "What to Look For",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "High-priority apps to remove:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            BulletPoint(text = "Apps not opened in 3+ months")
            BulletPoint(text = "Old games you no longer play")
            BulletPoint(text = "Abandoned trial apps or free trials")
            BulletPoint(text = "Apps for events or trips that already happened")
            BulletPoint(text = "Duplicate apps (multiple weather apps, calculators, etc.)")
            BulletPoint(text = "Apps from companies you no longer trust")

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Consider keeping:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            BulletPoint(text = "System apps (be careful not to remove critical system components)")
            BulletPoint(text = "Banking or financial apps (even if used infrequently)")
            BulletPoint(text = "Travel apps if you travel regularly")
            BulletPoint(text = "Emergency or safety apps")

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Pro tip: If you're unsure whether you'll need an app again, uninstall it anyway. Most apps keep your account data server-side, so you can reinstall and log back in if needed.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.tertiary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Mark as Done Button
            Button(
                onClick = onMarkDone,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Mark as Done")
            }
        }
    }
}

@Composable
private fun StepText(
    number: Int,
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = "$number. $text",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    )
}

@Composable
private fun BulletPoint(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = "• $text",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    )
}
