package com.devin.csuite.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_REFRESH_INTERVAL = intPreferencesKey("refresh_interval_minutes")
        private val KEY_SELECTED_ORG = stringPreferencesKey("selected_org_id")
        private val KEY_SELECTED_TIME_RANGE = stringPreferencesKey("selected_time_range")
        private val KEY_TOOLTIP_SHOWN = booleanPreferencesKey("tooltip_tour_shown")
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_THEME_MODE] ?: "dark"
    }

    val refreshInterval: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_REFRESH_INTERVAL] ?: 0
    }

    val selectedOrgId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_SELECTED_ORG]
    }

    val selectedTimeRange: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SELECTED_TIME_RANGE] ?: "30d"
    }

    val tooltipShown: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_TOOLTIP_SHOWN] ?: false
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode
        }
    }

    suspend fun setRefreshInterval(minutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_REFRESH_INTERVAL] = minutes
        }
    }

    suspend fun setSelectedOrg(orgId: String?) {
        context.dataStore.edit { prefs ->
            if (orgId != null) {
                prefs[KEY_SELECTED_ORG] = orgId
            } else {
                prefs.remove(KEY_SELECTED_ORG)
            }
        }
    }

    suspend fun setSelectedTimeRange(range: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SELECTED_TIME_RANGE] = range
        }
    }

    suspend fun setTooltipShown(shown: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TOOLTIP_SHOWN] = shown
        }
    }
}
