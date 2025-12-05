/*
 * REON Music App - Room Database
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 */

package com.reon.music.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.reon.music.data.database.dao.HistoryDao
import com.reon.music.data.database.dao.LyricsDao
import com.reon.music.data.database.dao.PlaylistDao
import com.reon.music.data.database.dao.SongDao
import com.reon.music.data.database.entities.AlbumEntity
import com.reon.music.data.database.entities.ArtistEntity
import com.reon.music.data.database.entities.ListenHistoryEntity
import com.reon.music.data.database.entities.LyricsEntity
import com.reon.music.data.database.entities.PlaylistEntity
import com.reon.music.data.database.entities.PlaylistSongCrossRef
import com.reon.music.data.database.entities.SongEntity

/**
 * Main Room Database for REON Music App
 * Stores all local data with sync capabilities
 */
@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        ArtistEntity::class,
        AlbumEntity::class,
        ListenHistoryEntity::class,
        LyricsEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class ReonDatabase : RoomDatabase() {
    
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun historyDao(): HistoryDao
    abstract fun lyricsDao(): LyricsDao
    
    companion object {
        const val DATABASE_NAME = "reon_music.db"
    }
}
