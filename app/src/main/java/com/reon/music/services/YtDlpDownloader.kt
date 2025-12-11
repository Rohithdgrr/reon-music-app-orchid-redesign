/*
 * REON Music App - yt-dlp Downloader Helper
 * Copyright (c) 2024 REON
 * Alternative download strategy using yt-dlp for enhanced compatibility
 */

package com.reon.music.services

import android.content.Context
import android.os.Build
import android.util.Log
import com.reon.music.core.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * YtDlpDownloader
 * Provides alternative download mechanism using yt-dlp command-line tool
 * 
 * NOTE: yt-dlp must be installed on the system:
 * - Install via: pip install yt-dlp
 * - macOS: brew install yt-dlp
 * - Windows: choco install yt-dlp or python -m pip install yt-dlp
 * 
 * Android limitations:
 * - Works best on development machine (Windows/macOS/Linux)
 * - Limited direct support on Android device (requires rooted device)
 * - Recommended for server/PC backend integration
 */
@Singleton
class YtDlpDownloader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "YtDlpDownloader"
        private const val YTDLP_EXECUTABLE = "yt-dlp"
    }
    
    /**
     * Download song using yt-dlp
     * 
     * @param song Song to download
     * @param videoUrl YouTube video URL or similar
     * @param outputPath Directory to save the downloaded file
     * @return File if successful, null otherwise
     */
    suspend fun downloadSongViaYtDlp(
        song: Song,
        videoUrl: String,
        outputPath: String = getDefaultDownloadPath()
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            // Validate input
            if (videoUrl.isBlank()) {
                return@withContext Result.failure(Exception("Video URL is empty"))
            }
            
            // Create output directory if needed
            val downloadDir = File(outputPath)
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }
            
            // Build yt-dlp command
            val outputTemplate = "${song.artist} - ${song.title}.%(ext)s"
            val command = buildYtDlpCommand(videoUrl, outputPath, outputTemplate)
            
            Log.d(TAG, "Executing yt-dlp command: ${command.joinToString(" ")}")
            
            // Execute command
            val process = ProcessBuilder(command)
                .directory(downloadDir)
                .redirectErrorStream(true)
                .start()
            
            // Wait for completion with timeout
            val completed = process.waitFor(15, TimeUnit.MINUTES)
            
            if (!completed) {
                process.destroy()
                return@withContext Result.failure(Exception("yt-dlp download timeout"))
            }
            
            val exitCode = process.exitValue()
            if (exitCode != 0) {
                // Log error output
                val errorOutput = process.inputStream.bufferedReader().use { it.readText() }
                Log.e(TAG, "yt-dlp error (exit code $exitCode): $errorOutput")
                return@withContext Result.failure(Exception("yt-dlp failed with exit code $exitCode"))
            }
            
            // Find downloaded file
            val downloadedFile = downloadDir.listFiles()?.firstOrNull { file ->
                file.name.contains(song.title.take(20), ignoreCase = true) &&
                (file.extension == "mp3" || file.extension == "m4a" || file.extension == "wav")
            }
            
            return@withContext if (downloadedFile != null) {
                Result.success(downloadedFile)
            } else {
                Result.failure(Exception("Downloaded file not found"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "YtDlp download failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Check if yt-dlp is available on system
     */
    suspend fun isYtDlpAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder("which", YTDLP_EXECUTABLE)
                .redirectErrorStream(true)
                .start()
            
            val available = process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0
            Log.d(TAG, "yt-dlp available: $available")
            return@withContext available
        } catch (e: Exception) {
            // Fallback: try direct execution
            return@withContext try {
                val process = ProcessBuilder(YTDLP_EXECUTABLE, "--version")
                    .redirectErrorStream(true)
                    .start()
                process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0
            } catch (e2: Exception) {
                Log.w(TAG, "yt-dlp not found: ${e.message}")
                false
            }
        }
    }
    
    /**
     * Get yt-dlp version info
     */
    suspend fun getYtDlpVersion(): String? = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder(YTDLP_EXECUTABLE, "--version")
                .redirectErrorStream(true)
                .start()
            
            if (process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0) {
                return@withContext process.inputStream.bufferedReader().use { it.readLine() }
            }
            return@withContext null
        } catch (e: Exception) {
            Log.w(TAG, "Could not get yt-dlp version", e)
            return@withContext null
        }
    }
    
    /**
     * Build yt-dlp command with optimal settings
     */
    private fun buildYtDlpCommand(
        videoUrl: String,
        outputPath: String,
        outputTemplate: String
    ): List<String> {
        return listOf(
            YTDLP_EXECUTABLE,
            // Output format: mp3 with best audio quality
            "-f", "bestaudio/best",
            "-x",  // Extract audio
            "--audio-format", "mp3",
            "--audio-quality", "192",
            // Output template
            "-o", outputTemplate,
            // Additional options for reliability
            "--no-warnings",
            "--quiet",
            "--progress",
            "--socket-timeout", "30",
            "--max-downloads", "1",
            // Retry on failure
            "--retries", "3",
            "--fragment-retries", "3",
            // Video URL
            videoUrl
        )
    }
    
    /**
     * Get default download path
     */
    private fun getDefaultDownloadPath(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+: Use app-specific external files directory
            context.getExternalFilesDir("downloads")?.absolutePath
                ?: context.cacheDir.absolutePath
        } else {
            // Fallback to cache directory
            context.cacheDir.absolutePath
        }
    }
    
    /**
     * Install yt-dlp command for user (instructions)
     * 
     * NOTE: Users should follow these steps to enable yt-dlp:
     * 
     * Windows:
     *   1. Install Python 3.8+
     *   2. Run: python -m pip install yt-dlp
     *   3. Add to PATH or use full path
     * 
     * macOS:
     *   1. Install Homebrew: /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
     *   2. Run: brew install yt-dlp
     * 
     * Linux:
     *   1. Ubuntu/Debian: sudo apt-get install yt-dlp
     *   2. Fedora: sudo dnf install yt-dlp
     *   3. Or: pip3 install --upgrade yt-dlp
     * 
     * Android:
     *   - Not directly supported on Android devices
     *   - Can be used via server backend or termux with Python
     */
    fun getInstallationInstructions(): String {
        val os = System.getProperty("os.name")?.lowercase() ?: "unknown"
        
        return when {
            os.contains("windows") -> """
                Windows Installation:
                1. Install Python 3.8+ from python.org
                2. Open Command Prompt and run:
                   python -m pip install yt-dlp
                3. Verify: yt-dlp --version
            """.trimIndent()
            
            os.contains("mac") -> """
                macOS Installation:
                1. Install Homebrew (if not already installed):
                   /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
                2. Run: brew install yt-dlp
                3. Verify: yt-dlp --version
            """.trimIndent()
            
            os.contains("linux") -> """
                Linux Installation:
                1. Ubuntu/Debian:
                   sudo apt-get update && sudo apt-get install yt-dlp
                2. Fedora:
                   sudo dnf install yt-dlp
                3. Or via pip:
                   pip3 install --upgrade yt-dlp
                4. Verify: yt-dlp --version
            """.trimIndent()
            
            else -> """
                yt-dlp Installation:
                - Install via pip: pip install yt-dlp
                - Or use package manager for your OS
                - Verify: yt-dlp --version
            """.trimIndent()
        }
    }
}
