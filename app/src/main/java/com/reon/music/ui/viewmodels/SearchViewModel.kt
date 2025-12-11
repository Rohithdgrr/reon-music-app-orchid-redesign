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
    val movies: List<Album> = emptyList(),
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

private data class ChannelPriorityMeta(
    val primaryName: String,
    val aliases: Set<String> = emptySet(),
    val languages: Set<String>,
    val weight: Int
)

private val CHANNEL_PRIORITY_LIST = listOf(
    // Hindi / Bollywood Channels
    ChannelPriorityMeta("T-Series", aliases = setOf("tseries"), languages = setOf("hindi", "bollywood", "global"), weight = 70),
    ChannelPriorityMeta("Zee Music Company", languages = setOf("hindi", "bollywood"), weight = 62),
    ChannelPriorityMeta("Tips Official", languages = setOf("hindi", "bollywood"), weight = 58),
    ChannelPriorityMeta("YRF", aliases = setOf("yrf music"), languages = setOf("hindi", "bollywood"), weight = 56),
    ChannelPriorityMeta("Sony Music India", languages = setOf("hindi", "bollywood"), weight = 55),
    ChannelPriorityMeta("Saregama Music", aliases = setOf("saregama"), languages = setOf("hindi", "bollywood"), weight = 54),
    ChannelPriorityMeta("T-Series Bollywood Classics", languages = setOf("hindi", "bollywood"), weight = 52),
    ChannelPriorityMeta("Goldmines Gaane Sune Ansune", languages = setOf("hindi", "bollywood"), weight = 50),
    ChannelPriorityMeta("Shemaroo Filmi Gaane", languages = setOf("hindi", "bollywood"), weight = 50),
    ChannelPriorityMeta("Eros Now Music", languages = setOf("hindi", "bollywood"), weight = 48),
    ChannelPriorityMeta("Arijit Singh", aliases = setOf("arijit singh official"), languages = setOf("hindi", "bollywood"), weight = 45),
    ChannelPriorityMeta("Shreya Ghoshal", aliases = setOf("shreya ghoshal official"), languages = setOf("hindi", "tamil", "malayalam", "kannada"), weight = 45),
    ChannelPriorityMeta("Vishal Dadlani", aliases = setOf("vishal dadlani official"), languages = setOf("hindi"), weight = 42),
    ChannelPriorityMeta("Neha Kakkar", aliases = setOf("neha kakkar official"), languages = setOf("hindi"), weight = 42),
    ChannelPriorityMeta("Atif Aslam", aliases = setOf("atif aslam official"), languages = setOf("hindi", "punjabi"), weight = 40),

    // Telugu Channels
    ChannelPriorityMeta("Aditya Music", languages = setOf("telugu"), weight = 60),
    ChannelPriorityMeta("Infobells - Telugu", aliases = setOf("infobells telugu"), languages = setOf("telugu"), weight = 55),
    ChannelPriorityMeta("T-Series Telugu", languages = setOf("telugu"), weight = 54),
    ChannelPriorityMeta("Mango Music", languages = setOf("telugu"), weight = 52),
    ChannelPriorityMeta("Volga Music", languages = setOf("telugu"), weight = 50),
    ChannelPriorityMeta("Lahari Music", languages = setOf("telugu", "kannada"), weight = 48),
    ChannelPriorityMeta("Devi Sri Prasad", aliases = setOf("devi sri prasad official"), languages = setOf("telugu"), weight = 46),
    ChannelPriorityMeta("S.P. Balasubrahmanyam", aliases = setOf("sp balu", "s p balasubrahmanyam"), languages = setOf("telugu", "tamil"), weight = 44),
    ChannelPriorityMeta("Thaman S", aliases = setOf("thaman s official"), languages = setOf("telugu"), weight = 44),
    ChannelPriorityMeta("K.S. Chithra", aliases = setOf("ks chitra", "k s chithra"), languages = setOf("telugu", "tamil", "malayalam", "kannada"), weight = 43),
    ChannelPriorityMeta("Sid Sriram", aliases = setOf("sid sriram official"), languages = setOf("telugu", "tamil"), weight = 46),
    ChannelPriorityMeta("Mangli", aliases = setOf("mangli official"), languages = setOf("telugu"), weight = 42),
    ChannelPriorityMeta("Geetha Madhuri", aliases = setOf("geetha madhuri official"), languages = setOf("telugu"), weight = 40),

    // Tamil Channels
    ChannelPriorityMeta("Infobells - Tamil", aliases = setOf("infobells tamil"), languages = setOf("tamil"), weight = 55),
    ChannelPriorityMeta("Think Music India", languages = setOf("tamil", "malayalam"), weight = 54),
    ChannelPriorityMeta("Sony Music South", languages = setOf("tamil", "telugu", "malayalam", "kannada"), weight = 53),
    ChannelPriorityMeta("T-Series Tamil", languages = setOf("tamil"), weight = 52),
    ChannelPriorityMeta("Divo Music", languages = setOf("tamil"), weight = 50),
    ChannelPriorityMeta("Anirudh Ravichander", aliases = setOf("anirudh ravichander official"), languages = setOf("tamil"), weight = 52),
    ChannelPriorityMeta("A.R. Rahman", aliases = setOf("a r rahman official"), languages = setOf("tamil", "hindi"), weight = 58),
    ChannelPriorityMeta("Sid Sriram", aliases = setOf("sid sriram tamil"), languages = setOf("tamil", "telugu"), weight = 46),
    ChannelPriorityMeta("Chinmayi Sripada", aliases = setOf("chinmayi official"), languages = setOf("tamil"), weight = 45),
    ChannelPriorityMeta("Dhee", aliases = setOf("dhee official"), languages = setOf("tamil"), weight = 43),
    ChannelPriorityMeta("Karthik", aliases = setOf("karthik official"), languages = setOf("tamil"), weight = 42),
    ChannelPriorityMeta("Andrea Jeremiah", aliases = setOf("andrea jeremiah official"), languages = setOf("tamil"), weight = 41),
    ChannelPriorityMeta("Hariharan", aliases = setOf("hariharan official"), languages = setOf("tamil", "hindi"), weight = 40),

    // English / Global Artists
    ChannelPriorityMeta("Justin Bieber", languages = setOf("english", "global"), weight = 60),
    ChannelPriorityMeta("EminemMusic", aliases = setOf("eminem"), languages = setOf("english", "global"), weight = 58),
    ChannelPriorityMeta("Taylor Swift", languages = setOf("english", "global"), weight = 60),
    ChannelPriorityMeta("Ed Sheeran", languages = setOf("english", "global"), weight = 58),
    ChannelPriorityMeta("Marshmello", languages = setOf("english", "global"), weight = 56),
    ChannelPriorityMeta("Billie Eilish", languages = setOf("english", "global"), weight = 55),
    ChannelPriorityMeta("Ariana Grande", languages = setOf("english", "global"), weight = 55),
    ChannelPriorityMeta("Bad Bunny", languages = setOf("english", "global"), weight = 54),
    ChannelPriorityMeta("Shakira", languages = setOf("english", "global"), weight = 53),
    ChannelPriorityMeta("Alan Walker", languages = setOf("english", "global"), weight = 52),
    ChannelPriorityMeta("Rihanna", languages = setOf("english", "global"), weight = 52),
    ChannelPriorityMeta("Bruno Mars", languages = setOf("english", "global"), weight = 51),
    ChannelPriorityMeta("Jason Derulo", languages = setOf("english", "global"), weight = 49),
    ChannelPriorityMeta("The Weeknd", languages = setOf("english", "global"), weight = 53),
    ChannelPriorityMeta("Selena Gomez", languages = setOf("english", "global"), weight = 52),
    ChannelPriorityMeta("J Balvin", languages = setOf("english", "global"), weight = 50),
    ChannelPriorityMeta("Lady Gaga", languages = setOf("english", "global"), weight = 49),
    ChannelPriorityMeta("Dua Lipa", languages = setOf("english", "global"), weight = 50),

    // Malayalam Channels & Artists
    ChannelPriorityMeta("Saregama Malayalam", languages = setOf("malayalam"), weight = 48),
    ChannelPriorityMeta("MC Music", languages = setOf("malayalam"), weight = 44),
    ChannelPriorityMeta("Think Music India", aliases = setOf("think music malayalam"), languages = setOf("malayalam", "tamil"), weight = 46),
    ChannelPriorityMeta("Sony Music South", aliases = setOf("sony music south malayalam"), languages = setOf("malayalam", "tamil", "telugu", "kannada"), weight = 45),
    ChannelPriorityMeta("Vijay Yesudas", aliases = setOf("vijay yesudas official"), languages = setOf("malayalam"), weight = 42),
    ChannelPriorityMeta("K.S. Harishankar", aliases = setOf("ks harishankar", "k s harishankar"), languages = setOf("malayalam"), weight = 40),
    ChannelPriorityMeta("Sithara Krishnakumar", aliases = setOf("sithara"), languages = setOf("malayalam"), weight = 38),
    ChannelPriorityMeta("Dabzee", aliases = setOf("dabzee official"), languages = setOf("malayalam"), weight = 36),
    ChannelPriorityMeta("Shweta Mohan", aliases = setOf("swetha mohan", "shwetha mohan"), languages = setOf("malayalam", "tamil"), weight = 42),

    // Kannada Channels & Artists
    ChannelPriorityMeta("Saregama Kannada", languages = setOf("kannada"), weight = 46),
    ChannelPriorityMeta("T-Series Kannada", languages = setOf("kannada"), weight = 45),
    ChannelPriorityMeta("Anand Audio", languages = setOf("kannada"), weight = 44),
    ChannelPriorityMeta("Think Music Kannada", languages = setOf("kannada"), weight = 43),
    ChannelPriorityMeta("Kailash Kher", aliases = setOf("kailash kher official"), languages = setOf("kannada", "hindi"), weight = 40),
    ChannelPriorityMeta("Sanjith Hegde", aliases = setOf("sanjith hegde"), languages = setOf("kannada"), weight = 40),
    ChannelPriorityMeta("Varijashree Venugopal", aliases = setOf("varijashree"), languages = setOf("kannada"), weight = 36),

    // Punjabi Channels & Artists
    ChannelPriorityMeta("Speed Records", languages = setOf("punjabi"), weight = 55),
    ChannelPriorityMeta("T-Series Apna Punjab", languages = setOf("punjabi"), weight = 53),
    ChannelPriorityMeta("Ishtar Punjabi", languages = setOf("punjabi"), weight = 50),
    ChannelPriorityMeta("Geet MP3", languages = setOf("punjabi"), weight = 50),
    ChannelPriorityMeta("Sidhu Moose Wala", languages = setOf("punjabi"), weight = 52),
    ChannelPriorityMeta("Diljit Dosanjh", aliases = setOf("diljit dosanjh official"), languages = setOf("punjabi", "hindi"), weight = 48),
    ChannelPriorityMeta("Yo Yo Honey Singh", languages = setOf("punjabi", "hindi"), weight = 47),
    ChannelPriorityMeta("Karan Aujla", aliases = setOf("karan aujla official"), languages = setOf("punjabi"), weight = 46),
    ChannelPriorityMeta("Guru Randhawa", aliases = setOf("guru randhawa official"), languages = setOf("punjabi", "hindi"), weight = 46),
    ChannelPriorityMeta("B Praak", aliases = setOf("b praak official"), languages = setOf("punjabi"), weight = 45),
    ChannelPriorityMeta("Ammy Virk", aliases = setOf("ammy virk official"), languages = setOf("punjabi"), weight = 44),
    ChannelPriorityMeta("Nimrat Khaira", aliases = setOf("nimrat khaira official"), languages = setOf("punjabi"), weight = 43)
)

private val CHANNEL_PRIORITY_MAP: Map<String, ChannelPriorityMeta> = buildMap {
    CHANNEL_PRIORITY_LIST.forEach { meta ->
        (setOf(meta.primaryName) + meta.aliases).forEach { alias ->
            val key = normalizeChannelName(alias)
            val existing = this[key]
            if (existing == null || meta.weight > existing.weight) {
                this[key] = meta
            }
        }
    }
}

private val CHANNEL_PRIORITY_BY_LANGUAGE: Map<String, List<ChannelPriorityMeta>> =
    CHANNEL_PRIORITY_LIST.flatMap { meta ->
        meta.languages.map { language -> language to meta }
    }.groupBy({ it.first }, { it.second })

private val DETECTABLE_LANGUAGES = setOf(
    "hindi", "telugu", "tamil", "english", "malayalam", "kannada", "punjabi"
)

private val NON_ALPHANUMERIC_REGEX = Regex("[^a-z0-9 ]")
private val MULTI_WHITESPACE_REGEX = Regex("\\s+")

private fun normalizeChannelName(name: String): String {
    if (name.isBlank()) return ""
    val lower = name.lowercase(Locale.US)
    val stripped = NON_ALPHANUMERIC_REGEX.replace(lower, " ")
    return MULTI_WHITESPACE_REGEX.replace(stripped, " ").trim()
}

private fun detectLanguageFromQuery(normalizedQuery: String): String? {
    return when {
        normalizedQuery.contains("telugu") || normalizedQuery.contains("tollywood") -> "telugu"
        normalizedQuery.contains("hindi") || normalizedQuery.contains("bollywood") -> "hindi"
        normalizedQuery.contains("tamil") -> "tamil"
        normalizedQuery.contains("malayalam") || normalizedQuery.contains("mollywood") -> "malayalam"
        normalizedQuery.contains("kannada") -> "kannada"
        normalizedQuery.contains("punjabi") -> "punjabi"
        normalizedQuery.contains("english") || normalizedQuery.contains("international") -> "english"
        else -> null
    }
}

private fun findChannelMetaFromQuery(normalizedQuery: String): ChannelPriorityMeta? {
    CHANNEL_PRIORITY_MAP[normalizedQuery]?.let { return it }
    return CHANNEL_PRIORITY_LIST.firstOrNull { meta ->
        val candidates = (setOf(meta.primaryName) + meta.aliases).map { normalizeChannelName(it) }
        candidates.any { candidate -> candidate.isNotBlank() && normalizedQuery.contains(candidate) }
    }
}

private fun computeChannelPriorityScore(
    song: Song,
    queryLanguage: String?,
    queryChannelMeta: ChannelPriorityMeta?
): Double {
    val normalizedChannelName = normalizeChannelName(song.channelName)
    val normalizedArtistName = normalizeChannelName(song.artist)
    val meta = CHANNEL_PRIORITY_MAP[normalizedChannelName]
    var score = 0.0

    if (meta != null) {
        score += meta.weight.toDouble()
        if (queryLanguage != null && meta.languages.contains(queryLanguage)) {
            score += 25.0
        }
    }

    if (queryChannelMeta != null) {
        val queryChannelName = normalizeChannelName(queryChannelMeta.primaryName)
        if (meta != null && meta.primaryName == queryChannelMeta.primaryName) {
            score += 40.0
        } else if (queryChannelName.isNotBlank() && normalizedArtistName.contains(queryChannelName)) {
            score += 15.0
        }
    }

    return score
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val youTubeClient: YouTubeMusicClient,
    private val repository: MusicRepository
) : ViewModel() {
    
    companion object {
        private const val TAG = "SearchViewModel"
        private const val DEBOUNCE_MS = 400L
        private const val SUGGESTION_DEBOUNCE_MS = 300L
        private const val INITIAL_SEARCH_LIMIT = 100
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
            
            // Perform parallel searches in chunks to avoid resource exhaustion
            val allSongs = mutableListOf<Song>()
            
            // Limit total queries to prevent overload
            val limitedQueries = searchQueries.take(12) 
            
            // Process in batches of 4
            limitedQueries.chunked(4).forEach { batch ->
                val batchResults = batch.map { searchQuery ->
                    viewModelScope.async {
                        try {
                            // Add a small random delay to prevent thundering herd
                            delay((10..50).random().toLong())
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
                
                // Wait for this batch to complete before starting next
                batchResults.awaitAll().forEach { songs ->
                    allSongs.addAll(songs)
                }
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
                    val normalizedArtist = normalizeChannelName(song.artist)
                    val meta = CHANNEL_PRIORITY_MAP[normalizedArtist]
                    Artist(
                        id = song.artist.lowercase(Locale.US).hashCode().toString(),
                        name = song.artist,
                        artworkUrl = song.artworkUrl,
                        followerCount = maxOf(song.channelSubscriberCount.toInt(), meta?.weight?.times(1000000 / 10) ?: 0),
                        topSongs = emptyList()
                    )
                }
                .distinctBy { it.name.lowercase() }
                .sortedByDescending { it.followerCount }
                .take(20)
            
            // Extract unique albums from songs
            val albumBuckets = rankedSongs
                .filter { it.album.isNotBlank() || it.movieName.isNotBlank() }
                .groupBy { (it.album.takeIf { album -> album.isNotBlank() } ?: it.movieName).lowercase() }
            val albums = albumBuckets.map { (key, songs) ->
                val representative = songs.first()
                Album(
                    id = key.hashCode().toString(),
                    name = representative.album.takeIf { it.isNotBlank() } ?: representative.movieName,
                    artist = representative.artist,
                    artworkUrl = representative.artworkUrl,
                    songs = songs.take(10)
                )
            }.sortedByDescending { it.songs.size }.take(20)
            
            val movieResults = rankedSongs
                .filter { it.movieName.isNotBlank() || it.album.contains("(From", ignoreCase = true) || it.album.contains("Original Motion", ignoreCase = true) }
                .groupBy { it.movieName.lowercase() }
                .map { (name, songs) ->
                    Album(
                        id = ("movie_" + name).hashCode().toString(),
                        name = songs.first().movieName,
                        artist = songs.first().heroName.ifBlank { songs.first().artist },
                        artworkUrl = songs.first().artworkUrl,
                        songs = songs
                    )
                }
                .sortedByDescending { it.songs.size }
                .take(20)
            
            val artistDetails = rankedSongs
                .groupBy { it.artist.lowercase() }
                .map { (name, songs) ->
                    val representative = songs.first()
                    Artist(
                        id = name.hashCode().toString(),
                        name = representative.artist,
                        artworkUrl = representative.artworkUrl,
                        followerCount = representative.channelSubscriberCount.toInt(),
                        topSongs = songs.take(10)
                    )
                }
                .sortedByDescending { it.followerCount }
                .take(25)
            
            _uiState.value = _uiState.value.copy(
                songs = rankedSongs.take(INITIAL_SEARCH_LIMIT),
                albums = albums,
                artists = artistDetails,
                movies = movieResults.take(15),
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
        val normalizedQuery = normalizeChannelName(query)
        val queryLanguage = detectLanguageFromQuery(normalizedQuery)
        val queryChannelMeta = findChannelMetaFromQuery(normalizedQuery)
        
        // Primary query
        queries.add(query)
        
        // === SONG & ALBUM SEARCHES ===
        queries.add("$query song")
        queries.add("$query songs")
        queries.add("$query album")
        queries.add("$query track")
        
        // === MOVIE SEARCHES (for film songs) ===
        queries.add("$query movie songs")
        queries.add("$query movie soundtrack")
        queries.add("$query film songs")
        queries.add("$query cinema songs")
        
        // === ARTIST SEARCHES ===
        queries.add("$query artist")
        queries.add("$query singer")
        queries.add("$query performer")
        
        // === ACTOR/HERO/HEROINE SEARCHES ===
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
        
        // === CHANNEL-FOCUSED VARIATIONS ===
        queryChannelMeta?.let { meta ->
            queries.add("${meta.primaryName} $query")
            queries.add("$query ${meta.primaryName}")
            meta.aliases.take(3).forEach { alias ->
                queries.add("$alias $query")
                queries.add("$query $alias")
            }
        }
        
        val detectedLanguage = queryLanguage ?: run {
            DETECTABLE_LANGUAGES.firstOrNull { language ->
                normalizedQuery.contains(language)
            }
        }
        
        val isIndianLanguageQuery = detectedLanguage != null || INDIAN_LANGUAGES.any {
            query.lowercase().contains(it)
        }
        
        if (detectedLanguage != null) {
            CHANNEL_PRIORITY_BY_LANGUAGE[detectedLanguage]
                ?.distinctBy { it.primaryName }
                ?.take(4)
                ?.forEach { meta ->
                    queries.add("${meta.primaryName} $query")
                    queries.add("$query ${meta.primaryName}")
                }
        }
        
        // Add Indian language variations for better coverage when language not specified
        if (!isIndianLanguageQuery) {
            queries.add("$query telugu")
            queries.add("$query telugu songs")
            queries.add("$query telugu movie songs")
            
            queries.add("$query hindi")
            queries.add("$query hindi songs")
            queries.add("$query bollywood $query")
            
            queries.add("$query tamil")
            queries.add("$query tamil songs")
            
            queries.add("$query kannada")
            queries.add("$query kannada songs")
            
            queries.add("$query punjabi")
            queries.add("$query punjabi songs")
            
            queries.add("$query malayalam")
            queries.add("$query malayalam songs")
        }
        
        return queries.distinct().take(20)
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
        
        val normalizedQuery = normalizeChannelName(query)
        val queryLanguage = detectLanguageFromQuery(normalizedQuery)
        val queryChannelMeta = findChannelMetaFromQuery(normalizedQuery)
        val languageTopChannels = queryLanguage?.let { language ->
            CHANNEL_PRIORITY_BY_LANGUAGE[language]?.mapIndexed { index, meta ->
                normalizeChannelName(meta.primaryName) to (40.0 - index * 6)
            }?.toMap()
        } ?: emptyMap()

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
                
                val channelPriorityScore = computeChannelPriorityScore(song, queryLanguage, queryChannelMeta)
                val topChannelLanguageBoost = languageTopChannels[normalizeChannelName(song.channelName)] ?: 0.0
                
                val score = relevance + engagement + recencyScore + officialBoost + durationScore + qualityScore + channelPriorityScore + topChannelLanguageBoost
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
