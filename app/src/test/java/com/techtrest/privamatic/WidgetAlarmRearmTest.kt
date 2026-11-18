package com.techtrest.privamatic

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * App start re-arms the 6-hour widget alarm only when it is missing (review round 1, L1):
 * re-arming a live alarm would move its next fire 6 hours out from every app start.
 */
class WidgetAlarmRearmTest {

    @Test
    fun widgetPlaced_alarmMissing_rearms() {
        assertTrue(PrivacyWidgetProvider.needsRearm(placedWidgets = 1, alarmPending = false))
        assertTrue(PrivacyWidgetProvider.needsRearm(placedWidgets = 3, alarmPending = false))
    }

    @Test
    fun widgetPlaced_alarmPending_leavesItAlone() {
        assertFalse(PrivacyWidgetProvider.needsRearm(placedWidgets = 1, alarmPending = true))
    }

    @Test
    fun noWidget_neverArms() {
        assertFalse(PrivacyWidgetProvider.needsRearm(placedWidgets = 0, alarmPending = false))
        assertFalse(PrivacyWidgetProvider.needsRearm(placedWidgets = 0, alarmPending = true))
    }
}
