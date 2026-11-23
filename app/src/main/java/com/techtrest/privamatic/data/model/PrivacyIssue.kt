package com.techtrest.privamatic.data.model

import androidx.annotation.StringRes
import com.techtrest.privamatic.R

data class PrivacyIssue(
    val check: PrivacyCheck,
    val isSecure: Boolean,
    val currentStatus: String,
    val technicalDetails: String? = null,
    val customPointDeduction: Int? = null,
    val isSystemApp: Boolean = false,
    val flaggedPackages: List<String> = emptyList(),
    /**
     * The checker could not determine the real state (no API, unrecognised app, or an
     * error). Unknown results cost 0 points. isUnknown controls display and counting
     * everywhere: info icon, excluded from both pass and issue counts. isSecure only
     * decides whether the related quick win and tips appear.
     */
    val isUnknown: Boolean = false,
    /** The detected app's display name, for checks that report one (default apps). */
    val appName: String? = null
) {
    val pointDeduction: Int
        get() = if (isSecure) 0 else (customPointDeduction ?: check.pointDeduction)

    @get:StringRes
    val recommendation: Int
        get() = check.recommendation

    /**
     * A flagged app that shipped with the device can't be uninstalled, so its row must not
     * suggest it: it gets [systemAppRecommendation] (formatted with the app's name) instead.
     */
    val isPreinstalledApp: Boolean
        get() = isSystemApp && check.packageName != null

    @get:StringRes
    val systemAppRecommendation: Int
        get() = check.systemAppRecommendation ?: R.string.fmt_recommendation_system_app

    /**
     * The settings page the row's button opens, or null for no button. A preinstalled app
     * gets none: its App info page puts Disable one tap away, which can break a stock phone
     * (v1.6 hard rule, Oliver's call 10). Its point deduction is unchanged (call 12).
     */
    val rowActionType: ActionType?
        get() = if (isPreinstalledApp) null else check.actionType
}

/**
 * True when this issue has flagged packages and every one of them is in the
 * user's trusted set — i.e. the issue should be treated as fully resolved by trust.
 */
fun PrivacyIssue.isFullyTrusted(trusted: Set<String>): Boolean =
    flaggedPackages.isNotEmpty() && flaggedPackages.all { it in trusted }
