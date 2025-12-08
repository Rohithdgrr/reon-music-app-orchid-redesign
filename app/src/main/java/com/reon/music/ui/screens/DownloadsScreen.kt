/*
 * REON Music App - SimpMusic-Inspired Downloads Screen
 * Copyright (c) 2024 REON
 * Modern, Clean Design with Red Palette Light Theme
 */

package com.reon.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.reon.music.core.model.Song
import com.reon.music.ui.viewmodels.LibraryViewModel
import com.reon.music.ui.viewmodels.PlayerViewModel
import java.io.File
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import com.reon.music.data.database.entities.PlaylistEntity
import com.reon.music.ui.viewmodels.SettingsViewModel
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

// SimpMusic Color Palette
private val BackgroundWhite = Color(0xFFFFFFFF)
private val SurfaceLight = Color(0xFFFAFAFA)
private val TextPrimary = Color(0xFF1C1C1C)
private val TextSecondary = Color(0xFF757575)
private val AccentRed = Color(0xFFE53935)
private val AccentGreen = Color(0xFF43A047)
private val AccentOrange = Color(0xFFFF9800)

// Download status enum
enum class DownloadStatus {
    COMPLETED,
    DOWNLOADING,
    FAILED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by libraryViewModel.uiState.collectAsState()
    val playerState by playerViewModel.playerState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    
    val context = LocalContext.current
    
    // Tab state for grouping
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("All Songs", "By Playlist", "By Artist")
    
    // Song options state
    var showSongOptions by remember { mutableStateOf(false) }
    var selectedSong by remember { mutableStateOf<Song?>(null) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Downloaded",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { /* Navigate back */ }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Search */ }) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = TextPrimary
                        )
                    }
                    // Radio mode button
                    IconButton(onClick = { 
                        if (uiState.downloadedSongs.isNotEmpty()) {
                            playerViewModel.enableRadioMode(uiState.downloadedSongs)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Radio,
                            contentDescription = "Radio Mode",
                            tint = AccentRed
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundWhite
                )
            )
        },
        containerColor = BackgroundWhite
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.downloadedSongs.isEmpty()) {
                EmptyDownloadsState()
            } else {
                // Storage Indicator
                StorageIndicator(
                    usedMB = calculateStorageUsed(uiState.downloadedSongs),
                    totalMB = 500 // Default or from settings
                )
                
                // Offline Mode Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Offline Mode",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Play only downloaded songs",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = settingsState.offlineModeEnabled,
                        onCheckedChange = { settingsViewModel.setOfflineModeEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AccentRed,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = SurfaceLight
                        )
                    )
                }
                
                // Tab Row for grouping
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = BackgroundWhite,
                    contentColor = AccentRed,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = AccentRed,
                            height = 3.dp
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == index) AccentRed else TextSecondary
                                )
                            }
                        )
                    }
                }
                
                // Play All / Shuffle Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { 
                            if (uiState.downloadedSongs.isNotEmpty()) {
                                playerViewModel.playQueue(uiState.downloadedSongs)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Play All")
                    }
                    OutlinedButton(
                        onClick = { 
                            if (uiState.downloadedSongs.isNotEmpty()) {
                                playerViewModel.playQueue(uiState.downloadedSongs.shuffled())
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed)
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Shuffle")
                    }
                }
                
                // Downloads List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(uiState.downloadedSongs) { index, song ->
                        DownloadItem(
                            song = song,
                            isPlaying = playerState.currentSong?.id == song.id,
                            downloadStatus = DownloadStatus.COMPLETED,
                            fileSize = "3.2MB", // Calculate actual size
                            onClick = { playerViewModel.playSong(song) },
                            onMoreClick = {
                                selectedSong = song
                                showSongOptions = true
                            }
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }
        
        // Song Options Bottom Sheet
        if (showSongOptions && selectedSong != null) {
            DownloadSongOptionsSheet(
                song = selectedSong!!,
                onDismiss = { showSongOptions = false },
                onPlay = { 
                    playerViewModel.playSong(selectedSong!!)
                    showSongOptions = false
                },
                onPlayNext = { 
                    playerViewModel.addToQueue(selectedSong!!, playNext = true)
                    showSongOptions = false
                },
                onAddToQueue = { 
                    playerViewModel.addToQueue(selectedSong!!)
                    showSongOptions = false
                },
                onAddToPlaylist = { 
                    showAddToPlaylistDialog = true
                    showSongOptions = false
                },
                onRemoveDownload = { 
                    libraryViewModel.removeDownload(selectedSong!!)
                    showSongOptions = false
                },
                onShare = { 
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "Listen to ${selectedSong!!.title} by ${selectedSong!!.artist}")
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Song"))
                    showSongOptions = false
                }
            )
        }
        
        // Add to Playlist Dialog
        if (showAddToPlaylistDialog && selectedSong != null) {
            AddToPlaylistDialog(
                playlists = uiState.playlists,
                onDismiss = { showAddToPlaylistDialog = false },
                onPlaylistSelected = { playlist ->
                    libraryViewModel.addToPlaylist(playlist.id, selectedSong!!)
                    showAddToPlaylistDialog = false
                }
            )
        }
    }
}

@Composable
private fun StorageIndicator(
    usedMB: Long,
    totalMB: Long
) {
    val percentage = (usedMB.toFloat() / totalMB.toFloat()).coerceIn(0f, 1f)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${usedMB}MB of ${totalMB}MB used",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Text(
                text = "${(percentage * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LinearProgressIndicator(
            progress = { percentage },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = AccentRed,
            trackColor = SurfaceLight
        )
    }
}

@Composable
private fun DownloadItem(
    song: Song,
    isPlaying: Boolean,
    downloadStatus: DownloadStatus,
    fileSize: String,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isPlaying) AccentRed.copy(alpha = 0.1f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album art thumbnail
        AsyncImage(
            model = song.getHighQualityArtwork(),
            contentDescription = song.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Song info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (isPlaying) AccentRed else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Show album/movie name if available
            if (song.album.isNotBlank()) {
                Text(
                    text = song.album,
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentRed.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Download status badge
        DownloadStatusBadge(
            status = downloadStatus,
            fileSize = fileSize
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // More options - NOW FUNCTIONAL
        IconButton(onClick = onMoreClick) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More Options",
                tint = TextSecondary
            )
        }
    }
}

@Composable
private fun DownloadStatusBadge(
    status: DownloadStatus,
    fileSize: String
) {
    val (text, color) = when (status) {
        DownloadStatus.COMPLETED -> "Done $fileSize" to AccentGreen
        DownloadStatus.DOWNLOADING -> "Downloading 45%" to AccentOrange
        DownloadStatus.FAILED -> "Failed" to AccentRed
    }
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadSongOptionsSheet(
    song: Song,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onRemoveDownload: () -> Unit,
    onShare: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BackgroundWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Song header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = song.getHighQualityArtwork(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = TextPrimary
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            HorizontalDivider(color = SurfaceLight)
            
            // Options
            OptionMenuItem(icon = Icons.Default.PlayArrow, title = "Play", onClick = onPlay)
            OptionMenuItem(icon = Icons.Default.PlaylistAdd, title = "Play Next", onClick = onPlayNext)
            OptionMenuItem(icon = Icons.Default.QueueMusic, title = "Add to Queue", onClick = onAddToQueue)
            OptionMenuItem(icon = Icons.Outlined.PlaylistAdd, title = "Add to Playlist", onClick = onAddToPlaylist)
            
            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = SurfaceLight)
            
            OptionMenuItem(icon = Icons.Default.Share, title = "Share", onClick = onShare)
            OptionMenuItem(
                icon = Icons.Default.Delete,
                title = "Remove Download",
                onClick = onRemoveDownload,
                tint = AccentRed
            )
        }
    }
}

@Composable
private fun OptionMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    tint: Color = TextPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = tint
        )
    }
}

@Composable
private fun EmptyDownloadsState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Download,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = TextSecondary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No downloads yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Download songs to listen offline",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

private fun calculateStorageUsed(songs: List<Song>): Long {
    // Calculate total storage used by downloaded songs
    // This is a placeholder - actual implementation would check file sizes
    return songs.size * 3L // Assume ~3MB per song on average
}

@Composable
private fun AddToPlaylistDialog(
    playlists: List<PlaylistEntity>,
    onDismiss: () -> Unit,
    onPlaylistSelected: (PlaylistEntity) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Playlist") },
        text = {
            if (playlists.isEmpty()) {
                Text(
                    text = "No playlists found.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(playlists) { playlist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPlaylistSelected(playlist) }
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.QueueMusic,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = playlist.title,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = BackgroundWhite,
        titleContentColor = TextPrimary,
        textContentColor = TextPrimary
    )
}

