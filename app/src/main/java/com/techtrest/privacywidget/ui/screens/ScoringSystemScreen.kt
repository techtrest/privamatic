package com.techtrest.privacywidget.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.techtrest.privacywidget.data.model.PrivacyCheck

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoringSystemScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Scoring System",
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
            // Introduction
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Privamatic evaluates your device across security settings and surveillance tracking. Each check contributes to your overall privacy score out of 100.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Security Checks Section
            SecurityChecksSection()

            Spacer(modifier = Modifier.height(8.dp))

            // Surveillance Checks Section
            SurveillanceChecksSection()

            Spacer(modifier = Modifier.height(8.dp))

            // Footer Note
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Your score starts at 100 and points are deducted based on detected issues. The minimum score is 0.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun SecurityChecksSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Security checks
            val securityChecks = remember {
                listOf(
                    PrivacyCheck.SCREEN_LOCK,
                    PrivacyCheck.DEVICE_ENCRYPTION,
                    PrivacyCheck.NOTIFICATION_LISTENER,
                    PrivacyCheck.ACCESSIBILITY_SERVICE,
                    PrivacyCheck.USB_DEBUGGING,
                    PrivacyCheck.DEVICE_ADMIN,
                    PrivacyCheck.DEVELOPER_OPTIONS,
                    PrivacyCheck.BIOMETRIC_AUTH
                )
            }

            val securityMaxPoints = remember { securityChecks.sumOf { it.pointDeduction } }
            val sortedSecurityChecks = remember { securityChecks.sortedByDescending { it.pointDeduction } }

            // Section Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Security ($securityMaxPoints points max)",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            sortedSecurityChecks.forEachIndexed { index, check ->
                CheckPointItem(
                    checkName = check.displayName,
                    points = check.pointDeduction
                )
                if (index < sortedSecurityChecks.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun SurveillanceChecksSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Surveillance checks
            val surveillanceChecks = remember {
                listOf(
                    PrivacyCheck.PRIVATE_DNS,
                    PrivacyCheck.ADVERTISING_ID,
                    PrivacyCheck.VPN_CONNECTION,
                    PrivacyCheck.DEFAULT_BROWSER,
                    PrivacyCheck.DEFAULT_KEYBOARD,
                    PrivacyCheck.WIFI_SCANNING,
                    PrivacyCheck.FIND_MY_DEVICE,
                    PrivacyCheck.DEFAULT_SMS,
                    PrivacyCheck.DEFAULT_EMAIL,
                    PrivacyCheck.DEFAULT_LAUNCHER,
                    PrivacyCheck.ALWAYS_ON_VPN,
                    PrivacyCheck.GOOGLE_CHROME,
                    PrivacyCheck.GMAIL_APP,
                    PrivacyCheck.GOOGLE_MAPS,
                    PrivacyCheck.GOOGLE_PHOTOS,
                    PrivacyCheck.GOOGLE_DRIVE,
                    PrivacyCheck.FACEBOOK_APP,
                    PrivacyCheck.INSTAGRAM_APP,
                    PrivacyCheck.WHATSAPP_APP,
                    PrivacyCheck.MESSENGER_APP,
                    PrivacyCheck.EDGE_APP,
                    PrivacyCheck.OUTLOOK_APP,
                    PrivacyCheck.CHATGPT_APP,
                    PrivacyCheck.GOOGLE_GEMINI,
                    PrivacyCheck.MICROSOFT_COPILOT,
                    PrivacyCheck.CLAUDE_APP,
                    PrivacyCheck.META_AI,
                    PrivacyCheck.TIKTOK_APP,
                    PrivacyCheck.TWITTER_APP,
                    PrivacyCheck.AMAZON_SHOPPING
                )
            }

            val surveillanceMaxPoints = remember { surveillanceChecks.sumOf { it.pointDeduction } }
            val sortedSurveillanceChecks = remember { surveillanceChecks.sortedByDescending { it.pointDeduction } }

            // Section Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Surveillance & Tracking ($surveillanceMaxPoints points max)",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            sortedSurveillanceChecks.forEachIndexed { index, check ->
                CheckPointItem(
                    checkName = check.displayName,
                    points = check.pointDeduction
                )
                if (index < sortedSurveillanceChecks.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun CheckPointItem(
    checkName: String,
    points: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = checkName,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "-$points pts",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (points > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
