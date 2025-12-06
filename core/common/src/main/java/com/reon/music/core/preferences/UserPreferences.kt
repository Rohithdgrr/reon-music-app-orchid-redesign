/*
 * REON Music App - User Preferences
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 */

package com.reon.music.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "reon_settings")

/**
 * User Preferences Manager
 * Uses DataStore for reactive settings
 */
@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore
    
    companion object {
        // Playback
        private val AUDIO_QUALITY = stringPreferencesKey("audio_quality")
        private val CROSSFADE_DURATION = intPreferencesKey("crossfade_duration")
        private val GAPLESS_PLAYBACK = booleanPreferencesKey("gapless_playback")
        private val NORMALIZE_AUDIO = booleanPreferencesKey("normalize_audio")
        private val SKIP_SILENCE = booleanPreferencesKey("skip_silence")
        private val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        
        // Appearance
        private val THEME = stringPreferencesKey("theme")
        private val PURE_BLACK = booleanPreferencesKey("pure_black")
        private val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
        private val LANGUAGE = stringPreferencesKey("language")
        
        // Downloads
        private val DOWNLOAD_QUALITY = stringPreferencesKey("download_quality")
        private val DOWNLOAD_WIFI_ONLY = booleanPreferencesKey("download_wifi_only")
        private val AUTO_DOWNLOAD_LIKED = booleanPreferencesKey("auto_download_liked")
        
        // Lyrics
        private val SHOW_LYRICS_DEFAULT = booleanPreferencesKey("show_lyrics_default")
        private val ENABLE_AI_TRANSLATION = booleanPreferencesKey("enable_ai_translation")
        private val TRANSLATION_LANGUAGE = stringPreferencesKey("translation_language")
        
        // Privacy
        private val SAVE_HISTORY = booleanPreferencesKey("save_history")
        private val INCOGNITO_MODE = booleanPreferencesKey("incognito_mode")
        
        // Integrations
        private val SPONSOR_BLOCK_ENABLED = booleanPreferencesKey("sponsor_block_enabled")
        private val DISCORD_RPC_ENABLED = booleanPreferencesKey("discord_rpc_enabled")
        
        // Content settings
        private val PREFERRED_SOURCE = stringPreferencesKey("preferred_source")
        
        // Sync
        private val DEVICE_ID = stringPreferencesKey("device_id")
        private val CLOUD_SYNC_ENABLED = booleanPreferencesKey("cloud_sync_enabled")
        private val LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
    }
    
    // Playback settings
    val audioQuality: Flow<AudioQuality> = dataStore.data.map { prefs ->
        AudioQuality.fromString(prefs[AUDIO_QUALITY] ?: "high")
    }
    
    val crossfadeDuration: Flow<Int> = dataStore.data.map { prefs ->
        prefs[CROSSFADE_DURATION] ?: 0
    }
    
    val gaplessPlayback: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[GAPLESS_PLAYBACK] ?: true
    }
    
    val normalizeAudio: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[NORMALIZE_AUDIO] ?: false
    }
    
    val skipSilence: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[SKIP_SILENCE] ?: false
    }
    
    val playbackSpeed: Flow<Float> = dataStore.data.map { prefs ->
        prefs[PLAYBACK_SPEED] ?: 1.0f
    }
    
    // Appearance settings
    val theme: Flow<AppTheme> = dataStore.data.map { prefs ->
        AppTheme.fromString(prefs[THEME] ?: "system")
    }
    
    val pureBlack: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PURE_BLACK] ?: false
    }
    
    val dynamicColors: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[DYNAMIC_COLORS] ?: true
    }
    
    val language: Flow<String> = dataStore.data.map { prefs ->
        prefs[LANGUAGE] ?: "en"
    }
    
    // Download settings
    val downloadQuality: Flow<AudioQuality> = dataStore.data.map { prefs ->
        AudioQuality.fromString(prefs[DOWNLOAD_QUALITY] ?: "high")
    }
    
    val downloadWifiOnly: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[DOWNLOAD_WIFI_ONLY] ?: true
    }
    
    val autoDownloadLiked: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[AUTO_DOWNLOAD_LIKED] ?: false
    }
    
    // Lyrics settings
    val showLyricsDefault: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[SHOW_LYRICS_DEFAULT] ?: false
    }
    
    val enableAiTranslation: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[ENABLE_AI_TRANSLATION] ?: false
    }
    
    val translationLanguage: Flow<String> = dataStore.data.map { prefs ->
        prefs[TRANSLATION_LANGUAGE] ?: "en"
    }
    
    // Privacy settings
    val saveHistory: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[SAVE_HISTORY] ?: true
    }
    
    val incognitoMode: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[INCOGNITO_MODE] ?: false
    }
    
    // Integration settings
    val sponsorBlockEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[SPONSOR_BLOCK_ENABLED] ?: true
    }
    
    val discordRpcEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[DISCORD_RPC_ENABLED] ?: false
    }
    
    // Sync settings
    val deviceId: Flow<String> = dataStore.data.map { prefs ->
        prefs[DEVICE_ID] ?: java.util.UUID.randomUUID().toString()
    }
    
    val cloudSyncEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[CLOUD_SYNC_ENABLED] ?: true
    }
    
    val lastSyncTime: Flow<Long> = dataStore.data.map { prefs ->
        prefs[LAST_SYNC_TIME] ?: 0L
    }
    
    // Content settings
    val preferredSource: Flow<MusicSource> = dataStore.data.map { prefs ->
        MusicSource.fromString(prefs[PREFERRED_SOURCE] ?: "jiosaavn")
    }
    
    suspend fun setPreferredSource(source: MusicSource) {
        dataStore.edit { it[PREFERRED_SOURCE] = source.name.lowercase() }
    }
    
    // Setters
    suspend fun setAudioQuality(quality: AudioQuality) {
        dataStore.edit { it[AUDIO_QUALITY] = quality.name.lowercase() }
    }
    
    suspend fun setCrossfadeDuration(seconds: Int) {
        dataStore.edit { it[CROSSFADE_DURATION] = seconds.coerceIn(0, 12) }
    }
    
    suspend fun setGaplessPlayback(enabled: Boolean) {
        dataStore.edit { it[GAPLESS_PLAYBACK] = enabled }
    }
    
    suspend fun setNormalizeAudio(enabled: Boolean) {
        dataStore.edit { it[NORMALIZE_AUDIO] = enabled }
    }
    
    suspend fun setTheme(theme: AppTheme) {
        dataStore.edit { it[THEME] = theme.name.lowercase() }
    }
    
    suspend fun setPureBlack(enabled: Boolean) {
        dataStore.edit { it[PURE_BLACK] = enabled }
    }
    
    suspend fun setDynamicColors(enabled: Boolean) {
        dataStore.edit { it[DYNAMIC_COLORS] = enabled }
    }
    
    suspend fun setDownloadQuality(quality: AudioQuality) {
        dataStore.edit { it[DOWNLOAD_QUALITY] = quality.name.lowercase() }
    }
    
    suspend fun setDownloadWifiOnly(enabled: Boolean) {
        dataStore.edit { it[DOWNLOAD_WIFI_ONLY] = enabled }
    }
    
    suspend fun setSaveHistory(enabled: Boolean) {
        dataStore.edit { it[SAVE_HISTORY] = enabled }
    }
    
    suspend fun setIncognitoMode(enabled: Boolean) {
        dataStore.edit { it[INCOGNITO_MODE] = enabled }
    }
    
    suspend fun setSponsorBlockEnabled(enabled: Boolean) {
        dataStore.edit { it[SPONSOR_BLOCK_ENABLED] = enabled }
    }
    
    suspend fun setDiscordRpcEnabled(enabled: Boolean) {
        dataStore.edit { it[DISCORD_RPC_ENABLED] = enabled }
    }
    
    suspend fun setDeviceId(id: String) {
        dataStore.edit { it[DEVICE_ID] = id }
    }
    
    suspend fun setCloudSyncEnabled(enabled: Boolean) {
        dataStore.edit { it[CLOUD_SYNC_ENABLED] = enabled }
    }
    
    suspend fun setLastSyncTime(timestamp: Long) {
        dataStore.edit { it[LAST_SYNC_TIME] = timestamp }
    }
}

enum class AudioQuality(val bitrate: Int, val label: String) {
    LOW(96, "Low (96 kbps)"),
    MEDIUM(160, "Medium (160 kbps)"),
    HIGH(256, "High (256 kbps)"),
    VERY_HIGH(320, "Very High (320 kbps)");
    
    companion object {
        fun fromString(value: String): AudioQuality {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: HIGH
        }
    }
}

enum class AppTheme(val label: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System Default"),
    AMOLED("AMOLED Black");
    
    companion object {
        fun fromString(value: String): AppTheme {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: SYSTEM
        }
    }
}

enum class MusicSource(val label: String) {
    JIOSAAVN("JioSaavn"),
    YOUTUBE("YouTube"),
    BOTH("Both");
    
    companion object {
        fun fromString(value: String): MusicSource {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: BOTH
        }
    }
}
