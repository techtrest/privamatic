package com.techtrest.privamatic.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.techtrest.privamatic.PrivacyWidgetProvider
import com.techtrest.privamatic.data.HistoryFilter
import com.techtrest.privamatic.data.PrivacySnapshotRepository
import com.techtrest.privamatic.data.QuickWinsDetector
import com.techtrest.privamatic.data.ScoreHistoryRepository
import com.techtrest.privamatic.data.SdkScanRepository
import com.techtrest.privamatic.data.TrustedAppsAdjuster
import com.techtrest.privamatic.data.TrustedAppsRepository
import com.techtrest.privamatic.data.model.FlaggedApp
import com.techtrest.privamatic.data.model.PrivacyScore
import com.techtrest.privamatic.data.model.PrivacySnapshot
import com.techtrest.privamatic.data.model.QuickWin
import com.techtrest.privamatic.data.model.ScoreHistory
import com.techtrest.privamatic.data.model.SdkScanResult
import com.techtrest.privamatic.data.model.isFullyTrusted
import com.techtrest.privamatic.data.scanner.PrivacyScanner
import com.techtrest.privamatic.data.scanner.SdkScanner
import com.techtrest.privamatic.data.util.PackageManagerUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class PrivacyScanState {
    data object Idle : PrivacyScanState()
    data object Scanning : PrivacyScanState()
    data class Success(val privacyScore: PrivacyScore) : PrivacyScanState()
    data class Error(val message: String) : PrivacyScanState()
}

sealed class SdkScanState {
    data object Idle : SdkScanState()
    data object Scanning : SdkScanState()
    data object Error : SdkScanState()
}

class PrivacyViewModel(application: Application) : AndroidViewModel(application) {

    private val privacyScanner = PrivacyScanner(application)
    private val scoreHistoryRepository = ScoreHistoryRepository(application)
    private val snapshotRepository = PrivacySnapshotRepository(application)
    private val trustedAppsRepository = TrustedAppsRepository(application)
    private val sdkScanner = SdkScanner(application)
    private val sdkScanRepository = SdkScanRepository(application)

    private val _scanState = MutableStateFlow<PrivacyScanState>(PrivacyScanState.Idle)
    val scanState: StateFlow<PrivacyScanState> = _scanState.asStateFlow()

    private val _scoreHistory = MutableStateFlow<ScoreHistory?>(null)
    val scoreHistory: StateFlow<ScoreHistory?> = _scoreHistory.asStateFlow()

    // Raw scan result, stored so trust changes can recalculate without re-scanning
    private val _rawPrivacyScore = MutableStateFlow<PrivacyScore?>(null)

    // Raw quick wins (before trust filtering), updated on each scan
    private val _rawQuickWins = MutableStateFlow<List<QuickWin>>(emptyList())

    val trustedPackages: StateFlow<Set<String>> = trustedAppsRepository.trustedPackages
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val isAppsBannerDismissed: StateFlow<Boolean> = trustedAppsRepository.isAppsBannerDismissed
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // Quick wins with trusted apps filtered out; reactive to both scan results and whitelist changes
    val filteredQuickWins: StateFlow<List<QuickWin>> = combine(
        _rawQuickWins,
        _rawPrivacyScore,
        trustedPackages
    ) { rawWins, rawScore, trusted ->
        if (trusted.isEmpty() || rawScore == null) rawWins
        else rawWins.filter { quickWin ->
            val issue = rawScore.issues.find { it.check == quickWin.relatedCheck }
            issue == null || !issue.isFullyTrusted(trusted)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _flaggedApps = MutableStateFlow<List<FlaggedApp>>(emptyList())
    val flaggedApps: StateFlow<List<FlaggedApp>> = _flaggedApps.asStateFlow()

    private val _selectedFilter = MutableStateFlow(HistoryFilter.MONTH)
    val selectedFilter: StateFlow<HistoryFilter> = _selectedFilter.asStateFlow()

    private val _historySnapshots = MutableStateFlow<List<PrivacySnapshot>>(emptyList())
    val historySnapshots: StateFlow<List<PrivacySnapshot>> = _historySnapshots.asStateFlow()

    private val _sdkScanResults = MutableStateFlow<SdkScanResult?>(null)
    val sdkScanResults: StateFlow<SdkScanResult?> = _sdkScanResults.asStateFlow()

    private val _sdkScanState = MutableStateFlow<SdkScanState>(SdkScanState.Idle)
    val sdkScanState: StateFlow<SdkScanState> = _sdkScanState.asStateFlow()

    private var currentScanJob: Job? = null
    private var isInitialLoad = true

    init {
        // Recompute score reactively whenever the whitelist changes
        viewModelScope.launch {
            trustedPackages.collect { trusted ->
                val rawScore = _rawPrivacyScore.value ?: return@collect
                if (_scanState.value is PrivacyScanState.Scanning) return@collect
                _scanState.value = PrivacyScanState.Success(TrustedAppsAdjuster.computeAdjustedScore(rawScore, trusted))
                PrivacyWidgetProvider.requestImmediateUpdate(getApplication())
            }
        }
        performScan()
        loadSdkResults()
    }

    fun runSdkScan() {
        if (_sdkScanState.value is SdkScanState.Scanning) return
        viewModelScope.launch {
            _sdkScanState.value = SdkScanState.Scanning
            try {
                val trusted = trustedAppsRepository.trustedPackages.first()
                val findings = sdkScanner.scan(trusted)
                val result = SdkScanResult(
                    timestamp = System.currentTimeMillis(),
                    findings = findings
                )
                sdkScanRepository.save(result)
                _sdkScanResults.value = result
                _sdkScanState.value = SdkScanState.Idle
            } catch (_: Exception) {
                _sdkScanState.value = SdkScanState.Error
            }
        }
    }

    fun loadSdkResults() {
        viewModelScope.launch {
            sdkScanRepository.load()?.let { cached ->
                if (_sdkScanResults.value == null) {
                    _sdkScanResults.value = cached
                }
            }
        }
    }

    fun performScan() {
        currentScanJob?.cancel()
        currentScanJob = viewModelScope.launch {
            _scanState.value = PrivacyScanState.Scanning
            try {
                val scanDeferred = async { privacyScanner.performCompleteScan() }

                if (isInitialLoad) {
                    val result = scanDeferred.await()
                    onScanSuccess(result)
                    isInitialLoad = false
                } else {
                    val delayDeferred = async { delay(MINIMUM_SCAN_DISPLAY_TIME_MS) }
                    awaitAll(scanDeferred, delayDeferred)
                    val result = scanDeferred.await()
                    onScanSuccess(result)
                }

                PrivacyWidgetProvider.requestImmediateUpdate(getApplication())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _scanState.value = PrivacyScanState.Error(
                    e.message ?: "An unknown error occurred during scanning"
                )
                isInitialLoad = false
            }
        }
    }

    fun trustApp(packageName: String) {
        viewModelScope.launch { trustedAppsRepository.trustApp(packageName) }
    }

    fun untrustApp(packageName: String) {
        viewModelScope.launch { trustedAppsRepository.untrustApp(packageName) }
    }

    fun dismissAppsBanner() {
        viewModelScope.launch { trustedAppsRepository.dismissAppsBanner() }
    }

    fun setHistoryFilter(filter: HistoryFilter) {
        _selectedFilter.value = filter
        viewModelScope.launch {
            _historySnapshots.value = snapshotRepository.getSnapshots(filter)
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            _historySnapshots.value = snapshotRepository.getSnapshots(_selectedFilter.value)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            snapshotRepository.clearAll()
            _historySnapshots.value = emptyList()
        }
    }

    private suspend fun onScanSuccess(result: PrivacyScore) {
        _rawPrivacyScore.value = result
        _rawQuickWins.value = QuickWinsDetector.detectQuickWins(result, getApplication())
        _flaggedApps.value = computeFlaggedApps(result)
        val trusted = trustedAppsRepository.trustedPackages.first()
        val adjustedScore = TrustedAppsAdjuster.computeAdjustedScore(result, trusted)
        _scoreHistory.value = scoreHistoryRepository.recordScore(adjustedScore.score)
        _historySnapshots.value = snapshotRepository.getSnapshots(_selectedFilter.value)
        _scanState.value = PrivacyScanState.Success(adjustedScore)
    }

    private fun computeFlaggedApps(rawScore: PrivacyScore): List<FlaggedApp> {
        val pm = getApplication<Application>().packageManager
        return rawScore.issues
            .filter { !it.isSecure && it.flaggedPackages.isNotEmpty() }
            .flatMap { issue ->
                issue.flaggedPackages.map { packageName ->
                    FlaggedApp(
                        packageName = packageName,
                        appName = PackageManagerUtil.getAppName(pm, packageName),
                        associatedCheck = issue.check,
                        isBlacklisted = FlaggedApp.isBlacklisted(packageName),
                        isSystemApp = PackageManagerUtil.isSystemApp(pm, packageName)
                    )
                }
            }
            .filter { !it.isSystemApp || it.isBlacklisted }
            .distinctBy { it.packageName }
    }

    companion object {
        private const val MINIMUM_SCAN_DISPLAY_TIME_MS = 800L
    }
}
