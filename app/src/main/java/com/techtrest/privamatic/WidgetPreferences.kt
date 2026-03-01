package com.techtrest.privamatic

import android.content.Context

object WidgetPreferences {

    private const val PREFS_NAME = "widget_preferences"
    private const val KEY_PREFIX = "widget_opacity_"
    private const val DEFAULT_OPACITY = 100f

    fun saveOpacity(context: Context, widgetId: Int, opacity: Float) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putFloat("$KEY_PREFIX$widgetId", opacity)
            .apply()
    }

    fun getOpacity(context: Context, widgetId: Int): Float {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getFloat("$KEY_PREFIX$widgetId", DEFAULT_OPACITY)
    }

    fun deleteOpacity(context: Context, widgetId: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove("$KEY_PREFIX$widgetId")
            .apply()
    }
}
