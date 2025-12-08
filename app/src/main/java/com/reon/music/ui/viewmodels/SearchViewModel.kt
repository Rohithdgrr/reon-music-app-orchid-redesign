/*
 * REON Music App - Enhanced Power Search ViewModel
 * Copyright (c) 2024 REON
 * YouTube Music Only - Multi-Language Real-time Search
 */

package com.reon.music.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reon.music.core.common.Result
import com.reon.music.core.model.Album
import com.reon.music.core.model.Artist
import com.reon.music.core.model.SearchResult
import com.reon.music.core.model.Song
import com.reon.music.data.network.youtube.YouTubeMusicClient
import com.reon.music.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.Calendar

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val searchHistory: List<String> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val trendingSearches: List<String> = emptyList(),
    val activeFilter: SearchFilter = SearchFilter.ALL,
    val error: String? = null,
    val hasSearched: Boolean = false,
    val hasMore: Boolean = true,
    val currentPage: Int = 1,
    val isUnlimitedMode: Boolean = false
)

enum class SearchFilter {
    ALL, SONGS, ALBUMS, ARTISTS, MOVIES
}

// Indian language search priorities
private val INDIAN_LANGUAGES = listOf(
    "telugu", "hindi", "tamil", "malayalam", "kannada", 
    "punjabi", "marathi", "bengali", "gujarati", "bhojpuri",
    "banjara", "rajasthani", "odia"
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val youTubeClient: YouTubeMusicClient,
    private val repository: MusicRepository
) : ViewModel() {
    
    companion object {
        private const val TAG = "SearchViewModel"
        private const val DEBOUNCE_MS = 400L
        private const val SUGGESTION_DEBOUNCE_MS = 300L
        private const val INITIAL_SEARCH_LIMIT = 120
        private const val MAX_RESULTS = 400
        private const val PAGE_SIZE = 30
    }
    
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    
    private var searchJob: Job? = null
    private var suggestionJob: Job? = null
    private var loadMoreJob: Job? = null
    private val searchHistoryList = mutableListOf<String>()
    private var allSearchResults = mutableListOf<Song>()
    
    init {
        loadTrendingSearches()
    }
    
    private fun loadTrendingSearches() {
        viewModelScope.launch {
            // Enhanced trending searches with Indian languages first
            val trending = listOf(
                "Telugu Songs", "Hindi Songs", "Tamil Hits",
                "Arijit Singh", "Devi Sri Prasad", "AR Rahman",
                "Pushpa", "RRR", "Pathaan", "Animal",
                "Romantic Songs", "Party Songs", "Sad Songs",
                "English Pop", "BTS", "Punjabi Hits"
            )
            _uiState.value = _uiState.value.copy(trendingSearches = trending)
        }
    }
    
    /**
     * Update query and trigger real-time search and suggestions
     */
    fun updateQuery(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        
        if (query.isBlank()) {
            // Clear results when query is empty
            _uiState.value = _uiState.value.copy(
                songs = emptyList(),
                albums = emptyList(),
                artists = emptyList(),
                suggestions = emptyList(),
                hasSearched = false,
                isLoading = false
            )
            allSearchResults.clear()
            return
        }
        
        // Get suggestions as user types
        suggestionJob?.cancel()
        suggestionJob = viewModelScope.launch {
            delay(SUGGESTION_DEBOUNCE_MS)
            loadSuggestions(query)
        }
        
        // Debounced search - search as user types
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            performPowerSearch(query)
        }
    }
    
    /**
     * Load search suggestions using autocomplete
     */
    private suspend fun loadSuggestions(query: String) {
        try {
            val result = repository.autocomplete(query)
            val suggestions = mutableListOf<String>()
            
            result.getOrNull()?.let { searchResult ->
                // Extract suggestions from autocomplete results
                searchResult.artists.take(3).forEach { suggestions.add(it.name) }
                searchResult.albums.take(3).forEach { suggestions.add(it.name) }
                searchResult.playlists.take(2).forEach { suggestions.add(it.name) }
            }
            
            // Add movie/album suggestions
            val movieSuggestions = listOf(
                "$query movie songs", "$query telugu", "$query hindi",
                "$query tamil", "$query album"
            )
            suggestions.addAll(movieSuggestions.take(3))
            
            // Also add matching history items
            searchHistoryList.filter { it.contains(query, ignoreCase = true) }
                .take(3)
                .forEach { suggestions.add(it) }
            
            _uiState.value = _uiState.value.copy(suggestions = suggestions.distinct().take(10))
        } catch (e: Exception) {
            Log.e(TAG, "Error loading suggestions", e)
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
            performPowerSearch(query)
        }
    }
    
    /**
     * POWER SEARCH - Multi-language, multi-source comprehensive search
     * Prioritizes Indian languages and searches by song, artist, album/movie
     */
    private suspend fun performPowerSearch(query: String) {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            hasSearched = true,
            hasMore = true,
            currentPage = 1,
            error = null
        )
        allSearchResults.clear()
        
        try {
            // Build comprehensive search queries
            val searchQueries = buildPowerSearchQueries(query)
            
            // Perform parallel searches for faster results
            val allSongs = mutableListOf<Song>()
            val searchResults = searchQueries.map { searchQuery ->
                viewModelScope.async {
                    try {
                        val result = youTubeClient.searchSongs(searchQuery)
                        when (result) {
                            is Result.Success -> result.data
                            else -> emptyList()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Search query failed: $searchQuery", e)
                        emptyList()
                    }
                }
            }
            
            // Collect all results
            searchResults.awaitAll().forEach { songs ->
                allSongs.addAll(songs)
            }
            
            // Remove duplicates and prioritize by relevance
            val uniqueSongs = allSongs
                .distinctBy { it.id }
                .sortedByDescending { song ->
                    calculateRelevanceScore(query, song)
                }
                .take(MAX_RESULTS)
            
            allSearchResults.addAll(uniqueSongs)
            
            // Extract unique artists from songs
            val artists = uniqueSongs
                .map { song -> 
                    Artist(
                        id = song.artist.hashCode().toString(),
                        name = song.artist,
                        artworkUrl = song.artworkUrl,
                        followerCount = song.channelSubscriberCount.toInt()
                    )
                }
                .distinctBy { it.name.lowercase() }
                .sortedByDescending { it.followerCount }
                .take(15)
            
            // Extract unique albums from songs
            val albums = uniqueSongs
                .filter { it.album.isNotBlank() }
                .map { song ->
                    Album(
                        id = song.album.hashCode().toString(),
                        name = song.album,
                        artist = song.artist,
                        artworkUrl = song.artworkUrl
                    )
                }
                .distinctBy { it.name.lowercase() }
                .take(15)
            
            _uiState.value = _uiState.value.copy(
                songs = uniqueSongs.take(INITIAL_SEARCH_LIMIT),
                albums = albums,
                artists = artists,
                isLoading = false,
                hasMore = uniqueSongs.size > INITIAL_SEARCH_LIMIT,
                error = if (uniqueSongs.isEmpty()) "No results found. Try searching with different keywords." else null
            )
            
            Log.d(TAG, "Power search completed: ${uniqueSongs.size} songs found for '$query'")
            
        } catch (e: Exception) {
            Log.e(TAG, "Search error", e)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = e.message ?: "Search failed"
            )
        }
    }
    
    /**
     * Build comprehensive search queries for power search
     * Prioritizes Indian languages and includes movie/album searches
     */
    private fun buildPowerSearchQueries(query: String): List<String> {
        val queries = mutableListOf<String>()
        
        // Primary query
        queries.add(query)
        
        // Movie/Album search variations
        queries.add("$query movie songs")
        queries.add("$query songs")
        queries.add("$query album")
        
        // Determine if query might be targeting specific language
        val isIndianLanguageQuery = INDIAN_LANGUAGES.any { 
            query.lowercase().contains(it) 
        }
        
        // Add Indian language variations for better coverage
        if (!isIndianLanguageQuery) {
            // Telugu - highest priority
            queries.add("$query telugu")
            queries.add("$query telugu songs")
            
            // Hindi - second priority
            queries.add("$query hindi")
            queries.add("$query hindi songs")
            
            // Tamil
            queries.add("$query tamil")
            
            // Punjabi
            queries.add("$query punjabi")
        }
        
        // Artist search
        queries.add("$query artist songs")
        
        return queries.distinct().take(8) // Limit to prevent too many parallel requests
    }
    
    /**
     * Calculate relevance score for sorting results
     * Enhanced algorithm for better ranking
     */
    private fun calculateRelevanceScore(query: String, song: Song): Int {
        var score = 0
        val lowerQuery = query.lowercase()
        val lowerTitle = song.title.lowercase()
        val lowerArtist = song.artist.lowercase()
        val lowerAlbum = song.album.lowercase()
        
        // === TITLE MATCHING (Highest Priority) ===
        when {
            lowerTitle == lowerQuery -> score += 200 // Exact match
            lowerTitle.startsWith(lowerQuery) -> score += 150 // Starts with query
            lowerTitle.contains(" $lowerQuery ") -> score += 120 // Word boundary match
            lowerTitle.contains(lowerQuery) -> score += 80 // Contains query
        }
        
        // === ARTIST MATCHING ===
        when {
            lowerArtist == lowerQuery -> score += 160 // Exact artist match
            lowerArtist.startsWith(lowerQuery) -> score += 110 // Artist starts with query
            lowerArtist.contains(lowerQuery) -> score += 60 // Artist contains query
        }
        
        // === ALBUM/MOVIE MATCHING ===
        when {
            lowerAlbum == lowerQuery -> score += 140 // Exact album match
            lowerAlbum.startsWith(lowerQuery) -> score += 90 // Album starts with query
            lowerAlbum.contains(lowerQuery) -> score += 50 // Album contains query
        }
        
        // === POPULARITY SCORING ===
        // View count (normalized, max 50 points)
        val viewScore = (song.viewCount / 1000000).toInt().coerceAtMost(50)
        score += viewScore
        
        // Like count (normalized, max 30 points)
        val likeScore = (song.likeCount / 100000).toInt().coerceAtMost(30)
        score += likeScore
        
        // Channel subscriber count (max 25 points)
        val channelScore = (song.channelSubscriberCount / 1000000).toInt().coerceAtMost(25)
        score += channelScore
        
        // === QUALITY SCORING ===
        if (song.is320kbps) score += 20 // High quality audio
        if (song.quality.contains("HD", ignoreCase = true)) score += 15
        if (song.quality.contains("4K", ignoreCase = true)) score += 25
        
        // === LANGUAGE PREFERENCE ===
        val language = song.language.lowercase()
        when {
            language.contains("telugu") -> score += 25 // Telugu highest priority
            language.contains("hindi") -> score += 20 // Hindi second
            language.contains("tamil") -> score += 18 // Tamil third
            INDIAN_LANGUAGES.contains(language) -> score += 15 // Other Indian languages
        }
        
        // Prefer songs with richer metadata (album + artist present)
        if (song.album.isNotBlank()) score += 10
        if (song.artist.isNotBlank()) score += 10
        
        // === RECENCY BONUS ===
        // Newer uploads get slight boost
        if (song.uploadDate.isNotBlank()) {
            try {
                // Boost recent uploads (within last 6 months)
                val uploadYear = song.uploadDate.takeLast(4).toIntOrNull() ?: 0
                val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                if (uploadYear >= currentYear - 1) score += 10
            } catch (e: Exception) {
                // Ignore parsing errors
            }
        }
        
        return score
    }
    
    /**
     * Load more results for endless scrolling
     */
    fun loadMoreResults() {
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMore) return
        
        loadMoreJob?.cancel()
        loadMoreJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            
            val currentPage = _uiState.value.currentPage
            val startIndex = currentPage * PAGE_SIZE
            val endIndex = startIndex + PAGE_SIZE
            
            if (startIndex < allSearchResults.size) {
                val nextBatch = allSearchResults.subList(
                    startIndex,
                    endIndex.coerceAtMost(allSearchResults.size)
                )
                
                _uiState.value = _uiState.value.copy(
                    songs = _uiState.value.songs + nextBatch,
                    currentPage = currentPage + 1,
                    isLoadingMore = false,
                    hasMore = endIndex < allSearchResults.size
                )
            } else {
                // Fetch more from API
                val query = _uiState.value.query
                if (query.isNotBlank()) {
                    try {
                        val moreQueries = listOf(
                            "$query ${currentPage + 1}",
                            "$query latest",
                            "$query hits"
                        )
                        
                        moreQueries.forEach { searchQuery ->
                            val result = youTubeClient.searchSongs(searchQuery)
                            if (result is Result.Success) {
                                val newSongs = result.data.filter { newSong ->
                                    !allSearchResults.any { it.id == newSong.id }
                                }
                                allSearchResults.addAll(newSongs)
                                _uiState.value = _uiState.value.copy(
                                    songs = _uiState.value.songs + newSongs,
                                    currentPage = currentPage + 1,
                                    hasMore = newSongs.isNotEmpty()
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading more results", e)
                    }
                }
                _uiState.value = _uiState.value.copy(isLoadingMore = false)
            }
        }
    }
    
    /**
     * Toggle unlimited search mode
     */
    fun toggleUnlimitedMode() {
        val newMode = !_uiState.value.isUnlimitedMode
        _uiState.value = _uiState.value.copy(isUnlimitedMode = newMode)
        
        if (newMode && _uiState.value.query.isNotBlank()) {
            performUnlimitedSearch(_uiState.value.query)
        }
    }
    
    /**
     * Unlimited search - fetches maximum results
     */
    private fun performUnlimitedSearch(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // Perform extensive search across all variations
                val allQueries = buildPowerSearchQueries(query) + listOf(
                    "$query full album", "$query jukebox", "$query all songs",
                    "$query collection", "$query best of", "$query top hits"
                )
                
                val allSongs = mutableListOf<Song>()
                allQueries.forEach { searchQuery ->
                    try {
                        val result = repository.searchSongsWithLimit(searchQuery, 100)
                        result.getOrNull()?.let { songs ->
                            allSongs.addAll(songs)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Unlimited search query failed: $searchQuery", e)
                    }
                }
                
                val uniqueSongs = allSongs.distinctBy { it.id }
                    .sortedByDescending { calculateRelevanceScore(query, it) }
                
                allSearchResults.clear()
                allSearchResults.addAll(uniqueSongs)
                
                _uiState.value = _uiState.value.copy(
                    songs = uniqueSongs,
                    isLoading = false,
                    hasMore = false // All results loaded
                )
                
                Log.d(TAG, "Unlimited search: ${uniqueSongs.size} total songs found")
                
            } catch (e: Exception) {
                Log.e(TAG, "Unlimited search error", e)
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    
    fun setFilter(filter: SearchFilter) {
        _uiState.value = _uiState.value.copy(activeFilter = filter)
    }
    
    private fun addToHistory(query: String) {
        if (query.isNotBlank() && !searchHistoryList.contains(query)) {
            searchHistoryList.add(0, query)
            if (searchHistoryList.size > 15) {
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
        loadMoreJob?.cancel()
        allSearchResults.clear()
        _uiState.value = SearchUiState(searchHistory = searchHistoryList.toList())
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
