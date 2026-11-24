package com.techtrest.privamatic.ui.utils

import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import com.techtrest.privamatic.R
import com.techtrest.privamatic.data.ScanOutcome
import com.techtrest.privamatic.data.model.ManualCheckType
import com.techtrest.privamatic.data.model.PrivacyCheck

/** The lookups [ScanFeedbackText] needs, so its texts can be tested without Android. */
interface FeedbackStrings {
    fun string(@StringRes id: Int, vararg args: Any): String
    fun plural(@PluralsRes id: Int, count: Int): String
}

class ResourceFeedbackStrings(private val resources: Resources) : FeedbackStrings {
    override fun string(id: Int, vararg args: Any): String = resources.getString(id, *args)
    override fun plural(id: Int, count: Int): String = resources.getQuantityString(id, count, count)
}

/** The snackbar text for a finished user scan. */
object ScanFeedbackText {

    fun text(outcome: ScanOutcome, strings: FeedbackStrings): String = when (outcome) {
        is ScanOutcome.CheckFixed -> strings.line(
            strings.string(R.string.fmt_snackbar_fixed, strings.name(outcome.check)),
            strings.gain(outcome.points), outcome.score
        )
        is ScanOutcome.ChecksFixed -> strings.line(
            strings.plural(R.plurals.plural_snackbar_checks_fixed, outcome.count),
            strings.gain(outcome.points), outcome.score
        )
        is ScanOutcome.CheckImproved ->
            strings.line(strings.name(outcome.check), strings.gain(outcome.points), outcome.score)
        is ScanOutcome.ReviewDone -> strings.line(
            strings.string(R.string.fmt_snackbar_review_done, strings.string(reviewName(outcome.type))),
            outcome.points?.let { strings.gain(it) }, outcome.score
        )
        is ScanOutcome.AdIdConfirmed -> strings.line(
            strings.string(R.string.fmt_snackbar_confirmed, strings.name(PrivacyCheck.ADVERTISING_ID)),
            outcome.points?.let { strings.gain(it) }, outcome.score
        )
        is ScanOutcome.Gain -> strings.line(strings.gain(outcome.points), null, outcome.score)
        is ScanOutcome.Loss -> {
            val points = strings.plural(R.plurals.plural_snackbar_loss, outcome.points)
            if (outcome.check == null) strings.line(points, null, outcome.score)
            else strings.line(strings.name(outcome.check), points, outcome.score)
        }
        is ScanOutcome.Mixed -> strings.line(
            if (outcome.exact) {
                strings.string(
                    R.string.fmt_snackbar_mixed,
                    strings.plural(R.plurals.plural_snackbar_fixed, outcome.better),
                    strings.plural(R.plurals.plural_snackbar_new_issues, outcome.worse)
                )
            } else {
                strings.string(R.string.fmt_snackbar_better_worse, outcome.better, outcome.worse)
            },
            null, outcome.score
        )
        is ScanOutcome.NotFixedYet ->
            strings.line(strings.string(R.string.copy_snackbar_not_fixed), null, outcome.score)
        is ScanOutcome.NoChange ->
            strings.line(strings.string(R.string.copy_snackbar_no_change), null, outcome.score)
    }

    @StringRes
    private fun reviewName(type: ManualCheckType): Int = when (type) {
        ManualCheckType.LOCATION_ALWAYS_ON -> R.string.label_snackbar_review_location
        ManualCheckType.CAMERA_MIC_ACCESS -> R.string.label_snackbar_review_camera_mic
        ManualCheckType.UNUSED_APPS -> R.string.label_snackbar_review_unused_apps
        // Has its own trigger and text; here only so the mapping is complete.
        ManualCheckType.ADVERTISING_ID_CHECK -> R.string.manual_check_advertising_id_check_name
    }

    private fun FeedbackStrings.name(check: PrivacyCheck) = string(check.displayName)

    private fun FeedbackStrings.gain(points: Int) = plural(R.plurals.plural_quick_win_points, points)

    private fun FeedbackStrings.line(headline: String, points: String?, score: Int): String =
        if (points == null) string(R.string.fmt_snackbar_headline, headline, score)
        else string(R.string.fmt_snackbar_headline_points, headline, points, score)
}
