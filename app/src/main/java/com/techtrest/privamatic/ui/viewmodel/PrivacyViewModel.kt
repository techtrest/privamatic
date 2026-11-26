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
import kotlinx.coroutines.CoroutineScope
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
    ) : ScanEvent() {
        /** The open Quick Win page's Action with its new gain; null when it is gone. */
        fun updated(open: QuickWin): QuickWin? =
            quickWins.find { it.type == open.type && it.relatedCheck == open.relatedCheck }
    }

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

/** The primary constructor is for tests; the app uses the one that only takes the application. */
class PrivacyViewModel internal constructor(
    application: Application,
    private val scoreHistoryRepository: ScoreHistoryRepository,
    private val snapshotRepository: PrivacySnapshotRepository,
    private val trustedAppsRepository: TrustedAppsRepository,
    private val sdkScanner: SdkScanner,
    private val sdkScanRepository: SdkScanRepository,
    private val scan: suspend () -> PrivacyScore,
    private val requestWidgetUpdate: () -> Unit,
    testScope: CoroutineScope?
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application = application,
        scoreHistoryRepository = ScoreHistoryRepository(application),
        snapshotRepository = PrivacySnapshotRepository(application),
        trustedAppsRepository = TrustedAppsRepository(application),
        sdkScanner = SdkScanner(application),
        sdkScanRepository = SdkScanRepository(application),
        scan = PrivacyScanner(application)::performCompleteScan,
        requestWidgetUpdate = { PrivacyWidgetProvider.requestImmediateUpdate(application) },
        testScope = null
    )

    private val scope: CoroutineScope = testScope ?: viewModelScope

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
        .stateIn(scope, SharingStarted.Eagerly, emptySet())

    val isAppsBannerDismissed: StateFlow<Boolean> = trustedAppsRepository.isAppsBannerDismissed
        .stateIn(scope, SharingStarted.Eagerly, false)

    // Quick wins with trusted apps filtered out; reactive to both scan results and whitelist changes
    val filteredQuickWins: StateFlow<List<QuickWin>> = combine(
        _rawQuickWins,
        _rawPrivacyScore,
        trustedPackages
    ) { rawWins, rawScore, trusted ->
        if (rawScore == null) rawWins else filterTrusted(rawWins, rawScore, trusted)
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

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

    // The stored trusted set (null until first read), and the one the shown score uses
    private var latestTrusted: Set<String>? = null
    private var shownTrusted: Set<String>? = null

    init {
        // Recompute score reactively whenever the whitelist changes
        scope.launch {
            trustedAppsRepository.trustedPackages.collect { trusted ->
                latestTrusted = trusted
                applyTrustChange()
            }
        }
        performScan()
        loadSdkResults()
    }

    fun runSdkScan() {
        if (_sdkScanState.value is SdkScanState.Scanning) return
        scope.launch {
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
        scope.launch {
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

        currentScanJob = scope.launch {
            try {
                val minimumMs = if (isInitialLoad) 0L else MINIMUM_SCAN_DISPLAY_TIME_MS
                onScanSuccess(runScan(minimumMs) { scan() })
            } catch (e: Exception) {
                // A cancelled scan that fails on its way out: the state is the newer scan's now
                ensureActive()
                onScanFailed(e)
            }
        }
    }

    private fun onScanFailed(e: Exception) {
        isInitialLoad = false
        if (_scanState.value is PrivacyScanState.Success) {
            val pending = pendingUserScan
            pendingUserScan = null
            _isRefreshing.value = false
            applyTrustChange()
            pending?.let { _scanEvents.trySend(ScanEvent.Failed(it.trigger)) }
        } else {
            _scanState.value = PrivacyScanState.Error(
                e.message ?: "An unknown error occurred during scanning"
            )
        }
    }

    /** Shows a trust change. While a scan runs it does nothing: the end of the scan shows it. */
    private fun applyTrustChange() {
        val rawScore = _rawPrivacyScore.value ?: return
        val trusted = latestTrusted ?: return
        if (trusted == shownTrusted || _isRefreshing.value) return
        shownTrusted = trusted
        _scanState.value = PrivacyScanState.Success(TrustedAppsAdjuster.computeAdjustedScore(rawScore, trusted))
        requestWidgetUpdate()
    }

    fun trustApp(packageName: String) {
        scope.launch { trustedAppsRepository.trustApp(packageName) }
    }

    fun untrustApp(packageName: String) {
        scope.launch { trustedAppsRepository.untrustApp(packageName) }
    }

    fun dismissAppsBanner() {
        scope.launch { trustedAppsRepository.dismissAppsBanner() }
    }

    fun setHistoryFilter(filter: HistoryFilter) {
        _selectedFilter.value = filter
        scope.launch { reloadHistory() }
    }

    fun loadHistory() {
        scope.launch { reloadHistory() }
    }

    fun clearHistory() {
        scope.launch {
            snapshotRepository.clearAll()
            _historySnapshots.value = emptyList()
            _hasHistory.value = false
        }
    }

    private class History(val snapshots: List<PrivacySnapshot>, val hasAny: Boolean)

    private suspend fun readHistory(): History {
        val hasAny = snapshotRepository.hasAnySnapshot()
        return History(snapshotRepository.getSnapshots(_selectedFilter.value), hasAny)
    }

    private fun publishHistory(history: History) {
        _historySnapshots.value = history.snapshots
        _hasHistory.value = history.hasAny
    }

    private suspend fun reloadHistory() = publishHistory(readHistory())

    /**
     * Records the scan, then publishes everything at once: no suspension after the first
     * assignment, so a cancelled scan never leaves half of its result on screen.
     */
    private suspend fun onScanSuccess(result: PrivacyScore) {
        val quickWins = QuickWinsDetector.detectQuickWins(result, getApplication())
        val flaggedApps = computeFlaggedApps(result)
        val recordedTrust = trustedAppsRepository.trustedPackages.first()
        val recordedScore = TrustedAppsAdjuster.computeAdjustedScore(result, recordedTrust)
        val scoreHistory = scoreHistoryRepository.recordScore(recordedScore.score)
        snapshotRepository.recordScan(recordedScore)
        val history = readHistory()
        currentCoroutineContext().ensureActive()

        // An app trusted while the result was saved already counts in the score and the snackbar;
        // today's history point gets it with the next scan, like any trust change.
        val trusted = latestTrusted ?: recordedTrust
        val adjustedScore = TrustedAppsAdjuster.computeAdjustedScore(result, trusted)
        val pending = pendingUserScan
        pendingUserScan = null
        publishHistory(history)
        _rawPrivacyScore.value = result
        _rawQuickWins.value = quickWins
        _flaggedApps.value = flaggedApps
        _scoreHistory.value = scoreHistory
        shownTrusted = trusted
        _scanState.value = PrivacyScanState.Success(adjustedScore)
        _isRefreshing.value = false
        isInitialLoad = false
        requestWidgetUpdate()
        pending?.let {
            _scanEvents.trySend(
                ScanEvent.Finished(
                    outcome = ScanFeedback.between(
                        before = TrustedAppsAdjuster.computeAdjustedScore(it.before, trusted),
                        after = adjustedScore,
                        trigger = it.trigger
                    ),
                    trigger = it.trigger,
                    quickWins = filterTrusted(quickWins, result, trusted)
                )
            )
        }
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
