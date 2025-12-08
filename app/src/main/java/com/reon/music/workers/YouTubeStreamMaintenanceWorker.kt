/*
 * REON Music App - YouTube Stream Maintenance Worker
 * Copyright (c) 2024 REON
 * Periodic background maintenance for stream URL caching
 */

package com.reon.music.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.reon.music.data.repository.MusicRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background worker for YouTube stream cache maintenance
 * Runs periodically to:
 * 1. Refresh URLs that will expire soon
 * 2. Clean up expired cache entries
 * 3. Optimize cache performance
 */
@HiltWorker
class YouTubeStreamMaintenanceWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: MusicRepository
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            // Step 1: Refresh URLs that will expire soon (within 30 mins)
            repository.refreshExpiringUrls()
            
            // Step 2: Clean up expired cache entries
            repository.cleanupExpiredCache()
            
            // Step 3: Get statistics for logging
            val stats = repository.getStreamCacheStats()
            
            // Log success with stats
            android.util.Log.d(TAG, "Stream cache maintenance completed: $stats")
            
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Stream cache maintenance failed", e)
            
            // Retry on failure
            if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
    
    companion object {
        private const val TAG = "StreamMaintenance"
        private const val MAX_RETRIES = 3
        const val WORK_NAME = "youtube_stream_maintenance"
    }
}
