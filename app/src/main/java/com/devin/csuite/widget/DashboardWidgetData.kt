package com.devin.csuite.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.widgetDataStore: DataStore<Preferences> by preferencesDataStore(name = "widget_data")

object WidgetDataKeys {
    val ACU_USED = doublePreferencesKey("acu_used")
    val ACU_LIMIT = doublePreferencesKey("acu_limit")
    val ACTIVE_SESSIONS = intPreferencesKey("active_sessions")
    val MAU = intPreferencesKey("mau")
    val LAST_UPDATED = longPreferencesKey("last_updated")
}

suspend fun Context.saveWidgetData(
    acuUsed: Double,
    acuLimit: Double,
    activeSessions: Int,
    mau: Int
) {
    widgetDataStore.edit { prefs ->
        prefs[WidgetDataKeys.ACU_USED] = acuUsed
        prefs[WidgetDataKeys.ACU_LIMIT] = acuLimit
        prefs[WidgetDataKeys.ACTIVE_SESSIONS] = activeSessions
        prefs[WidgetDataKeys.MAU] = mau
        prefs[WidgetDataKeys.LAST_UPDATED] = System.currentTimeMillis()
    }
}

data class WidgetData(
    val acuUsed: Double = 0.0,
    val acuLimit: Double = 0.0,
    val activeSessions: Int = 0,
    val mau: Int = 0,
    val lastUpdated: Long = 0
) {
    val acuPercent: Float
        get() = if (acuLimit > 0) (acuUsed / acuLimit * 100).toFloat() else 0f
}

suspend fun Context.getWidgetData(): WidgetData {
    val prefs = widgetDataStore.data.first()
    return WidgetData(
        acuUsed = prefs[WidgetDataKeys.ACU_USED] ?: 0.0,
        acuLimit = prefs[WidgetDataKeys.ACU_LIMIT] ?: 0.0,
        activeSessions = prefs[WidgetDataKeys.ACTIVE_SESSIONS] ?: 0,
        mau = prefs[WidgetDataKeys.MAU] ?: 0,
        lastUpdated = prefs[WidgetDataKeys.LAST_UPDATED] ?: 0
    )
}
