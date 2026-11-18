package com.techtrest.privamatic

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * Debug builds only: the shell's door to [WidgetDebugOverride], since the widget provider is not
 * exported. Exported here on purpose, so another app can trigger a scan in a debug build; release
 * builds contain neither this class nor its manifest entry.
 */
class WidgetDebugReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
            ComponentName(context, PrivacyWidgetProvider::class.java)
        )
        WidgetDebugOverride.applyExtras(context, intent, ids)
        PrivacyWidgetProvider.requestImmediateUpdate(context)
    }
}
