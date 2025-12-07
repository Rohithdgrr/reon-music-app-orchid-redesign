/*
 * REON Music App - Song Sort Options
 * Copyright (c) 2024 REON
 */

package com.reon.music.core.model

/**
 * Options for sorting songs
 */
enum class SongSortOption {
    DEFAULT,              // Default order (relevance)
    VIEWS,                // Sort by view count (highest first)
    LIKES,                // Sort by like count (highest first)
    CHANNEL_POPULARITY,   // Sort by channel subscriber count
    QUALITY,              // Sort by audio/video quality
    RECENT,               // Sort by upload date (newest first)
    DURATION,              // Sort by duration (longest first)
    TITLE,                // Sort alphabetically by title
    ARTIST                // Sort alphabetically by artist
}

