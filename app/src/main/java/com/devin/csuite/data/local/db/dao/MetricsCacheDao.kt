package com.devin.csuite.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.devin.csuite.data.local.db.entity.CachedMetric
import com.devin.csuite.data.local.db.entity.CacheMetadata

@Dao
interface MetricsCacheDao {

    @Upsert
    suspend fun upsertMetric(metric: CachedMetric)

    @Query("SELECT * FROM cached_metrics WHERE metricKey = :key")
    suspend fun getMetric(key: String): CachedMetric?

    @Query("DELETE FROM cached_metrics WHERE metricKey = :key")
    suspend fun deleteMetric(key: String)

    @Query("DELETE FROM cached_metrics")
    suspend fun clearAll()

    @Query("DELETE FROM cached_metrics WHERE cachedAt < :expiryTime")
    suspend fun deleteExpired(expiryTime: Long)

    @Upsert
    suspend fun upsertMetadata(metadata: CacheMetadata)

    @Query("SELECT * FROM cache_metadata WHERE cacheKey = :key")
    suspend fun getMetadata(key: String): CacheMetadata?

    @Query("DELETE FROM cache_metadata")
    suspend fun clearAllMetadata()
}
