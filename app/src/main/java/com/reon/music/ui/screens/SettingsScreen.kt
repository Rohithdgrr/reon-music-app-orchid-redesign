/*
 * REON Music App - Settings Screen
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 */

package com.reon.music.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.reon.music.core.preferences.AppTheme
import com.reon.music.ui.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by settingsViewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Playback Section
            item {
                SettingsSection(title = "Playback") {
                    // Audio Quality
                    SettingsItem(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        title = "Audio Quality",
                        subtitle = uiState.audioQuality.label,
                        onClick = { /* Show quality picker */ }
                    )
                    
                    // Crossfade
                    SettingsSliderItem(
                        icon = Icons.Default.Speed,
                        title = "Crossfade",
                        value = uiState.crossfadeDuration.toFloat(),
                        valueRange = 0f..12f,
                        steps = 11,
                        valueLabel = if (uiState.crossfadeDuration == 0) "Off" 
                                     else "${uiState.crossfadeDuration}s",
                        onValueChange = { settingsViewModel.setCrossfade(it.toInt()) }
                    )
                    
                    // Gapless Playback
                    SettingsSwitchItem(
                        icon = Icons.Default.MusicNote,
                        title = "Gapless Playback",
                        subtitle = "Removes silence between tracks",
                        checked = uiState.gaplessPlayback,
                        onCheckedChange = { settingsViewModel.setGaplessPlayback(it) }
                    )
                    
                    // Normalize Audio
                    SettingsSwitchItem(
                        icon = Icons.Default.Tune,
                        title = "Normalize Audio",
                        subtitle = "Consistent volume across tracks",
                        checked = uiState.normalizeAudio,
                        onCheckedChange = { settingsViewModel.setNormalizeAudio(it) }
                    )
                }
            }
            
            // Appearance Section
            item {
                SettingsSection(title = "Appearance") {
                    // Theme
                    SettingsItem(
                        icon = Icons.Default.Brightness4,
                        title = "Theme",
                        subtitle = uiState.theme.label,
                        onClick = { /* Show theme picker */ }
                    )
                    
                    // Dynamic Colors
                    SettingsSwitchItem(
                        icon = Icons.Default.Palette,
                        title = "Dynamic Colors",
                        subtitle = "Adapt colors from album art",
                        checked = uiState.dynamicColors,
                        onCheckedChange = { settingsViewModel.setDynamicColors(it) }
                    )
                    
                    // Pure Black
                    if (uiState.theme == AppTheme.DARK || uiState.theme == AppTheme.AMOLED) {
                        SettingsSwitchItem(
                            icon = Icons.Default.ColorLens,
                            title = "Pure Black Background",
                            subtitle = "AMOLED-friendly dark mode",
                            checked = uiState.pureBlack,
                            onCheckedChange = { settingsViewModel.setPureBlack(it) }
                        )
                    }
                    
                    // Language
                    SettingsItem(
                        icon = Icons.Default.Language,
                        title = "Language",
                        subtitle = "English",
                        onClick = { /* Show language picker */ }
                    )
                }
            }
            
            // Downloads Section
            item {
                SettingsSection(title = "Downloads") {
                    SettingsItem(
                        icon = Icons.Default.Download,
                        title = "Download Quality",
                        subtitle = uiState.downloadQuality.label,
                        onClick = { /* Show quality picker */ }
                    )
                    
                    SettingsSwitchItem(
                        icon = Icons.Default.Cloud,
                        title = "Download on Wi-Fi Only",
                        subtitle = "Save mobile data",
                        checked = uiState.downloadWifiOnly,
                        onCheckedChange = { settingsViewModel.setDownloadWifiOnly(it) }
                    )
                }
            }
            
            // Lyrics Section
            item {
                SettingsSection(title = "Lyrics") {
                    SettingsSwitchItem(
                        icon = Icons.Default.Lyrics,
                        title = "Show Lyrics by Default",
                        subtitle = "Display lyrics on Now Playing",
                        checked = uiState.showLyricsDefault,
                        onCheckedChange = { settingsViewModel.setShowLyricsDefault(it) }
                    )
                }
            }
            
            // Privacy Section
            item {
                SettingsSection(title = "Privacy") {
                    SettingsSwitchItem(
                        icon = Icons.Default.History,
                        title = "Save Listening History",
                        subtitle = "Track your listening for recommendations",
                        checked = uiState.saveHistory,
                        onCheckedChange = { settingsViewModel.setSaveHistory(it) }
                    )
                    
                    SettingsSwitchItem(
                        icon = Icons.Default.VisibilityOff,
                        title = "Incognito Mode",
                        subtitle = "Disable history & sync temporarily",
                        checked = uiState.incognitoMode,
                        onCheckedChange = { settingsViewModel.setIncognitoMode(it) }
                    )
                }
            }
            
            // Cloud Sync Section
            item {
                SettingsSection(title = "Cloud Sync") {
                    SettingsSwitchItem(
                        icon = Icons.Default.Cloud,
                        title = "Enable Cloud Sync",
                        subtitle = "Sync playlists & favorites across devices",
                        checked = uiState.cloudSyncEnabled,
                        onCheckedChange = { settingsViewModel.setCloudSyncEnabled(it) }
                    )
                    
                    if (uiState.cloudSyncEnabled) {
                        SettingsItem(
                            icon = Icons.Default.Storage,
                            title = "Last Synced",
                            subtitle = if (uiState.lastSyncTime > 0) 
                                formatTimestamp(uiState.lastSyncTime) 
                            else "Never",
                            onClick = { settingsViewModel.syncNow() }
                        )
                    }
                }
            }
            
            // Storage Section
            item {
                SettingsSection(title = "Storage") {
                    SettingsItem(
                        icon = Icons.Default.Storage,
                        title = "Clear Cache",
                        subtitle = "Free up space",
                        onClick = { settingsViewModel.clearCache() }
                    )
                }
            }
            
            // About Section
            item {
                SettingsSection(title = "About") {
                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = "Version",
                        subtitle = "1.0.0 (Build 1)",
                        onClick = { }
                    )
                    
                    SettingsItem(
                        icon = Icons.Default.Code,
                        title = "GitHub",
                        subtitle = "github.com/Rohithdgrr/REON-Music-app",
                        onClick = { /* Open GitHub */ }
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
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
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsSliderItem(
    icon: ImageVector,
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueLabel: String,
    onValueChange: (Float) -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(value) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onValueChange(sliderValue) },
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.padding(start = 40.dp)
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000} minutes ago"
        diff < 86400_000 -> "${diff / 3600_000} hours ago"
        else -> "${diff / 86400_000} days ago"
    }
}
