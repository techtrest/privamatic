package com.techtrest.privamatic.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.techtrest.privamatic.R
import com.techtrest.privamatic.accentOnSurface
import com.techtrest.privamatic.accentOutlinedButtonColors
import com.techtrest.privamatic.data.UserScan
import com.techtrest.privamatic.data.model.QuickWin
import com.techtrest.privamatic.ui.utils.IntentHelper

/**
 * Full-screen detail view for a Quick Win.
 * Shows title, description, step-by-step instructions, and dismiss option.
 *
 * [snackbarHeight]: the result snackbar at the bottom, 0 when none shows. The content ends above it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickWinDetailScreen(
    quickWin: QuickWin,
    onBackClick: () -> Unit,
    onDismiss: () -> Unit,
    onCheckAgain: () -> Unit,
    isChecking: Boolean,
    snackbarHeight: Dp,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBackClick()
    }

    val context = LocalContext.current

    // Check again is at the end: a result that arrives while the page is open scrolls to it, so
    // the snackbar never hides it. One already there when the page opens leaves it at its top.
    val scrollState = rememberScrollState()
    var lastSnackbarHeight by remember { mutableStateOf(snackbarHeight) }
    LaunchedEffect(snackbarHeight) {
        val isNewResult = snackbarHeight > lastSnackbarHeight
        lastSnackbarHeight = snackbarHeight
        if (isNewResult) {
            withFrameNanos { } // laid out with the new height
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = quickWin.displayTitle(context),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.label_common_back)
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = stringResource(R.string.label_quick_win_ignore),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
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
                .padding(bottom = snackbarHeight)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = quickWin.type.icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Points gained, including checks the same fix also clears
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = pluralStringResource(
                        R.plurals.plural_quick_win_gain, quickWin.impact, quickWin.impact
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.accentOnSurface
                )
                quickWin.alsoFixes.forEach { issue ->
                    Text(
                        text = stringResource(
                            R.string.fmt_quick_win_also_fixes,
                            stringResource(issue.check.displayName),
                            issue.pointDeduction
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Description
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.label_quick_win_what_this_does),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(quickWin.type.description),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Instructions
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.label_quick_win_instructions),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(quickWin.type.instructions),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action button — opens relevant settings screen
            val actionType = quickWin.type.actionType
            if (actionType != null) {
                Button(
                    onClick = {
                        IntentHelper.launchActionIntent(
                            context = context,
                            actionType = actionType,
                            packageName = quickWin.relatedCheck?.packageName,
                            onReturn = UserScan.FixReturn(quickWin.relatedCheck)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        quickWin.type.actionLabel?.let { stringResource(it) }
                            ?: stringResource(R.string.label_common_open_settings)
                    )
                }
            }

            OutlinedButton(
                onClick = onCheckAgain,
                enabled = !isChecking,
                modifier = Modifier.fillMaxWidth(),
                colors = accentOutlinedButtonColors()
            ) {
                Text(
                    stringResource(
                        if (isChecking) R.string.label_check_again_checking
                        else R.string.label_check_again
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
