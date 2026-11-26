package com.techtrest.privamatic.ui.utils

import com.techtrest.privamatic.R
import com.techtrest.privamatic.data.ScanOutcome
import com.techtrest.privamatic.data.model.ManualCheckType
import com.techtrest.privamatic.data.model.PrivacyCheck
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** The exact snackbar texts, built from strings.xml with the English one/other rule. */
class ScanFeedbackTextTest {

    private object XmlStrings : FeedbackStrings {
        private val root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File("src/main/res/values/strings.xml")).documentElement
        private val strings = elements("string").associate { it.getAttribute("name") to it.textContent }
        private val plurals = elements("plurals").associate { plural ->
            val items = plural.getElementsByTagName("item")
            plural.getAttribute("name") to (0 until items.length).map { items.item(it) as Element }
                .associate { it.getAttribute("quantity") to it.textContent }
        }

        private fun elements(tag: String): List<Element> {
            val nodes = root.getElementsByTagName(tag)
            return (0 until nodes.length).map { nodes.item(it) as Element }
        }

        private fun name(holder: Class<*>, id: Int) = holder.fields.first { it.getInt(null) == id }.name

        override fun string(id: Int, vararg args: Any): String =
            strings.getValue(name(R.string::class.java, id)).replace("\\'", "'").let {
                if (args.isEmpty()) it else it.format(*args)
            }

        override fun plural(id: Int, count: Int): String =
            plurals.getValue(name(R.plurals::class.java, id))
                .getValue(if (count == 1) "one" else "other").format(count)
    }

    private fun check(expected: String, outcome: ScanOutcome) =
        // "Score 36" holds a no-break space, so the number never wraps alone
        assertEquals(expected.replace("Score ", "Score\u00A0"), ScanFeedbackText.text(outcome, XmlStrings))

    @Test
    fun `one fix`() =
        check("Screen Lock fixed · +11 pts · Score 36", ScanOutcome.CheckFixed(PrivacyCheck.SCREEN_LOCK, 11, 36))

    @Test
    fun `one point is singular`() {
        check("Developer Options fixed · +1 pt · Score 36", ScanOutcome.CheckFixed(PrivacyCheck.DEVELOPER_OPTIONS, 1, 36))
        check("−1 pt · Score 32", ScanOutcome.Loss(null, 1, 32))
        check("+1 pt · Score 32", ScanOutcome.Gain(1, 32, hasLosses = false))
    }

    @Test
    fun `several fixes`() = check("2 checks fixed · +6 pts · Score 41", ScanOutcome.ChecksFixed(2, 6, 41))

    @Test
    fun `partial improvement`() =
        check("Private DNS · +3 pts · Score 36", ScanOutcome.CheckImproved(PrivacyCheck.PRIVATE_DNS, 3, 36))

    @Test
    fun reviews() {
        check("Camera & Mic review done · +5 pts · Score 40", ScanOutcome.ReviewDone(ManualCheckType.CAMERA_MIC_ACCESS, 5, 40))
        check("Location review done · +5 pts · Score 40", ScanOutcome.ReviewDone(ManualCheckType.LOCATION_ALWAYS_ON, 5, 40))
        check("Unused apps review done · Score 40", ScanOutcome.ReviewDone(ManualCheckType.UNUSED_APPS, null, 40))
    }

    @Test
    fun `Ad ID`() {
        check("Advertising ID confirmed · +5 pts · Score 30", ScanOutcome.AdIdConfirmed(5, 30))
        check("Advertising ID confirmed · Score 30", ScanOutcome.AdIdConfirmed(null, 30))
    }

    @Test
    fun losses() {
        check("−3 pts · Score 32", ScanOutcome.Loss(null, 3, 32))
        check("Private DNS · −3 pts · Score 32", ScanOutcome.Loss(PrivacyCheck.PRIVATE_DNS, 3, 32))
    }

    @Test
    fun mixed() {
        check("1 fixed, 1 new issue · Score 33", ScanOutcome.Mixed(1, 1, exact = true, score = 33))
        check("1 fixed, 2 new issues · Score 33", ScanOutcome.Mixed(1, 2, exact = true, score = 33))
        check("1 better, 1 worse · Score 33", ScanOutcome.Mixed(1, 1, exact = false, score = 33))
    }

    @Test
    fun unchanged() {
        check("Not fixed yet · Score 33", ScanOutcome.NotFixedYet(PrivacyCheck.PRIVATE_DNS, 33))
        check("No change · Score 33", ScanOutcome.NoChange(33))
    }

    @Test
    fun `failed scan text has no exclamation mark`() {
        val text = XmlStrings.string(R.string.copy_snackbar_scan_failed)
        assertEquals("Couldn't check. Your last result is still shown.", text)
        assertFalse('!' in text)
    }
}
