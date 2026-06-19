package com.devin.csuite.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.notificationPrefsStore: DataStore<Preferences> by preferencesDataStore(name = "notification_prefs")

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ACU_OVERAGE = "acu_overage"
        const val CHANNEL_ERROR_SPIKE = "error_spike"
        const val CHANNEL_GUARDRAIL = "guardrail_violation"

        private const val NOTIFICATION_ID_ACU = 1001
        private const val NOTIFICATION_ID_ERROR = 1002
        private const val NOTIFICATION_ID_GUARDRAIL = 1003
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val channels = listOf(
            NotificationChannel(
                CHANNEL_ACU_OVERAGE,
                "ACU Overage Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when ACU usage exceeds thresholds"
            },
            NotificationChannel(
                CHANNEL_ERROR_SPIKE,
                "Error Spike Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when session error rate spikes"
            },
            NotificationChannel(
                CHANNEL_GUARDRAIL,
                "Guardrail Violations",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts for guardrail policy violations"
            }
        )

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        channels.forEach { notificationManager.createNotificationChannel(it) }
    }

    fun showAcuOverageNotification(title: String, message: String) {
        showNotification(CHANNEL_ACU_OVERAGE, NOTIFICATION_ID_ACU, title, message)
    }

    fun showErrorSpikeNotification(title: String, message: String) {
        showNotification(CHANNEL_ERROR_SPIKE, NOTIFICATION_ID_ERROR, title, message)
    }

    fun showGuardrailNotification(title: String, message: String) {
        showNotification(CHANNEL_GUARDRAIL, NOTIFICATION_ID_GUARDRAIL, title, message)
    }

    private fun showNotification(channelId: String, notificationId: Int, title: String, message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    fun isNotificationEnabledFlow(channelId: String): Flow<Boolean> {
        val key = booleanPreferencesKey("notification_$channelId")
        return context.notificationPrefsStore.data.map { prefs ->
            prefs[key] ?: true
        }
    }

    suspend fun isNotificationEnabled(channelId: String): Boolean {
        val key = booleanPreferencesKey("notification_$channelId")
        return context.notificationPrefsStore.data.first()[key] ?: true
    }

    suspend fun setNotificationEnabled(channelId: String, enabled: Boolean) {
        val key = booleanPreferencesKey("notification_$channelId")
        context.notificationPrefsStore.edit { prefs ->
            prefs[key] = enabled
        }
    }
}
