/*
 * REON Music App - Podcast Models
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 */

package com.reon.music.core.model

import kotlinx.serialization.Serializable

/**
 * Represents a podcast
 */
@Serializable
data class Podcast(
    val id: String,
    val title: String,
    val description: String = "",
    val artworkUrl: String? = null,
    val author: String = "",
    val episodeCount: Int = 0,
    val episodes: List<PodcastEpisode> = emptyList(),
    val category: String = "",
    val language: String = "",
    val permaUrl: String? = null
)

/**
 * Represents a podcast episode
 */
@Serializable
data class PodcastEpisode(
    val id: String,
    val title: String,
    val description: String = "",
    val artworkUrl: String? = null,
    val duration: Int = 0, // in seconds
    val publishDate: String = "",
    val streamUrl: String? = null,
    val source: String = "youtube", // "youtube", "jiosaavn"
    val podcastId: String = "",
    val podcastTitle: String = ""
) {
    /**
     * Format duration as HH:MM:SS or MM:SS
     */
    fun formattedDuration(): String {
        val hours = duration / 3600
        val minutes = (duration % 3600) / 60
        val seconds = duration % 60
        
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%d:%02d".format(minutes, seconds)
        }
    }
}

