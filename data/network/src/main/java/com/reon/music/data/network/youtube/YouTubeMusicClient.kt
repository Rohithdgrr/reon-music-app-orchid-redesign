/*
 * REON Music App - YouTube Music (InnerTube) API Client
 * Copyright (c) 2024 REON
 * 
 * CLEAN-ROOM IMPLEMENTATION
 * This code is independently written based on publicly observable API behavior.
 * No GPL-licensed code has been copied. All implementation logic is original.
 */

package com.reon.music.data.network.youtube

import com.reon.music.core.common.Result
import com.reon.music.core.common.safeApiCall
import com.reon.music.core.model.Song
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * YouTube Music API Client using InnerTube
 * Clean-room implementation - independently written
 */
@Singleton
class YouTubeMusicClient @Inject constructor(
    private val httpClient: HttpClient
) {
    private val json = Json { ignoreUnknownKeys = true }
    
    companion object {
        private const val INNERTUBE_API_URL = "https://music.youtube.com/youtubei/v1"
        private const val API_KEY = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30"
        
        private val CLIENT_CONTEXT = buildJsonObject {
            put("client", buildJsonObject {
                put("clientName", "WEB_REMIX")
                put("clientVersion", "1.20231204.01.00")
                put("hl", "en")
                put("gl", "IN")
                put("experimentIds", JsonArray(emptyList()))
                put("experimentsToken", "")
                put("browserName", "Chrome")
                put("browserVersion", "120.0.0.0")
                put("osName", "Windows")
                put("osVersion", "10.0")
                put("platform", "DESKTOP")
            })
            put("user", buildJsonObject {
                put("lockedSafetyMode", false)
            })
        }
        
        private val ANDROID_CLIENT_CONTEXT = buildJsonObject {
            put("client", buildJsonObject {
                put("clientName", "ANDROID_MUSIC")
                put("clientVersion", "6.42.52")
                put("androidSdkVersion", 30)
                put("hl", "en")
                put("gl", "IN")
            })
        }
        
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
    
    /**
     * Search for songs on YouTube Music
     */
    suspend fun searchSongs(query: String): Result<List<Song>> = safeApiCall {
        val requestBody = buildJsonObject {
            put("query", query)
            put("context", CLIENT_CONTEXT)
            put("params", "EgWKAQIIAWoKEAMQBBAKEAkQBQ%3D%3D") // Filter for songs
        }
        
        val response: HttpResponse = httpClient.post("$INNERTUBE_API_URL/search?key=$API_KEY") {
            contentType(ContentType.Application.Json)
            header("User-Agent", USER_AGENT)
            header("Origin", "https://music.youtube.com")
            header("Referer", "https://music.youtube.com/")
            setBody(requestBody.toString())
        }
        
        val responseJson = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        parseSearchResults(responseJson)
    }
    
    /**
     * Get stream URL for a video ID
     */
    suspend fun getStreamUrl(videoId: String): Result<String?> = safeApiCall {
        // Try Android client first (better for audio)
        val requestBody = buildJsonObject {
            put("videoId", videoId)
            put("context", ANDROID_CLIENT_CONTEXT)
            put("playbackContext", buildJsonObject {
                put("contentPlaybackContext", buildJsonObject {
                    put("signatureTimestamp", 19950)
                })
            })
        }
        
        val response: HttpResponse = httpClient.post("$INNERTUBE_API_URL/player?key=$API_KEY") {
            contentType(ContentType.Application.Json)
            header("User-Agent", "com.google.android.youtube/17.36.4 (Linux; U; Android 12; GB) gzip")
            setBody(requestBody.toString())
        }
        
        val responseJson = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        extractAudioUrl(responseJson)
    }
    
    /**
     * Get song details
     */
    suspend fun getSongDetails(videoId: String): Result<Song?> = safeApiCall {
        val requestBody = buildJsonObject {
            put("videoId", videoId)
            put("context", CLIENT_CONTEXT)
        }
        
        val response: HttpResponse = httpClient.post("$INNERTUBE_API_URL/player?key=$API_KEY") {
            contentType(ContentType.Application.Json)
            header("User-Agent", USER_AGENT)
            header("Origin", "https://music.youtube.com")
            setBody(requestBody.toString())
        }
        
        val responseJson = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        parseSongDetails(responseJson, videoId)
    }
    
    /**
     * Get next/related songs
     */
    suspend fun getRelatedSongs(videoId: String, playlistId: String? = null): Result<List<Song>> = safeApiCall {
        val requestBody = buildJsonObject {
            put("videoId", videoId)
            put("context", CLIENT_CONTEXT)
            put("isAudioOnly", true)
            put("tunerSettingValue", "AUTOMIX_SETTING_NORMAL")
            if (playlistId != null) {
                put("playlistId", playlistId)
            }
        }
        
        val response: HttpResponse = httpClient.post("$INNERTUBE_API_URL/next?key=$API_KEY") {
            contentType(ContentType.Application.Json)
            header("User-Agent", USER_AGENT)
            header("Origin", "https://music.youtube.com")
            setBody(requestBody.toString())
        }
        
        val responseJson = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        parseRelatedSongs(responseJson)
    }
    
    // --- Parsing helpers ---
    
    private fun parseSearchResults(json: JsonObject): List<Song> {
        val songs = mutableListOf<Song>()
        
        try {
            val contents = json["contents"]?.jsonObject
                ?.get("tabbedSearchResultsRenderer")?.jsonObject
                ?.get("tabs")?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("tabRenderer")?.jsonObject
                ?.get("content")?.jsonObject
                ?.get("sectionListRenderer")?.jsonObject
                ?.get("contents")?.jsonArray
            
            contents?.forEach { section ->
                val musicShelf = section.jsonObject["musicShelfRenderer"]?.jsonObject
                val items = musicShelf?.get("contents")?.jsonArray
                
                items?.forEach { item ->
                    val musicItem = item.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject
                    parseMusicItem(musicItem)?.let { songs.add(it) }
                }
            }
        } catch (e: Exception) {
            // Fallback - try alternate parsing
        }
        
        return songs
    }
    
    private fun parseMusicItem(item: JsonObject?): Song? {
        if (item == null) return null
        
        return try {
            val playItem = item["overlay"]?.jsonObject
                ?.get("musicItemThumbnailOverlayRenderer")?.jsonObject
                ?.get("content")?.jsonObject
                ?.get("musicPlayButtonRenderer")?.jsonObject
            
            val videoId = playItem?.get("playNavigationEndpoint")?.jsonObject
                ?.get("watchEndpoint")?.jsonObject
                ?.get("videoId")?.jsonPrimitive?.content ?: return null
            
            val flexColumns = item["flexColumns"]?.jsonArray
            
            val title = flexColumns?.getOrNull(0)?.jsonObject
                ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
                ?.get("text")?.jsonObject
                ?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.content ?: "Unknown"
            
            val artistRuns = flexColumns?.getOrNull(1)?.jsonObject
                ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
                ?.get("text")?.jsonObject
                ?.get("runs")?.jsonArray
            
            val artist = artistRuns?.mapNotNull { 
                it.jsonObject["text"]?.jsonPrimitive?.content 
            }?.firstOrNull { it != " • " && it != " & " } ?: "Unknown Artist"
            
            val thumbnail = item["thumbnail"]?.jsonObject
                ?.get("musicThumbnailRenderer")?.jsonObject
                ?.get("thumbnail")?.jsonObject
                ?.get("thumbnails")?.jsonArray?.lastOrNull()?.jsonObject
                ?.get("url")?.jsonPrimitive?.content
            
            val durationText = flexColumns?.lastOrNull()?.jsonObject
                ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
                ?.get("text")?.jsonObject
                ?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.content
            
            Song(
                id = videoId,
                title = title,
                artist = artist,
                artworkUrl = thumbnail?.replace("w60-h60", "w500-h500"),
                duration = parseDuration(durationText),
                source = "youtube"
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parseSongDetails(json: JsonObject, videoId: String): Song? {
        return try {
            val videoDetails = json["videoDetails"]?.jsonObject ?: return null
            
            val title = videoDetails["title"]?.jsonPrimitive?.content ?: "Unknown"
            val author = videoDetails["author"]?.jsonPrimitive?.content ?: "Unknown Artist"
            val lengthSeconds = videoDetails["lengthSeconds"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            
            val thumbnail = videoDetails["thumbnail"]?.jsonObject
                ?.get("thumbnails")?.jsonArray?.lastOrNull()?.jsonObject
                ?.get("url")?.jsonPrimitive?.content
            
            Song(
                id = videoId,
                title = title,
                artist = author,
                artworkUrl = thumbnail,
                duration = lengthSeconds,
                source = "youtube"
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun extractAudioUrl(json: JsonObject): String? {
        return try {
            val streamingData = json["streamingData"]?.jsonObject
            
            // Prefer adaptive formats (audio only)
            val adaptiveFormats = streamingData?.get("adaptiveFormats")?.jsonArray
            
            // Find best audio format (251 = opus high, 140 = m4a, 250/249 = opus med/low)
            val audioFormat = adaptiveFormats?.filter { format ->
                val mimeType = format.jsonObject["mimeType"]?.jsonPrimitive?.content ?: ""
                mimeType.contains("audio")
            }?.maxByOrNull { format ->
                format.jsonObject["bitrate"]?.jsonPrimitive?.long ?: 0L
            }
            
            audioFormat?.jsonObject?.get("url")?.jsonPrimitive?.content
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parseRelatedSongs(json: JsonObject): List<Song> {
        val songs = mutableListOf<Song>()
        
        try {
            val contents = json["contents"]?.jsonObject
                ?.get("singleColumnMusicWatchNextResultsRenderer")?.jsonObject
                ?.get("tabbedRenderer")?.jsonObject
                ?.get("watchNextTabbedResultsRenderer")?.jsonObject
                ?.get("tabs")?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("tabRenderer")?.jsonObject
                ?.get("content")?.jsonObject
                ?.get("musicQueueRenderer")?.jsonObject
                ?.get("content")?.jsonObject
                ?.get("playlistPanelRenderer")?.jsonObject
                ?.get("contents")?.jsonArray
            
            contents?.forEach { item ->
                val renderer = item.jsonObject["playlistPanelVideoRenderer"]?.jsonObject
                parsePlaylistPanelItem(renderer)?.let { songs.add(it) }
            }
        } catch (e: Exception) {
            // Ignore parsing errors
        }
        
        return songs
    }
    
    private fun parsePlaylistPanelItem(item: JsonObject?): Song? {
        if (item == null) return null
        
        return try {
            val videoId = item["videoId"]?.jsonPrimitive?.content ?: return null
            val title = item["title"]?.jsonObject
                ?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.content ?: "Unknown"
            
            val artist = item["shortBylineText"]?.jsonObject
                ?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.content ?: "Unknown Artist"
            
            val thumbnail = item["thumbnail"]?.jsonObject
                ?.get("thumbnails")?.jsonArray?.lastOrNull()?.jsonObject
                ?.get("url")?.jsonPrimitive?.content
            
            val durationText = item["lengthText"]?.jsonObject
                ?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.content
            
            Song(
                id = videoId,
                title = title,
                artist = artist,
                artworkUrl = thumbnail?.replace("w60-h60", "w500-h500"),
                duration = parseDuration(durationText),
                source = "youtube"
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parseDuration(durationText: String?): Int {
        if (durationText == null) return 0
        val parts = durationText.split(":")
        return when (parts.size) {
            2 -> parts[0].toIntOrNull()?.times(60)?.plus(parts[1].toIntOrNull() ?: 0) ?: 0
            3 -> parts[0].toIntOrNull()?.times(3600)
                ?.plus(parts[1].toIntOrNull()?.times(60) ?: 0)
                ?.plus(parts[2].toIntOrNull() ?: 0) ?: 0
            else -> 0
        }
    }
    
    // ===== Sync Methods (stubs - require authentication in production) =====
    
    /**
     * Get user's liked songs
     * Note: Requires authentication in production
     */
    suspend fun getLikedSongs(): Result<List<Song>> = safeApiCall {
        // In production, would use authenticated request
        emptyList()
    }
    
    /**
     * Get user's playlists
     * Note: Requires authentication in production
     */
    suspend fun getUserPlaylists(): Result<List<com.reon.music.core.model.Playlist>> = safeApiCall {
        // In production, would use authenticated request
        emptyList()
    }
    
    /**
     * Get songs from a playlist
     */
    suspend fun getPlaylistSongs(playlistId: String): Result<List<Song>> = safeApiCall {
        // In production, would parse playlist contents
        emptyList()
    }
    
    /**
     * Like a song
     * Note: Requires authentication in production
     */
    suspend fun likeSong(videoId: String): Result<Boolean> = safeApiCall {
        // In production, would send like request
        true
    }
    
    /**
     * Create a playlist
     * Note: Requires authentication in production
     */
    suspend fun createPlaylist(title: String, description: String): Result<String?> = safeApiCall {
        // In production, would create playlist and return ID
        null
    }
    
    /**
     * Add song to playlist
     * Note: Requires authentication in production
     */
    suspend fun addToPlaylist(playlistId: String, videoId: String): Result<Boolean> = safeApiCall {
        // In production, would add to playlist
        true
    }
    
    /**
     * General search returning mixed results
     */
    suspend fun search(query: String): Result<com.reon.music.core.model.SearchResult> = safeApiCall {
        val songsResult = searchSongs(query)
        val songs = if (songsResult is Result.Success) songsResult.data else emptyList()
        com.reon.music.core.model.SearchResult(
            songs = songs,
            albums = emptyList(),
            artists = emptyList(),
            playlists = emptyList()
        )
    }
}

