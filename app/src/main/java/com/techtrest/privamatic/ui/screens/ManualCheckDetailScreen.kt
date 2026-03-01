package com.techtrest.privamatic.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.techtrest.privamatic.Amber
import com.techtrest.privamatic.data.model.ActionType
import com.techtrest.privamatic.data.model.ManualCheckState
import com.techtrest.privamatic.data.model.ManualCheckType
import com.techtrest.privamatic.ui.utils.IntentHelper

private val HERO_ICON_SIZE = 32.dp
private val HERO_ICON_PADDING = 12.dp
private val PROGRESS_BAR_HEIGHT = 8.dp

/**
 * Detail screen for a Manual Check.
 * Hero area provides at-a-glance context, progress card shows timing,
 * educational content is collapsed by default, and action buttons sit
 * inline below the last section.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualCheckDetailScreen(
    checkState: ManualCheckState,
    onBackClick: () -> Unit,
    onMarkDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBackClick()
    }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
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
                .padding(16.dp)
        ) {
            // 1. Hero area
            Spacer(modifier = Modifier.height(8.dp))
            HeroSection(checkState = checkState)
            Spacer(modifier = Modifier.height(24.dp))

            // 2. Progress card
            ProgressCard(checkState = checkState)
            Spacer(modifier = Modifier.height(16.dp))

            // 3. Why This Matters (merged with What to Look For)
            ExpandableSectionCard(title = "Why This Matters") {
                Text(
                    text = getWhyItMattersText(checkState.type),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                WhatToLookForContent(type = checkState.type)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. How to Check
            ExpandableSectionCard(title = "How to Check") {
                val steps = getSteps(checkState.type)
                steps.forEachIndexed { index, step ->
                    Text(
                        text = "${index + 1}. $step",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(
                            bottom = if (index < steps.lastIndex) 8.dp else 0.dp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 5. Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onMarkDone,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Mark as Done")
                }

                OutlinedButton(
                    onClick = {
                        IntentHelper.launchActionIntent(
                            context = context,
                            actionType = getSettingsActionType(checkState.type)
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Settings")
                }
            }
        }
    }
}

/**
 * Hero area: large icon, title, and a short teaser describing the check.
 */
@Composable
private fun HeroSection(
    checkState: ManualCheckState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondary
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier.padding(HERO_ICON_PADDING),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = checkState.type.icon,
                    contentDescription = null,
                    modifier = Modifier.size(HERO_ICON_SIZE),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Text(
            text = checkState.type.displayName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Text(
            text = checkState.type.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
    }
}

/**
 * Progress card showing the review bar, interval, and days remaining.
 */
@Composable
private fun ProgressCard(
    checkState: ManualCheckState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Progress",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            val progressValue = checkState.fillPercentage.coerceIn(0f, 1f)
            val progressColor = getProgressColor(checkState)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PROGRESS_BAR_HEIGHT)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                if (progressValue > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressValue)
                            .background(progressColor)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Review every ${checkState.type.periodDays} days",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = getStatusText(checkState),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Accordion card: shows only the section title when collapsed.
 * Tapping the row toggles the educational content open or closed.
 * State survives configuration changes via rememberSaveable.
 */
@Composable
private fun ExpandableSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
        ) {
            // Tappable header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) { expanded = !expanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = if (expanded) {
                        Icons.Default.KeyboardArrowUp
                    } else {
                        Icons.Default.KeyboardArrowDown
                    },
                    contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Conditionally shown content
            if (expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    content()
                }
            }
        }
    }
}

/**
 * Renders the "What to Look For" content, which varies in structure per check type.
 */
@Composable
private fun WhatToLookForContent(
    type: ManualCheckType,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        when (type) {
            ManualCheckType.LOCATION_ALWAYS_ON -> {
                val items = listOf(
                    "Maps and navigation apps: These may legitimately need \"Always\" for features like traffic alerts, but consider if you really use those features",
                    "Weather apps: Usually don't need \"Always\" access \u2014 they work fine with \"While using the app\"",
                    "Social media apps: Almost never need \"Always\" location access",
                    "Shopping or delivery apps: Only need location when actively using them",
                    "Unknown or rarely-used apps: If you don't recognize an app with always-on access, revoke it immediately"
                )
                items.forEachIndexed { index, item ->
                    BulletPoint(
                        text = item,
                        modifier = Modifier.padding(
                            bottom = if (index < items.lastIndex) 8.dp else 0.dp
                        )
                    )
                }
            }

            ManualCheckType.CAMERA_MIC_ACCESS -> {
                Text(
                    text = "Apps that legitimately need camera/microphone:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                val legitimate = listOf(
                    "Video calling apps (Zoom, WhatsApp, Signal)",
                    "Camera apps and photo editors",
                    "Voice recorder apps",
                    "Social media apps (for posting photos/videos)"
                )
                legitimate.forEach { item ->
                    BulletPoint(
                        text = item,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Concerning examples:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                val concerning = listOf(
                    "Flashlight apps with camera access",
                    "Calculator apps with microphone access",
                    "Games that don't use voice or AR features",
                    "Shopping apps with no scanning features",
                    "Any app you don't recognize or haven't used recently"
                )
                concerning.forEach { item ->
                    BulletPoint(
                        text = item,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }

            ManualCheckType.ADVERTISING_ID_CHECK -> {
                Text(
                    text = "Verify that your Advertising ID has been deleted in Settings → Privacy → Ads.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            ManualCheckType.UNUSED_APPS -> {
                Text(
                    text = "High-priority apps to remove:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                val highPriority = listOf(
                    "Apps not opened in 3+ months",
                    "Old games you no longer play",
                    "Abandoned trial apps or free trials",
                    "Apps for events or trips that already happened",
                    "Duplicate apps (multiple weather apps, calculators, etc.)",
                    "Apps from companies you no longer trust"
                )
                highPriority.forEach { item ->
                    BulletPoint(
                        text = item,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Consider keeping:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                val keep = listOf(
                    "System apps (be careful not to remove critical system components)",
                    "Banking or financial apps (even if used infrequently)",
                    "Travel apps if you travel regularly",
                    "Emergency or safety apps"
                )
                keep.forEach { item ->
                    BulletPoint(
                        text = item,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Tip: If you're unsure whether you'll need an app again, uninstall it anyway. Most apps keep your account data server-side, so you can reinstall and log back in if needed.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun BulletPoint(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = "\u2022 $text",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    )
}

// --- Content providers ---

private fun getWhyItMattersText(type: ManualCheckType): String {
    return when (type) {
        ManualCheckType.LOCATION_ALWAYS_ON ->
            "Apps with \"Always\" location access can track your movements 24/7, even when you're not using the app. Most apps only need location \"While using the app\" or not at all. Continuous tracking drains battery, creates detailed movement profiles, and poses significant privacy risks if the app is compromised or shares data with third parties."

        ManualCheckType.CAMERA_MIC_ACCESS ->
            "Camera and microphone permissions allow apps to potentially record you at any time. Many apps request these permissions unnecessarily or \"just in case\" you might use a feature. A compromised app with these permissions could spy on you. Regular audits help ensure only trusted apps can access these sensitive sensors."

        ManualCheckType.UNUSED_APPS ->
            "Unused apps are a hidden privacy and security risk. They still have all their permissions, continue receiving data access, may have unpatched vulnerabilities, and increase your attack surface. Apps you installed months or years ago might have been updated with new tracking features or sold to different companies. Regular cleanups reduce these risks."

        ManualCheckType.ADVERTISING_ID_CHECK ->
            "Your Advertising ID is a unique identifier that lets advertisers link your activity across different apps and services to build a profile of your habits and interests. Deleting it resets this profile and prevents further cross-app behavioral tracking until a new ID is created."
    }
}

private fun getSteps(type: ManualCheckType): List<String> {
    return when (type) {
        ManualCheckType.LOCATION_ALWAYS_ON -> listOf(
            "Open Settings on your device",
            "Navigate to Privacy \u2192 Permission manager",
            "Tap \"Location\"",
            "Look for apps with \"Allowed all the time\" or \"Always\" access",
            "For each app, tap it and change to \"Allow only while using the app\" or \"Don't allow\""
        )

        ManualCheckType.CAMERA_MIC_ACCESS -> listOf(
            "Open Settings on your device",
            "Navigate to Privacy \u2192 Permission manager",
            "Tap \"Camera\" to see all apps with camera access",
            "Review each app \u2014 ask yourself: \"Does this app really need camera access?\"",
            "Revoke camera access for apps that don't need it",
            "Go back and repeat for \"Microphone\" permissions"
        )

        ManualCheckType.UNUSED_APPS -> listOf(
            "Open Settings on your device",
            "Navigate to Apps or Application Manager",
            "Look for a sort option and select \"Last used\" or \"Unused apps\"",
            "Scroll through the list and identify apps you haven't opened in months",
            "For each unused app, ask: \"Will I actually use this again?\"",
            "Uninstall apps you don't need \u2014 you can always reinstall later if needed"
        )

        ManualCheckType.ADVERTISING_ID_CHECK -> listOf(
            "Open Settings on your device",
            "Navigate to Privacy \u2192 Ads",
            "Tap \"Delete Advertising ID\" and confirm"
        )
    }
}

private fun getSettingsActionType(type: ManualCheckType): ActionType {
    return when (type) {
        ManualCheckType.LOCATION_ALWAYS_ON -> ActionType.LOCATION_SETTINGS
        ManualCheckType.CAMERA_MIC_ACCESS -> ActionType.PRIVACY_SETTINGS
        ManualCheckType.UNUSED_APPS -> ActionType.APP_MANAGEMENT_SETTINGS
        ManualCheckType.ADVERTISING_ID_CHECK -> ActionType.PRIVACY_SETTINGS
    }
}

@Composable
private fun getProgressColor(checkState: ManualCheckState): Color {
    return when {
        checkState.fillPercentage >= 1f -> MaterialTheme.colorScheme.primary
        checkState.fillPercentage >= 0.96f -> MaterialTheme.colorScheme.tertiary
        checkState.fillPercentage >= 0.86f -> Amber
        else -> MaterialTheme.colorScheme.primary
    }
}

private fun getStatusText(checkState: ManualCheckState): String {
    return when {
        checkState.isOverdue -> "Review needed"
        checkState.daysRemaining == 1 -> "1 day remaining"
        else -> "${checkState.daysRemaining} days remaining"
    }
}
