/*
 * REON Music App - Library Screen
 * Copyright (c) 2024 REON
 * Modern Light Theme with Colorful Quick Access Cards
 */

package com.reon.music.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.reon.music.core.model.Song
import com.reon.music.data.database.entities.PlaylistEntity
import com.reon.music.ui.viewmodels.LibraryTab
import com.reon.music.ui.viewmodels.LibraryViewModel
import com.reon.music.ui.viewmodels.PlayerViewModel

// Light theme colors
private val LightBackground = Color(0xFFFAFAFA)
private val SurfaceColor = Color.White
private val CardColor = Color(0xFFF0F0F0)
private val AccentRed = Color(0xFFE53935)
private val TextPrimary = Color(0xFF1A1A1A)
private val TextSecondary = Color(0xFF666666)

// Colorful card colors (matching reference image)
private val FavoriteColor = Color(0xFFFF6B9D)  // Pink
private val FollowedColor = Color(0xFFFFEB3B)  // Yellow
private val MostPlayedColor = Color(0xFF4DD0E1) // Cyan
private val DownloadedColor = Color(0xFF69F0AE) // Green

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by libraryViewModel.uiState.collectAsState()
    val playerState by playerViewModel.playerState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Library",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LightBackground
                )
            )
        },
        floatingActionButton = {
            if (uiState.selectedTab == LibraryTab.PLAYLISTS) {
                FloatingActionButton(
                    onClick = { libraryViewModel.showCreatePlaylistDialog() },
                    containerColor = AccentRed,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Playlist")
                }
            }
        },
        containerColor = LightBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(LibraryTab.entries) { tab ->
                    FilterChip(
                        selected = uiState.selectedTab == tab,
                        onClick = { libraryViewModel.selectTab(tab) },
                        label = { 
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (uiState.selectedTab == tab) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = when (tab) {
                                        LibraryTab.OVERVIEW -> "Your library"
                                        LibraryTab.LIKED -> "Liked"
                                        LibraryTab.PLAYLISTS -> "Playlists"
                                        LibraryTab.DOWNLOADS -> "Downloads"
                                        LibraryTab.HISTORY -> "History"
                                    }
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TextPrimary,
                            selectedLabelColor = Color.White,
                            containerColor = CardColor,
                            labelColor = TextPrimary
                        ),
                        border = null
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AccentRed)
                    }
                }
                else -> {
                    when (uiState.selectedTab) {
                        LibraryTab.OVERVIEW -> LibraryOverview(
                            uiState = uiState,
                            onSongClick = { playerViewModel.playSong(it) },
                            onPlaylistClick = { libraryViewModel.selectTab(LibraryTab.PLAYLISTS) },
                            onLikedClick = { libraryViewModel.selectTab(LibraryTab.LIKED) },
                            onDownloadsClick = { libraryViewModel.selectTab(LibraryTab.DOWNLOADS) },
                            onHistoryClick = { libraryViewModel.selectTab(LibraryTab.HISTORY) }
                        )
                        LibraryTab.LIKED -> LikedSongsScreen(
                            songs = uiState.likedSongs,
                            currentSongId = playerState.currentSong?.id,
                            onSongClick = { playerViewModel.playSong(it) },
                            onShuffleAll = { 
                                if (uiState.likedSongs.isNotEmpty()) {
                                    playerViewModel.playQueue(uiState.likedSongs.shuffled())
                                }
                            }
                        )
                        LibraryTab.PLAYLISTS -> PlaylistsScreen(
                            playlists = uiState.playlists,
                            onPlaylistClick = { /* Navigate to playlist */ },
                            onDeletePlaylist = { libraryViewModel.deletePlaylist(it) }
                        )
                        LibraryTab.DOWNLOADS -> DownloadsTabContent(
                            songs = uiState.downloadedSongs,
                            currentSongId = playerState.currentSong?.id,
                            onSongClick = { playerViewModel.playSong(it) }
                        )
                        LibraryTab.HISTORY -> HistoryScreen(
                            songs = uiState.recentlyPlayed,
                            currentSongId = playerState.currentSong?.id,
                            onSongClick = { playerViewModel.playSong(it) }
                        )
                    }
                }
            }
        }
        
        // Create playlist dialog
        if (uiState.showCreatePlaylistDialog) {
            CreatePlaylistDialog(
                onDismiss = { libraryViewModel.hideCreatePlaylistDialog() },
                onCreate = { name, desc -> libraryViewModel.createPlaylist(name, desc) }
            )
        }
    }
}

@Composable
private fun LibraryOverview(
    uiState: com.reon.music.ui.viewmodels.LibraryUiState,
    onSongClick: (Song) -> Unit,
    onPlaylistClick: () -> Unit,
    onLikedClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Colorful quick access cards (2x2 grid)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickAccessCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Favorite,
                    title = "Favorite",
                    backgroundColor = FavoriteColor,
                    iconColor = Color(0xFFD50000),
                    onClick = onLikedClick
                )
                QuickAccessCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.TrendingUp,
                    title = "Followed",
                    backgroundColor = FollowedColor,
                    iconColor = Color(0xFF827717),
                    onClick = onPlaylistClick
                )
            }
        }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickAccessCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.ShowChart,
                    title = "Most Played",
                    backgroundColor = MostPlayedColor,
                    iconColor = Color(0xFF00838F),
                    onClick = onHistoryClick
                )
                QuickAccessCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Download,
                    title = "Downloaded",
                    backgroundColor = DownloadedColor,
                    iconColor = Color(0xFF2E7D32),
                    onClick = onDownloadsClick
                )
            }
        }
        
        // Recently Added section
        if (uiState.recentlyPlayed.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Recently Added",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            
            items(uiState.recentlyPlayed.take(10)) { song ->
                SongListItem(
                    song = song,
                    isPlaying = false,
                    onClick = { onSongClick(song) }
                )
            }
        }
        
        // Bottom spacing
        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}

@Composable
private fun QuickAccessCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    backgroundColor: Color,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(56.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun LikedSongsScreen(
    songs: List<Song>,
    currentSongId: String?,
    onSongClick: (Song) -> Unit,
    onShuffleAll: () -> Unit
) {
    if (songs.isEmpty()) {
        EmptyStateView(
            icon = Icons.Outlined.FavoriteBorder,
            title = "No liked songs yet",
            subtitle = "Songs you like will appear here"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) {
            item {
                // Shuffle button
                Button(
                    onClick = onShuffleAll,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Shuffle All (${songs.size})")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            itemsIndexed(songs) { _, song ->
                SongListItem(
                    song = song,
                    isPlaying = song.id == currentSongId,
                    onClick = { onSongClick(song) }
                )
            }
            
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
private fun PlaylistsScreen(
    playlists: List<PlaylistEntity>,
    onPlaylistClick: (PlaylistEntity) -> Unit,
    onDeletePlaylist: (Long) -> Unit
) {
    if (playlists.isEmpty()) {
        EmptyStateView(
            icon = Icons.Outlined.PlaylistPlay,
            title = "No playlists yet",
            subtitle = "Create your first playlist"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) {
            items(playlists) { playlist ->
                PlaylistItem(
                    playlist = playlist,
                    onClick = { onPlaylistClick(playlist) },
                    onDelete = { onDeletePlaylist(playlist.id) }
                )
            }
            
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
private fun PlaylistItem(
    playlist: PlaylistEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(CardColor),
            contentAlignment = Alignment.Center
        ) {
            if (playlist.thumbnailUrl != null) {
                AsyncImage(
                    model = playlist.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.PlaylistPlay,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${playlist.songCount} songs",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = TextSecondary
            )
        }
    }
}

@Composable
private fun DownloadsTabContent(
    songs: List<Song>,
    currentSongId: String?,
    onSongClick: (Song) -> Unit
) {
    if (songs.isEmpty()) {
        EmptyStateView(
            icon = Icons.Outlined.Download,
            title = "No downloads yet",
            subtitle = "Download songs to listen offline"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) {
            itemsIndexed(songs) { _, song ->
                SongListItem(
                    song = song,
                    isPlaying = song.id == currentSongId,
                    onClick = { onSongClick(song) },
                    showDownloadIcon = true
                )
            }
            
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
private fun HistoryScreen(
    songs: List<Song>,
    currentSongId: String?,
    onSongClick: (Song) -> Unit
) {
    if (songs.isEmpty()) {
        EmptyStateView(
            icon = Icons.Outlined.History,
            title = "No history yet",
            subtitle = "Songs you play will appear here"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) {
            itemsIndexed(songs) { _, song ->
                SongListItem(
                    song = song,
                    isPlaying = song.id == currentSongId,
                    onClick = { onSongClick(song) }
                )
            }
            
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
private fun EmptyStateView(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = TextSecondary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SongListItem(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    showDownloadIcon: Boolean = false,
    onMoreClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isPlaying) AccentRed.copy(alpha = 0.1f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(CardColor),
            contentAlignment = Alignment.Center
        ) {
            if (song.artworkUrl != null) {
                AsyncImage(
                    model = song.artworkUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = TextSecondary
                )
            }
        }
        
        Spacer(modifier = Modifier.width(14.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isPlaying) AccentRed else TextPrimary
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        if (showDownloadIcon) {
            Icon(
                Icons.Default.Download,
                contentDescription = "Downloaded",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        IconButton(onClick = onMoreClick, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Playlist", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Playlist name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentRed,
                        cursorColor = AccentRed
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentRed,
                        cursorColor = AccentRed
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onCreate(name, description.takeIf { it.isNotBlank() }) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
