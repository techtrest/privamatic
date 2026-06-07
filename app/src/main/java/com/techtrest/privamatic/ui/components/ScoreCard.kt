package com.techtrest.privamatic.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.techtrest.privamatic.R
import com.techtrest.privamatic.data.model.PrivacyScore
import com.techtrest.privamatic.data.scanner.PrivacyScoreCalculator

@Composable
fun ScoreCard(
    privacyScore: PrivacyScore,
    modifier: Modifier = Modifier
) {
    val scoreColor = getScoreColor(privacyScore.score)

    val animatedProgress by animateFloatAsState(
        targetValue = privacyScore.scorePercentage / 100f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "score_progress"
    )

    val animatedScore by animateIntAsState(
        targetValue = privacyScore.score,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "score_value"
    )

    val rating = PrivacyScoreCalculator.getScoreRating(privacyScore.score)

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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.label_score_card_title),
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(140.dp)
            ) {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(140.dp),
                    color = scoreColor,
                    strokeWidth = 12.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Text(
                    text = "$animatedScore",
                    style = MaterialTheme.typography.displayLarge,
                    color = scoreColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = rating.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = scoreColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = rating.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun getScoreColor(score: Int): Color {
    return when {
        score >= 85 -> MaterialTheme.colorScheme.primary // Excellent
        score >= 70 -> MaterialTheme.colorScheme.primary // Good
        score >= 50 -> MaterialTheme.colorScheme.tertiary // Fair (orange-ish)
        score >= 30 -> MaterialTheme.colorScheme.error // Poor
        else -> MaterialTheme.colorScheme.error // Critical
    }
}
