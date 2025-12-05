/*
 * REON Music App - Song DAO
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 */

package com.reon.music.data.database.dao

import androidx.room.*
import com.reon.music.data.database.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    
    @Query("SELECT * FROM songs ORDER BY lastPlayed DESC")
    fun getAllSongs(): Flow<List<SongEntity>>
    
    @Query("SELECT * FROM songs WHERE isLiked = 1 ORDER BY addedToLibrary DESC")
    fun getLikedSongs(): Flow<List<SongEntity>>
    
    @Query("SELECT * FROM songs WHERE isDownloaded = 1")
    fun getDownloadedSongs(): Flow<List<SongEntity>>
    
    @Query("SELECT * FROM songs WHERE sourceId = :sourceId AND source = :source")
    suspend fun getSongBySourceId(sourceId: String, source: String): SongEntity?
    
    @Query("SELECT * FROM songs ORDER BY playCount DESC LIMIT :limit")
    fun getMostPlayed(limit: Int = 20): Flow<List<SongEntity>>
    
    @Query("SELECT * FROM songs ORDER BY lastPlayed DESC LIMIT :limit")
    fun getRecentlyPlayed(limit: Int = 20): Flow<List<SongEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: SongEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<SongEntity>)
    
    @Update
    suspend fun update(song: SongEntity)
    
    @Delete
    suspend fun delete(song: SongEntity)
    
    @Query("DELETE FROM songs WHERE id = :songId")
    suspend fun deleteById(songId: Long)
    
    @Query("UPDATE songs SET isLiked = :isLiked WHERE id = :songId")
    suspend fun updateLikedStatus(songId: Long, isLiked: Boolean)
    
    @Query("UPDATE songs SET playCount = playCount + 1, lastPlayed = :lastPlayed WHERE id = :songId")
    suspend fun incrementPlayCount(songId: Long, lastPlayed: String)
}
