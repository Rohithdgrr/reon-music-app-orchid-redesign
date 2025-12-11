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
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.ln
import kotlin.math.max

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
            
            // Remove duplicates then enrich + re-rank similar to YouTube ordering
            val uniqueSongs = allSongs.distinctBy { it.id }
            val enrichedTop = repository.enhanceSongsWithMetadata(uniqueSongs.take(180))
            val mergedSongs = (enrichedTop + uniqueSongs.drop(180)).distinctBy { it.id }
            val rankedSongs = reRankWithYouTubeSignals(query, mergedSongs).take(MAX_RESULTS)
            
            allSearchResults.addAll(rankedSongs)
            
            // Extract unique artists from songs
            val artists = rankedSongs
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
            val albums = rankedSongs
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
                songs = rankedSongs.take(INITIAL_SEARCH_LIMIT),
                albums = albums,
                artists = artists,
                isLoading = false,
                hasMore = rankedSongs.size > INITIAL_SEARCH_LIMIT,
                error = if (rankedSongs.isEmpty()) "No results found. Try searching with different keywords." else null
            )
            
            Log.d(TAG, "Power search completed: ${rankedSongs.size} songs found for '$query'")
            
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
     * Supports searching by: song name, artist, album, movie, metadata, hero/heroine names
     * Prioritizes Indian languages and includes movie/album searches
     */
    private fun buildPowerSearchQueries(query: String): List<String> {
        val queries = mutableListOf<String>()
        
        // Primary query
        queries.add(query)
        
        // === SONG & ALBUM SEARCHES ===
        queries.add("$query song")
        queries.add("$query songs")
        queries.add("$query album")
        queries.add("$query track")
        
        // === MOVIE SEARCHES (for film songs) ===
        // This helps when user searches by movie name
        queries.add("$query movie songs")
        queries.add("$query movie soundtrack")
        queries.add("$query film songs")
        queries.add("$query cinema songs")
        
        // === ARTIST SEARCHES ===
        queries.add("$query artist")
        queries.add("$query singer")
        queries.add("$query performer")
        
        // === ACTOR/HERO/HEROINE SEARCHES ===
        // Helps find songs from movies by searching hero/heroine names
        queries.add("$query hero songs")
        queries.add("$query heroine songs")
        queries.add("$query actor songs")
        queries.add("$query actress songs")
        
        // === METADATA & CATEGORY SEARCHES ===
        queries.add("$query romantic")
        queries.add("$query love songs")
        queries.add("$query sad songs")
        queries.add("$query devotional")
        queries.add("$query classical")
        
        // Determine if query might be targeting specific language
        val isIndianLanguageQuery = INDIAN_LANGUAGES.any { 
            query.lowercase().contains(it) 
        }
        
        // Add Indian language variations for better coverage
        if (!isIndianLanguageQuery) {
            // Telugu - highest priority
            queries.add("$query telugu")
            queries.add("$query telugu songs")
            queries.add("$query telugu movie songs")
            
            // Hindi - second priority
            queries.add("$query hindi")
            queries.add("$query hindi songs")
            queries.add("$query bollywood $query")
            
            // Tamil
            queries.add("$query tamil")
            queries.add("$query tamil songs")
            
            // Kannada
            queries.add("$query kannada")
            queries.add("$query kannada songs")
            
            // Punjabi
            queries.add("$query punjabi")
            queries.add("$query punjabi songs")
            
            // Malayalam
            queries.add("$query malayalam")
            queries.add("$query malayalam songs")
        }
        
        return queries.distinct().take(15) // Increased limit for comprehensive search
    }
    
    /**
     * Calculate relevance score for sorting results
     * Enhanced algorithm for multi-field searching with better ranking
     * Considers: title, artist, album, movie name, hero/heroine, director, metadata
     */
    private fun calculateRelevanceScore(query: String, song: Song): Int {
        var score = 0
        val lowerQuery = query.lowercase()
        val lowerTitle = song.title.lowercase()
        val lowerArtist = song.artist.lowercase()
        val lowerAlbum = song.album.lowercase()
        val lowerMovieName = song.movieName.lowercase()
        val lowerHero = song.heroName.lowercase()
        val lowerHeroine = song.heroineName.lowercase()
        val lowerDirector = song.director.lowercase()
        val lowerProducer = song.producer.lowercase()
        val lowerGenre = song.genre.lowercase()
        val lowerDescription = song.description.lowercase()
        
        // === TITLE MATCHING (Highest Priority - 200 base points) ===
        when {
            lowerTitle == lowerQuery -> score += 200 // Exact match
            lowerTitle.startsWith(lowerQuery) -> score += 180 // Starts with query
            lowerTitle.contains(" $lowerQuery ") -> score += 160 // Word boundary match
            lowerTitle.contains(lowerQuery) -> score += 120 // Contains query
            lowerTitle.split(" ").any { it == lowerQuery } -> score += 140 // Complete word match
        }
        
        // === ARTIST MATCHING (160 base points) ===
        when {
            lowerArtist == lowerQuery -> score += 160 // Exact artist match
            lowerArtist.startsWith(lowerQuery) -> score += 130 // Artist starts with query
            lowerArtist.contains(lowerQuery) -> score += 100 // Artist contains query
            lowerArtist.split(",").any { it.trim() == lowerQuery } -> score += 140 // Multiple artists
        }
        
        // === ALBUM/MOVIE MATCHING (140 base points) ===
        // Album search
        when {
            lowerAlbum == lowerQuery -> score += 140 // Exact album match
            lowerAlbum.startsWith(lowerQuery) -> score += 110 // Album starts with query
            lowerAlbum.contains(lowerQuery) -> score += 80 // Album contains query
        }
        
        // Movie name search (130 base points - high priority for film songs)
        when {
            lowerMovieName == lowerQuery -> score += 130 // Exact movie match
            lowerMovieName.startsWith(lowerQuery) -> score += 110 // Movie starts with query
            lowerMovieName.contains(lowerQuery) -> score += 85 // Movie contains query
        }
        
        // === HERO/HEROINE/ACTOR SEARCHES (120 base points) ===
        // Helps find songs from movies by searching lead actor names
        when {
            lowerHero == lowerQuery -> score += 120 // Exact hero match
            lowerHero.startsWith(lowerQuery) -> score += 100 // Hero starts with query
            lowerHero.contains(lowerQuery) -> score += 75 // Hero contains query
        }
        
        when {
            lowerHeroine == lowerQuery -> score += 120 // Exact heroine match
            lowerHeroine.startsWith(lowerQuery) -> score += 100 // Heroine starts with query
            lowerHeroine.contains(lowerQuery) -> score += 75 // Heroine contains query
        }
        
        // === DIRECTOR/PRODUCER/CREW SEARCHES (90 base points) ===
        when {
            lowerDirector == lowerQuery -> score += 90 // Exact director match
            lowerDirector.contains(lowerQuery) -> score += 60 // Director contains query
        }
        
        when {
            lowerProducer == lowerQuery -> score += 80 // Exact producer match
            lowerProducer.contains(lowerQuery) -> score += 50 // Producer contains query
        }
        
        // === METADATA SEARCHES (Genre, Description - 70 base points) ===
        if (lowerGenre.isNotBlank()) {
            when {
                lowerGenre == lowerQuery -> score += 70 // Exact genre match
                lowerGenre.contains(lowerQuery) -> score += 50 // Genre contains query
                lowerQuery in lowerGenre.split(",").map { it.trim() } -> score += 60
            }
        }
        
        // Movie genre search (for songs from specific genre movies)
        if (song.movieGenre.isNotBlank() && lowerQuery.contains("movie")) {
            val lowerMovieGenre = song.movieGenre.lowercase()
            when {
                lowerMovieGenre.contains(lowerQuery.replace("movie", "").trim()) -> score += 50
            }
        }
        
        // Description/metadata search
        if (lowerDescription.isNotBlank()) {
            if (lowerDescription.contains(lowerQuery)) {
                score += 40 // Description contains query
            }
        }
        
        // === POPULARITY SCORING (Max 105 points) ===
        // View count (normalized, max 50 points)
        val viewScore = (song.viewCount / 1000000).toInt().coerceAtMost(50)
        score += viewScore
        
        // Like count (normalized, max 30 points)
        val likeScore = (song.likeCount / 100000).toInt().coerceAtMost(30)
        score += likeScore
        
        // Channel subscriber count (max 25 points)
        val channelScore = (song.channelSubscriberCount / 1000000).toInt().coerceAtMost(25)
        score += channelScore
        
        // === QUALITY SCORING (Max 60 points) ===
        if (song.is320kbps) score += 25 // High quality audio (most important)
        if (song.quality.contains("HD", ignoreCase = true)) score += 20
        if (song.quality.contains("4K", ignoreCase = true)) score += 35
        
        // === LANGUAGE PREFERENCE (Max 25 points) ===
        val language = song.language.lowercase()
        when {
            language.contains("telugu") -> score += 25 // Telugu highest priority
            language.contains("hindi") -> score += 20 // Hindi second
            language.contains("tamil") -> score += 18 // Tamil third
            language.contains("kannada") -> score += 16 // Kannada
            language.contains("malayalam") -> score += 14 // Malayalam
            INDIAN_LANGUAGES.contains(language) -> score += 12 // Other Indian languages
        }
        
        // === METADATA COMPLETENESS (Max 30 points) ===
        // Prefer songs with richer metadata (more fields filled = better quality result)
        if (song.album.isNotBlank()) score += 5
        if (song.artist.isNotBlank()) score += 5
        if (song.movieName.isNotBlank()) score += 8 // Film songs get bonus for complete movie info
        if (song.heroName.isNotBlank()) score += 4
        if (song.heroineName.isNotBlank()) score += 4
        if (song.director.isNotBlank()) score += 2
        if (song.genre.isNotBlank()) score += 2
        
        // === RECENCY BONUS (Max 10 points) ===
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
        
        // === CHANNEL QUALITY BONUS ===
        // Boost results from official channels or popular creators
        if (song.channelName.contains("official", ignoreCase = true)) score += 15
        if (song.channelName.contains("music", ignoreCase = true)) score += 10
        if (song.channelSubscriberCount > 10000000) score += 8 // Mega channels
        
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
                val reRanked = reRankWithYouTubeSignals(_uiState.value.query, allSearchResults)
                allSearchResults = reRanked.toMutableList()
                val sliceEnd = (startIndex + PAGE_SIZE).coerceAtMost(reRanked.size)
                val nextBatch = reRanked.subList(startIndex, sliceEnd)
                
                _uiState.value = _uiState.value.copy(
                    songs = _uiState.value.songs + nextBatch,
                    currentPage = currentPage + 1,
                    isLoadingMore = false,
                    hasMore = sliceEnd < reRanked.size
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
                                val reRanked = reRankWithYouTubeSignals(query, allSearchResults)
                                allSearchResults = reRanked.toMutableList()
                                _uiState.value = _uiState.value.copy(
                                    songs = _uiState.value.songs + reRanked.drop(_uiState.value.songs.size),
                                    currentPage = currentPage + 1,
                                    hasMore = reRanked.size > _uiState.value.songs.size
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
                val rankedSongs = reRankWithYouTubeSignals(query, uniqueSongs)
                
                allSearchResults.clear()
                allSearchResults.addAll(rankedSongs)
                
                _uiState.value = _uiState.value.copy(
                    songs = rankedSongs,
                    isLoading = false,
                    hasMore = false // All results loaded
                )
                
                Log.d(TAG, "Unlimited search: ${rankedSongs.size} total songs found")
                
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
    
    /**
     * Rank songs using YouTube-style signals (relevance + engagement + recency)
     */
    private fun reRankWithYouTubeSignals(query: String, songs: List<Song>): List<Song> {
        val nowMillis = System.currentTimeMillis()
        
        return songs
            .map { song ->
                val relevance = calculateRelevanceScore(query, song).toDouble()
                
                val engagement = (
                    ln(song.viewCount.coerceAtLeast(1).toDouble()) * 12 +
                    ln(song.likeCount.coerceAtLeast(1).toDouble()) * 8 +
                    ln(song.channelSubscriberCount.coerceAtLeast(1).toDouble()) * 6
                )
                
                val recencyMillis = parseUploadDateMillis(song.uploadDate)
                val monthsOld = recencyMillis?.let { (nowMillis - it).coerceAtLeast(0) / (1000L * 60 * 60 * 24 * 30) } ?: 48L
                val recencyScore = max(0.0, 24.0 - monthsOld.toDouble()) * 3
                
                val officialBoost = when {
                    song.channelName.contains("official", ignoreCase = true) -> 25.0
                    song.channelName.contains("music", ignoreCase = true) -> 10.0
                    else -> 0.0
                }
                
                // Prefer typical music durations (~2-7 minutes) to mimic YouTube ordering
                val durationScore = when {
                    song.duration in 120..420 -> 18.0
                    song.duration in 90..540 -> 8.0
                    else -> 0.0
                }
                
                val qualityScore = when {
                    song.is320kbps -> 6.0
                    song.quality.contains("4K", true) -> 8.0
                    song.quality.contains("HD", true) -> 4.0
                    else -> 0.0
                }
                
                val score = relevance + engagement + recencyScore + officialBoost + durationScore + qualityScore
                song to score
            }
            .sortedByDescending { it.second }
            .map { it.first }
    }
    
    /**
     * Parse upload date string to millis for recency scoring
     */
    private fun parseUploadDateMillis(uploadDate: String): Long? {
        if (uploadDate.isBlank()) return null
        val formats = listOf(
            "yyyy-MM-dd",
            "dd MMM yyyy",
            "MMM dd, yyyy",
            "yyyy"
        )
        formats.forEach { pattern ->
            runCatching {
                val formatter = SimpleDateFormat(pattern, Locale.US)
                formatter.parse(uploadDate)?.time
            }.getOrNull()?.let { return it }
        }
        return null
    }
}
