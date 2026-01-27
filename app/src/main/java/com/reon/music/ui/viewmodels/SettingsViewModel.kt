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
    val autoPlaySimilar: Boolean = true,
    
    // Appearance
    val theme: AppTheme = AppTheme.SYSTEM,
    val pureBlack: Boolean = false,
    val dynamicColors: Boolean = true,
    val themePresetId: String? = null, // NEW: Theme preset selection
    val fontPresetId: String? = null, // NEW: Font preset selection
    val fontSizePreset: com.reon.music.ui.theme.FontSizePreset = com.reon.music.ui.theme.FontSizePreset.MEDIUM, // NEW: Font size
    
    // Downloads
    val downloadQuality: AudioQuality = AudioQuality.HIGH,
    val downloadWifiOnly: Boolean = true,
    val offlineModeEnabled: Boolean = false,
    
    // Smart Offline Cache
    val autoCacheEnabled: Boolean = true,
    val offlineSongCount: Int = 0,
    val cacheWifiOnly: Boolean = true,
    
    // Lyrics
    val showLyricsDefault: Boolean = false,
    
    // Privacy
    val saveHistory: Boolean = true,
    val incognitoMode: Boolean = false,
    
    // Social & Sharing
    val shareActivity: Boolean = false,
    val discordRichPresence: Boolean = false,
    val artistNotifications: Boolean = true,
    
    // AI & Recommendations
    val aiRecommendations: Boolean = true,
    
    // Car Mode
    val carMode: Boolean = false,
    val voiceCommands: Boolean = false,
    
    // Sleep Timer
    val sleepTimerMinutes: Int = 0,
    
    // Sync
    val cloudSyncEnabled: Boolean = true,
    val lastSyncTime: Long = 0,
    val isSyncing: Boolean = false,
    
    // Content
    val preferredSource: MusicSource = MusicSource.BOTH,
    
    // Auto-Update (NEW)
    val autoUpdateEnabled: Boolean = true,
    val autoUpdateFrequency: Int = 60, // minutes
    val autoUpdateWifiOnly: Boolean = true,
    
    // Data Saver (NEW)
    val dataSaverEnabled: Boolean = false,
    val mobileStreamingQuality: String = "medium", // "low", "medium", "high", "auto"
    val wifiStreamingQuality: String = "high",
    val autoQualityEnabled: Boolean = true,
    
    // Interests (NEW)
    val interestedArtists: Set<String> = emptySet(),
    val notInterestedArtists: Set<String> = emptySet(),
    val interestedRegions: Set<String> = emptySet(),
    val notInterestedRegions: Set<String> = emptySet(),
    val interestedLanguages: Set<String> = emptySet(),
    val notInterestedLanguages: Set<String> = emptySet(),
    val interestedCategories: Set<String> = emptySet(),
    val notInterestedCategories: Set<String> = emptySet()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val neonSyncClient: NeonSyncClient,
    private val smartOfflineCache: com.reon.music.services.SmartOfflineCache,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
        observeOfflineCount()
    }
    
    private fun observeOfflineCount() {
        viewModelScope.launch {
            smartOfflineCache.offlineSongCount.collect { count ->
                updateOfflineSongCount(count)
            }
        }
    }
    
    private fun updateOfflineSongCount(count: Int) {
        _uiState.value = _uiState.value.copy(offlineSongCount = count)
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            val playbackFlow = combine(
                userPreferences.audioQuality,
                userPreferences.crossfadeDuration,
                userPreferences.gaplessPlayback,
                userPreferences.normalizeAudio
            ) { quality, crossfade, gapless, normalize ->
                PlaybackSettings(quality, crossfade, gapless, normalize)
            }
            
            val appearanceFlow = combine(
                userPreferences.theme,
                userPreferences.pureBlack,
                userPreferences.dynamicColors
            ) { theme, pureBlack, dynamic ->
                AppearanceSettings(theme, pureBlack, dynamic)
            }
            
            val downloadFlow = combine(
                userPreferences.downloadQuality,
                userPreferences.downloadWifiOnly,
                userPreferences.showLyricsDefault
            ) { quality, wifiOnly, lyrics ->
                DownloadSettings(quality, wifiOnly, lyrics)
            }
            
            val privacyFlow = combine(
                userPreferences.saveHistory,
                userPreferences.incognitoMode,
                userPreferences.cloudSyncEnabled,
                userPreferences.lastSyncTime,
                userPreferences.preferredSource
            ) { saveHistory, incognito, cloudSync, lastSync, source ->
                PrivacySettings(saveHistory, incognito, cloudSync, lastSync, source)
            }
            
            val interestsFlow = combine(
                userPreferences.interestedArtists,
                userPreferences.notInterestedArtists,
                userPreferences.interestedRegions,
                userPreferences.notInterestedRegions
            ) { intArtists, notIntArtists, intRegions, notIntRegions ->
                InterestsA(intArtists, notIntArtists, intRegions, notIntRegions)
            }
            
            val interests2Flow = combine(
                userPreferences.interestedLanguages,
                userPreferences.notInterestedLanguages,
                userPreferences.interestedCategories,
                userPreferences.notInterestedCategories
            ) { intLang, notIntLang, intCat, notIntCat ->
                InterestsB(intLang, notIntLang, intCat, notIntCat)
            }
            
            // Combine all flows step by step for compatibility
            combine(playbackFlow, appearanceFlow) { playback, appearance ->
                Pair(playback, appearance)
            }.combine(downloadFlow) { (playback, appearance), download ->
                Triple(playback, appearance, download)
            }.combine(privacyFlow) { (playback, appearance, download), privacy ->
                Quartet(playback, appearance, download, privacy)
            }.combine(interestsFlow) { (playback, appearance, download, privacy), interestsA ->
                Quintuple(playback, appearance, download, privacy, interestsA)
            }.combine(interests2Flow) { (playback, appearance, download, privacy, interestsA), interestsB ->
                SettingsUiState(
                    audioQuality = playback.audioQuality,
                    crossfadeDuration = playback.crossfadeDuration,
                    gaplessPlayback = playback.gaplessPlayback,
                    normalizeAudio = playback.normalizeAudio,
                    skipSilence = false,
                    playbackSpeed = 1.0f,
                    autoPlaySimilar = true,
                    
                    // Appearance
                    theme = appearance.theme,
                    pureBlack = appearance.pureBlack,
                    dynamicColors = appearance.dynamicColors,
                    themePresetId = null,
                    fontPresetId = null,
                    fontSizePreset = com.reon.music.ui.theme.FontSizePreset.MEDIUM,
                    
                    // Downloads
                    downloadQuality = download.downloadQuality,
                    downloadWifiOnly = download.downloadWifiOnly,
                    showLyricsDefault = download.showLyricsDefault,
                    
                    // Privacy
                    saveHistory = privacy.saveHistory,
                    incognitoMode = privacy.incognitoMode,
                    cloudSyncEnabled = privacy.cloudSyncEnabled,
                    lastSyncTime = privacy.lastSyncTime,
                    preferredSource = privacy.preferredSource,
                    
                    // Interests
                    interestedArtists = interestsA.interestedArtists,
                    notInterestedArtists = interestsA.notInterestedArtists,
                    interestedRegions = interestsA.interestedRegions,
                    notInterestedRegions = interestsA.notInterestedRegions,
                    interestedLanguages = interestsB.interestedLanguages,
                    notInterestedLanguages = interestsB.notInterestedLanguages,
                    interestedCategories = interestsB.interestedCategories,
                    notInterestedCategories = interestsB.notInterestedCategories
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
    
    private data class PlaybackSettings(
        val audioQuality: AudioQuality,
        val crossfadeDuration: Int,
        val gaplessPlayback: Boolean,
        val normalizeAudio: Boolean
    )
    
    private data class AppearanceSettings(
        val theme: AppTheme,
        val pureBlack: Boolean,
        val dynamicColors: Boolean
    )
    
    private data class DownloadSettings(
        val downloadQuality: AudioQuality,
        val downloadWifiOnly: Boolean,
        val showLyricsDefault: Boolean
    )
    
    private data class PrivacySettings(
        val saveHistory: Boolean,
        val incognitoMode: Boolean,
        val cloudSyncEnabled: Boolean,
        val lastSyncTime: Long,
        val preferredSource: MusicSource
    )
    
    private data class InterestsA(
        val interestedArtists: Set<String>,
        val notInterestedArtists: Set<String>,
        val interestedRegions: Set<String>,
        val notInterestedRegions: Set<String>
    )
    
    private data class InterestsB(
        val interestedLanguages: Set<String>,
        val notInterestedLanguages: Set<String>,
        val interestedCategories: Set<String>,
        val notInterestedCategories: Set<String>
    )
    
    // Helper data classes for combining flows
    private data class Pair<A, B>(val first: A, val second: B)
    private data class Triple<A, B, C>(val first: A, val second: B, val third: C)
    private data class Quartet<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
    private data class Quintuple<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)
    
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
    
    // NEW: Theme preset selection
    fun setThemePreset(presetId: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(themePresetId = presetId)
            userPreferences.setThemePreset(presetId)
        }
    }
    
    // NEW: Font preset selection
    fun setFontPreset(presetId: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(fontPresetId = presetId)
            userPreferences.setFontPreset(presetId)
        }
    }
    
    // NEW: Font size selection
    fun setFontSize(sizePreset: com.reon.music.ui.theme.FontSizePreset) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(fontSizePreset = sizePreset)
            userPreferences.setFontSizePreset(sizePreset.name)
        }
    }
    
    // NEW: Auto-update settings
    fun setAutoUpdateEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(autoUpdateEnabled = enabled)
            userPreferences.setAutoUpdateEnabled(enabled)
            
            if (enabled) {
                // Schedule periodic sync
                com.reon.music.workers.ContentSyncScheduler.scheduleSync(
                    context = context,
                    frequencyMinutes = _uiState.value.autoUpdateFrequency,
                    wifiOnly = _uiState.value.autoUpdateWifiOnly
                )
            } else {
                // Cancel scheduled sync
                com.reon.music.workers.ContentSyncScheduler.cancelSync(context)
            }
        }
    }
    
    fun setAutoUpdateFrequency(minutes: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(autoUpdateFrequency = minutes)
            userPreferences.setAutoUpdateFrequency(minutes)
            
            // Reschedule if auto-update is enabled
            if (_uiState.value.autoUpdateEnabled) {
                com.reon.music.workers.ContentSyncScheduler.scheduleSync(
                    context = context,
                    frequencyMinutes = minutes,
                    wifiOnly = _uiState.value.autoUpdateWifiOnly
                )
            }
        }
    }
    
    fun setAutoUpdateWifiOnly(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(autoUpdateWifiOnly = enabled)
            userPreferences.setAutoUpdateWifiOnly(enabled)
            
            // Reschedule if auto-update is enabled
            if (_uiState.value.autoUpdateEnabled) {
                com.reon.music.workers.ContentSyncScheduler.scheduleSync(
                    context = context,
                    frequencyMinutes = _uiState.value.autoUpdateFrequency,
                    wifiOnly = enabled
                )
            }
        }
    }
    
    // Download settings
    fun setDownloadQuality(quality: AudioQuality) {
        viewModelScope.launch { userPreferences.setDownloadQuality(quality) }
    }
    
    fun setDownloadWifiOnly(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setDownloadWifiOnly(enabled) }
    }
    
    fun setOfflineModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(offlineModeEnabled = enabled)
            userPreferences.setOfflineModeEnabled(enabled)
        }
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
                // Trigger immediate sync via WorkManager
                val workInfo = com.reon.music.workers.ContentSyncScheduler.syncNow(
                    context = context,
                    wifiOnly = false // Allow sync on any connection for manual sync
                )
                
                // Observe work status
                workInfo.observeForever { info ->
                    when (info?.state) {
                        androidx.work.WorkInfo.State.SUCCEEDED -> {
                            // Update last sync time
                            viewModelScope.launch {
                                val syncTime = info.outputData.getLong(
                                    com.reon.music.workers.ContentSyncWorker.KEY_SYNC_TIME,
                                    System.currentTimeMillis()
                                )
                                userPreferences.setLastSyncTime(syncTime)
                                _uiState.value = _uiState.value.copy(
                                    isSyncing = false,
                                    lastSyncTime = syncTime
                                )
                            }
                        }
                        androidx.work.WorkInfo.State.FAILED,
                        androidx.work.WorkInfo.State.CANCELLED -> {
                            _uiState.value = _uiState.value.copy(isSyncing = false)
                        }
                        else -> {
                            // Still running or enqueued
                        }
                    }
                }
            } catch (e: Exception) {
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
    
    // Smart Offline Cache
    fun setAutoCacheEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(autoCacheEnabled = enabled)
        }
    }
    
    fun setCacheWifiOnly(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cacheWifiOnly = enabled)
        }
    }
    
    // Advanced Playback
    fun setAutoPlaySimilar(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(autoPlaySimilar = enabled)
        }
    }
    
    fun setSkipSilence(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(skipSilence = enabled)
        }
    }
    
    // Social & Sharing
    fun setShareActivity(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(shareActivity = enabled)
        }
    }
    
    fun setDiscordRichPresence(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(discordRichPresence = enabled)
        }
    }
    
    fun setArtistNotifications(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(artistNotifications = enabled)
        }
    }
    
    // AI & Recommendations
    fun setAIRecommendations(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(aiRecommendations = enabled)
        }
    }
    
    // Car Mode
    fun setCarMode(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(carMode = enabled)
        }
    }
    
    fun setVoiceCommands(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(voiceCommands = enabled)
        }
    }
    
    // Data Saver
    fun setDataSaverEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(dataSaverEnabled = enabled)
        }
    }
    
    fun setMobileStreamingQuality(quality: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(mobileStreamingQuality = quality)
        }
    }
    
    fun setWifiStreamingQuality(quality: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(wifiStreamingQuality = quality)
        }
    }
    
    fun setAutoQualityEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(autoQualityEnabled = enabled)
        }
    }
}
