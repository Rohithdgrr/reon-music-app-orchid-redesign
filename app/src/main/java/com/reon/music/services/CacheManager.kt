/*
 * REON Music App - Cache Manager
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 */

package com.reon.music.services

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class CacheInfo(
    val totalSizeBytes: Long = 0,
    val audioCacheSizeBytes: Long = 0,
    val imageCacheSizeBytes: Long = 0,
    val lyricsCacheSizeBytes: Long = 0,
    val otherCacheSizeBytes: Long = 0
) {
    val totalSizeMB: Float get() = totalSizeBytes / (1024f * 1024f)
    
    fun formattedSize(): String {
        val sizeMB = totalSizeMB
        return when {
            sizeMB >= 1024 -> "%.1f GB".format(sizeMB / 1024)
            sizeMB >= 1 -> "%.1f MB".format(sizeMB)
            else -> "${(totalSizeBytes / 1024)} KB"
        }
    }
}

/**
 * Cache Manager
 * Manages audio, image, and data caching
 */
@Singleton
class CacheManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "CacheManager"
        private const val AUDIO_CACHE_DIR = "audio_cache"
        private const val IMAGE_CACHE_DIR = "image_cache"
        private const val LYRICS_CACHE_DIR = "lyrics_cache"
        
        // Default max cache sizes
        const val DEFAULT_AUDIO_CACHE_MB = 500
        const val DEFAULT_IMAGE_CACHE_MB = 100
        const val DEFAULT_MAX_CACHE_AGE_DAYS = 30
    }
    
    private val _cacheInfo = MutableStateFlow(CacheInfo())
    val cacheInfo: StateFlow<CacheInfo> = _cacheInfo.asStateFlow()
    
    private var maxAudioCacheMB: Int = DEFAULT_AUDIO_CACHE_MB
    private var maxImageCacheMB: Int = DEFAULT_IMAGE_CACHE_MB
    private var maxCacheAgeDays: Int = DEFAULT_MAX_CACHE_AGE_DAYS
    
    /**
     * Get cache directories
     */
    val audioCacheDir: File
        get() = File(context.cacheDir, AUDIO_CACHE_DIR).also { it.mkdirs() }
    
    val imageCacheDir: File
        get() = File(context.cacheDir, IMAGE_CACHE_DIR).also { it.mkdirs() }
    
    val lyricsCacheDir: File
        get() = File(context.cacheDir, LYRICS_CACHE_DIR).also { it.mkdirs() }
    
    /**
     * Calculate and update cache info
     */
    suspend fun updateCacheInfo() = withContext(Dispatchers.IO) {
        try {
            val audioSize = calculateDirSize(audioCacheDir)
            val imageSize = calculateDirSize(imageCacheDir)
            val lyricsSize = calculateDirSize(lyricsCacheDir)
            val otherSize = calculateDirSize(context.cacheDir) - audioSize - imageSize - lyricsSize
            
            _cacheInfo.value = CacheInfo(
                totalSizeBytes = audioSize + imageSize + lyricsSize + otherSize,
                audioCacheSizeBytes = audioSize,
                imageCacheSizeBytes = imageSize,
                lyricsCacheSizeBytes = lyricsSize,
                otherCacheSizeBytes = otherSize.coerceAtLeast(0)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating cache size", e)
        }
    }
    
    /**
     * Clear all cache
     */
    suspend fun clearAllCache() = withContext(Dispatchers.IO) {
        try {
            clearAudioCache()
            clearImageCache()
            clearLyricsCache()
            context.cacheDir.deleteRecursively()
            context.cacheDir.mkdirs()
            updateCacheInfo()
            Log.d(TAG, "All cache cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cache", e)
        }
    }
    
    /**
     * Clear audio cache
     */
    suspend fun clearAudioCache() = withContext(Dispatchers.IO) {
        try {
            audioCacheDir.deleteRecursively()
            audioCacheDir.mkdirs()
            updateCacheInfo()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing audio cache", e)
        }
    }
    
    /**
     * Clear image cache
     */
    suspend fun clearImageCache() = withContext(Dispatchers.IO) {
        try {
            imageCacheDir.deleteRecursively()
            imageCacheDir.mkdirs()
            updateCacheInfo()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing image cache", e)
        }
    }
    
    /**
     * Clear lyrics cache
     */
    suspend fun clearLyricsCache() = withContext(Dispatchers.IO) {
        try {
            lyricsCacheDir.deleteRecursively()
            lyricsCacheDir.mkdirs()
            updateCacheInfo()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing lyrics cache", e)
        }
    }
    
    /**
     * Clean old cache files
     */
    suspend fun cleanOldCache() = withContext(Dispatchers.IO) {
        try {
            val cutoffTime = System.currentTimeMillis() - (maxCacheAgeDays * 24 * 60 * 60 * 1000L)
            
            cleanOldFiles(audioCacheDir, cutoffTime)
            cleanOldFiles(imageCacheDir, cutoffTime)
            cleanOldFiles(lyricsCacheDir, cutoffTime)
            
            updateCacheInfo()
            Log.d(TAG, "Old cache cleaned")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning old cache", e)
        }
    }
    
    /**
     * Trim cache to size limit
     */
    suspend fun trimCacheToLimit() = withContext(Dispatchers.IO) {
        try {
            trimDirToSize(audioCacheDir, maxAudioCacheMB * 1024 * 1024L)
            trimDirToSize(imageCacheDir, maxImageCacheMB * 1024 * 1024L)
            updateCacheInfo()
        } catch (e: Exception) {
            Log.e(TAG, "Error trimming cache", e)
        }
    }
    
    /**
     * Set max cache sizes
     */
    fun setMaxCacheSize(audioMB: Int, imageMB: Int) {
        maxAudioCacheMB = audioMB
        maxImageCacheMB = imageMB
    }
    
    /**
     * Set max cache age
     */
    fun setMaxCacheAge(days: Int) {
        maxCacheAgeDays = days
    }
    
    /**
     * Get cached audio file for a song
     */
    fun getCachedAudio(songId: String): File? {
        val file = File(audioCacheDir, "${songId}.mp3")
        return if (file.exists()) file else null
    }
    
    /**
     * Save audio to cache
     */
    suspend fun cacheAudio(songId: String, data: ByteArray) = withContext(Dispatchers.IO) {
        try {
            val file = File(audioCacheDir, "${songId}.mp3")
            file.writeBytes(data)
            trimCacheToLimit()
        } catch (e: Exception) {
            Log.e(TAG, "Error caching audio", e)
        }
    }
    
    private fun calculateDirSize(dir: File): Long {
        if (!dir.exists()) return 0
        return dir.walkTopDown()
            .filter { it.isFile }
            .map { it.length() }
            .sum()
    }
    
    private fun cleanOldFiles(dir: File, cutoffTime: Long) {
        if (!dir.exists()) return
        dir.listFiles()?.forEach { file ->
            if (file.isFile && file.lastModified() < cutoffTime) {
                file.delete()
            }
        }
    }
    
    private fun trimDirToSize(dir: File, maxSizeBytes: Long) {
        if (!dir.exists()) return
        
        val files = dir.listFiles()?.filter { it.isFile }?.toMutableList() ?: return
        files.sortBy { it.lastModified() } // Oldest first
        
        var currentSize = files.sumOf { it.length() }
        
        while (currentSize > maxSizeBytes && files.isNotEmpty()) {
            val file = files.removeAt(0)
            currentSize -= file.length()
            file.delete()
        }
    }
}
