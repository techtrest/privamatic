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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.techtrest.privamatic.Cream
import com.techtrest.privamatic.R
import com.techtrest.privamatic.data.DetailsViewPreferences
import com.techtrest.privamatic.data.model.FlaggedApp
import com.techtrest.privamatic.data.model.PrivacyCategory
import com.techtrest.privamatic.data.model.PrivacyCheck
import com.techtrest.privamatic.data.model.PrivacyScore
import com.techtrest.privamatic.data.model.SdkScanResult
import com.techtrest.privamatic.data.scanner.PrivacyScoreCalculator
import com.techtrest.privamatic.ui.components.CategoryGroup
import com.techtrest.privamatic.ui.components.IssueDisplayStatus
import com.techtrest.privamatic.ui.components.statusCounts
import com.techtrest.privamatic.ui.navigation.ChecksView
import com.techtrest.privamatic.ui.navigation.DetailsTab
import com.techtrest.privamatic.ui.viewmodel.SdkScanState
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

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
    sdkScanResult: SdkScanResult?,
    sdkScanState: SdkScanState,
    onRunSdkScan: () -> Unit,
    onNavigateToManualChecks: () -> Unit,
    isBreakdownRequested: Boolean,
    onBreakdownShown: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Dark primary is the lighter brand green (#00854A): onPrimary at 0.8 alpha only reaches
    // 3.6:1 on it, so dark mode keeps unselected labels at full onPrimary (4.7:1) and marks
    // the selected tab with the Cream indicator plus a heavier weight instead.
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    Column(modifier = modifier.fillMaxSize()) {
        // Tabs sit on the app bar colour so app bar + tabs read as one header.
        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                    color = Cream
                )
            },
            divider = {}
        ) {
            DetailsTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    selectedContentColor = MaterialTheme.colorScheme.onPrimary,
                    unselectedContentColor = if (isDark) MaterialTheme.colorScheme.onPrimary
                                             else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    // TabRow splits width evenly, so at large font scales a long label
                    // would wrap and make its tab taller than the rest. Truncate
                    // instead of wrapping to keep the row even.
                    text = {
                        Text(
                            text = stringResource(tab.label),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = when {
                                !isDark -> null
                                selectedTab == tab -> FontWeight.SemiBold
                                else -> FontWeight.Normal
                            }
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            DetailsTab.CHECKS -> ChecksTab(
                privacyScore = privacyScore,
                trustedPackages = trustedPackages,
                onNavigateToManualChecks = onNavigateToManualChecks,
                isBreakdownRequested = isBreakdownRequested,
                onBreakdownShown = onBreakdownShown
            )
            DetailsTab.APPS -> AppsContent(
                flaggedApps = flaggedApps,
                trustedPackages = trustedPackages,
                isAppsBannerDismissed = isAppsBannerDismissed,
                onDismissAppsBanner = onDismissAppsBanner,
                onTrustApp = onTrustApp,
                onUntrustApp = onUntrustApp
            )
            DetailsTab.SDK -> SdkTabContent(
                scanResult = sdkScanResult,
                scanState = sdkScanState,
                onRunScan = onRunSdkScan
            )
        }
    }
}

/**
 * Checks tab: a fixed header (summary + view toggle) over either the check list or the
 * score breakdown. Same data, two views, so this is a toggle rather than another tab.
 */
@Composable
private fun ChecksTab(
    privacyScore: PrivacyScore,
    trustedPackages: Set<String>,
    onNavigateToManualChecks: () -> Unit,
    isBreakdownRequested: Boolean,
    onBreakdownShown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewPrefs = remember { DetailsViewPreferences(context) }
    var view by remember { mutableStateOf(viewPrefs.getChecksView()) }
    fun showView(newView: ChecksView) {
        view = newView
        viewPrefs.setChecksView(newView)
    }

    // Hoisted out of the list so expansion and scroll position survive the view toggle, and
    // so a Breakdown row can open its category and scroll to its check.
    var expandedCategories by remember { mutableStateOf(emptySet<PrivacyCategory>()) }
    // Expanded check rows (recommendation shown); a jump opens only the target's.
    var expandedChecks by remember { mutableStateOf(emptySet<PrivacyCheck>()) }
    var scrollTarget by remember { mutableStateOf<PrivacyCheck?>(null) }
    // Set once a jump has scrolled; cleared when its fade ends or the user changes view/expansion,
    // so a plain toggle back to the list never replays it.
    var highlightedCheck by remember { mutableStateOf<PrivacyCheck?>(null) }
    val listState = rememberLazyListState()

    LaunchedEffect(isBreakdownRequested) {
        if (isBreakdownRequested) {
            highlightedCheck = null
            showView(ChecksView.BREAKDOWN)
            onBreakdownShown()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        ChecksHeader(
            privacyScore = privacyScore,
            trustedPackages = trustedPackages,
            view = view,
            onToggleView = {
                highlightedCheck = null
                showView(if (view == ChecksView.LIST) ChecksView.BREAKDOWN else ChecksView.LIST)
            }
        )

        when (view) {
            ChecksView.LIST -> ChecksContent(
                privacyScore = privacyScore,
                trustedPackages = trustedPackages,
                listState = listState,
                expandedCategories = expandedCategories,
                onToggleCategory = { category ->
                    highlightedCheck = null
                    expandedCategories = if (category in expandedCategories) expandedCategories - category
                                         else expandedCategories + category
                },
                expandedChecks = expandedChecks,
                onToggleCheck = { check ->
                    expandedChecks = if (check in expandedChecks) expandedChecks - check
                                     else expandedChecks + check
                },
                scrollTarget = scrollTarget,
                onScrollTargetShown = {
                    highlightedCheck = scrollTarget
                    scrollTarget = null
                },
                highlightedCheck = highlightedCheck,
                onHighlightFinished = { highlightedCheck = null }
            )
            ChecksView.BREAKDOWN -> BreakdownContent(
                privacyScore = privacyScore,
                onCheckClick = { check ->
                    PrivacyCategory.getCategoryForCheck(check)?.let { category ->
                        // A jump shows only the target's category and opens only its row;
                        // plain toggles keep expansion.
                        expandedCategories = setOf(category)
                        expandedChecks = setOf(check)
                        scrollTarget = check
                        showView(ChecksView.LIST)
                    }
                },
                onManualChecksClick = onNavigateToManualChecks
            )
        }
    }
}

/**
 * List view: status summary over every category, counted exactly like the category chips.
 * Breakdown view: the breakdown's total (checks + manual checks) and the resulting score.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChecksHeader(
    privacyScore: PrivacyScore,
    trustedPackages: Set<String>,
    view: ChecksView,
    onToggleView: () -> Unit,
    modifier: Modifier = Modifier
) {
    val summary = when (view) {
        ChecksView.LIST -> {
            val counts = remember(privacyScore, trustedPackages) {
                PrivacyCategory.entries
                    .flatMap { PrivacyCategory.getIssuesForCategory(it, privacyScore) }
                    .statusCounts(trustedPackages)
            }
            val issuesCount = counts[IssueDisplayStatus.FAIL] ?: 0
            val passCount = counts[IssueDisplayStatus.PASS] ?: 0
            val infoCount = counts[IssueDisplayStatus.INFO] ?: 0
            val issues = pluralStringResource(R.plurals.plural_category_issues, issuesCount, issuesCount)
            val pass = pluralStringResource(R.plurals.plural_category_pass, passCount, passCount)
            if (infoCount > 0) {
                val review = pluralStringResource(R.plurals.plural_category_review, infoCount, infoCount)
                stringResource(R.string.fmt_details_checks_summary_review, issues, pass, review)
            } else {
                stringResource(R.string.fmt_details_checks_summary, issues, pass)
            }
        }
        ChecksView.BREAKDOWN -> {
            val total = PrivacyScoreCalculator.totalDeduction(privacyScore)
            if (total > 0) stringResource(R.string.fmt_details_breakdown_total, total, privacyScore.score)
            else stringResource(R.string.label_details_breakdown_no_deductions)
        }
    }
    // Names the view the button switches to, for both the tooltip and TalkBack.
    val toggleLabel = stringResource(
        if (view == ChecksView.LIST) R.string.label_details_view_breakdown
        else R.string.label_details_view_checks
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = summary,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
        )
        TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip = { PlainTooltip { Text(toggleLabel) } },
            state = rememberTooltipState()
        ) {
            IconButton(onClick = onToggleView) {
                Icon(
                    imageVector = if (view == ChecksView.LIST) Icons.Outlined.BarChart
                                  else Icons.AutoMirrored.Outlined.List,
                    contentDescription = toggleLabel,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Category list. When [scrollTarget] is set, scrolls its category into view, then far enough
 * that the check's row is fully visible (keeping the category header if it fits), and calls
 * [onScrollTargetShown]. [highlightedCheck]'s row gets the fading jump highlight.
 */
@Composable
private fun ChecksContent(
    privacyScore: PrivacyScore,
    trustedPackages: Set<String>,
    listState: LazyListState,
    expandedCategories: Set<PrivacyCategory>,
    onToggleCategory: (PrivacyCategory) -> Unit,
    expandedChecks: Set<PrivacyCheck>,
    onToggleCheck: (PrivacyCheck) -> Unit,
    scrollTarget: PrivacyCheck?,
    onScrollTargetShown: () -> Unit,
    highlightedCheck: PrivacyCheck?,
    onHighlightFinished: () -> Unit,
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

    // Lazy item index: [security label, security…, surveillance label, surveillance…]
    fun itemIndex(category: PrivacyCategory): Int =
        if (category in securityCategories) 1 + securityCategories.indexOf(category)
        else 2 + securityCategories.size + surveillanceCategories.indexOf(category)

    // Target row's top/bottom inside its category card, reported once the card lays it out.
    var targetRowBounds by remember { mutableStateOf<IntRange?>(null) }
    val bottomMarginPx = with(LocalDensity.current) { 16.dp.roundToPx() }

    LaunchedEffect(scrollTarget) {
        val category = scrollTarget?.let { PrivacyCategory.getCategoryForCheck(it) } ?: return@LaunchedEffect
        val index = itemIndex(category)
        listState.scrollToItem(index)
        val row = snapshotFlow { targetRowBounds }.filterNotNull().first()
        val viewport = listState.layoutInfo.let { it.viewportEndOffset - it.viewportStartOffset }
        val offset = (row.last - viewport + bottomMarginPx).coerceIn(0, row.first)
        listState.scrollToItem(index, offset)
        // Cleared only after use: the row may report before this effect starts.
        targetRowBounds = null
        onScrollTargetShown()
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 16.dp),
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
                isExpanded = category in expandedCategories,
                onToggleExpanded = { onToggleCategory(category) },
                expandedChecks = expandedChecks,
                onToggleCheck = onToggleCheck,
                trustedPackages = trustedPackages,
                scrollTarget = scrollTarget,
                onScrollTargetPlaced = { top, bottom -> targetRowBounds = top..bottom },
                highlightedCheck = highlightedCheck,
                onHighlightFinished = onHighlightFinished
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
                isExpanded = category in expandedCategories,
                onToggleExpanded = { onToggleCategory(category) },
                expandedChecks = expandedChecks,
                onToggleCheck = onToggleCheck,
                trustedPackages = trustedPackages,
                scrollTarget = scrollTarget,
                onScrollTargetPlaced = { top, bottom -> targetRowBounds = top..bottom },
                highlightedCheck = highlightedCheck,
                onHighlightFinished = onHighlightFinished
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
                // The row is the switch for accessibility: checked state, and disabled for
                // data-broker apps that can't be trusted.
                .toggleable(
                    value = isTrusted,
                    enabled = !app.isBlacklisted,
                    role = Role.Switch,
                    onValueChange = onToggle
                )
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
                onCheckedChange = null,
                enabled = !app.isBlacklisted
            )
        }
    }
}
