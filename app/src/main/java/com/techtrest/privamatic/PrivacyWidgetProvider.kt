package com.techtrest.privamatic

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import com.techtrest.privamatic.BuildConfig
import android.widget.RemoteViews
import android.widget.TextView
import com.techtrest.privamatic.data.ScoreHistoryRepository
import com.techtrest.privamatic.data.TrustedAppsAdjuster
import com.techtrest.privamatic.data.TrustedAppsRepository
import com.techtrest.privamatic.data.util.DeviceNameUtil
import com.techtrest.privamatic.data.scanner.PrivacyScanner
import com.techtrest.privamatic.data.scanner.PrivacyScoreCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
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
        if (BuildConfig.DEBUG) Log.d(TAG, "Received ${intent.action}")
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
        if (BuildConfig.DEBUG) Log.d(TAG, "Widget update: running a scan")
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val scanner = PrivacyScanner(context)
                val rawScore = scanner.performCompleteScan()
                val trusted = TrustedAppsRepository(context).trustedPackages.first()
                val privacyScore = TrustedAppsAdjuster.computeAdjustedScore(rawScore, trusted)
                val score = privacyScore.score
                val rating = PrivacyScoreCalculator.getScoreRating(score)
                val deviceName = DeviceNameUtil.getMarketingName()
                val osName = detectOperatingSystem(context)
                val ratingText = rating.toShortLabel(context)

                // Record score history and calculate delta
                val scoreHistoryRepository = ScoreHistoryRepository(context)
                val scoreHistory = scoreHistoryRepository.recordScore(score)
                val scoreDelta = scoreHistory.scoreDelta
                val darkText = noBackgroundUsesDarkText(context)
                val showPrivacyChange = scoreDelta != null &&
                        scoreDelta != 0 &&
                        scoreHistory.deltaFirstAppearedTimestamp != 0L &&
                        (System.currentTimeMillis() - scoreHistory.deltaFirstAppearedTimestamp) <= CHANGE_EXPIRY_MS
                // Debug builds only: a device check can force the change state; always null in release.
                val forcedDelta = WidgetDebugOverride.forcedDelta(context)

                for (appWidgetId in appWidgetIds) {
                    updateWidget(
                        context = context,
                        appWidgetManager = appWidgetManager,
                        appWidgetId = appWidgetId,
                        deviceName = deviceName,
                        osName = osName,
                        scoreText = score.toString(),
                        rating = rating,
                        ratingText = ratingText,
                        scoreDelta = forcedDelta ?: scoreDelta,
                        showDelta = if (forcedDelta != null) forcedDelta != 0 else showPrivacyChange,
                        darkText = darkText
                    )
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.e(TAG, "Error updating widget", e)
                val deviceName = DeviceNameUtil.getMarketingName()
                val osName = detectOperatingSystem(context)
                for (appWidgetId in appWidgetIds) {
                    updateWidget(
                        context = context,
                        appWidgetManager = appWidgetManager,
                        appWidgetId = appWidgetId,
                        deviceName = deviceName,
                        osName = osName,
                        scoreText = "--",
                        rating = null,
                        ratingText = "—",
                        scoreDelta = null,
                        showDelta = false,
                        darkText = noBackgroundUsesDarkText(context)
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
        rating: PrivacyScoreCalculator.ScoreRating?,
        ratingText: String,
        scoreDelta: Int?,
        showDelta: Boolean,
        darkText: Boolean
    ) {
        // Stored values below the card minimum draw as the minimum; 0 is the no-background style.
        val opacity = WidgetStyle.effectiveOpacity(WidgetPreferences.getOpacity(context, appWidgetId))
        val noBackground = opacity == WidgetStyle.NO_BACKGROUND
        val layout = when {
            !noBackground -> R.layout.widget_privacy
            darkText -> R.layout.widget_privacy_no_background_dark_text
            else -> R.layout.widget_privacy_no_background
        }
        val views = RemoteViews(context.packageName, layout)

        // Debug builds only: a device check can force a long name; always null in release.
        views.setTextViewText(R.id.widget_device_name, WidgetDebugOverride.forcedDeviceName(context) ?: deviceName)
        views.setTextViewText(R.id.widget_os_name, osName)
        views.setTextViewText(R.id.widget_score_value, scoreText)
        views.setTextViewText(R.id.widget_score_rating, ratingText)

        // While a change is live it takes the shield's place. The shield goes invisible, not gone,
        // so the sides keep their anchor. Both states are set every time: the launcher reapplies
        // an update onto the views it already shows.
        val isIncrease = scoreDelta != null && scoreDelta > 0
        if (showDelta && scoreDelta != null && scoreDelta != 0) {
            val changeText = context.getString(
                if (isIncrease) R.string.fmt_widget_delta_up else R.string.fmt_widget_delta_down,
                abs(scoreDelta)
            )
            val textSize = centreChangeTextSize(context, layout, changeText)
            views.setTextViewText(R.id.widget_centre_change, changeText)
            views.setTextViewTextSize(R.id.widget_centre_change, TypedValue.COMPLEX_UNIT_PX, textSize)
            views.setContentDescription(
                R.id.widget_centre_change,
                context.resources.getQuantityString(
                    if (isIncrease) R.plurals.plural_cd_widget_score_rose else R.plurals.plural_cd_widget_score_dropped,
                    abs(scoreDelta),
                    abs(scoreDelta)
                )
            )
            views.setViewVisibility(R.id.widget_centre_change, View.VISIBLE)
            views.setViewVisibility(R.id.widget_divider, View.INVISIBLE)
        } else {
            views.setViewVisibility(R.id.widget_centre_change, View.GONE)
            views.setViewVisibility(R.id.widget_divider, View.VISIBLE)
        }
        val scoreColors = rating?.let { WidgetStyle.scoreColors(it) }
        val deltaColors = WidgetStyle.deltaColors(isIncrease)

        when {
            // No card: the layout's own text and shadow. Dark text on a light wallpaper takes the
            // light-theme tints, light text the light-on-dark ones.
            noBackground -> {
                scoreColors?.let {
                    views.setTextColor(R.id.widget_score_value, if (darkText) it.first else it.second)
                }
                views.setTextColor(R.id.widget_centre_change, if (darkText) deltaColors.first else deltaColors.second)
            }
            // API 31+: opacity on the background layer only. Colours come as light/dark pairs so
            // the launcher picks the right one when the theme changes, without a new update.
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                views.setFloat(R.id.widget_background, "setAlpha", opacity / 100f)
                scoreColors?.let {
                    views.setColorInt(R.id.widget_score_value, "setTextColor", it.first, it.second)
                }
                views.setColorInt(R.id.widget_centre_change, "setTextColor", deltaColors.first, deltaColors.second)
            }
            // On Android 11 and below (API < 31), android:theme on non-root RemoteViews
            // views is not properly supported and can cause RemoteViews inflation failure
            // on some builds (e.g. LineageOS on older hardware), showing "Problem loading
            // widget". The android:theme attribute has been removed from the base layout,
            // and here we explicitly apply static brand colours (Cream text) so the widget
            // is always readable regardless of the device's theme engine or colour resolution.
            // The card is dark in both themes, so the tints use the light-on-dark values.
            else -> {
                views.setInt(R.id.widget_background, "setImageAlpha", ((opacity / 100f) * 255).toInt())
                val cream = Cream.toArgb()
                views.setTextColor(R.id.widget_device_name, cream)
                views.setTextColor(R.id.widget_os_name, cream)
                views.setTextColor(R.id.widget_score_value, scoreColors?.second ?: cream)
                views.setTextColor(R.id.widget_score_rating, cream)
                views.setTextColor(R.id.widget_centre_change, deltaColors.second)
            }
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
        const val ACTION_UPDATE_WIDGET = "com.techtrest.privamatic.ACTION_UPDATE_WIDGET"

        /** Broadcast action sent by AlarmManager at 23:50 to record a daily snapshot. */
        const val ACTION_MIDNIGHT_SNAPSHOT = "com.techtrest.privamatic.ACTION_MIDNIGHT_SNAPSHOT"

        private const val ALARM_REQUEST_CODE = 1001

        /** Smallest size the centre change shrinks to. */
        private const val CENTRE_CHANGE_MIN_TEXT_DP = 8f

        /** How far each fitting step shrinks the centre change. */
        private const val CENTRE_CHANGE_STEP_DP = 0.5f

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
            if (BuildConfig.DEBUG) Log.d(TAG, "Periodic widget updates scheduled (6-hour interval)")
        }

        /**
         * Brings the alarm back after a force-stop cleared it. Only a reboot restored it before.
         * Does nothing when no widget is placed. The manager is null on devices without widgets.
         */
        fun rearmPeriodicUpdatesIfPlaced(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(
                ComponentName(context, PrivacyWidgetProvider::class.java)
            )
            if (ids.isNotEmpty()) schedulePeriodicUpdates(context)
        }

        /** Cancels the repeating alarm set by [schedulePeriodicUpdates]. */
        fun cancelPeriodicUpdates(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(buildAlarmPendingIntent(context))
            if (BuildConfig.DEBUG) Log.d(TAG, "Periodic widget updates cancelled")
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
                else -> context.getString(R.string.fmt_android_version, Build.VERSION.RELEASE)
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

        /**
         * Whether the no-background style uses dark text, read from the home wallpaper's colour
         * hints like Pixel Launcher's labels. No broadcast reaches a manifest receiver when the
         * wallpaper changes (ACTION_WALLPAPER_CHANGED is not exempt from the implicit-broadcast
         * limits), so a new wallpaper shows from the next widget update.
         */
        private fun noBackgroundUsesDarkText(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
            val hints = try {
                WallpaperManager.getInstance(context)
                    .getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                    ?.colorHints
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.w(TAG, "Wallpaper colours unavailable", e)
                null
            }
            return WidgetStyle.noBackgroundUsesDarkText(Build.VERSION.SDK_INT, hints)
        }

        /**
         * The largest size, up to the layout's own 18sp at this font scale, at which [text] fits the
         * shield's box on one line. It measures the layout's own TextView (halo padding included),
         * inflated from the plain context: on the Pixel 8 that measured "↓12" exactly as the launcher
         * drew it, while a Theme.DeviceDefault wrapper picked a narrower typeface (98 against 116 px).
         */
        private fun centreChangeTextSize(context: Context, layout: Int, text: String): Float {
            val root = LayoutInflater.from(context).inflate(layout, null)
            val slot = root.findViewById<View>(R.id.widget_divider).layoutParams
            val change = root.findViewById<TextView>(R.id.widget_centre_change)
            change.text = text
            val density = context.resources.displayMetrics.density
            val min = CENTRE_CHANGE_MIN_TEXT_DP * density
            var size = change.textSize
            while (true) {
                change.setTextSize(TypedValue.COMPLEX_UNIT_PX, size)
                change.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
                val fits = change.measuredWidth <= slot.width && change.measuredHeight <= slot.height
                if (fits || size - CENTRE_CHANGE_STEP_DP * density < min) {
                    if (BuildConfig.DEBUG) {
                        Log.d(
                            TAG,
                            "Centre change \"$text\" at ${size}px, measured " +
                                "${change.measuredWidth}x${change.measuredHeight} in ${slot.width}x${slot.height}"
                        )
                    }
                    return size
                }
                size -= CENTRE_CHANGE_STEP_DP * density
            }
        }

        private fun PrivacyScoreCalculator.ScoreRating.toShortLabel(context: Context): String = context.getString(this.displayName)
    }
}
