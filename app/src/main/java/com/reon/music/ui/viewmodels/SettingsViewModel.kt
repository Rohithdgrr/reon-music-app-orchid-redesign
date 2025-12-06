/*
 * REON Music App - Settings ViewModel
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 */

package com.reon.music.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reon.music.core.preferences.AppTheme
import com.reon.music.core.preferences.AudioQuality
import com.reon.music.core.preferences.MusicSource
import com.reon.music.core.preferences.UserPreferences
import com.reon.music.data.database.sync.NeonSyncClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    // Playback
    val audioQuality: AudioQuality = AudioQuality.HIGH,
    val crossfadeDuration: Int = 0,
    val gaplessPlayback: Boolean = true,
    val normalizeAudio: Boolean = false,
    val skipSilence: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    
    // Appearance
    val theme: AppTheme = AppTheme.SYSTEM,
    val pureBlack: Boolean = false,
    val dynamicColors: Boolean = true,
    
    // Downloads
    val downloadQuality: AudioQuality = AudioQuality.HIGH,
    val downloadWifiOnly: Boolean = true,
    
    // Lyrics
    val showLyricsDefault: Boolean = false,
    
    // Privacy
    val saveHistory: Boolean = true,
    val incognitoMode: Boolean = false,
    
    // Sync
    // Sync
    val cloudSyncEnabled: Boolean = true,
    val lastSyncTime: Long = 0,
    val isSyncing: Boolean = false,
    
    // Content
    val preferredSource: MusicSource = MusicSource.BOTH
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val neonSyncClient: NeonSyncClient
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            combine(
                userPreferences.audioQuality,
                userPreferences.crossfadeDuration,
                userPreferences.gaplessPlayback,
                userPreferences.normalizeAudio,
                userPreferences.theme,
                userPreferences.pureBlack,
                userPreferences.dynamicColors,
                userPreferences.downloadQuality,
                userPreferences.downloadWifiOnly,
                userPreferences.showLyricsDefault,
                userPreferences.saveHistory,
                userPreferences.incognitoMode,
                userPreferences.cloudSyncEnabled,
                userPreferences.lastSyncTime,
                userPreferences.preferredSource
            ) { values ->
                @Suppress("UNCHECKED_CAST")
                SettingsUiState(
                    audioQuality = values[0] as AudioQuality,
                    crossfadeDuration = values[1] as Int,
                    gaplessPlayback = values[2] as Boolean,
                    normalizeAudio = values[3] as Boolean,
                    theme = values[4] as AppTheme,
                    pureBlack = values[5] as Boolean,
                    dynamicColors = values[6] as Boolean,
                    downloadQuality = values[7] as AudioQuality,
                    downloadWifiOnly = values[8] as Boolean,
                    showLyricsDefault = values[9] as Boolean,
                    saveHistory = values[10] as Boolean,
                    incognitoMode = values[11] as Boolean,
                    cloudSyncEnabled = values[12] as Boolean,
                    lastSyncTime = values[13] as Long,
                    preferredSource = values[14] as MusicSource
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
    
    // Playback settings
    fun setAudioQuality(quality: AudioQuality) {
        viewModelScope.launch { userPreferences.setAudioQuality(quality) }
    }
    
    fun setCrossfade(seconds: Int) {
        viewModelScope.launch { userPreferences.setCrossfadeDuration(seconds) }
    }
    
    fun setGaplessPlayback(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setGaplessPlayback(enabled) }
    }
    
    fun setNormalizeAudio(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setNormalizeAudio(enabled) }
    }
    
    // Appearance settings
    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { userPreferences.setTheme(theme) }
    }
    
    fun setPureBlack(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setPureBlack(enabled) }
    }
    
    fun setDynamicColors(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setDynamicColors(enabled) }
    }
    
    // Download settings
    fun setDownloadQuality(quality: AudioQuality) {
        viewModelScope.launch { userPreferences.setDownloadQuality(quality) }
    }
    
    fun setDownloadWifiOnly(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setDownloadWifiOnly(enabled) }
    }
    
    // Content settings
    fun setPreferredSource(source: com.reon.music.core.preferences.MusicSource) {
        viewModelScope.launch { userPreferences.setPreferredSource(source) }
    }
    
    // Lyrics settings
    fun setShowLyricsDefault(enabled: Boolean) {
        viewModelScope.launch { 
            // Save to preferences (would need to add this method)
        }
    }
    
    // Privacy settings
    fun setSaveHistory(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setSaveHistory(enabled) }
    }
    
    fun setIncognitoMode(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setIncognitoMode(enabled) }
    }
    
    // Cloud sync
    fun setCloudSyncEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setCloudSyncEnabled(enabled) }
    }
    
    fun syncNow() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true)
            try {
                // Initialize tables if needed
                neonSyncClient.initializeTables()
                
                // Sync data here...
                
                userPreferences.setLastSyncTime(System.currentTimeMillis())
            } catch (e: Exception) {
                // Handle error
            } finally {
                _uiState.value = _uiState.value.copy(isSyncing = false)
            }
        }
    }
    
    fun clearCache() {
        viewModelScope.launch {
            // Clear various caches
            // - Image cache
            // - Stream cache
            // - Old lyrics
        }
    }
}
