package com.devin.csuite.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_metrics")
data class CachedMetric(
    @PrimaryKey val metricKey: String,
    val jsonData: String,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_sessions")
data class CachedSession(
    @PrimaryKey val sessionId: String,
    val title: String?,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
    val acusConsumed: Double,
    val userEmail: String?,
    val origin: String?,
    val tagsJson: String,
    val pullRequestsJson: String,
    val url: String?,
    val orgId: String?,
    val playbookId: String?,
    val statusEnum: String?,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_billing_cycles")
data class CachedBillingCycle(
    @PrimaryKey val cycleKey: String,
    val cycleStart: String,
    val cycleEnd: String,
    val acusUsed: Double,
    val acuLimit: Double,
    val status: String,
    val isCurrent: Boolean = false,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_audit_logs")
data class CachedAuditLog(
    @PrimaryKey val id: String,
    val actorName: String?,
    val actorEmail: String,
    val action: String,
    val resourceType: String,
    val resourceId: String,
    val timestamp: Long,
    val details: String?,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cache_metadata")
data class CacheMetadata(
    @PrimaryKey val cacheKey: String,
    val lastUpdated: Long = System.currentTimeMillis()
)
