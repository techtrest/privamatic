package com.techtrest.privamatic.data.util

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

/**
 * Shared helpers for resolving package metadata via the [PackageManager].
 * Centralises the duplicated app-name / system-app lookups used by the scanners.
 */
object PackageManagerUtil {

    /**
     * Resolve the user-facing label for a package, falling back to the package
     * name itself when the app cannot be resolved.
     */
    fun getAppName(packageManager: PackageManager, packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            packageName
        }
    }

    /**
     * Whether the given package is a system app. On failure to resolve, assumes
     * a system app to avoid false positives.
     */
    fun isSystemApp(packageManager: PackageManager, packageName: String): Boolean {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        } catch (_: Exception) {
            true
        }
    }
}
