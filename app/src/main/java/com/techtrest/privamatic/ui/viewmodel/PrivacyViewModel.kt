package com.techtrest.privamatic.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.techtrest.privamatic.PrivacyWidgetProvider
import com.techtrest.privamatic.data.HistoryFilter
import com.techtrest.privamatic.data.PendingUserScan
import com.techtrest.privamatic.data.PrivacySnapshotRepository
import com.techtrest.privamatic.data.QuickWinsDetector
import com.techtrest.privamatic.data.ScanFeedback
import com.techtrest.privamatic.data.ScanOutcome
import com.techtrest.privamatic.data.ScoreHistoryRepository
import com.techtrest.privamatic.data.SdkScanRepository
import com.techtrest.privamatic.data.TrustedAppsAdjuster
import com.techtrest.privamatic.data.TrustedAppsRepository
import com.techtrest.privamatic.data.UserScan
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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class PrivacyScanState {
    data object Idle : PrivacyScanState()
    data object Scanning : PrivacyScanState()
    data class Success(val privacyScore: PrivacyScore) : PrivacyScanState()
    data class Error(val message: String) : PrivacyScanState()
}

/** One-shot result of a scan while an earlier result was on screen. */
sealed class ScanEvent {
    /** [quickWins]: the list the finished scan produced, trust filter applied. */
    data class Finished(
        val outcome: ScanOutcome,
        val trigger: UserScan,
        val quickWins: List<QuickWin>
    ) : ScanEvent()

    data class Failed(val trigger: UserScan) : ScanEvent()
}

sealed class SdkScanState {
    data object Idle : SdkScanState()
    data object Scanning : SdkScanState()
    data object Error : SdkScanState()
}

/**
 * Runs [scan] for at least [minimumMs]. A failing scan throws here instead of failing the
 * caller's job (which would crash the app). A cancelled caller always gets its cancellation,
 * also when the scan fails on its way out: a newer scan is running by then.
 */
internal suspend fun <T> runScan(minimumMs: Long, scan: suspend () -> T): T = try {
    coroutineScope {
        val result = async { scan() }
        delay(minimumMs)
        result.await()
    }
} catch (e: Exception) {
    currentCoroutineContext().ensureActive()
    throw e
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

    /** A scan is running while the previous result stays on screen. */
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _scanEvents = Channel<ScanEvent>(Channel.BUFFERED)
    val scanEvents: Flow<ScanEvent> = _scanEvents.receiveAsFlow()

    // Survives a cancelled scan, so the next one still compares with what the user last saw
    private var pendingUserScan: PendingUserScan? = null

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
        if (rawScore == null) rawWins else filterTrusted(rawWins, rawScore, trusted)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _flaggedApps = MutableStateFlow<List<FlaggedApp>>(emptyList())
    val flaggedApps: StateFlow<List<FlaggedApp>> = _flaggedApps.asStateFlow()

    private val _selectedFilter = MutableStateFlow(HistoryFilter.MONTH)
    val selectedFilter: StateFlow<HistoryFilter> = _selectedFilter.asStateFlow()

    private val _historySnapshots = MutableStateFlow<List<PrivacySnapshot>>(emptyList())
    val historySnapshots: StateFlow<List<PrivacySnapshot>> = _historySnapshots.asStateFlow()

    /** Whether any history exists in any range; Clear history is disabled without it. */
    private val _hasHistory = MutableStateFlow(false)
    val hasHistory: StateFlow<Boolean> = _hasHistory.asStateFlow()

    private val _sdkScanResults = MutableStateFlow<SdkScanResult?>(null)
    val sdkScanResults: StateFlow<SdkScanResult?> = _sdkScanResults.asStateFlow()

    private val _sdkScanState = MutableStateFlow<SdkScanState>(SdkScanState.Idle)
    val sdkScanState: StateFlow<SdkScanState> = _sdkScanState.asStateFlow()

    private var currentScanJob: Job? = null
    private var isInitialLoad = true

    // The trusted set the shown score uses
    private var shownTrusted: Set<String>? = null

    init {
        // Recompute score reactively whenever the whitelist changes
        viewModelScope.launch {
            trustedPackages.collect { trusted ->
                val rawScore = _rawPrivacyScore.value ?: return@collect
                // The running scan applies the change when it finishes
                if (_scanState.value is PrivacyScanState.Scanning || _isRefreshing.value) return@collect
                showAdjusted(rawScore, trusted)
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
                val findings = sdkScanner.scan()
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

    /**
     * Scans the device. The first scan shows [PrivacyScanState.Scanning]; later ones keep the
     * result on screen and only set [isRefreshing]. [trigger] is set when the user asked for the
     * scan; it reports a [ScanEvent] if there is an earlier result to compare with.
     */
    fun performScan(trigger: UserScan? = null) {
        currentScanJob?.cancel()
        val hasResult = _scanState.value is PrivacyScanState.Success
        pendingUserScan = ScanFeedback.merge(
            pendingUserScan, trigger, _rawPrivacyScore.value.takeIf { hasResult }
        )
        if (hasResult) _isRefreshing.value = true else _scanState.value = PrivacyScanState.Scanning

        currentScanJob = viewModelScope.launch {
            try {
                val minimumMs = if (isInitialLoad) 0L else MINIMUM_SCAN_DISPLAY_TIME_MS
                onScanSuccess(runScan(minimumMs) { privacyScanner.performCompleteScan() })
                isInitialLoad = false
                _isRefreshing.value = false
                PrivacyWidgetProvider.requestImmediateUpdate(getApplication())
                applyMissedTrustChange()
            } catch (e: CancellationException) {
                // A newer scan took over, or the ViewModel is gone
                throw e
            } catch (e: Exception) {
                isInitialLoad = false
                if (_scanState.value is PrivacyScanState.Success) {
                    _isRefreshing.value = false
                    pendingUserScan?.let { _scanEvents.trySend(ScanEvent.Failed(it.trigger)) }
                    pendingUserScan = null
                    applyMissedTrustChange()
                } else {
                    _scanState.value = PrivacyScanState.Error(
                        e.message ?: "An unknown error occurred during scanning"
                    )
                }
            }
        }
    }

    /** The trust collector skips changes made while a scan runs; applies one it skipped. */
    private suspend fun applyMissedTrustChange() {
        val trusted = trustedAppsRepository.trustedPackages.first()
        val raw = _rawPrivacyScore.value ?: return
        if (trusted != shownTrusted) showAdjusted(raw, trusted)
    }

    private fun showAdjusted(rawScore: PrivacyScore, trusted: Set<String>) {
        shownTrusted = trusted
        _scanState.value = PrivacyScanState.Success(TrustedAppsAdjuster.computeAdjustedScore(rawScore, trusted))
        PrivacyWidgetProvider.requestImmediateUpdate(getApplication())
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
        viewModelScope.launch { reloadHistory() }
    }

    fun loadHistory() {
        viewModelScope.launch { reloadHistory() }
    }

    fun clearHistory() {
        viewModelScope.launch {
            snapshotRepository.clearAll()
            _historySnapshots.value = emptyList()
            _hasHistory.value = false
        }
    }

    private suspend fun reloadHistory() {
        _historySnapshots.value = snapshotRepository.getSnapshots(_selectedFilter.value)
        _hasHistory.value = snapshotRepository.hasAnySnapshot()
    }

    /**
     * Records the scan, then publishes everything at once: no suspension after the first
     * assignment, so a cancelled scan never leaves half of its result on screen.
     */
    private suspend fun onScanSuccess(result: PrivacyScore) {
        val quickWins = QuickWinsDetector.detectQuickWins(result, getApplication())
        val flaggedApps = computeFlaggedApps(result)
        val trusted = trustedAppsRepository.trustedPackages.first()
        val adjustedScore = TrustedAppsAdjuster.computeAdjustedScore(result, trusted)
        val history = scoreHistoryRepository.recordScore(adjustedScore.score)
        snapshotRepository.recordScan(adjustedScore)
        reloadHistory()

        _rawPrivacyScore.value = result
        _rawQuickWins.value = quickWins
        _flaggedApps.value = flaggedApps
        _scoreHistory.value = history
        shownTrusted = trusted
        _scanState.value = PrivacyScanState.Success(adjustedScore)
        pendingUserScan?.let { pending ->
            _scanEvents.trySend(
                ScanEvent.Finished(
                    outcome = ScanFeedback.between(
                        before = TrustedAppsAdjuster.computeAdjustedScore(pending.before, trusted),
                        after = adjustedScore,
                        trigger = pending.trigger
                    ),
                    trigger = pending.trigger,
                    quickWins = filterTrusted(quickWins, result, trusted)
                )
            )
        }
        pendingUserScan = null
    }

    private fun filterTrusted(
        wins: List<QuickWin>,
        rawScore: PrivacyScore,
        trusted: Set<String>
    ): List<QuickWin> = wins.filter { quickWin ->
        val issue = rawScore.issues.find { it.check == quickWin.relatedCheck }
        issue == null || !issue.isFullyTrusted(trusted)
    }

    private fun computeFlaggedApps(rawScore: PrivacyScore): List<FlaggedApp> {
        val pm = getApplication<Application>().packageManager
        // Resolved once per scan — the companion-package lookup must not run per package
        val isMicroGInstalled = PackageManagerUtil.isMicroGInstalled(pm)
        return rawScore.issues
            .filter { !it.isSecure && it.flaggedPackages.isNotEmpty() }
            .flatMap { issue ->
                issue.flaggedPackages.map { packageName ->
                    FlaggedApp(
                        packageName = packageName,
                        appName = PackageManagerUtil.getAppName(pm, packageName),
                        associatedCheck = issue.check,
                        isBlacklisted = FlaggedApp.isBlacklisted(packageName, isMicroGInstalled),
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
