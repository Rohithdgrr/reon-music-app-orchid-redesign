/*
 * REON Music App - Lyrics Entity
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 */

package com.reon.music.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Cached lyrics entity
 */
@Entity(tableName = "lyrics")
data class LyricsEntity(
    @PrimaryKey
    val songId: String,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null, // JSON serialized list of LyricLine
    val source: String = "unknown", // "lrclib", "musixmatch", "youtube"
    val language: String? = null,
    val translatedLyrics: String? = null, // Translated synced lyrics JSON
    val translatedLanguage: String? = null,
    val fetchedAt: Long = System.currentTimeMillis()
) {
    fun getSyncedLines(): List<LyricLine>? {
        return syncedLyrics?.let {
            try {
                Json.decodeFromString<List<LyricLine>>(it)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    fun getTranslatedLines(): List<LyricLine>? {
        return translatedLyrics?.let {
            try {
                Json.decodeFromString<List<LyricLine>>(it)
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Single lyric line with timing
 */
@Serializable
data class LyricLine(
    val startTimeMs: Long,
    val endTimeMs: Long? = null,
    val text: String
) {
    companion object {
        fun fromLrc(lrcLine: String): LyricLine? {
            // Parse [mm:ss.xx] format
            val regex = """\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)""".toRegex()
            val match = regex.find(lrcLine) ?: return null
            
            val (min, sec, ms, text) = match.destructured
            val startTime = min.toLong() * 60000 + sec.toLong() * 1000 + ms.padEnd(3, '0').toLong()
            
            return LyricLine(
                startTimeMs = startTime,
                text = text.trim()
            )
        }
    }
}

/**
 * Complete lyrics data
 */
data class SyncedLyrics(
    val songId: String,
    val lines: List<LyricLine>,
    val source: String,
    val language: String?
) {
    fun toJson(): String = Json.encodeToString(lines)
}
