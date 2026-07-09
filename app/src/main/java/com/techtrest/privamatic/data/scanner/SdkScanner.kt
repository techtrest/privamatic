package com.techtrest.privamatic.data.scanner

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.techtrest.privamatic.data.model.AppSdkFindings
import com.techtrest.privamatic.data.model.TrackerCategory
import com.techtrest.privamatic.data.model.TrackerSdk
import com.techtrest.privamatic.data.util.PackageManagerUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Fingerprints tracker SDKs embedded in installed apps by matching manifest
 * component class names against code signatures from the Exodus Privacy
 * tracker database (bundled as an asset — fully offline).
 */
class SdkScanner(private val context: Context) {

    private data class TrackerDefinition(
        val name: String,
        val signatures: List<String>,
        val categories: List<TrackerCategory>
    )

    /**
     * Scans all user-installed apps for known tracker SDKs.
     * Runs on a background dispatcher; safe to call from the main thread.
     */
    suspend fun scan(trustedPackages: Set<String>): List<AppSdkFindings> =
        withContext(Dispatchers.Default) {
            val trackers = loadTrackerDefinitions()
            val packageManager = context.packageManager

            packageManager.getInstalledPackages(0)
                .asSequence()
                .mapNotNull { it.packageName }
                .filter { it != context.packageName }
                .filter { it !in trustedPackages }
                .filter { !PackageManagerUtil.isSystemApp(packageManager, it) }
                .mapNotNull { packageName -> scanApp(packageManager, packageName, trackers) }
                .sortedWith(compareByDescending<AppSdkFindings> { it.trackers.size }.thenBy { it.appName })
                .toList()
        }

    private fun scanApp(
        packageManager: PackageManager,
        packageName: String,
        trackers: List<TrackerDefinition>
    ): AppSdkFindings? {
        val components = getComponentClassNames(packageManager, packageName)
        if (components.isEmpty()) return null

        val matched = trackers.filter { tracker ->
            // Suppress a company's own SDK inside their own app
            // (e.g. the Facebook SDK inside com.facebook.katana)
            val isOwnSdk = tracker.signatures.any { packageName.startsWith(it) }
            !isOwnSdk && tracker.signatures.any { signature ->
                components.any { it.startsWith(signature) }
            }
        }
        if (matched.isEmpty()) return null

        return AppSdkFindings(
            packageName = packageName,
            appName = PackageManagerUtil.getAppName(packageManager, packageName),
            trackers = matched
                .map { TrackerSdk(name = it.name, categories = it.categories) }
                .sortedBy { it.name }
        )
    }

    /**
     * Collects the class names of all manifest-declared components. Queried
     * per package (not in one bulk call) to stay under the binder
     * transaction size limit; unresolvable packages are skipped.
     */
    private fun getComponentClassNames(
        packageManager: PackageManager,
        packageName: String
    ): List<String> {
        val info: PackageInfo = try {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SERVICES or PackageManager.GET_RECEIVERS or
                    PackageManager.GET_PROVIDERS or PackageManager.GET_ACTIVITIES
            )
        } catch (_: Exception) {
            return emptyList()
        }

        val components = ArrayList<String>()
        info.services?.mapTo(components) { it.name }
        info.receivers?.mapTo(components) { it.name }
        info.providers?.mapTo(components) { it.name }
        info.activities?.mapTo(components) { it.name }
        return components
    }

    private fun loadTrackerDefinitions(): List<TrackerDefinition> {
        val json = context.assets.open(TRACKERS_ASSET).bufferedReader().use { it.readText() }
        val trackersObject = JSONObject(json).getJSONObject("trackers")

        val definitions = ArrayList<TrackerDefinition>()
        for (key in trackersObject.keys()) {
            val tracker = trackersObject.getJSONObject(key)

            val signatures = tracker.optString("code_signature")
                .split('|')
                .map { it.trim() }
                .filter {
                    it.length >= MIN_SIGNATURE_LENGTH &&
                        it.count { c -> c == '.' } >= MIN_SIGNATURE_DOTS &&
                        !it.startsWith(".")
                }
            if (signatures.isEmpty()) continue

            val categoriesArray = tracker.optJSONArray("categories")
            val categories = ArrayList<TrackerCategory>()
            if (categoriesArray != null) {
                for (i in 0 until categoriesArray.length()) {
                    TrackerCategory.fromJsonName(categoriesArray.getString(i))?.let { categories.add(it) }
                }
            }
            // Crash reporting tools (Sentry, Bugsnag, Crashlytics) are
            // legitimate developer tooling, not surveillance — exclude them
            if (TrackerCategory.CRASH_REPORTING in categories) continue

            definitions.add(
                TrackerDefinition(
                    name = tracker.getString("name"),
                    signatures = signatures,
                    categories = categories
                )
            )
        }
        return definitions
    }

    companion object {
        private const val TRACKERS_ASSET = "trackers.json"
        private const val MIN_SIGNATURE_LENGTH = 8
        private const val MIN_SIGNATURE_DOTS = 1
    }
}
