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
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by settingsViewModel.uiState.collectAsState()
    
    var showAboutDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showCacheDialog by remember { mutableStateOf(false) }
    
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
            
            // Appearance Section
            item {
                SettingsSection(title = "Appearance") {
                    SettingsCard {
                        SettingsSwitchItem(
                            icon = Icons.Outlined.Palette,
                            title = "Dynamic Colors",
                            subtitle = "Colors adapt to album art",
                            checked = uiState.dynamicColors,
                            onCheckedChange = { settingsViewModel.setDynamicColors(it) }
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
