/*
 * REON Music App - Settings Screen
 * Copyright (c) 2024 REON
 * Created by Rohith
 */

package com.reon.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.reon.music.core.preferences.AudioQuality
import com.reon.music.ui.viewmodels.SettingsViewModel

// Light theme colors
private val LightBackground = Color(0xFFFAFAFA)
private val SurfaceColor = Color.White
private val CardColor = Color(0xFFF0F0F0)
private val AccentRed = Color(0xFFE53935)
private val TextPrimary = Color(0xFF1A1A1A)
private val TextSecondary = Color(0xFF666666)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit = {},
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by settingsViewModel.uiState.collectAsState()
    
    var showAboutDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showCacheDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showThemePresetDialog by remember { mutableStateOf(false) }
    var showFontDialog by remember { mutableStateOf(false) }
    var showFontSizeDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LightBackground
                )
            )
        },
        containerColor = LightBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Audio & Playback Section
            item {
                SettingsSection(title = "Audio & Playback") {
                    SettingsCard {
                        SettingsItem(
                            icon = Icons.Outlined.HighQuality,
                            title = "Audio Quality",
                            subtitle = when (uiState.audioQuality.name) {
                                "LOW" -> "Low (96 kbps) - Save data"
                                "MEDIUM" -> "Medium (160 kbps)"
                                "HIGH" -> "High (320 kbps) - Best quality"
                                else -> "High (320 kbps)"
                            },
                            onClick = { showQualityDialog = true }
                        )
                        
                        HorizontalDivider(color = CardColor)
                        
                        SettingsSwitchItem(
                            icon = Icons.Outlined.GraphicEq,
                            title = "Equalizer",
                            subtitle = "Customize sound with equalizer",
                            checked = uiState.normalizeAudio,
                            onCheckedChange = { settingsViewModel.setNormalizeAudio(it) }
                        )
                        
                        HorizontalDivider(color = CardColor)
                        
                        SettingsSwitchItem(
                            icon = Icons.Outlined.SkipNext,
                            title = "Gapless Playback",
                            subtitle = "Seamless transition between tracks",
                            checked = uiState.gaplessPlayback,
                            onCheckedChange = { settingsViewModel.setGaplessPlayback(it) }
                        )
                        
                        HorizontalDivider(color = CardColor)
                        
                        SettingsSwitchItem(
                            icon = Icons.Outlined.Headphones,
                            title = "Crossfade",
                            subtitle = "Smooth fade between songs",
                            checked = uiState.crossfadeDuration > 0,
                            onCheckedChange = { settingsViewModel.setCrossfade(if (it) 3 else 0) }
                        )
                    }
                }
            }
            
            // Downloads Section
            item {
                SettingsSection(title = "Downloads") {
                    SettingsCard {
                        SettingsItem(
                            icon = Icons.Outlined.Download,
                            title = "Download Quality",
                            subtitle = "320 kbps - Best available",
                            onClick = { showQualityDialog = true }
                        )
                        
                        HorizontalDivider(color = CardColor)
                        
                        SettingsSwitchItem(
                            icon = Icons.Outlined.Wifi,
                            title = "Download on Wi-Fi Only",
                            subtitle = "Save mobile data",
                            checked = uiState.downloadWifiOnly,
                            onCheckedChange = { settingsViewModel.setDownloadWifiOnly(it) }
                        )
                        
                        HorizontalDivider(color = CardColor)
                        
                        SettingsItem(
                            icon = Icons.Outlined.Storage,
                            title = "Storage Used",
                            subtitle = "0 MB used",
                            onClick = { showCacheDialog = true }
                        )
                    }
                }
            }
            
            // Theme & Appearance Section
            item {
                SettingsSection(title = "Theme & Appearance") {
                    SettingsCard {
                        SettingsItem(
                            icon = Icons.Outlined.Palette,
                            title = "Theme",
                            subtitle = when (uiState.theme) {
                                com.reon.music.core.preferences.AppTheme.LIGHT -> "Light"
                                com.reon.music.core.preferences.AppTheme.DARK -> "Dark"
                                com.reon.music.core.preferences.AppTheme.SYSTEM -> "System Default"
                                com.reon.music.core.preferences.AppTheme.AMOLED -> "AMOLED Black"
                            },
                            onClick = { showThemeDialog = true }
                        )
                        
                        HorizontalDivider(color = CardColor)
                        
                        // NEW: Theme Preset Selector
                        SettingsItem(
                            icon = Icons.Outlined.ColorLens,
                            title = "Theme Preset",
                            subtitle = uiState.themePresetId?.let { 
                                com.reon.music.ui.theme.ThemePresets.getPresetById(it)?.let { preset ->
                                    "${preset.emoji} ${preset.name}"
                                }
                            } ?: "System Default",
                            onClick = { showThemePresetDialog = true }
                        )
                        
                        HorizontalDivider(color = CardColor)
                        
                        SettingsSwitchItem(
                            icon = Icons.Outlined.Palette,
                            title = "Dynamic Colors",
                            subtitle = "Colors adapt to album art",
                            checked = uiState.dynamicColors,
                            onCheckedChange = { settingsViewModel.setDynamicColors(it) }
                        )
                        
                        HorizontalDivider(color = CardColor)
                        
                        // NEW: Font Family Selector
                        SettingsItem(
                            icon = Icons.Outlined.FontDownload,
                            title = "Font Family",
                            subtitle = uiState.fontPresetId?.let { 
                                com.reon.music.ui.theme.FontPresets.getPresetById(it)?.name
                            } ?: "System Default",
                            onClick = { showFontDialog = true }
                        )
                        
                        HorizontalDivider(color = CardColor)
                        
                        // NEW: Font Size Selector
                        SettingsItem(
                            icon = Icons.Outlined.FormatSize,
                            title = "Font Size",
                            subtitle = uiState.fontSizePreset.displayName,
                            onClick = { showFontSizeDialog = true }
                        )
                        
                        HorizontalDivider(color = CardColor)
                        
                        SettingsSwitchItem(
                            icon = Icons.Outlined.Lyrics,
                            title = "Show Lyrics",
                            subtitle = "Display lyrics when available",
                            checked = uiState.showLyricsDefault,
                            onCheckedChange = { settingsViewModel.setShowLyricsDefault(it) }
                        )
                    }
                }
            }
            
            // Smart Offline Cache Section (NEW)
            item {
                SettingsSection(title = "Smart Offline Cache") {
                    SettingsCard {
                        SettingsSwitchItem(
                            icon = Icons.Outlined.CloudDownload,
                            title = "Auto-Cache Completed Songs",
                            subtitle = "Automatically cache songs you've listened to",
                            checked = uiState.autoCacheEnabled,
                            onCheckedChange = { settingsViewModel.setAutoCacheEnabled(it) }
                        )
                        
                        HorizontalDivider(color = CardColor)
                        
                        SettingsItem(
                            icon = Icons.Outlined.Storage,
                            title = "Offline Songs",
                            subtitle = "${uiState.offlineSongCount} songs available offline",
                            onClick = { /* Show offline songs */ }
                        )
                        
                        HorizontalDivider(color = CardColor)
                        
                        SettingsSwitchItem(
                            icon = Icons.Outlined.Wifi,
                            title = "Cache on Wi-Fi Only",
                            subtitle = "Save mobile data",
                            checked = uiState.cacheWifiOnly,
                            onCheckedChange = { settingsViewModel.setCacheWifiOnly(it) }
                        )
                    }
                }
            }
            
            // NEW: Auto-Update Section
            item {
                SettingsSection(title = "Auto-Update") {
                    SettingsCard {
                        SettingsSwitchItem(
                            icon = Icons.Outlined.Sync,
                            title = "Enable Auto-Update",
                            subtitle = "Automatically update charts and playlists",
                            checked = uiState.autoUpdateEnabled,
                            onCheckedChange = { settingsViewModel.setAutoUpdateEnabled(it) }
                        )
                        
                        if (uiState.autoUpdateEnabled) {
                            HorizontalDivider(color = CardColor)
                            
                            SettingsItem(
                                icon = Icons.Outlined.Schedule,
                                title = "Update Frequency",
                                subtitle = when (uiState.autoUpdateFrequency) {
                                    15 -> "Every 15 minutes"
                                    30 -> "Every 30 minutes"
                                    60 -> "Every hour"
                                    120 -> "Every 2 hours"
                                    360 -> "Every 6 hours"
                                    720 -> "Every 12 hours"
                                    1440 -> "Once daily"
                                    else -> "${uiState.autoUpdateFrequency} minutes"
                                },
                                onClick = { /* Show frequency picker */ }
                            )
                            
                            HorizontalDivider(color = CardColor)
                            
                            SettingsSwitchItem(
                                icon = Icons.Outlined.Wifi,
                                title = "WiFi Only",
                                subtitle = "Update only on WiFi connection",
                                checked = uiState.autoUpdateWifiOnly,
                                onCheckedChange = { settingsViewModel.setAutoUpdateWifiOnly(it) }
                            )
                            
                            HorizontalDivider(color = CardColor)
                            
                            SettingsItem(
                                icon = Icons.Outlined.CloudSync,
                                title = "Sync Now",
                                subtitle = if (uiState.isSyncing) "Syncing..." 
                                          else if (uiState.lastSyncTime > 0) 
                                              "Last synced: ${formatLastSync(uiState.lastSyncTime)}"
                                          else "Never synced",
                                onClick = { settingsViewModel.syncNow() }
                            )
                        }
                    }
                }
            }
            
            // Playback Features Section (NEW - Advanced)
            item {
                SettingsSection(title = "Advanced Playback") {
                    SettingsCard {
                        SettingsSwitchItem(
                            icon = Icons.Outlined.PlayCircle,
                            title = "Auto-Play Similar Songs",
                            subtitle = "Automatically play related songs",
                            checked = uiState.autoPlaySimilar,
                            onCheckedChange = { settingsViewModel.setAutoPlaySimilar(it) }
                        )
                        
                        HorizontalDivider(color = CardColor)
                        
                        SettingsSwitchItem(
                            icon = Icons.Outlined.VolumeUp,
                            title = "Normalize Volume",
                            subtitle = "Equalize volume across songs",
                            checked = uiState.normalizeAudio,
                            onCheckedChange = { settingsViewModel.setNormalizeAudio(it) }
                        )
                        
                        HorizontalDivider(color = CardColor)
                        
                        SettingsSwitchItem(
                            icon = Icons.Outlined.SkipNext,
                            title = "Skip Silence",
                            subtitle = "Automatically skip silent parts",
                            checked = uiState.skipSilence,
                            onCheckedChange = { settingsViewModel.setSkipSilence(it) }
                        )
                        
                        HorizontalDivider(color = CardColor)
                        
                        SettingsItem(
                            icon = Icons.Outlined.Speed,
                            title = "Default Playback Speed",
                            subtitle = "${uiState.playbackSpeed}x",
                            onClick = { /* Speed picker */ }
                        )
                    }
                }
            }
            
            // Social & Sharing Section (NEW - Spotify/YouTube Music)
            item {
                SettingsSection(title = "Social & Sharing") {
                    SettingsCard {
                        SettingsSwitchItem(
                            icon = Icons.Outlined.Share,
                            title = "Share Listening Activity",
                            subtitle = "Share what you're listening to",
                            checked = uiState.shareActivity,
                            onCheckedChange = { settingsViewModel.setShareActivity(it) }
                        )
                        
                        HorizontalDivider(color = CardColor)
                        
                        SettingsSwitchItem(
                            icon = Icons.Outlined.Public,
                            title = "Discord Rich Presence",
                            subtitle = "Show playing song on Discord",
                            checked = uiState.discordRichPresence,
                            onCheckedChange = { settingsViewModel.setDiscordRichPresence(it) }
                        )
                        
                        HorizontalDivider(color = CardColor)
                        
                        SettingsSwitchItem(
                            icon = Icons.Outlined.Notifications,
                            title = "Artist Notifications",
                            subtitle = "Get notified about new releases",
                            checked = uiState.artistNotifications,
                            onCheckedChange = { settingsViewModel.setArtistNotifications(it) }
                        )
                    }
                }
            }
            
            // Content Section
            item {
                SettingsSection(title = "Content") {
                    SettingsCard {
                        SettingsItem(
                            icon = Icons.Outlined.Language,
                            title = "Content Language",
                            subtitle = "English, Hindi, Telugu, Tamil",
                            onClick = { /* Language picker */ }
                        )
                        
                        HorizontalDivider(color = CardColor)
                        
                        SettingsSwitchItem(
                            icon = Icons.Outlined.Block,
                            title = "Incognito Mode",
                            subtitle = "Don't save listening history",
                            checked = uiState.incognitoMode,
                            onCheckedChange = { settingsViewModel.setIncognitoMode(it) }
                        )
                        
                        HorizontalDivider(color = CardColor)
                        
                        SettingsSwitchItem(
                            icon = Icons.Outlined.AutoAwesome,
                            title = "AI Recommendations",
                            subtitle = "Get personalized song suggestions",
                            checked = uiState.aiRecommendations,
                            onCheckedChange = { settingsViewModel.setAIRecommendations(it) }
                        )
                    }
                }
            }
            
            // Car Mode Section (NEW - Gaana/Wynk)
            item {
                SettingsSection(title = "Car Mode") {
                    SettingsCard {
                        SettingsSwitchItem(
                            icon = Icons.Outlined.DirectionsCar,
                            title = "Enable Car Mode",
                            subtitle = "Large buttons for easy control",
                            checked = uiState.carMode,
                            onCheckedChange = { settingsViewModel.setCarMode(it) }
                        )
                        
                        HorizontalDivider(color = CardColor)
                        
                        SettingsSwitchItem(
                            icon = Icons.Outlined.Mic,
                            title = "Voice Commands",
                            subtitle = "Control with voice",
                            checked = uiState.voiceCommands,
                            onCheckedChange = { settingsViewModel.setVoiceCommands(it) }
                        )
                    }
                }
            }
            
            // Sleep Timer Section (NEW - All apps)
            item {
                SettingsSection(title = "Sleep Timer") {
                    SettingsCard {
                        SettingsItem(
                            icon = Icons.Outlined.Bedtime,
                            title = "Sleep Timer",
                            subtitle = if (uiState.sleepTimerMinutes > 0) 
                                "${uiState.sleepTimerMinutes} minutes" else "Off",
                            onClick = { /* Sleep timer picker */ }
                        )
                    }
                }
            }
            
            // Data & Privacy Section
            item {
                SettingsSection(title = "Data & Privacy") {
                    SettingsCard {
                        SettingsItem(
                            icon = Icons.Outlined.History,
                            title = "Save Listening History",
                            subtitle = if (uiState.saveHistory) "History is saved" else "History is not saved",
                            onClick = { settingsViewModel.setSaveHistory(!uiState.saveHistory) }
                        )
                        
                        HorizontalDivider(color = CardColor)
                        
                        SettingsItem(
                            icon = Icons.Outlined.Delete,
                            title = "Clear Cache",
                            subtitle = "Free up storage space",
                            onClick = { settingsViewModel.clearCache() }
                        )
                        
                        HorizontalDivider(color = CardColor)
                        
                        SettingsItem(
                            icon = Icons.Outlined.Search,
                            title = "Cloud Sync",
                            subtitle = if (uiState.cloudSyncEnabled) "Sync enabled" else "Sync disabled",
                            onClick = { settingsViewModel.setCloudSyncEnabled(!uiState.cloudSyncEnabled) }
                        )
                    }
                }
            }
            
            // About Section
            item {
                SettingsSection(title = "About") {
                    SettingsCard {
                        SettingsItem(
                            icon = Icons.Outlined.Info,
                            title = "About REON",
                            subtitle = "Version 1.0.0",
                            onClick = { showAboutDialog = true }
                        )
                        
                        HorizontalDivider(color = CardColor)
                        
                        SettingsItem(
                            icon = Icons.Outlined.Person,
                            title = "Created by",
                            subtitle = "Rohith",
                            onClick = { }
                        )
                        
                        HorizontalDivider(color = CardColor)
                        
                        SettingsItem(
                            icon = Icons.Outlined.Code,
                            title = "Open Source Libraries",
                            subtitle = "View licenses",
                            onClick = { /* Open source credits */ }
                        )
                    }
                }
            }
            
            // Bottom spacing
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
    
    // Audio Quality Dialog
    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text("Audio Quality", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    QualityOption(
                        title = "Low (96 kbps)",
                        subtitle = "Save data, lower quality",
                        selected = uiState.audioQuality == AudioQuality.LOW,
                        onClick = {
                            settingsViewModel.setAudioQuality(AudioQuality.LOW)
                            showQualityDialog = false
                        }
                    )
                    QualityOption(
                        title = "Medium (160 kbps)",
                        subtitle = "Balanced quality and data",
                        selected = uiState.audioQuality == AudioQuality.MEDIUM,
                        onClick = {
                            settingsViewModel.setAudioQuality(AudioQuality.MEDIUM)
                            showQualityDialog = false
                        }
                    )
                    QualityOption(
                        title = "High (320 kbps)",
                        subtitle = "Best quality, uses more data",
                        selected = uiState.audioQuality == AudioQuality.HIGH,
                        onClick = {
                            settingsViewModel.setAudioQuality(AudioQuality.HIGH)
                            showQualityDialog = false
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showQualityDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // App Logo
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF424242),
                                        Color(0xFF212121),
                                        Color(0xFF1A1A1A)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "R",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "REON Music",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Version 1.0.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "A premium music streaming experience with high-quality audio, lyrics support, and seamless playback.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Created by Rohith",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "© 2024 REON. All rights reserved.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAboutDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text("Close")
                }
            }
        )
    }
    
    // Cache Dialog
    if (showCacheDialog) {
        AlertDialog(
            onDismissRequest = { showCacheDialog = false },
            title = { Text("Clear Cache", fontWeight = FontWeight.Bold) },
            text = {
                Text("This will clear all cached images and data. Downloaded songs will not be affected.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        settingsViewModel.clearCache()
                        showCacheDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCacheDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Theme Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Theme", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    ThemeOption(
                        title = "Light",
                        subtitle = "Light theme",
                        selected = uiState.theme == com.reon.music.core.preferences.AppTheme.LIGHT,
                        onClick = {
                            settingsViewModel.setTheme(com.reon.music.core.preferences.AppTheme.LIGHT)
                            showThemeDialog = false
                        }
                    )
                    ThemeOption(
                        title = "Dark",
                        subtitle = "Dark theme",
                        selected = uiState.theme == com.reon.music.core.preferences.AppTheme.DARK,
                        onClick = {
                            settingsViewModel.setTheme(com.reon.music.core.preferences.AppTheme.DARK)
                            showThemeDialog = false
                        }
                    )
                    ThemeOption(
                        title = "System Default",
                        subtitle = "Follow system theme",
                        selected = uiState.theme == com.reon.music.core.preferences.AppTheme.SYSTEM,
                        onClick = {
                            settingsViewModel.setTheme(com.reon.music.core.preferences.AppTheme.SYSTEM)
                            showThemeDialog = false
                        }
                    )
                    ThemeOption(
                        title = "AMOLED Black",
                        subtitle = "Pure black for OLED displays",
                        selected = uiState.theme == com.reon.music.core.preferences.AppTheme.AMOLED,
                        onClick = {
                            settingsViewModel.setTheme(com.reon.music.core.preferences.AppTheme.AMOLED)
                            showThemeDialog = false
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // NEW: Theme Preset Dialog
    if (showThemePresetDialog) {
        com.reon.music.ui.components.ThemePresetSelector(
            selectedPresetId = uiState.themePresetId,
            onPresetSelected = { presetId ->
                settingsViewModel.setThemePreset(presetId)
                showThemePresetDialog = false
            },
            onDismiss = { showThemePresetDialog = false }
        )
    }
    
    // NEW: Font Dialog
    if (showFontDialog) {
        com.reon.music.ui.components.FontPresetSelector(
            selectedFontId = uiState.fontPresetId,
            onFontSelected = { fontId ->
                settingsViewModel.setFontPreset(fontId)
                showFontDialog = false
            },
            onDismiss = { showFontDialog = false }
        )
    }
    
    // NEW: Font Size Dialog
    if (showFontSizeDialog) {
        com.reon.music.ui.components.FontSizeSelector(
            selectedSize = uiState.fontSizePreset,
            onSizeSelected = { size ->
                settingsViewModel.setFontSize(size)
                showFontSizeDialog = false
            },
            onDismiss = { showFontSizeDialog = false }
        )
    }
}

@Composable
private fun ThemeOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(if (selected) AccentRed.copy(alpha = 0.1f) else Color.Transparent)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = AccentRed)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) AccentRed else TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun QualityOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(if (selected) AccentRed.copy(alpha = 0.1f) else Color.Transparent)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = AccentRed)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) AccentRed else TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        content()
    }
}

@Composable
private fun SettingsCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = AccentRed,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = AccentRed,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AccentRed,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = CardColor
            )
        )
    }
}

/**
 * Format last sync timestamp to human-readable string
 */
private fun formatLastSync(timestamp: Long): String {
    if (timestamp == 0L) return "Never"
    
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000} minutes ago"
        diff < 86400_000 -> "${diff / 3600_000} hours ago"
        diff < 604800_000 -> "${diff / 86400_000} days ago"
        else -> {
            val date = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                .format(java.util.Date(timestamp))
            date
        }
    }
}
