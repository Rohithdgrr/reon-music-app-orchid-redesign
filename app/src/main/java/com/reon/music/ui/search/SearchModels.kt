/*
 * REON Music App - Search Data Models
 * Copyright (c) 2024 REON
 * Data models for search functionality
 */

package com.reon.music.ui.search

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*

data class SearchResult(
    val id: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String?,
    val type: SearchResultType,
    val duration: String? = null
)

enum class SearchResultType {
    SONG, VIDEO, ARTIST, ALBUM, PLAYLIST
}

data class SearchSuggestion(
    val text: String,
    val type: SuggestionType,
    val metadata: String? = null
)

enum class SuggestionType {
    TRENDING, RECENT, ARTIST, PERSONALIZED
}

data class SearchFilter(
    val id: String,
    val name: String,
    val icon: ImageVector
)

// Mock filter data for YouTubeMusicSearchUI
val availableFilters = listOf(
    SearchFilter("all", "All", Icons.Filled.Search),
    SearchFilter("songs", "Songs", Icons.Filled.MusicNote),
    SearchFilter("videos", "Videos", Icons.Filled.VideoFile),
    SearchFilter("artists", "Artists", Icons.Filled.Person),
    SearchFilter("albums", "Albums", Icons.Filled.Album),
    SearchFilter("playlists", "Playlists", Icons.AutoMirrored.Filled.QueueMusic)
)