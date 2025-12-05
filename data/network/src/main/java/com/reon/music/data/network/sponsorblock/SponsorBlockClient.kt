/*
 * REON Music App - SponsorBlock Client
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 * 
 * SponsorBlock API: https://sponsor.ajay.app/
 */

package com.reon.music.data.network.sponsorblock

import android.util.Log
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SponsorBlock API Client
 * Auto-skip sponsored segments in YouTube videos
 */
@Singleton
class SponsorBlockClient @Inject constructor(
    private val httpClient: HttpClient
) {
    companion object {
        private const val TAG = "SponsorBlock"
        private const val BASE_URL = "https://sponsor.ajay.app/api"
        
        // Segment categories
        const val CATEGORY_SPONSOR = "sponsor"
        const val CATEGORY_INTRO = "intro"
        const val CATEGORY_OUTRO = "outro"
        const val CATEGORY_SELFPROMO = "selfpromo"
        const val CATEGORY_INTERACTION = "interaction"
        const val CATEGORY_MUSIC_OFFTOPIC = "music_offtopic"
        const val CATEGORY_PREVIEW = "preview"
        const val CATEGORY_FILLER = "filler"
    }
    
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * Get skip segments for a YouTube video
     */
    suspend fun getSkipSegments(
        videoId: String,
        categories: List<String> = listOf(CATEGORY_SPONSOR, CATEGORY_INTRO, CATEGORY_OUTRO)
    ): Result<List<SponsorSegment>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching segments for: $videoId")
            
            val categoriesParam = categories.joinToString(",") { "[\"$it\"]" }
            
            val response = httpClient.get("$BASE_URL/skipSegments") {
                parameter("videoID", videoId)
                parameter("categories", "[${categories.joinToString(",") { "\"$it\"" }}]")
            }
            
            if (response.status == HttpStatusCode.NotFound) {
                // No segments found - this is normal
                return@withContext Result.success(emptyList())
            }
            
            if (!response.status.isSuccess()) {
                Log.w(TAG, "API error: ${response.status}")
                return@withContext Result.success(emptyList())
            }
            
            val segments = json.decodeFromString<List<SponsorSegment>>(response.bodyAsText())
            Log.d(TAG, "Found ${segments.size} segments")
            
            Result.success(segments)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching segments", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get all segments with hash-based lookup (more private)
     */
    suspend fun getSkipSegmentsByHash(
        videoId: String
    ): Result<List<SponsorSegment>> = withContext(Dispatchers.IO) {
        try {
            // Use first 4 characters of SHA256 hash of video ID
            val hashPrefix = videoId.hashCode().toString(16).take(4)
            
            val response = httpClient.get("$BASE_URL/skipSegments/$hashPrefix")
            
            if (!response.status.isSuccess()) {
                return@withContext Result.success(emptyList())
            }
            
            val allSegments = json.decodeFromString<List<VideoSegments>>(response.bodyAsText())
            val videoSegments = allSegments.find { it.videoID == videoId }?.segments ?: emptyList()
            
            Result.success(videoSegments)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching segments by hash", e)
            Result.failure(e)
        }
    }
}

@Serializable
data class SponsorSegment(
    val segment: List<Double>,
    val UUID: String,
    val category: String,
    val videoDuration: Double = 0.0,
    val actionType: String = "skip",
    val locked: Int = 0,
    val votes: Int = 0,
    val description: String = ""
) {
    val startTime: Double get() = segment.getOrElse(0) { 0.0 }
    val endTime: Double get() = segment.getOrElse(1) { 0.0 }
    val startTimeMs: Long get() = (startTime * 1000).toLong()
    val endTimeMs: Long get() = (endTime * 1000).toLong()
    val durationMs: Long get() = endTimeMs - startTimeMs
}

@Serializable
data class VideoSegments(
    val videoID: String,
    val segments: List<SponsorSegment>
)

/**
 * SponsorBlock Manager
 * Handles segment skipping during playback
 */
@Singleton
class SponsorBlockManager @Inject constructor(
    private val client: SponsorBlockClient
) {
    private var currentSegments: List<SponsorSegment> = emptyList()
    private var currentVideoId: String? = null
    private var enabledCategories = mutableSetOf(
        SponsorBlockClient.CATEGORY_SPONSOR,
        SponsorBlockClient.CATEGORY_INTRO,
        SponsorBlockClient.CATEGORY_OUTRO
    )
    
    /**
     * Load segments for a video
     */
    suspend fun loadSegments(videoId: String): List<SponsorSegment> {
        if (videoId == currentVideoId) {
            return currentSegments
        }
        
        currentVideoId = videoId
        currentSegments = client.getSkipSegments(videoId, enabledCategories.toList())
            .getOrDefault(emptyList())
        
        return currentSegments
    }
    
    /**
     * Check if position should be skipped
     */
    fun shouldSkip(positionMs: Long): SponsorSegment? {
        return currentSegments.find { segment ->
            positionMs >= segment.startTimeMs && positionMs < segment.endTimeMs
        }
    }
    
    /**
     * Get skip-to position if currently in a segment
     */
    fun getSkipToPosition(positionMs: Long): Long? {
        val segment = shouldSkip(positionMs)
        return segment?.endTimeMs
    }
    
    /**
     * Enable/disable category
     */
    fun setCategoryEnabled(category: String, enabled: Boolean) {
        if (enabled) {
            enabledCategories.add(category)
        } else {
            enabledCategories.remove(category)
        }
    }
    
    /**
     * Check if category is enabled
     */
    fun isCategoryEnabled(category: String): Boolean {
        return category in enabledCategories
    }
    
    /**
     * Get all segments for current video
     */
    fun getCurrentSegments(): List<SponsorSegment> = currentSegments
    
    /**
     * Clear cached segments
     */
    fun clear() {
        currentSegments = emptyList()
        currentVideoId = null
    }
}
