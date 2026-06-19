package com.devin.csuite.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.devin.csuite.data.local.SecureKeyStore
import com.devin.csuite.data.remote.EnterpriseApi
import com.devin.csuite.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val api: EnterpriseApi,
    private val secureKeyStore: SecureKeyStore,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (!secureKeyStore.hasApiKey()) return Result.success()

        return try {
            checkAcuThresholds()
            checkErrorRate()
            checkGuardrailViolations()
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private suspend fun checkAcuThresholds() {
        if (!notificationHelper.isNotificationEnabled(NotificationHelper.CHANNEL_ACU_OVERAGE)) return

        val response = api.getBillingCycles()
        if (!response.isSuccessful) return

        val body = response.body() ?: return
        val current = body.currentCycle ?: body.cycles.firstOrNull() ?: return
        if (current.acuLimit <= 0) return

        val usagePercent = (current.acusUsed / current.acuLimit) * 100
        when {
            usagePercent > 95 -> notificationHelper.showAcuOverageNotification(
                "Critical: ACU usage at ${String.format("%.0f", usagePercent)}%",
                "Your enterprise has consumed ${String.format("%.0f", current.acusUsed)} of ${String.format("%.0f", current.acuLimit)} ACUs."
            )
            usagePercent > 80 -> notificationHelper.showAcuOverageNotification(
                "Warning: ACU usage at ${String.format("%.0f", usagePercent)}%",
                "Approaching ACU limit. Consider reviewing usage."
            )
        }
    }

    private suspend fun checkErrorRate() {
        if (!notificationHelper.isNotificationEnabled(NotificationHelper.CHANNEL_ERROR_SPIKE)) return

        val response = api.getSessionMetrics()
        if (!response.isSuccessful) return

        val body = response.body() ?: return
        if (body.totalSessions <= 0) return

        val sessionsResponse = api.getSessions(limit = 100, status = "error")
        if (!sessionsResponse.isSuccessful) return
        val errorCount = sessionsResponse.body()?.totalCount ?: 0
        val errorRate = (errorCount.toDouble() / body.totalSessions) * 100

        if (errorRate > 15) {
            notificationHelper.showErrorSpikeNotification(
                "Session Error Spike: ${String.format("%.0f", errorRate)}%",
                "$errorCount of ${body.totalSessions} sessions have errors."
            )
        }
    }

    private suspend fun checkGuardrailViolations() {
        if (!notificationHelper.isNotificationEnabled(NotificationHelper.CHANNEL_GUARDRAIL)) return
        // Guardrail violations use beta API - gracefully skip if unavailable
    }

    companion object {
        private const val WORK_NAME = "notification_polling"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<NotificationWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
