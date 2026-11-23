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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.techtrest.privamatic.R
import com.techtrest.privamatic.accentOnSurface
import com.techtrest.privamatic.accentOutlinedButtonColors
import com.techtrest.privamatic.data.model.PrivacyIssue
import com.techtrest.privamatic.data.model.PrivacyCheck
import com.techtrest.privamatic.data.model.isFullyTrusted
import com.techtrest.privamatic.ui.utils.IntentHelper

/** How a check row reads at a glance; shared by the row icon and the category chip. */
internal enum class IssueDisplayStatus { PASS, FAIL, INFO }

/**
 * Unknown results and installed informational apps are INFO, never PASS or FAIL, whatever
 * their isSecure value (which only drives quick wins and tips). Trust only turns a FAIL
 * into a PASS.
 */
internal fun PrivacyIssue.displayStatus(trustedPackages: Set<String>): IssueDisplayStatus = when {
    isUnknown -> IssueDisplayStatus.INFO
    check.isInformational && !isSecure -> IssueDisplayStatus.INFO
    isSecure || isFullyTrusted(trustedPackages) -> IssueDisplayStatus.PASS
    else -> IssueDisplayStatus.FAIL
}

/**
 * Check row. Expansion is hoisted so a Breakdown jump can open the target row's
 * recommendation.
 */
@Composable
fun IssueItem(
    issue: PrivacyIssue,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    trustedPackages: Set<String> = emptySet(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val allPackagesTrusted = remember(issue.flaggedPackages, trustedPackages) {
        issue.isFullyTrusted(trustedPackages)
    }
    val effectivelySecure = issue.isSecure || allPackagesTrusted

    val isInformational = issue.check.isInformational && !issue.isSecure
    val displayStatus = issue.displayStatus(trustedPackages)
    val statusIcon = when (displayStatus) {
        IssueDisplayStatus.FAIL -> Icons.Outlined.RemoveCircleOutline
        IssueDisplayStatus.PASS -> Icons.Outlined.CheckCircle
        IssueDisplayStatus.INFO -> Icons.Outlined.Info
    }
    val statusIconTint = when (displayStatus) {
        IssueDisplayStatus.FAIL -> MaterialTheme.colorScheme.error
        IssueDisplayStatus.PASS -> MaterialTheme.colorScheme.primary
        IssueDisplayStatus.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusIconDesc = when {
        issue.isUnknown -> stringResource(R.string.label_issue_unknown)
        isInformational -> stringResource(R.string.label_issue_informational)
        effectivelySecure -> stringResource(R.string.label_issue_secure)
        else -> stringResource(R.string.label_issue_detected)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleExpanded)
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
                modifier = Modifier.size(20.dp)
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

            // Point deduction badge - not shown for informational, unknown or effectively-trusted items
            if (!effectivelySecure && !isInformational && !issue.isUnknown) {
                DeductionChip(points = issue.pointDeduction)
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
                            issue.rowActionType?.let { actionType ->
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        IntentHelper.launchActionIntent(
                                            context = context,
                                            actionType = actionType,
                                            packageName = issue.check.packageName,
                                            rescanOnReturn = true
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = accentOutlinedButtonColors()
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
                        } else if (!effectivelySecure || issue.check == PrivacyCheck.BIOMETRIC_AUTH) {
                            Text(
                                text = stringResource(R.string.label_issue_recommendation),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.accentOnSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when {
                                    effectivelySecure && issue.check == PrivacyCheck.BIOMETRIC_AUTH ->
                                        stringResource(R.string.privacy_check_biometric_auth_passing_note)
                                    issue.isPreinstalledApp -> stringResource(
                                        issue.systemAppRecommendation,
                                        stringResource(issue.check.displayName)
                                    )
                                    else -> stringResource(issue.recommendation)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Action button
                            issue.rowActionType?.let { actionType ->
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        IntentHelper.launchActionIntent(
                                            context = context,
                                            actionType = actionType,
                                            packageName = issue.check.packageName,
                                            rescanOnReturn = true
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = accentOutlinedButtonColors()
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

/** Soft "−N" point badge: error text on a pale errorContainer tint. Checks rows and Breakdown. */
@Composable
internal fun DeductionChip(points: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = StatusTintAlpha),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = stringResource(R.string.fmt_deduction_chip, points),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
