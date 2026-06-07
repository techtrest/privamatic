package com.techtrest.privamatic.ui.screens

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.techtrest.privamatic.R
import com.techtrest.privamatic.data.model.ActionType
import com.techtrest.privamatic.data.model.ManualCheckState
import com.techtrest.privamatic.data.model.ManualCheckType
import com.techtrest.privamatic.ui.components.getProgressColor
import com.techtrest.privamatic.ui.components.getStatusText
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
                            contentDescription = stringResource(R.string.label_common_back)
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
            ExpandableSectionCard(title = stringResource(R.string.label_manual_check_why_this_matters)) {
                Text(
                    text = getWhyItMattersText(context, checkState.type),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                WhatToLookForContent(type = checkState.type)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. How to Check
            ExpandableSectionCard(title = stringResource(R.string.label_manual_check_how_to_check)) {
                val steps = getSteps(context, checkState.type)
                steps.forEachIndexed { index, step ->
                    Text(
                        text = stringResource(R.string.fmt_numbered_step, index + 1, step),
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
                    Text(stringResource(R.string.label_manual_check_mark_done))
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
                    Text(stringResource(R.string.label_common_open_settings))
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
                text = stringResource(R.string.label_manual_check_progress),
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
                    text = stringResource(R.string.fmt_review_period, checkState.type.periodDays),
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
                    contentDescription = if (expanded) stringResource(R.string.fmt_cd_collapse, title)
                                         else stringResource(R.string.fmt_cd_expand, title),
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
                    stringResource(R.string.copy_manual_location_look_maps),
                    stringResource(R.string.copy_manual_location_look_weather),
                    stringResource(R.string.copy_manual_location_look_social),
                    stringResource(R.string.copy_manual_location_look_shopping),
                    stringResource(R.string.copy_manual_location_look_unknown)
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
                    text = stringResource(R.string.copy_manual_camera_legitimate_header),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                val legitimate = listOf(
                    stringResource(R.string.copy_manual_camera_legitimate_1),
                    stringResource(R.string.copy_manual_camera_legitimate_2),
                    stringResource(R.string.copy_manual_camera_legitimate_3),
                    stringResource(R.string.copy_manual_camera_legitimate_4)
                )
                legitimate.forEach { item ->
                    BulletPoint(
                        text = item,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.copy_manual_camera_concerning_header),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                val concerning = listOf(
                    stringResource(R.string.copy_manual_camera_concerning_1),
                    stringResource(R.string.copy_manual_camera_concerning_2),
                    stringResource(R.string.copy_manual_camera_concerning_3),
                    stringResource(R.string.copy_manual_camera_concerning_4),
                    stringResource(R.string.copy_manual_camera_concerning_5)
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
                    text = stringResource(R.string.copy_manual_adid_verify),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            ManualCheckType.UNUSED_APPS -> {
                Text(
                    text = stringResource(R.string.copy_manual_unused_priority_header),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                val highPriority = listOf(
                    stringResource(R.string.copy_manual_unused_priority_1),
                    stringResource(R.string.copy_manual_unused_priority_2),
                    stringResource(R.string.copy_manual_unused_priority_3),
                    stringResource(R.string.copy_manual_unused_priority_4),
                    stringResource(R.string.copy_manual_unused_priority_5),
                    stringResource(R.string.copy_manual_unused_priority_6)
                )
                highPriority.forEach { item ->
                    BulletPoint(
                        text = item,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.copy_manual_unused_keep_header),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                val keep = listOf(
                    stringResource(R.string.copy_manual_unused_keep_1),
                    stringResource(R.string.copy_manual_unused_keep_2),
                    stringResource(R.string.copy_manual_unused_keep_3),
                    stringResource(R.string.copy_manual_unused_keep_4)
                )
                keep.forEach { item ->
                    BulletPoint(
                        text = item,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.copy_manual_unused_tip),
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
        text = "• $text",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    )
}

// --- Content providers ---

private fun getWhyItMattersText(context: Context, type: ManualCheckType): String {
    return when (type) {
        ManualCheckType.LOCATION_ALWAYS_ON -> context.getString(R.string.copy_manual_why_location)
        ManualCheckType.CAMERA_MIC_ACCESS -> context.getString(R.string.copy_manual_why_camera)
        ManualCheckType.UNUSED_APPS -> context.getString(R.string.copy_manual_why_unused)
        ManualCheckType.ADVERTISING_ID_CHECK -> context.getString(R.string.copy_manual_why_adid)
    }
}

private fun getSteps(context: Context, type: ManualCheckType): List<String> {
    return when (type) {
        ManualCheckType.LOCATION_ALWAYS_ON -> listOf(
            context.getString(R.string.copy_manual_step_open_settings_device),
            context.getString(R.string.copy_manual_step_nav_privacy_permissions),
            context.getString(R.string.copy_manual_location_step_3),
            context.getString(R.string.copy_manual_location_step_4),
            context.getString(R.string.copy_manual_location_step_5)
        )

        ManualCheckType.CAMERA_MIC_ACCESS -> listOf(
            context.getString(R.string.copy_manual_step_open_settings_device),
            context.getString(R.string.copy_manual_step_nav_privacy_permissions),
            context.getString(R.string.copy_manual_camera_step_3),
            context.getString(R.string.copy_manual_camera_step_4),
            context.getString(R.string.copy_manual_camera_step_5),
            context.getString(R.string.copy_manual_camera_step_6)
        )

        ManualCheckType.UNUSED_APPS -> listOf(
            context.getString(R.string.copy_manual_step_open_settings_device),
            context.getString(R.string.copy_manual_unused_step_2),
            context.getString(R.string.copy_manual_unused_step_3),
            context.getString(R.string.copy_manual_unused_step_4),
            context.getString(R.string.copy_manual_unused_step_5),
            context.getString(R.string.copy_manual_unused_step_6)
        )

        ManualCheckType.ADVERTISING_ID_CHECK -> listOf(
            context.getString(R.string.copy_manual_step_open_settings_device),
            context.getString(R.string.copy_manual_adid_step_2),
            context.getString(R.string.copy_manual_adid_step_3)
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
