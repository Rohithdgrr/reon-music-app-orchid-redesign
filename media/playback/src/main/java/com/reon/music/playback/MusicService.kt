/*
 * REON Music App - Music Service
 * Copyright (c) 2024 REON
 * 
 * CLEAN-ROOM IMPLEMENTATION
 * This service is independently written using official Media3 APIs.
 * No GPL-licensed code has been copied.
 */

package com.reon.music.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import javax.inject.Inject

/**
 * Music Playback Service
 * Clean-room implementation using Media3 APIs
 */
@AndroidEntryPoint
class MusicService : MediaLibraryService() {
    
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaLibrarySession
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        
        // Initialize ExoPlayer with audio focus handling
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true // handleAudioFocus
            )
            .setHandleAudioBecomingNoisy(true) // Pause when headphones disconnected
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        
        // Create pending intent for notification click
        val pendingIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let { intent ->
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        
        // Create MediaLibrarySession
        mediaSession = MediaLibrarySession.Builder(this, player, MediaLibraryCallback())
            .setSessionActivity(pendingIntent!!)
            .build()
    }
    
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession {
        return mediaSession
    }
    
    override fun onDestroy() {
        mediaSession.release()
        player.release()
        serviceScope.cancel()
        super.onDestroy()
    }
    
    /**
     * MediaLibrarySession callback
     */
    private inner class MediaLibraryCallback : MediaLibrarySession.Callback {
        // Default implementations are sufficient for basic playback
    }
    
    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "reon_playback_channel"
    }
}
