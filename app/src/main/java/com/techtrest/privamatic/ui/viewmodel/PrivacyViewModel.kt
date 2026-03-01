package com.techtrest.privamatic.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.techtrest.privamatic.PrivacyWidgetProvider
import com.techtrest.privamatic.data.ScoreHistoryRepository
import com.techtrest.privamatic.data.model.PrivacyScore
import com.techtrest.privamatic.data.model.ScoreHistory
import com.techtrest.privamatic.data.scanner.PrivacyScanner
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _scanState = MutableStateFlow<PrivacyScanState>(PrivacyScanState.Idle)
    val scanState: StateFlow<PrivacyScanState> = _scanState.asStateFlow()

    private val _scoreHistory = MutableStateFlow<ScoreHistory?>(null)
    val scoreHistory: StateFlow<ScoreHistory?> = _scoreHistory.asStateFlow()

    private var currentScanJob: Job? = null
    private var isInitialLoad = true

    init {
        // Automatically perform scan on initialization
        performScan()
    }

    fun performScan() {
        currentScanJob?.cancel()
        currentScanJob = viewModelScope.launch {
            _scanState.value = PrivacyScanState.Scanning
            try {
                // Run scan
                val scanDeferred = async { privacyScanner.performCompleteScan() }

                // Skip animation delay on initial load, show it for manual refreshes
                if (isInitialLoad) {
                    // Initial load: show results immediately
                    val result = scanDeferred.await()
                    _scoreHistory.value = scoreHistoryRepository.recordScore(result.score)
                    _scanState.value = PrivacyScanState.Success(result)
                    isInitialLoad = false
                } else {
                    // Manual refresh: show 800ms animation for user feedback
                    val delayDeferred = async { delay(MINIMUM_SCAN_DISPLAY_TIME_MS) }

                    // Wait for both to complete (whichever takes longer)
                    awaitAll(scanDeferred, delayDeferred)

                    // Get the scan result
                    val result = scanDeferred.await()
                    _scoreHistory.value = scoreHistoryRepository.recordScore(result.score)
                    _scanState.value = PrivacyScanState.Success(result)
                }
                // Keep the home screen widget in sync with the latest scan result.
                // NOTE: This triggers a second full scan inside PrivacyWidgetProvider.onUpdate.
                // Passing the computed score directly would require shared storage and a
                // conditional scan path in the widget, which introduces regression risk.
                // The redundant scan is intentionally left as-is; it runs on a SupervisorJob
                // in a separate process context and has no impact on the UI scan result.
                PrivacyWidgetProvider.requestImmediateUpdate(getApplication())
            } catch (e: Exception) {
                _scanState.value = PrivacyScanState.Error(
                    e.message ?: "An unknown error occurred during scanning"
                )
                isInitialLoad = false
            }
        }
    }

    companion object {
        private const val MINIMUM_SCAN_DISPLAY_TIME_MS = 800L
    }
}
