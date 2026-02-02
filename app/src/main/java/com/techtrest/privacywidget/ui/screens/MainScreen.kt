package com.techtrest.privacywidget.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.techtrest.privacywidget.data.maintenance.MaintenanceManager
import com.techtrest.privacywidget.data.model.ManualCheckType
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
    val navigationState = rememberAppNavigationState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showInfoDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showScoringSystemScreen by remember { mutableStateOf(false) }
    var showManualChecksScreen by remember { mutableStateOf(false) }
    var showGuideScreen by remember { mutableStateOf<ManualCheckType?>(null) }
    val sheetState = rememberModalBottomSheetState()

    PrivacyNavigationDrawer(
        drawerState = drawerState,
        onScoringSystemClick = {
            showScoringSystemScreen = true
            scope.launch {
                drawerState.close()
            }
        },
        onManualChecksClick = {
            showManualChecksScreen = true
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
                        // Show selected tab content
                        when (navigationState.selectedTab) {
                            NavigationTab.DASHBOARD -> {
                                DashboardScreen(
                                    privacyScore = state.privacyScore,
                                    navigationState = navigationState,
                                    onRefresh = {
                                        viewModel.performScan()
                                    },
                                    isRefreshing = scanState is PrivacyScanState.Scanning,
                                    onNavigateToManualChecks = {
                                        showManualChecksScreen = true
                                    }
                                )
                            }

                            NavigationTab.ACTIONS -> {
                                ActionsScreen(
                                    privacyScore = state.privacyScore
                                )
                            }

                            NavigationTab.DETAILS -> {
                                DetailsScreen(
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

    // Manual Checks Screen
    if (showManualChecksScreen) {
        ManualChecksScreen(
            onBackClick = { showManualChecksScreen = false },
            onNavigateToGuide = { checkType ->
                showGuideScreen = checkType
            }
        )
    }

    // Guide Screens
    when (showGuideScreen) {
        ManualCheckType.LOCATION_ALWAYS_ON -> {
            LocationAlwaysOnGuide(
                onBackClick = { showGuideScreen = null },
                onMarkDone = {
                    scope.launch {
                        maintenanceManager.markCheckCompleted(ManualCheckType.LOCATION_ALWAYS_ON)
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
                        showGuideScreen = null
                    }
                }
            )
        }
        null -> { /* No guide screen shown */ }
    }
}
