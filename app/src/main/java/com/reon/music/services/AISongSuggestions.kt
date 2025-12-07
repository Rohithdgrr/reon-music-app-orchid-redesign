/*
 * REON Music App - AI Song Suggestions
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 */

package com.reon.music.services

import android.util.Log
import com.reon.music.core.model.Song
import com.reon.music.data.database.dao.SongDao
import com.reon.music.data.database.entities.SongEntity
import com.reon.music.data.repository.MusicRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI Song Suggestions Manager
 * Provides intelligent song recommendations based on listening history
 * 
 * Uses collaborative filtering and content-based filtering
 */
@Singleton
class AISongSuggestions @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    private val songDao: SongDao,
    private val repository: MusicRepository
) {
    companion object {
        private const val TAG = "AISongSuggestions"
        private const val SUGGESTION_LIMIT = 20
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _isEnabled = MutableStateFlow(true)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()
    
    /**
     * Get song suggestions based on current song
     */
    suspend fun getSuggestions(currentSong: Song): List<Song> {
        if (!_isEnabled.value) return emptyList()
        
        return try {
            // Strategy 1: Get related songs from same artist
            val artistSongs = repository.getRelatedSongs(currentSong).getOrNull() ?: emptyList()
            
            // Strategy 2: Get songs from same genre
            val genreSongs = if (currentSong.genre.isNotBlank()) {
                repository.getSongsByGenre(currentSong.genre).getOrNull() ?: emptyList()
            } else {
                emptyList()
            }
            
            // Strategy 3: Get songs from listening history (collaborative filtering)
            val historySongs = songDao.getRecentlyPlayed(50)
                .first()
                .mapNotNull { it.toSong() }
                .filter { it.genre == currentSong.genre || it.artist == currentSong.artist }
            
            // Combine and deduplicate
            val allSuggestions = (artistSongs + genreSongs + historySongs)
                .distinctBy { it.id }
                .filter { it.id != currentSong.id }
                .shuffled()
                .take(SUGGESTION_LIMIT)
            
            Log.d(TAG, "Generated ${allSuggestions.size} suggestions for ${currentSong.title}")
            allSuggestions
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating suggestions", e)
            emptyList()
        }
    }
    
    /**
     * Get personalized recommendations based on listening history
     */
    suspend fun getPersonalizedRecommendations(): List<Song> {
        if (!_isEnabled.value) return emptyList()
        
        return try {
            // Get user's listening history
            val history = songDao.getRecentlyPlayed(100)
                .first()
                .mapNotNull { it.toSong() }
            
            if (history.isEmpty()) {
                // Fallback to trending songs
                return repository.getTrendingSongs().getOrNull() ?: emptyList()
            }
            
            // Analyze listening patterns
            val topGenres = history.groupBy { it.genre }
                .filterKeys { it.isNotBlank() }
                .toList()
                .sortedByDescending { it.second.size }
                .take(3)
                .map { it.first }
            
            val topArtists = history.groupBy { it.artist }
                .toList()
                .sortedByDescending { it.second.size }
                .take(5)
                .map { it.first }
            
            // Get recommendations based on patterns
            val recommendations = mutableListOf<Song>()
            
            topGenres.forEach { genre ->
                repository.getSongsByGenre(genre).getOrNull()?.let {
                    recommendations.addAll(it)
                }
            }
            
            // Get songs from top artists (simplified - would need artist details endpoint)
            topArtists.forEach { artistName ->
                repository.searchSongsWithLimit("$artistName songs", 10).getOrNull()?.let {
                    recommendations.addAll(it)
                }
            }
            
            // Remove already played songs and deduplicate
            val playedIds = history.map { it.id }.toSet()
            recommendations
                .distinctBy { it.id }
                .filter { it.id !in playedIds }
                .shuffled()
                .take(SUGGESTION_LIMIT)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating personalized recommendations", e)
            emptyList()
        }
    }
    
    /**
     * Enable/disable AI suggestions
     */
    fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
    }
}

