/*
 * REON Music App - Player ViewModel
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 */

package com.reon.music.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reon.music.core.model.Song
import com.reon.music.data.network.StreamResolver
import com.reon.music.data.repository.MusicRepository
import com.reon.music.playback.PlayerController
import com.reon.music.playback.PlayerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val showPlayer: Boolean = false,
    val isLiked: Boolean = false,
    val userPlaylists: List<com.reon.music.data.database.entities.PlaylistEntity> = emptyList()
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerController: PlayerController,
    private val streamResolver: StreamResolver,
    private val repository: MusicRepository,
    private val songDao: com.reon.music.data.database.dao.SongDao,
    private val playlistDao: com.reon.music.data.database.dao.PlaylistDao,
    private val downloadManager: com.reon.music.services.DownloadManager,
    private val discordRichPresence: com.reon.music.services.DiscordRichPresence,
    private val aiSuggestions: com.reon.music.services.AISongSuggestions,
    private val smartOfflineCache: com.reon.music.services.SmartOfflineCache
) : ViewModel() {
    
    companion object {
        private const val TAG = "PlayerViewModel"
    }

    
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()
    
    val playerState: StateFlow<PlayerState> = playerController.playerState
    
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()
    
    private var positionUpdateJob: Job? = null
    
    init {
        startPositionUpdates()
        observeCurrentSong()
        loadPlaylists()
        observeDiscordPresence()
    }
    
    /**
     * Update Discord Rich Presence when song changes
     */
    private fun observeDiscordPresence() {
        viewModelScope.launch {
            playerController.playerState.collect { state ->
                state.currentSong?.let { song ->
                    discordRichPresence.updatePresence(
                        songTitle = song.title,
                        artist = song.artist,
                        albumArtUrl = song.getHighQualityArtwork(),
                        isPlaying = state.isPlaying,
                        position = _currentPosition.value,
                        duration = (song.duration * 1000).toLong()
                    )
                } ?: discordRichPresence.clearPresence()
            }
        }
    }
    
    /**
     * Get AI song suggestions for current song
     */
    fun getAISuggestions(): Flow<List<Song>> = flow {
        val currentSong = playerState.value.currentSong
        if (currentSong != null) {
            val suggestions = aiSuggestions.getSuggestions(currentSong)
            emit(suggestions)
        } else {
            emit(emptyList())
        }
    }
    
    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = viewModelScope.launch {
            while (isActive) {
                val position = playerController.getCurrentPosition()
                _currentPosition.value = position
                
                // Track playback for smart offline caching
                playerState.value.currentSong?.let { song ->
                    val duration = song.duration * 1000L
                    if (duration > 0) {
                        smartOfflineCache.trackPlayback(song, position, duration)
                    }
                }
                
                delay(500) // Update every 500ms
            }
        }
    }
    
    private fun observeCurrentSong() {
        viewModelScope.launch {
            var previousSong: Song? = null
            
            playerController.playerState.collect { state ->
                state.currentSong?.let { song ->
                    checkIfLiked(song.id)
                    
                    // If song changed, mark previous song as completed
                    if (previousSong != null && previousSong?.id != song.id) {
                        previousSong?.let { completedSong ->
                            smartOfflineCache.markSongCompleted(completedSong)
                        }
                    }
                    
                    previousSong = song
                }
            }
        }
    }
    
    /**
     * Handle song completion (called when song ends)
     */
    fun onSongCompleted() {
        viewModelScope.launch {
            playerState.value.currentSong?.let { song ->
                smartOfflineCache.markSongCompleted(song)
            }
        }
    }
    
    private fun loadPlaylists() {
        viewModelScope.launch {
            playlistDao.getAllPlaylists().collect { playlists ->
                _uiState.value = _uiState.value.copy(userPlaylists = playlists)
            }
        }
    }
    
    private suspend fun checkIfLiked(songId: String) {
        val isLiked = songDao.isLiked(songId)
        _uiState.value = _uiState.value.copy(isLiked = isLiked)
    }

    fun toggleLike() {
        val song = playerState.value.currentSong ?: return
        viewModelScope.launch {
            val current = songDao.getSongById(song.id)
            val newLikedState = !(current?.isLiked ?: false)
            
            if (current == null) {
                songDao.insert(com.reon.music.data.database.entities.SongEntity.fromSong(song, isLiked = true))
            } else {
                songDao.setLiked(song.id, newLikedState)
            }
            // Update state immediately
            _uiState.value = _uiState.value.copy(isLiked = newLikedState)
        }
    }
    
    fun addToPlaylist(playlistId: Long) {
        val song = playerState.value.currentSong ?: return
        viewModelScope.launch {
            if (songDao.getSongById(song.id) == null) {
                songDao.insert(com.reon.music.data.database.entities.SongEntity.fromSong(song))
            }
            
            val maxPos = playlistDao.getMaxPosition(playlistId) ?: -1
            playlistDao.addSongToPlaylist(
                com.reon.music.data.database.entities.PlaylistSongCrossRef(
                    playlistId = playlistId,
                    songId = song.id,
                    position = maxPos + 1
                )
            )
            playlistDao.updatePlaylistStats(playlistId)
        }
    }
    
    /**
     * Play a song - resolves stream URL and plays with retry logic
     */
    fun playSong(song: Song, retryCount: Int = 0) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                Log.d(TAG, "Playing song: ${song.title} (${song.source}) - attempt ${retryCount + 1}")
                Log.d(TAG, "Existing streamUrl: ${song.streamUrl?.take(100) ?: "null"}")
                
                val streamUrl = streamResolver.resolveStreamUrl(song)
                
                if (streamUrl != null) {
                    Log.d(TAG, "Resolved stream URL: ${streamUrl.take(100)}...")
                    Log.d(TAG, "Starting playback...")
                    playerController.playSong(song, streamUrl)
                    _uiState.value = _uiState.value.copy(isLoading = false, showPlayer = true)
                    Log.d(TAG, "Playback started successfully")
                } else {
                    Log.e(TAG, "Could not resolve stream URL")
                    
                    // Retry up to 2 times
                    if (retryCount < 2) {
                        Log.d(TAG, "Retrying...")
                        delay(1000) // Wait 1 second before retry
                        playSong(song, retryCount + 1)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Could not play: ${song.title}. Please try again."
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error playing song", e)
                
                // Retry on exception
                if (retryCount < 2) {
                    Log.d(TAG, "Retrying after error...")
                    delay(1000)
                    playSong(song, retryCount + 1)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Error playing: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Play a list of songs
     */
    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        viewModelScope.launch {
            if (songs.isEmpty()) return@launch
            
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // First, resolve and play the current song
                val currentSong = songs[startIndex]
                val streamUrl = streamResolver.resolveStreamUrl(currentSong)
                
                if (streamUrl != null) {
                    val streamUrls = mutableMapOf(currentSong.id to streamUrl)
                    playerController.playQueue(songs, startIndex, streamUrls)
                    _uiState.value = _uiState.value.copy(isLoading = false, showPlayer = true)
                    
                    // Pre-resolve next songs in background
                    songs.forEachIndexed { index, song ->
                        if (index != startIndex && index < startIndex + 5) {
                            launch {
                                streamResolver.resolveStreamUrl(song)?.let { url ->
                                    // Cache the URL for later
                                }
                            }
                        }
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Could not play: ${currentSong.title}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error: ${e.message}"
                )
            }
        }
    }
    
    fun togglePlayPause() {
        playerController.togglePlayPause()
    }
    
    fun play() {
        playerController.play()
    }
    
    fun pause() {
        playerController.pause()
    }
    
    fun skipToNext() {
        viewModelScope.launch {
            val state = playerState.value
            val nextIndex = state.currentIndex + 1
            val nextSong = state.queue.getOrNull(nextIndex)
            
            if (nextSong != null) {
                // Resolve URL for next song if needed
                val url = streamResolver.resolveStreamUrl(nextSong)
                if (url != null) {
                    playerController.skipToNext()
                }
            } else {
                playerController.skipToNext()
            }
        }
    }
    
    fun skipToPrevious() {
        playerController.skipToPrevious()
    }
    
    fun seekTo(position: Long) {
        playerController.seekTo(position)
        _currentPosition.value = position
    }
    
    fun seekToPercent(percent: Float) {
        val duration = playerState.value.duration
        if (duration > 0) {
            val position = (duration * percent).toLong()
            seekTo(position)
        }
    }
    
    fun toggleShuffle() {
        playerController.toggleShuffle()
    }
    
    fun toggleRepeat() {
        playerController.toggleRepeatMode()
    }
    
    fun showPlayer() {
        _uiState.value = _uiState.value.copy(showPlayer = true)
    }
    
    fun hidePlayer() {
        _uiState.value = _uiState.value.copy(showPlayer = false)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Get progress as percentage (0-1)
     */
    fun getProgress(): Float {
        val position = currentPosition.value
        val duration = playerState.value.duration
        return if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
    }
    
    override fun onCleared() {
        super.onCleared()
        positionUpdateJob?.cancel()
    }
    
    fun downloadSong() {
        val song = playerState.value.currentSong ?: return
        downloadSong(song)
    }
    
    fun downloadSong(song: Song) {
        viewModelScope.launch {
            try {
                // Initiate download via DownloadManager
                downloadManager.downloadSong(song)
                
                // Also ensure it's in DB
                if (songDao.getSongById(song.id) == null) {
                    songDao.insert(com.reon.music.data.database.entities.SongEntity.fromSong(song, isDownloaded = true))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting download", e)
            }
        }
    }
    
    /**
     * Play song from queue at specific index
     */
    fun playFromQueue(index: Int) {
        playerController.playFromQueue(index)
    }
    
    /**
     * Add song to queue - either at end or as next song
     * Note: playNext currently adds to queue end (player controller feature pending)
     */
    fun addToQueue(song: Song, playNext: Boolean = false) {
        viewModelScope.launch {
            try {
                val streamUrl = streamResolver.resolveStreamUrl(song)
                if (streamUrl != null) {
                    playerController.addToQueue(song, streamUrl)
                    Log.d(TAG, if (playNext) "Added as next: ${song.title}" else "Added to queue: ${song.title}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding to queue", e)
            }
        }
    }
    
    /**
     * Remove song from queue at specific index
     */
    fun removeFromQueue(index: Int) {
        playerController.removeFromQueue(index)
    }
    
    /**
     * Set playback speed (0.5x to 2.0x)
     */
    fun setPlaybackSpeed(speed: Float) {
        playerController.setPlaybackSpeed(speed)
    }
    
    /**
     * Set sleep timer in minutes
     */
    fun setSleepTimer(minutes: Int) {
        viewModelScope.launch {
            delay(minutes * 60 * 1000L)
            if (isActive) {
                playerController.pause()
            }
        }
    }
    
    /**
     * Create a new playlist
     */
    fun createPlaylist(name: String, description: String? = null) {
        viewModelScope.launch {
            val playlist = com.reon.music.data.database.entities.PlaylistEntity(
                title = name,
                description = description
            )
            playlistDao.insert(playlist)
            loadPlaylists() // Reload playlists
        }
    }
    
    /**
     * Cycle through repeat modes: OFF -> ALL -> ONE -> OFF
     */
    fun cycleRepeatMode() {
        playerController.toggleRepeatMode()
    }
    
    /**
     * Enable radio mode - plays endless songs automatically with smart auto-queue
     */
    fun enableRadioMode(seedSongs: List<Song>) {
        viewModelScope.launch {
            // Start with seed songs
            if (seedSongs.isNotEmpty()) {
                playQueue(seedSongs)
            }
            
            // Set up automatic queue extension with smart queue management
            playerController.enableRadioMode { currentSong ->
                if (currentSong != null) {
                    viewModelScope.launch {
                        // Get multiple sources for variety
                        val relatedSongs = repository.getRelatedSongs(currentSong, 30).getOrNull() ?: emptyList()
                        val artistSongs = if (currentSong.artist.isNotBlank()) {
                            repository.searchSongsWithLimit("${currentSong.artist} songs", 20).getOrNull() ?: emptyList()
                        } else emptyList()
                        
                        val genreSongs = if (currentSong.genre.isNotBlank()) {
                            repository.searchSongsWithLimit("${currentSong.genre} songs", 20).getOrNull() ?: emptyList()
                        } else emptyList()
                        
                        // Combine and shuffle for variety
                        val allNewSongs = (relatedSongs + artistSongs + genreSongs)
                            .distinctBy { it.id }
                            .filter { it.id != currentSong.id }
                            .shuffled()
                            .take(25) // Add 25 songs at a time
                        
                        // Add to queue
                        allNewSongs.forEach { song ->
                            addToQueue(song)
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Disable radio mode
     */
    fun disableRadioMode() {
        playerController.disableRadioMode()
    }
}
