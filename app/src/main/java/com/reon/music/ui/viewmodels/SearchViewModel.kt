/*
 * REON Music App - Enhanced Search ViewModel
 * Copyright (c) 2024 REON
 * YouTube Music Only - Real-time Search
 */

package com.reon.music.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reon.music.core.common.Result
import com.reon.music.core.model.Album
import com.reon.music.core.model.Artist
import com.reon.music.core.model.Song
import com.reon.music.data.network.youtube.YouTubeMusicClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
    val searchHistory: List<String> = emptyList(),
    val activeFilter: SearchFilter = SearchFilter.ALL,
    val error: String? = null,
    val hasSearched: Boolean = false
)

enum class SearchFilter {
    ALL, SONGS, ALBUMS, ARTISTS
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val youTubeClient: YouTubeMusicClient
) : ViewModel() {
    
    companion object {
        private const val TAG = "SearchViewModel"
        private const val DEBOUNCE_MS = 400L
    }
    
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    
    private var searchJob: Job? = null
    private val searchHistoryList = mutableListOf<String>()
    
    /**
     * Update query and trigger real-time search
     */
    fun updateQuery(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        
        if (query.isBlank()) {
            // Clear results when query is empty
            _uiState.value = _uiState.value.copy(
                songs = emptyList(),
                albums = emptyList(),
                artists = emptyList(),
                hasSearched = false,
                isLoading = false
            )
            return
        }
        
        // Debounced search - search as user types
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            performSearch(query)
        }
    }
    
    /**
     * Perform immediate search (e.g., when user presses search button)
     */
    fun search(query: String) {
        if (query.isBlank()) return
        
        searchJob?.cancel()
        addToHistory(query)
        
        searchJob = viewModelScope.launch {
            performSearch(query)
        }
    }
    
    /**
     * Internal search function - uses YouTube Music only
     */
    private suspend fun performSearch(query: String) {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            hasSearched = true,
            error = null
        )
        
        try {
            // Search for songs using YouTube Music
            val songsResult = youTubeClient.searchSongs(query)
            val songs = when (songsResult) {
                is Result.Success -> songsResult.data
                is Result.Error -> {
                    Log.e(TAG, "Error searching songs: ${songsResult.message}")
                    emptyList()
                }
                is Result.Loading -> emptyList()
            }
            
            // Extract unique artists from songs
            val artists = songs
                .map { song -> 
                    Artist(
                        id = song.artist.hashCode().toString(),
                        name = song.artist,
                        artworkUrl = song.artworkUrl
                    )
                }
                .distinctBy { it.name }
                .take(10)
            
            // Extract unique albums from songs
            val albums = songs
                .filter { it.album.isNotBlank() }
                .map { song ->
                    Album(
                        id = song.album.hashCode().toString(),
                        name = song.album,
                        artist = song.artist,
                        artworkUrl = song.artworkUrl
                    )
                }
                .distinctBy { it.name }
                .take(10)
            
            _uiState.value = _uiState.value.copy(
                songs = songs,
                albums = albums,
                artists = artists,
                isLoading = false,
                error = if (songs.isEmpty()) "No results found" else null
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Search error", e)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = e.message ?: "Search failed"
            )
        }
    }
    
    fun setFilter(filter: SearchFilter) {
        _uiState.value = _uiState.value.copy(activeFilter = filter)
    }
    
    private fun addToHistory(query: String) {
        if (query.isNotBlank() && !searchHistoryList.contains(query)) {
            searchHistoryList.add(0, query)
            if (searchHistoryList.size > 10) {
                searchHistoryList.removeLast()
            }
            _uiState.value = _uiState.value.copy(searchHistory = searchHistoryList.toList())
        }
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
        searchJob?.cancel()
        _uiState.value = SearchUiState(searchHistory = searchHistoryList.toList())
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
