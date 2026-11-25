package com.techtrest.privamatic.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavigationStateTest {

    private val state = AppNavigationState()

    @Test
    fun `asking for the Breakdown opens Details, Checks and keeps the request until it is shown`() {
        state.selectDetailsTab(DetailsTab.APPS)
        state.openBreakdown()
        assertEquals(NavigationTab.DETAILS, state.selectedTab)
        assertEquals(DetailsTab.CHECKS, state.selectedDetailsTab)
        assertTrue(state.isBreakdownRequested)
        state.selectTab(NavigationTab.DETAILS)
        assertTrue("arriving on Details keeps it", state.isBreakdownRequested)
        state.onBreakdownShown()
        assertFalse(state.isBreakdownRequested)
    }

    @Test
    fun `going to another tab before the Breakdown shows drops the request`() {
        state.openBreakdown()
        state.selectTab(NavigationTab.DASHBOARD)
        assertFalse(state.isBreakdownRequested)
        state.openBreakdown()
        state.selectTab(NavigationTab.ACTIONS)
        assertFalse(state.isBreakdownRequested)
    }

    @Test
    fun `going to another Details tab before the Breakdown shows drops the request`() {
        state.openBreakdown()
        state.selectDetailsTab(DetailsTab.SDK)
        assertFalse(state.isBreakdownRequested)
    }
}
