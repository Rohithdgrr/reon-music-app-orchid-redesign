/*
 * REON Music App - Video Player Manager
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 */

package com.reon.music.services

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.os.Build
import android.util.Log
import android.util.Rational
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class VideoPlayerState(
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val currentPosition: Long = 0,
    val duration: Long = 0,
    val currentQuality: VideoQuality = VideoQuality.AUTO,
    val availableQualities: List<VideoQuality> = emptyList(),
    val showSubtitles: Boolean = false,
    val subtitleTracks: List<SubtitleTrack> = emptyList(),
    val currentSubtitleTrack: SubtitleTrack? = null,
    val isInPiPMode: Boolean = false,
    val error: String? = null
)

enum class VideoQuality(val label: String, val height: Int) {
    AUTO("Auto", 0),
    P360("360p", 360),
    P480("480p", 480),
    P720("720p", 720),
    P1080("1080p", 1080)
}

data class SubtitleTrack(
    val id: String,
    val language: String,
    val label: String,
    val url: String?
)

/**
 * Video Player Manager
 * Handles video playback with quality selection, subtitles, and PiP
 */
@Singleton
class VideoPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "VideoPlayerManager"
    }
    
    private var player: ExoPlayer? = null
    
    private val _state = MutableStateFlow(VideoPlayerState())
    val state: StateFlow<VideoPlayerState> = _state.asStateFlow()
    
    private var currentVideoId: String? = null
    private var currentVideoUrl: String? = null
    
    /**
     * Initialize player
     */
    @OptIn(UnstableApi::class)
    fun initializePlayer(): ExoPlayer {
        if (player == null) {
            player = ExoPlayer.Builder(context)
                .setHandleAudioBecomingNoisy(true)
                .build().apply {
                    addListener(playerListener)
                    videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                }
        }
        return player!!
    }
    
    /**
     * Play video
     */
    @OptIn(UnstableApi::class)
    fun playVideo(videoId: String, videoUrl: String, subtitles: List<SubtitleTrack> = emptyList()) {
        currentVideoId = videoId
        currentVideoUrl = videoUrl
        
        _state.value = _state.value.copy(
            isLoading = true,
            subtitleTracks = subtitles,
            error = null
        )
        
        try {
            val exoPlayer = initializePlayer()
            
            val mediaItemBuilder = MediaItem.Builder()
                .setUri(videoUrl)
            
            // Add subtitle tracks
            if (subtitles.isNotEmpty()) {
                val subtitleConfigs = subtitles.mapNotNull { track ->
                    track.url?.let { url ->
                        MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(url))
                            .setMimeType(MimeTypes.TEXT_VTT)
                            .setLanguage(track.language)
                            .setLabel(track.label)
                            .build()
                    }
                }
                mediaItemBuilder.setSubtitleConfigurations(subtitleConfigs)
            }
            
            exoPlayer.setMediaItem(mediaItemBuilder.build())
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
            
            Log.d(TAG, "Playing video: $videoId")
        } catch (e: Exception) {
            Log.e(TAG, "Error playing video", e)
            _state.value = _state.value.copy(
                isLoading = false,
                error = "Failed to play video: ${e.message}"
            )
        }
    }
    
    /**
     * Set video quality
     */
    @OptIn(UnstableApi::class)
    fun setQuality(quality: VideoQuality) {
        _state.value = _state.value.copy(currentQuality = quality)
        
        // In production, would switch to different quality stream
        // This requires having multiple quality URLs from YouTube
    }
    
    /**
     * Toggle subtitles
     */
    fun toggleSubtitles() {
        val current = _state.value
        _state.value = current.copy(showSubtitles = !current.showSubtitles)
        
        // Enable/disable subtitle track in player
        player?.let { exo ->
            val trackSelector = exo.trackSelector
            // Would configure track selection here
        }
    }
    
    /**
     * Select subtitle track
     */
    fun selectSubtitleTrack(track: SubtitleTrack?) {
        _state.value = _state.value.copy(
            currentSubtitleTrack = track,
            showSubtitles = track != null
        )
    }
    
    /**
     * Enter Picture-in-Picture mode
     */
    fun enterPiPMode(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val aspectRatio = Rational(16, 9)
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio)
                    .build()
                
                activity.enterPictureInPictureMode(params)
                _state.value = _state.value.copy(isInPiPMode = true)
                return true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to enter PiP mode", e)
            }
        }
        return false
    }
    
    /**
     * Exit Picture-in-Picture mode
     */
    fun exitPiPMode() {
        _state.value = _state.value.copy(isInPiPMode = false)
    }
    
    /**
     * Play/Pause toggle
     */
    fun togglePlayPause() {
        player?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.play()
            }
        }
    }
    
    /**
     * Seek to position
     */
    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs)
    }
    
    /**
     * Get current position
     */
    fun getCurrentPosition(): Long = player?.currentPosition ?: 0
    
    /**
     * Get duration
     */
    fun getDuration(): Long = player?.duration ?: 0
    
    /**
     * Switch to audio-only (background)
     */
    @OptIn(UnstableApi::class)
    fun switchToAudioOnly() {
        player?.trackSelectionParameters = player?.trackSelectionParameters?.buildUpon()
            ?.setMaxVideoSize(0, 0)
            ?.build() ?: return
    }
    
    /**
     * Switch back to video
     */
    @OptIn(UnstableApi::class)
    fun switchToVideo() {
        player?.trackSelectionParameters = player?.trackSelectionParameters?.buildUpon()
            ?.clearVideoSizeConstraints()
            ?.build() ?: return
    }
    
    /**
     * Release player
     */
    fun release() {
        player?.apply {
            removeListener(playerListener)
            release()
        }
        player = null
        _state.value = VideoPlayerState()
    }
    
    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            _state.value = _state.value.copy(
                isLoading = playbackState == Player.STATE_BUFFERING
            )
            
            if (playbackState == Player.STATE_READY) {
                _state.value = _state.value.copy(
                    duration = player?.duration ?: 0
                )
            }
        }
        
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.value = _state.value.copy(isPlaying = isPlaying)
        }
    }
}
