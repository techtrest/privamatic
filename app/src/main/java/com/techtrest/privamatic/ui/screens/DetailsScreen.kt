package com.techtrest.privamatic.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.techtrest.privamatic.R
import com.techtrest.privamatic.data.model.FlaggedApp
import com.techtrest.privamatic.data.model.PrivacyCategory
import com.techtrest.privamatic.data.model.PrivacyScore
import com.techtrest.privamatic.ui.components.CategoryGroup
import com.techtrest.privamatic.ui.navigation.DetailsTab

@Composable
fun DetailsScreen(
    privacyScore: PrivacyScore,
    flaggedApps: List<FlaggedApp>,
    trustedPackages: Set<String>,
    selectedTab: DetailsTab,
    onTabSelected: (DetailsTab) -> Unit,
    isAppsBannerDismissed: Boolean,
    onDismissAppsBanner: () -> Unit,
    onTrustApp: (String) -> Unit,
    onUntrustApp: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab.ordinal) {
            DetailsTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    text = { Text(tab.label) }
                )
            }
        }

        when (selectedTab) {
            DetailsTab.CHECKS -> ChecksContent(
                privacyScore = privacyScore,
                trustedPackages = trustedPackages
            )
            DetailsTab.APPS -> AppsContent(
                flaggedApps = flaggedApps,
                trustedPackages = trustedPackages,
                isAppsBannerDismissed = isAppsBannerDismissed,
                onDismissAppsBanner = onDismissAppsBanner,
                onTrustApp = onTrustApp,
                onUntrustApp = onUntrustApp
            )
        }
    }
}

@Composable
private fun ChecksContent(
    privacyScore: PrivacyScore,
    trustedPackages: Set<String>,
    modifier: Modifier = Modifier
) {
    val securityCategories = listOf(PrivacyCategory.SYSTEM_SECURITY)

    val surveillanceCategories = listOf(
        PrivacyCategory.NETWORK_PRIVACY,
        PrivacyCategory.GOOGLE_SERVICES,
        PrivacyCategory.DEFAULT_APPS,
        PrivacyCategory.GOOGLE_APPS,
        PrivacyCategory.META_FACEBOOK_APPS,
        PrivacyCategory.MICROSOFT_APPS,
        PrivacyCategory.AI_AND_OTHER_APPS
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.details_section_security),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        items(securityCategories, key = { it.name }) { category ->
            CategoryGroup(
                category = category,
                privacyScore = privacyScore,
                trustedPackages = trustedPackages
            )
        }

        item {
            Text(
                text = stringResource(R.string.details_section_surveillance),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )
        }

        items(surveillanceCategories, key = { it.name }) { category ->
            CategoryGroup(
                category = category,
                privacyScore = privacyScore,
                trustedPackages = trustedPackages
            )
        }
    }
}

@Composable
private fun AppsContent(
    flaggedApps: List<FlaggedApp>,
    trustedPackages: Set<String>,
    isAppsBannerDismissed: Boolean,
    onDismissAppsBanner: () -> Unit,
    onTrustApp: (String) -> Unit,
    onUntrustApp: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!isAppsBannerDismissed) {
            item(key = "banner") {
                AppsBannerCard(onDismiss = onDismissAppsBanner)
            }
        }

        if (flaggedApps.isEmpty()) {
            item(key = "empty") {
                EmptyAppsState()
            }
        } else {
            items(flaggedApps, key = { it.packageName }) { app ->
                AppTrustRow(
                    app = app,
                    isTrusted = app.packageName in trustedPackages,
                    onToggle = { checked ->
                        if (checked) onTrustApp(app.packageName)
                        else onUntrustApp(app.packageName)
                    }
                )
            }
        }
    }
}

@Composable
private fun AppsBannerCard(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.copy_details_apps_banner),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.label_common_dismiss),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyAppsState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.label_details_no_flagged_apps),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AppTrustRow(
    app: FlaggedApp,
    isTrusted: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val iconBitmap: Bitmap? = remember(app.packageName) {
        try {
            context.packageManager.getApplicationIcon(app.packageName).toBitmap()
        } catch (_: Exception) { null }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isTrusted) MaterialTheme.colorScheme.primaryContainer
                             else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconBitmap != null) {
                Image(
                    painter = BitmapPainter(iconBitmap.asImageBitmap()),
                    contentDescription = stringResource(R.string.fmt_app_icon_cd, app.appName),
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Android,
                    contentDescription = stringResource(R.string.fmt_app_icon_cd, app.appName),
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = if (isTrusted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = if (isTrusted) stringResource(R.string.label_details_app_trusted)
                                     else stringResource(R.string.label_details_app_flagged),
                tint = if (isTrusted) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = isTrusted,
                onCheckedChange = if (app.isBlacklisted) null else onToggle,
                enabled = !app.isBlacklisted
            )
        }
    }
}
