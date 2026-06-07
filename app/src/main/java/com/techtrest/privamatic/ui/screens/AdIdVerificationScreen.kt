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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.techtrest.privamatic.R
import com.techtrest.privamatic.data.scanner.checks.NetworkSecurityChecker
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

private const val CORRECT_ANSWER_INDEX = 2
private const val WRONG_ANSWER_RESET_DELAY_MS = 600L

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
                        text = stringResource(R.string.label_adid_screen_title),
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
                    Text(stringResource(R.string.label_adid_open_settings))
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
                    Text(stringResource(R.string.label_adid_confirm_button))
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
            text = stringResource(R.string.label_adid_deleted_headline),
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
                    text = stringResource(R.string.fmt_adid_confirmed_date, confirmedDate),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.fmt_adid_next_review, nextReviewDate),
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
            Text(stringResource(R.string.label_adid_reset))
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
                text = stringResource(R.string.label_adid_explanation_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.copy_adid_explanation_1),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.copy_adid_explanation_2),
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
    val quizAnswers = listOf(
        stringResource(R.string.label_adid_quiz_answer_1),
        stringResource(R.string.label_adid_quiz_answer_2),
        stringResource(R.string.label_adid_quiz_answer_3),
        stringResource(R.string.label_adid_quiz_answer_4)
    )

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
                text = stringResource(R.string.label_adid_quiz_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.label_adid_quiz_question),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            quizAnswers.forEachIndexed { index, answerText ->
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
