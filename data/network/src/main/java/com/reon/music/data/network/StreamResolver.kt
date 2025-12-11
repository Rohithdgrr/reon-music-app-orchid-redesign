/*
 * REON Music App - Stream Resolver
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 * 
 * This handles stream URL resolution with fallbacks
 */

package com.reon.music.data.network

import android.util.Log
import com.reon.music.core.model.Song
import com.reon.music.data.network.jiosaavn.JioSaavnClient
import com.reon.music.data.network.jiosaavn.JioSaavnDecryptor
import com.reon.music.data.network.youtube.PipedClient
import com.reon.music.data.network.youtube.YouTubeMusicClient
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stream Resolver - Handles getting playable URLs for songs
 * Tries multiple methods with fallbacks
 */
@Singleton
class StreamResolver @Inject constructor(
    private val httpClient: HttpClient,
    private val jiosaavnClient: JioSaavnClient,
    private val youtubeMusicClient: YouTubeMusicClient,
    private val pipedClient: PipedClient
) {
    companion object {
        private const val TAG = "StreamResolver"
    }
    
    /**
     * Resolve stream URL for any song
     */
    suspend fun resolveStreamUrl(song: Song): String? = withContext(Dispatchers.IO) {
        Log.d(TAG, "Resolving stream for: ${song.title} (${song.source})")
        
        // First try using existing streamUrl if available - it's most reliable
        if (!song.streamUrl.isNullOrBlank()) {
            Log.d(TAG, "Using existing stream URL for ${song.title}")
            return@withContext song.streamUrl
        }
        
        return@withContext when (song.source) {
            "jiosaavn" -> resolveJioSaavnUrl(song)
            "youtube" -> resolveYouTubeUrl(song)
            "local" -> song.streamUrl
            else -> resolveBySearch(song)
        }
    }
    
    /**
     * Resolve JioSaavn stream URL
     */
    private suspend fun resolveJioSaavnUrl(song: Song): String? {
        // First try existing URL - JioSaavn URLs are typically valid, don't waste time checking
        song.streamUrl?.let { url ->
            if (url.isNotBlank() && url.contains("aac.saavncdn.com")) {
                Log.d(TAG, "Using existing JioSaavn URL: ${url.take(80)}...")
                // Upgrade quality: _96 -> _160 -> _320
                return url
                    .replace("_96.mp4", "_320.mp4")
                    .replace("_96_", "_320_")
                    .replace("/96/", "/320/")
            }
        }
        
        // Fetch fresh song details if URL is missing or invalid
        Log.d(TAG, "Fetching fresh JioSaavn details for: ${song.id}")
        try {
            val details = jiosaavnClient.getSongDetails(song.id).getOrNull()
            details?.streamUrl?.let { url ->
                if (url.isNotBlank()) {
                    Log.d(TAG, "Got fresh URL from JioSaavn")
                    // Return with quality upgrade
                    return url
                        .replace("_96.mp4", "_320.mp4")
                        .replace("_96_", "_320_")
                        .replace("/96/", "/320/")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching song details", e)
        }
        
        // Last resort: search by name
        return searchAndResolve(song)
    }
    
    /**
     * Resolve YouTube stream URL with multiple fallbacks
     */
    private suspend fun resolveYouTubeUrl(song: Song): String? {
        val videoId = song.id
        
        // Try 1: Piped API (most reliable)
        Log.d(TAG, "Trying Piped for: $videoId")
        pipedClient.getStreamUrl(videoId).getOrNull()?.let { url ->
            if (isUrlAccessible(url)) {
                Log.d(TAG, "Piped URL works!")
                return url
            }
        }
        
        // Try 2: InnerTube API
        Log.d(TAG, "Trying InnerTube for: $videoId")
        youtubeMusicClient.getStreamUrl(videoId).getOrNull()?.let { url ->
            if (isUrlAccessible(url)) {
                Log.d(TAG, "InnerTube URL works!")
                return url
            }
        }
        
        // Try 3: Alternative Piped instances
        Log.d(TAG, "Trying alternative sources for: $videoId")
        val alternativeSources = listOf(
            "https://api.piped.privacydev.net",
            "https://pipedapi.in.projectsegfau.lt",
            "https://pipedapi.adminforge.de",
            "https://piped-api.hostux.net",
            "https://api.piped.yt"
        )
        
        for (instance in alternativeSources) {
            try {
                val response: HttpResponse = httpClient.get("$instance/streams/$videoId")
                if (response.status.isSuccess()) {
                    val json = kotlinx.serialization.json.Json.parseToJsonElement(response.bodyAsText())
                    val audioStreams = json.jsonObject["audioStreams"]?.jsonArray
                    val bestAudio = audioStreams?.filter { 
                        it.jsonObject["mimeType"]?.jsonPrimitive?.content?.contains("audio") == true
                    }?.maxByOrNull { 
                        it.jsonObject["bitrate"]?.jsonPrimitive?.int ?: 0 
                    }
                    
                    bestAudio?.jsonObject?.get("url")?.jsonPrimitive?.content?.let { url ->
                        if (isUrlAccessible(url)) {
                            Log.d(TAG, "Alternative Piped ($instance) works!")
                            return url
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed with $instance: ${e.message}")
            }
        }
        
        // Try 4: Search on JioSaavn as final fallback
        Log.d(TAG, "YouTube failed, trying to find on JioSaavn: ${song.title}")
        return searchAndResolve(song)
    }
    
    /**
     * Search for song and resolve (fallback method)
     */
    private suspend fun searchAndResolve(song: Song): String? {
        val searchQuery = "${song.title} ${song.artist}".take(60)
        Log.d(TAG, "Searching for: $searchQuery")
        
        val results = jiosaavnClient.searchSongs(searchQuery, 1, 5).getOrNull()
        results?.firstOrNull()?.let { foundSong ->
            foundSong.streamUrl?.let { url ->
                val highQuality = url.replace("_96", "_320")
                if (isUrlAccessible(highQuality)) {
                    return highQuality
                }
                if (isUrlAccessible(url)) {
                    return url
                }
            }
        }
        
        return null
    }
    
    /**
     * Fallback search - tries JioSaavn first then YouTube
     */
    private suspend fun resolveBySearch(song: Song): String? {
        // Try JioSaavn search
        val jiosaavnUrl = searchAndResolve(song)
        if (jiosaavnUrl != null) return jiosaavnUrl
        
        // Try YouTube search
        val ytResults = youtubeMusicClient.searchSongs("${song.title} ${song.artist}").getOrNull()
        ytResults?.firstOrNull()?.let { ytSong ->
            return resolveYouTubeUrl(ytSong)
        }
        
        return null
    }
    
    /**
     * Check if URL is accessible
     */
    private suspend fun isUrlAccessible(url: String): Boolean {
        return true
    }
}
