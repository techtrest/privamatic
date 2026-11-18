package com.techtrest.privamatic

import android.content.Context
import android.content.Intent

/**
 * Debug builds only; release builds compile the no-op in src/release. Lets a device check force the
 * widget's change state through [WidgetDebugReceiver]. The forced value is kept for every update
 * until cleared, so the system's own updates don't replace it.
 *
 * `adb shell am broadcast -n com.techtrest.privamatic.debug/com.techtrest.privamatic.WidgetDebugReceiver`
 * plus one of:
 * - `--ei debug_delta -6`: show "↓6" (0 shows the shield);
 * - `--ez debug_clear true`: back to the real change;
 * - `--ef debug_opacity 0`: store that opacity for every placed widget;
 * - `--es debug_device_name "Galaxy S8"`: show that device name (cleared with `debug_clear`).
 */
internal object WidgetDebugOverride {
    private const val PREFS_NAME = "widget_debug_override"
    private const val EXTRA_DELTA = "debug_delta"
    private const val EXTRA_CLEAR = "debug_clear"
    private const val EXTRA_OPACITY = "debug_opacity"
    private const val EXTRA_DEVICE_NAME = "debug_device_name"

    fun applyExtras(context: Context, intent: Intent, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (intent.getBooleanExtra(EXTRA_CLEAR, false)) prefs.edit().clear().commit()
        if (intent.hasExtra(EXTRA_DELTA)) {
            prefs.edit().putInt(EXTRA_DELTA, intent.getIntExtra(EXTRA_DELTA, 0)).commit()
        }
        intent.getStringExtra(EXTRA_DEVICE_NAME)?.let {
            prefs.edit().putString(EXTRA_DEVICE_NAME, it).commit()
        }
        if (intent.hasExtra(EXTRA_OPACITY)) {
            val opacity = intent.getFloatExtra(EXTRA_OPACITY, WidgetStyle.MAX_OPACITY)
            for (id in appWidgetIds) WidgetPreferences.saveOpacity(context, id, opacity)
        }
    }

    /** The forced change (0 = the shield), or null when none is set. */
    fun forcedDelta(context: Context): Int? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return if (prefs.contains(EXTRA_DELTA)) prefs.getInt(EXTRA_DELTA, 0) else null
    }

    /** The forced device name, or null when none is set. */
    fun forcedDeviceName(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(EXTRA_DEVICE_NAME, null)
}
