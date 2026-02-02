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
 * Full-screen guide for checking always-on location access.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationAlwaysOnGuide(
    onBackClick: () -> Unit,
    onMarkDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Always-On Location Access",
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
                text = "Apps with \"Always\" location access can track your movements 24/7, even when you're not using the app. Most apps only need location \"While using the app\" or not at all. Continuous tracking drains battery, creates detailed movement profiles, and poses significant privacy risks if the app is compromised or shares data with third parties.",
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
            StepText(number = 2, text = "Navigate to Privacy → Permission manager")
            StepText(number = 3, text = "Tap \"Location\"")
            StepText(number = 4, text = "Look for apps with \"Allowed all the time\" or \"Always\" access")
            StepText(number = 5, text = "For each app, tap it and change to \"Allow only while using the app\" or \"Don't allow\"")

            Spacer(modifier = Modifier.height(8.dp))

            // What to Look For
            Text(
                text = "What to Look For",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            BulletPoint(text = "Maps and navigation apps: These may legitimately need \"Always\" for features like traffic alerts, but consider if you really use those features")
            BulletPoint(text = "Weather apps: Usually don't need \"Always\" access - they work fine with \"While using the app\"")
            BulletPoint(text = "Social media apps: Almost never need \"Always\" location access")
            BulletPoint(text = "Shopping or delivery apps: Only need location when actively using them")
            BulletPoint(text = "Unknown or rarely-used apps: If you don't recognize an app with always-on access, revoke it immediately")

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
