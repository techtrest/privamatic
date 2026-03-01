package com.techtrest.privamatic.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.techtrest.privamatic.data.model.PrivacyScore

@Composable
fun IssuesList(privacyScore: PrivacyScore, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Privacy Checks",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${privacyScore.insecureIssues.size} issue${if (privacyScore.insecureIssues.size != 1) "s" else ""} found",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Display insecure issues first
            if (privacyScore.insecureIssues.isNotEmpty()) {
                Text(
                    text = "Issues Detected (${privacyScore.insecureIssues.size})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                privacyScore.insecureIssues.forEach { issue ->
                    IssueItem(issue = issue)
                    if (issue != privacyScore.insecureIssues.last()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }

                if (privacyScore.secureIssues.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Display secure checks
            if (privacyScore.secureIssues.isNotEmpty()) {
                Text(
                    text = "Secure Settings (${privacyScore.secureIssues.size})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                privacyScore.secureIssues.forEach { issue ->
                    IssueItem(issue = issue)
                    if (issue != privacyScore.secureIssues.last()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}
