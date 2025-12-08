/*
 * REON Music App - SimpMusic-Inspired Library Screen
 * Copyright (c) 2024 REON
 * Modern, Clean Design with Red Palette Light Theme
 */

package com.reon.music.ui.screens

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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.reon.music.core.model.Song
import com.reon.music.data.database.entities.PlaylistEntity
import com.reon.music.ui.viewmodels.LibraryTab
import com.reon.music.ui.viewmodels.LibraryViewModel
import com.reon.music.ui.viewmodels.PlayerViewModel
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import com.reon.music.ui.theme.*
import com.reon.music.ui.navigation.ReonDestination
import androidx.navigation.NavOptions

// SimpMusic Color Palette
private val BackgroundWhite = Color(0xFFFFFFFF)
private val SurfaceLight = Color(0xFFFAFAFA)
private val TextPrimary = Color(0xFF1C1C1C)
private val TextSecondary = Color(0xFF757575)
private val AccentRed = Color(0xFFE53935)

// Category colors from theme
private val CategoryFavorite = Color(0xFFFFB3D9)     // Pink
private val CategoryFollowed = Color(0xFFFFD54F)     // Yellow
private val CategoryMostPlayed = Color(0xFF4DD0E1)   // Cyan
private val CategoryDownloaded = Color(0xFF81C784)   // Green

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    navController: NavHostController = rememberNavController()
) {
    val uiState by libraryViewModel.uiState.collectAsState()
    val playerState by playerViewModel.playerState.collectAsState()
    
    val context = LocalContext.current
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Your library", "Your YouTube Playlists", "Made for you")
    
    // Sheet states
    var showSongOptions by remember { mutableStateOf(false) }
    var selectedSong by remember { mutableStateOf<Song?>(null) }
    var showPlaylistOptions by remember { mutableStateOf(false) }
    var selectedPlaylist by remember { mutableStateOf<PlaylistEntity?>(null) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Library",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                actions = {
                    // Radio mode for library
                    if (uiState.recentlyPlayed.isNotEmpty()) {
                        IconButton(onClick = { 
                            playerViewModel.enableRadioMode(uiState.recentlyPlayed)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Radio,
                                contentDescription = "Radio Mode",
                                tint = AccentRed
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundWhite
                )
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { libraryViewModel.showCreatePlaylistDialog() },
                    containerColor = AccentRed,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Playlist")
                }
            }
        },
        containerColor = BackgroundWhite
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Switcher
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
            
            // Content based on selected tab
            when (selectedTab) {
                0 -> LibraryOverviewContent(
                    uiState = uiState,
                    playerState = playerState,
                    onSongClick = { playerViewModel.playSong(it) },
                    onSongMoreClick = { song ->
                        selectedSong = song
                        showSongOptions = true
                    },
                    onFavoriteClick = { 
                        navController.navigate(
                            route = ReonDestination.ChartDetail.createRoute("alltimefavorite", "Favorites"),
                            navOptions = NavOptions.Builder().setLaunchSingleTop(true).build()
                        )
                    },
                    onFollowedClick = { selectedTab = 1 }, // Switch to YouTube Playlists tab (safe, no crash)
                    onMostPlayedClick = { 
                        navController.navigate(
                            route = ReonDestination.ChartDetail.createRoute("mostlistening", "Most Played"),
                            navOptions = NavOptions.Builder().setLaunchSingleTop(true).build()
                        )
                    },
                    onDownloadedClick = { 
                        navController.navigate(
                            route = ReonDestination.Downloads.route,
                            navOptions = NavOptions.Builder().setLaunchSingleTop(true).build()
                        )
                    },
                    onPlaylistClick = { libraryViewModel.selectTab(LibraryTab.PLAYLISTS) }
                )
                1 -> YouTubePlaylistsContent(
                    playlists = uiState.playlists,
                    onPlaylistClick = { /* Navigate to playlist */ },
                    onPlaylistMoreClick = { playlist ->
                        selectedPlaylist = playlist
                        showPlaylistOptions = true
                    }
                )
                2 -> MadeForYouContent(
                    uiState = uiState,
                    onSongClick = { playerViewModel.playSong(it) },
                    onSongMoreClick = { song ->
                        selectedSong = song
                        showSongOptions = true
                    }
                )
            }
        }
        
        // Create playlist dialog
        if (uiState.showCreatePlaylistDialog) {
            CreatePlaylistDialog(
                onDismiss = { libraryViewModel.hideCreatePlaylistDialog() },
                onCreate = { name, desc -> libraryViewModel.createPlaylist(name, desc) }
            )
        }
        
        // Song Options Bottom Sheet
        if (showSongOptions && selectedSong != null) {
            LibrarySongOptionsSheet(
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
                onDownload = { 
                    playerViewModel.downloadSong(selectedSong!!)
                    showSongOptions = false
                },
                onAddToPlaylist = { 
                    showAddToPlaylistDialog = true
                    showSongOptions = false
                },
                onRemoveFromLibrary = { 
                    libraryViewModel.removeSongFromLibrary(selectedSong!!)
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
        
        // Playlist Options Bottom Sheet
        if (showPlaylistOptions && selectedPlaylist != null) {
            PlaylistOptionsSheet(
                playlist = selectedPlaylist!!,
                onDismiss = { showPlaylistOptions = false },
                onPlay = { 
                    // Play all songs in playlist
                    showPlaylistOptions = false
                },
                onShuffle = { 
                    // Shuffle play playlist
                    showPlaylistOptions = false
                },
                onAddToQueue = { 
                    // Add all songs to queue
                    showPlaylistOptions = false
                },
                onDownload = { 
                    // Download playlist
                    showPlaylistOptions = false
                },
                onDelete = { 
                    libraryViewModel.deletePlaylist(selectedPlaylist!!)
                    showPlaylistOptions = false
                },
                onShare = { 
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "Check out this playlist: ${selectedPlaylist!!.title}")
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Playlist"))
                    showPlaylistOptions = false
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
private fun LibraryOverviewContent(
    uiState: com.reon.music.ui.viewmodels.LibraryUiState,
    playerState: com.reon.music.playback.PlayerState,
    onSongClick: (Song) -> Unit,
    onSongMoreClick: (Song) -> Unit,
    onFavoriteClick: () -> Unit,
    onFollowedClick: () -> Unit,
    onMostPlayedClick: () -> Unit,
    onDownloadedClick: () -> Unit,
    onPlaylistClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Category Cards (2x2 Grid)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CategoryCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Favorite,
                    title = "Favorite",
                    backgroundColor = CategoryFavorite,
                    iconColor = Color(0xFFD32F2F),
                    onClick = onFavoriteClick
                )
                CategoryCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.TrendingUp,
                    title = "Followed",
                    backgroundColor = CategoryFollowed,
                    iconColor = Color(0xFFFBC02D),
                    onClick = onFollowedClick
                )
            }
        }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CategoryCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.ShowChart,
                    title = "Most Played",
                    backgroundColor = CategoryMostPlayed,
                    iconColor = Color(0xFF00ACC1),
                    onClick = onMostPlayedClick
                )
                CategoryCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Download,
                    title = "Downloaded",
                    backgroundColor = CategoryDownloaded,
                    iconColor = Color(0xFF43A047),
                    onClick = onDownloadedClick
                )
            }
        }
        
        // Recently Added Section
        if (uiState.recentlyPlayed.isNotEmpty()) {
            item {
                Text(
                    text = "Recently Added",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextPrimary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            itemsIndexed(uiState.recentlyPlayed.take(20)) { index, item ->
                RecentlyAddedItem(
                    item = item,
                    isPlaying = playerState.currentSong?.id == item.id,
                    onClick = { onSongClick(item) },
                    onMoreClick = { onSongMoreClick(item) }
                )
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun CategoryCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    backgroundColor: Color,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun RecentlyAddedItem(
    item: Any,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    when (item) {
        is Song -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isPlaying) AccentRed.copy(alpha = 0.1f) else Color.Transparent)
                    .clickable(onClick = onClick)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = (item as Song).getHighQualityArtwork(),
                    contentDescription = (item as Song).title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = (item as Song).title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = if (isPlaying) AccentRed else TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = (item as Song).artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Show album/movie name if available
                    if ((item as Song).album.isNotBlank()) {
                        Text(
                            text = "Album: ${(item as Song).album}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentRed.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                // NOW FUNCTIONAL
                IconButton(onClick = onMoreClick) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More Options",
                        tint = TextSecondary
                    )
                }
            }
        }
        is PlaylistEntity -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceLight),
                    contentAlignment = Alignment.Center
                ) {
                    if ((item as PlaylistEntity).thumbnailUrl != null) {
                        AsyncImage(
                            model = (item as PlaylistEntity).thumbnailUrl,
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
                        text = (item as PlaylistEntity).title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Playlist • YouTube Music",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                IconButton(onClick = onMoreClick) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More Options",
                        tint = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun YouTubePlaylistsContent(
    playlists: List<PlaylistEntity>,
    onPlaylistClick: (PlaylistEntity) -> Unit,
    onPlaylistMoreClick: (PlaylistEntity) -> Unit
) {
    if (playlists.isEmpty()) {
        EmptyStateView(
            icon = Icons.Outlined.PlaylistPlay,
            title = "No YouTube playlists",
            subtitle = "Your YouTube Music playlists will appear here"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(playlists) { playlist ->
                PlaylistItem(
                    playlist = playlist,
                    onClick = { onPlaylistClick(playlist) },
                    onMoreClick = { onPlaylistMoreClick(playlist) }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun MadeForYouContent(
    uiState: com.reon.music.ui.viewmodels.LibraryUiState,
    onSongClick: (Song) -> Unit,
    onSongMoreClick: (Song) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (uiState.recentlyPlayed.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Outlined.MusicNote,
                    title = "No recommendations yet",
                    subtitle = "Start listening to get personalized recommendations"
                )
            }
        } else {
            itemsIndexed(uiState.recentlyPlayed.take(20)) { _, song ->
                SongListItem(
                    song = song,
                    isPlaying = false,
                    onClick = { onSongClick(song) },
                    onMoreClick = { onSongMoreClick(song) }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun PlaylistItem(
    playlist: PlaylistEntity,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceLight),
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
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        
        // NOW FUNCTIONAL
        IconButton(onClick = onMoreClick) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "More Options",
                tint = TextSecondary
            )
        }
    }
}

@Composable
private fun SongListItem(
    song: Song,
    isPlaying: Boolean,
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
        AsyncImage(
            model = song.getHighQualityArtwork(),
            contentDescription = song.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
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
                    text = "Album: ${song.album}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentRed.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        // NOW FUNCTIONAL
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
private fun EmptyStateView(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = TextSecondary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibrarySongOptionsSheet(
    song: Song,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onDownload: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onRemoveFromLibrary: () -> Unit,
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
            OptionMenuItem(icon = Icons.Outlined.Download, title = "Download", onClick = onDownload)
            OptionMenuItem(icon = Icons.Outlined.PlaylistAdd, title = "Add to Playlist", onClick = onAddToPlaylist)
            
            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = SurfaceLight)
            
            OptionMenuItem(icon = Icons.Default.Share, title = "Share", onClick = onShare)
            OptionMenuItem(
                icon = Icons.Default.RemoveCircleOutline,
                title = "Remove from Library",
                onClick = onRemoveFromLibrary,
                tint = AccentRed
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistOptionsSheet(
    playlist: PlaylistEntity,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onAddToQueue: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
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
            // Playlist header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceLight),
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
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playlist.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = TextPrimary
                    )
                    Text(
                        text = "${playlist.songCount} songs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
            
            HorizontalDivider(color = SurfaceLight)
            
            // Options
            OptionMenuItem(icon = Icons.Default.PlayArrow, title = "Play All", onClick = onPlay)
            OptionMenuItem(icon = Icons.Default.Shuffle, title = "Shuffle Play", onClick = onShuffle)
            OptionMenuItem(icon = Icons.Default.QueueMusic, title = "Add to Queue", onClick = onAddToQueue)
            OptionMenuItem(icon = Icons.Outlined.Download, title = "Download", onClick = onDownload)
            
            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = SurfaceLight)
            
            OptionMenuItem(icon = Icons.Default.Share, title = "Share", onClick = onShare)
            OptionMenuItem(
                icon = Icons.Default.Delete,
                title = "Delete Playlist",
                onClick = onDelete,
                tint = AccentRed
            )
        }
    }
}

@Composable
private fun OptionMenuItem(
    icon: ImageVector,
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
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Create Playlist",
                fontWeight = FontWeight.Bold
            )
        },
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
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(name, description.takeIf { it.isNotBlank() })
                        onDismiss()
                    }
                },
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
        containerColor = Color(0xFFFFFFFF),
        titleContentColor = Color(0xFF1C1C1C),
        textContentColor = Color(0xFF1C1C1C)
    )
}
