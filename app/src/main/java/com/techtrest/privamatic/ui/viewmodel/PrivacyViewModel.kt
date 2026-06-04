package com.techtrest.privamatic.ui.viewmodel

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.techtrest.privamatic.PrivacyWidgetProvider
import com.techtrest.privamatic.data.ScoreHistoryRepository
import com.techtrest.privamatic.data.TrustedAppsRepository
import com.techtrest.privamatic.data.model.FlaggedApp
import com.techtrest.privamatic.data.model.PrivacyIssue
import com.techtrest.privamatic.data.model.PrivacyScore
import com.techtrest.privamatic.data.model.ScoreHistory
import com.techtrest.privamatic.data.scanner.PrivacyScanner
import com.techtrest.privamatic.data.scanner.PrivacyScoreCalculator
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class PrivacyScanState {
    data object Idle : PrivacyScanState()
    data object Scanning : PrivacyScanState()
    data class Success(val privacyScore: PrivacyScore) : PrivacyScanState()
    data class Error(val message: String) : PrivacyScanState()
}

class PrivacyViewModel(application: Application) : AndroidViewModel(application) {

    private val privacyScanner = PrivacyScanner(application)
    private val scoreHistoryRepository = ScoreHistoryRepository(application)
    private val trustedAppsRepository = TrustedAppsRepository(application)

    private val _scanState = MutableStateFlow<PrivacyScanState>(PrivacyScanState.Idle)
    val scanState: StateFlow<PrivacyScanState> = _scanState.asStateFlow()

    private val _scoreHistory = MutableStateFlow<ScoreHistory?>(null)
    val scoreHistory: StateFlow<ScoreHistory?> = _scoreHistory.asStateFlow()

    // Raw scan result, stored so trust changes can recalculate without re-scanning
    private val _rawPrivacyScore = MutableStateFlow<PrivacyScore?>(null)

    private val _flaggedApps = MutableStateFlow<List<FlaggedApp>>(emptyList())
    val flaggedApps: StateFlow<List<FlaggedApp>> = _flaggedApps.asStateFlow()

    val trustedPackages: StateFlow<Set<String>> = trustedAppsRepository.trustedPackages
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val isAppsBannerDismissed: StateFlow<Boolean> = trustedAppsRepository.isAppsBannerDismissed
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private var currentScanJob: Job? = null
    private var isInitialLoad = true

    init {
        // Recompute score reactively whenever the whitelist changes
        viewModelScope.launch {
            trustedPackages.collect { trusted ->
                val rawScore = _rawPrivacyScore.value ?: return@collect
                if (_scanState.value is PrivacyScanState.Scanning) return@collect
                _scanState.value = PrivacyScanState.Success(computeAdjustedScore(rawScore, trusted))
            }
        }
        performScan()
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

    private suspend fun onScanSuccess(result: PrivacyScore) {
        _rawPrivacyScore.value = result
        _flaggedApps.value = computeFlaggedApps(result)
        val trusted = trustedAppsRepository.trustedPackages.first()
        val adjustedScore = computeAdjustedScore(result, trusted)
        _scoreHistory.value = scoreHistoryRepository.recordScore(adjustedScore.score)
        _scanState.value = PrivacyScanState.Success(adjustedScore)
    }

    private fun computeAdjustedScore(rawScore: PrivacyScore, trusted: Set<String>): PrivacyScore {
        if (trusted.isEmpty()) return rawScore
        val adjustedIssues = rawScore.issues.map { adjustIssueForTrust(it, trusted) }
        return PrivacyScoreCalculator.calculateScore(adjustedIssues, rawScore.manualCheckPoints)
    }

    private fun adjustIssueForTrust(issue: PrivacyIssue, trusted: Set<String>): PrivacyIssue {
        if (issue.isSecure || issue.flaggedPackages.isEmpty()) return issue
        val untrustedCount = issue.flaggedPackages.count { it !in trusted }
        if (untrustedCount == issue.flaggedPackages.size) return issue
        return issue.copy(customPointDeduction = untrustedCount * issue.check.pointDeduction)
    }

    private fun computeFlaggedApps(rawScore: PrivacyScore): List<FlaggedApp> {
        val pm = getApplication<Application>().packageManager
        return rawScore.issues
            .filter { !it.isSecure && it.flaggedPackages.isNotEmpty() }
            .flatMap { issue ->
                issue.flaggedPackages.map { packageName ->
                    val appInfo = try { pm.getApplicationInfo(packageName, 0) } catch (_: Exception) { null }
                    val isSystem = appInfo?.let { (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0 } ?: false
                    val appName = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: packageName
                    FlaggedApp(
                        packageName = packageName,
                        appName = appName,
                        associatedCheck = issue.check,
                        isBlacklisted = FlaggedApp.isBlacklisted(packageName),
                        isSystemApp = isSystem
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
