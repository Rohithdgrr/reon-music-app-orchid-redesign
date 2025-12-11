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
            
            val usedYtDlp = tryYtDlp(streamUrl, outputFile)
            
            if (!usedYtDlp) {
                // Fallback: Download using simple URL connection
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
            }
            
            Log.d(TAG, "Download completed: ${outputFile.absolutePath}")
            
            // Verify file exists and is readable
            if (!outputFile.exists() || !outputFile.isFile || outputFile.length() == 0L) {
                Log.e(TAG, "Downloaded file is invalid: ${outputFile.absolutePath}")
                return@withContext Result.failure()
            }
            Log.d(TAG, "File verified - Size: ${outputFile.length()} bytes")
            
            // Persist download state + path so offline playback works
            try {
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    ReonDatabase::class.java,
                    ReonDatabase.DATABASE_NAME
                ).build()
                
                val absolutePath = outputFile.absolutePath
                Log.d(TAG, "Updating database with path: $absolutePath")
                
                db.songDao().updateDownloadState(
                    songId = songId,
                    state = DownloadState.DOWNLOADED,
                    path = absolutePath
                )
                
                Log.d(TAG, "Database updated successfully for song: $songId")
            } catch (dbError: Exception) {
                Log.e(TAG, "Failed to update download state", dbError)
                return@withContext Result.failure()
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
    
    /**
     * Try downloading via yt-dlp for YouTube/piped links. Returns true if executed.
     */
    private fun tryYtDlp(streamUrl: String, outputFile: File): Boolean {
        return try {
            // Heuristic: attempt yt-dlp if URL looks like YouTube or lacks file extension
            val shouldUseYt = streamUrl.contains("youtube.com", true) ||
                    streamUrl.contains("youtu.be", true) ||
                    !streamUrl.contains(".mp3", true) && !streamUrl.contains(".m4a", true)
            if (!shouldUseYt) return false
            
            val process = ProcessBuilder(
                "yt-dlp",
                "-f", "bestaudio",
                "-o", outputFile.absolutePath,
                streamUrl
            )
                .redirectErrorStream(true)
                .start()
            
            // Basic read to keep buffer clear and detect completion
            process.inputStream.bufferedReader().use { reader ->
                reader.forEachLine { line ->
                    // yt-dlp prints progress; we could parse but keep lightweight
                    Log.d(TAG, line)
                }
            }
            
            val exit = process.waitFor()
            if (exit != 0) {
                Log.w(TAG, "yt-dlp exited with code $exit, falling back")
                false
            } else {
                true
            }
        } catch (e: Exception) {
            Log.w(TAG, "yt-dlp unavailable or failed: ${e.message}")
            false
        }
    }
}
