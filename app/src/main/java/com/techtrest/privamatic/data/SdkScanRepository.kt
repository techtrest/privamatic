package com.techtrest.privamatic.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.techtrest.privamatic.data.model.AppSdkFindings
import com.techtrest.privamatic.data.model.SdkScanResult
import com.techtrest.privamatic.data.model.TrackerCategory
import com.techtrest.privamatic.data.model.TrackerSdk
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

private val Context.sdkScanDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "sdk_scan_prefs"
)

/**
 * Caches the most recent SDK fingerprinting scan so results are available
 * immediately when the SDK tab is opened, without re-scanning.
 */
class SdkScanRepository(private val context: Context) {

    private val SCAN_RESULT_KEY = stringPreferencesKey("sdk_scan_result")

    suspend fun save(result: SdkScanResult) {
        val json = serialize(result)
        context.sdkScanDataStore.edit { prefs ->
            prefs[SCAN_RESULT_KEY] = json
        }
    }

    suspend fun load(): SdkScanResult? {
        val json = context.sdkScanDataStore.data.first()[SCAN_RESULT_KEY] ?: return null
        return try {
            deserialize(json)
        } catch (_: Exception) {
            null
        }
    }

    private fun serialize(result: SdkScanResult): String {
        val appsArray = JSONArray()
        result.findings.forEach { app ->
            val trackersArray = JSONArray()
            app.trackers.forEach { tracker ->
                trackersArray.put(
                    JSONObject()
                        .put("name", tracker.name)
                        .put("categories", JSONArray(tracker.categories.map { it.name }))
                )
            }
            appsArray.put(
                JSONObject()
                    .put("packageName", app.packageName)
                    .put("appName", app.appName)
                    .put("trackers", trackersArray)
            )
        }
        return JSONObject()
            .put("timestamp", result.timestamp)
            .put("apps", appsArray)
            .toString()
    }

    private fun deserialize(json: String): SdkScanResult {
        val root = JSONObject(json)
        val appsArray = root.getJSONArray("apps")
        val findings = ArrayList<AppSdkFindings>(appsArray.length())
        for (i in 0 until appsArray.length()) {
            val app = appsArray.getJSONObject(i)
            val trackersArray = app.getJSONArray("trackers")
            val trackers = ArrayList<TrackerSdk>(trackersArray.length())
            for (j in 0 until trackersArray.length()) {
                val tracker = trackersArray.getJSONObject(j)
                val categoriesArray = tracker.getJSONArray("categories")
                val categories = ArrayList<TrackerCategory>(categoriesArray.length())
                for (k in 0 until categoriesArray.length()) {
                    // Ignore category names from older app versions that no longer exist
                    runCatching { TrackerCategory.valueOf(categoriesArray.getString(k)) }
                        .getOrNull()?.let { categories.add(it) }
                }
                trackers.add(TrackerSdk(name = tracker.getString("name"), categories = categories))
            }
            findings.add(
                AppSdkFindings(
                    packageName = app.getString("packageName"),
                    appName = app.getString("appName"),
                    trackers = trackers
                )
            )
        }
        return SdkScanResult(timestamp = root.getLong("timestamp"), findings = findings)
    }
}
