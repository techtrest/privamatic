package com.techtrest.privamatic.ui.viewmodel

import android.app.Application
import android.content.pm.PackageManager
import com.techtrest.privamatic.data.InMemorySharedPreferences
import com.techtrest.privamatic.data.PrivacySnapshotDao
import com.techtrest.privamatic.data.PrivacySnapshotRepository
import com.techtrest.privamatic.data.ScanOutcome
import com.techtrest.privamatic.data.ScoreHistoryRepository
import com.techtrest.privamatic.data.SdkScanRepository
import com.techtrest.privamatic.data.TrustedAppsRepository
import com.techtrest.privamatic.data.UserScan
import com.techtrest.privamatic.data.model.PrivacyCheck
import com.techtrest.privamatic.data.model.PrivacyIssue
import com.techtrest.privamatic.data.model.PrivacyScore
import com.techtrest.privamatic.data.model.PrivacySnapshot
import com.techtrest.privamatic.data.scanner.PrivacyScoreCalculator
import com.techtrest.privamatic.data.scanner.SdkScanner
import java.io.IOException
import kotlin.coroutines.ContinuationInterceptor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * The real view model on a single "main" thread (the runBlocking loop), with a scan, a history
 * table and a trusted list that can be held up or made to fail at the points under test.
 */
class PrivacyViewModelScanTest {

    private val wifi = PrivacyCheck.WIFI_SCANNING
    private val listeners = PrivacyCheck.NOTIFICATION_LISTENER

    private fun fail(check: PrivacyCheck, points: Int, vararg packages: String) = PrivacyIssue(
        check, isSecure = false, currentStatus = "", customPointDeduction = points,
        flaggedPackages = packages.toList()
    )

    private fun pass(check: PrivacyCheck) = PrivacyIssue(check, isSecure = true, currentStatus = "")

    private fun score(vararg issues: PrivacyIssue): PrivacyScore =
        PrivacyScoreCalculator.calculateScore(issues.toList(), 15)

    private val failing = score(fail(wifi, 2))
    private val fixed = score(pass(wifi))

    /**
     * Holds a call until [release], then throws [thenThrow]. Unless [isCancellable], the call
     * ignores that its scan was cancelled meanwhile, like one that blocks without suspending.
     */
    private class Gate(
        private val thenThrow: Exception? = null,
        private val isCancellable: Boolean = false
    ) {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        suspend fun pass() {
            entered.complete(Unit)
            if (isCancellable) release.await() else withContext(NonCancellable) { release.await() }
            thenThrow?.let { throw it }
        }
    }

    private class FakeDao : PrivacySnapshotDao {
        val rows = mutableListOf<PrivacySnapshot>()
        var gateOnHasAny: Gate? = null
        override suspend fun insert(snapshot: PrivacySnapshot) {
            rows += snapshot.copy(id = rows.size + 1L)
        }
        override suspend fun getSnapshotsSince(since: Long) = rows.toList()
        override suspend fun getAllSnapshots() = rows.toList()
        override suspend fun update(snapshot: PrivacySnapshot) {
            rows[rows.indexOfFirst { it.id == snapshot.id }] = snapshot
        }
        override suspend fun latestSnapshotForDay(dayStart: Long, dayEnd: Long) =
            rows.lastOrNull { it.timestamp in dayStart until dayEnd }
        override suspend fun deleteAll() = rows.clear()
        override suspend fun getEarliestTimestamp(): Long? {
            gateOnHasAny?.let { gateOnHasAny = null; it.pass() }
            return rows.minOfOrNull { it.timestamp }
        }
    }

    private class Env(val scope: CoroutineScope) {
        var uncaught: Throwable? = null
        val dao = FakeDao()
        val trusted = MutableStateFlow(emptySet<String>())
        var trustReadGate: Gate? = null
        var trustReadError: Exception? = null
        var widgetUpdates = 0
        val scans = ArrayDeque<suspend () -> PrivacyScore>()
        lateinit var vm: PrivacyViewModel

        fun start(): PrivacyViewModel {
            val packageManager = mock(PackageManager::class.java)
            val application = mock(Application::class.java)
            `when`(application.packageManager).thenReturn(packageManager)
            val trustRepository = mock(TrustedAppsRepository::class.java)
            `when`(trustRepository.trustedPackages).thenReturn(
                flow {
                    trustReadGate?.let { trustReadGate = null; it.pass() }
                    trustReadError?.let { throw it }
                    emitAll(trusted)
                }
            )
            `when`(trustRepository.isAppsBannerDismissed).thenReturn(MutableStateFlow(false))
            vm = PrivacyViewModel(
                application = application,
                scoreHistoryRepository = ScoreHistoryRepository(InMemorySharedPreferences()) { 0L },
                snapshotRepository = PrivacySnapshotRepository(dao, System::currentTimeMillis),
                trustedAppsRepository = trustRepository,
                sdkScanner = mock(SdkScanner::class.java),
                sdkScanRepository = mock(SdkScanRepository::class.java),
                scan = { scans.removeFirst().invoke() },
                requestWidgetUpdate = { widgetUpdates++ },
                testScope = scope
            )
            return vm
        }

        suspend fun awaitIdle() = withTimeout(5_000) {
            vm.scanState.first { it is PrivacyScanState.Success }
            vm.isRefreshing.first { !it }
        }

        suspend fun nextEvent(): ScanEvent = withTimeout(5_000) { vm.scanEvents.first() }

        suspend fun noEvent(): Boolean = withTimeoutOrNull(50) { vm.scanEvents.first() } == null

        val shownScore get() = (vm.scanState.value as PrivacyScanState.Success).privacyScore.score
    }

    /** Runs [test] after a first scan with [first] on screen; a crash fails the test. */
    private fun scanTest(first: PrivacyScore = failing, test: suspend Env.() -> Unit) = runBlocking {
        lateinit var env: Env
        val scope = CoroutineScope(
            SupervisorJob() + coroutineContext[ContinuationInterceptor]!! +
                CoroutineExceptionHandler { _, e -> env.uncaught = e }
        )
        env = Env(scope)
        env.scans += { first }
        env.start()
        try {
            env.awaitIdle()
            delay(50)
            env.test()
            delay(50)
            assertNull("crashed", env.uncaught)
        } finally {
            scope.cancel()
        }
    }

    /** The second scan takes over while the first hangs in [gate], then the first fails. */
    private suspend fun Env.supersededScanFails(gate: Gate) {
        val second = Gate()
        scans += { fixed }
        scans += { second.pass(); fixed }
        vm.performScan(UserScan.CheckAgain(wifi))
        withTimeout(5_000) { gate.entered.await() }
        vm.performScan(UserScan.Rescan)
        val widgetUpdatesBefore = widgetUpdates
        gate.release.complete(Unit)
        withTimeout(5_000) { second.entered.await() }
        delay(50)

        assertTrue("the newer scan's progress must go on", vm.isRefreshing.value)
        assertTrue("no event from the old scan", noEvent())
        assertEquals("no widget refresh", widgetUpdatesBefore, widgetUpdates)

        second.release.complete(Unit)
        val event = nextEvent() as ScanEvent.Finished
        assertEquals(ScanOutcome.CheckFixed(wifi, 2, 100), event.outcome)
        assertEquals(UserScan.CheckAgain(wifi), event.trigger)
        assertFalse(vm.isRefreshing.value)
    }

    // H1: a scan that is no longer current leaves everything alone

    @Test
    fun `a superseded scan failing while it reads the history leaves the newer scan alone`() = scanTest {
        val gate = Gate(IllegalStateException("database"))
        dao.gateOnHasAny = gate
        supersededScanFails(gate)
    }

    @Test
    fun `a superseded scan failing while it reads the trusted list leaves the newer scan alone`() = scanTest {
        val gate = Gate(IOException("DataStore"))
        trustReadGate = gate
        supersededScanFails(gate)
    }

    @Test
    fun `a superseded scan failing after its result is shown leaves the newer scan alone`() = scanTest {
        val gate = Gate(IOException("DataStore"))
        val second = Gate()
        scans += { fixed }
        scans += { second.pass(); failing }
        // The second read of the trusted list in that scan, if there is one
        val hasAny = Gate()
        dao.gateOnHasAny = hasAny
        vm.performScan(UserScan.CheckAgain(wifi))
        withTimeout(5_000) { hasAny.entered.await() }
        trustReadGate = gate
        hasAny.release.complete(Unit)
        assertTrue(nextEvent() is ScanEvent.Finished)
        delay(50)
        trustReadGate = null

        vm.performScan(UserScan.Rescan)
        val widgetUpdatesBefore = widgetUpdates
        gate.release.complete(Unit)
        withTimeout(5_000) { second.entered.await() }
        delay(50)
        assertTrue("the newer scan's progress must go on", vm.isRefreshing.value)
        assertTrue("no event from the old scan", noEvent())
        assertEquals(widgetUpdatesBefore, widgetUpdates)

        second.release.complete(Unit)
        assertEquals(ScanOutcome.Loss(wifi, 2, 98), (nextEvent() as ScanEvent.Finished).outcome)
    }

    @Test
    fun `a trusted list that can't be read after a failed scan doesn't crash`() = scanTest {
        scans += {
            trustReadError = IOException("DataStore")
            error("scan failed")
        }
        vm.performScan(UserScan.Rescan)
        assertEquals(ScanEvent.Failed(UserScan.Rescan), nextEvent())
        assertFalse(vm.isRefreshing.value)
        assertEquals(98, shownScore)
    }

    @Test
    fun `a trusted list that can't be read after a finished scan doesn't crash or report a failure`() = scanTest {
        val hasAny = Gate()
        dao.gateOnHasAny = hasAny
        scans += { fixed }
        vm.performScan(UserScan.Rescan)
        withTimeout(5_000) { hasAny.entered.await() }
        trustReadError = IOException("DataStore")
        hasAny.release.complete(Unit)
        assertEquals(ScanOutcome.CheckFixed(wifi, 2, 100), (nextEvent() as ScanEvent.Finished).outcome)
        assertTrue(noEvent())
        assertEquals(100, shownScore)
    }

    @Test
    fun `a failed scan keeps the result and reports it once`() = scanTest {
        scans += { error("scan failed") }
        val widgetUpdatesBefore = widgetUpdates
        vm.performScan(UserScan.CheckAgain(wifi))
        assertTrue(vm.isRefreshing.value)
        assertEquals(ScanEvent.Failed(UserScan.CheckAgain(wifi)), nextEvent())
        assertFalse(vm.isRefreshing.value)
        assertTrue(noEvent())
        assertEquals(98, shownScore)
        assertEquals(widgetUpdatesBefore, widgetUpdates)
    }

    @Test
    fun `a scan failing with a cancellation of something else still ends`() = scanTest {
        scans += { throw kotlinx.coroutines.CancellationException("the database was closed") }
        vm.performScan(UserScan.Rescan)
        assertEquals(ScanEvent.Failed(UserScan.Rescan), nextEvent())
        assertFalse(vm.isRefreshing.value)
    }

    // M3: the result waits while nobody collects

    @Test
    fun `a result nobody collected yet is kept`() = scanTest {
        scans += { fixed }
        vm.performScan(UserScan.Rescan)
        awaitIdle()
        delay(100)
        assertEquals(ScanOutcome.CheckFixed(wifi, 2, 100), (nextEvent() as ScanEvent.Finished).outcome)
    }

    // M4: a cancelled scan leaves nothing half-published

    /** A rescan is superseded while [gate] holds its history read; the newer scan fails. */
    private suspend fun Env.supersededScanPublishesNothing(gate: Gate) {
        val shownBefore = vm.historySnapshots.value
        assertEquals(98, shownBefore.single().score)
        dao.gateOnHasAny = gate
        scans += { fixed }
        scans += { error("scan failed") }
        vm.performScan(UserScan.Rescan)
        withTimeout(5_000) { gate.entered.await() }
        vm.performScan(UserScan.Rescan)
        gate.release.complete(Unit)
        assertEquals(ScanEvent.Failed(UserScan.Rescan), nextEvent())
        assertEquals(98, shownScore)
        assertEquals(shownBefore, vm.historySnapshots.value)
    }

    @Test
    fun `a scan cancelled while it reads the history publishes none of it`() = scanTest {
        supersededScanPublishesNothing(Gate(isCancellable = true))
    }

    @Test
    fun `a superseded scan that notices late never publishes its result`() = scanTest {
        supersededScanPublishesNothing(Gate())
    }

    // L5: a trust change while the result is being saved

    @Test
    fun `an app trusted while the result is saved counts in snackbar and score, one widget refresh`() {
        val oneApp = score(fail(listeners, 3, "com.example.app"))
        scanTest(first = oneApp) {
            val gate = Gate()
            dao.gateOnHasAny = gate
            scans += { oneApp }
            vm.performScan(UserScan.Rescan)
            withTimeout(5_000) { gate.entered.await() }
            trusted.value = setOf("com.example.app")
            delay(50)
            val widgetUpdatesBefore = widgetUpdates
            gate.release.complete(Unit)
            val event = nextEvent() as ScanEvent.Finished
            delay(50)
            assertEquals(shownScore, event.outcome.score)
            assertEquals(ScanOutcome.NoChange(100), event.outcome)
            assertEquals(widgetUpdatesBefore + 1, widgetUpdates)
        }
    }

    @Test
    fun `an app trusted while a scan runs that then fails shows in the score`() {
        val oneApp = score(fail(listeners, 3, "com.example.app"))
        scanTest(first = oneApp) {
            val gate = Gate(IllegalStateException("scan failed"))
            scans += { gate.pass(); oneApp }
            vm.performScan(UserScan.Rescan)
            withTimeout(5_000) { gate.entered.await() }
            trusted.value = setOf("com.example.app")
            delay(50)
            assertEquals(97, shownScore)
            gate.release.complete(Unit)
            assertTrue(nextEvent() is ScanEvent.Failed)
            delay(50)
            assertEquals(100, shownScore)
        }
    }

    @Test
    fun `an app trusted outside a scan shows at once`() {
        val oneApp = score(fail(listeners, 3, "com.example.app"))
        scanTest(first = oneApp) {
            val widgetUpdatesBefore = widgetUpdates
            trusted.value = setOf("com.example.app")
            delay(50)
            assertEquals(100, shownScore)
            assertEquals(widgetUpdatesBefore + 1, widgetUpdates)
        }
    }

    // The 800 ms minimum

    @Test
    fun `a rescan lasts at least 800 ms`() = scanTest {
        scans += { fixed }
        val start = System.nanoTime()
        vm.performScan(UserScan.Rescan)
        nextEvent()
        val ms = (System.nanoTime() - start) / 1_000_000
        assertTrue("$ms ms", ms >= 800)
    }
}
