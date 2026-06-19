package com.devin.csuite.data.local.datasource

import com.devin.csuite.data.local.db.AppDatabase
import com.devin.csuite.data.local.db.dao.BillingCacheDao
import com.devin.csuite.data.local.db.dao.MetricsCacheDao
import com.devin.csuite.data.local.db.entity.CachedBillingCycle
import com.devin.csuite.data.local.db.entity.CachedMetric
import com.devin.csuite.data.local.db.entity.CacheMetadata
import com.devin.csuite.domain.model.AcuLimitsResponse
import com.devin.csuite.domain.model.ActiveUsersResponse
import com.devin.csuite.domain.model.BillingCycle
import com.devin.csuite.domain.model.BillingCyclesResponse
import com.devin.csuite.domain.model.DailyConsumptionResponse
import com.devin.csuite.domain.model.DauMetricsResponse
import com.devin.csuite.domain.model.MauMetricsResponse
import com.devin.csuite.domain.model.OrganizationsResponse
import com.devin.csuite.domain.model.PrMetricsResponse
import com.devin.csuite.domain.model.SearchMetricsResponse
import com.devin.csuite.domain.model.SessionMetricsResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalMetricsDataSource @Inject constructor(
    private val metricsCacheDao: MetricsCacheDao,
    private val billingCacheDao: BillingCacheDao,
    private val json: Json
) {
    companion object {
        private const val KEY_ORGANIZATIONS = "organizations"
        private const val KEY_MAU = "mau_metrics"
        private const val KEY_DAU = "dau_metrics"
        private const val KEY_PR = "pr_metrics"
        private const val KEY_SESSION_METRICS = "session_metrics"
        private const val KEY_SEARCH = "search_metrics"
        private const val KEY_ACTIVE_USERS = "active_users"
        private const val KEY_ACU_LIMITS = "acu_limits"
        private const val KEY_DAILY_CONSUMPTION = "daily_consumption"
    }

    suspend fun getOrganizations(): OrganizationsResponse? = getMetric(KEY_ORGANIZATIONS)
    suspend fun saveOrganizations(data: OrganizationsResponse) = saveMetric(KEY_ORGANIZATIONS, data)

    suspend fun getMauMetrics(): MauMetricsResponse? = getMetric(KEY_MAU)
    suspend fun saveMauMetrics(data: MauMetricsResponse) = saveMetric(KEY_MAU, data)

    suspend fun getDauMetrics(): DauMetricsResponse? = getMetric(KEY_DAU)
    suspend fun saveDauMetrics(data: DauMetricsResponse) = saveMetric(KEY_DAU, data)

    suspend fun getPrMetrics(): PrMetricsResponse? = getMetric(KEY_PR)
    suspend fun savePrMetrics(data: PrMetricsResponse) = saveMetric(KEY_PR, data)

    suspend fun getSessionMetrics(): SessionMetricsResponse? = getMetric(KEY_SESSION_METRICS)
    suspend fun saveSessionMetrics(data: SessionMetricsResponse) = saveMetric(KEY_SESSION_METRICS, data)

    suspend fun getSearchMetrics(): SearchMetricsResponse? = getMetric(KEY_SEARCH)
    suspend fun saveSearchMetrics(data: SearchMetricsResponse) = saveMetric(KEY_SEARCH, data)

    suspend fun getActiveUsers(): ActiveUsersResponse? = getMetric(KEY_ACTIVE_USERS)
    suspend fun saveActiveUsers(data: ActiveUsersResponse) = saveMetric(KEY_ACTIVE_USERS, data)

    suspend fun getAcuLimits(): AcuLimitsResponse? = getMetric(KEY_ACU_LIMITS)
    suspend fun saveAcuLimits(data: AcuLimitsResponse) = saveMetric(KEY_ACU_LIMITS, data)

    suspend fun getDailyConsumption(): DailyConsumptionResponse? = getMetric(KEY_DAILY_CONSUMPTION)
    suspend fun saveDailyConsumption(data: DailyConsumptionResponse) = saveMetric(KEY_DAILY_CONSUMPTION, data)

    suspend fun getBillingCycles(): BillingCyclesResponse? {
        val cycles = billingCacheDao.getAllCycles()
        if (cycles.isEmpty()) return null
        val current = billingCacheDao.getCurrentCycle()
        return BillingCyclesResponse(
            cycles = cycles.map { it.toDomainModel() },
            currentCycle = current?.toDomainModel()
        )
    }

    suspend fun saveBillingCycles(data: BillingCyclesResponse) {
        val entities = data.cycles.mapIndexed { index, cycle ->
            CachedBillingCycle(
                cycleKey = "cycle_${cycle.cycleStart}_${cycle.cycleEnd}",
                cycleStart = cycle.cycleStart,
                cycleEnd = cycle.cycleEnd,
                acusUsed = cycle.acusUsed,
                acuLimit = cycle.acuLimit,
                status = cycle.status,
                isCurrent = data.currentCycle?.cycleStart == cycle.cycleStart
            )
        }
        billingCacheDao.upsertCycles(entities)
        if (data.currentCycle != null && entities.none { it.isCurrent }) {
            val currentEntity = CachedBillingCycle(
                cycleKey = "cycle_current",
                cycleStart = data.currentCycle.cycleStart,
                cycleEnd = data.currentCycle.cycleEnd,
                acusUsed = data.currentCycle.acusUsed,
                acuLimit = data.currentCycle.acuLimit,
                status = data.currentCycle.status,
                isCurrent = true
            )
            billingCacheDao.upsertCycle(currentEntity)
        }
        updateMetadata(KEY_DAILY_CONSUMPTION)
    }

    suspend fun getLastUpdated(key: String): Long? {
        return metricsCacheDao.getMetadata(key)?.lastUpdated
    }

    suspend fun clearAll() {
        metricsCacheDao.clearAll()
        metricsCacheDao.clearAllMetadata()
        billingCacheDao.clearAll()
    }

    suspend fun evictExpired() {
        val expiryTime = System.currentTimeMillis() - AppDatabase.CACHE_EXPIRY_MS
        metricsCacheDao.deleteExpired(expiryTime)
        billingCacheDao.deleteExpired(expiryTime)
    }

    private suspend inline fun <reified T> getMetric(key: String): T? {
        val cached = metricsCacheDao.getMetric(key) ?: return null
        val expiryTime = System.currentTimeMillis() - AppDatabase.CACHE_EXPIRY_MS
        if (cached.cachedAt < expiryTime) {
            metricsCacheDao.deleteMetric(key)
            return null
        }
        return try {
            json.decodeFromString<T>(cached.jsonData)
        } catch (_: Exception) {
            null
        }
    }

    private suspend inline fun <reified T> saveMetric(key: String, data: T) {
        val jsonString = json.encodeToString(data)
        metricsCacheDao.upsertMetric(CachedMetric(metricKey = key, jsonData = jsonString))
        updateMetadata(key)
    }

    private suspend fun updateMetadata(key: String) {
        metricsCacheDao.upsertMetadata(CacheMetadata(cacheKey = key))
    }

    private fun CachedBillingCycle.toDomainModel() = BillingCycle(
        cycleStart = cycleStart,
        cycleEnd = cycleEnd,
        acusUsed = acusUsed,
        acuLimit = acuLimit,
        status = status
    )
}
