package com.techtrest.privamatic.ui.components

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.techtrest.privamatic.R
import com.techtrest.privamatic.data.model.ScoreHistory
import com.techtrest.privamatic.data.util.DeviceNameUtil
import kotlin.math.abs

@Composable
fun DeviceInfoCard(
    scoreHistory: ScoreHistory?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scoreDelta = scoreHistory?.scoreDelta
    val showPrivacyChange = scoreDelta != null &&
            scoreDelta != 0 &&
            scoreHistory != null &&
            (System.currentTimeMillis() - scoreHistory.lastUpdateTimestamp) <= CHANGE_EXPIRY_MS

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.label_device_info_title),
                style = MaterialTheme.typography.titleMedium
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            DeviceInfoRow(stringResource(R.string.label_device_info_model), DeviceNameUtil.getMarketingName())
            DeviceInfoRow(stringResource(R.string.label_device_info_os), detectOperatingSystem(context))
            DeviceInfoRow(stringResource(R.string.label_device_info_android_version), Build.VERSION.RELEASE)

            if (showPrivacyChange && scoreDelta != null) {
                val isIncrease = scoreDelta > 0
                val changeText = if (isIncrease) {
                    stringResource(R.string.fmt_score_delta_up, abs(scoreDelta))
                } else {
                    stringResource(R.string.fmt_score_delta_down, abs(scoreDelta))
                }
                val changeColor = if (isIncrease) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
                DeviceInfoRow(
                    label = stringResource(R.string.label_device_info_privacy_change),
                    value = changeText,
                    valueColor = changeColor
                )
            }
        }
    }
}

private const val CHANGE_EXPIRY_MS = 172_800_000L  // 48 hours

/**
 * Detects the operating system/ROM running on the device
 */
private fun detectOperatingSystem(context: Context): String {
    // Check GrapheneOS FIRST via system packages
    // GrapheneOS mimics stock Android in Build.FINGERPRINT for privacy/compatibility
    if (isGrapheneOS(context)) return "GrapheneOS"

    // Convert all Build properties to lowercase for case-insensitive comparison
    val fingerprint = Build.FINGERPRINT.lowercase()
    val display = Build.DISPLAY.lowercase()
    val manufacturer = Build.MANUFACTURER.lowercase()
    val brand = Build.BRAND.lowercase()

    return when {
        fingerprint.contains("calyx") || brand.contains("calyx") -> "CalyxOS"
        display.contains("lineage") || fingerprint.contains("lineage") -> "LineageOS"
        display.contains("/e/") || display.contains("eos") || brand.contains("e_os") -> "/e/OS"
        manufacturer == "samsung" -> "Samsung One UI"
        manufacturer == "xiaomi" -> "Xiaomi MIUI"
        manufacturer == "oneplus" -> "OnePlus OxygenOS"
        manufacturer == "oppo" || manufacturer == "realme" -> "ColorOS"
        manufacturer == "google" -> "Stock Android"
        else -> "Android"
    }
}

/**
 * GrapheneOS detection via system packages
 * GrapheneOS mimics stock Android in Build properties for privacy/compatibility,
 * so we need to detect it via GrapheneOS-specific system packages
 */
private fun isGrapheneOS(context: Context): Boolean {
    val grapheneOSPackages = listOf(
        "app.grapheneos.apps",
        "app.grapheneos.camera",
        "app.grapheneos.gmscompat",
        "app.grapheneos.info"
    )

    for (pkg in grapheneOSPackages) {
        try {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(pkg, 0)
            return true
        } catch (e: PackageManager.NameNotFoundException) {
            // Package not found, continue checking
        }
    }

    return false
}

@Composable
private fun DeviceInfoRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor
        )
    }
}
