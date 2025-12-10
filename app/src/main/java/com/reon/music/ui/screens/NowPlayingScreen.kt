/*
 * REON Music App - Enhanced Now Playing Screen
 * Copyright (c) 2024 REON
 * Pure White Background Design with Full Functionality
 */

package com.reon.music.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.reon.music.core.model.Song
import com.reon.music.ui.viewmodels.PlayerViewModel

// Pure White Theme Colors - Clean Premium Design
private val PureWhiteBackground = Color(0xFFFFFFFF)
private val SurfaceColor = Color.White
private val CardColor = Color(0xFFF8F9FA)
private val AccentRed = Color(0xFFFF0000) // YouTube red for play button
private val AccentRedLight = Color(0xFFFF4444)
private val TextPrimary = Color(0xFF1A1A1A)
private val TextSecondary = Color(0xFF666666)
private val DividerColor = Color(0xFFEEEEEE)
private val ProgressTrackColor = Color(0xFFE0E0E0)
private val ProgressActiveColor = Color(0xFFE53935)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    playerViewModel: PlayerViewModel,
    onDismiss: () -> Unit
) {
    val playerState by playerViewModel.playerState.collectAsState()
    val uiState by playerViewModel.uiState.collectAsState()
    val currentPosition by playerViewModel.currentPosition.collectAsState()
    val context = LocalContext.current
    
    var showQueueSheet by remember { mutableStateOf(false) }
    var showOptionsSheet by remember { mutableStateOf(false) }
    var showAddToPlaylistSheet by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showSongInfoDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    val isLiked = uiState.isLiked
    
    val currentSong = playerState.currentSong
    val progress = if (currentSong != null && currentSong.duration > 0) {
        currentPosition.toFloat() / (currentSong.duration * 1000)
    } else 0f
    
    // Gesture state for swipe to dismiss
    var dragOffset by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    
    // Pure white background - modern clean design
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhiteBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragOffset > 200f) {
                            onDismiss()
                        }
                        dragOffset = 0f
                        isDragging = false
                    },
                    onVerticalDrag = { change, dragAmount ->
                        if (dragAmount > 0f) { // Only allow downward drag
                            dragOffset = dragOffset + dragAmount
                            isDragging = true
                        }
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = dragOffset
                    alpha = 1f - (dragOffset / 1000f).coerceIn(0f, 0.5f)
                }
        ) {
            // Top Bar - Dark text on white background
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
            
            Spacer(modifier = Modifier.weight(0.1f))
            
            // Album Artwork - Prominent display
            AsyncImage(
                model = currentSong?.artworkUrl ?: currentSong?.getHighQualityArtwork(),
                contentDescription = "Album Art",
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .aspectRatio(1f)
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .shadow(16.dp, RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.weight(0.1f))
            
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
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentSong?.artist ?: "Unknown Artist",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp
                        ),
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                IconButton(onClick = { playerViewModel.toggleLike() }) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) ProgressActiveColor else TextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Progress Bar - Red themed
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
                        thumbColor = ProgressActiveColor,
                        activeTrackColor = ProgressActiveColor,
                        inactiveTrackColor = ProgressTrackColor
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
            
            // Main Controls - Shuffle, Previous, Play/Pause (YouTube Red), Next, Repeat
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                IconButton(
                    onClick = { playerViewModel.toggleShuffle() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (playerState.shuffleEnabled) ProgressActiveColor else TextSecondary,
                        modifier = Modifier.size(26.dp)
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
                
                // Play/Pause - YouTube Red circle with white icon
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(AccentRed)
                        .clickable { playerViewModel.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(40.dp),
                        tint = Color.White
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
                
                // Repeat - Cycles through OFF -> ALL -> ONE -> OFF
                IconButton(
                    onClick = { playerViewModel.cycleRepeatMode() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = when (playerState.repeatMode) {
                            1 -> Icons.Default.RepeatOne
                            2 -> Icons.Default.Repeat
                            else -> Icons.Default.Repeat
                        },
                        contentDescription = "Repeat",
                        tint = if (playerState.repeatMode > 0) ProgressActiveColor else TextSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Bottom Controls (Info, Download, Add to Playlist, Queue)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Song Info Button
                IconButton(
                    onClick = { showSongInfoDialog = true },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Song Info",
                        tint = TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // Download Button - Working
                IconButton(
                    onClick = { 
                        currentSong?.let { 
                            playerViewModel.downloadSong(it)
                        }
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Download,
                        contentDescription = "Download",
                        tint = TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // Radio Mode Toggle (Endless Playback)
                IconButton(
                    onClick = { 
                        currentSong?.let {
                            if (playerState.radioModeEnabled) {
                                playerViewModel.disableRadioMode()
                            } else {
                                playerViewModel.enableRadioMode(listOf(it))
                            }
                        }
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Radio,
                        contentDescription = if (playerState.radioModeEnabled) "Disable Radio Mode" else "Enable Radio Mode (Endless Play)",
                        tint = if (playerState.radioModeEnabled) AccentRed else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // Add to Playlist Button
                IconButton(
                    onClick = { showAddToPlaylistSheet = true },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add to Playlist",
                        tint = TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // Queue Button
                IconButton(
                    onClick = { showQueueSheet = true },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = "Queue",
                        tint = TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Radio Mode Indicator
            AnimatedVisibility(
                visible = playerState.radioModeEnabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentRed.copy(alpha = 0.1f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Radio,
                        contentDescription = null,
                        tint = AccentRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Radio Mode: Endless Songs Playing",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AccentRed,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(0.1f))
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
                playerViewModel = playerViewModel,
                onDismiss = { showOptionsSheet = false },
                onDownload = { 
                    currentSong?.let { playerViewModel.downloadSong(it) }
                    showOptionsSheet = false
                },
                onShare = { 
                    currentSong?.let { song ->
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Check out \"${song.title}\" by ${song.artist}")
                            putExtra(Intent.EXTRA_SUBJECT, "Sharing ${song.title}")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                    }
                    showOptionsSheet = false
                },
                onViewArtist = { 
                    // Can navigate to artist detail
                    showOptionsSheet = false
                },
                onViewAlbum = {
                    // Can navigate to album detail
                    showOptionsSheet = false 
                },
                onAddToPlaylist = { 
                    showOptionsSheet = false
                    showAddToPlaylistSheet = true 
                },
                onSleepTimer = { 
                    showOptionsSheet = false
                    showSleepTimerDialog = true 
                },
                onEqualizer = { 
                    // Launch system equalizer
                    showOptionsSheet = false
                },
                onPlaybackSpeed = {
                    showOptionsSheet = false
                    showSpeedDialog = true
                }
            )
        }
        
        // Add to Playlist Bottom Sheet
        if (showAddToPlaylistSheet) {
            AddToPlaylistBottomSheet(
                playerViewModel = playerViewModel,
                currentSong = currentSong,
                onDismiss = { showAddToPlaylistSheet = false },
                onCreatePlaylist = { showCreatePlaylistDialog = true }
            )
        }
        
        // Create Playlist Dialog
        if (showCreatePlaylistDialog) {
            CreatePlaylistDialog(
                onDismiss = { showCreatePlaylistDialog = false },
                onCreate = { name, desc ->
                    playerViewModel.createPlaylist(name, desc)
                    showCreatePlaylistDialog = false
                    showAddToPlaylistSheet = true
                }
            )
        }
        
        // Song Info Dialog
        if (showSongInfoDialog && currentSong != null) {
            SongInfoDialog(
                song = currentSong,
                onDismiss = { showSongInfoDialog = false }
            )
        }
        
        // Sleep Timer Dialog
        if (showSleepTimerDialog) {
            SleepTimerDialog(
                onDismiss = { showSleepTimerDialog = false },
                onSetTimer = { minutes ->
                    playerViewModel.setSleepTimer(minutes)
                    showSleepTimerDialog = false
                }
            )
        }
        
        // Playback Speed Dialog
        if (showSpeedDialog) {
            PlaybackSpeedDialog(
                onDismiss = { showSpeedDialog = false },
                onSetSpeed = { speed ->
                    playerViewModel.setPlaybackSpeed(speed)
                    showSpeedDialog = false
                }
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
                    text = "Queue (${queue.size} songs)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Endless",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = endlessQueue,
                        onCheckedChange = { endlessQueue = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ProgressActiveColor,
                            checkedTrackColor = ProgressActiveColor.copy(alpha = 0.5f)
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Queue List
            if (queue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Queue is empty",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add songs to queue to see them here",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    itemsIndexed(queue) { index, song ->
                        QueueSongItem(
                            song = song,
                            isPlaying = index == currentIndex,
                            showEqualizer = index == currentIndex,
                            onClick = { onSongClick(index) },
                            onMoreClick = { onRemove(index) }
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
            .background(if (isPlaying) ProgressActiveColor.copy(alpha = 0.1f) else Color.Transparent)
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
                color = if (isPlaying) ProgressActiveColor else TextPrimary,
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
                imageVector = Icons.Default.Close,
                contentDescription = "Remove from queue",
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
                .background(ProgressActiveColor)
        )
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight(bar2)
                .clip(RoundedCornerShape(2.dp))
                .background(ProgressActiveColor)
        )
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight(bar3)
                .clip(RoundedCornerShape(2.dp))
                .background(ProgressActiveColor)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionsBottomSheet(
    song: Song?,
    playerViewModel: PlayerViewModel,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onViewArtist: () -> Unit,
    onViewAlbum: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onSleepTimer: () -> Unit,
    onEqualizer: () -> Unit,
    onPlaybackSpeed: () -> Unit = {}
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
            OptionItem(icon = Icons.Outlined.Download, title = "Download", onClick = onDownload)
            OptionItem(icon = Icons.Outlined.PlaylistAdd, title = "Add to Playlist", onClick = onAddToPlaylist)
            OptionItem(icon = Icons.Outlined.Person, title = "View Artist", onClick = onViewArtist)
            OptionItem(icon = Icons.Outlined.Album, title = "View Album", onClick = onViewAlbum)
            OptionItem(icon = Icons.Outlined.Share, title = "Share", onClick = onShare)
            OptionItem(icon = Icons.Outlined.Timer, title = "Sleep Timer", onClick = onSleepTimer)
            OptionItem(icon = Icons.Outlined.Speed, title = "Playback Speed", onClick = onPlaybackSpeed)
            OptionItem(icon = Icons.Outlined.Equalizer, title = "Equalizer", onClick = onEqualizer)
            
            // Like/Unlike option
            song?.let {
                val isLiked = playerViewModel.uiState.value.isLiked
                OptionItem(
                    icon = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    title = if (isLiked) "Remove from Liked" else "Add to Liked",
                    onClick = { 
                        playerViewModel.toggleLike()
                        onDismiss()
                    }
                )
            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddToPlaylistBottomSheet(
    playerViewModel: PlayerViewModel,
    currentSong: Song?,
    onDismiss: () -> Unit,
    onCreatePlaylist: () -> Unit
) {
    val playlists = playerViewModel.uiState.value.userPlaylists
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceColor,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Add to Playlist",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onCreatePlaylist) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create Playlist",
                        tint = ProgressActiveColor
                    )
                }
            }
            
            HorizontalDivider(color = CardColor)
            
            // Playlists List
            if (playlists.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PlaylistPlay,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No playlists yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onCreatePlaylist,
                            colors = ButtonDefaults.buttonColors(containerColor = ProgressActiveColor)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create Playlist")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(playlists) { playlist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentSong?.let { song ->
                                        playerViewModel.addToPlaylist(playlist.id)
                                    }
                                    onDismiss()
                                }
                                .padding(horizontal = 20.dp, vertical = 16.dp),
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
                            
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        }
                    }
                }
            }
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
                        focusedBorderColor = ProgressActiveColor,
                        cursorColor = ProgressActiveColor
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ProgressActiveColor,
                        cursorColor = ProgressActiveColor
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onCreate(name, description.takeIf { it.isNotBlank() }) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ProgressActiveColor)
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

@Composable
private fun SongInfoDialog(
    song: Song,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Song Info", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                InfoRow("Title", song.title)
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow("Artist", song.artist)
                if (song.album.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow("Album", song.album)
                }
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow("Duration", song.formattedDuration())
                if (song.source.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow("Source", song.source)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ProgressActiveColor)
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun SleepTimerDialog(
    onDismiss: () -> Unit,
    onSetTimer: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep Timer", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                listOf(
                    5 to "5 minutes",
                    10 to "10 minutes",
                    15 to "15 minutes",
                    30 to "30 minutes",
                    45 to "45 minutes",
                    60 to "1 hour"
                ).forEach { (minutes, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSetTimer(minutes) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Timer,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun PlaybackSpeedDialog(
    onDismiss: () -> Unit,
    onSetSpeed: (Float) -> Unit
) {
    var selectedSpeed by remember { mutableStateOf(1.0f) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Playback Speed", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                selectedSpeed = speed
                                onSetSpeed(speed)
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedSpeed == speed,
                            onClick = { 
                                selectedSpeed = speed
                                onSetSpeed(speed)
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = ProgressActiveColor
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${speed}x",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary,
                            fontWeight = if (speed == 1.0f) FontWeight.Bold else FontWeight.Normal
                        )
                        if (speed == 1.0f) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "(Normal)",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f).padding(start = 16.dp)
        )
    }
}
