/*
 * REON Music App - Library ViewModel
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 */

package com.reon.music.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reon.music.core.model.Song
import com.reon.music.data.database.dao.HistoryDao
import com.reon.music.data.database.dao.PlaylistDao
import com.reon.music.data.database.dao.SongDao
import com.reon.music.data.database.entities.PlaylistEntity
import com.reon.music.data.database.entities.PlaylistSongCrossRef
import com.reon.music.data.database.entities.SongEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File

data class LibraryUiState(
    val likedSongs: List<Song> = emptyList(),
    val recentlyPlayed: List<Song> = emptyList(),
    val playlists: List<PlaylistEntity> = emptyList(),
    val downloadedSongs: List<Song> = emptyList(),
    val likedCount: Int = 0,
    val downloadedCount: Int = 0,
    val playlistCount: Int = 0,
    val isLoading: Boolean = true,
    val selectedTab: LibraryTab = LibraryTab.OVERVIEW,
    val showCreatePlaylistDialog: Boolean = false,
    val error: String? = null
)

enum class LibraryTab {
    OVERVIEW, LIKED, PLAYLISTS, DOWNLOADS, HISTORY
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao,
    private val historyDao: HistoryDao,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()
    
    init {
        loadLibrary()
    }
    
    private fun loadLibrary() {
        viewModelScope.launch {
            // Combine in groups to avoid too many arguments
            val songsFlow = combine(
                songDao.getLikedSongs(),
                songDao.getRecentlyPlayed(20),
                songDao.getDownloadedSongs()
            ) { liked, recent, downloaded ->
                Triple(liked, recent, downloaded)
            }
            
            val countsFlow = combine(
                songDao.getLikedCount(),
                songDao.getDownloadedCount(),
                playlistDao.getPlaylistCount()
            ) { likedCount, downloadedCount, playlistCount ->
                Triple(likedCount, downloadedCount, playlistCount)
            }
            
            combine(
                songsFlow,
                countsFlow,
                playlistDao.getAllPlaylists()
            ) { songs, counts, playlists ->
                val (liked, recent, downloaded) = songs
                val (likedCount, downloadedCount, playlistCount) = counts
                
                LibraryUiState(
                    likedSongs = liked.map { entity -> entity.toSong() },
                    recentlyPlayed = recent.map { entity -> entity.toSong() },
                    playlists = playlists,
                    downloadedSongs = downloaded.map { entity -> entity.toSong() },
                    likedCount = likedCount,
                    downloadedCount = downloadedCount,
                    playlistCount = playlistCount,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state.copy(
                    selectedTab = _uiState.value.selectedTab,
                    showCreatePlaylistDialog = _uiState.value.showCreatePlaylistDialog
                )
            }
        }
    }
    
    fun selectTab(tab: LibraryTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }
    
    fun toggleLike(song: Song) {
        viewModelScope.launch {
            val current = songDao.getSongById(song.id)
            val newLikedState = !(current?.isLiked ?: false)
            
            if (current == null) {
                // Add to database first
                songDao.insert(SongEntity.fromSong(song, isLiked = newLikedState))
            } else {
                songDao.setLiked(song.id, newLikedState)
            }
        }
    }
    
    fun showCreatePlaylistDialog() {
        _uiState.value = _uiState.value.copy(showCreatePlaylistDialog = true)
    }
    
    fun hideCreatePlaylistDialog() {
        _uiState.value = _uiState.value.copy(showCreatePlaylistDialog = false)
    }
    
    fun createPlaylist(name: String, description: String? = null) {
        viewModelScope.launch {
            val playlist = PlaylistEntity(
                title = name,
                description = description
            )
            playlistDao.insert(playlist)
            hideCreatePlaylistDialog()
        }
    }
    
    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            playlistDao.deleteById(playlistId)
        }
    }
    
    fun addToPlaylist(playlistId: Long, song: Song) {
        viewModelScope.launch {
            // First ensure song is in database
            if (songDao.getSongById(song.id) == null) {
                songDao.insert(SongEntity.fromSong(song))
            }
            
            // Get max position
            val maxPos = playlistDao.getMaxPosition(playlistId) ?: -1
            
            // Add to playlist
            playlistDao.addSongToPlaylist(
                PlaylistSongCrossRef(
                    playlistId = playlistId,
                    songId = song.id,
                    position = maxPos + 1
                )
            )
            
            // Update playlist stats
            playlistDao.updatePlaylistStats(playlistId)
        }
    }
    
    fun removeFromPlaylist(playlistId: Long, songId: String) {
        viewModelScope.launch {
            playlistDao.removeSongFromPlaylist(playlistId, songId)
            playlistDao.updatePlaylistStats(playlistId)
        }
    }
    
    /**
     * Remove a downloaded song and delete the actual file
     */
    fun removeDownload(song: Song) {
        viewModelScope.launch {
            // Get the song entity to find the download path
            val songEntity = songDao.getSongById(song.id)
            
            // Delete the file if it exists
            if (!songEntity?.localPath.isNullOrBlank()) {
                try {
                    val file = File(songEntity!!.localPath!!)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    // Log error but continue with database update
                    e.printStackTrace()
                }
            } else {
                // Try to find file in downloads directory
                try {
                    val downloadsDir = File(context.getExternalFilesDir(null), "downloads")
                    if (downloadsDir.exists()) {
                        // Find files matching the song title
                        val matchingFiles = downloadsDir.listFiles { file ->
                            file.name.contains(song.title.take(20), ignoreCase = true)
                        }
                        matchingFiles?.forEach { it.delete() }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            // Update database
            songDao.updateDownloadState(song.id, 0, null)
        }
    }
    
    /**
     * Remove song from library
     */
    fun removeSongFromLibrary(song: Song) {
        viewModelScope.launch {
            songDao.setLiked(song.id, false)
        }
    }
    
    /**
     * Delete playlist by entity
     */
    fun deletePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            playlistDao.deleteById(playlist.id)
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
