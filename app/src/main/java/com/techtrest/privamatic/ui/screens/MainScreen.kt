package com.techtrest.privamatic.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.techtrest.privamatic.R
import androidx.lifecycle.viewmodel.compose.viewModel
import com.techtrest.privamatic.data.PrivacyTipSelector
import com.techtrest.privamatic.data.maintenance.MaintenanceManager
import com.techtrest.privamatic.data.maintenance.PrivacyTipHistory
import com.techtrest.privamatic.data.maintenance.QuickWinDismissalManager
import com.techtrest.privamatic.data.HistoryFilter
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
import com.techtrest.privamatic.ui.viewmodel.PrivacyScanState
import com.techtrest.privamatic.ui.viewmodel.PrivacyViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: PrivacyViewModel = viewModel()) {
    val context = LocalContext.current
    val maintenanceManager = remember { MaintenanceManager(context) }
    val dismissalManager = remember { QuickWinDismissalManager(context) }
    val tipHistory = remember { PrivacyTipHistory(context) }
    val scanState by viewModel.scanState.collectAsState()
    val scoreHistory by viewModel.scoreHistory.collectAsState()
    val historySnapshots by viewModel.historySnapshots.collectAsState()
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
    val currentTip: PrivacyTip? = remember(scanState, tipRevision) {
        val score = (scanState as? PrivacyScanState.Success)?.privacyScore ?: return@remember null
        PrivacyTipSelector.selectTip(score, tipHistory.getRecentlyShownIds())
    }

    // Record the displayed tip as shown as a side effect, not inside remember
    LaunchedEffect(currentTip) {
        currentTip?.let { tipHistory.markShown(it.id) }
    }

    var showInfoDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showScoringSystemScreen by remember { mutableStateOf(false) }
    var showSettingsScreen by remember { mutableStateOf(false) }
    var showManualCheckDetail by remember { mutableStateOf<ManualCheckType?>(null) }
    var showQuickWinDetail by remember { mutableStateOf<QuickWin?>(null) }
    var showAdIdVerification by remember { mutableStateOf(false) }
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
        if (pagerState.currentPage != navigationState.selectedTab.ordinal) {
            pagerState.animateScrollToPage(navigationState.selectedTab.ordinal)
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
                        viewModel.performScan()
                    },
                    onInfoClick = {
                        showInfoDialog = true
                    },
                    isScanning = scanState is PrivacyScanState.Scanning
                )
            },
            bottomBar = {
                // Only show bottom nav when in success state
                if (scanState is PrivacyScanState.Success) {
                    BottomNavigationBar(
                        selectedTab = navigationState.selectedTab,
                        onTabSelected = { tab ->
                            navigationState.selectTab(tab)
                        }
                    )
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
                                        viewModel.performScan()
                                    },
                                    isRefreshing = scanState is PrivacyScanState.Scanning,
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
                                            viewModel.performScan()
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
                                    onRunSdkScan = { viewModel.runSdkScan() }
                                )
                            }
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
            selectedFilter = selectedHistoryFilter,
            onFilterChanged = { viewModel.setHistoryFilter(it) },
            onClearHistory = { viewModel.clearHistory() },
            onBackClick = { navigationState.closeHistory() },
            onLoadHistory = { viewModel.loadHistory() }
        )
    }

    // Settings Screen
    if (showSettingsScreen) {
        SettingsScreen(
            onBackClick = { showSettingsScreen = false },
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
                        viewModel.performScan()
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
            }
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
                    viewModel.performScan()
                    showAdIdVerification = false
                }
            },
            onReset = {
                scope.launch {
                    maintenanceManager.resetAdIdCheck()
                    viewModel.performScan()
                }
            }
        )
    }
}
