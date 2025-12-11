/*
 * REON Music App - Smart Cache Manager
 * Copyright (c) 2024 REON
 * Cache expiration, cleanup, and intelligent eviction
 */

package com.reon.music.services

import android.content.Context
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import java.io.File
import java.util.concurrent.TimeUnit
import androidx.work.CoroutineWorker
import com.reon.music.core.optimization.CacheType
import com.reon.music.core.optimization.CacheStats

@Singleton
class SmartCacheManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val IMAGE_CACHE_TTL_DAYS = 7  // Images expire after 7 days
        private const val MAX_CACHE_SIZE_BYTES = 100L * 1024 * 1024  // 100MB
        private const val CACHE_CLEANUP_INTERVAL_HOURS = 6
    }
    
    init {
        scheduleCacheCleanup()
    }
    
    /**
     * Schedule periodic cache cleanup using WorkManager
     */
    private fun scheduleCacheCleanup() {
        val cacheCleanupRequest = PeriodicWorkRequestBuilder<CacheCleanupWorker>(
            CACHE_CLEANUP_INTERVAL_HOURS.toLong(),
            TimeUnit.HOURS
        ).build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "cache_cleanup",
            ExistingPeriodicWorkPolicy.KEEP,
            cacheCleanupRequest
        )
    }
    
    /**
     * Clean cache using LRU (Least Recently Used) strategy
     */
    suspend fun cleanCacheIfNeeded(): Long = withContext(Dispatchers.IO) {
        val cacheDir = context.cacheDir
        val cacheSize = getCacheSizeBytes(cacheDir)
        
        if (cacheSize > MAX_CACHE_SIZE_BYTES) {
            cleanupByLRU(cacheDir, (MAX_CACHE_SIZE_BYTES * 0.75).toLong())
        } else {
            cleanupExpiredFiles(cacheDir)
        }
    }
    
    /**
     * Clean expired files (older than TTL)
     */
    private fun cleanupExpiredFiles(cacheDir: File): Long {
        val now = System.currentTimeMillis()
        val expiredTime = now - TimeUnit.DAYS.toMillis(IMAGE_CACHE_TTL_DAYS.toLong())
        var freedBytes = 0L
        
        cacheDir.walkTopDown().forEach { file ->
            if (file.isFile && file.lastModified() < expiredTime) {
                val fileSize = file.length()
                if (file.delete()) {
                    freedBytes += fileSize
                }
            }
        }
        
        return freedBytes
    }
    
    /**
     * LRU cleanup: Delete oldest files until under target size
     */
    private fun cleanupByLRU(cacheDir: File, targetSize: Long): Long {
        var freedBytes = 0L
        
        // Sort files by last modified time (oldest first)
        val sortedFiles = cacheDir.walkTopDown()
            .filter { it.isFile }
            .sortedBy { it.lastModified() }
        
        for (file in sortedFiles) {
            if (getCacheSizeBytes(cacheDir) <= targetSize) break
            
            val fileSize = file.length()
            if (file.delete()) {
                freedBytes += fileSize
            }
        }
        
        return freedBytes
    }
    
    /**
     * Get total cache size in bytes
     */
    private fun getCacheSizeBytes(dir: File): Long {
        return dir.walkTopDown().fold(0L) { acc, file ->
            acc + (if (file.isFile) file.length() else 0L)
        }
    }
    
    /**
     * Force clear specific cache
     */
    suspend fun clearSpecificCache(cacheType: CacheType): Long = withContext(Dispatchers.IO) {
        val dir = when (cacheType) {
            CacheType.IMAGES -> File(context.cacheDir, "image_cache")
            CacheType.NETWORK -> File(context.cacheDir, "http_cache")
            CacheType.DATABASE -> File(context.cacheDir, "database_cache")
            CacheType.ALL -> context.cacheDir
        }
        
        if (dir.exists()) {
            val size = getCacheSizeBytes(dir)
            dir.deleteRecursively()
            size
        } else {
            0L
        }
    }
    
    /**
     * Get cache statistics
     */
    suspend fun getCacheStats(): CacheStats = withContext(Dispatchers.IO) {
        val imageCacheSize = getCacheSizeBytes(File(context.cacheDir, "image_cache"))
        val httpCacheSize = getCacheSizeBytes(File(context.cacheDir, "http_cache"))
        val databaseCacheSize = getCacheSizeBytes(File(context.cacheDir, "database_cache"))
        val totalCacheSize = getCacheSizeBytes(context.cacheDir)
        
        CacheStats(
            imageCacheMB = imageCacheSize / (1024 * 1024),
            httpCacheMB = httpCacheSize / (1024 * 1024),
            databaseCacheMB = databaseCacheSize / (1024 * 1024),
            totalCacheMB = totalCacheSize / (1024 * 1024),
            percentUsed = (totalCacheSize * 100) / MAX_CACHE_SIZE_BYTES
        )
    }
}

/**
 * WorkManager job for periodic cache cleanup
 */
class CacheCleanupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            val smartCache = SmartCacheManager(applicationContext)
            smartCache.cleanCacheIfNeeded()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
