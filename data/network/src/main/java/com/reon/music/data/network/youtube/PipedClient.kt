/*
 * REON Music App - Piped API Client (Fallback)
 * Copyright (c) 2024 REON
 * 
 * CLEAN-ROOM IMPLEMENTATION
 * Piped is an open-source YouTube frontend with a public API
 */

package com.reon.music.data.network.youtube

import com.reon.music.core.common.Result
import com.reon.music.core.common.safeApiCall
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Piped API Client for YouTube stream fallback
 * Uses the public Piped API
 */
@Singleton
class PipedClient @Inject constructor(
    private val httpClient: HttpClient
) {
    companion object {
        // Public Piped instances - updated with currently active servers
        private val PIPED_INSTANCES = listOf(
            "https://pipedapi.kavin.rocks",
            "https://api.piped.privacydev.net",
            "https://pipedapi.in.projectsegfau.lt",
            "https://pipedapi.adminforge.de",
            "https://piped-api.hostux.net",
            "https://api.piped.yt",
            "https://yapi.vyper.me"
        )
    }
    
    private var currentInstanceIndex = 0
    
    /**
     * Get stream URL from Piped
     */
    suspend fun getStreamUrl(videoId: String): Result<String?> = safeApiCall {
        var lastError: Exception? = null
        
        // Try each instance until one works
        for (i in PIPED_INSTANCES.indices) {
            val instanceIndex = (currentInstanceIndex + i) % PIPED_INSTANCES.size
            val instance = PIPED_INSTANCES[instanceIndex]
            
            try {
                val response: HttpResponse = httpClient.get("$instance/streams/$videoId")
                val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                
                // Get audio streams
                val audioStreams = json["audioStreams"]?.jsonArray
                
                // Find best audio quality
                val bestAudio = audioStreams?.filter { stream ->
                    val mimeType = stream.jsonObject["mimeType"]?.jsonPrimitive?.content ?: ""
                    mimeType.contains("audio")
                }?.maxByOrNull { stream ->
                    stream.jsonObject["bitrate"]?.jsonPrimitive?.int ?: 0
                }
                
                val url = bestAudio?.jsonObject?.get("url")?.jsonPrimitive?.content
                
                if (url != null) {
                    currentInstanceIndex = instanceIndex // Remember working instance
                    return@safeApiCall url
                }
            } catch (e: Exception) {
                lastError = e
                continue
            }
        }
        
        null
    }
    
    /**
     * Get video info from Piped
     */
    suspend fun getVideoInfo(videoId: String): Result<VideoInfo?> = safeApiCall {
        for (i in PIPED_INSTANCES.indices) {
            val instanceIndex = (currentInstanceIndex + i) % PIPED_INSTANCES.size
            val instance = PIPED_INSTANCES[instanceIndex]
            
            try {
                val response: HttpResponse = httpClient.get("$instance/streams/$videoId")
                val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                
                val title = json["title"]?.jsonPrimitive?.content ?: continue
                val uploader = json["uploader"]?.jsonPrimitive?.content ?: "Unknown"
                val duration = json["duration"]?.jsonPrimitive?.int ?: 0
                val thumbnail = json["thumbnailUrl"]?.jsonPrimitive?.content
                
                // Get audio streams
                val audioStreams = json["audioStreams"]?.jsonArray
                val bestAudio = audioStreams?.filter { stream ->
                    val mimeType = stream.jsonObject["mimeType"]?.jsonPrimitive?.content ?: ""
                    mimeType.contains("audio")
                }?.maxByOrNull { stream ->
                    stream.jsonObject["bitrate"]?.jsonPrimitive?.int ?: 0
                }
                
                val audioUrl = bestAudio?.jsonObject?.get("url")?.jsonPrimitive?.content
                
                currentInstanceIndex = instanceIndex
                return@safeApiCall VideoInfo(
                    videoId = videoId,
                    title = title,
                    artist = uploader,
                    duration = duration,
                    thumbnailUrl = thumbnail,
                    audioUrl = audioUrl
                )
            } catch (e: Exception) {
                continue
            }
        }
        
        null
    }
    
    /**
     * Search videos via Piped
     */
    suspend fun search(query: String): Result<List<VideoInfo>> = safeApiCall {
        for (i in PIPED_INSTANCES.indices) {
            val instanceIndex = (currentInstanceIndex + i) % PIPED_INSTANCES.size
            val instance = PIPED_INSTANCES[instanceIndex]
            
            try {
                val response: HttpResponse = httpClient.get("$instance/search") {
                    parameter("q", query)
                    parameter("filter", "music_songs")
                }
                
                val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                val items = json["items"]?.jsonArray ?: continue
                
                currentInstanceIndex = instanceIndex
                
                return@safeApiCall items.mapNotNull { item ->
                    val obj = item.jsonObject
                    val url = obj["url"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val videoId = url.substringAfter("/watch?v=")
                    
                    if (videoId.isBlank()) return@mapNotNull null
                    
                    VideoInfo(
                        videoId = videoId,
                        title = obj["title"]?.jsonPrimitive?.content ?: "Unknown",
                        artist = obj["uploaderName"]?.jsonPrimitive?.content ?: "Unknown",
                        duration = obj["duration"]?.jsonPrimitive?.int ?: 0,
                        thumbnailUrl = obj["thumbnail"]?.jsonPrimitive?.content,
                        audioUrl = null // Will be fetched on demand
                    )
                }
            } catch (e: Exception) {
                continue
            }
        }
        
        emptyList()
    }
}

/**
 * Video info from Piped
 */
data class VideoInfo(
    val videoId: String,
    val title: String,
    val artist: String,
    val duration: Int,
    val thumbnailUrl: String?,
    val audioUrl: String?
)
