/*
 * REON Music App - Enhanced Search ViewModel
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 */

package com.reon.music.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reon.music.core.common.Result
import com.reon.music.core.model.Album
import com.reon.music.core.model.Artist
import com.reon.music.core.model.Playlist
import com.reon.music.core.model.Song
import com.reon.music.data.network.jiosaavn.JioSaavnClient
import com.reon.music.data.network.youtube.YouTubeMusicClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val searchHistory: List<String> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val activeFilter: SearchFilter = SearchFilter.ALL,
    val sortBy: SearchSortBy = SearchSortBy.RELEVANCE,
    val error: String? = null,
    val hasSearched: Boolean = false
)

enum class SearchFilter {
    ALL, SONGS, ALBUMS, ARTISTS, PLAYLISTS
}

enum class SearchSortBy {
    RELEVANCE, DURATION_ASC, DURATION_DESC, TITLE_ASC, TITLE_DESC, DATE_NEWEST, DATE_OLDEST
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val jioSaavnClient: JioSaavnClient,
    private val youTubeClient: YouTubeMusicClient
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    
    private var searchJob: Job? = null
    private val searchHistoryList = mutableListOf<String>()
    
    companion object {
        private const val DEBOUNCE_MS = 300L
        private const val MAX_HISTORY_ITEMS = 20
    }
    
    init {
        loadSearchHistory()
    }
    
    fun updateQuery(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        
        // Debounced search suggestions
        searchJob?.cancel()
        if (query.isNotBlank() && query.length >= 2) {
            searchJob = viewModelScope.launch {
                delay(DEBOUNCE_MS)
                loadSuggestions(query)
            }
        } else {
            _uiState.value = _uiState.value.copy(suggestions = emptyList())
        }
    }
    
    fun search(query: String = _uiState.value.query) {
        if (query.isBlank()) return
        
        _uiState.value = _uiState.value.copy(
            query = query,
            isLoading = true,
            error = null,
            hasSearched = true
        )
        
        // Add to history
        addToHistory(query)
        
        viewModelScope.launch {
            try {
                // Search both sources in parallel
                val jioSaavnDeferred = async { jioSaavnClient.searchSongs(query) }
                val youtubeDeferred = async { youTubeClient.searchSongs(query) }
                
                val jioSaavnResult = jioSaavnDeferred.await()
                val youtubeResult = youtubeDeferred.await()
                
                // Combine results
                val allSongs = mutableListOf<Song>()
                
                if (jioSaavnResult is Result.Success) {
                    allSongs.addAll(jioSaavnResult.data)
                }
                
                if (youtubeResult is Result.Success) {
                    allSongs.addAll(youtubeResult.data)
                }
                
                // Apply filters and sorting
                val filteredSongs = applySorting(allSongs)
                
                _uiState.value = _uiState.value.copy(
                    songs = filteredSongs,
                    isLoading = false,
                    suggestions = emptyList()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Search failed: ${e.message}"
                )
            }
        }
    }
    
    fun setFilter(filter: SearchFilter) {
        _uiState.value = _uiState.value.copy(activeFilter = filter)
    }
    
    fun setSortBy(sortBy: SearchSortBy) {
        _uiState.value = _uiState.value.copy(sortBy = sortBy)
        // Re-apply sorting
        val sortedSongs = applySorting(_uiState.value.songs)
        _uiState.value = _uiState.value.copy(songs = sortedSongs)
    }
    
    private fun applySorting(songs: List<Song>): List<Song> {
        return when (_uiState.value.sortBy) {
            SearchSortBy.RELEVANCE -> songs
            SearchSortBy.DURATION_ASC -> songs.sortedBy { it.duration }
            SearchSortBy.DURATION_DESC -> songs.sortedByDescending { it.duration }
            SearchSortBy.TITLE_ASC -> songs.sortedBy { it.title.lowercase() }
            SearchSortBy.TITLE_DESC -> songs.sortedByDescending { it.title.lowercase() }
            SearchSortBy.DATE_NEWEST -> songs.sortedByDescending { it.releaseDate }
            SearchSortBy.DATE_OLDEST -> songs.sortedBy { it.releaseDate }
        }
    }
    
    private suspend fun loadSuggestions(query: String) {
        // Search suggestions not yet implemented
        // Would call API endpoint when available
        _uiState.value = _uiState.value.copy(suggestions = emptyList())
    }
    
    private fun loadSearchHistory() {
        // In production, load from DataStore
        _uiState.value = _uiState.value.copy(searchHistory = searchHistoryList.toList())
    }
    
    private fun addToHistory(query: String) {
        searchHistoryList.remove(query) // Remove if exists
        searchHistoryList.add(0, query) // Add to front
        
        // Limit history size
        while (searchHistoryList.size > MAX_HISTORY_ITEMS) {
            searchHistoryList.removeAt(searchHistoryList.lastIndex)
        }
        
        _uiState.value = _uiState.value.copy(searchHistory = searchHistoryList.toList())
    }
    
    fun removeFromHistory(query: String) {
        searchHistoryList.remove(query)
        _uiState.value = _uiState.value.copy(searchHistory = searchHistoryList.toList())
    }
    
    fun clearHistory() {
        searchHistoryList.clear()
        _uiState.value = _uiState.value.copy(searchHistory = emptyList())
    }
    
    fun clearSearch() {
        _uiState.value = SearchUiState(searchHistory = searchHistoryList.toList())
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
