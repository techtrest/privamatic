package com.techtrest.privacywidget.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.techtrest.privacywidget.data.model.PrivacyScore
import com.techtrest.privacywidget.data.scanner.PrivacyScanner
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

    private val _scanState = MutableStateFlow<PrivacyScanState>(PrivacyScanState.Idle)
    val scanState: StateFlow<PrivacyScanState> = _scanState.asStateFlow()

    init {
        // Automatically perform scan on initialization
        performScan()
    }

    fun performScan() {
        viewModelScope.launch {
            _scanState.value = PrivacyScanState.Scanning
            try {
                // Run scan and minimum display time in parallel
                val scanDeferred = async { privacyScanner.performCompleteScan() }
                val delayDeferred = async { delay(MINIMUM_SCAN_DISPLAY_TIME_MS) }

                // Wait for both to complete (whichever takes longer)
                awaitAll(scanDeferred, delayDeferred)

                // Get the scan result
                val result = scanDeferred.await()
                _scanState.value = PrivacyScanState.Success(result)
            } catch (e: Exception) {
                _scanState.value = PrivacyScanState.Error(
                    e.message ?: "An unknown error occurred during scanning"
                )
            }
        }
    }

    companion object {
        private const val MINIMUM_SCAN_DISPLAY_TIME_MS = 800L
    }
}
