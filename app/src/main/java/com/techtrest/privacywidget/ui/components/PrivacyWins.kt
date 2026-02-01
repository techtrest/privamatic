package com.techtrest.privacywidget.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.techtrest.privacywidget.data.model.PrivacyCategory
import com.techtrest.privacywidget.data.model.PrivacyCheck
import com.techtrest.privacywidget.data.model.PrivacyScore

@Composable
fun PrivacyWins(
    privacyScore: PrivacyScore,
    modifier: Modifier = Modifier,
    initialExpanded: Boolean = false
) {
    val wins = collectPrivacyWins(privacyScore)
    var isExpanded by remember { mutableStateOf(initialExpanded) }

    if (wins.isEmpty()) {
        return // Don't show anything if no wins
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Clickable Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = "Privacy Wins",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Privacy Wins (${wins.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Animated Wins list
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    wins.forEach { win ->
                        PrivacyWinItem(win = win)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyWinItem(
    win: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = win,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Collect all privacy wins from the privacy score
 */
private fun collectPrivacyWins(privacyScore: PrivacyScore): List<String> {
    val wins = mutableListOf<String>()

    // Helper to check if a check is secure
    fun isSecure(check: PrivacyCheck): Boolean {
        return privacyScore.issues.find { it.check == check }?.isSecure == true
    }

    // Helper to get app name from a check if it's secure
    fun getAppName(check: PrivacyCheck): String? {
        val issue = privacyScore.issues.find { it.check == check }
        if (issue?.isSecure == true) {
            val status = issue.currentStatus
            // Extract app name from "Using [AppName]" format
            if (status.startsWith("Using ")) {
                return status.removePrefix("Using ").substringBefore(" (")
            }
        }
        return null
    }

    // Helper to check if any apps from a category are installed
    fun hasAnyInstalledApps(category: PrivacyCategory): Boolean {
        return category.checks.any { check ->
            val issue = privacyScore.issues.find { it.check == check }
            issue?.isSecure == false
        }
    }

    // SYSTEM SECURITY WINS
    if (isSecure(PrivacyCheck.SCREEN_LOCK)) {
        wins.add("Screen Lock enabled")
    }
    if (isSecure(PrivacyCheck.DEVICE_ENCRYPTION)) {
        wins.add("Device Encryption enabled")
    }
    if (isSecure(PrivacyCheck.BIOMETRIC_AUTH)) {
        wins.add("Biometric authentication active")
    }
    if (isSecure(PrivacyCheck.NOTIFICATION_LISTENER)) {
        wins.add("No Notification Listener abuse")
    }
    if (isSecure(PrivacyCheck.ACCESSIBILITY_SERVICE)) {
        wins.add("No Accessibility Service abuse")
    }
    if (isSecure(PrivacyCheck.DEVICE_ADMIN)) {
        wins.add("No Device Admin abuse")
    }

    // NETWORK & TRACKING WINS
    if (isSecure(PrivacyCheck.PRIVATE_DNS)) {
        wins.add("Private DNS configured")
    }
    if (isSecure(PrivacyCheck.VPN_CONNECTION)) {
        wins.add("VPN protection active")
    }
    if (isSecure(PrivacyCheck.ALWAYS_ON_VPN)) {
        val issue = privacyScore.issues.find { it.check == PrivacyCheck.ALWAYS_ON_VPN }
        // Only show if VPN is active and always-on is enabled (not "Not applicable")
        if (issue?.currentStatus == "Enabled") {
            wins.add("Always-On VPN enabled")
        }
    }
    if (isSecure(PrivacyCheck.ADVERTISING_ID)) {
        wins.add("Advertising ID disabled")
    }
    if (isSecure(PrivacyCheck.WIFI_SCANNING)) {
        wins.add("Background Wi-Fi scanning disabled")
    }

    // GOOGLE AVOIDANCE WINS
    if (isSecure(PrivacyCheck.FIND_MY_DEVICE)) {
        wins.add("Find My Device disabled")
    }

    // APP CHOICE WINS
    getAppName(PrivacyCheck.DEFAULT_BROWSER)?.let { appName ->
        wins.add("Using $appName browser")
    }
    getAppName(PrivacyCheck.DEFAULT_KEYBOARD)?.let { appName ->
        wins.add("Using $appName keyboard")
    }
    getAppName(PrivacyCheck.DEFAULT_SMS)?.let { appName ->
        wins.add("Using $appName messaging")
    }
    getAppName(PrivacyCheck.DEFAULT_EMAIL)?.let { appName ->
        wins.add("Using $appName email")
    }
    getAppName(PrivacyCheck.DEFAULT_LAUNCHER)?.let { appName ->
        wins.add("Using $appName launcher")
    }

    // ECOSYSTEM AVOIDANCE WINS
    if (!hasAnyInstalledApps(PrivacyCategory.META_FACEBOOK_APPS)) {
        wins.add("No Meta apps detected")
    }
    if (!hasAnyInstalledApps(PrivacyCategory.MICROSOFT_APPS)) {
        wins.add("No Microsoft ecosystem")
    }

    // Check for AI/LLM apps specifically (subset of AI_AND_OTHER_APPS category)
    val aiChecks = listOf(
        PrivacyCheck.CHATGPT_APP,
        PrivacyCheck.GOOGLE_GEMINI,
        PrivacyCheck.MICROSOFT_COPILOT,
        PrivacyCheck.CLAUDE_APP,
        PrivacyCheck.PERPLEXITY_APP,
        PrivacyCheck.META_AI
    )
    val hasAnyAIApps = aiChecks.any { check ->
        val issue = privacyScore.issues.find { it.check == check }
        issue?.isSecure == false
    }
    if (!hasAnyAIApps) {
        wins.add("No AI/LLM apps detected")
    }

    // TikTok specifically
    if (isSecure(PrivacyCheck.TIKTOK_APP)) {
        wins.add("No TikTok")
    }

    return wins
}
