package com.techtrest.privacywidget.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.techtrest.privacywidget.data.model.ActionType
import com.techtrest.privacywidget.data.model.QuickWin
import com.techtrest.privacywidget.data.model.QuickWinType
import com.techtrest.privacywidget.ui.utils.IntentHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickWinsBottomSheet(
    quickWins: List<QuickWin>,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedQuickWin by remember { mutableStateOf<QuickWin?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (quickWins.isEmpty()) Icons.Default.CheckCircle else Icons.Default.Bolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Quick Wins",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = if (quickWins.isEmpty()) "All improvements completed!" else "Easy privacy improvements",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (quickWins.isEmpty()) {
                // All Done Message
                Text(
                    text = "Great job! You've completed all quick improvements for your device.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Quick Wins List
                quickWins.forEach { quickWin ->
                    QuickWinItem(
                        quickWin = quickWin,
                        onClick = { selectedQuickWin = quickWin },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Show Instructions Dialog
    selectedQuickWin?.let { quickWin ->
        InstructionsDialog(
            quickWin = quickWin,
            onDismiss = { selectedQuickWin = null }
        )
    }
}

@Composable
private fun QuickWinItem(
    quickWin: QuickWin,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = quickWin.type.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = quickWin.displayTitle,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Impact Badge
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "+${quickWin.impact} points",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        // Time Estimate
                        Text(
                            text = quickWin.type.timeEstimate,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = quickWin.type.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Show Instructions")
            }
        }
    }
}

@Composable
fun InstructionsDialog(
    quickWin: QuickWin,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val quickWinType = quickWin.type
    val configuration = LocalConfiguration.current

    // Determine button style based on quick win type
    val needsStoreButtons = quickWinType in listOf(
        QuickWinType.REPLACE_KEYBOARD,
        QuickWinType.REPLACE_BROWSER,
        QuickWinType.REPLACE_DEFAULT_SMS,
        QuickWinType.REPLACE_DEFAULT_EMAIL,
        QuickWinType.REPLACE_DEFAULT_LAUNCHER
    )
    val needsAppSettings = quickWinType == QuickWinType.UNINSTALL_APP

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = quickWinType.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = quickWin.displayTitle,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(max = (configuration.screenHeightDp.toFloat() * 0.6f).dp)
            ) {
                Text(
                    text = quickWinType.instructions,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            when {
                needsStoreButtons -> {
                    val searchQuery = when (quickWinType) {
                        QuickWinType.REPLACE_KEYBOARD -> "privacy keyboard"
                        QuickWinType.REPLACE_BROWSER -> "privacy browser"
                        QuickWinType.REPLACE_DEFAULT_SMS -> "privacy sms messaging"
                        QuickWinType.REPLACE_DEFAULT_EMAIL -> "privacy email client"
                        QuickWinType.REPLACE_DEFAULT_LAUNCHER -> "privacy launcher"
                        else -> ""
                    }
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = {
                                val searchTerm = when (quickWinType) {
                                    QuickWinType.REPLACE_KEYBOARD -> "keyboard"
                                    QuickWinType.REPLACE_BROWSER -> "browser"
                                    QuickWinType.REPLACE_DEFAULT_SMS -> "sms messaging"
                                    QuickWinType.REPLACE_DEFAULT_EMAIL -> "email"
                                    QuickWinType.REPLACE_DEFAULT_LAUNCHER -> "launcher"
                                    else -> ""
                                }
                                openFDroid(context, searchTerm)
                            }
                        ) {
                            Text("Browse F-Droid")
                        }
                        if (searchQuery.isNotEmpty()) {
                            Text(
                                text = "Not on F-Droid? Search '$searchQuery' on Play Store",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                needsAppSettings -> {
                    // For app uninstalls: Open the specific app's settings page
                    androidx.compose.material3.FilledTonalButton(
                        onClick = {
                            val packageName = quickWin.relatedCheck?.packageName
                            if (packageName != null) {
                                IntentHelper.launchActionIntent(
                                    context = context,
                                    actionType = ActionType.OPEN_APP_SETTINGS,
                                    packageName = packageName
                                )
                            }
                        }
                    ) {
                        Text("Open App Settings")
                    }
                }
                else -> {
                    // For settings toggles and system service revocations
                    val actionType = quickWinType.actionType
                    if (actionType != null) {
                        androidx.compose.material3.FilledTonalButton(
                            onClick = {
                                IntentHelper.launchActionIntent(
                                    context = context,
                                    actionType = actionType,
                                    packageName = quickWin.relatedCheck?.packageName
                                )
                            }
                        ) {
                            Text(quickWinType.actionLabel ?: "Open Settings")
                        }
                    }
                }
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        modifier = modifier
    )
}

/**
 * Opens F-Droid app with search if installed, otherwise opens F-Droid website
 */
private fun openFDroid(context: Context, searchTerm: String) {
    try {
        // Try to open F-Droid app with search
        val fdroidSearchIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("fdroid.app://search?q=$searchTerm")
            setPackage("org.fdroid.fdroid")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(fdroidSearchIntent)
    } catch (e: Exception) {
        // F-Droid app not available or search not supported, try opening app without search
        try {
            val fdroidIntent = context.packageManager.getLaunchIntentForPackage("org.fdroid.fdroid")
            if (fdroidIntent != null) {
                fdroidIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(fdroidIntent)
            } else {
                // F-Droid not installed, open website
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://f-droid.org/packages/"))
                webIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(webIntent)
            }
        } catch (e: Exception) {
            // Silently fail if neither works
        }
    }
}

