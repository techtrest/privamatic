package com.techtrest.privacywidget.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.techtrest.privacywidget.data.maintenance.MaintenanceManager
import com.techtrest.privacywidget.data.model.ManualCheckType
import com.techtrest.privacywidget.data.model.ScoreHistory
import com.techtrest.privacywidget.data.model.QuickWin
import com.techtrest.privacywidget.ui.components.AboutDialog
import com.techtrest.privacywidget.ui.components.BottomNavigationBar
import com.techtrest.privacywidget.ui.components.PrivacyNavigationDrawer
import com.techtrest.privacywidget.ui.components.PrivacyTopAppBar
import com.techtrest.privacywidget.ui.components.ScoringInfoDialog
import com.techtrest.privacywidget.ui.navigation.NavigationTab
import com.techtrest.privacywidget.ui.navigation.rememberAppNavigationState
import com.techtrest.privacywidget.ui.screens.guides.CameraMicGuide
import com.techtrest.privacywidget.ui.screens.guides.LocationAlwaysOnGuide
import com.techtrest.privacywidget.ui.screens.guides.UnusedAppsGuide
import com.techtrest.privacywidget.ui.viewmodel.PrivacyScanState
import com.techtrest.privacywidget.ui.viewmodel.PrivacyViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: PrivacyViewModel = viewModel()) {
    val context = LocalContext.current
    val maintenanceManager = remember { MaintenanceManager(context) }
    val scanState by viewModel.scanState.collectAsState()
    val scoreHistory by viewModel.scoreHistory.collectAsState()
    val navigationState = rememberAppNavigationState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Manual checks state (needed for Actions tab)
    val checkStates by maintenanceManager.getCheckStates().collectAsState(initial = emptyList())

    var showInfoDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showScoringSystemScreen by remember { mutableStateOf(false) }
    var showManualCheckDetail by remember { mutableStateOf<ManualCheckType?>(null) }
    var showGuideScreen by remember { mutableStateOf<ManualCheckType?>(null) }
    var showQuickWinDetail by remember { mutableStateOf<QuickWin?>(null) }
    val sheetState = rememberModalBottomSheetState()

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
    BackHandler(enabled = showScoringSystemScreen) {
        showScoringSystemScreen = false
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
            scope.launch {
                drawerState.close()
            }
        },
        onAboutClick = {
            showAboutDialog = true
            scope.launch {
                drawerState.close()
            }
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
                            Text(text = "Initializing scanner...")
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
                                        text = "Scanning privacy settings...",
                                        fontSize = 16.sp,
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
                                    onRefresh = {
                                        viewModel.performScan()
                                    },
                                    isRefreshing = scanState is PrivacyScanState.Scanning,
                                    onNavigateToManualChecks = {
                                        navigationState.selectTab(NavigationTab.ACTIONS)
                                    }
                                )
                                1 -> ActionsScreen(
                                    privacyScore = state.privacyScore,
                                    checkStates = checkStates,
                                    onNavigateToGuide = { checkType ->
                                        showManualCheckDetail = checkType
                                    },
                                    onMarkCheckDone = { checkType ->
                                        scope.launch {
                                            maintenanceManager.markCheckCompleted(checkType)
                                            viewModel.performScan()
                                        }
                                    },
                                    onQuickWinSelected = { quickWin ->
                                        showQuickWinDetail = quickWin
                                    }
                                )
                                else -> DetailsScreen(
                                    privacyScore = state.privacyScore,
                                    selectedSubTab = navigationState.selectedDetailsSubTab,
                                    onSubTabSelected = { navigationState.selectDetailsSubTab(it) }
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
                                        text = "Error",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = state.message,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(onClick = { viewModel.performScan() }) {
                                        Text("Retry")
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

    // Manual Check Detail Screen
    showManualCheckDetail?.let { checkType ->
        val checkState = checkStates.find { it.type == checkType }
        checkState?.let { state ->
            ManualCheckDetailScreen(
                checkState = state,
                onBackClick = { showManualCheckDetail = null },
                onViewGuide = {
                    showManualCheckDetail = null
                    showGuideScreen = checkType
                },
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

    // Guide Screens
    when (showGuideScreen) {
        ManualCheckType.LOCATION_ALWAYS_ON -> {
            LocationAlwaysOnGuide(
                onBackClick = { showGuideScreen = null },
                onMarkDone = {
                    scope.launch {
                        maintenanceManager.markCheckCompleted(ManualCheckType.LOCATION_ALWAYS_ON)
                        viewModel.performScan()
                        showGuideScreen = null
                    }
                }
            )
        }
        ManualCheckType.CAMERA_MIC_ACCESS -> {
            CameraMicGuide(
                onBackClick = { showGuideScreen = null },
                onMarkDone = {
                    scope.launch {
                        maintenanceManager.markCheckCompleted(ManualCheckType.CAMERA_MIC_ACCESS)
                        viewModel.performScan()
                        showGuideScreen = null
                    }
                }
            )
        }
        ManualCheckType.UNUSED_APPS -> {
            UnusedAppsGuide(
                onBackClick = { showGuideScreen = null },
                onMarkDone = {
                    scope.launch {
                        maintenanceManager.markCheckCompleted(ManualCheckType.UNUSED_APPS)
                        viewModel.performScan()
                        showGuideScreen = null
                    }
                }
            )
        }
        null -> { /* No guide screen shown */ }
    }

    // Quick Win Detail Screen
    showQuickWinDetail?.let { quickWin ->
        QuickWinDetailScreen(
            quickWin = quickWin,
            onBackClick = { showQuickWinDetail = null }
        )
    }
}
