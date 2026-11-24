package com.techtrest.privamatic.ui.viewmodel

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RunScanTest {

    private var uncaught: Throwable? = null

    /** Like viewModelScope: a supervisor, so a failed job ends in the uncaught handler (a crash). */
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, e -> uncaught = e }
    )

    @Test
    fun `a failing scan can be caught and never reaches the crash handler`() = runBlocking {
        var caught: Exception? = null
        scope.launch {
            try {
                runScan(1_000) { error("scan failed") }
            } catch (e: Exception) {
                caught = e
            }
        }.join()

        assertEquals("scan failed", caught?.message)
        assertNull(uncaught)
    }

    @Test
    fun `a cancelled scan that fails on its way out is cancelled, not failed`() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var caught: Exception? = null
        val job = scope.launch {
            try {
                runScan(0) {
                    // A checker blocks without suspending, so it only fails after the cancel
                    withContext(NonCancellable) {
                        started.complete(Unit)
                        release.await()
                    }
                    error("scan failed")
                }
            } catch (e: Exception) {
                caught = e
            }
        }
        started.await()
        job.cancel()
        release.complete(Unit)
        job.join()

        assertTrue("$caught", caught is CancellationException)
    }

    @Test
    fun `a scan's result comes back`() = runBlocking {
        assertEquals(42, runScan(0) { 42 })
    }
}
