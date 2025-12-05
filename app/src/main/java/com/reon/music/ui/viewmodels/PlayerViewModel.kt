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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val showPlayer: Boolean = false
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerController: PlayerController,
    private val streamResolver: StreamResolver,
    private val repository: MusicRepository
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
    }
    
    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = viewModelScope.launch {
            while (isActive) {
                _currentPosition.value = playerController.getCurrentPosition()
                delay(500) // Update every 500ms
            }
        }
    }
    
    /**
     * Play a song - resolves stream URL and plays
     */
    fun playSong(song: Song) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                Log.d(TAG, "Playing song: ${song.title}")
                
                val streamUrl = streamResolver.resolveStreamUrl(song)
                
                if (streamUrl != null) {
                    Log.d(TAG, "Got stream URL, playing...")
                    playerController.playSong(song, streamUrl)
                    _uiState.value = _uiState.value.copy(isLoading = false, showPlayer = true)
                } else {
                    Log.e(TAG, "Could not resolve stream URL")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Could not play: ${song.title}"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error playing song", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error: ${e.message}"
                )
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
}
