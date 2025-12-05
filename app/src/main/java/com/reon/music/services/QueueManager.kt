/*
 * REON Music App - Queue Manager
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 */

package com.reon.music.services

import com.reon.music.core.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Queue data class
 */
data class QueueState(
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = -1,
    val originalQueue: List<Song> = emptyList(), // For shuffle restore
    val isShuffled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF
) {
    val currentSong: Song? get() = queue.getOrNull(currentIndex)
    val hasNext: Boolean get() = currentIndex < queue.size - 1 || repeatMode == RepeatMode.ALL
    val hasPrevious: Boolean get() = currentIndex > 0 || repeatMode == RepeatMode.ALL
    val isEmpty: Boolean get() = queue.isEmpty()
    val size: Int get() = queue.size
    
    fun upcomingSongs(): List<Song> {
        return if (currentIndex < queue.size - 1) {
            queue.subList(currentIndex + 1, queue.size)
        } else {
            emptyList()
        }
    }
}

enum class RepeatMode {
    OFF, ONE, ALL
}

/**
 * Queue Manager
 * Manages the playback queue with shuffle, repeat, etc.
 */
@Singleton
class QueueManager @Inject constructor() {
    
    private val _queueState = MutableStateFlow(QueueState())
    val queueState: StateFlow<QueueState> = _queueState.asStateFlow()
    
    /**
     * Set queue with songs and start at given index
     */
    fun setQueue(songs: List<Song>, startIndex: Int = 0) {
        _queueState.value = QueueState(
            queue = songs,
            originalQueue = songs,
            currentIndex = startIndex.coerceIn(0, songs.size - 1),
            isShuffled = false
        )
    }
    
    /**
     * Add song to end of queue
     */
    fun addToQueue(song: Song) {
        val current = _queueState.value
        _queueState.value = current.copy(
            queue = current.queue + song,
            originalQueue = current.originalQueue + song
        )
    }
    
    /**
     * Add song to play next
     */
    fun addToPlayNext(song: Song) {
        val current = _queueState.value
        val insertIndex = (current.currentIndex + 1).coerceIn(0, current.queue.size)
        val newQueue = current.queue.toMutableList().apply { add(insertIndex, song) }
        val newOriginal = current.originalQueue.toMutableList().apply { add(song) }
        
        _queueState.value = current.copy(
            queue = newQueue,
            originalQueue = newOriginal
        )
    }
    
    /**
     * Remove song from queue by index
     */
    fun removeFromQueue(index: Int) {
        val current = _queueState.value
        if (index !in current.queue.indices) return
        
        val newQueue = current.queue.toMutableList().apply { removeAt(index) }
        val newIndex = when {
            newQueue.isEmpty() -> -1
            index < current.currentIndex -> current.currentIndex - 1
            index == current.currentIndex && index >= newQueue.size -> newQueue.size - 1
            else -> current.currentIndex
        }
        
        _queueState.value = current.copy(
            queue = newQueue,
            currentIndex = newIndex
        )
    }
    
    /**
     * Move song in queue
     */
    fun moveSong(fromIndex: Int, toIndex: Int) {
        val current = _queueState.value
        if (fromIndex !in current.queue.indices || toIndex !in 0..current.queue.size) return
        
        val newQueue = current.queue.toMutableList()
        val song = newQueue.removeAt(fromIndex)
        val adjustedToIndex = if (toIndex > fromIndex) toIndex - 1 else toIndex
        newQueue.add(adjustedToIndex, song)
        
        // Adjust current index
        val newIndex = when {
            fromIndex == current.currentIndex -> adjustedToIndex
            fromIndex < current.currentIndex && adjustedToIndex >= current.currentIndex -> current.currentIndex - 1
            fromIndex > current.currentIndex && adjustedToIndex <= current.currentIndex -> current.currentIndex + 1
            else -> current.currentIndex
        }
        
        _queueState.value = current.copy(
            queue = newQueue,
            currentIndex = newIndex
        )
    }
    
    /**
     * Skip to next song
     */
    fun skipToNext(): Song? {
        val current = _queueState.value
        if (current.isEmpty) return null
        
        val nextIndex = when {
            current.repeatMode == RepeatMode.ONE -> current.currentIndex
            current.currentIndex < current.queue.size - 1 -> current.currentIndex + 1
            current.repeatMode == RepeatMode.ALL -> 0
            else -> return null
        }
        
        _queueState.value = current.copy(currentIndex = nextIndex)
        return _queueState.value.currentSong
    }
    
    /**
     * Skip to previous song
     */
    fun skipToPrevious(): Song? {
        val current = _queueState.value
        if (current.isEmpty) return null
        
        val prevIndex = when {
            current.currentIndex > 0 -> current.currentIndex - 1
            current.repeatMode == RepeatMode.ALL -> current.queue.size - 1
            else -> 0
        }
        
        _queueState.value = current.copy(currentIndex = prevIndex)
        return _queueState.value.currentSong
    }
    
    /**
     * Skip to specific song in queue
     */
    fun skipToIndex(index: Int): Song? {
        val current = _queueState.value
        if (index !in current.queue.indices) return null
        
        _queueState.value = current.copy(currentIndex = index)
        return _queueState.value.currentSong
    }
    
    /**
     * Toggle shuffle mode
     */
    fun toggleShuffle() {
        val current = _queueState.value
        
        if (current.isShuffled) {
            // Unshuffle - restore original order
            val currentSong = current.currentSong
            val newIndex = currentSong?.let { current.originalQueue.indexOf(it) } ?: 0
            
            _queueState.value = current.copy(
                queue = current.originalQueue,
                currentIndex = newIndex.coerceIn(0, current.originalQueue.size - 1),
                isShuffled = false
            )
        } else {
            // Shuffle - keep current song at position 0
            val currentSong = current.currentSong
            val shuffled = current.queue.shuffled().toMutableList()
            
            currentSong?.let {
                shuffled.remove(it)
                shuffled.add(0, it)
            }
            
            _queueState.value = current.copy(
                queue = shuffled,
                currentIndex = 0,
                isShuffled = true
            )
        }
    }
    
    /**
     * Cycle through repeat modes
     */
    fun cycleRepeatMode(): RepeatMode {
        val current = _queueState.value
        val nextMode = when (current.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        
        _queueState.value = current.copy(repeatMode = nextMode)
        return nextMode
    }
    
    /**
     * Set repeat mode directly
     */
    fun setRepeatMode(mode: RepeatMode) {
        _queueState.value = _queueState.value.copy(repeatMode = mode)
    }
    
    /**
     * Clear the queue
     */
    fun clearQueue() {
        _queueState.value = QueueState()
    }
}
