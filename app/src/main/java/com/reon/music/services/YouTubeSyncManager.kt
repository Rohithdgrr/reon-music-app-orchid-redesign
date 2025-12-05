/*
 * REON Music App - YouTube Music Sync Manager
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 */

package com.reon.music.services

import android.util.Log
import com.reon.music.core.model.Playlist
import com.reon.music.core.model.Song
import com.reon.music.data.database.dao.PlaylistDao
import com.reon.music.data.database.dao.SongDao
import com.reon.music.data.database.entities.PlaylistEntity
import com.reon.music.data.database.entities.SongEntity
import com.reon.music.data.network.youtube.YouTubeMusicClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class SyncState(
    val isSyncing: Boolean = false,
    val lastSyncTime: Long? = null,
    val syncProgress: Float = 0f,
    val currentTask: String = "",
    val error: String? = null,
    val likedSongsSynced: Int = 0,
    val playlistsSynced: Int = 0,
    val historySynced: Int = 0
)

/**
 * YouTube Music Sync Manager
 * Bi-directional sync with YouTube Music account
 */
@Singleton
class YouTubeSyncManager @Inject constructor(
    private val youTubeClient: YouTubeMusicClient,
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao,
    private val accountManager: AccountManager
) {
    companion object {
        private const val TAG = "YouTubeSyncManager"
    }
    
    private val _syncState = MutableStateFlow(SyncState())
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    
    /**
     * Perform full sync
     */
    suspend fun performFullSync(
        syncLikedSongs: Boolean = true,
        syncPlaylists: Boolean = true,
        syncHistory: Boolean = false,
        uploadToYouTube: Boolean = false
    ) = withContext(Dispatchers.IO) {
        if (!accountManager.isYouTubeLinked()) {
            _syncState.value = _syncState.value.copy(
                error = "YouTube account not linked"
            )
            return@withContext
        }
        
        _syncState.value = SyncState(isSyncing = true)
        
        try {
            var progress = 0f
            val totalSteps = listOf(syncLikedSongs, syncPlaylists, syncHistory).count { it } +
                           if (uploadToYouTube) 1 else 0
            val stepProgress = 1f / totalSteps
            
            // Sync liked songs
            if (syncLikedSongs) {
                _syncState.value = _syncState.value.copy(
                    currentTask = "Syncing liked songs...",
                    syncProgress = progress
                )
                
                val likedCount = syncLikedSongsFromYouTube()
                _syncState.value = _syncState.value.copy(likedSongsSynced = likedCount)
                progress += stepProgress
            }
            
            // Sync playlists
            if (syncPlaylists) {
                _syncState.value = _syncState.value.copy(
                    currentTask = "Syncing playlists...",
                    syncProgress = progress
                )
                
                val playlistCount = syncPlaylistsFromYouTube()
                _syncState.value = _syncState.value.copy(playlistsSynced = playlistCount)
                progress += stepProgress
            }
            
            // Sync history
            if (syncHistory) {
                _syncState.value = _syncState.value.copy(
                    currentTask = "Syncing history...",
                    syncProgress = progress
                )
                
                val historyCount = syncHistoryFromYouTube()
                _syncState.value = _syncState.value.copy(historySynced = historyCount)
                progress += stepProgress
            }
            
            // Upload to YouTube
            if (uploadToYouTube) {
                _syncState.value = _syncState.value.copy(
                    currentTask = "Uploading to YouTube Music...",
                    syncProgress = progress
                )
                
                uploadLocalDataToYouTube()
                progress += stepProgress
            }
            
            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                lastSyncTime = System.currentTimeMillis(),
                syncProgress = 1f,
                currentTask = "Sync complete"
            )
            
            Log.d(TAG, "Sync completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                error = "Sync failed: ${e.message}"
            )
        }
    }
    
    /**
     * Sync liked songs from YouTube Music
     */
    private suspend fun syncLikedSongsFromYouTube(): Int {
        val likedSongs: List<Song> = youTubeClient.getLikedSongs().getOrNull() ?: return 0
        
        var count = 0
        for (song in likedSongs) {
            val existingSong = songDao.getSongById(song.id)
            if (existingSong == null) {
                songDao.insert(SongEntity.fromSong(song, isLiked = true))
            } else if (!existingSong.isLiked) {
                songDao.setLiked(song.id, true)
            }
            count++
        }
        
        return count
    }
    
    /**
     * Sync playlists from YouTube Music
     */
    private suspend fun syncPlaylistsFromYouTube(): Int {
        val playlists: List<Playlist> = youTubeClient.getUserPlaylists().getOrNull() ?: return 0
        
        var count = 0
        for (playlist in playlists) {
            // Check if playlist exists locally
            val existingPlaylist = playlistDao.getPlaylistByYouTubeId(playlist.id)
            
            if (existingPlaylist == null) {
                // Create new playlist
                val newPlaylist = PlaylistEntity(
                    title = playlist.name,
                    description = playlist.description,
                    thumbnailUrl = playlist.artworkUrl,
                    youTubePlaylistId = playlist.id,
                    songCount = playlist.songCount,
                    isFromYouTube = true
                )
                val playlistId = playlistDao.insert(newPlaylist)
                
                // Sync playlist songs
                syncPlaylistSongs(playlistId, playlist.id)
            } else {
                // Update existing playlist
                playlistDao.update(existingPlaylist.copy(
                    title = playlist.name,
                    description = playlist.description,
                    thumbnailUrl = playlist.artworkUrl,
                    songCount = playlist.songCount,
                    updatedAt = System.currentTimeMillis()
                ))
            }
            count++
        }
        
        return count
    }
    
    /**
     * Sync songs within a playlist
     */
    private suspend fun syncPlaylistSongs(localPlaylistId: Long, youTubePlaylistId: String) {
        val songs = youTubeClient.getPlaylistSongs(youTubePlaylistId).getOrNull() ?: return
        
        songs.forEachIndexed { index, song ->
            // Ensure song exists in database
            if (songDao.getSongById(song.id) == null) {
                songDao.insert(SongEntity.fromSong(song))
            }
            
            // Add to playlist
            playlistDao.addSongToPlaylist(
                com.reon.music.data.database.entities.PlaylistSongCrossRef(
                    playlistId = localPlaylistId,
                    songId = song.id,
                    position = index
                )
            )
        }
    }
    
    /**
     * Sync listening history from YouTube Music
     */
    private suspend fun syncHistoryFromYouTube(): Int {
        // Would fetch history from YouTube Music
        // This requires additional API endpoints
        return 0
    }
    
    /**
     * Upload local data to YouTube Music ("Send back to Google")
     */
    private suspend fun uploadLocalDataToYouTube() {
        // Upload liked songs
        val localLikedSongs: List<SongEntity> = songDao.getLikedSongsOnce()
        for (song in localLikedSongs) {
            if (song.source == "youtube") {
                youTubeClient.likeSong(song.id)
            }
        }
        
        // Upload local playlists (not from YouTube)
        val localPlaylists: List<PlaylistEntity> = playlistDao.getLocalPlaylistsOnce()
        for (playlist in localPlaylists) {
            if (!playlist.isFromYouTube && playlist.youTubePlaylistId == null) {
                // Create playlist on YouTube
                val ytPlaylistId = youTubeClient.createPlaylist(
                    playlist.title,
                    playlist.description ?: ""
                ).let { result -> 
                    if (result is com.reon.music.core.common.Result.Success) result.data else null
                }
                
                if (ytPlaylistId != null) {
                    // Update local playlist with YouTube ID
                    playlistDao.update(playlist.copy(youTubePlaylistId = ytPlaylistId))
                    
                    // Add songs to YouTube playlist
                    val songs: List<SongEntity> = playlistDao.getPlaylistSongsOnce(playlist.id)
                    for (song in songs) {
                        if (song.source == "youtube") {
                            youTubeClient.addToPlaylist(ytPlaylistId, song.id)
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Clear sync error
     */
    fun clearError() {
        _syncState.value = _syncState.value.copy(error = null)
    }
}
