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
 * Full-screen guide for checking camera and microphone access.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraMicGuide(
    onBackClick: () -> Unit,
    onMarkDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Camera & Microphone Access",
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
                text = "Camera and microphone permissions allow apps to potentially record you at any time. Many apps request these permissions unnecessarily or \"just in case\" you might use a feature. A compromised app with these permissions could spy on you. Regular audits help ensure only trusted apps can access these sensitive sensors.",
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
            StepText(number = 3, text = "Tap \"Camera\" to see all apps with camera access")
            StepText(number = 4, text = "Review each app - ask yourself: \"Does this app really need camera access?\"")
            StepText(number = 5, text = "Revoke camera access for apps that don't need it")
            StepText(number = 6, text = "Go back and repeat for \"Microphone\" permissions")

            Spacer(modifier = Modifier.height(8.dp))

            // What to Look For
            Text(
                text = "What to Look For",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Apps that legitimately need camera/microphone:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            BulletPoint(text = "Video calling apps (Zoom, WhatsApp, Signal)")
            BulletPoint(text = "Camera apps and photo editors")
            BulletPoint(text = "Voice recorder apps")
            BulletPoint(text = "Social media apps (for posting photos/videos)")

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Concerning examples:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            BulletPoint(text = "Flashlight apps with camera access")
            BulletPoint(text = "Calculator apps with microphone access")
            BulletPoint(text = "Games that don't use voice or AR features")
            BulletPoint(text = "Shopping apps with no scanning features")
            BulletPoint(text = "Any app you don't recognize or haven't used recently")

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
