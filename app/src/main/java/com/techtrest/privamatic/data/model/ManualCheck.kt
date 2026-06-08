package com.techtrest.privamatic.data.model

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.graphics.vector.ImageVector
import com.techtrest.privamatic.R

/**
 * Types of manual privacy checks users should perform periodically.
 * Each check has a maintenance period and point value.
 */
enum class ManualCheckType(
    @StringRes val displayName: Int,
    val icon: ImageVector,
    val periodDays: Int,
    val pointValue: Int,
    @StringRes val description: Int
) {
    LOCATION_ALWAYS_ON(
        displayName = R.string.manual_check_location_always_on_name,
        icon = Icons.Default.Place,
        periodDays = 60,
        pointValue = 5,
        description = R.string.manual_check_location_always_on_description
    ),
    CAMERA_MIC_ACCESS(
        displayName = R.string.manual_check_camera_mic_access_name,
        icon = Icons.Default.Videocam,
        periodDays = 90,
        pointValue = 5,
        description = R.string.manual_check_camera_mic_access_description
    ),
    UNUSED_APPS(
        displayName = R.string.manual_check_unused_apps_name,
        icon = Icons.Default.Apps,
        periodDays = 120,
        pointValue = 5,
        description = R.string.manual_check_unused_apps_description
    ),
    ADVERTISING_ID_CHECK(
        displayName = R.string.manual_check_advertising_id_check_name,
        icon = Icons.Default.TrackChanges,
        periodDays = 180,
        pointValue = 5,
        description = R.string.manual_check_advertising_id_check_description
    )
}

/**
 * Current state of a manual privacy check.
 *
 * @property type The type of check
 * @property lastCompletedTimestamp Timestamp when check was last completed (0 = never)
 * @property daysRemaining Days until next review is due
 * @property fillPercentage Progress bar fill (0.0 to 1.0)
 * @property isOverdue Whether the check is past due
 */
data class ManualCheckState(
    val type: ManualCheckType,
    val lastCompletedTimestamp: Long,
    val daysRemaining: Int,
    val fillPercentage: Float,
    val isOverdue: Boolean
)
