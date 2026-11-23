package com.techtrest.privamatic.data.model

import com.techtrest.privamatic.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class PrivacyIssueTest {

    /** Reads strings.xml as text, like StockSafeAdviceTest (module dir is the working dir). */
    private fun stringResource(name: String): String {
        val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File("src/main/res/values/strings.xml"))
            .documentElement.getElementsByTagName("string")
        return (0 until nodes.length).map { nodes.item(it) as Element }
            .single { it.getAttribute("name") == name }.textContent
    }

    private fun installed(check: PrivacyCheck, isSystemApp: Boolean) = PrivacyIssue(
        check = check,
        isSecure = false,
        currentStatus = "Installed",
        isSystemApp = isSystemApp
    )

    @Test
    fun `play services offers no settings action`() {
        // Disabling GMS on stock Android breaks the Play Store, push and Find Hub (audit H3).
        assertNull(PrivacyCheck.GOOGLE_PLAY_SERVICES.actionType)
    }

    @Test
    fun `play services is not treated as a preinstalled app row`() {
        val issue = installed(PrivacyCheck.GOOGLE_PLAY_SERVICES, isSystemApp = true)
        assertFalse(issue.isPreinstalledApp)
    }

    @Test
    fun `preinstalled app gets the generic came-with-your-phone advice`() {
        val issue = installed(PrivacyCheck.GOOGLE_MAPS, isSystemApp = true)
        assertTrue(issue.isPreinstalledApp)
        assertEquals(R.string.fmt_recommendation_system_app, issue.systemAppRecommendation)
    }

    @Test
    fun `preinstalled camera gets its own keep-it advice`() {
        val issue = installed(PrivacyCheck.GOOGLE_CAMERA, isSystemApp = true)
        assertTrue(issue.isPreinstalledApp)
        assertEquals(
            R.string.privacy_check_google_camera_system_recommendation,
            issue.systemAppRecommendation
        )
    }

    @Test
    fun `user-installed camera keeps its uninstall recommendation`() {
        // GrapheneOS and other ROMs: Google Camera is a user app and may be removed.
        val issue = installed(PrivacyCheck.GOOGLE_CAMERA, isSystemApp = false)
        assertFalse(issue.isPreinstalledApp)
        assertEquals(PrivacyCheck.GOOGLE_CAMERA.recommendation, issue.recommendation)
    }

    @Test
    fun `user-installed app keeps its uninstall recommendation`() {
        val issue = installed(PrivacyCheck.GOOGLE_KEEP, isSystemApp = false)
        assertFalse(issue.isPreinstalledApp)
        assertEquals(PrivacyCheck.GOOGLE_KEEP.recommendation, issue.recommendation)
    }

    // ----- Row button (Oliver's call 10) -----

    @Test
    fun `preinstalled flagged app row has no settings button`() {
        for (check in listOf(PrivacyCheck.GOOGLE_CHROME, PrivacyCheck.GMAIL_APP,
            PrivacyCheck.YOUTUBE, PrivacyCheck.GOOGLE_CAMERA)) {
            assertNull(check.name, installed(check, isSystemApp = true).rowActionType)
        }
    }

    @Test
    fun `user-installed flagged app row keeps its app settings button`() {
        val issue = installed(PrivacyCheck.FACEBOOK_APP, isSystemApp = false)
        assertEquals(ActionType.OPEN_APP_SETTINGS, issue.rowActionType)
    }

    @Test
    fun `preinstalled informational app row has no settings button either`() {
        // Gemini ships with stock Pixels; its App info page has the same Disable button.
        val issue = installed(PrivacyCheck.GOOGLE_GEMINI, isSystemApp = true)
        assertNull(issue.rowActionType)
    }

    @Test
    fun `rows without an app keep their check's button`() {
        // isSystemApp only means something for app rows; a settings row is never preinstalled.
        val issue = installed(PrivacyCheck.NOTIFICATION_LISTENER, isSystemApp = true)
        assertEquals(ActionType.NOTIFICATION_LISTENER, issue.rowActionType)
    }

    // ----- Scoring unchanged (Oliver's call 12) -----

    @Test
    fun `preinstalled flagged app still costs its points`() {
        val issue = installed(PrivacyCheck.GOOGLE_CHROME, isSystemApp = true)
        assertEquals(PrivacyCheck.GOOGLE_CHROME.pointDeduction, issue.pointDeduction)
        assertEquals(1, issue.pointDeduction)
    }

    @Test
    fun `every scored app check costs 1 pt as the preinstalled advice says`() {
        // fmt_recommendation_system_app says "so it costs 1 pt while it's here". If an app
        // check's value or that text changes, the other must change with it.
        assertTrue("costs 1 pt" in stringResource("fmt_recommendation_system_app"))
        val scoredAppChecks = PrivacyCheck.entries.filter {
            it.packageName != null && !it.isInformational
        }
        assertTrue(scoredAppChecks.isNotEmpty())
        for (check in scoredAppChecks) {
            assertEquals(check.name, 1, check.pointDeduction)
        }
    }
}
