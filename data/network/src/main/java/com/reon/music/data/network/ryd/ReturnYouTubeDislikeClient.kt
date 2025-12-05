/*
 * REON Music App - Return YouTube Dislike Client
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 * 
 * Uses Return YouTube Dislike API: https://returnyoutubedislike.com/
 */

package com.reon.music.data.network.ryd

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
 * Return YouTube Dislike API Client
 * Gets like/dislike counts for YouTube videos
 */
@Singleton
class ReturnYouTubeDislikeClient @Inject constructor(
    private val httpClient: HttpClient
) {
    companion object {
        private const val TAG = "RYDClient"
        private const val BASE_URL = "https://returnyoutubedislikeapi.com"
    }
    
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * Get vote data for a video
     */
    suspend fun getVotes(videoId: String): Result<VideoVotes> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching votes for: $videoId")
            
            val response = httpClient.get("$BASE_URL/votes") {
                parameter("videoId", videoId)
            }
            
            if (!response.status.isSuccess()) {
                return@withContext Result.failure(Exception("API error: ${response.status}"))
            }
            
            val votes = json.decodeFromString<VideoVotes>(response.bodyAsText())
            Result.success(votes)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching votes", e)
            Result.failure(e)
        }
    }
}

@Serializable
data class VideoVotes(
    val id: String,
    val dateCreated: String = "",
    val likes: Long = 0,
    val dislikes: Long = 0,
    val rating: Double = 0.0,
    val viewCount: Long = 0,
    val deleted: Boolean = false
) {
    val likeRatio: Float
        get() = if (likes + dislikes > 0) {
            likes.toFloat() / (likes + dislikes)
        } else {
            0.5f
        }
    
    fun formattedLikes(): String = formatCount(likes)
    fun formattedDislikes(): String = formatCount(dislikes)
    
    private fun formatCount(count: Long): String {
        return when {
            count >= 1_000_000_000 -> "%.1fB".format(count / 1_000_000_000.0)
            count >= 1_000_000 -> "%.1fM".format(count / 1_000_000.0)
            count >= 1_000 -> "%.1fK".format(count / 1_000.0)
            else -> count.toString()
        }
    }
}
