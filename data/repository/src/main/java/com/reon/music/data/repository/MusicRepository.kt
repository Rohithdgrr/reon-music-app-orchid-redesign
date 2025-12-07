/*
 * REON Music App - Music Repository
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 */

package com.reon.music.data.repository

import com.reon.music.core.common.Result
import com.reon.music.core.model.Album
import com.reon.music.core.model.Artist
import com.reon.music.core.model.Playlist
import com.reon.music.core.model.SearchResult
import com.reon.music.core.model.Song
import com.reon.music.core.model.SongSortOption
import com.reon.music.data.network.jiosaavn.JioSaavnClient
import com.reon.music.data.network.youtube.PipedClient
import com.reon.music.data.network.youtube.YouTubeMusicClient
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Music Repository
 * Combines JioSaavn and YouTube Music sources
 */
@Singleton
class MusicRepository @Inject constructor(
    private val jiosaavnClient: JioSaavnClient,
    private val youtubeMusicClient: YouTubeMusicClient,
    private val pipedClient: PipedClient
) {
    /**
     * Search for songs from both sources - Enhanced to ensure all YouTube songs are accessible
     */
    suspend fun searchSongs(query: String, page: Int = 1): Result<List<Song>> = coroutineScope {
        // Search both sources in parallel with multiple attempts
        val jiosaavnDeferred = async { jiosaavnClient.searchSongs(query, page) }
        val youtubeDeferred = async { youtubeMusicClient.searchSongs(query) }
        
        // Also try alternative search strategies for YouTube
        val youtubeAltDeferred = async { 
            // Try with "official" keyword
            youtubeMusicClient.searchSongs("$query official").getOrNull() ?: emptyList()
        }
        val youtubeMusicDeferred = async {
            // Try with "music" keyword
            youtubeMusicClient.searchSongs("$query music").getOrNull() ?: emptyList()
        }
        
        val jiosaavnResult = jiosaavnDeferred.await()
        val youtubeResult = youtubeDeferred.await()
        val youtubeAlt = youtubeAltDeferred.await()
        val youtubeMusic = youtubeMusicDeferred.await()
        
        val songs = mutableListOf<Song>()
        
        // Add JioSaavn results first (higher quality available)
        jiosaavnResult.getOrNull()?.let { songs.addAll(it) }
        
        // Add YouTube results - combine all YouTube searches
        youtubeResult.getOrNull()?.let { songs.addAll(it) }
        songs.addAll(youtubeAlt)
        songs.addAll(youtubeMusic)
        
        // Remove duplicates by ID
        val uniqueSongs = songs.distinctBy { it.id }
        
        if (uniqueSongs.isEmpty()) {
            // Return first error if no results
            when {
                jiosaavnResult is Result.Error -> jiosaavnResult
                youtubeResult is Result.Error -> youtubeResult
                else -> Result.Success(emptyList())
            }
        } else {
            Result.Success(uniqueSongs)
        }
    }
    
    /**
     * Search songs flow - emits results as they come in
     */
    fun searchSongsFlow(query: String): Flow<List<Song>> = flow {
        val allSongs = mutableListOf<Song>()
        
        // Emit JioSaavn results first
        jiosaavnClient.searchSongs(query).getOrNull()?.let { songs ->
            allSongs.addAll(songs)
            emit(allSongs.toList())
        }
        
        // Then emit combined with YouTube
        youtubeMusicClient.searchSongs(query).getOrNull()?.let { songs ->
            allSongs.addAll(songs)
            emit(allSongs.toList())
        }
    }
    
    /**
     * Get stream URL for a song
     * Tries multiple sources
     */
    suspend fun getStreamUrl(song: Song): Result<String> {
        return when (song.source) {
            "jiosaavn" -> {
                // JioSaavn: URL is usually already decoded
                song.streamUrl?.let { url ->
                    if (url.isNotBlank()) {
                        Result.Success(url)
                    } else {
                        // Try to fetch fresh details
                        val details = jiosaavnClient.getSongDetails(song.id).getOrNull()
                        details?.streamUrl?.let { Result.Success(it) }
                            ?: Result.Error(Exception("No stream URL available"))
                    }
                } ?: Result.Error(Exception("No stream URL"))
            }
            "youtube" -> {
                // Try InnerTube first
                val innerTubeUrl = youtubeMusicClient.getStreamUrl(song.id).getOrNull()
                if (innerTubeUrl != null) {
                    return Result.Success(innerTubeUrl)
                }
                
                // Fallback to Piped
                val pipedUrl = pipedClient.getStreamUrl(song.id).getOrNull()
                if (pipedUrl != null) {
                    return Result.Success(pipedUrl)
                }
                
                Result.Error(Exception("Could not get stream URL"))
            }
            else -> {
                song.streamUrl?.let { Result.Success(it) }
                    ?: Result.Error(Exception("Unknown source"))
            }
        }
    }
    
    /**
     * Get song details
     */
    suspend fun getSongDetails(songId: String, source: String): Result<Song?> {
        return when (source) {
            "jiosaavn" -> jiosaavnClient.getSongDetails(songId)
            "youtube" -> youtubeMusicClient.getSongDetails(songId)
            else -> Result.Error(Exception("Unknown source"))
        }
    }
    
    /**
     * Get album details
     */
    suspend fun getAlbumDetails(token: String): Result<Album?> {
        return jiosaavnClient.getAlbumDetails(token)
    }
    
    /**
     * Get playlist details
     */
    suspend fun getPlaylistDetails(token: String): Result<Playlist?> {
        return jiosaavnClient.getPlaylistDetails(token)
    }
    
    /**
     * Get artist details
     */
    suspend fun getArtistDetails(token: String): Result<Artist?> {
        return jiosaavnClient.getArtistDetails(token)
    }
    
    /**
     * Get related songs - Enhanced to fetch ALL related songs
     */
    suspend fun getRelatedSongs(song: Song, limit: Int = 100): Result<List<Song>> = coroutineScope {
        try {
            val allRelatedSongs = mutableListOf<Song>()
            
            // Strategy 1: Get related songs from API
            when (song.source) {
                "jiosaavn" -> {
                    jiosaavnClient.getRelatedSongs(song.id).getOrNull()?.let { allRelatedSongs.addAll(it) }
                }
                "youtube" -> {
                    youtubeMusicClient.getRelatedSongs(song.id).getOrNull()?.let { allRelatedSongs.addAll(it) }
                }
            }
            
            // Strategy 2: Get songs from same artist
            if (song.artist.isNotBlank()) {
                val artistSongs = searchSongsWithLimit("${song.artist} songs", 30).getOrNull()
                artistSongs?.let { allRelatedSongs.addAll(it) }
            }
            
            // Strategy 3: Get songs from same album
            if (song.album.isNotBlank()) {
                val albumSongs = searchSongsWithLimit("${song.album} ${song.artist}", 20).getOrNull()
                albumSongs?.let { allRelatedSongs.addAll(it) }
            }
            
            // Strategy 4: Get songs from same genre
            if (song.genre.isNotBlank()) {
                val genreSongs = getSongsByGenre(song.genre).getOrNull()
                genreSongs?.let { allRelatedSongs.addAll(it) }
            }
            
            // Strategy 5: Get similar songs by searching title keywords
            val titleKeywords = song.title.split(" ").take(2)
            if (titleKeywords.isNotEmpty()) {
                val keywordQuery = titleKeywords.joinToString(" ")
                val similarSongs = searchSongsWithLimit(keywordQuery, 20).getOrNull()
                similarSongs?.let { allRelatedSongs.addAll(it) }
            }
            
            // Deduplicate and limit
            val uniqueSongs = allRelatedSongs
                .distinctBy { it.id }
                .filter { it.id != song.id }
                .take(limit)
            
            Result.Success(uniqueSongs)
        } catch (e: Exception) {
            Result.Error(Exception("Error fetching related songs: ${e.message}"))
        }
    }
    
    /**
     * Autocomplete search
     */
    suspend fun autocomplete(query: String): Result<SearchResult> {
        return jiosaavnClient.autocomplete(query)
    }
    
    /**
     * Get trending songs (from JioSaavn)
     */
    suspend fun getTrendingSongs(): Result<List<Song>> {
        return jiosaavnClient.searchSongs("trending songs 2024", 1, 20)
    }
    
    /**
     * Get new releases
     */
    suspend fun getNewReleases(): Result<List<Song>> {
        return jiosaavnClient.searchSongs("latest hindi songs 2024", 1, 20)
    }
    
    /**
     * Get Top 50 Hindi songs
     */
    suspend fun getTop50Hindi(): Result<List<Song>> {
        return jiosaavnClient.searchSongs("top 50 hindi songs", 1, 50)
    }
    
    /**
     * Get Top 100 songs
     */
    suspend fun getTop100(): Result<List<Song>> {
        return jiosaavnClient.searchSongs("top 100 bollywood songs", 1, 50)
    }
    
    /**
     * Get Telugu songs
     */
    suspend fun getTeluguSongs(): Result<List<Song>> {
        return jiosaavnClient.searchSongs("telugu songs 2024", 1, 20)
    }
    
    /**
     * Get Telugu Top songs
     */
    suspend fun getTeluguTop(): Result<List<Song>> {
        return jiosaavnClient.searchSongs("top telugu songs", 1, 20)
    }
    
    /**
     * Get Tamil songs
     */
    suspend fun getTamilSongs(): Result<List<Song>> {
        return jiosaavnClient.searchSongs("tamil songs 2024", 1, 20)
    }
    
    /**
     * Get Punjabi songs
     */
    suspend fun getPunjabiSongs(): Result<List<Song>> {
        return jiosaavnClient.searchSongs("punjabi songs 2024", 1, 20)
    }
    
    /**
     * Get English Top songs
     */
    suspend fun getEnglishSongs(): Result<List<Song>> {
        return jiosaavnClient.searchSongs("english pop songs 2024", 1, 20)
    }
    
    /**
     * Get Romantic songs
     */
    suspend fun getRomanticSongs(): Result<List<Song>> {
        return jiosaavnClient.searchSongs("romantic hindi songs", 1, 20)
    }
    
    /**
     * Get Party/Dance songs
     */
    suspend fun getPartySongs(): Result<List<Song>> {
        return jiosaavnClient.searchSongs("party bollywood songs dance", 1, 20)
    }
    
    /**
     * Get Sad songs
     */
    suspend fun getSadSongs(): Result<List<Song>> {
        return jiosaavnClient.searchSongs("sad hindi songs", 1, 20)
    }
    
    /**
     * Get Devotional songs
     */
    suspend fun getDevotionalSongs(): Result<List<Song>> {
        return jiosaavnClient.searchSongs("devotional bhajan songs", 1, 20)
    }
    
    /**
     * Get Lo-Fi/Chill songs
     */
    suspend fun getLofiSongs(): Result<List<Song>> {
        return jiosaavnClient.searchSongs("lofi hindi songs", 1, 20)
    }
    
    /**
     * Get Workout songs
     */
    suspend fun getWorkoutSongs(): Result<List<Song>> {
        return jiosaavnClient.searchSongs("workout gym songs", 1, 20)
    }
    
    /**
     * Get 90s Retro songs
     */
    suspend fun getRetroSongs(): Result<List<Song>> {
        return jiosaavnClient.searchSongs("90s bollywood songs", 1, 20)
    }
    
    /**
     * Get Arijit Singh songs
     */
    suspend fun getArijitSinghSongs(): Result<List<Song>> {
        return jiosaavnClient.searchSongs("arijit singh songs", 1, 20)
    }
    
    /**
     * Get AR Rahman songs
     */
    suspend fun getARRahmanSongs(): Result<List<Song>> {
        return jiosaavnClient.searchSongs("ar rahman songs", 1, 20)
    }
    
    /**
     * Get Atif Aslam songs
     */
    suspend fun getAtifAslamSongs(): Result<List<Song>> {
        return jiosaavnClient.searchSongs("atif aslam songs", 1, 20)
    }
    
    /**
     * Get Shreya Ghoshal songs
     */
    suspend fun getShreyaGhoshalSongs(): Result<List<Song>> {
        return jiosaavnClient.searchSongs("shreya ghoshal songs", 1, 20)
    }
    
    /**
     * Get songs by language
     */
    suspend fun getSongsByLanguage(language: String): Result<List<Song>> {
        return jiosaavnClient.searchSongs("$language songs 2024", 1, 30)
    }
    
    /**
     * Get songs by genre
     */
    suspend fun getSongsByGenre(genre: String): Result<List<Song>> {
        return jiosaavnClient.searchSongs("$genre songs", 1, 20)
    }
    
    /**
     * Get songs by mood
     */
    suspend fun getSongsByMood(mood: String): Result<List<Song>> {
        return jiosaavnClient.searchSongs("$mood mood songs", 1, 20)
    }
    
    /**
     * Get featured playlists - Enhanced to fetch from both sources
     */
    suspend fun getFeaturedPlaylists(): Result<List<Playlist>> = coroutineScope {
        // Fetch playlists from multiple sources and queries
        val jiosaavnFeaturedDeferred = async { jiosaavnClient.searchPlaylists("featured hindi", 1, 10) }
        val jiosaavnTrendingDeferred = async { jiosaavnClient.searchPlaylists("trending", 1, 10) }
        val jiosaavnPopularDeferred = async { jiosaavnClient.searchPlaylists("popular", 1, 10) }
        val youtubeDeferred = async { youtubeMusicClient.searchPlaylists("trending playlists", 1, 10) }
        
        val allPlaylists = mutableListOf<Playlist>()
        
        // Add JioSaavn playlists
        jiosaavnFeaturedDeferred.await().getOrNull()?.let { allPlaylists.addAll(it) }
        jiosaavnTrendingDeferred.await().getOrNull()?.let { allPlaylists.addAll(it) }
        jiosaavnPopularDeferred.await().getOrNull()?.let { allPlaylists.addAll(it) }
        
        // Add YouTube playlists
        youtubeDeferred.await().getOrNull()?.let { allPlaylists.addAll(it) }
        
        // Remove duplicates by ID
        val uniquePlaylists = allPlaylists.distinctBy { it.id }
        
        if (uniquePlaylists.isEmpty()) {
            // Fallback to single query
            jiosaavnClient.searchPlaylists("bollywood playlists", 1, 20)
        } else {
            Result.Success(uniquePlaylists.take(30)) // Return more playlists
        }
    }
    
    /**
     * Get top artists
     */
    suspend fun getTopArtists(): Result<List<Artist>> {
        return jiosaavnClient.searchArtists("Arijit Singh", 1, 15) // Searching for a top artist often returns similar top artists
        // Alternatively, searching for "artists" might work but "Arijit Singh" or generic "bollywood" is safer
    }
    
    /**
     * Search songs with custom limit - Enhanced to search both sources for endless results
     */
    suspend fun searchSongsWithLimit(query: String, limit: Int): Result<List<Song>> = coroutineScope {
        // Search both sources in parallel
        val jiosaavnDeferred = async { jiosaavnClient.searchSongs(query, 1, limit) }
        val youtubeDeferred = async { youtubeMusicClient.searchSongs(query) }
        
        val allSongs = mutableListOf<Song>()
        
        // Add JioSaavn results first
        jiosaavnDeferred.await().getOrNull()?.let { allSongs.addAll(it) }
        
        // Add YouTube results (they may have different limit, so we take what we need)
        youtubeDeferred.await().getOrNull()?.let { songs ->
            allSongs.addAll(songs.take(limit))
        }
        
        if (allSongs.isEmpty()) {
            jiosaavnClient.searchSongs(query, 1, limit)
        } else {
            Result.Success(allSongs.distinctBy { it.id }.take(limit))
        }
    }
    
    /**
     * Search songs with unlimited results from YouTube
     */
    fun searchSongsUnlimited(query: String, maxResults: Int = 1000): Flow<List<Song>> {
        return youtubeMusicClient.searchSongsUnlimited(query, maxResults)
    }
    
    /**
     * Sort songs by various criteria
     */
    fun sortSongs(songs: List<Song>, sortBy: SongSortOption): List<Song> {
        return when (sortBy) {
            SongSortOption.VIEWS -> songs.sortedByDescending { it.viewCount }
            SongSortOption.LIKES -> songs.sortedByDescending { it.likeCount }
            SongSortOption.CHANNEL_POPULARITY -> songs.sortedByDescending { it.channelSubscriberCount }
            SongSortOption.QUALITY -> songs.sortedByDescending { 
                when {
                    it.is320kbps -> 3
                    it.quality.contains("HD", ignoreCase = true) -> 2
                    it.quality.contains("4K", ignoreCase = true) -> 4
                    else -> 1
                }
            }
            SongSortOption.RECENT -> songs.sortedByDescending { it.uploadDate }
            SongSortOption.DURATION -> songs.sortedByDescending { it.duration }
            SongSortOption.TITLE -> songs.sortedBy { it.title.lowercase() }
            SongSortOption.ARTIST -> songs.sortedBy { it.artist.lowercase() }
            SongSortOption.DEFAULT -> songs
        }
    }
    
    /**
     * Get video metadata for a song
     */
    suspend fun getVideoMetadata(videoId: String): Result<com.reon.music.data.network.youtube.VideoMetadata> {
        return youtubeMusicClient.getVideoMetadata(videoId)
    }
    
    /**
     * Enhance songs with metadata
     */
    suspend fun enhanceSongsWithMetadata(songs: List<Song>): List<Song> = coroutineScope {
        songs.map { song ->
            if (song.source == "youtube" && song.viewCount == 0L) {
                // Fetch metadata if not already present
                val metadata = youtubeMusicClient.getVideoMetadata(song.id).getOrNull()
                metadata?.let {
                    song.copy(
                        viewCount = it.viewCount,
                        likeCount = it.likeCount,
                        channelName = it.channelName.ifEmpty { song.channelName },
                        channelId = it.channelId.ifEmpty { song.channelId },
                        uploadDate = it.uploadDate.ifEmpty { song.uploadDate }
                    )
                } ?: song
            } else {
                song
            }
        }
    }

    
    /**
     * Search playlists - Enhanced to search both sources
     */
    suspend fun searchPlaylists(query: String): Result<List<Playlist>> = coroutineScope {
        val jiosaavnDeferred = async { jiosaavnClient.searchPlaylists(query, 1, 20) }
        val youtubeDeferred = async { youtubeMusicClient.searchPlaylists(query, 1, 20) }
        
        val allPlaylists = mutableListOf<Playlist>()
        
        jiosaavnDeferred.await().getOrNull()?.let { allPlaylists.addAll(it) }
        youtubeDeferred.await().getOrNull()?.let { allPlaylists.addAll(it) }
        
        if (allPlaylists.isEmpty()) {
            jiosaavnClient.searchPlaylists(query, 1, 15)
        } else {
            Result.Success(allPlaylists.distinctBy { it.id }.take(30))
        }
    }
    
    /**
     * Search albums
     */
    suspend fun searchAlbums(query: String): Result<List<Album>> {
        return jiosaavnClient.searchAlbums(query, 1, 15)
    }
    
    /**
     * Search artists
     */
    suspend fun searchArtists(query: String): Result<List<Artist>> {
        return jiosaavnClient.searchArtists(query, 1, 15)
    }
    
    /**
     * Get trending albums
     */
    suspend fun getTrendingAlbums(): Result<List<Album>> {
        return jiosaavnClient.searchAlbums("bollywood albums 2024", 1, 15)
    }
}
