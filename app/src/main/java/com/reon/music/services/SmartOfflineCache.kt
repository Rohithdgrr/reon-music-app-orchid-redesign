/*
 * REON Music App - Smart Offline Cache Manager
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 * 
 * Intelligently caches songs that users have listened to completely
 * Ensures 100+ songs are available offline after listening
 */

package com.reon.music.services

import android.content.Context
import android.util.Log
import com.reon.music.core.model.Song
import com.reon.music.data.database.dao.SongDao
import com.reon.music.data.database.entities.DownloadState
import com.reon.music.data.database.entities.SongEntity
import com.reon.music.data.network.StreamResolver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Smart Offline Cache Manager
 * Automatically caches songs that users have listened to completely
 * Maintains a minimum of 100 songs offline
 */
@Singleton
class SmartOfflineCache @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songDao: SongDao,
    private val cacheManager: CacheManager,
    private val streamResolver: StreamResolver,
    private val downloadManager: DownloadManager
) {
    companion object {
        private const val TAG = "SmartOfflineCache"
        private const val MIN_OFFLINE_SONGS = 100
        private const val COMPLETION_THRESHOLD = 0.9f // 90% completion = fully listened
        private const val CACHE_PRIORITY_DAYS = 30 // Keep cached songs for 30 days
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _offlineSongCount = MutableStateFlow(0)
    val offlineSongCount: StateFlow<Int> = _offlineSongCount.asStateFlow()
    
    private val _isCaching = MutableStateFlow(false)
    val isCaching: StateFlow<Boolean> = _isCaching.asStateFlow()
    
    init {
        scope.launch { updateOfflineCount() }
    }
    
    /**
     * Track song playback progress and cache when completed
     */
    fun trackPlayback(song: Song, currentPosition: Long, duration: Long) {
        if (duration <= 0) return
        
        val completionRatio = currentPosition.toFloat() / duration
        
        // If song is 90%+ complete, mark for caching
        if (completionRatio >= COMPLETION_THRESHOLD) {
            scope.launch {
                markSongForCaching(song)
            }
        }
    }
    
    /**
     * Mark song as fully listened and cache it
     */
    suspend fun markSongCompleted(song: Song) = withContext(Dispatchers.IO) {
        try {
            // Update play count and last played
            val existing = songDao.getSongById(song.id)
            if (existing != null) {
                songDao.incrementPlayCount(song.id, System.currentTimeMillis())
            } else {
                songDao.insert(SongEntity.fromSong(song))
            }
            
            // Mark for offline caching
            markSongForCaching(song)
            
            Log.d(TAG, "Marked song as completed: ${song.title}")
        } catch (e: Exception) {
            Log.e(TAG, "Error marking song as completed", e)
        }
    }
    
    /**
     * Mark song for caching (if not already cached)
     */
    private suspend fun markSongForCaching(song: Song) = withContext(Dispatchers.IO) {
        try {
            // Check if already cached
            val cachedFile = cacheManager.getCachedAudio(song.id)
            if (cachedFile != null && cachedFile.exists()) {
                // Already cached, update priority
                updateCachePriority(song.id)
                return@withContext
            }
            
            // Check if already downloaded
            val existing = songDao.getSongById(song.id)
            if (existing?.downloadState == DownloadState.DOWNLOADED) {
                return@withContext
            }
            
            // Queue for background caching
            scope.launch {
                cacheSongInBackground(song)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error marking song for caching", e)
        }
    }
    
    /**
     * Cache song in background
     */
    private suspend fun cacheSongInBackground(song: Song) = withContext(Dispatchers.IO) {
        try {
            _isCaching.value = true
            
            // Resolve stream URL
            val streamUrl = streamResolver.resolveStreamUrl(song) ?: run {
                Log.w(TAG, "Could not resolve stream URL for ${song.title}")
                return@withContext
            }
            
            // Download and cache
            downloadManager.downloadSong(song)
            
            // Update database - Mark as AUTO_CACHED, not DOWNLOADED
            // This ensures auto-cached songs don't appear in the Downloads screen
            val existing = songDao.getSongById(song.id)
            if (existing != null && existing.downloadState != DownloadState.DOWNLOADED) {
                // Only update if not already user-downloaded
                songDao.update(
                    existing.copy(
                        downloadState = DownloadState.AUTO_CACHED,
                        lastPlayedAt = System.currentTimeMillis()
                    )
                )
            }
            
            updateOfflineCount()
            Log.d(TAG, "Cached song: ${song.title}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error caching song in background", e)
        } finally {
            _isCaching.value = false
        }
    }
    
    /**
     * Ensure minimum offline songs are available
     */
    suspend fun ensureMinimumOfflineSongs() = withContext(Dispatchers.IO) {
        try {
            val currentCount = _offlineSongCount.value
            
            if (currentCount < MIN_OFFLINE_SONGS) {
                Log.d(TAG, "Offline songs below threshold ($currentCount/$MIN_OFFLINE_SONGS), caching more...")
                
                // Get most played songs that aren't cached
                val mostPlayed = songDao.getMostPlayed(200).first()
                val uncachedSongs = mostPlayed
                    .filter { it.downloadState != DownloadState.DOWNLOADED }
                    .mapNotNull { it.toSong() }
                    .take(MIN_OFFLINE_SONGS - currentCount)
                
                // Cache them in background
                uncachedSongs.forEach { song ->
                    scope.launch {
                        cacheSongInBackground(song)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error ensuring minimum offline songs", e)
        }
    }
    
    /**
     * Cache recently played songs for offline
     */
    suspend fun cacheRecentlyPlayed(count: Int = 50) = withContext(Dispatchers.IO) {
        try {
            val recentlyPlayed = songDao.getRecentlyPlayed(count).first()
            val uncachedSongs = recentlyPlayed
                .filter { it.downloadState != DownloadState.DOWNLOADED }
                .mapNotNull { it.toSong() }
            
            uncachedSongs.forEach { song ->
                scope.launch {
                    cacheSongInBackground(song)
                }
            }
            
            Log.d(TAG, "Caching ${uncachedSongs.size} recently played songs")
        } catch (e: Exception) {
            Log.e(TAG, "Error caching recently played songs", e)
        }
    }
    
    /**
     * Cache entire playlist/chart for offline
     */
    suspend fun cachePlaylist(songs: List<Song>) = withContext(Dispatchers.IO) {
        try {
            songs.forEach { song ->
                scope.launch {
                    cacheSongInBackground(song)
                }
            }
            Log.d(TAG, "Caching playlist with ${songs.size} songs")
        } catch (e: Exception) {
            Log.e(TAG, "Error caching playlist", e)
        }
    }
    
    /**
     * Update cache priority (touch file to update last modified)
     */
    private fun updateCachePriority(songId: String) {
        val cachedFile = cacheManager.getCachedAudio(songId)
        cachedFile?.setLastModified(System.currentTimeMillis())
    }
    
    /**
     * Update offline song count
     */
    suspend fun updateOfflineCount() = withContext(Dispatchers.IO) {
        try {
            val cachedSongs = songDao.getDownloadedSongs().first()
            _offlineSongCount.value = cachedSongs.size
        } catch (e: Exception) {
            Log.e(TAG, "Error updating offline count", e)
        }
    }
    
    /**
     * Get offline songs
     */
    suspend fun getOfflineSongs(): List<Song> = withContext(Dispatchers.IO) {
        try {
            songDao.getDownloadedSongs().first()
                .mapNotNull { it.toSong() }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting offline songs", e)
            emptyList()
        }
    }
    
    /**
     * Clean old cached songs (keep only recent ones)
     */
    suspend fun cleanOldCachedSongs() = withContext(Dispatchers.IO) {
        try {
            val cutoffTime = System.currentTimeMillis() - (CACHE_PRIORITY_DAYS * 24 * 60 * 60 * 1000L)
            
            val allCached = songDao.getDownloadedSongs().first()
            val oldCached = allCached.filter { 
                it.lastPlayedAt != null && it.lastPlayedAt!! < cutoffTime 
            }
            
            // Remove old cached songs (but keep at least MIN_OFFLINE_SONGS)
            if (allCached.size > MIN_OFFLINE_SONGS) {
                oldCached.take(allCached.size - MIN_OFFLINE_SONGS).forEach { song ->
                    val cachedFile = cacheManager.getCachedAudio(song.id)
                    cachedFile?.delete()
                    
                    songDao.update(
                        song.copy(
                            downloadState = DownloadState.NOT_DOWNLOADED,
                            localPath = null
                        )
                    )
                }
                
                updateOfflineCount()
                Log.d(TAG, "Cleaned ${oldCached.size} old cached songs")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning old cached songs", e)
        }
    }
}

