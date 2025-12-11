/*
 * REON Music App - Enhanced Power Search Screen
 * Copyright (c) 2024 REON
 * Modern, Clean Design with Red Palette Light Theme
 * Features: Multi-language search, album/movie name display, unlimited mode
 */

package com.reon.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.reon.music.core.model.Album
import com.reon.music.core.model.Artist
import com.reon.music.core.model.Song
import com.reon.music.ui.components.SongOptionsSheet
import com.reon.music.services.DownloadStatus
import com.reon.music.ui.viewmodels.PlayerViewModel
import com.reon.music.ui.viewmodels.SearchFilter
import com.reon.music.ui.viewmodels.SearchViewModel

// SimpMusic Color Palette
private val BackgroundWhite = Color(0xFFFFFFFF)
private val SurfaceLight = Color(0xFFFAFAFA)
private val SearchBarBg = Color(0xFFF5F5F5)
private val TextPrimary = Color(0xFF1C1C1C)
private val TextSecondary = Color(0xFF757575)
private val AccentRed = Color(0xFFE53935)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    searchViewModel: SearchViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel,
    onAlbumClick: (Album) -> Unit = {},
    onArtistClick: (Artist) -> Unit = {},
    onPlaylistClick: (com.reon.music.core.model.Playlist) -> Unit = {}
) {
    val uiState by searchViewModel.uiState.collectAsState()
    val playerState by playerViewModel.playerState.collectAsState()
    val downloadProgress by playerViewModel.downloadProgress.collectAsState()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showSongOptions by remember { mutableStateOf<Song?>(null) }
    
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            searchViewModel.clearError()
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundWhite
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Rounded Search Bar
            SimpMusicSearchBar(
                query = uiState.query,
                onQueryChange = { searchViewModel.updateQuery(it) },
                onClear = { searchViewModel.clearSearch() },
                onSearch = {
                    focusManager.clearFocus()
                    if (uiState.query.isNotBlank()) {
                        searchViewModel.search(uiState.query)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            )
            
            // Content
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = AccentRed)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Searching across all languages...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                    
                    !uiState.hasSearched && uiState.searchHistory.isEmpty() && uiState.query.isBlank() -> {
                        EmptySearchState()
                    }
                    
                    !uiState.hasSearched && uiState.searchHistory.isNotEmpty() && uiState.query.isBlank() -> {
                        SearchHistorySection(
                            history = uiState.searchHistory,
                            onHistoryItemClick = { searchViewModel.search(it) },
                            onClearAll = { searchViewModel.clearHistory() }
                        )
                    }
                    
                    uiState.songs.isEmpty() && uiState.albums.isEmpty() && uiState.artists.isEmpty() && uiState.query.isNotBlank() && !uiState.isLoading -> {
                        NoResultsState(query = uiState.query)
                    }
                    
                    else -> {
                        SearchResultsSection(
                            filter = uiState.activeFilter,
                            songs = uiState.songs,
                            albums = uiState.albums,
                            artists = uiState.artists,
                            currentSong = playerState.currentSong,
                            isPlaying = playerState.isPlaying,
                            isUnlimitedMode = uiState.isUnlimitedMode,
                            isLoadingMore = uiState.isLoadingMore,
                            hasMore = uiState.hasMore,
                            onSongClick = { song -> playerViewModel.playSong(song) },
                            onAlbumClick = onAlbumClick,
                            onArtistClick = onArtistClick,
                            onSongOptions = { showSongOptions = it },
                            onFilterChange = { searchViewModel.setFilter(it) },
                            onLoadMore = { searchViewModel.loadMoreResults() },
                            onToggleUnlimited = { searchViewModel.toggleUnlimitedMode() }
                        )
                    }
                }
            }
        }
        
        // Song Options Bottom Sheet
        showSongOptions?.let { song ->
            val progress = downloadProgress[song.id]
            SongOptionsSheet(
                song = song,
                isDownloading = progress?.status == DownloadStatus.QUEUED || progress?.status == DownloadStatus.DOWNLOADING,
                downloadProgress = progress?.progress ?: 0,
                onDismiss = { showSongOptions = null },
                onPlay = { 
                    playerViewModel.playSong(song)
                    showSongOptions = null
                },
                onPlayNext = { 
                    playerViewModel.addToQueue(song, playNext = true)
                    showSongOptions = null 
                },
                onAddToQueue = { 
                    playerViewModel.addToQueue(song)
                    showSongOptions = null 
                },
                onDownload = {
                    playerViewModel.downloadSong(song)
                    showSongOptions = null
                },
                onRemoveDownload = { /* no-op for now */ },
                onAddToPlaylist = { showSongOptions = null },
                onShare = { showSongOptions = null }
            )
        }
    }
}

@Composable
private fun SimpMusicSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    
    OutlinedTextField(
        interactionSource = interactionSource,
        value = query,
        onValueChange = onQueryChange,
        placeholder = { 
            Text(
                text = "Search songs, artists, movies...",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = if (isFocused) AccentRed else TextSecondary
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = TextSecondary
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentRed,
            unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = BackgroundWhite,
            unfocusedContainerColor = SearchBarBg,
            cursorColor = AccentRed,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = { onSearch() }
        ),
        modifier = modifier
    )
}

@Composable
private fun EmptySearchState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = AccentRed.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Search Everything",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Search by song name, artist, movie/album name\nResults include Telugu, Hindi, Tamil, English & more",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun NoResultsState(query: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = TextSecondary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No results for \"$query\"",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Try different keywords or check spelling",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SearchHistorySection(
    history: List<String>,
    onHistoryItemClick: (String) -> Unit,
    onClearAll: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClearAll)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Clear search history",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AccentRed,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        items(history) { query ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onHistoryItemClick(query) }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccessTime,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = query,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.NorthEast,
                    contentDescription = "Search",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun SearchResultsSection(
    filter: SearchFilter,
    songs: List<Song>,
    albums: List<Album>,
    artists: List<Artist>,
    currentSong: Song?,
    isPlaying: Boolean,
    isUnlimitedMode: Boolean,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onSongClick: (Song) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onArtistClick: (Artist) -> Unit,
    onSongOptions: (Song) -> Unit,
    onFilterChange: (SearchFilter) -> Unit,
    onLoadMore: () -> Unit,
    onToggleUnlimited: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Filter Chips
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(SearchFilter.entries.toList()) { filterOption ->
                    FilterChip(
                        selected = filter == filterOption,
                        onClick = { onFilterChange(filterOption) },
                        label = {
                            Text(
                                text = when (filterOption) {
                                    SearchFilter.ALL -> "All"
                                    SearchFilter.SONGS -> "Songs"
                                    SearchFilter.ALBUMS -> "Albums"
                                    SearchFilter.ARTISTS -> "Artists"
                                    SearchFilter.MOVIES -> "Movies"
                                },
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentRed,
                            selectedLabelColor = Color.White,
                            containerColor = SurfaceLight,
                            labelColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }
        
        // Unlimited Mode Toggle
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isUnlimitedMode) AccentRed.copy(alpha = 0.1f) else SurfaceLight)
                    .clickable { onToggleUnlimited() }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AllInclusive,
                        contentDescription = null,
                        tint = if (isUnlimitedMode) AccentRed else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Unlimited Search",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (isUnlimitedMode) AccentRed else TextPrimary
                        )
                        Text(
                            text = "Show ALL results from all languages",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
                Switch(
                    checked = isUnlimitedMode,
                    onCheckedChange = { onToggleUnlimited() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AccentRed,
                        checkedTrackColor = AccentRed.copy(alpha = 0.5f)
                    )
                )
            }
        }
        
        // Songs Section
        if ((filter == SearchFilter.ALL || filter == SearchFilter.SONGS) && songs.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Songs (${songs.size}${if (hasMore) "+" else ""})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = TextPrimary
                    )
                }
            }
            
            // Show all songs when in unlimited mode or songs filter
            val songsToShow = if (filter == SearchFilter.SONGS || isUnlimitedMode) songs else songs.take(10)
            items(songsToShow) { song ->
                SongResultItem(
                    song = song,
                    isCurrentSong = currentSong?.id == song.id,
                    isPlaying = isPlaying && currentSong?.id == song.id,
                    onClick = { onSongClick(song) },
                    onMoreClick = { onSongOptions(song) }
                )
            }
            
            // Load more button
            if (hasMore && (filter == SearchFilter.SONGS || isUnlimitedMode)) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clickable { onLoadMore() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoadingMore) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = AccentRed
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Load More Songs",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AccentRed,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = AccentRed
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Artists Section
        if ((filter == SearchFilter.ALL || filter == SearchFilter.ARTISTS) && artists.isNotEmpty()) {
            item {
                Text(
                    text = "Artists",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
            
            items(if (filter == SearchFilter.ARTISTS) artists else artists.take(5)) { artist ->
                ArtistResultItem(
                    artist = artist,
                    onClick = { onArtistClick(artist) }
                )
            }
        }
        
        // Albums Section
        if ((filter == SearchFilter.ALL || filter == SearchFilter.ALBUMS || filter == SearchFilter.MOVIES) && albums.isNotEmpty()) {
            item {
                Text(
                    text = "Albums / Movies",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
            
            items(if (filter == SearchFilter.ALBUMS || filter == SearchFilter.MOVIES) albums else albums.take(5)) { album ->
                AlbumResultItem(
                    album = album,
                    onClick = { onAlbumClick(album) }
                )
            }
        }
    }
}

@Composable
private fun SongResultItem(
    song: Song,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isCurrentSong) AccentRed.copy(alpha = 0.1f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail with duration badge (YouTube-style)
        Box(
            modifier = Modifier
                .size(120.dp, 68.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = song.getHighQualityArtwork(),
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Duration badge (YouTube-style)
            if (song.duration > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = formatDuration(song.duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Quality badge (HD/4K/320kbps)
            if (song.quality.contains("4K", ignoreCase = true) || 
                song.quality.contains("HD", ignoreCase = true) || 
                song.is320kbps) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(
                            color = AccentRed,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = when {
                            song.quality.contains("4K", ignoreCase = true) -> "4K"
                            song.quality.contains("HD", ignoreCase = true) -> "HD"
                            song.is320kbps -> "HQ"
                            else -> "HD"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontSize = androidx.compose.ui.unit.TextUnit(9f, androidx.compose.ui.unit.TextUnitType.Sp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Playing indicator overlay
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AccentRed.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Playing",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Song info (YouTube-style layout)
        Column(modifier = Modifier.weight(1f)) {
            // Song Title
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isCurrentSong) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = if (isCurrentSong) AccentRed else TextPrimary,
                lineHeight = androidx.compose.ui.unit.TextUnit(18f, androidx.compose.ui.unit.TextUnitType.Sp)
            )
            
            Spacer(modifier = Modifier.height(2.dp))
            
            // Channel/Artist name with subscriber count
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (song.channelName.isNotBlank()) song.channelName else song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                
                // Show subscriber count if available
                if (song.channelSubscriberCount > 0) {
                    Text(
                        text = " • ${formatCount(song.channelSubscriberCount)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
            }
            
            // View count and upload date (YouTube-style)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                if (song.viewCount > 0) {
                    Text(
                        text = formatCount(song.viewCount) + " views",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
                
                if (song.uploadDate.isNotBlank() && song.viewCount > 0) {
                    Text(
                        text = " • ${song.uploadDate}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (song.uploadDate.isNotBlank()) {
                    Text(
                        text = song.uploadDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
            }
            
            // Album/Movie name (if available)
            if (song.album.isNotBlank() || song.movieName.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    val displayName = if (song.movieName.isNotBlank()) song.movieName else song.album
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Show language badge
                    if (song.language.isNotBlank()) {
                        Text(
                            text = " • ${song.language}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary.copy(alpha = 0.7f),
                            maxLines = 1
                        )
                    }
                }
            }
        }
        
        // More options button
        IconButton(onClick = onMoreClick, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// Helper function to format large numbers (YouTube style)
private fun formatCount(count: Long): String {
    return when {
        count >= 1_000_000_000 -> String.format("%.1fB", count / 1_000_000_000.0)
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}

// Helper function to format duration like YouTube (MM:SS)
private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", minutes, secs)
}

@Composable
private fun AlbumResultItem(album: Album, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = album.artworkUrl,
            contentDescription = album.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        
        Spacer(modifier = Modifier.width(14.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = TextPrimary
            )
            Text(
                text = "Album / Movie • ${album.artist}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        IconButton(onClick = {}, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More",
                tint = TextSecondary
            )
        }
    }
}

@Composable
private fun ArtistResultItem(artist: Artist, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = artist.artworkUrl,
            contentDescription = artist.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
        )
        
        Spacer(modifier = Modifier.width(14.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = TextPrimary
            )
            Text(
                text = "Artist",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        
        IconButton(onClick = {}, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More",
                tint = TextSecondary
            )
        }
    }
}

