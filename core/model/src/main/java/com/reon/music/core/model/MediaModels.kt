/*
 * REON Music App - Media Models
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 */

package com.reon.music.core.model

import kotlinx.serialization.Serializable

/**
 * Represents a song/track
 * Clean-room design - not copied from any GPL source
 */
@Serializable
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val albumId: String? = null,
    val duration: Int = 0, // in seconds
    val artworkUrl: String? = null,
    val streamUrl: String? = null,
    val source: String = "unknown", // "jiosaavn", "youtube", "local"
    val hasLyrics: Boolean = false,
    val language: String = "",
    val year: String = "",
    val releaseDate: String = "",
    val is320kbps: Boolean = false,
    val permaUrl: String? = null,
    val extras: Map<String, String> = emptyMap()
) {
    /**
     * Get high quality artwork URL
     */
    fun getHighQualityArtwork(): String? {
        return artworkUrl
            ?.replace("50x50", "500x500")
            ?.replace("150x150", "500x500")
            ?.replace("http:", "https:")
    }
    
    /**
     * Get stream URL with quality variant
     */
    fun getStreamUrlWithQuality(quality: String = "_320"): String? {
        return streamUrl?.let { url ->
            when {
                url.contains("_96") -> url.replace("_96", quality)
                url.contains("_160") -> url.replace("_160", quality)
                url.contains("_320") -> url.replace("_320", quality)
                else -> url
            }
        }
    }
    
    /**
     * Format duration as MM:SS
     */
    fun formattedDuration(): String {
        val minutes = duration / 60
        val seconds = duration % 60
        return "%d:%02d".format(minutes, seconds)
    }
}

/**
 * Represents an album
 */
@Serializable
data class Album(
    val id: String,
    val name: String,
    val artist: String,
    val artworkUrl: String? = null,
    val year: String = "",
    val songCount: Int = 0,
    val songs: List<Song> = emptyList(),
    val permaUrl: String? = null,
    val language: String = ""
)

/**
 * Represents an artist
 */
@Serializable
data class Artist(
    val id: String,
    val name: String,
    val artworkUrl: String? = null,
    val followerCount: Int = 0,
    val topSongs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val permaUrl: String? = null
)

/**
 * Represents a playlist
 */
@Serializable
data class Playlist(
    val id: String,
    val name: String,
    val description: String = "",
    val artworkUrl: String? = null,
    val songCount: Int = 0,
    val songs: List<Song> = emptyList(),
    val isLocal: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val permaUrl: String? = null
)

/**
 * Lyrics data
 */
@Serializable
data class Lyrics(
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null
) {
    fun getSyncedLines(): List<LyricLine> {
        if (syncedLyrics == null) return emptyList()
        
        val regex = """\[(\d{2}):(\d{2})\.(\d{2,3})](.*)""".toRegex()
        return syncedLyrics.lines().mapNotNull { line ->
            regex.find(line)?.let { match ->
                val (min, sec, ms) = match.destructured
                val timestamp = min.toLong() * 60000 + 
                               sec.toLong() * 1000 + 
                               ms.padEnd(3, '0').toLong()
                LyricLine(timestamp, match.groupValues[4].trim())
            }
        }
    }
}

/**
 * Single lyric line with timestamp
 */
@Serializable
data class LyricLine(
    val timestamp: Long, // in milliseconds
    val text: String
)

/**
 * Search result container
 */
@Serializable
data class SearchResult(
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList()
)
