package com.techtrest.privamatic.data.model

import androidx.annotation.StringRes
import com.techtrest.privamatic.R

/**
 * Category of a tracker SDK as classified by the Exodus Privacy database.
 */
enum class TrackerCategory(@StringRes val displayNameRes: Int) {
    ADVERTISEMENT(R.string.tracker_category_advertisement_name),
    ANALYTICS(R.string.tracker_category_analytics_name),
    LOCATION(R.string.tracker_category_location_name),
    PROFILING(R.string.tracker_category_profiling_name),
    IDENTIFICATION(R.string.tracker_category_identification_name),
    CRASH_REPORTING(R.string.tracker_category_crash_reporting_name);

    companion object {
        /** Maps the category label used in trackers.json to an enum entry. */
        fun fromJsonName(name: String): TrackerCategory? = when (name) {
            "Advertisement" -> ADVERTISEMENT
            "Analytics" -> ANALYTICS
            "Location" -> LOCATION
            "Profiling" -> PROFILING
            "Identification" -> IDENTIFICATION
            "Crash reporting" -> CRASH_REPORTING
            else -> null
        }
    }
}

/**
 * A tracker SDK detected inside an installed app.
 */
data class TrackerSdk(
    val name: String,
    val categories: List<TrackerCategory>
)

/**
 * All tracker SDKs found in a single installed app.
 */
data class AppSdkFindings(
    val packageName: String,
    val appName: String,
    val trackers: List<TrackerSdk>
)

/**
 * Result of a full SDK fingerprinting scan across installed apps.
 */
data class SdkScanResult(
    val timestamp: Long,
    val findings: List<AppSdkFindings>
)
