/*
 * REON Music App - Enhanced Now Playing Screen
 * Copyright (c) 2024 REON
 * Modern Light Theme Design
 */

package com.reon.music.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.reon.music.core.model.Song
import com.reon.music.ui.viewmodels.PlayerViewModel

// Light theme color palette
private val LightBackground = Color(0xFFFAFAFA)
private val SurfaceColor = Color.White
private val CardColor = Color(0xFFF5F5F5)
private val AccentRed = Color(0xFFE53935)
private val AccentGreen = Color(0xFF4CAF50)
private val TextPrimary = Color(0xFF1A1A1A)
private val TextSecondary = Color(0xFF666666)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    playerViewModel: PlayerViewModel,
    onDismiss: () -> Unit
) {
    val playerState by playerViewModel.playerState.collectAsState()
    val uiState by playerViewModel.uiState.collectAsState()
    val currentPosition by playerViewModel.currentPosition.collectAsState()
    
    var showQueueSheet by remember { mutableStateOf(false) }
    var showOptionsSheet by remember { mutableStateOf(false) }
    var isLiked by remember { mutableStateOf(false) }
    
    val currentSong = playerState.currentSong
    val progress = if (currentSong != null && currentSong.duration > 0) {
        currentPosition.toFloat() / (currentSong.duration * 1000)
    } else 0f
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Close",
                        tint = TextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NOW PLAYING",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = currentSong?.album?.takeIf { it.isNotBlank() } ?: "Unknown Album",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                IconButton(onClick = { showOptionsSheet = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = TextPrimary
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(0.3f))
            
            // Album Art with shadow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .shadow(24.dp, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                ) {
                    if (currentSong?.artworkUrl != null) {
                        AsyncImage(
                            model = currentSong.getHighQualityArtwork(),
                            contentDescription = "Album Art",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(CardColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(100.dp),
                                tint = TextSecondary
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(0.3f))
            
            // Song Info with Like Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentSong?.title ?: "No song playing",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentSong?.artist ?: "Unknown Artist",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                IconButton(onClick = { isLiked = !isLiked }) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) AccentRed else TextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Progress Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                Slider(
                    value = progress.coerceIn(0f, 1f),
                    onValueChange = { value ->
                        currentSong?.let { song ->
                            val newPosition = (value * song.duration * 1000).toLong()
                            playerViewModel.seekTo(newPosition)
                        }
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = AccentRed,
                        activeTrackColor = AccentRed,
                        inactiveTrackColor = CardColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(currentPosition),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = formatTime((currentSong?.duration ?: 0) * 1000L),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Main Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                IconButton(onClick = { playerViewModel.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (playerState.shuffleEnabled) AccentRed else TextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                // Previous
                IconButton(
                    onClick = { playerViewModel.skipToPrevious() },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = TextPrimary,
                        modifier = Modifier.size(40.dp)
                    )
                }
                
                // Play/Pause
                FloatingActionButton(
                    onClick = { playerViewModel.togglePlayPause() },
                    modifier = Modifier.size(72.dp),
                    containerColor = AccentRed,
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(40.dp)
                    )
                }
                
                // Next
                IconButton(
                    onClick = { playerViewModel.skipToNext() },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = TextPrimary,
                        modifier = Modifier.size(40.dp)
                    )
                }
                
                // Repeat
                IconButton(onClick = { playerViewModel.cycleRepeatMode() }) {
                    Icon(
                        imageVector = when (playerState.repeatMode) {
                            1 -> Icons.Default.RepeatOne
                            2 -> Icons.Default.Repeat
                            else -> Icons.Default.Repeat
                        },
                        contentDescription = "Repeat",
                        tint = if (playerState.repeatMode > 0) AccentRed else TextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Bottom Controls (Info, Download, Add to Playlist, Queue)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Song Info
                IconButton(onClick = { /* Show song info */ }) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Song Info",
                        tint = TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // Download Button
                IconButton(onClick = { currentSong?.let { playerViewModel.downloadSong(it) } }) {
                    Icon(
                        imageVector = Icons.Outlined.Download,
                        contentDescription = "Download",
                        tint = TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Add to Playlist
                IconButton(onClick = { /* Add to playlist */ }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add to Playlist",
                        tint = TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // Queue
                IconButton(onClick = { showQueueSheet = true }) {
                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = "Queue",
                        tint = TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Queue Bottom Sheet
        if (showQueueSheet) {
            QueueBottomSheet(
                currentSong = currentSong,
                queue = playerState.queue,
                currentIndex = playerState.currentIndex,
                onDismiss = { showQueueSheet = false },
                onSongClick = { index -> playerViewModel.playFromQueue(index) },
                onRemove = { index -> playerViewModel.removeFromQueue(index) }
            )
        }
        
        // Options Bottom Sheet
        if (showOptionsSheet) {
            OptionsBottomSheet(
                song = currentSong,
                onDismiss = { showOptionsSheet = false },
                onDownload = { currentSong?.let { playerViewModel.downloadSong(it) } },
                onShare = { /* Share */ },
                onViewArtist = { /* View artist */ },
                onViewAlbum = { /* View album */ },
                onAddToPlaylist = { /* Add to playlist */ },
                onSleepTimer = { /* Sleep timer */ },
                onEqualizer = { /* Equalizer */ }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueBottomSheet(
    currentSong: Song?,
    queue: List<Song>,
    currentIndex: Int,
    onDismiss: () -> Unit,
    onSongClick: (Int) -> Unit,
    onRemove: (Int) -> Unit
) {
    var endlessQueue by remember { mutableStateOf(true) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceColor,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
        ) {
            // Header
            Text(
                text = "Now Playing",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )
            
            currentSong?.let { song ->
                QueueSongItem(
                    song = song,
                    isPlaying = true,
                    showEqualizer = true,
                    onClick = {},
                    onMoreClick = {}
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Queue Section with Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Queue",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Endless queue",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = endlessQueue,
                        onCheckedChange = { endlessQueue = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentRed,
                            checkedTrackColor = AccentRed.copy(alpha = 0.5f)
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Queue List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                itemsIndexed(queue) { index, song ->
                    if (index != currentIndex) {
                        QueueSongItem(
                            song = song,
                            isPlaying = false,
                            showEqualizer = false,
                            onClick = { onSongClick(index) },
                            onMoreClick = { /* Options */ }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueSongItem(
    song: Song,
    isPlaying: Boolean,
    showEqualizer: Boolean,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (isPlaying) AccentRed.copy(alpha = 0.1f) else Color.Transparent)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail or Equalizer
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(CardColor),
            contentAlignment = Alignment.Center
        ) {
            if (showEqualizer) {
                EqualizerAnimation()
            } else if (song.artworkUrl != null) {
                AsyncImage(
                    model = song.artworkUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = TextSecondary
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        IconButton(onClick = onMoreClick) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More",
                tint = TextSecondary
            )
        }
    }
}

@Composable
private fun EqualizerAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer")
    
    val bar1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    
    val bar2 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    
    val bar3 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )
    
    Row(
        modifier = Modifier.size(24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight(bar1)
                .clip(RoundedCornerShape(2.dp))
                .background(AccentRed)
        )
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight(bar2)
                .clip(RoundedCornerShape(2.dp))
                .background(AccentRed)
        )
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight(bar3)
                .clip(RoundedCornerShape(2.dp))
                .background(AccentRed)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionsBottomSheet(
    song: Song?,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onViewArtist: () -> Unit,
    onViewAlbum: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onSleepTimer: () -> Unit,
    onEqualizer: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceColor,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Song header
            song?.let {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CardColor)
                    ) {
                        if (it.artworkUrl != null) {
                            AsyncImage(
                                model = it.artworkUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = it.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = it.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                HorizontalDivider(color = CardColor)
            }
            
            // Options
            OptionItem(icon = Icons.Outlined.Download, title = "Download", onClick = { onDownload(); onDismiss() })
            OptionItem(icon = Icons.Outlined.PlaylistAdd, title = "Add to Playlist", onClick = { onAddToPlaylist(); onDismiss() })
            OptionItem(icon = Icons.Outlined.Person, title = "View Artist", onClick = { onViewArtist(); onDismiss() })
            OptionItem(icon = Icons.Outlined.Album, title = "View Album", onClick = { onViewAlbum(); onDismiss() })
            OptionItem(icon = Icons.Outlined.Share, title = "Share", onClick = { onShare(); onDismiss() })
            OptionItem(icon = Icons.Outlined.Timer, title = "Sleep Timer", onClick = { onSleepTimer(); onDismiss() })
            OptionItem(icon = Icons.Outlined.Equalizer, title = "Equalizer", onClick = { onEqualizer(); onDismiss() })
        }
    }
}

@Composable
private fun OptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
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
            tint = TextPrimary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary
        )
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
