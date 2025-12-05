/*
 * REON Music App - Database Entities
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 */

package com.reon.music.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Song entity for local storage
 */
@Entity(
    tableName = "songs",
    indices = [Index(value = ["source", "sourceId"], unique = true)]
)
data class SongEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceId: String,
    val source: String, // "jiosaavn", "youtube", "local"
    val title: String,
    val artist: String,
    val album: String = "",
    val albumId: String? = null,
    val duration: Int = 0,
    val artworkUrl: String? = null,
    val streamUrl: String? = null,
    val isLiked: Boolean = false,
    val totalPlayTime: Long = 0,
    val playCount: Int = 0,
    val lastPlayed: LocalDateTime? = null,
    val addedToLibrary: LocalDateTime? = null,
    val isDownloaded: Boolean = false,
    val downloadPath: String? = null,
    val extras: String = "{}" // JSON for additional data
)

/**
 * Playlist entity
 */
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val artworkUrl: String? = null,
    val isLocal: Boolean = true,
    val remoteId: String? = null,
    val source: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

/**
 * Many-to-many relationship between playlists and songs
 */
@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"],
    indices = [Index(value = ["playlistId"]), Index(value = ["songId"])]
)
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: Long,
    val position: Int,
    val addedAt: LocalDateTime = LocalDateTime.now()
)
