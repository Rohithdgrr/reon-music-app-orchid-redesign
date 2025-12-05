/*
 * REON Music App - Playlist Entity
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 */

package com.reon.music.data.database.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * Room entity for user-created playlists
 */
@Entity(
    tableName = "playlists",
    indices = [
        Index(value = ["title"]),
        Index(value = ["youTubePlaylistId"])
    ]
)
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String? = null,
    val thumbnailUrl: String? = null,
    val isLocal: Boolean = true,
    val remoteId: String? = null, // For Neon sync
    val youTubePlaylistId: String? = null, // For YouTube Music sync
    val isFromYouTube: Boolean = false,
    val songCount: Int = 0,
    val totalDuration: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null,
    val needsSync: Boolean = false
)

/**
 * Cross-reference entity for playlist-song relationship
 */
@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"],
    indices = [
        Index(value = ["playlistId"]),
        Index(value = ["songId"])
    ]
)
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: String,
    val position: Int,
    val addedAt: Long = System.currentTimeMillis()
)

/**
 * Playlist with songs using Room relations
 */
data class PlaylistWithSongs(
    @Embedded
    val playlist: PlaylistEntity,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PlaylistSongCrossRef::class,
            parentColumn = "playlistId",
            entityColumn = "songId"
        )
    )
    val songs: List<SongEntity>
)
