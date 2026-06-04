package com.techtrest.privamatic.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.trustedAppsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "trusted_apps_prefs"
)

class TrustedAppsRepository(private val context: Context) {

    private val TRUSTED_PACKAGES_KEY = stringSetPreferencesKey("trusted_packages")
    private val APPS_BANNER_DISMISSED_KEY = booleanPreferencesKey("apps_banner_dismissed")

    val trustedPackages: Flow<Set<String>> = context.trustedAppsDataStore.data
        .map { it[TRUSTED_PACKAGES_KEY] ?: emptySet() }

    val isAppsBannerDismissed: Flow<Boolean> = context.trustedAppsDataStore.data
        .map { it[APPS_BANNER_DISMISSED_KEY] ?: false }

    suspend fun trustApp(packageName: String) {
        context.trustedAppsDataStore.edit { prefs ->
            prefs[TRUSTED_PACKAGES_KEY] = (prefs[TRUSTED_PACKAGES_KEY] ?: emptySet()) + packageName
        }
    }

    suspend fun untrustApp(packageName: String) {
        context.trustedAppsDataStore.edit { prefs ->
            prefs[TRUSTED_PACKAGES_KEY] = (prefs[TRUSTED_PACKAGES_KEY] ?: emptySet()) - packageName
        }
    }

    suspend fun dismissAppsBanner() {
        context.trustedAppsDataStore.edit { prefs ->
            prefs[APPS_BANNER_DISMISSED_KEY] = true
        }
    }
}
