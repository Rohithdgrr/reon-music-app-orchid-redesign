/*
 * REON Music App - JioSaavn API Client
 * Copyright (c) 2024 REON
 * 
 * CLEAN-ROOM IMPLEMENTATION
 * This code is independently written based on publicly observable API behavior.
 * No GPL-licensed code has been copied. All implementation logic is original.
 */

package com.reon.music.data.network.jiosaavn

import com.reon.music.core.common.ApiConstants
import com.reon.music.core.common.Result
import com.reon.music.core.common.safeApiCall
import com.reon.music.core.model.Album
import com.reon.music.core.model.Artist
import com.reon.music.core.model.Playlist
import com.reon.music.core.model.SearchResult
import com.reon.music.core.model.Song
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * JioSaavn API Client
 * Clean-room implementation - independently written
 */
@Singleton
class JioSaavnClient @Inject constructor(
    private val httpClient: HttpClient
) {
    private val baseUrl = "https://${ApiConstants.JIOSAAVN_BASE_URL}${ApiConstants.JIOSAAVN_API_PATH}"
    private val baseParams = ApiConstants.JIOSAAVN_API_PARAMS
    
    /**
     * Search for songs
     */
    suspend fun searchSongs(
        query: String,
        page: Int = 1,
        limit: Int = 20
    ): Result<List<Song>> = safeApiCall {
        val url = "$baseUrl?$baseParams&__call=search.getResults&p=$page&q=$query&n=$limit"
        
        val response: HttpResponse = httpClient.get(url) {
            header("Accept", "application/json")
            header("User-Agent", USER_AGENT)
        }
        
        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val results = json["results"]?.jsonArray ?: return@safeApiCall emptyList()
        
        results.mapNotNull { element ->
            parseSongFromJson(element.jsonObject)
        }
    }
    
    /**
     * Search for playlists
     */
    suspend fun searchPlaylists(
        query: String,
        page: Int = 1,
        limit: Int = 20
    ): Result<List<Playlist>> = safeApiCall {
        val url = "$baseUrl?$baseParams&__call=search.getPlaylistResults&p=$page&q=$query&n=$limit"
        
        val response: HttpResponse = httpClient.get(url) {
            header("Accept", "application/json")
            header("User-Agent", USER_AGENT)
        }
        
        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val results = json["results"]?.jsonArray ?: return@safeApiCall emptyList()
        
        results.mapNotNull { element ->
            parsePlaylistFromSearchJson(element.jsonObject)
        }
    }
    
    /**
     * Search for artists
     */
    suspend fun searchArtists(
        query: String,
        page: Int = 1,
        limit: Int = 20
    ): Result<List<Artist>> = safeApiCall {
        val url = "$baseUrl?$baseParams&__call=search.getArtistResults&p=$page&q=$query&n=$limit"
        
        val response: HttpResponse = httpClient.get(url) {
            header("Accept", "application/json")
            header("User-Agent", USER_AGENT)
        }
        
        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val results = json["results"]?.jsonArray ?: return@safeApiCall emptyList()
        
        results.mapNotNull { element ->
            parseArtistFromSearchJson(element.jsonObject)
        }
    }
    
    /**
     * Search for albums
     */
    suspend fun searchAlbums(
        query: String,
        page: Int = 1,
        limit: Int = 20
    ): Result<List<Album>> = safeApiCall {
        val url = "$baseUrl?$baseParams&__call=search.getAlbumResults&p=$page&q=$query&n=$limit"
        
        val response: HttpResponse = httpClient.get(url) {
            header("Accept", "application/json")
            header("User-Agent", USER_AGENT)
        }
        
        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val results = json["results"]?.jsonArray ?: return@safeApiCall emptyList()
        
        results.mapNotNull { element ->
            parseAlbumFromSearchJson(element.jsonObject)
        }
    }
    
    /**
     * Get song details by ID
     */
    suspend fun getSongDetails(songId: String): Result<Song?> = safeApiCall {
        val url = "$baseUrl?$baseParams&__call=song.getDetails&pids=$songId"
        
        val response: HttpResponse = httpClient.get(url) {
            header("Accept", "application/json")
            header("User-Agent", USER_AGENT)
        }
        
        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val songs = json["songs"]?.jsonArray ?: json[songId]?.jsonObject?.let { jsonArrayOf(it) }
        
        songs?.firstOrNull()?.jsonObject?.let { parseSongFromJson(it) }
    }
    
    /**
     * Get album details
     */
    suspend fun getAlbumDetails(token: String): Result<Album?> = safeApiCall {
        val url = "$baseUrl?$baseParams&__call=webapi.get&token=$token&type=album"
        
        val response: HttpResponse = httpClient.get(url) {
            header("Accept", "application/json")
            header("User-Agent", USER_AGENT)
        }
        
        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        parseAlbumFromJson(json)
    }
    
    /**
     * Get playlist details
     */
    suspend fun getPlaylistDetails(
        token: String,
        page: Int = 1,
        limit: Int = 100
    ): Result<Playlist?> = safeApiCall {
        val url = "$baseUrl?$baseParams&__call=webapi.get&token=$token&type=playlist&n=$limit&p=$page"
        
        val response: HttpResponse = httpClient.get(url) {
            header("Accept", "application/json")
            header("User-Agent", USER_AGENT)
        }
        
        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        parsePlaylistFromJson(json)
    }
    
    /**
     * Get artist details
     */
    suspend fun getArtistDetails(
        token: String,
        songLimit: Int = 50,
        albumLimit: Int = 50
    ): Result<Artist?> = safeApiCall {
        val url = "$baseUrl?$baseParams&__call=webapi.get&token=$token&type=artist&n_song=$songLimit&n_album=$albumLimit"
        
        val response: HttpResponse = httpClient.get(url) {
            header("Accept", "application/json")
            header("User-Agent", USER_AGENT)
        }
        
        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        parseArtistFromJson(json)
    }
    
    /**
     * Get related songs
     */
    suspend fun getRelatedSongs(songId: String): Result<List<Song>> = safeApiCall {
        val url = "$baseUrl?$baseParams&__call=reco.getreco&pid=$songId"
        
        val response: HttpResponse = httpClient.get(url) {
            header("Accept", "application/json")
            header("User-Agent", USER_AGENT)
        }
        
        val json = Json.parseToJsonElement(response.bodyAsText())
        
        when {
            json is JsonArray -> json.mapNotNull { parseSongFromJson(it.jsonObject) }
            else -> emptyList()
        }
    }
    
    /**
     * Autocomplete search
     */
    suspend fun autocomplete(query: String): Result<SearchResult> = safeApiCall {
        val url = "$baseUrl?$baseParams&__call=autocomplete.get&cc=in&includeMetaTags=1&query=$query"
        
        val response: HttpResponse = httpClient.get(url) {
            header("Accept", "application/json")
            header("User-Agent", USER_AGENT)
        }
        
        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        
        val albums = json["albums"]?.jsonObject?.get("data")?.jsonArray
            ?.mapNotNull { parseAlbumFromSearchJson(it.jsonObject) } ?: emptyList()
            
        val artists = json["artists"]?.jsonObject?.get("data")?.jsonArray
            ?.mapNotNull { parseArtistFromSearchJson(it.jsonObject) } ?: emptyList()
            
        val playlists = json["playlists"]?.jsonObject?.get("data")?.jsonArray
            ?.mapNotNull { parsePlaylistFromSearchJson(it.jsonObject) } ?: emptyList()
        
        SearchResult(
            albums = albums,
            artists = artists,
            playlists = playlists
        )
    }
    
    // --- Parsing helpers ---
    
    private fun parseSongFromJson(json: JsonObject): Song? {
        return try {
            val id = json["id"]?.jsonPrimitive?.content ?: return null
            val title = (json["song"] ?: json["title"])?.jsonPrimitive?.content?.decodeHtml() ?: ""
            
            val moreInfo = json["more_info"]?.jsonObject
            
            // Get artist
            val artist = when {
                moreInfo?.get("music")?.jsonPrimitive?.content?.isNotBlank() == true ->
                    moreInfo["music"]?.jsonPrimitive?.content?.decodeHtml() ?: ""
                moreInfo?.get("artistMap")?.jsonObject?.get("primary_artists")?.jsonArray?.isNotEmpty() == true ->
                    moreInfo["artistMap"]?.jsonObject?.get("primary_artists")?.jsonArray
                        ?.mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.content }
                        ?.joinToString(", ")?.decodeHtml() ?: ""
                else -> json["subtitle"]?.jsonPrimitive?.content?.decodeHtml() ?: ""
            }
            
            // Get encrypted URL and decrypt
            val encryptedUrl = (json["encrypted_media_url"] 
                ?: moreInfo?.get("encrypted_media_url"))?.jsonPrimitive?.content
            
            val streamUrl = encryptedUrl?.let { JioSaavnDecryptor.decrypt(it) }
            
            Song(
                id = id,
                title = title,
                artist = artist,
                album = (json["album"] ?: moreInfo?.get("album"))?.jsonPrimitive?.content?.decodeHtml() ?: "",
                duration = (json["duration"] ?: moreInfo?.get("duration"))?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                artworkUrl = json["image"]?.jsonPrimitive?.content?.toHighQualityImage(),
                streamUrl = streamUrl,
                source = "jiosaavn",
                hasLyrics = (json["has_lyrics"] ?: moreInfo?.get("has_lyrics"))?.jsonPrimitive?.content == "true",
                language = json["language"]?.jsonPrimitive?.content?.capitalize() ?: "",
                year = json["year"]?.jsonPrimitive?.content ?: "",
                releaseDate = (json["release_date"] ?: moreInfo?.get("release_date"))?.jsonPrimitive?.content ?: "",
                is320kbps = (json["320kbps"] ?: moreInfo?.get("320kbps"))?.jsonPrimitive?.content == "true",
                permaUrl = json["perma_url"]?.jsonPrimitive?.content
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parseAlbumFromJson(json: JsonObject): Album? {
        return try {
            Album(
                id = json["id"]?.jsonPrimitive?.content ?: return null,
                name = json["title"]?.jsonPrimitive?.content?.decodeHtml() ?: "",
                artist = json["subtitle"]?.jsonPrimitive?.content?.decodeHtml() ?: "",
                artworkUrl = json["image"]?.jsonPrimitive?.content?.toHighQualityImage(),
                year = json["year"]?.jsonPrimitive?.content ?: json["more_info"]?.jsonObject?.get("year")?.jsonPrimitive?.content ?: "",
                songCount = json["list"]?.jsonArray?.size ?: 0,
                songs = json["list"]?.jsonArray?.mapNotNull { parseSongFromJson(it.jsonObject) } ?: emptyList(),
                permaUrl = json["perma_url"]?.jsonPrimitive?.content,
                language = json["more_info"]?.jsonObject?.get("language")?.jsonPrimitive?.content?.capitalize() ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parseAlbumFromSearchJson(json: JsonObject): Album? {
        return try {
            val permaUrl = json["url"]?.jsonPrimitive?.content ?: json["perma_url"]?.jsonPrimitive?.content
            Album(
                id = json["id"]?.jsonPrimitive?.content ?: return null,
                name = json["title"]?.jsonPrimitive?.content?.decodeHtml() ?: "",
                artist = json["music"]?.jsonPrimitive?.content?.decodeHtml() 
                    ?: json["subtitle"]?.jsonPrimitive?.content?.decodeHtml() ?: "",
                artworkUrl = json["image"]?.jsonPrimitive?.content?.toHighQualityImage(),
                permaUrl = permaUrl
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parsePlaylistFromJson(json: JsonObject): Playlist? {
        return try {
            Playlist(
                id = json["id"]?.jsonPrimitive?.content ?: json["listid"]?.jsonPrimitive?.content ?: return null,
                name = json["title"]?.jsonPrimitive?.content?.decodeHtml() 
                    ?: json["listname"]?.jsonPrimitive?.content?.decodeHtml() ?: "",
                description = json["description"]?.jsonPrimitive?.content?.decodeHtml() 
                    ?: json["subtitle"]?.jsonPrimitive?.content?.decodeHtml() ?: "",
                artworkUrl = json["image"]?.jsonPrimitive?.content?.toHighQualityImage(),
                songCount = json["list"]?.jsonArray?.size ?: 0,
                songs = json["list"]?.jsonArray?.mapNotNull { parseSongFromJson(it.jsonObject) } ?: emptyList(),
                permaUrl = json["perma_url"]?.jsonPrimitive?.content
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parsePlaylistFromSearchJson(json: JsonObject): Playlist? {
        return try {
            Playlist(
                id = json["id"]?.jsonPrimitive?.content ?: return null,
                name = json["title"]?.jsonPrimitive?.content?.decodeHtml() ?: "",
                description = json["subtitle"]?.jsonPrimitive?.content?.decodeHtml() ?: "",
                artworkUrl = json["image"]?.jsonPrimitive?.content?.toHighQualityImage(),
                permaUrl = json["url"]?.jsonPrimitive?.content ?: json["perma_url"]?.jsonPrimitive?.content
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parseArtistFromJson(json: JsonObject): Artist? {
        return try {
            Artist(
                id = json["id"]?.jsonPrimitive?.content ?: json["artistId"]?.jsonPrimitive?.content ?: return null,
                name = json["name"]?.jsonPrimitive?.content?.decodeHtml() 
                    ?: json["title"]?.jsonPrimitive?.content?.decodeHtml() ?: "",
                artworkUrl = json["image"]?.jsonPrimitive?.content?.toHighQualityImage(),
                followerCount = json["follower_count"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                topSongs = json["topSongs"]?.jsonArray?.mapNotNull { parseSongFromJson(it.jsonObject) } ?: emptyList(),
                albums = json["topAlbums"]?.jsonArray?.mapNotNull { parseAlbumFromSearchJson(it.jsonObject) } ?: emptyList(),
                permaUrl = json["perma_url"]?.jsonPrimitive?.content ?: json["url"]?.jsonPrimitive?.content
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parseArtistFromSearchJson(json: JsonObject): Artist? {
        return try {
            Artist(
                id = json["id"]?.jsonPrimitive?.content ?: return null,
                name = json["title"]?.jsonPrimitive?.content?.decodeHtml() 
                    ?: json["name"]?.jsonPrimitive?.content?.decodeHtml() ?: "",
                artworkUrl = json["image"]?.jsonPrimitive?.content?.toHighQualityImage(),
                permaUrl = json["url"]?.jsonPrimitive?.content ?: json["perma_url"]?.jsonPrimitive?.content
            )
        } catch (e: Exception) {
            null
        }
    }
    
    // --- String extensions ---
    
    private fun String.decodeHtml(): String {
        return this
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
    }
    
    private fun String.toHighQualityImage(): String {
        return this
            .replace("http:", "https:")
            .replace("50x50", "500x500")
            .replace("150x150", "500x500")
    }
    
    private fun String.capitalize(): String {
        return this.lowercase().replaceFirstChar { it.uppercase() }
    }
    
    private fun jsonArrayOf(vararg elements: JsonElement): JsonArray {
        return buildJsonArray { elements.forEach { add(it) } }
    }
    
    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
}
