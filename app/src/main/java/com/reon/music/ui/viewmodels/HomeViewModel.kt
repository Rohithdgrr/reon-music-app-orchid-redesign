/*
 * REON Music App - Home ViewModel
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 */

package com.reon.music.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reon.music.core.model.Album
import com.reon.music.core.model.Artist
import com.reon.music.core.model.Playlist
import com.reon.music.core.model.Song
import com.reon.music.core.preferences.MusicSource
import com.reon.music.core.preferences.UserPreferences
import com.reon.music.data.database.dao.HistoryDao
import com.reon.music.data.database.dao.SongDao
import com.reon.music.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Represents a chart section with title and songs
 */
data class ChartSection(
    val id: String,
    val title: String,
    val songs: List<Song> = emptyList()
)

/**
 * Represents a genre for selection
 */
data class Genre(
    val id: String,
    val name: String,
    val emoji: String,
    val color: Long
)

/**
 * UI State for Home Screen
 */
data class HomeUiState(
    // Quick Picks - User's recently played
    val quickPicksSongs: List<Song> = emptyList(),
    val quickPicksArtists: List<Artist> = emptyList(),
    val quickPicksPlaylists: List<Playlist> = emptyList(),
    
    // Most Played
    val mostPlayedSongs: List<Song> = emptyList(),
    
    // Telugu Section
    val teluguSongs: List<Song> = emptyList(),
    val teluguPlaylists: List<Playlist> = emptyList(),
    
    // ST Banjara Songs
    val banjaraSongs: List<Song> = emptyList(),
    
    // Playlists
    val featuredPlaylists: List<Playlist> = emptyList(),
    val moodPlaylists: List<Playlist> = emptyList(),
    
    // New Releases
    val newReleases: List<Song> = emptyList(),
    val newAlbums: List<Album> = emptyList(),
    
    // Albums
    val trendingAlbums: List<Album> = emptyList(),
    
    // Artists
    val topArtists: List<Artist> = emptyList(),
    val recommendedArtists: List<Artist> = emptyList(),
    
    // Charts
    val charts: List<ChartSection> = emptyList(),
    val top50Hindi: List<Song> = emptyList(),
    val top50English: List<Song> = emptyList(),
    val top50Telugu: List<Song> = emptyList(),
    
    // Language/Region Sections
    val hindiSongs: List<Song> = emptyList(),
    val tamilSongs: List<Song> = emptyList(),
    val punjabiSongs: List<Song> = emptyList(),
    val englishSongs: List<Song> = emptyList(),
    
    // Mood/Genre Sections
    val romanticSongs: List<Song> = emptyList(),
    val partySongs: List<Song> = emptyList(),
    val sadSongs: List<Song> = emptyList(),
    val lofiSongs: List<Song> = emptyList(),
    val devotionalSongs: List<Song> = emptyList(),
    val workoutSongs: List<Song> = emptyList(),
    val retroSongs: List<Song> = emptyList(),
    
    // Artist Spotlights
    val arijitSinghSongs: List<Song> = emptyList(),
    val arRahmanSongs: List<Song> = emptyList(),
    val shreyaGhoshalSongs: List<Song> = emptyList(),
    
    // Genres for Selection
    val genres: List<Genre> = defaultGenres,
    val selectedGenre: Genre? = null,
    val genreSongs: List<Song> = emptyList(),
    
    // State
    val isLoading: Boolean = true,
    val error: String? = null,
    val preferredSource: MusicSource = MusicSource.BOTH
) {
    companion object {
        val defaultGenres = listOf(
            Genre("pop", "Pop", "🎵", 0xFFE91E63),
            Genre("rock", "Rock", "🎸", 0xFF9C27B0),
            Genre("hiphop", "Hip-Hop", "🎤", 0xFF673AB7),
            Genre("classical", "Classical", "🎻", 0xFF3F51B5),
            Genre("jazz", "Jazz", "🎷", 0xFF2196F3),
            Genre("electronic", "Electronic", "🎧", 0xFF00BCD4),
            Genre("folk", "Folk", "🪕", 0xFF4CAF50),
            Genre("indie", "Indie", "🎹", 0xFFFF9800),
            Genre("metal", "Metal", "🤘", 0xFF795548),
            Genre("rnb", "R&B", "💜", 0xFF607D8B),
            Genre("country", "Country", "🤠", 0xFFCDDC39),
            Genre("bollywood", "Bollywood", "🎬", 0xFFFF5722)
        )
    }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val userPreferences: UserPreferences,
    private val historyDao: HistoryDao,
    private val songDao: SongDao
) : ViewModel() {
    
    companion object {
        private const val TAG = "HomeViewModel"
    }
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        // Observe preferred source
        viewModelScope.launch {
            userPreferences.preferredSource.collect { source ->
                _uiState.value = _uiState.value.copy(preferredSource = source)
            }
        }
        loadHomeContent()
    }
    
    fun loadHomeContent() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            Log.d(TAG, "Loading home content...")
            
            try {
                // Load Quick Picks (recently played) from database
                loadQuickPicks()
                
                // Load Most Played
                loadMostPlayed()
                
                // Load primary sections in parallel
                val trendingDeferred = async { repository.getTrendingSongs() }
                val newReleasesDeferred = async { repository.getNewReleases() }
                val playlistsDeferred = async { repository.getFeaturedPlaylists() }
                val albumsDeferred = async { repository.getTrendingAlbums() }
                
                val trendingResult = trendingDeferred.await()
                val newReleasesResult = newReleasesDeferred.await()
                val playlistsResult = playlistsDeferred.await()
                val albumsResult = albumsDeferred.await()
                
                _uiState.value = _uiState.value.copy(
                    newReleases = newReleasesResult.getOrNull() ?: emptyList(),
                    featuredPlaylists = playlistsResult.getOrNull() ?: emptyList(),
                    trendingAlbums = albumsResult.getOrNull() ?: emptyList(),
                    isLoading = false
                )
                
                Log.d(TAG, "Primary content loaded")
                
                // Load secondary sections in background
                loadSecondarySections()
                loadCharts()
                loadArtists()
                loadLanguageSections()
                loadMoodSections()
                loadArtistSpotlights()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading home content", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load content"
                )
            }
        }
    }
    
    private suspend fun loadQuickPicks() {
        try {
            // Get recent history from database
            val recentHistory = historyDao.getRecentHistory(20).first()
            val recentSongIds = recentHistory.map { it.songId }
            
            // Convert to Songs
            val recentSongs = recentSongIds.mapNotNull { songId ->
                songDao.getSongById(songId)?.toSong()
            }.take(10)
            
            _uiState.value = _uiState.value.copy(quickPicksSongs = recentSongs)
            Log.d(TAG, "Loaded ${recentSongs.size} quick picks")
            
            // Get unique artists from recent songs
            val recentArtistNames = recentSongs.map { it.artist }.distinct().take(5)
            val artists = recentArtistNames.map { name ->
                Artist(id = name.hashCode().toString(), name = name, artworkUrl = null)
            }
            _uiState.value = _uiState.value.copy(quickPicksArtists = artists)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading quick picks", e)
        }
    }
    
    private suspend fun loadMostPlayed() {
        try {
            val mostPlayedSongs = historyDao.getMostPlayedFromHistory(15).first()
            val songs = mostPlayedSongs.map { it.toSong() }
            _uiState.value = _uiState.value.copy(mostPlayedSongs = songs)
            Log.d(TAG, "Loaded ${songs.size} most played songs")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading most played", e)
        }
    }
    
    private fun loadSecondarySections() {
        // Telugu Songs
        viewModelScope.launch {
            try {
                repository.getTeluguSongs().getOrNull()?.let { songs ->
                    _uiState.value = _uiState.value.copy(teluguSongs = songs)
                    Log.d(TAG, "Loaded ${songs.size} Telugu songs")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading Telugu songs", e)
            }
        }
        
        // Telugu Playlists
        viewModelScope.launch {
            try {
                repository.searchPlaylists("telugu hits").getOrNull()?.let { playlists ->
                    _uiState.value = _uiState.value.copy(teluguPlaylists = playlists)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading Telugu playlists", e)
            }
        }
        
        // ST Banjara Songs - Multiple search terms for better results
        viewModelScope.launch {
            try {
                // Search for authentic Banjara/Lambadi tribal music
                val searchTerms = listOf(
                    "lambadi songs indian tribal",
                    "banjara folk songs telugu",
                    "lambani traditional music",
                    "banjara dj songs",
                    "lambadi songs"
                )
                
                val allSongs = mutableListOf<Song>()
                for (term in searchTerms) {
                    repository.searchSongs(term).getOrNull()?.let { songs ->
                        allSongs.addAll(songs.take(4))
                    }
                    if (allSongs.size >= 15) break
                }
                
                _uiState.value = _uiState.value.copy(banjaraSongs = allSongs.distinctBy { it.id }.take(15))
                Log.d(TAG, "Loaded ${allSongs.size} Banjara/Lambadi songs")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading Banjara songs", e)
            }
        }
        
        // Mood Playlists
        viewModelScope.launch {
            try {
                repository.searchPlaylists("mood chill relax").getOrNull()?.let { playlists ->
                    _uiState.value = _uiState.value.copy(moodPlaylists = playlists)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading mood playlists", e)
            }
        }
        
        // New Albums
        viewModelScope.launch {
            try {
                repository.searchAlbums("new 2024").getOrNull()?.let { albums ->
                    _uiState.value = _uiState.value.copy(newAlbums = albums)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading new albums", e)
            }
        }
    }
    
    private fun loadCharts() {
        // Top 50 Hindi
        viewModelScope.launch {
            try {
                repository.getTop50Hindi().getOrNull()?.let { songs ->
                    _uiState.value = _uiState.value.copy(top50Hindi = songs)
                    updateCharts()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading Top 50 Hindi", e)
            }
        }
        
        // Top 50 English
        viewModelScope.launch {
            try {
                repository.searchSongsWithLimit("top english songs 2024", 50).getOrNull()?.let { songs ->
                    _uiState.value = _uiState.value.copy(top50English = songs)
                    updateCharts()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading Top 50 English", e)
            }
        }
        
        // Top 50 Telugu
        viewModelScope.launch {
            try {
                repository.searchSongsWithLimit("top telugu songs 2024", 50).getOrNull()?.let { songs ->
                    _uiState.value = _uiState.value.copy(top50Telugu = songs)
                    updateCharts()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading Top 50 Telugu", e)
            }
        }
    }
    
    private fun updateCharts() {
        val state = _uiState.value
        val charts = mutableListOf<ChartSection>()
        
        if (state.top50Hindi.isNotEmpty()) {
            charts.add(ChartSection("hindi", "🇮🇳 Top 50 Hindi", state.top50Hindi))
        }
        if (state.top50English.isNotEmpty()) {
            charts.add(ChartSection("english", "🌍 Top 50 English", state.top50English))
        }
        if (state.top50Telugu.isNotEmpty()) {
            charts.add(ChartSection("telugu", "🎬 Top 50 Telugu", state.top50Telugu))
        }
        
        _uiState.value = state.copy(charts = charts)
    }
    
    private fun loadArtists() {
        viewModelScope.launch {
            try {
                repository.getTopArtists().getOrNull()?.let { artists ->
                    _uiState.value = _uiState.value.copy(topArtists = artists)
                    Log.d(TAG, "Loaded ${artists.size} Top Artists")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading Top Artists", e)
            }
        }
        
        viewModelScope.launch {
            try {
                repository.searchArtists("recommended indian artists").getOrNull()?.let { artists ->
                    _uiState.value = _uiState.value.copy(recommendedArtists = artists)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading recommended artists", e)
            }
        }
    }
    
    private fun loadLanguageSections() {
        // Hindi
        viewModelScope.launch {
            try {
                repository.searchSongsWithLimit("latest hindi songs", 20).getOrNull()?.let { songs ->
                    _uiState.value = _uiState.value.copy(hindiSongs = songs)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading Hindi songs", e)
            }
        }
        
        // Tamil
        viewModelScope.launch {
            try {
                repository.getTamilSongs().getOrNull()?.let { songs ->
                    _uiState.value = _uiState.value.copy(tamilSongs = songs)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading Tamil songs", e)
            }
        }
        
        // Punjabi
        viewModelScope.launch {
            try {
                repository.getPunjabiSongs().getOrNull()?.let { songs ->
                    _uiState.value = _uiState.value.copy(punjabiSongs = songs)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading Punjabi songs", e)
            }
        }
        
        // English
        viewModelScope.launch {
            try {
                repository.getEnglishSongs().getOrNull()?.let { songs ->
                    _uiState.value = _uiState.value.copy(englishSongs = songs)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading English songs", e)
            }
        }
    }
    
    private fun loadMoodSections() {
        viewModelScope.launch {
            try {
                repository.getRomanticSongs().getOrNull()?.let { songs ->
                    _uiState.value = _uiState.value.copy(romanticSongs = songs)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading Romantic songs", e)
            }
        }
        
        viewModelScope.launch {
            try {
                repository.getPartySongs().getOrNull()?.let { songs ->
                    _uiState.value = _uiState.value.copy(partySongs = songs)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading Party songs", e)
            }
        }
        
        viewModelScope.launch {
            try {
                repository.getSadSongs().getOrNull()?.let { songs ->
                    _uiState.value = _uiState.value.copy(sadSongs = songs)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading Sad songs", e)
            }
        }
        
        viewModelScope.launch {
            try {
                repository.getLofiSongs().getOrNull()?.let { songs ->
                    _uiState.value = _uiState.value.copy(lofiSongs = songs)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading Lo-Fi songs", e)
            }
        }
        
        viewModelScope.launch {
            try {
                repository.getDevotionalSongs().getOrNull()?.let { songs ->
                    _uiState.value = _uiState.value.copy(devotionalSongs = songs)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading Devotional songs", e)
            }
        }
        
        viewModelScope.launch {
            try {
                repository.getWorkoutSongs().getOrNull()?.let { songs ->
                    _uiState.value = _uiState.value.copy(workoutSongs = songs)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading Workout songs", e)
            }
        }
        
        viewModelScope.launch {
            try {
                repository.getRetroSongs().getOrNull()?.let { songs ->
                    _uiState.value = _uiState.value.copy(retroSongs = songs)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading Retro songs", e)
            }
        }
    }
    
    private fun loadArtistSpotlights() {
        viewModelScope.launch {
            try {
                repository.getArijitSinghSongs().getOrNull()?.let { songs ->
                    _uiState.value = _uiState.value.copy(arijitSinghSongs = songs)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading Arijit Singh songs", e)
            }
        }
        
        viewModelScope.launch {
            try {
                repository.getARRahmanSongs().getOrNull()?.let { songs ->
                    _uiState.value = _uiState.value.copy(arRahmanSongs = songs)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading AR Rahman songs", e)
            }
        }
        
        viewModelScope.launch {
            try {
                repository.getShreyaGhoshalSongs().getOrNull()?.let { songs ->
                    _uiState.value = _uiState.value.copy(shreyaGhoshalSongs = songs)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading Shreya Ghoshal songs", e)
            }
        }
    }
    
    fun selectGenre(genre: Genre) {
        _uiState.value = _uiState.value.copy(selectedGenre = genre)
        loadGenreSongs(genre)
    }
    
    private fun loadGenreSongs(genre: Genre) {
        viewModelScope.launch {
            try {
                repository.searchSongsWithLimit("${genre.name} songs", 30).getOrNull()?.let { songs ->
                    _uiState.value = _uiState.value.copy(genreSongs = songs)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading ${genre.name} songs", e)
            }
        }
    }
    
    fun setPreferredSource(source: MusicSource) {
        viewModelScope.launch {
            userPreferences.setPreferredSource(source)
            // Reload content with new source
            loadHomeContent()
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun refresh() {
        loadHomeContent()
    }
}
