package com.techtrest.privamatic.data.util

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class PackageManagerUtilTest {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private companion object {
        const val GMS = "com.google.android.gms"
        const val PHOTOS = "com.google.android.apps.photos"
    }

    /** android.jar stubs throw on construction, so build the PM data objects as mocks with fields set. */
    private fun activity(className: String): ActivityInfo =
        mock(ActivityInfo::class.java).apply { name = className }

    private fun packageManagerWithGmsActivities(vararg classNames: String): PackageManager {
        val info = mock(PackageInfo::class.java).apply {
            activities = classNames.map { activity(it) }.toTypedArray()
        }
        val pm = mock(PackageManager::class.java)
        `when`(pm.getPackageInfo(GMS, PackageManager.GET_ACTIVITIES)).thenReturn(info)
        return pm
    }

    /** A package under Google Photos' name requesting [permissions], with or without a launcher activity. */
    private fun packageManagerWithPhotos(
        permissions: Array<String>?,
        hasLauncher: Boolean
    ): PackageManager {
        val info = mock(PackageInfo::class.java).apply { requestedPermissions = permissions }
        val pm = mock(PackageManager::class.java)
        `when`(pm.getPackageInfo(PHOTOS, PackageManager.GET_PERMISSIONS)).thenReturn(info)
        `when`(pm.getLaunchIntentForPackage(PHOTOS))
            .thenReturn(if (hasLauncher) mock(Intent::class.java) else null)
        return pm
    }

    // -------------------------------------------------------------------------
    // isMicroGInstalled
    // -------------------------------------------------------------------------

    @Test
    fun `microG detected when gms package declares an org microg activity`() {
        val pm = packageManagerWithGmsActivities(
            "org.microg.gms.ui.SettingsActivity",
            "org.microg.gms.ui.SelfCheckActivity"
        )
        assertTrue(PackageManagerUtil.isMicroGInstalled(pm))
    }

    @Test
    fun `microG detected even when only one org microg component sits among google ones`() {
        val pm = packageManagerWithGmsActivities(
            "com.google.android.gms.app.settings.GoogleSettingsActivity",
            "org.microg.gms.ui.SettingsActivity"
        )
        assertTrue(PackageManagerUtil.isMicroGInstalled(pm))
    }

    @Test
    fun `real Play Services with no org microg components is not microG`() {
        val pm = packageManagerWithGmsActivities(
            "com.google.android.gms.app.settings.GoogleSettingsActivity",
            "com.google.android.gms.auth.uiflows.common.LoginActivity",
            "com.google.android.gms.ads.AdActivity"
        )
        assertFalse(PackageManagerUtil.isMicroGInstalled(pm))
    }

    @Test
    fun `gms package with no activities at all is not microG`() {
        val info = mock(PackageInfo::class.java) // activities stays null
        val pm = mock(PackageManager::class.java)
        `when`(pm.getPackageInfo(GMS, PackageManager.GET_ACTIVITIES)).thenReturn(info)
        assertFalse(PackageManagerUtil.isMicroGInstalled(pm))
    }

    @Test
    fun `gms package not installed falls through to not microG`() {
        val pm = mock(PackageManager::class.java)
        `when`(pm.getPackageInfo(GMS, PackageManager.GET_ACTIVITIES))
            .thenThrow(PackageManager.NameNotFoundException(GMS))
        assertFalse(PackageManagerUtil.isMicroGInstalled(pm))
    }

    @Test
    fun `package manager failure degrades to not microG rather than crashing the scan`() {
        val pm = mock(PackageManager::class.java)
        `when`(pm.getPackageInfo(GMS, PackageManager.GET_ACTIVITIES))
            .thenThrow(RuntimeException("binder transaction too large"))
        assertFalse(PackageManagerUtil.isMicroGInstalled(pm))
    }

    // -------------------------------------------------------------------------
    // isMicroGPackage — the flag consumed by the #11 trust-toggle bypass
    // -------------------------------------------------------------------------

    @Test
    fun `isMicroGPackage only claims the gms package and only when microG is present`() {
        assertTrue(PackageManagerUtil.isMicroGPackage(GMS, isMicroGInstalled = true))
        assertFalse(PackageManagerUtil.isMicroGPackage(GMS, isMicroGInstalled = false))
        assertFalse(PackageManagerUtil.isMicroGPackage("com.google.android.gsf", isMicroGInstalled = true))
    }

    // -------------------------------------------------------------------------
    // googlePhotosShimReason / isGooglePhotosShim (#20)
    // -------------------------------------------------------------------------

    @Test
    fun `real Google Photos requesting INTERNET with a launcher is genuine`() {
        val pm = packageManagerWithPhotos(
            permissions = arrayOf(
                Manifest.permission.INTERNET,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_MEDIA_IMAGES
            ),
            hasLauncher = true
        )
        assertNull(PackageManagerUtil.googlePhotosShimReason(pm))
        assertFalse(PackageManagerUtil.isGooglePhotosShim(pm))
    }

    @Test
    fun `Gcam Services Provider photosonly flavor requests nothing and is a shim`() {
        // photosonly ships no <uses-permission> at all and a Theme.NoDisplay activity
        val pm = packageManagerWithPhotos(permissions = null, hasLauncher = false)
        assertEquals(
            "does not request android.permission.INTERNET",
            PackageManagerUtil.googlePhotosShimReason(pm)
        )
        assertTrue(PackageManagerUtil.isGooglePhotosShim(pm))
    }

    @Test
    fun `CalyxOS GCamPhotosPreview requesting only QUERY_ALL_PACKAGES is a shim`() {
        val pm = packageManagerWithPhotos(
            permissions = arrayOf(Manifest.permission.QUERY_ALL_PACKAGES),
            hasLauncher = false
        )
        assertEquals(
            "does not request android.permission.INTERNET",
            PackageManagerUtil.googlePhotosShimReason(pm)
        )
        assertTrue(PackageManagerUtil.isGooglePhotosShim(pm))
    }

    @Test
    fun `headless package requesting INTERNET is still flagged by the launcher signal`() {
        val pm = packageManagerWithPhotos(
            permissions = arrayOf(Manifest.permission.INTERNET),
            hasLauncher = false
        )
        assertEquals("has no launcher activity", PackageManagerUtil.googlePhotosShimReason(pm))
    }

    @Test
    fun `photos package not installed is not reported as a shim`() {
        val pm = mock(PackageManager::class.java)
        `when`(pm.getPackageInfo(PHOTOS, PackageManager.GET_PERMISSIONS))
            .thenThrow(PackageManager.NameNotFoundException(PHOTOS))
        assertNull(PackageManagerUtil.googlePhotosShimReason(pm))
        assertFalse(PackageManagerUtil.isGooglePhotosShim(pm))
    }

    @Test
    fun `package manager failure degrades to genuine rather than crashing the scan`() {
        val pm = mock(PackageManager::class.java)
        `when`(pm.getPackageInfo(PHOTOS, PackageManager.GET_PERMISSIONS))
            .thenThrow(RuntimeException("binder transaction too large"))
        assertFalse(PackageManagerUtil.isGooglePhotosShim(pm))
    }

    // -------------------------------------------------------------------------
    // isSystemApp: "came with the phone" for the v1.6 hard rule
    // -------------------------------------------------------------------------

    private fun packageManagerWithFlags(pkg: String, appFlags: Int): PackageManager {
        val appInfo = mock(ApplicationInfo::class.java).apply { flags = appFlags }
        val pm = mock(PackageManager::class.java)
        `when`(pm.getApplicationInfo(pkg, 0)).thenReturn(appInfo)
        return pm
    }

    @Test
    fun `app on the system image came with the phone`() {
        val pm = packageManagerWithFlags("com.android.chrome", ApplicationInfo.FLAG_SYSTEM)
        assertTrue(PackageManagerUtil.isSystemApp(pm, "com.android.chrome"))
    }

    @Test
    fun `system app updated from the Play Store still came with the phone`() {
        // Android keeps FLAG_SYSTEM on an updated system app and adds FLAG_UPDATED_SYSTEM_APP.
        val pm = packageManagerWithFlags(
            "com.google.android.gm",
            ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
        )
        assertTrue(PackageManagerUtil.isSystemApp(pm, "com.google.android.gm"))
    }

    @Test
    fun `user-installed app did not come with the phone`() {
        val pm = packageManagerWithFlags("com.facebook.katana", 0)
        assertFalse(PackageManagerUtil.isSystemApp(pm, "com.facebook.katana"))
    }

    @Test
    fun `unresolvable package counts as system so no removal advice is given`() {
        val pm = mock(PackageManager::class.java)
        `when`(pm.getApplicationInfo(anyString(), anyInt()))
            .thenThrow(PackageManager.NameNotFoundException::class.java)
        assertTrue(PackageManagerUtil.isSystemApp(pm, "com.example.gone"))
    }
}
