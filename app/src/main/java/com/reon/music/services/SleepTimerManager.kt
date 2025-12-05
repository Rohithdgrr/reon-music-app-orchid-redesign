/*
 * REON Music App - Sleep Timer
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 */

package com.reon.music.services

import android.os.CountDownTimer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class SleepTimerState(
    val isActive: Boolean = false,
    val remainingTimeMs: Long = 0,
    val totalTimeMs: Long = 0,
    val fadeOutEnabled: Boolean = true
) {
    val remainingMinutes: Int get() = (remainingTimeMs / 60000).toInt()
    val remainingSeconds: Int get() = ((remainingTimeMs % 60000) / 1000).toInt()
    
    fun formattedTime(): String {
        val min = remainingMinutes
        val sec = remainingSeconds
        return "%02d:%02d".format(min, sec)
    }
}

/**
 * Sleep Timer Service
 * Manages sleep timer with fade out option
 */
@Singleton
class SleepTimerManager @Inject constructor() {
    
    private val _state = MutableStateFlow(SleepTimerState())
    val state: StateFlow<SleepTimerState> = _state.asStateFlow()
    
    private var countDownTimer: CountDownTimer? = null
    private var onTimerComplete: (() -> Unit)? = null
    private var onVolumeChange: ((Float) -> Unit)? = null
    
    companion object {
        val PRESET_TIMES = listOf(
            5 * 60 * 1000L,   // 5 minutes
            10 * 60 * 1000L,  // 10 minutes
            15 * 60 * 1000L,  // 15 minutes
            30 * 60 * 1000L,  // 30 minutes
            45 * 60 * 1000L,  // 45 minutes
            60 * 60 * 1000L,  // 1 hour
            90 * 60 * 1000L,  // 1.5 hours
            120 * 60 * 1000L  // 2 hours
        )
        
        private const val FADE_OUT_DURATION = 30 * 1000L // 30 seconds fade
    }
    
    /**
     * Set callbacks for timer events
     */
    fun setCallbacks(
        onComplete: () -> Unit,
        onVolume: (Float) -> Unit
    ) {
        onTimerComplete = onComplete
        onVolumeChange = onVolume
    }
    
    /**
     * Start sleep timer
     */
    fun start(durationMs: Long, fadeOut: Boolean = true) {
        cancel() // Cancel any existing timer
        
        _state.value = SleepTimerState(
            isActive = true,
            remainingTimeMs = durationMs,
            totalTimeMs = durationMs,
            fadeOutEnabled = fadeOut
        )
        
        countDownTimer = object : CountDownTimer(durationMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _state.value = _state.value.copy(remainingTimeMs = millisUntilFinished)
                
                // Handle fade out in the last 30 seconds
                if (fadeOut && millisUntilFinished <= FADE_OUT_DURATION) {
                    val fadeProgress = millisUntilFinished.toFloat() / FADE_OUT_DURATION
                    onVolumeChange?.invoke(fadeProgress)
                }
            }
            
            override fun onFinish() {
                _state.value = SleepTimerState()
                onTimerComplete?.invoke()
            }
        }.start()
    }
    
    /**
     * Add time to the current timer
     */
    fun addTime(additionalMs: Long) {
        val current = _state.value
        if (current.isActive) {
            val newTotal = current.remainingTimeMs + additionalMs
            cancel()
            start(newTotal, current.fadeOutEnabled)
        }
    }
    
    /**
     * Cancel sleep timer
     */
    fun cancel() {
        countDownTimer?.cancel()
        countDownTimer = null
        _state.value = SleepTimerState()
        onVolumeChange?.invoke(1.0f) // Restore volume
    }
    
    /**
     * Check if timer is active
     */
    fun isActive(): Boolean = _state.value.isActive
    
    /**
     * Get remaining time in milliseconds
     */
    fun getRemainingTime(): Long = _state.value.remainingTimeMs
}
