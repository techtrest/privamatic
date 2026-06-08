package com.techtrest.privamatic.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.techtrest.privamatic.R
import com.techtrest.privamatic.data.model.PrivacyIssue
import com.techtrest.privamatic.data.model.isFullyTrusted
import com.techtrest.privamatic.ui.utils.IntentHelper

@Composable
fun IssueItem(
    issue: PrivacyIssue,
    trustedPackages: Set<String> = emptySet(),
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val allPackagesTrusted = remember(issue.flaggedPackages, trustedPackages) {
        issue.isFullyTrusted(trustedPackages)
    }
    val effectivelySecure = issue.isSecure || allPackagesTrusted

    val isInformational = issue.check.isInformational && !issue.isSecure
    val statusIcon = when {
        isInformational -> Icons.Default.Info
        effectivelySecure -> Icons.Default.CheckCircle
        else -> Icons.Default.Warning
    }
    val statusIconTint = when {
        isInformational -> MaterialTheme.colorScheme.onSurfaceVariant
        effectivelySecure -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.tertiary
    }
    val statusIconDesc = when {
        isInformational -> stringResource(R.string.label_issue_informational)
        effectivelySecure -> stringResource(R.string.label_issue_secure)
        else -> stringResource(R.string.label_issue_detected)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status icon
            Icon(
                imageVector = statusIcon,
                contentDescription = statusIconDesc,
                tint = statusIconTint,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Title and subtitle
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(issue.check.displayName),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = issue.currentStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Point deduction badge - not shown for informational or effectively-trusted items
            if (!effectivelySecure && !isInformational) {
                Surface(
                    color = MaterialTheme.colorScheme.error,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "-${issue.pointDeduction}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Expandable detail section
        if (isExpanded) {
            if (!effectivelySecure || issue.technicalDetails != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        if (isInformational) {
                            Text(
                                text = stringResource(issue.check.description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            issue.check.actionType?.let { actionType ->
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        IntentHelper.launchActionIntent(
                                            context = context,
                                            actionType = actionType,
                                            packageName = issue.check.packageName
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = issue.check.actionLabel?.let { stringResource(it) }
                                            ?: stringResource(R.string.label_issue_fix),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        } else if (!effectivelySecure) {
                            Text(
                                text = stringResource(R.string.label_issue_recommendation),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(issue.recommendation),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Action button
                            issue.check.actionType?.let { actionType ->
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        IntentHelper.launchActionIntent(
                                            context = context,
                                            actionType = actionType,
                                            packageName = issue.check.packageName
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = issue.check.actionLabel?.let { stringResource(it) }
                                            ?: stringResource(R.string.label_issue_fix),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }

                        issue.technicalDetails?.let { details ->
                            if (!effectivelySecure) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Text(
                                text = details,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                }
            }
        }
    }
}
