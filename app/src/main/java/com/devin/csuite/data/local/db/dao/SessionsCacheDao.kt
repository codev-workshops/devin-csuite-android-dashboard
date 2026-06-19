package com.devin.csuite.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.devin.csuite.data.local.db.entity.CachedSession

@Dao
interface SessionsCacheDao {

    @Upsert
    suspend fun upsertSession(session: CachedSession)

    @Upsert
    suspend fun upsertSessions(sessions: List<CachedSession>)

    @Query("SELECT * FROM cached_sessions ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun getRecentSessions(limit: Int = 100): List<CachedSession>

    @Query("SELECT * FROM cached_sessions WHERE sessionId = :id")
    suspend fun getSession(id: String): CachedSession?

    @Query("SELECT * FROM cached_sessions WHERE status = :status ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun getSessionsByStatus(status: String, limit: Int = 100): List<CachedSession>

    @Query("SELECT COUNT(*) FROM cached_sessions WHERE status = 'running'")
    suspend fun getActiveSessionCount(): Int

    @Query("DELETE FROM cached_sessions")
    suspend fun clearAll()

    @Query("DELETE FROM cached_sessions WHERE cachedAt < :expiryTime")
    suspend fun deleteExpired(expiryTime: Long)
}
