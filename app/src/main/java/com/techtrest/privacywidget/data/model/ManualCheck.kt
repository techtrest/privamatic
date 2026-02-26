package com.techtrest.privacywidget.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Types of manual privacy checks users should perform periodically.
 * Each check has a maintenance period and point value.
 */
enum class ManualCheckType(
    val displayName: String,
    val icon: ImageVector,
    val periodDays: Int,
    val pointValue: Int,
    val description: String
) {
    LOCATION_ALWAYS_ON(
        displayName = "Always-On Location Access",
        icon = Icons.Default.Place,
        periodDays = 60,
        pointValue = 5,
        description = "Most apps don't need 24/7 location tracking. Review which apps have \"Always\" access."
    ),
    CAMERA_MIC_ACCESS(
        displayName = "Camera & Microphone Access",
        icon = Icons.Default.Videocam,
        periodDays = 90,
        pointValue = 5,
        description = "Many apps over-request camera and microphone permissions. Review which apps can access them."
    ),
    UNUSED_APPS(
        displayName = "Unused Apps Review",
        icon = Icons.Default.Apps,
        periodDays = 120,
        pointValue = 5,
        description = "Unused apps still have permissions and potential vulnerabilities. Find and remove apps you haven't used."
    ),
    ADVERTISING_ID_CHECK(
        displayName = "Advertising ID",
        icon = Icons.Default.TrackChanges,
        periodDays = 180,
        pointValue = 5,
        description = "Verify your Advertising ID is deleted to prevent cross-app tracking by advertisers."
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
