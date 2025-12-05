/*
 * REON Music App - Lyrics DAO
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 */

package com.reon.music.data.database.dao

import androidx.room.*
import com.reon.music.data.database.entities.LyricsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LyricsDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(lyrics: LyricsEntity)
    
    @Update
    suspend fun update(lyrics: LyricsEntity)
    
    @Delete
    suspend fun delete(lyrics: LyricsEntity)
    
    @Query("DELETE FROM lyrics WHERE songId = :songId")
    suspend fun deleteBySongId(songId: String)
    
    @Query("SELECT * FROM lyrics WHERE songId = :songId")
    suspend fun getLyrics(songId: String): LyricsEntity?
    
    @Query("SELECT * FROM lyrics WHERE songId = :songId")
    fun getLyricsFlow(songId: String): Flow<LyricsEntity?>
    
    @Query("SELECT EXISTS(SELECT 1 FROM lyrics WHERE songId = :songId)")
    suspend fun hasLyrics(songId: String): Boolean
    
    @Query("SELECT EXISTS(SELECT 1 FROM lyrics WHERE songId = :songId AND syncedLyrics IS NOT NULL)")
    suspend fun hasSyncedLyrics(songId: String): Boolean
    
    // Update translation
    @Query("UPDATE lyrics SET translatedLyrics = :translation, translatedLanguage = :language WHERE songId = :songId")
    suspend fun updateTranslation(songId: String, translation: String, language: String)
    
    // Clear old lyrics (cache management)
    @Query("DELETE FROM lyrics WHERE fetchedAt < :olderThan")
    suspend fun deleteOlderThan(olderThan: Long)
    
    // Count cached lyrics
    @Query("SELECT COUNT(*) FROM lyrics")
    fun getCachedCount(): Flow<Int>
}
