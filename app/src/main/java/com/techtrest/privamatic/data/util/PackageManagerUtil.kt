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

    /**
     * Whether microG is installed, detected by the presence of any of its companion
     * packages. microG itself installs under Google's package name via signature
     * spoofing, so [MICROG_SPOOFED_PACKAGE] alone proves nothing — the companions do.
     *
     * This lookup hits the PackageManager; call it once per scan, not per package.
     */
    fun isMicroGInstalled(packageManager: PackageManager): Boolean {
        return MICROG_COMPANION_PACKAGES.any { pkg ->
            try {
                packageManager.getApplicationInfo(pkg, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    /**
     * Whether [packageName] is served by microG rather than by Google. Takes the
     * already-resolved [isMicroGInstalled] result so callers can evaluate microG
     * presence once and still check many packages cheaply.
     */
    fun isMicroGPackage(packageName: String, isMicroGInstalled: Boolean): Boolean =
        isMicroGInstalled && packageName == MICROG_SPOOFED_PACKAGE

    /** The Google package name microG installs itself under. */
    private const val MICROG_SPOOFED_PACKAGE = "com.google.android.gms"

    private val MICROG_COMPANION_PACKAGES = listOf(
        "org.microg.gms.self",       // microG Settings — most reliable, present in all standard builds
        "org.microg.gms.droidguard", // SafetyNet module — optional
        "org.microg.nlp"             // Network location provider — older builds
    )
}
