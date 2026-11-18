package com.techtrest.privamatic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Other apps must not be able to trigger a full scan (docs/specs/widget-refresh-hardening.md).
 * Unit tests run with the module directory as working directory.
 */
class WidgetManifestTest {

    private val androidNs = "http://schemas.android.com/apk/res/android"

    private fun receivers(path: String): List<Element> {
        val nodes = DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(File(path))
            .documentElement
            .getElementsByTagName("receiver")
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }

    private fun name(element: Element) = element.getAttributeNS(androidNs, "name")

    private fun provider(): Element =
        receivers("src/main/AndroidManifest.xml").single { name(it) == ".PrivacyWidgetProvider" }

    @Test
    fun widgetProviderIsNotExported() {
        assertEquals("false", provider().getAttributeNS(androidNs, "exported"))
    }

    @Test
    fun widgetProviderStillDeclaresTheWidget() {
        val provider = provider()
        val actions = provider.getElementsByTagName("action")
        val names = (0 until actions.length).map { name(actions.item(it) as Element) }
        assertTrue(names.contains("android.appwidget.action.APPWIDGET_UPDATE"))
        val meta = provider.getElementsByTagName("meta-data").item(0) as? Element
        assertNotNull(meta)
        assertEquals("android.appwidget.provider", meta?.let(::name))
    }

    @Test
    fun debugReceiverIsDeclaredOnlyInTheDebugManifest() {
        assertFalse(receivers("src/main/AndroidManifest.xml").any { name(it) == ".WidgetDebugReceiver" })
        assertEquals(1, receivers("src/debug/AndroidManifest.xml").count { name(it) == ".WidgetDebugReceiver" })
        assertFalse(File("src/release/AndroidManifest.xml").exists())
        assertFalse(File("src/release/java/com/techtrest/privamatic/WidgetDebugReceiver.kt").exists())
    }
}
