package com.devin.csuite.worker

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.devin.csuite.data.local.SecureKeyStore
import com.devin.csuite.data.remote.EnterpriseApi
import com.devin.csuite.widget.MediumDashboardWidget
import com.devin.csuite.widget.SmallDashboardWidget
import com.devin.csuite.widget.saveWidgetData
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class WidgetRefreshWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val api: EnterpriseApi,
    private val secureKeyStore: SecureKeyStore
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (!secureKeyStore.hasApiKey()) return Result.success()

        return try {
            var acuUsed = 0.0
            var acuLimit = 0.0
            var activeSessions = 0
            var mau = 0

            val cyclesResponse = api.getBillingCycles()
            if (cyclesResponse.isSuccessful) {
                cyclesResponse.body()?.let { body ->
                    val current = body.currentCycle ?: body.cycles.firstOrNull()
                    acuUsed = current?.acusUsed ?: 0.0
                    acuLimit = current?.acuLimit ?: 0.0
                }
            }

            val sessionsResponse = api.getSessions(limit = 1, status = "running")
            if (sessionsResponse.isSuccessful) {
                activeSessions = sessionsResponse.body()?.totalCount ?: 0
            }

            val mauResponse = api.getMauMetrics()
            if (mauResponse.isSuccessful) {
                mau = mauResponse.body()?.mau ?: 0
            }

            context.saveWidgetData(acuUsed, acuLimit, activeSessions, mau)

            SmallDashboardWidget().updateAll(context)
            MediumDashboardWidget().updateAll(context)

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "widget_refresh"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(
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
