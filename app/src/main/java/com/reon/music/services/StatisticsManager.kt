/*
 * REON Music App - Listening Statistics
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 */

package com.reon.music.services

import com.reon.music.data.database.dao.HistoryDao
import com.reon.music.data.database.dao.SongDao
import com.reon.music.data.database.entities.ListenHistoryEntity
import com.reon.music.data.database.entities.SongEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Listening Statistics data classes
 */
data class ListeningStats(
    val totalPlayCount: Int = 0,
    val totalListeningTimeMs: Long = 0,
    val uniqueSongsPlayed: Int = 0,
    val completedPlays: Int = 0,
    val songsPlayedToday: Int = 0
) {
    val totalListeningTimeMinutes: Int get() = (totalListeningTimeMs / 60000).toInt()
    val totalListeningTimeHours: Float get() = totalListeningTimeMs / 3600000f
    
    fun formattedListeningTime(): String {
        val hours = totalListeningTimeMs / 3600000
        val minutes = (totalListeningTimeMs % 3600000) / 60000
        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }
    }
}

data class TopItem<T>(
    val item: T,
    val playCount: Int,
    val rank: Int
)

data class DailyStats(
    val date: String,
    val playCount: Int,
    val listeningTimeMs: Long
)

data class WrappedStats(
    val totalMinutesListened: Int,
    val topSongs: List<TopItem<SongEntity>>,
    val topArtists: List<TopItem<String>>,
    val favoriteGenres: List<String>,
    val peakListeningHour: String,
    val songsDiscovered: Int,
    val period: String // "2024", "December 2024", etc.
)

/**
 * Statistics Manager
 * Tracks and provides listening analytics
 */
@Singleton
class StatisticsManager @Inject constructor(
    private val historyDao: HistoryDao,
    private val songDao: SongDao
) {
    
    /**
     * Get current listening stats
     */
    fun getStats(): Flow<ListeningStats> {
        val startOfToday = getStartOfDay()
        
        return combine(
            songDao.getTotalPlayCount().map { it ?: 0 },
            historyDao.getTotalListeningTime().map { it ?: 0L },
            historyDao.getCompletedPlaysCount(),
            songDao.getLibrarySize()
        ) { totalCount, listeningTime, completedPlays, librarySize ->
            ListeningStats(
                totalPlayCount = totalCount,
                totalListeningTimeMs = listeningTime,
                uniqueSongsPlayed = librarySize,
                completedPlays = completedPlays,
                songsPlayedToday = 0 // Calculated separately
            )
        }
    }
    
    /**
     * Get top songs
     */
    fun getTopSongs(limit: Int = 10): Flow<List<TopItem<SongEntity>>> {
        return songDao.getMostPlayed(limit).map { songs ->
            songs.mapIndexed { index, song ->
                TopItem(
                    item = song,
                    playCount = song.playCount,
                    rank = index + 1
                )
            }
        }
    }
    
    /**
     * Get recently played songs
     */
    fun getRecentlyPlayed(limit: Int = 50): Flow<List<SongEntity>> {
        return songDao.getRecentlyPlayed(limit)
    }
    
    /**
     * Record a play
     */
    suspend fun recordPlay(
        songId: String,
        playDuration: Long,
        completed: Boolean,
        source: String = "unknown"
    ) {
        // Insert history entry
        val history = ListenHistoryEntity(
            songId = songId,
            playedAt = System.currentTimeMillis(),
            playDuration = playDuration,
            completedPlay = completed,
            source = source
        )
        historyDao.insert(history)
        
        // Update song play count
        songDao.incrementPlayCount(songId)
    }
    
    /**
     * Get listening data for a date range
     */
    suspend fun getListeningHistory(
        startTime: Long,
        endTime: Long
    ): List<ListenHistoryEntity> {
        return historyDao.getHistoryBetween(startTime, endTime)
    }
    
    /**
     * Get daily listening stats for the past N days
     */
    suspend fun getDailyStats(days: Int = 7): List<DailyStats> {
        val result = mutableListOf<DailyStats>()
        val calendar = Calendar.getInstance()
        
        for (i in 0 until days) {
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            
            val startOfDay = getStartOfDay(calendar.timeInMillis)
            val endOfDay = startOfDay + (24 * 60 * 60 * 1000) - 1
            
            val history = historyDao.getHistoryBetween(startOfDay, endOfDay)
            val listeningTime = historyDao.getListeningTimeBetween(startOfDay, endOfDay) ?: 0L
            
            val dateStr = "%02d/%02d".format(
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.MONTH) + 1
            )
            
            result.add(DailyStats(
                date = dateStr,
                playCount = history.size,
                listeningTimeMs = listeningTime
            ))
        }
        
        return result.reversed()
    }
    
    /**
     * Get "REON Wrapped" style stats
     */
    suspend fun getWrappedStats(year: Int = Calendar.getInstance().get(Calendar.YEAR)): WrappedStats {
        val calendar = Calendar.getInstance()
        calendar.set(year, Calendar.JANUARY, 1, 0, 0, 0)
        val startTime = calendar.timeInMillis
        
        calendar.set(year, Calendar.DECEMBER, 31, 23, 59, 59)
        val endTime = calendar.timeInMillis
        
        val history = historyDao.getHistoryBetween(startTime, endTime)
        val totalTime = historyDao.getListeningTimeBetween(startTime, endTime) ?: 0L
        
        // Get top songs
        val songPlayCounts = history.groupBy { it.songId }
            .mapValues { it.value.size }
            .entries
            .sortedByDescending { it.value }
            .take(10)
        
        val topSongs = songPlayCounts.mapIndexedNotNull { index, entry ->
            val song = songDao.getSongById(entry.key)
            song?.let {
                TopItem(it, entry.value, index + 1)
            }
        }
        
        // Get top artists
        val artistPlayCounts = mutableMapOf<String, Int>()
        topSongs.forEach { item ->
            val artist = item.item.artist
            artistPlayCounts[artist] = artistPlayCounts.getOrDefault(artist, 0) + item.playCount
        }
        
        val topArtists = artistPlayCounts.entries
            .sortedByDescending { it.value }
            .take(5)
            .mapIndexed { index, entry ->
                TopItem(entry.key, entry.value, index + 1)
            }
        
        return WrappedStats(
            totalMinutesListened = (totalTime / 60000).toInt(),
            topSongs = topSongs,
            topArtists = topArtists,
            favoriteGenres = emptyList(), // Would need genre data
            peakListeningHour = "9 PM", // Simplified
            songsDiscovered = history.map { it.songId }.distinct().size,
            period = year.toString()
        )
    }
    
    private fun getStartOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
