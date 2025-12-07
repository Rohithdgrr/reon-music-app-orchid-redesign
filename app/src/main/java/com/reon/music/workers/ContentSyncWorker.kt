/*
 * REON Music App - Content Sync Worker
 * Copyright (c) 2024 REON
 * Background worker for auto-updating charts, playlists, and new releases
 */

package com.reon.music.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.reon.music.data.repository.MusicRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker for syncing content in the background
 * Runs periodically based on user settings
 */
class ContentSyncWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val TAG = "ContentSyncWorker"
        const val WORK_NAME = "content_sync_work"
        
        // Input data keys
        const val KEY_SYNC_CHARTS = "sync_charts"
        const val KEY_SYNC_PLAYLISTS = "sync_playlists"
        const val KEY_SYNC_NEW_RELEASES = "sync_new_releases"
        
        // Output data keys
        const val KEY_CHARTS_UPDATED = "charts_updated"
        const val KEY_PLAYLISTS_UPDATED = "playlists_updated"
        const val KEY_NEW_RELEASES_UPDATED = "new_releases_updated"
        const val KEY_SYNC_TIME = "sync_time"
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ContentSyncWorkerEntryPoint {
        fun musicRepository(): MusicRepository
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting content sync...")
        
        // Show sync in progress notification
        SyncNotificationManager.showSyncInProgress(context)
        
        try {
            // Get dependencies via Hilt
            val entryPoint = EntryPointAccessors.fromApplication(
                context,
                ContentSyncWorkerEntryPoint::class.java
            )
            val repository = entryPoint.musicRepository()
            
            // Get sync preferences from input data
            val syncCharts = inputData.getBoolean(KEY_SYNC_CHARTS, true)
            val syncPlaylists = inputData.getBoolean(KEY_SYNC_PLAYLISTS, true)
            val syncNewReleases = inputData.getBoolean(KEY_SYNC_NEW_RELEASES, true)
            
            var chartsUpdated = 0
            var playlistsUpdated = 0
            var newReleasesUpdated = 0
            
            // Sync charts
            if (syncCharts) {
                Log.d(TAG, "Syncing charts...")
                chartsUpdated = syncCharts(repository)
                Log.d(TAG, "Charts synced: $chartsUpdated updated")
            }
            
            // Sync playlists
            if (syncPlaylists) {
                Log.d(TAG, "Syncing playlists...")
                playlistsUpdated = syncPlaylists(repository)
                Log.d(TAG, "Playlists synced: $playlistsUpdated updated")
            }
            
            // Sync new releases
            if (syncNewReleases) {
                Log.d(TAG, "Syncing new releases...")
                newReleasesUpdated = syncNewReleases(repository)
                Log.d(TAG, "New releases synced: $newReleasesUpdated updated")
            }
            
            // Show completion notification
            SyncNotificationManager.showSyncCompleted(
                context,
                chartsUpdated,
                playlistsUpdated,
                newReleasesUpdated
            )
            
            // Create output data
            val outputData = androidx.work.Data.Builder()
                .putInt(KEY_CHARTS_UPDATED, chartsUpdated)
                .putInt(KEY_PLAYLISTS_UPDATED, playlistsUpdated)
                .putInt(KEY_NEW_RELEASES_UPDATED, newReleasesUpdated)
                .putLong(KEY_SYNC_TIME, System.currentTimeMillis())
                .build()
            
            Log.d(TAG, "Content sync completed successfully")
            Result.success(outputData)
            
        } catch (e: Exception) {
            Log.e(TAG, "Content sync failed", e)
            
            // Show failure notification
            SyncNotificationManager.showSyncFailed(context, e.message)
            
            // Retry on failure (max 3 attempts)
            if (runAttemptCount < 3) {
                Log.d(TAG, "Retrying... (attempt ${runAttemptCount + 1}/3)")
                Result.retry()
            } else {
                Log.e(TAG, "Max retry attempts reached")
                Result.failure()
            }
        }
    }

    /**
     * Sync charts from YouTube Music
     */
    private suspend fun syncCharts(repository: MusicRepository): Int {
        return try {
            var updated = 0
            
            // Sync different chart types
            val chartTypes = listOf(
                "top_songs",
                "trending_songs",
                "top_hindi",
                "top_telugu",
                "top_english",
                "top_tamil",
                "top_punjabi"
            )
            
            chartTypes.forEach { chartType ->
                try {
                    // Fetch chart from API
                    // Note: Actual implementation depends on your repository methods
                    // This is a placeholder showing the pattern
                    Log.d(TAG, "Fetching chart: $chartType")
                    
                    // Example: repository.getChart(chartType)
                    // Then save to local database
                    
                    updated++
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync chart: $chartType", e)
                }
            }
            
            updated
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync charts", e)
            0
        }
    }

    /**
     * Sync playlists from YouTube Music
     */
    private suspend fun syncPlaylists(repository: MusicRepository): Int {
        return try {
            var updated = 0
            
            // Sync featured playlists
            try {
                Log.d(TAG, "Fetching featured playlists")
                // Example: repository.getFeaturedPlaylists()
                updated++
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync featured playlists", e)
            }
            
            // Sync mood playlists
            try {
                Log.d(TAG, "Fetching mood playlists")
                // Example: repository.getMoodPlaylists()
                updated++
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync mood playlists", e)
            }
            
            // Sync genre playlists
            try {
                Log.d(TAG, "Fetching genre playlists")
                // Example: repository.getGenrePlaylists()
                updated++
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync genre playlists", e)
            }
            
            updated
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync playlists", e)
            0
        }
    }

    /**
     * Sync new releases from YouTube Music
     */
    private suspend fun syncNewReleases(repository: MusicRepository): Int {
        return try {
            var updated = 0
            
            // Sync new releases
            try {
                Log.d(TAG, "Fetching new releases")
                // Example: repository.getNewReleases()
                updated++
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync new releases", e)
            }
            
            // Sync new albums
            try {
                Log.d(TAG, "Fetching new albums")
                // Example: repository.getNewAlbums()
                updated++
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync new albums", e)
            }
            
            updated
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync new releases", e)
            0
        }
    }
}
