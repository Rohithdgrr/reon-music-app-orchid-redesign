/*
 * REON Music App - Song Entity
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 */

package com.reon.music.data.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.reon.music.core.model.Song

/**
 * Room entity for storing songs locally
 */
@Entity(
    tableName = "songs",
    indices = [
        Index(value = ["isLiked"]),
        Index(value = ["inLibrary"]),
        Index(value = ["downloadState"]),
        Index(value = ["lastPlayedAt"]),
        Index(value = ["playCount"])
    ]
)
data class SongEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String? = null,
    val album: String = "",
    val albumId: String? = null,
    val duration: Int = 0,
    val artworkUrl: String? = null,
    val streamUrl: String? = null,
    val source: String = "unknown", // "jiosaavn", "youtube", "local"
    val isLiked: Boolean = false,
    val inLibrary: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedAt: Long? = null,
    val downloadState: Int = DownloadState.NOT_DOWNLOADED,
    val localPath: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null, // For Neon sync
    val needsSync: Boolean = false
) {
    fun toSong(): Song = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        duration = duration,
        artworkUrl = artworkUrl,
        streamUrl = if (downloadState == DownloadState.DOWNLOADED && localPath != null) localPath else streamUrl,
        source = source
    )
    
    companion object {
        fun fromSong(song: Song, isLiked: Boolean = false, inLibrary: Boolean = false, isDownloaded: Boolean = false): SongEntity {
            return SongEntity(
                id = song.id,
                title = song.title,
                artist = song.artist,
                album = song.album,
                duration = song.duration,
                artworkUrl = song.artworkUrl,
                streamUrl = song.streamUrl,
                source = song.source,
                isLiked = isLiked,
                inLibrary = inLibrary,
                downloadState = if (isDownloaded) DownloadState.DOWNLOADED else DownloadState.NOT_DOWNLOADED
            )
        }
    }
}

object DownloadState {
    const val NOT_DOWNLOADED = 0
    const val DOWNLOADING = 1
    const val DOWNLOADED = 2  // Only user-initiated downloads
    const val FAILED = 3
    const val PAUSED = 4
    const val AUTO_CACHED = 5 // For smart offline cache only, NOT user downloads
}
