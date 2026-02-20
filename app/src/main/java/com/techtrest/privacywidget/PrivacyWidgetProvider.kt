package com.techtrest.privacywidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.techtrest.privacywidget.data.ScoreHistoryRepository
import com.techtrest.privacywidget.data.scanner.PrivacyScanner
import com.techtrest.privacywidget.data.scanner.PrivacyScoreCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.compose.ui.graphics.toArgb

class PrivacyWidgetProvider : AppWidgetProvider() {

    // -------------------------------------------------------------------------
    // Lifecycle callbacks
    // -------------------------------------------------------------------------

    /** Called when the first widget instance is added to the home screen. */
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        schedulePeriodicUpdates(context)
    }

    /** Called when the last widget instance is removed from the home screen. */
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelPeriodicUpdates(context)
    }

    /** Clean up per-widget preferences when widget instances are deleted. */
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        for (widgetId in appWidgetIds) {
            WidgetPreferences.deleteOpacity(context, widgetId)
        }
    }

    /**
     * Handles both the standard APPWIDGET_UPDATE broadcast (routed here by
     * [AppWidgetProvider.onReceive]) and our custom ACTION_UPDATE_WIDGET
     * broadcast (sent by AlarmManager, BootReceiver, and the app after a scan).
     */
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent) // routes APPWIDGET_UPDATE → onUpdate()
        if (intent.action == ACTION_UPDATE_WIDGET) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, PrivacyWidgetProvider::class.java)
            )
            if (ids.isNotEmpty()) {
                onUpdate(context, manager, ids)
            }
        }
    }

    /** Runs the privacy scan and refreshes every placed widget instance. */
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // goAsync() returns null when onUpdate is called directly (not via broadcast),
        // e.g. from WidgetConfigurationActivity. Safe-call ensures no NPE.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val scanner = PrivacyScanner(context)
                val privacyScore = scanner.performCompleteScan()
                val score = privacyScore.score
                val rating = PrivacyScoreCalculator.getScoreRating(score)
                val deviceName = Build.MODEL
                val osName = detectOperatingSystem(context)
                val ratingText = rating.toShortLabel()

                // Record score history and calculate delta
                val scoreHistoryRepository = ScoreHistoryRepository(context)
                val scoreHistory = scoreHistoryRepository.recordScore(score)
                val scoreDelta = scoreHistory.scoreDelta
                val showPrivacyChange = scoreDelta != null &&
                        scoreDelta != 0 &&
                        (System.currentTimeMillis() - scoreHistory.lastUpdateTimestamp) <= CHANGE_EXPIRY_MS

                Log.d(TAG, "PRIVACY_CHANGE_DEBUG: score=$score, currentScore=${scoreHistory.currentScore}, dailyBaseline=${scoreHistory.dailyBaselineScore}, scoreDelta=$scoreDelta, showPrivacyChange=$showPrivacyChange, timeDiff=${System.currentTimeMillis() - scoreHistory.lastUpdateTimestamp}")

                for (appWidgetId in appWidgetIds) {
                    updateWidget(
                        context = context,
                        appWidgetManager = appWidgetManager,
                        appWidgetId = appWidgetId,
                        deviceName = deviceName,
                        osName = osName,
                        scoreText = score.toString(),
                        ratingText = ratingText,
                        scoreDelta = scoreDelta,
                        showDelta = showPrivacyChange
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating widget", e)
                val deviceName = Build.MODEL
                val osName = detectOperatingSystem(context)
                for (appWidgetId in appWidgetIds) {
                    updateWidget(
                        context = context,
                        appWidgetManager = appWidgetManager,
                        appWidgetId = appWidgetId,
                        deviceName = deviceName,
                        osName = osName,
                        scoreText = "--",
                        ratingText = "—",
                        scoreDelta = null,
                        showDelta = false
                    )
                }
            } finally {
                pendingResult?.finish()
            }
        }
    }

    // -------------------------------------------------------------------------
    // RemoteViews builder
    // -------------------------------------------------------------------------

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        deviceName: String,
        osName: String,
        scoreText: String,
        ratingText: String,
        scoreDelta: Int?,
        showDelta: Boolean
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_privacy)

        views.setTextViewText(R.id.widget_device_name, deviceName)
        views.setTextViewText(R.id.widget_os_name, osName)
        views.setTextViewText(R.id.widget_score_value, scoreText)
        views.setTextViewText(R.id.widget_score_rating, ratingText)

        // Handle privacy change indicator.
        // Uses two separate TextViews (up/down) with XML-defined colors so that
        // theme changes (light↔dark) always resolve the correct color from
        // resource qualifiers, avoiding stale programmatic setTextColor values.
        Log.d(TAG, "PRIVACY_CHANGE_DEBUG: updateWidget - showDelta=$showDelta, scoreDelta=$scoreDelta")

        if (showDelta && scoreDelta != null) {
            val isIncrease = scoreDelta > 0
            val changeText = "${if (isIncrease) "↑" else "↓"}${abs(scoreDelta)}"

            Log.d(TAG, "PRIVACY_CHANGE_DEBUG: Setting visible - text=$changeText, isIncrease=$isIncrease")

            if (isIncrease) {
                views.setTextViewText(R.id.widget_privacy_change_up, changeText)
                views.setViewVisibility(R.id.widget_privacy_change_up, View.VISIBLE)
                views.setViewVisibility(R.id.widget_privacy_change_down, View.GONE)
            } else {
                views.setTextViewText(R.id.widget_privacy_change_down, changeText)
                views.setViewVisibility(R.id.widget_privacy_change_down, View.VISIBLE)
                views.setViewVisibility(R.id.widget_privacy_change_up, View.GONE)
            }
        } else {
            Log.d(TAG, "PRIVACY_CHANGE_DEBUG: Hiding indicator")
            views.setViewVisibility(R.id.widget_privacy_change_up, View.GONE)
            views.setViewVisibility(R.id.widget_privacy_change_down, View.GONE)
        }

        // Apply per-widget opacity to background only (text/icons stay fully opaque)
        val opacity = WidgetPreferences.getOpacity(context, appWidgetId)
        views.setFloat(R.id.widget_background, "setAlpha", opacity / 100f)

        // On Android 11 and below (API < 31), android:theme on non-root RemoteViews
        // views is not properly supported and can cause RemoteViews inflation failure
        // on some builds (e.g. LineageOS on older hardware), showing "Problem loading
        // widget". The android:theme attribute has been removed from the base layout,
        // and here we explicitly apply static brand colours (Cream text) so the widget
        // is always readable regardless of the device's theme engine or colour resolution.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            val cream = Cream.toArgb()
            views.setTextColor(R.id.widget_device_name, cream)
            views.setTextColor(R.id.widget_os_name, cream)
            views.setTextColor(R.id.widget_score_value, cream)
            views.setTextColor(R.id.widget_score_rating, cream)
            // Change indicators: use light colours visible on the dark BRG/dark background
            views.setTextColor(R.id.widget_privacy_change_up, 0xFFA5D6A7.toInt())
            views.setTextColor(R.id.widget_privacy_change_down, 0xFFEF9A9A.toInt())
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    // -------------------------------------------------------------------------
    // Companion: alarm scheduling + OS detection (shared with BootReceiver)
    // -------------------------------------------------------------------------

    companion object {
        private const val TAG = "PrivacyWidgetProvider"

        /** Broadcast action used by AlarmManager, BootReceiver, and the app. */
        const val ACTION_UPDATE_WIDGET = "com.techtrest.privacywidget.ACTION_UPDATE_WIDGET"

        private const val ALARM_REQUEST_CODE = 1001

        /** Six hours in milliseconds. */
        private const val UPDATE_INTERVAL_MS = 6L * 60 * 60 * 1000

        /** 48 hours in milliseconds - privacy change expiry time. */
        private const val CHANGE_EXPIRY_MS = 172_800_000L

        /**
         * Registers an inexact 6-hour repeating alarm.
         * Safe to call multiple times — subsequent calls just update the existing alarm.
         */
        fun schedulePeriodicUpdates(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + UPDATE_INTERVAL_MS,
                UPDATE_INTERVAL_MS,
                buildAlarmPendingIntent(context)
            )
            Log.d(TAG, "Periodic widget updates scheduled (6-hour interval)")
        }

        /** Cancels the repeating alarm set by [schedulePeriodicUpdates]. */
        fun cancelPeriodicUpdates(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(buildAlarmPendingIntent(context))
            Log.d(TAG, "Periodic widget updates cancelled")
        }

        /**
         * Sends ACTION_UPDATE_WIDGET so the provider re-runs the scan immediately.
         * Call this from the app after a completed scan to keep the widget in sync.
         */
        fun requestImmediateUpdate(context: Context) {
            val intent = Intent(context, PrivacyWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_WIDGET
            }
            context.sendBroadcast(intent)
        }

        private fun buildAlarmPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, PrivacyWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_WIDGET
            }
            return PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        // -----------------------------------------------------------------
        // OS detection – mirrors DeviceInfoCard logic
        // -----------------------------------------------------------------

        fun detectOperatingSystem(context: Context): String {
            if (isGrapheneOS(context)) return "GrapheneOS"

            val fingerprint = Build.FINGERPRINT.lowercase()
            val display = Build.DISPLAY.lowercase()
            val manufacturer = Build.MANUFACTURER.lowercase()
            val brand = Build.BRAND.lowercase()

            return when {
                fingerprint.contains("calyx") || brand.contains("calyx") -> "CalyxOS"
                display.contains("lineage") || fingerprint.contains("lineage") -> "LineageOS"
                display.contains("/e/") || brand.contains("e_os") -> "/e/OS"
                manufacturer == "samsung" -> "Samsung One UI"
                manufacturer == "xiaomi" -> "Xiaomi MIUI"
                manufacturer == "oneplus" -> "OnePlus OxygenOS"
                manufacturer == "oppo" || manufacturer == "realme" -> "ColorOS"
                manufacturer == "google" -> "Stock Android"
                else -> "Android ${Build.VERSION.RELEASE}"
            }
        }

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
                    // Package not present – keep checking
                }
            }
            return false
        }

        private fun PrivacyScoreCalculator.ScoreRating.toShortLabel(): String = when (this) {
            PrivacyScoreCalculator.ScoreRating.EXCELLENT -> "Excellent"
            PrivacyScoreCalculator.ScoreRating.GOOD -> "Good"
            PrivacyScoreCalculator.ScoreRating.FAIR -> "Fair"
            PrivacyScoreCalculator.ScoreRating.POOR -> "Poor"
            PrivacyScoreCalculator.ScoreRating.CRITICAL -> "Critical"
        }
    }
}
