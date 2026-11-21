package com.techtrest.privamatic.data

import com.techtrest.privamatic.data.model.CheckDeduction
import com.techtrest.privamatic.data.model.PrivacySnapshot
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * One history point per local calendar day, set by every finished scan: a later scan the same
 * day replaces the point, the first scan after local midnight starts a new one.
 *
 * Times are local times in Europe/Berlin, the zone the phones use.
 */
class PrivacySnapshotRepositoryTest {

    private lateinit var savedZone: TimeZone
    private var clock = 0L
    private lateinit var dao: FakeSnapshotDao
    private lateinit var repo: PrivacySnapshotRepository

    @Before
    fun setUp() {
        savedZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"))
        dao = FakeSnapshotDao()
        repo = PrivacySnapshotRepository(dao) { clock }
    }

    @After
    fun restoreZone() {
        TimeZone.setDefault(savedZone)
    }

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): Long =
        Calendar.getInstance().apply {
            clear()
            set(year, month, day, hour, minute)
        }.timeInMillis

    private fun nov(day: Int, hour: Int, minute: Int = 0) = at(2026, Calendar.NOVEMBER, day, hour, minute)

    private fun scanAt(time: Long, score: Int, deductions: List<CheckDeduction> = emptyList()) {
        clock = time
        runBlocking { repo.recordSnapshot(score, deductions) }
    }

    @Test
    fun `a later scan the same day replaces the earlier point`() {
        scanAt(nov(23, 8), 53, listOf(CheckDeduction("USB_DEBUGGING", 4)))
        scanAt(nov(23, 19, 30), 61)

        val rows = dao.rows()
        assertEquals("one point for the day", 1, rows.size)
        assertEquals(61, rows[0].score)
        assertEquals(nov(23, 19, 30), rows[0].timestamp)
        assertEquals("[]", rows[0].deductionsJson)
    }

    @Test
    fun `a scan that started earlier but saves last never replaces a newer point`() {
        // Widget scan stamped 19:30:05 saves first; a refresh stamped 19:30:02 saves after it.
        scanAt(nov(23, 19, 30) + 5_000, 61)
        scanAt(nov(23, 19, 30) + 2_000, 58)

        val rows = dao.rows()
        assertEquals(1, rows.size)
        assertEquals(61, rows[0].score)
        assertEquals(nov(23, 19, 30) + 5_000, rows[0].timestamp)
    }

    @Test
    fun `a day has a point only once a scan recorded one`() {
        clock = nov(23, 22, 15)
        assertEquals(false, runBlocking { repo.hasPointToday() })
        scanAt(nov(22, 21), 53)
        clock = nov(23, 22, 15)
        assertEquals("yesterday's point doesn't count", false, runBlocking { repo.hasPointToday() })
        scanAt(nov(23, 9), 55)
        clock = nov(23, 22, 15)
        assertEquals(true, runBlocking { repo.hasPointToday() })
    }

    @Test
    fun `the first scan after midnight starts a new day and leaves yesterday alone`() {
        scanAt(nov(23, 23, 59), 53)
        scanAt(nov(24, 0, 1), 57)

        val rows = dao.rows()
        assertEquals(2, rows.size)
        assertEquals(nov(23, 23, 59), rows[0].timestamp)
        assertEquals(53, rows[0].score)
        assertEquals(nov(24, 0, 1), rows[1].timestamp)
        assertEquals(57, rows[1].score)
    }

    @Test
    fun `a day with only the alarm's scan keeps that point`() {
        scanAt(nov(23, 12), 53)
        // 24 Nov: no app or widget scan, only the safety-net alarm at 22:20.
        scanAt(nov(24, 22, 20), 55)
        scanAt(nov(25, 9), 55)

        val rows = dao.rows()
        assertEquals(3, rows.size)
        assertEquals(nov(24, 22, 20), rows[1].timestamp)
        assertEquals(55, rows[1].score)
    }

    @Test
    fun `old points stay as they are`() {
        dao.seed(PrivacySnapshot(timestamp = nov(20, 10, 3), score = 53, deductionsJson = "[]"))
        dao.seed(PrivacySnapshot(timestamp = nov(21, 3, 52), score = 53, deductionsJson = "[]"))
        val before = dao.rows()

        scanAt(nov(22, 9), 60)
        scanAt(nov(22, 21), 62)

        val rows = dao.rows()
        assertEquals(before, rows.take(2))
        assertEquals(3, rows.size)
        assertEquals(62, rows[2].score)
    }

    @Test
    fun `clocks go back - the 25 hour day is still one day`() {
        // Sun 25 Oct 2026, 03:00 CEST -> 02:00 CET: the local day has 25 hours.
        scanAt(at(2026, Calendar.OCTOBER, 25, 10), 50)
        scanAt(at(2026, Calendar.OCTOBER, 25, 23, 15), 52)
        scanAt(at(2026, Calendar.OCTOBER, 25, 23, 45), 54)
        scanAt(at(2026, Calendar.OCTOBER, 26, 0, 10), 56)

        val rows = dao.rows()
        assertEquals("one point on 25 Oct, one on 26 Oct", 2, rows.size)
        assertEquals(at(2026, Calendar.OCTOBER, 25, 23, 45), rows[0].timestamp)
        assertEquals(54, rows[0].score)
        assertEquals(56, rows[1].score)
    }

    @Test
    fun `clocks go forward - the 23 hour day ends at local midnight`() {
        // Sun 29 Mar 2026, 02:00 CET -> 03:00 CEST: the local day has 23 hours.
        scanAt(at(2026, Calendar.MARCH, 29, 10), 50)
        scanAt(at(2026, Calendar.MARCH, 29, 23, 30), 52)
        scanAt(at(2026, Calendar.MARCH, 30, 0, 30), 54)

        val rows = dao.rows()
        assertEquals(2, rows.size)
        assertEquals(at(2026, Calendar.MARCH, 29, 23, 30), rows[0].timestamp)
        assertEquals(52, rows[0].score)
        assertEquals(at(2026, Calendar.MARCH, 30, 0, 30), rows[1].timestamp)
    }
}

/** In-memory stand-in for the Room DAO. [setPointForDay] is the real default method. */
private class FakeSnapshotDao : PrivacySnapshotDao {
    private val table = mutableListOf<PrivacySnapshot>()
    private var nextId = 1L

    fun rows(): List<PrivacySnapshot> = table.sortedBy { it.timestamp }

    fun seed(snapshot: PrivacySnapshot) {
        table += snapshot.copy(id = nextId++)
    }

    override suspend fun insert(snapshot: PrivacySnapshot) = seed(snapshot)

    override suspend fun getSnapshotsSince(since: Long) = rows().filter { it.timestamp >= since }

    override suspend fun getAllSnapshots() = rows()

    override suspend fun update(snapshot: PrivacySnapshot) {
        val index = table.indexOfFirst { it.id == snapshot.id }
        check(index >= 0) { "no row ${snapshot.id}" }
        table[index] = snapshot
    }

    override suspend fun latestSnapshotForDay(dayStart: Long, dayEnd: Long) =
        table.filter { it.timestamp >= dayStart && it.timestamp < dayEnd }
            .maxWithOrNull(compareBy<PrivacySnapshot>({ it.timestamp }, { it.id }))

    override suspend fun deleteAll() = table.clear()

    override suspend fun getEarliestTimestamp() = table.minOfOrNull { it.timestamp }
}
