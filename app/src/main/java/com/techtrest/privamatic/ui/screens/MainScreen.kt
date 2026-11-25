package com.techtrest.privamatic.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.techtrest.privamatic.Cream
import com.techtrest.privamatic.R
import androidx.lifecycle.viewmodel.compose.viewModel
import com.techtrest.privamatic.data.PrivacyTipSelector
import com.techtrest.privamatic.data.maintenance.MaintenanceManager
import com.techtrest.privamatic.data.maintenance.PrivacyTipHistory
import com.techtrest.privamatic.data.maintenance.QuickWinDismissalManager
import com.techtrest.privamatic.data.HistoryFilter
import com.techtrest.privamatic.data.ScanOutcome
import com.techtrest.privamatic.data.UserScan
import com.techtrest.privamatic.data.model.PrivacyTip
import com.techtrest.privamatic.data.model.ManualCheckType
import com.techtrest.privamatic.data.model.QuickWinType
import com.techtrest.privamatic.data.model.ScoreHistory
import com.techtrest.privamatic.data.model.QuickWin
import com.techtrest.privamatic.ui.components.AboutDialog
import com.techtrest.privamatic.ui.components.BottomNavigationBar
import com.techtrest.privamatic.ui.components.PrivacyNavigationDrawer
import com.techtrest.privamatic.ui.components.PrivacyTopAppBar
import com.techtrest.privamatic.ui.components.ScoringInfoDialog
import com.techtrest.privamatic.ui.navigation.NavigationTab
import com.techtrest.privamatic.ui.navigation.rememberAppNavigationState
import com.techtrest.privamatic.ui.utils.IntentHelper
import com.techtrest.privamatic.ui.utils.ResourceFeedbackStrings
import com.techtrest.privamatic.ui.utils.ScanFeedbackText
import com.techtrest.privamatic.ui.viewmodel.PrivacyScanState
import com.techtrest.privamatic.ui.viewmodel.PrivacyViewModel
import com.techtrest.privamatic.ui.viewmodel.ScanEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: PrivacyViewModel = viewModel()) {
    val context = LocalContext.current
    val maintenanceManager = remember { MaintenanceManager(context) }
    val dismissalManager = remember { QuickWinDismissalManager(context) }
    val tipHistory = remember { PrivacyTipHistory(context) }
    val scanState by viewModel.scanState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val scoreHistory by viewModel.scoreHistory.collectAsState()
    val historySnapshots by viewModel.historySnapshots.collectAsState()
    val hasHistory by viewModel.hasHistory.collectAsState()

    // Back from Settings after a fix deep link: rescan once so the score shows the fix.
    LifecycleResumeEffect(Unit) {
        IntentHelper.consumeRescanOnReturn()?.let { viewModel.performScan(it) }
        onPauseOrDispose { }
    }
    val selectedHistoryFilter by viewModel.selectedFilter.collectAsState()
    val trustedPackages by viewModel.trustedPackages.collectAsState()
    val isAppsBannerDismissed by viewModel.isAppsBannerDismissed.collectAsState()
    val flaggedApps by viewModel.flaggedApps.collectAsState()
    val filteredQuickWins by viewModel.filteredQuickWins.collectAsState()
    val sdkScanResults by viewModel.sdkScanResults.collectAsState()
    val sdkScanState by viewModel.sdkScanState.collectAsState()
    val navigationState = rememberAppNavigationState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Manual checks state (needed for Actions tab)
    val checkStates by maintenanceManager.getCheckStates().collectAsState(initial = emptyList())

    // Dismissed Quick Wins state
    val dismissedCheckNames by dismissalManager.getDismissedCheckNames()
        .collectAsState(initial = emptySet())

    // Privacy tips — revision counter allows "Next tip" to trigger reselection
    var tipRevision by remember { mutableStateOf(0) }
    // ID of the tip on screen, so reselection can exclude it and always show something new
    val displayedTipId = remember { mutableStateOf<String?>(null) }
    val currentTip: PrivacyTip? = remember(scanState, tipRevision) {
        val score = (scanState as? PrivacyScanState.Success)?.privacyScore ?: return@remember null
        PrivacyTipSelector.selectTip(
            score,
            tipHistory.getRecentlyShownIds(),
            excludeId = displayedTipId.value
        )
    }

    // Record the displayed tip as shown as a side effect, not inside remember
    LaunchedEffect(currentTip) {
        currentTip?.let {
            tipHistory.markShown(it.id)
            displayedTipId.value = it.id
        }
    }

    var showInfoDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showScoringSystemScreen by remember { mutableStateOf(false) }
    var showSettingsScreen by remember { mutableStateOf(false) }
    var showManualCheckDetail by remember { mutableStateOf<ManualCheckType?>(null) }
    var showQuickWinDetail by remember { mutableStateOf<QuickWin?>(null) }
    var showAdIdVerification by remember { mutableStateOf(false) }
    // Pull to refresh shows its own spinner; other scans only the bar under the top bar
    var isPullRefresh by remember { mutableStateOf(false) }
    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) isPullRefresh = false
    }
    val snackbarHostState = remember { SnackbarHostState() }
    var bottomBarHeightPx by remember { mutableStateOf(0) }
    var snackbarHeightPx by remember { mutableStateOf(0) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val pagerState = rememberPagerState(pageCount = { NavigationTab.entries.size })

    // Sync: pager fully settles → update bottom nav selection.
    // Uses settledPage instead of currentPage to avoid firing mid-animation,
    // which would cause a circular feedback loop and hijack programmatic navigation.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            navigationState.selectTab(NavigationTab.entries[page])
        }
    }

    // Sync: user taps bottom nav or tile click → animate pager to that page
    LaunchedEffect(navigationState.selectedTab) {
        val page = navigationState.selectedTab.ordinal
        if (pagerState.currentPage == page) return@LaunchedEffect
        try {
            pagerState.animateScrollToPage(page)
        } catch (e: CancellationException) {
            // A newer tab took over
            ensureActive()
            // A swipe did. Back on the page it started from, the collector above sees no change,
            // so follow the swipe from here: the tab, and a Breakdown request, must not stay.
            withFrameNanos { }
            snapshotFlow { pagerState.isScrollInProgress }.first { !it }
            navigationState.selectTab(NavigationTab.entries[pagerState.currentPage])
        }
    }

    // Result of a user scan: update the open Quick Win page, then one snackbar.
    // A result that arrives while the app is in the background waits for the user to come back.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        val strings = ResourceFeedbackStrings(context.resources)
        var snackbarJob: Job? = null
        val snackbarScope = this // outlives the collector, so leaving the app doesn't dismiss it
        fun showResult(event: ScanEvent) {
            if (event is ScanEvent.Finished) showQuickWinDetail = showQuickWinDetail?.let(event::updated)

            val text = when (event) {
                is ScanEvent.Finished -> ScanFeedbackText.text(event.outcome, strings)
                is ScanEvent.Failed -> strings.string(R.string.copy_snackbar_scan_failed)
            }
            val action: Pair<Int, () -> Unit>? = when (event) {
                is ScanEvent.Failed ->
                    R.string.label_snackbar_try_again to { viewModel.performScan(event.trigger) }
                is ScanEvent.Finished -> {
                    val outcome = event.outcome
                    when {
                        outcome is ScanOutcome.NotFixedYet -> R.string.label_check_again to {
                            viewModel.performScan(UserScan.CheckAgain(outcome.check))
                        }
                        outcome is ScanOutcome.Loss || outcome is ScanOutcome.Mixed ||
                            (outcome is ScanOutcome.Gain && outcome.hasLosses) ->
                            R.string.label_snackbar_details to {
                                showManualCheckDetail = null
                                showQuickWinDetail = null
                                showAdIdVerification = false
                                showScoringSystemScreen = false
                                showSettingsScreen = false
                                navigationState.closeHistory()
                                scope.launch { drawerState.close() }
                                navigationState.openBreakdown()
                            }
                        else -> null
                    }
                }
            }
            // The newer snackbar replaces the older
            snackbarJob?.cancel()
            snackbarJob = snackbarScope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = text,
                    actionLabel = action?.let { strings.string(it.first) },
                    duration = if (action == null) SnackbarDuration.Short else SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) action?.second?.invoke()
            }
        }
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // Immediate: an event taken from the channel is handled before the app can stop
            withContext(Dispatchers.Main.immediate) {
                viewModel.scanEvents.collect { showResult(it) }
            }
        }
    }

    // Handle back gesture with proper navigation hierarchy
    // Note: Guide screens handle their own back gesture via BackHandler in each guide
    BackHandler(enabled = showAdIdVerification) {
        showAdIdVerification = false
    }

    BackHandler(enabled = navigationState.showHistoryScreen) {
        navigationState.closeHistory()
    }

    BackHandler(enabled = showScoringSystemScreen) {
        showScoringSystemScreen = false
    }

    BackHandler(enabled = showSettingsScreen) {
        showSettingsScreen = false
    }
    
    BackHandler(enabled = showAboutDialog) {
        showAboutDialog = false
    }
    
    BackHandler(enabled = showInfoDialog) {
        showInfoDialog = false
    }
    
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch {
            drawerState.close()
        }
    }

    PrivacyNavigationDrawer(
        drawerState = drawerState,
        onScoringSystemClick = {
            showScoringSystemScreen = true
            scope.launch { drawerState.close() }
        },
        onHistoryClick = {
            navigationState.openHistory()
            scope.launch { drawerState.close() }
        },
        onSettingsClick = {
            showSettingsScreen = true
            scope.launch { drawerState.close() }
        },
        onAboutClick = {
            showAboutDialog = true
            scope.launch { drawerState.close() }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                PrivacyTopAppBar(
                    onMenuClick = {
                        scope.launch {
                            drawerState.open()
                        }
                    },
                    onRescanClick = {
                        viewModel.performScan(UserScan.Rescan)
                    },
                    onInfoClick = {
                        showInfoDialog = true
                    },
                    isScanning = isRefreshing || scanState is PrivacyScanState.Scanning
                )
            },
            bottomBar = {
                // Only show bottom nav when in success state
                if (scanState is PrivacyScanState.Success) {
                    Box(modifier = Modifier.onSizeChanged { bottomBarHeightPx = it.height }) {
                        BottomNavigationBar(
                            selectedTab = navigationState.selectedTab,
                            onTabSelected = { tab ->
                                navigationState.selectTab(tab)
                            }
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .consumeWindowInsets(paddingValues)
            ) {
                when (val state = scanState) {
                    is PrivacyScanState.Idle -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = stringResource(R.string.label_main_initializing))
                        }
                    }

                    is PrivacyScanState.Scanning -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(48.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = stringResource(R.string.label_main_scanning),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    is PrivacyScanState.Success -> {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            beyondViewportPageCount = 1
                        ) { page ->
                            when (page) {
                                0 -> DashboardScreen(
                                    privacyScore = state.privacyScore,
                                    scoreHistory = scoreHistory,
                                    navigationState = navigationState,
                                    allQuickWins = filteredQuickWins,
                                    dismissedCheckNames = dismissedCheckNames,
                                    onRefresh = {
                                        isPullRefresh = true
                                        viewModel.performScan(UserScan.Rescan)
                                    },
                                    isRefreshing = isRefreshing && isPullRefresh,
                                    onNavigateToManualChecks = {
                                        navigationState.selectTab(NavigationTab.ACTIONS)
                                    }
                                )
                                1 -> ActionsScreen(
                                    allQuickWins = filteredQuickWins,
                                    checkStates = checkStates,
                                    dismissedCheckNames = dismissedCheckNames,
                                    currentTip = currentTip,
                                    onNavigateToGuide = { checkType ->
                                        if (checkType == ManualCheckType.ADVERTISING_ID_CHECK) {
                                            showAdIdVerification = true
                                        } else {
                                            showManualCheckDetail = checkType
                                        }
                                    },
                                    onMarkCheckDone = { checkType ->
                                        scope.launch {
                                            maintenanceManager.markCheckCompleted(checkType)
                                            viewModel.performScan(UserScan.ManualDone(checkType))
                                        }
                                    },
                                    onQuickWinSelected = { quickWin ->
                                        if (quickWin.type == QuickWinType.DISABLE_ADVERTISING_ID) {
                                            showAdIdVerification = true
                                        } else {
                                            showQuickWinDetail = quickWin
                                        }
                                    },
                                    onRestoreQuickWin = { quickWin ->
                                        scope.launch {
                                            dismissalManager.restore(quickWin)
                                        }
                                    },
                                    onNextTip = { tipRevision++ }
                                )
                                else -> DetailsScreen(
                                    privacyScore = state.privacyScore,
                                    flaggedApps = flaggedApps,
                                    trustedPackages = trustedPackages,
                                    selectedTab = navigationState.selectedDetailsTab,
                                    onTabSelected = { tab -> navigationState.selectDetailsTab(tab) },
                                    isAppsBannerDismissed = isAppsBannerDismissed,
                                    onDismissAppsBanner = { viewModel.dismissAppsBanner() },
                                    onTrustApp = { pkg -> viewModel.trustApp(pkg) },
                                    onUntrustApp = { pkg -> viewModel.untrustApp(pkg) },
                                    sdkScanResult = sdkScanResults,
                                    sdkScanState = sdkScanState,
                                    onRunSdkScan = { viewModel.runSdkScan() },
                                    onNavigateToManualChecks = {
                                        navigationState.selectTab(NavigationTab.ACTIONS)
                                    },
                                    isBreakdownRequested = navigationState.isBreakdownRequested,
                                    onBreakdownShown = { navigationState.onBreakdownShown() }
                                )
                            }
                        }
                        // On top of the content, so it never shifts the layout. Header colour behind
                        // the whole bar (its track has gaps), Cream like the tab indicator: onPrimary
                        // white vanishes against the light content below.
                        if (isRefreshing) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter)
                                    .background(MaterialTheme.colorScheme.primary),
                                color = Cream,
                                trackColor = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    is PrivacyScanState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = stringResource(R.string.label_main_error),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(onClick = { viewModel.performScan() }) {
                                        Text(stringResource(R.string.label_main_retry))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Scoring Info Dialog
    if (showInfoDialog) {
        ScoringInfoDialog(
            onDismiss = { showInfoDialog = false },
            sheetState = sheetState
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AboutDialog(
            onDismiss = { showAboutDialog = false }
        )
    }

    // Scoring System Screen
    if (showScoringSystemScreen) {
        ScoringSystemScreen(
            onBackClick = { showScoringSystemScreen = false }
        )
    }

    // History Screen
    if (navigationState.showHistoryScreen) {
        HistoryScreen(
            snapshots = historySnapshots,
            hasHistory = hasHistory,
            selectedFilter = selectedHistoryFilter,
            onFilterChanged = { viewModel.setHistoryFilter(it) },
            onClearHistory = { viewModel.clearHistory() },
            onBackClick = { navigationState.closeHistory() },
            onLoadHistory = { viewModel.loadHistory() }
        )
    }

    // Settings Screen
    if (showSettingsScreen) {
        // The nightly snapshot may have landed since the last scan.
        LaunchedEffect(Unit) { viewModel.loadHistory() }
        SettingsScreen(
            onBackClick = { showSettingsScreen = false },
            canClearHistory = hasHistory,
            onClearHistory = { viewModel.clearHistory() }
        )
    }

    // Manual Check Detail Screen
    showManualCheckDetail?.let { checkType ->
        val checkState = checkStates.find { it.type == checkType }
        checkState?.let { state ->
            ManualCheckDetailScreen(
                checkState = state,
                onBackClick = { showManualCheckDetail = null },
                onMarkDone = {
                    scope.launch {
                        maintenanceManager.markCheckCompleted(checkType)
                        viewModel.performScan(UserScan.ManualDone(checkType))
                        showManualCheckDetail = null
                    }
                }
            )
        }
    }

    // Quick Win Detail Screen
    showQuickWinDetail?.let { quickWin ->
        QuickWinDetailScreen(
            quickWin = quickWin,
            onBackClick = { showQuickWinDetail = null },
            onDismiss = {
                scope.launch {
                    dismissalManager.dismiss(quickWin)
                    showQuickWinDetail = null
                }
            },
            onCheckAgain = {
                quickWin.relatedCheck?.let { viewModel.performScan(UserScan.CheckAgain(it)) }
            },
            isChecking = isRefreshing,
            snackbarHeight = with(LocalDensity.current) { snackbarHeightPx.toDp() }
        )
    }

    // Advertising ID Verification Screen
    if (showAdIdVerification) {
        AdIdVerificationScreen(
            lastCompletedTimestamp = checkStates.find { it.type == ManualCheckType.ADVERTISING_ID_CHECK }?.lastCompletedTimestamp ?: 0L,
            onBackClick = { showAdIdVerification = false },
            onConfirmed = {
                scope.launch {
                    maintenanceManager.markCheckCompleted(ManualCheckType.ADVERTISING_ID_CHECK)
                    viewModel.performScan(UserScan.AdIdConfirmed)
                    showAdIdVerification = false
                }
            },
            onReset = {
                scope.launch {
                    maintenanceManager.resetAdIdCheck()
                    viewModel.performScan(UserScan.AdIdReset)
                }
            }
        )
    }

    // Result snackbar: above every overlay screen, and above the bottom bar when that shows.
    val isOverlayShown = showScoringSystemScreen || navigationState.showHistoryScreen ||
        showSettingsScreen || showManualCheckDetail != null || showQuickWinDetail != null ||
        showAdIdVerification
    val bottomBarPadding = with(LocalDensity.current) { bottomBarHeightPx.toDp() }
    Box(modifier = Modifier.fillMaxSize()) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .then(
                    if (isOverlayShown || scanState !is PrivacyScanState.Success) {
                        Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    } else {
                        Modifier.padding(bottom = bottomBarPadding)
                    }
                )
                .onSizeChanged { snackbarHeightPx = it.height }
        )
    }
}
