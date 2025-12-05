/*
 * REON Music App - Playlist DAO
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 */

package com.reon.music.data.database.dao

import androidx.room.*
import com.reon.music.data.database.entities.PlaylistEntity
import com.reon.music.data.database.entities.PlaylistSongCrossRef
import com.reon.music.data.database.entities.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    
    // Playlist CRUD
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: PlaylistEntity): Long
    
    @Update
    suspend fun update(playlist: PlaylistEntity)
    
    @Delete
    suspend fun delete(playlist: PlaylistEntity)
    
    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deleteById(playlistId: Long)
    
    // Get playlists
    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>
    
    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: Long): PlaylistEntity?
    
    @Query("SELECT * FROM playlists WHERE id = :id")
    fun getPlaylistByIdFlow(id: Long): Flow<PlaylistEntity?>
    
    @Query("SELECT COUNT(*) FROM playlists")
    fun getPlaylistCount(): Flow<Int>
    
    // Playlist songs
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongToPlaylist(crossRef: PlaylistSongCrossRef)
    
    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: String)
    
    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun clearPlaylist(playlistId: Long)
    
    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN playlist_songs ps ON s.id = ps.songId
        WHERE ps.playlistId = :playlistId
        ORDER BY ps.position ASC
    """)
    fun getPlaylistSongs(playlistId: Long): Flow<List<SongEntity>>
    
    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN playlist_songs ps ON s.id = ps.songId
        WHERE ps.playlistId = :playlistId
        ORDER BY ps.position ASC
    """)
    suspend fun getPlaylistSongsList(playlistId: Long): List<SongEntity>
    
    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun getPlaylistSongCount(playlistId: Long): Int
    
    @Query("SELECT MAX(position) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun getMaxPosition(playlistId: Long): Int?
    
    // Update position for reorder
    @Query("UPDATE playlist_songs SET position = :position WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun updateSongPosition(playlistId: Long, songId: String, position: Int)
    
    // Check if song is in playlist
    @Query("SELECT EXISTS(SELECT 1 FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId)")
    suspend fun isSongInPlaylist(playlistId: Long, songId: String): Boolean
    
    // Get playlists containing a song
    @Query("""
        SELECT p.* FROM playlists p
        INNER JOIN playlist_songs ps ON p.id = ps.playlistId
        WHERE ps.songId = :songId
    """)
    suspend fun getPlaylistsContainingSong(songId: String): List<PlaylistEntity>
    
    // Sync
    @Query("SELECT * FROM playlists WHERE needsSync = 1")
    suspend fun getPlaylistsNeedingSync(): List<PlaylistEntity>
    
    @Query("UPDATE playlists SET syncedAt = :timestamp, needsSync = 0 WHERE id = :playlistId")
    suspend fun markSynced(playlistId: Long, timestamp: Long = System.currentTimeMillis())
    
    // Update counts
    @Transaction
    suspend fun updatePlaylistStats(playlistId: Long) {
        val count = getPlaylistSongCount(playlistId)
        // Also calculate total duration if needed
        updateTrackCount(playlistId, count)
    }
    
    @Query("UPDATE playlists SET trackCount = :count, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateTrackCount(id: Long, count: Int, timestamp: Long = System.currentTimeMillis())
}
