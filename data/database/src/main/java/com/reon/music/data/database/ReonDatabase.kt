/*
 * REON Music App - Database
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 */

package com.reon.music.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.reon.music.data.database.dao.PlaylistDao
import com.reon.music.data.database.dao.SongDao
import com.reon.music.data.database.entity.PlaylistEntity
import com.reon.music.data.database.entity.PlaylistSongCrossRef
import com.reon.music.data.database.entity.SongEntity

/**
 * REON Room Database
 * Clean-room implementation
 */
@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class ReonDatabase : RoomDatabase() {
    
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    
    companion object {
        const val DATABASE_NAME = "reon_database"
        
        fun create(context: Context): ReonDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                ReonDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
