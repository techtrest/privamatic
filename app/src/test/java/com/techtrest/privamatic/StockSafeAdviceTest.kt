package com.techtrest.privamatic

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * v1.6 hard rule (docs/decisions/v1.6-fix-to-result.md, section 2): nothing tells the user
 * to disable or uninstall an app that came with the phone. Reads strings.xml as text.
 * Unit tests run with the module directory as working directory.
 */
class StockSafeAdviceTest {

    private val strings: Map<String, String> by lazy {
        val root = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(File("src/main/res/values/strings.xml"))
            .documentElement
        val nodes = root.getElementsByTagName("string")
        (0 until nodes.length)
            .map { nodes.item(it) as Element }
            .associate { it.getAttribute("name") to it.textContent }
    }

    private fun text(name: String): String =
        requireNotNull(strings[name]) { "missing string $name" }.lowercase()

    /** Shown only on rows for apps that came with the phone. */
    private val systemAppStrings = listOf(
        "fmt_recommendation_system_app",
        "privacy_check_google_camera_system_recommendation"
    )

    @Test
    fun `system-app advice never says disable`() {
        for (name in systemAppStrings) {
            assertFalse(name, "disabl" in text(name))
        }
    }

    @Test
    fun `system-app advice never says uninstall or remove it`() {
        for (name in systemAppStrings) {
            val t = text(name)
            assertFalse(name, "uninstall it" in t)
            assertFalse(name, "remove it" in t)
        }
    }

    @Test
    fun `system-app advice says the app came with the phone`() {
        for (name in systemAppStrings) {
            assertTrue(name, "came with your phone" in text(name))
        }
    }

    @Test
    fun `system-app advice has no exclamation marks`() {
        for (name in systemAppStrings) {
            assertFalse(name, '!' in text(name))
        }
    }

    @Test
    fun `uninstall quick win no longer mentions pre-installed apps`() {
        // The tile only exists for user-installed apps (QuickWinsDetector.checkInstalledApps).
        val t = text("quick_win_uninstall_app_instructions")
        assertFalse("pre-installed" in t)
        assertFalse("preinstalled" in t)
        assertFalse("came with" in t)
    }

    @Test
    fun `uninstall quick win never mentions disabling`() {
        // User-installed apps have Uninstall on their App info page, no Disable.
        assertFalse("disabl" in text("quick_win_uninstall_app_instructions"))
        assertTrue("Tap Uninstall." in requireNotNull(strings["quick_win_uninstall_app_instructions"]))
    }
}
