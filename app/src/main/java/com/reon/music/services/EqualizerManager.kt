/*
 * REON Music App - Audio Equalizer
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 */

package com.reon.music.services

import android.media.audiofx.Equalizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Equalizer preset
 */
data class EqualizerPreset(
    val name: String,
    val bands: List<Int> // Band values from -15 to 15 dB (stored as millibels/100)
)

data class EqualizerState(
    val isEnabled: Boolean = false,
    val currentPreset: String = "Normal",
    val bands: List<BandState> = emptyList(),
    val bassBoost: Int = 0, // 0-1000
    val virtualizer: Int = 0, // 0-1000
    val loudnessEnhancer: Int = 0 // 0-1000
)

data class BandState(
    val index: Int,
    val frequency: Int, // in Hz
    val frequencyLabel: String,
    val level: Int, // Current level (-1500 to 1500 millibels)
    val minLevel: Int,
    val maxLevel: Int
)

/**
 * Audio Equalizer Manager
 * Controls EQ settings for audio playback
 */
@Singleton
class EqualizerManager @Inject constructor() {
    
    companion object {
        private const val TAG = "EqualizerManager"
        
        val PRESETS = listOf(
            EqualizerPreset("Normal", listOf(0, 0, 0, 0, 0)),
            EqualizerPreset("Bass Boost", listOf(6, 4, 0, 0, 0)),
            EqualizerPreset("Bass Reducer", listOf(-6, -4, 0, 0, 0)),
            EqualizerPreset("Treble Boost", listOf(0, 0, 0, 4, 6)),
            EqualizerPreset("Treble Reducer", listOf(0, 0, 0, -4, -6)),
            EqualizerPreset("Vocal Boost", listOf(-2, 0, 4, 2, 0)),
            EqualizerPreset("Rock", listOf(5, 3, 0, 3, 5)),
            EqualizerPreset("Pop", listOf(-1, 2, 4, 2, -1)),
            EqualizerPreset("Jazz", listOf(3, 0, 2, 4, 3)),
            EqualizerPreset("Classical", listOf(4, 2, -1, 2, 4)),
            EqualizerPreset("Electronic", listOf(4, 2, 0, 2, 4)),
            EqualizerPreset("Hip Hop", listOf(5, 3, 0, 2, 3)),
            EqualizerPreset("R&B", listOf(3, 5, 2, 0, 2)),
            EqualizerPreset("Acoustic", listOf(3, 1, 0, 2, 4)),
            EqualizerPreset("Loud", listOf(5, 4, 2, 4, 5))
        )
    }
    
    private var equalizer: Equalizer? = null
    private var audioSessionId: Int = 0
    
    private val _state = MutableStateFlow(EqualizerState())
    val state: StateFlow<EqualizerState> = _state.asStateFlow()
    
    /**
     * Initialize equalizer with audio session
     */
    fun initialize(sessionId: Int) {
        try {
            release()
            audioSessionId = sessionId
            equalizer = Equalizer(0, sessionId).apply {
                enabled = _state.value.isEnabled
            }
            updateBandsState()
            Log.d(TAG, "Equalizer initialized for session: $sessionId")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing equalizer", e)
        }
    }
    
    /**
     * Enable/disable equalizer
     */
    fun setEnabled(enabled: Boolean) {
        try {
            equalizer?.enabled = enabled
            _state.value = _state.value.copy(isEnabled = enabled)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting equalizer enabled", e)
        }
    }
    
    /**
     * Set band level
     */
    fun setBandLevel(bandIndex: Int, level: Int) {
        try {
            equalizer?.setBandLevel(bandIndex.toShort(), level.toShort())
            updateBandsState()
        } catch (e: Exception) {
            Log.e(TAG, "Error setting band level", e)
        }
    }
    
    /**
     * Apply preset
     */
    fun applyPreset(preset: EqualizerPreset) {
        try {
            val eq = equalizer ?: return
            val numberOfBands = eq.numberOfBands.toInt()
            
            preset.bands.forEachIndexed { index, level ->
                if (index < numberOfBands) {
                    // Convert dB to millibels (level * 100)
                    val millibels = level * 100
                    eq.setBandLevel(index.toShort(), millibels.toShort())
                }
            }
            
            _state.value = _state.value.copy(currentPreset = preset.name)
            updateBandsState()
        } catch (e: Exception) {
            Log.e(TAG, "Error applying preset", e)
        }
    }
    
    /**
     * Apply preset by name
     */
    fun applyPreset(name: String) {
        PRESETS.find { it.name == name }?.let { applyPreset(it) }
    }
    
    /**
     * Reset to flat response
     */
    fun reset() {
        applyPreset(PRESETS.first())
    }
    
    /**
     * Set bass boost level (0-1000)
     */
    fun setBassBoost(level: Int) {
        _state.value = _state.value.copy(bassBoost = level.coerceIn(0, 1000))
        // Would need android.media.audiofx.BassBoost for actual implementation
    }
    
    /**
     * Set virtualizer level (0-1000)
     */
    fun setVirtualizer(level: Int) {
        _state.value = _state.value.copy(virtualizer = level.coerceIn(0, 1000))
        // Would need android.media.audiofx.Virtualizer for actual implementation
    }
    
    /**
     * Release equalizer resources
     */
    fun release() {
        try {
            equalizer?.release()
            equalizer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing equalizer", e)
        }
    }
    
    private fun updateBandsState() {
        try {
            val eq = equalizer ?: return
            val numberOfBands = eq.numberOfBands.toInt()
            val levelRange = eq.bandLevelRange
            
            val bands = (0 until numberOfBands).map { index ->
                val frequency = eq.getCenterFreq(index.toShort())
                val level = eq.getBandLevel(index.toShort()).toInt()
                
                BandState(
                    index = index,
                    frequency = frequency / 1000, // Convert to Hz
                    frequencyLabel = formatFrequency(frequency),
                    level = level,
                    minLevel = levelRange[0].toInt(),
                    maxLevel = levelRange[1].toInt()
                )
            }
            
            _state.value = _state.value.copy(bands = bands)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating bands state", e)
        }
    }
    
    private fun formatFrequency(freqMilliHz: Int): String {
        val freqHz = freqMilliHz / 1000
        return when {
            freqHz >= 1000 -> "${freqHz / 1000}kHz"
            else -> "${freqHz}Hz"
        }
    }
}
