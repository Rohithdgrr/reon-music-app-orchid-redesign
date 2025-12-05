/*
 * REON Music App - LRCLib Client
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 * 
 * LRCLib is a free, open-source synced lyrics database
 * https://lrclib.net
 */

package com.reon.music.data.network.lyrics

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
 * LRCLib API Client
 * Free, open-source synced lyrics
 */
@Singleton
class LrcLibClient @Inject constructor(
    private val httpClient: HttpClient
) {
    companion object {
        private const val TAG = "LrcLibClient"
        private const val BASE_URL = "https://lrclib.net/api"
    }
    
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * Search for lyrics by song info
     */
    suspend fun searchLyrics(
        title: String,
        artist: String,
        album: String? = null,
        duration: Int? = null
    ): Result<LrcLibResponse?> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Searching lyrics: $title - $artist")
            
            val response = httpClient.get("$BASE_URL/search") {
                parameter("track_name", title)
                parameter("artist_name", artist)
                album?.let { parameter("album_name", it) }
                duration?.let { parameter("duration", it) }
            }
            
            if (!response.status.isSuccess()) {
                Log.w(TAG, "Search failed: ${response.status}")
                return@withContext Result.success(null)
            }
            
            val results = json.decodeFromString<List<LrcLibResponse>>(response.bodyAsText())
            
            // Return best match
            val bestMatch = results.firstOrNull { it.syncedLyrics != null }
                ?: results.firstOrNull()
            
            Result.success(bestMatch)
        } catch (e: Exception) {
            Log.e(TAG, "Search error", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get lyrics by exact match
     */
    suspend fun getLyrics(
        title: String,
        artist: String,
        album: String,
        duration: Int
    ): Result<LrcLibResponse?> = withContext(Dispatchers.IO) {
        try {
            val response = httpClient.get("$BASE_URL/get") {
                parameter("track_name", title)
                parameter("artist_name", artist)
                parameter("album_name", album)
                parameter("duration", duration)
            }
            
            if (!response.status.isSuccess()) {
                return@withContext Result.success(null)
            }
            
            val lyrics = json.decodeFromString<LrcLibResponse>(response.bodyAsText())
            Result.success(lyrics)
        } catch (e: Exception) {
            Log.e(TAG, "Get lyrics error", e)
            Result.failure(e)
        }
    }
    
    /**
     * Parse synced lyrics from LRC format to list of lines
     */
    fun parseSyncedLyrics(lrcString: String): List<LyricLineData> {
        val regex = """\[(\d{2}):(\d{2})\.(\d{2,3})](.*)""".toRegex()
        return lrcString.lines()
            .mapNotNull { line ->
                regex.find(line)?.let { match ->
                    val (min, sec, ms, text) = match.destructured
                    val startTime = min.toLong() * 60000 + sec.toLong() * 1000 + ms.padEnd(3, '0').toLong()
                    LyricLineData(startTime, text.trim())
                }
            }
            .sortedBy { it.startTimeMs }
    }
}

@Serializable
data class LrcLibResponse(
    val id: Long,
    val trackName: String,
    val artistName: String,
    val albumName: String? = null,
    val duration: Double? = null,
    val instrumental: Boolean = false,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null
)

data class LyricLineData(
    val startTimeMs: Long,
    val text: String
)
