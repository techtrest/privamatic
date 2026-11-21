package com.techtrest.privamatic.data

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * The widget shows a score change while `now - deltaFirstAppearedTimestamp <= 48 h`
 * (PrivacyWidgetProvider). These tests pin when that timestamp starts.
 *
 * Times are local times in Europe/Berlin, the zone the phones use, in November 2026.
 */
class ScoreHistoryRepositoryTest {

    private val cap = 48L * 60 * 60 * 1000
    private lateinit var savedZone: TimeZone
    private var clock = 0L

    @Before
    fun setZone() {
        savedZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"))
    }

    @After
    fun restoreZone() {
        TimeZone.setDefault(savedZone)
    }

    private fun at(day: Int, hour: Int, minute: Int = 0): Long = Calendar.getInstance().apply {
        clear()
        set(2026, Calendar.NOVEMBER, day, hour, minute)
    }.timeInMillis

    private fun repo(storage: InMemorySharedPreferences) = ScoreHistoryRepository(storage) { clock }

    @Test
    fun `a fresh change after a few days away starts a new 48 h window`() {
        val storage = InMemorySharedPreferences()
        clock = at(16, 10); repo(storage).recordScore(80)           // Mon 10:00, first scan
        clock = at(16, 11); repo(storage).recordScore(70)           // Mon 11:00, a setting turned on: ↓10

        // Thu 09:00, first scan since Monday: the score dropped again, 70 → 60.
        clock = at(19, 9)
        val thursday = repo(storage).recordScore(60)

        assertEquals(70, thursday.dailyBaselineScore)
        assertEquals(-10, thursday.scoreDelta)
        assertEquals(at(19, 9), thursday.deltaFirstAppearedTimestamp)
        assertTrue("the widget hides a change seen 0 h ago", clock - thursday.deltaFirstAppearedTimestamp <= cap)
    }

    @Test
    fun `a change again each morning is still shown in the evening`() {
        val storage = InMemorySharedPreferences()
        clock = at(16, 8); repo(storage).recordScore(80)            // Mon 08:00
        clock = at(16, 22); repo(storage).recordScore(70)           // Mon 22:00: ↓10
        clock = at(17, 7, 30); repo(storage).recordScore(60)        // Tue 07:30: ↓10 again, across midnight
        clock = at(18, 7); repo(storage).recordScore(55)            // Wed 07:00: ↓5, across midnight

        // Wed 23:00, the 6 h widget update, nothing changed since 07:00.
        clock = at(18, 23)
        val wednesdayNight = repo(storage).recordScore(55)

        assertEquals(60, wednesdayNight.dailyBaselineScore)
        assertEquals(-5, wednesdayNight.scoreDelta)
        assertEquals(at(18, 7), wednesdayNight.deltaFirstAppearedTimestamp)
        assertTrue("the widget hides a change seen 16 h ago", clock - wednesdayNight.deltaFirstAppearedTimestamp <= cap)
    }

    @Test
    fun `later scans on the same day keep the time the change first appeared`() {
        val storage = InMemorySharedPreferences()
        clock = at(16, 8); repo(storage).recordScore(80)
        clock = at(16, 9); repo(storage).recordScore(72)            // ↓8 first seen at 09:00
        clock = at(16, 15); val afternoon = repo(storage).recordScore(70)  // ↓10 against the same baseline

        assertEquals(80, afternoon.dailyBaselineScore)
        assertEquals(-10, afternoon.scoreDelta)
        assertEquals(at(16, 9), afternoon.deltaFirstAppearedTimestamp)
    }

    @Test
    fun `no change since yesterday clears the change after midnight`() {
        val storage = InMemorySharedPreferences()
        clock = at(16, 8); repo(storage).recordScore(80)
        clock = at(16, 9); repo(storage).recordScore(70)
        clock = at(17, 3); val tuesday = repo(storage).recordScore(70)    // 6 h widget update after midnight

        assertEquals(70, tuesday.dailyBaselineScore)
        assertEquals(0, tuesday.scoreDelta)
        assertEquals(0L, tuesday.deltaFirstAppearedTimestamp)
    }

    @Test
    fun `first scan ever seeds the baseline and shows no change`() {
        val storage = InMemorySharedPreferences()
        clock = at(16, 8)
        val first = repo(storage).recordScore(80)

        assertEquals(80, first.dailyBaselineScore)
        assertEquals(0, first.scoreDelta)
        assertEquals(at(16, 0), first.dailyBaselineTimestamp)
        assertEquals(0L, first.deltaFirstAppearedTimestamp)
    }
}
