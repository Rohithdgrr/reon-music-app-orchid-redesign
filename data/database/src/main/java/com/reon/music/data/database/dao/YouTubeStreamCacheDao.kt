/*
 * REON Music App - YouTube Stream Cache DAO
 * Copyright (c) 2024 REON
 * Clean-room implementation
 */

package com.reon.music.data.database.dao

import androidx.room.*
import com.reon.music.data.database.entities.YouTubeStreamCacheEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for YouTube stream URL caching
 */
@Dao
interface YouTubeStreamCacheDao {
    
    /**
     * Get cached stream URL for a video
     */
    @Query("SELECT * FROM youtube_stream_cache WHERE videoId = :videoId LIMIT 1")
    suspend fun getStreamCache(videoId: String): YouTubeStreamCacheEntity?
    
    /**
     * Get cached stream URL as Flow (reactive)
     */
    @Query("SELECT * FROM youtube_stream_cache WHERE videoId = :videoId LIMIT 1")
    fun getStreamCacheFlow(videoId: String): Flow<YouTubeStreamCacheEntity?>
    
    /**
     * Get all valid (non-expired) cache entries
     */
    @Query("SELECT * FROM youtube_stream_cache WHERE expiresAt > :currentTime")
    suspend fun getValidCacheEntries(currentTime: Long = System.currentTimeMillis()): List<YouTubeStreamCacheEntity>
    
    /**
     * Get cache entries that will expire soon (within specified minutes)
     */
    @Query("""
        SELECT * FROM youtube_stream_cache 
        WHERE expiresAt > :currentTime 
        AND expiresAt <= :soonTime
    """)
    suspend fun getExpiringSoon(
        currentTime: Long = System.currentTimeMillis(),
        soonTime: Long = System.currentTimeMillis() + (30 * 60 * 1000) // 30 minutes
    ): List<YouTubeStreamCacheEntity>
    
    /**
     * Insert or update stream cache
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreamCache(cache: YouTubeStreamCacheEntity)
    
    /**
     * Insert multiple cache entries
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreamCaches(caches: List<YouTubeStreamCacheEntity>)
    
    /**
     * Update access tracking
     */
    @Query("""
        UPDATE youtube_stream_cache 
        SET lastAccessedAt = :timestamp, 
            accessCount = accessCount + 1 
        WHERE videoId = :videoId
    """)
    suspend fun updateAccess(videoId: String, timestamp: Long = System.currentTimeMillis())
    
    /**
     * Delete expired cache entries
     */
    @Query("DELETE FROM youtube_stream_cache WHERE expiresAt <= :currentTime")
    suspend fun deleteExpired(currentTime: Long = System.currentTimeMillis()): Int
    
    /**
     * Delete specific cache entry
     */
    @Query("DELETE FROM youtube_stream_cache WHERE videoId = :videoId")
    suspend fun deleteStreamCache(videoId: String)
    
    /**
     * Delete all cache entries
     */
    @Query("DELETE FROM youtube_stream_cache")
    suspend fun deleteAll()
    
    /**
     * Get cache statistics
     */
    @Query("""
        SELECT 
            COUNT(*) as total,
            SUM(CASE WHEN expiresAt > :currentTime THEN 1 ELSE 0 END) as valid,
            SUM(CASE WHEN expiresAt <= :currentTime THEN 1 ELSE 0 END) as expired
        FROM youtube_stream_cache
    """)
    suspend fun getCacheStats(currentTime: Long = System.currentTimeMillis()): CacheStats
    
    /**
     * Get most accessed cache entries
     */
    @Query("SELECT * FROM youtube_stream_cache ORDER BY accessCount DESC LIMIT :limit")
    suspend fun getMostAccessed(limit: Int = 10): List<YouTubeStreamCacheEntity>
    
    /**
     * Get recently accessed cache entries
     */
    @Query("SELECT * FROM youtube_stream_cache ORDER BY lastAccessedAt DESC LIMIT :limit")
    suspend fun getRecentlyAccessed(limit: Int = 20): List<YouTubeStreamCacheEntity>
}

/**
 * Cache statistics data class
 */
data class CacheStats(
    val total: Int = 0,
    val valid: Int = 0,
    val expired: Int = 0
) {
    val hitRate: Float
        get() = if (total > 0) (valid.toFloat() / total.toFloat()) else 0f
}
