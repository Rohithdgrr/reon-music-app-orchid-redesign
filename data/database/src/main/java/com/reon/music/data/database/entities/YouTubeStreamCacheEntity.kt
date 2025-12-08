/*
 * REON Music App - YouTube Stream URL Cache
 * Copyright (c) 2024 REON
 * Clean-room implementation
 */

package com.reon.music.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cache for YouTube stream URLs with expiry tracking
 * YouTube stream URLs expire after ~6 hours
 */
@Entity(tableName = "youtube_stream_cache")
data class YouTubeStreamCacheEntity(
    @PrimaryKey
    val videoId: String,
    
    // Stream information
    val audioStreamUrl: String,
    val codec: String = "opus", // opus, m4a, aac
    val bitrate: Int = 160, // in kbps
    val quality: String = "MEDIUM", // LOW, MEDIUM, HIGH
    
    // Expiry tracking
    val fetchedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (6 * 60 * 60 * 1000), // 6 hours
    
    // Metadata for context
    val title: String = "",
    val channelName: String = "",
    
    // Usage tracking
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val accessCount: Int = 0
) {
    /**
     * Check if the stream URL is still valid
     */
    fun isExpired(): Boolean {
        return System.currentTimeMillis() >= expiresAt
    }
    
    /**
     * Check if URL will expire soon (within 30 minutes)
     */
    fun willExpireSoon(): Boolean {
        val thirtyMinutes = 30 * 60 * 1000
        return (expiresAt - System.currentTimeMillis()) <= thirtyMinutes
    }
    
    /**
     * Get time until expiry in minutes
     */
    fun minutesUntilExpiry(): Long {
        val millisUntilExpiry = expiresAt - System.currentTimeMillis()
        return millisUntilExpiry / (60 * 1000)
    }
}
