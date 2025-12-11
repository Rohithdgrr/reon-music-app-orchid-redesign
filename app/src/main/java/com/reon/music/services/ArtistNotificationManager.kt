/*
 * REON Music App - Artist Notification Manager
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 */

package com.reon.music.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.Manifest
import com.reon.music.core.model.Artist
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Artist Notification Manager
 * Sends push notifications when followed artists release new content
 */
@Singleton
class ArtistNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ArtistNotificationManager"
        private const val CHANNEL_ID = "artist_notifications"
        private const val CHANNEL_NAME = "Artist Notifications"
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _isEnabled = MutableStateFlow(true)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()
    
    init {
        createNotificationChannel()
    }
    
    /**
     * Create notification channel for Android O+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for new releases from followed artists"
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Enable/disable artist notifications
     */
    fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
    }
    
    /**
     * Follow an artist to receive notifications
     */
    fun followArtist(artist: Artist) {
        scope.launch {
            try {
                // TODO: Store followed artist in database
                // artistDao.insertFollowedArtist(artist)
                // Schedule periodic checks for new releases
                Log.d(TAG, "Following artist: ${artist.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Error following artist", e)
            }
        }
    }
    
    /**
     * Unfollow an artist
     */
    fun unfollowArtist(artistId: String) {
        scope.launch {
            try {
                // TODO: Remove from followed artists
                // artistDao.removeFollowedArtist(artistId)
                Log.d(TAG, "Unfollowing artist: $artistId")
            } catch (e: Exception) {
                Log.e(TAG, "Error unfollowing artist", e)
            }
        }
    }
    
    /**
     * Send notification for new release
     */
    fun notifyNewRelease(artist: Artist, releaseTitle: String, releaseType: String) {
        if (!_isEnabled.value) return
        if (!hasPostNotificationPermission()) {
            Log.w(TAG, "Notification permission not granted; skipping artist notification.")
            return
        }
        
        scope.launch {
            try {
                val intent = Intent(context, Class.forName("com.reon.music.MainActivity")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("artist_id", artist.id)
                }
                
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                
                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .setContentTitle("New $releaseType from ${artist.name}")
                    .setContentText(releaseTitle)
                    .setStyle(NotificationCompat.BigTextStyle()
                        .bigText("${artist.name} just released: $releaseTitle"))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()
                
                NotificationManagerCompat.from(context).notify(
                    artist.id.hashCode(),
                    notification
                )
                
                Log.d(TAG, "Sent notification for ${artist.name}: $releaseTitle")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending notification", e)
            }
        }
    }
    
    /**
     * POST_NOTIFICATIONS runtime permission check (Android 13+)
     */
    private fun hasPostNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
    
    /**
     * Check for new releases from followed artists
     * This would be called periodically by a WorkManager
     */
    suspend fun checkForNewReleases() {
        if (!_isEnabled.value) return
        
        try {
            // TODO: Get list of followed artists
            // val followedArtists = artistDao.getFollowedArtists()
            
            // TODO: For each artist, check for new releases
            // This would query the API for new songs/albums from the artist
            // and compare with what we've already seen
            
            Log.d(TAG, "Checking for new releases")
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for new releases", e)
        }
    }
}

