package com.devin.csuite.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.devin.csuite.data.local.db.dao.BillingCacheDao
import com.devin.csuite.data.local.db.dao.MetricsCacheDao
import com.devin.csuite.data.local.db.dao.SessionsCacheDao
import com.devin.csuite.data.local.db.entity.CachedAuditLog
import com.devin.csuite.data.local.db.entity.CachedBillingCycle
import com.devin.csuite.data.local.db.entity.CachedMetric
import com.devin.csuite.data.local.db.entity.CachedSession
import com.devin.csuite.data.local.db.entity.CacheMetadata

@Database(
    entities = [
        CachedMetric::class,
        CachedSession::class,
        CachedBillingCycle::class,
        CachedAuditLog::class,
        CacheMetadata::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun metricsCacheDao(): MetricsCacheDao
    abstract fun sessionsCacheDao(): SessionsCacheDao
    abstract fun billingCacheDao(): BillingCacheDao

    companion object {
        const val DATABASE_NAME = "csuite_cache.db"
        const val CACHE_EXPIRY_MS = 24 * 60 * 60 * 1000L // 24 hours
    }
}
