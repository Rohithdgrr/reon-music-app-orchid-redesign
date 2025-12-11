/*
 * REON Music App - Offline Mode Manager
 * Copyright (c) 2024 REON
 * Manages offline playback and local file caching
 */

package com.reon.music.services

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import com.reon.music.data.database.dao.SongDao
import com.reon.music.data.database.entities.DownloadState

/**
 * Manages offline playback and ensures downloaded files are accessible
 */
@Singleton
class OfflineModeManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songDao: SongDao
) {
    companion object {
        private const val TAG = "OfflineModeManager"
        private const val DOWNLOADS_DIR = "downloads"
    }
    
    /**
     * Get the downloads directory
     */
    fun getDownloadsDirectory(): File {
        val dir = File(context.getExternalFilesDir(null), DOWNLOADS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
    
    /**
     * Get a song's local file if available
     */
    suspend fun getLocalFile(songId: String): File? {
        return try {
            val song = songDao.getSongById(songId)
            if (song?.downloadState == DownloadState.DOWNLOADED && song.localPath != null) {
                val localPath = song.localPath!!
                val file = File(localPath)
                if (file.exists() && file.isFile && file.canRead() && file.length() > 0) {
                    Log.d(TAG, "Found valid offline file for $songId: ${file.absolutePath}")
                    return file
                } else {
                    Log.w(TAG, "Offline file invalid for $songId: exists=${file.exists()}, readable=${file.canRead()}, size=${file.length()}")
                    // Clean up invalid entry
                    songDao.updateDownloadState(songId, DownloadState.FAILED, null)
                    null
                }
            } else {
                Log.d(TAG, "Song $songId not downloaded")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting local file for $songId", e)
            null
        }
    }
    
    /**
     * Get all downloaded files and verify they're valid
     */
    suspend fun getDownloadedFiles(): Map<String, File> {
        return try {
            val downloadedSongs = songDao.getDownloadedSongs()
            val downloadsDir = getDownloadsDirectory()
            val validFiles = mutableMapOf<String, File>()
            
            Log.d(TAG, "Checking ${downloadsDir.absolutePath} for files")
            
            downloadsDir.listFiles()?.forEach { file ->
                if (file.isFile && file.canRead() && file.length() > 0) {
                    Log.d(TAG, "Found valid file: ${file.name} (${file.length()} bytes)")
                    validFiles[file.name] = file
                }
            }
            
            Log.d(TAG, "Total valid downloaded files: ${validFiles.size}")
            validFiles
        } catch (e: Exception) {
            Log.e(TAG, "Error getting downloaded files", e)
            emptyMap()
        }
    }
    
    /**
     * Verify and repair offline file references
     */
    suspend fun verifyOfflineFiles() {
        try {
            Log.d(TAG, "Verifying offline file references...")
            
            val songs = songDao.getDownloadedSongs().first()
            for (song in songs) {
                if (song.localPath != null) {
                    val localPath = song.localPath!!
                    val file = File(localPath)
                    if (!file.exists() || !file.isFile || !file.canRead() || file.length() == 0L) {
                        Log.w(TAG, "Invalid file reference for ${song.title}: ${song.localPath}")
                        songDao.updateDownloadState(
                            songId = song.id,
                            state = DownloadState.FAILED,
                            path = null
                        )
                    } else {
                        Log.d(TAG, "Valid file for ${song.title}: ${file.absolutePath} (${file.length()} bytes)")
                    }
                }
            }
            
            Log.d(TAG, "Offline file verification complete")
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying offline files", e)
        }
    }
    
    /**
     * Delete a downloaded file
     */
    suspend fun deleteDownloadedFile(songId: String): Boolean {
        return try {
            val song = songDao.getSongById(songId)
            if (song?.localPath != null) {
                val localPath = song.localPath!!
                val file = File(localPath)
                if (file.exists() && file.delete()) {
                    Log.d(TAG, "Deleted file: ${file.absolutePath}")
                    
                    // Update database
                    songDao.updateDownloadState(
                        songId = songId,
                        state = DownloadState.NOT_DOWNLOADED,
                        path = null
                    )
                    true
                } else {
                    Log.w(TAG, "Failed to delete file: ${file.absolutePath}")
                    false
                }
            } else {
                Log.w(TAG, "No file path for song $songId")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting downloaded file", e)
            false
        }
    }
    
    /**
     * Get total size of all downloaded files
     */
    suspend fun getTotalDownloadedSize(): Long {
        return try {
            val songs = songDao.getDownloadedSongs().first()
            var totalSize = 0L
            
            for (song in songs) {
                if (song?.localPath != null) {
                    val localPath = song.localPath!!
                    val file = File(localPath)
                    if (file.exists()) {
                        totalSize += file.length()
                    }
                }
            }
            
            Log.d(TAG, "Total downloaded size: $totalSize bytes (${totalSize / (1024 * 1024)} MB)")
            totalSize
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating download size", e)
            0L
        }
    }
    
    /**
     * Clear all offline files
     */
    suspend fun clearAllOfflineFiles(): Boolean {
        return try {
            val downloadsDir = getDownloadsDirectory()
            var successCount = 0
            var failCount = 0
            
            downloadsDir.listFiles()?.forEach { file ->
                if (file.delete()) {
                    Log.d(TAG, "Deleted: ${file.name}")
                    successCount++
                } else {
                    Log.w(TAG, "Failed to delete: ${file.name}")
                    failCount++
                }
            }
            
            // Clear database entries
            val songs = songDao.getDownloadedSongs().first()
            for (song in songs) {
                songDao.updateDownloadState(
                    songId = song.id,
                    state = DownloadState.NOT_DOWNLOADED,
                    path = null
                )
            }
            
            Log.d(TAG, "Clear complete - Deleted: $successCount, Failed: $failCount")
            failCount == 0
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing offline files", e)
            false
        }
    }
}
