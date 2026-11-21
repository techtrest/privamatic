package com.techtrest.privamatic

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * The nightly safety-net alarm is armed for the next 22:00 local time, also across a DST change,
 * and app start, boot and update arm it only when none is pending.
 */
class SnapshotAlarmTest {

    private lateinit var savedZone: TimeZone

    @Before
    fun setZone() {
        savedZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"))
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

    @Test
    fun `before 22_00 it is armed for tonight`() {
        val now = at(2026, Calendar.NOVEMBER, 21, 14, 54)
        assertEquals(at(2026, Calendar.NOVEMBER, 21, 22), SnapshotReceiver.nextTriggerAt(now))
    }

    @Test
    fun `at or after 22_00 it is armed for tomorrow night`() {
        assertEquals(
            at(2026, Calendar.NOVEMBER, 22, 22),
            SnapshotReceiver.nextTriggerAt(at(2026, Calendar.NOVEMBER, 21, 22))
        )
        assertEquals(
            at(2026, Calendar.NOVEMBER, 22, 22),
            SnapshotReceiver.nextTriggerAt(at(2026, Calendar.NOVEMBER, 21, 22, 37))
        )
    }

    @Test
    fun `across the clocks going back it stays at 22_00 local`() {
        // Fired Sat 24 Oct 2026 22:10 CEST; the night of 24 to 25 Oct has 25 hours.
        val next = SnapshotReceiver.nextTriggerAt(at(2026, Calendar.OCTOBER, 24, 22, 10))
        assertEquals(at(2026, Calendar.OCTOBER, 25, 22), next)
        assertEquals(25L * 60 * 60 * 1000, next - at(2026, Calendar.OCTOBER, 24, 22))
    }

    @Test
    fun `across the clocks going forward it stays at 22_00 local`() {
        val next = SnapshotReceiver.nextTriggerAt(at(2026, Calendar.MARCH, 28, 22, 5))
        assertEquals(at(2026, Calendar.MARCH, 29, 22), next)
        assertEquals(23L * 60 * 60 * 1000, next - at(2026, Calendar.MARCH, 28, 22))
    }

    @Test
    fun `a pending alarm is left alone, a missing one is armed`() {
        assertFalse(SnapshotReceiver.needsArm(alarmPending = true))
        assertTrue(SnapshotReceiver.needsArm(alarmPending = false))
    }
}
