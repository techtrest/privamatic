package com.techtrest.privamatic.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.techtrest.privamatic.data.scanner.checks.NetworkSecurityChecker
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

private const val CORRECT_ANSWER_INDEX = 2
private const val WRONG_ANSWER_RESET_DELAY_MS = 600L

private val QUIZ_ANSWERS = listOf(
    "Replaces it with a new random ID",
    "Disables all ads on your device",
    "Removes the ID so advertisers can't link your activity across apps",
    "Stops apps from tracking you permanently"
)

/**
 * Guided verification screen for the Advertising ID manual check.
 * Walks the user through a short quiz to confirm they understand what
 * deleting their Advertising ID does, then lets them confirm completion.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdIdVerificationScreen(
    lastCompletedTimestamp: Long,
    onBackClick: () -> Unit,
    onConfirmed: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBackClick()
    }

    val context = LocalContext.current
    var showQuiz by rememberSaveable { mutableStateOf(lastCompletedTimestamp == 0L) }
    var selectedAnswer by rememberSaveable { mutableStateOf(-1) }
    var hasAnsweredCorrectly by rememberSaveable { mutableStateOf(false) }

    // Auto-clear a wrong answer after a brief delay
    LaunchedEffect(selectedAnswer) {
        if (selectedAnswer >= 0 && selectedAnswer != CORRECT_ANSWER_INDEX) {
            delay(WRONG_ANSWER_RESET_DELAY_MS)
            if (selectedAnswer != CORRECT_ANSWER_INDEX) {
                selectedAnswer = -1
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Advertising ID",
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
            if (!showQuiz) {
                ConfirmedView(
                    lastCompletedTimestamp = lastCompletedTimestamp,
                    onReset = {
                        selectedAnswer = -1
                        hasAnsweredCorrectly = false
                        showQuiz = true
                        onReset()
                    }
                )
            } else {
                // Explanation card
                ExplanationCard()

                // Quiz card
                QuizCard(
                    selectedAnswer = selectedAnswer,
                    hasAnsweredCorrectly = hasAnsweredCorrectly,
                    onAnswerSelected = { index ->
                        if (index == selectedAnswer) {
                            // Tapping the currently selected answer deselects it
                            selectedAnswer = -1
                            hasAnsweredCorrectly = false
                        } else {
                            selectedAnswer = index
                            hasAnsweredCorrectly = (index == CORRECT_ANSWER_INDEX)
                        }
                    }
                )

                // Open settings button
                OutlinedButton(
                    onClick = { launchAdSettings(context) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Ad Settings")
                }

                // Confirm button — enabled only after correct quiz answer
                Button(
                    onClick = {
                        saveVerification(context)
                        onConfirmed()
                    },
                    enabled = hasAnsweredCorrectly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.TrackChanges,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("I've deleted my Advertising ID")
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy")
private const val REVIEW_PERIOD_DAYS = 180L

@Composable
private fun ConfirmedView(
    lastCompletedTimestamp: Long,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val confirmedDate = remember(lastCompletedTimestamp) {
        Instant.ofEpochMilli(lastCompletedTimestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(DATE_FORMATTER)
    }
    val nextReviewDate = remember(lastCompletedTimestamp) {
        Instant.ofEpochMilli(lastCompletedTimestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .plusDays(REVIEW_PERIOD_DAYS)
            .format(DATE_FORMATTER)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Advertising ID Deleted",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
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
                    text = "Confirmed on $confirmedDate",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Next review: $nextReviewDate",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.error
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Reset verification")
        }
    }
}

@Composable
private fun ExplanationCard(
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
                text = "What is an Advertising ID?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Your Advertising ID is a unique identifier assigned to your device. Advertisers use it to track your activity across different apps and services, building a detailed profile of your interests and habits to serve targeted ads.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Deleting your Advertising ID removes it entirely. Android will no longer provide an advertising identifier to apps, breaking cross-app tracking.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuizCard(
    selectedAnswer: Int,
    hasAnsweredCorrectly: Boolean,
    onAnswerSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Quick Check",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "What does deleting your Advertising ID do?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            QUIZ_ANSWERS.forEachIndexed { index, answerText ->
                val isSelected = selectedAnswer == index
                val isCorrectSelected = hasAnsweredCorrectly && index == CORRECT_ANSWER_INDEX
                val isWrongSelected = isSelected && !hasAnsweredCorrectly

                if (isCorrectSelected) {
                    Button(
                        onClick = {},
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(answerText)
                    }
                } else {
                    OutlinedButton(
                        onClick = { onAnswerSelected(index) },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(
                            width = 1.dp,
                            color = when {
                                isWrongSelected -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.outline
                            }
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = when {
                                isWrongSelected -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    ) {
                        Text(answerText)
                    }
                }
            }
        }
    }
}

private fun saveVerification(context: Context) {
    context.getSharedPreferences(NetworkSecurityChecker.AD_ID_PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(NetworkSecurityChecker.KEY_AD_ID_VERIFIED, true)
        .apply()
}

private fun launchAdSettings(context: Context) {
    val intentsToTry = listOf(
        // Direct GMS Ads Identity settings (works on GrapheneOS with sandboxed GMS)
        Intent().setClassName(
            "com.google.android.gms",
            "com.google.android.gms.adsidentity.settings.AdsIdentitySettingsActivity"
        ),
        // Fallback: older GMS ads settings activity
        Intent().setClassName(
            "com.google.android.gms",
            "com.google.android.gms.adid.settings.AdsSettingsActivity"
        ),
        // Fallback: standard Privacy settings page
        Intent(Settings.ACTION_PRIVACY_SETTINGS)
    ).map { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

    for (intent in intentsToTry) {
        try {
            context.startActivity(intent)
            return
        } catch (e: ActivityNotFoundException) {
            continue
        }
    }
}
