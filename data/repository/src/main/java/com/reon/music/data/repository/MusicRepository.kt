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
     * Search for songs from both sources
     */
    suspend fun searchSongs(query: String, page: Int = 1): Result<List<Song>> = coroutineScope {
        // Search both sources in parallel
        val jiosaavnDeferred = async { jiosaavnClient.searchSongs(query, page) }
        val youtubeDeferred = async { youtubeMusicClient.searchSongs(query) }
        
        val jiosaavnResult = jiosaavnDeferred.await()
        val youtubeResult = youtubeDeferred.await()
        
        val songs = mutableListOf<Song>()
        
        // Add JioSaavn results first (higher quality available)
        jiosaavnResult.getOrNull()?.let { songs.addAll(it) }
        
        // Add YouTube results
        youtubeResult.getOrNull()?.let { songs.addAll(it) }
        
        if (songs.isEmpty()) {
            // Return first error if no results
            when {
                jiosaavnResult is Result.Error -> jiosaavnResult
                youtubeResult is Result.Error -> youtubeResult
                else -> Result.Success(emptyList())
            }
        } else {
            Result.Success(songs)
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
     * Get related songs
     */
    suspend fun getRelatedSongs(song: Song): Result<List<Song>> {
        return when (song.source) {
            "jiosaavn" -> jiosaavnClient.getRelatedSongs(song.id)
            "youtube" -> youtubeMusicClient.getRelatedSongs(song.id)
            else -> Result.Success(emptyList())
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
     * Get featured playlists
     */
    suspend fun getFeaturedPlaylists(): Result<List<Playlist>> {
        return jiosaavnClient.searchPlaylists("featured hindi", 1, 15)
    }
    
    /**
     * Get top artists
     */
    suspend fun getTopArtists(): Result<List<Artist>> {
        return jiosaavnClient.searchArtists("Arijit Singh", 1, 15) // Searching for a top artist often returns similar top artists
        // Alternatively, searching for "artists" might work but "Arijit Singh" or generic "bollywood" is safer
    }
    
    /**
     * Search songs with custom limit
     */
    suspend fun searchSongsWithLimit(query: String, limit: Int): Result<List<Song>> {
        return jiosaavnClient.searchSongs(query, 1, limit)
    }
    
    /**
     * Search playlists
     */
    suspend fun searchPlaylists(query: String): Result<List<Playlist>> {
        return jiosaavnClient.searchPlaylists(query, 1, 15)
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
