/*
 * REON Music App - Artist Entity
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 */

package com.reon.music.data.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Artist entity for followed artists
 */
@Entity(
    tableName = "artists",
    indices = [Index(value = ["isFollowed"])]
)
data class ArtistEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val thumbnailUrl: String? = null,
    val bio: String? = null,
    val source: String = "unknown", // "jiosaavn", "youtube"
    val isFollowed: Boolean = false,
    val songCount: Int = 0,
    val addedAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null,
    val needsSync: Boolean = false
)

/**
 * Album entity for saved albums
 */
@Entity(
    tableName = "albums",
    indices = [Index(value = ["isSaved"])]
)
data class AlbumEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String? = null,
    val thumbnailUrl: String? = null,
    val year: Int? = null,
    val trackCount: Int = 0,
    val source: String = "unknown",
    val isSaved: Boolean = false,
    val addedAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null,
    val needsSync: Boolean = false
)
