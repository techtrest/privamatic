package com.techtrest.privamatic.ui.viewmodel

import com.techtrest.privamatic.data.ScanOutcome
import com.techtrest.privamatic.data.UserScan
import com.techtrest.privamatic.data.model.ManualCheckType
import com.techtrest.privamatic.data.model.PrivacyCheck
import com.techtrest.privamatic.data.model.PrivacyIssue
import com.techtrest.privamatic.data.model.QuickWin
import com.techtrest.privamatic.data.model.QuickWinType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The open Quick Win page after a finished scan, whatever started that scan. */
class ScanEventTest {

    private val wifi = QuickWin(QuickWinType.DISABLE_WIFI_SCANNING, PrivacyCheck.WIFI_SCANNING)
    private val developer = QuickWin(
        QuickWinType.DISABLE_DEVELOPER_OPTIONS, PrivacyCheck.DEVELOPER_OPTIONS,
        alsoFixes = listOf(PrivacyIssue(PrivacyCheck.USB_DEBUGGING, isSecure = false, currentStatus = ""))
    )

    private val triggers = listOf(
        UserScan.FixReturn(PrivacyCheck.WIFI_SCANNING),
        UserScan.FixReturn(null),
        UserScan.CheckAgain(PrivacyCheck.WIFI_SCANNING),
        UserScan.Rescan,
        UserScan.ManualDone(ManualCheckType.LOCATION_ALWAYS_ON),
        UserScan.AdIdConfirmed,
        UserScan.AdIdReset
    )

    private fun finished(trigger: UserScan, vararg wins: QuickWin) =
        ScanEvent.Finished(ScanOutcome.NoChange(50), trigger, wins.toList())

    @Test
    fun `the page closes when its Action is gone`() = triggers.forEach {
        assertNull("$it", finished(it, developer).updated(wifi))
    }

    @Test
    fun `the page shows the new gain when its Action is still there`() = triggers.forEach {
        val cheaper = developer.copy(alsoFixes = emptyList())
        assertEquals("$it", cheaper, finished(it, wifi, cheaper).updated(developer))
    }

    @Test
    fun `another app's uninstall Action doesn't keep the page open`() {
        val chrome = QuickWin(QuickWinType.UNINSTALL_APP, PrivacyCheck.GOOGLE_CHROME)
        val maps = QuickWin(QuickWinType.UNINSTALL_APP, PrivacyCheck.GOOGLE_MAPS)
        assertNull(finished(UserScan.Rescan, maps).updated(chrome))
    }
}
