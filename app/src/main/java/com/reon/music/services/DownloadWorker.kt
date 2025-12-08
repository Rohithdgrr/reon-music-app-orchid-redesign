/*
 * REON Music App - Download Worker
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 */

package com.reon.music.services

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import com.reon.music.data.database.ReonDatabase
import com.reon.music.data.database.entities.DownloadState

/**
 * Download Worker for downloading songs using WorkManager
 * Note: Uses standard Worker instead of HiltWorker for simplicity
 */
class DownloadWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        private const val TAG = "DownloadWorker"
        const val KEY_SONG_ID = "song_id"
        const val KEY_STREAM_URL = "stream_url"
        const val KEY_TITLE = "title"
        const val KEY_QUALITY = "quality"
        const val KEY_PROGRESS = "progress"
        const val KEY_FILE_PATH = "file_path"
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val songId = inputData.getString(KEY_SONG_ID) ?: return@withContext Result.failure()
        val streamUrl = inputData.getString(KEY_STREAM_URL) ?: return@withContext Result.failure()
        val title = inputData.getString(KEY_TITLE) ?: "download"
        val quality = inputData.getString(KEY_QUALITY) ?: "320"
        
        Log.d(TAG, "Starting download for: $title ($songId)")
        
        try {
            // Create downloads directory
            val downloadsDir = File(context.getExternalFilesDir(null), "downloads")
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            
            // Clean filename
            val cleanTitle = title.replace(Regex("[^a-zA-Z0-9\\s-]"), "").take(50)
            val fileName = "${cleanTitle}_${quality}kbps.mp3"
            val outputFile = File(downloadsDir, fileName)
            
            // Download the file using simple URL connection
            val url = URL(streamUrl)
            val connection = url.openConnection()
            connection.connect()
            
            val contentLength = connection.contentLength
            
            // Write to file with progress
            connection.getInputStream().use { input ->
                outputFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var totalBytesRead = 0L
                    var bytesRead: Int
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        
                        // Report progress
                        if (contentLength > 0) {
                            val progress = ((totalBytesRead * 100) / contentLength).toInt()
                            setProgress(workDataOf(KEY_PROGRESS to progress))
                        }
                    }
                }
            }
            
            Log.d(TAG, "Download completed: ${outputFile.absolutePath}")
            
            // Persist download state + path so offline playback works
            try {
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    ReonDatabase::class.java,
                    ReonDatabase.DATABASE_NAME
                ).build()
                db.songDao().updateDownloadState(
                    songId = songId,
                    state = DownloadState.DOWNLOADED,
                    path = outputFile.absolutePath
                )
            } catch (dbError: Exception) {
                Log.e(TAG, "Failed to update download state", dbError)
            }
            
            // Return success with file path
            Result.success(
                workDataOf(
                    KEY_FILE_PATH to outputFile.absolutePath,
                    KEY_SONG_ID to songId
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Download error", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
