package com.techtrest.privacywidget.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.techtrest.privacywidget.data.model.QuickWin
import com.techtrest.privacywidget.data.model.QuickWinType

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

    // Determine if this quick win needs store buttons (keyboard and browser replacements)
    val needsStoreButtons = quickWinType == QuickWinType.REPLACE_KEYBOARD || quickWinType == QuickWinType.REPLACE_BROWSER

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
            if (needsStoreButtons) {
                // For keyboard and browser: Show store buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // F-Droid button
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            val searchTerm = if (quickWinType == QuickWinType.REPLACE_KEYBOARD) {
                                "keyboard"
                            } else {
                                "browser"
                            }
                            openFDroid(context, searchTerm)
                        }
                    ) {
                        Text("F-Droid")
                    }
                    // Play Store button
                    androidx.compose.material3.FilledTonalButton(
                        onClick = {
                            val searchQuery = if (quickWinType == QuickWinType.REPLACE_KEYBOARD) {
                                "privacy keyboard"
                            } else {
                                "privacy browser"
                            }
                            openPlayStore(context, searchQuery)
                        }
                    ) {
                        Text("Play Store")
                    }
                }
            } else {
                // For other quick wins: Show "Open Settings" button
                androidx.compose.material3.FilledTonalButton(
                    onClick = {
                        openSettings(context, quickWinType)
                    }
                ) {
                    Text("Open Settings")
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

/**
 * Opens Play Store with search query
 */
private fun openPlayStore(context: Context, searchQuery: String) {
    try {
        val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$searchQuery"))
        playStoreIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(playStoreIntent)
    } catch (e: Exception) {
        // Fallback to web if Play Store not available
        try {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=$searchQuery"))
            webIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(webIntent)
        } catch (e: Exception) {
            // Silently fail
        }
    }
}

/**
 * Opens relevant settings screen based on quick win type
 */
private fun openSettings(context: Context, quickWinType: QuickWinType) {
    try {
        val intent = when (quickWinType) {
            QuickWinType.DISABLE_ADVERTISING_ID -> {
                Intent(Settings.ACTION_PRIVACY_SETTINGS)
            }
            QuickWinType.ENABLE_PRIVATE_DNS -> {
                Intent(Settings.ACTION_WIRELESS_SETTINGS)
            }
            QuickWinType.DISABLE_WIFI_SCANNING -> {
                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            }
            QuickWinType.DISABLE_FIND_MY_DEVICE -> {
                Intent(Settings.ACTION_SECURITY_SETTINGS)
            }
            else -> {
                Intent(Settings.ACTION_SETTINGS)
            }
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback to general settings if deep link fails
        try {
            val fallbackIntent = Intent(Settings.ACTION_SETTINGS)
            fallbackIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(fallbackIntent)
        } catch (e: Exception) {
            // Silently fail if settings can't be opened
        }
    }
}
