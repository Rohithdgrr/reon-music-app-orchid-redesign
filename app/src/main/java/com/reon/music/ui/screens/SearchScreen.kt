/*
 * REON Music App - Enhanced Search Screen
 * Copyright (c) 2024 REON
 * Modern Light Theme Design with Real-time Search
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.reon.music.core.model.Album
import com.reon.music.core.model.Artist
import com.reon.music.core.model.Song
import com.reon.music.ui.viewmodels.PlayerViewModel
import com.reon.music.ui.viewmodels.SearchFilter
import com.reon.music.ui.viewmodels.SearchViewModel

// Light theme color palette
private val LightBackground = Color(0xFFFAFAFA)
private val SurfaceColor = Color.White
private val CardColor = Color(0xFFF0F0F0)
private val AccentRed = Color(0xFFE53935)
private val AccentGreen = Color(0xFF4CAF50)
private val TextPrimary = Color(0xFF1A1A1A)
private val TextSecondary = Color(0xFF666666)

/**
 * Enhanced Search Screen with light theme
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    searchViewModel: SearchViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel,
    onAlbumClick: (Album) -> Unit = {},
    onArtistClick: (Artist) -> Unit = {}
) {
    val uiState by searchViewModel.uiState.collectAsState()
    val playerState by playerViewModel.playerState.collectAsState()
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
        containerColor = LightBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header
            Text(
                text = "Search",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )
            
            // Search Bar
            OutlinedTextField(
                value = uiState.query,
                onValueChange = { searchViewModel.updateQuery(it) },
                placeholder = { 
                    Text(
                        text = "What do you want to listen to?",
                        color = TextSecondary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextSecondary
                    )
                },
                trailingIcon = {
                    if (uiState.query.isNotEmpty()) {
                        IconButton(onClick = { searchViewModel.clearSearch() }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = TextSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentRed,
                    unfocusedBorderColor = CardColor,
                    focusedContainerColor = SurfaceColor,
                    unfocusedContainerColor = CardColor,
                    cursorColor = AccentRed,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                        searchViewModel.search(uiState.query)
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Content
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = AccentRed)
                        }
                    }
                    
                    !uiState.hasSearched && uiState.searchHistory.isEmpty() -> {
                        EmptySearchState()
                    }
                    
                    !uiState.hasSearched && uiState.searchHistory.isNotEmpty() -> {
                        SearchHistoryList(
                            history = uiState.searchHistory,
                            onHistoryItemClick = { searchViewModel.search(it) },
                            onRemove = { searchViewModel.removeFromHistory(it) },
                            onClearAll = { searchViewModel.clearHistory() }
                        )
                    }
                    
                    uiState.songs.isEmpty() && uiState.albums.isEmpty() && uiState.artists.isEmpty() -> {
                        NoResultsState(query = uiState.query)
                    }
                    
                    else -> {
                        Column {
                            // Filter Chips
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(SearchFilter.entries) { filter ->
                                    val isSelected = uiState.activeFilter == filter
                                    
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { searchViewModel.setFilter(filter) },
                                        label = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                if (isSelected && filter == SearchFilter.ALL) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                Text(
                                                    text = filter.name.lowercase().replaceFirstChar { it.uppercase() },
                                                    fontSize = 14.sp
                                                )
                                            }
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = AccentRed,
                                            selectedLabelColor = Color.White,
                                            containerColor = CardColor,
                                            labelColor = TextPrimary
                                        ),
                                        border = null
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Search Results
                            SearchResultsList(
                                filter = uiState.activeFilter,
                                songs = uiState.songs,
                                albums = uiState.albums,
                                artists = uiState.artists,
                                currentSong = playerState.currentSong,
                                isPlaying = playerState.isPlaying,
                                onSongClick = { song -> playerViewModel.playSong(song) },
                                onAlbumClick = onAlbumClick,
                                onArtistClick = onArtistClick,
                                onSongOptions = { showSongOptions = it }
                            )
                        }
                    }
                }
            }
        }
        
        // Song Options Bottom Sheet
        showSongOptions?.let { song ->
            SongOptionsSheet(
                song = song,
                onDismiss = { showSongOptions = null },
                onPlay = { 
                    playerViewModel.playSong(song)
                    showSongOptions = null
                },
                onPlayNext = { showSongOptions = null },
                onAddToQueue = { showSongOptions = null },
                onDownload = {
                    playerViewModel.downloadSong(song)
                    showSongOptions = null
                },
                onAddToPlaylist = { showSongOptions = null },
                onShare = { showSongOptions = null }
            )
        }
    }
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
            modifier = Modifier.size(80.dp),
            tint = TextSecondary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Search for music",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Find songs, artists, and albums",
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
private fun SearchHistoryList(
    history: List<String>,
    onHistoryItemClick: (String) -> Unit,
    onRemove: (String) -> Unit,
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
                    imageVector = Icons.Outlined.History,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = query,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { onHistoryItemClick(query) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NorthWest,
                        contentDescription = "Use this search",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun SearchResultsList(
    filter: SearchFilter,
    songs: List<Song>,
    albums: List<Album>,
    artists: List<Artist>,
    currentSong: Song?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onArtistClick: (Artist) -> Unit,
    onSongOptions: (Song) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
    ) {
        // Songs
        if ((filter == SearchFilter.ALL || filter == SearchFilter.SONGS) && songs.isNotEmpty()) {
            items(songs) { song ->
                val isCurrentSong = currentSong?.id == song.id
                SongResultItem(
                    song = song,
                    isCurrentSong = isCurrentSong,
                    isPlaying = isPlaying && isCurrentSong,
                    onClick = { onSongClick(song) },
                    onMoreClick = { onSongOptions(song) }
                )
            }
        }
        
        // Albums
        if ((filter == SearchFilter.ALL || filter == SearchFilter.ALBUMS) && albums.isNotEmpty()) {
            items(albums) { album ->
                AlbumResultItem(
                    album = album,
                    onClick = { onAlbumClick(album) }
                )
            }
        }
        
        // Artists
        if ((filter == SearchFilter.ALL || filter == SearchFilter.ARTISTS) && artists.isNotEmpty()) {
            items(artists) { artist ->
                ArtistResultItem(
                    artist = artist,
                    onClick = { onArtistClick(artist) }
                )
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(100.dp))
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
                    imageVector = Icons.Default.MusicNote,
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
                fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isCurrentSong) AccentRed else TextPrimary
            )
            
            Spacer(modifier = Modifier.height(2.dp))
            
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        IconButton(onClick = onMoreClick, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = TextSecondary
            )
        }
    }
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
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(CardColor),
            contentAlignment = Alignment.Center
        ) {
            if (album.artworkUrl != null) {
                AsyncImage(
                    model = album.artworkUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(imageVector = Icons.Default.Album, contentDescription = null, tint = TextSecondary)
            }
        }
        
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
                text = "Album • ${album.artist}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        IconButton(onClick = {}, modifier = Modifier.size(40.dp)) {
            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More", tint = TextSecondary)
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
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(CardColor),
            contentAlignment = Alignment.Center
        ) {
            if (artist.artworkUrl != null) {
                AsyncImage(
                    model = artist.artworkUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = TextSecondary)
            }
        }
        
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
            Text(text = "Artist", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        
        IconButton(onClick = {}, modifier = Modifier.size(40.dp)) {
            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More", tint = TextSecondary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SongOptionsSheet(
    song: Song,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onDownload: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShare: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceColor,
        contentColor = TextPrimary
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(CardColor)
                ) {
                    if (song.artworkUrl != null) {
                        AsyncImage(model = song.artworkUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = song.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = song.artist, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            HorizontalDivider(color = CardColor)
            OptionMenuItem(Icons.Default.PlayArrow, "Play Now", onPlay)
            OptionMenuItem(Icons.Outlined.Queue, "Play Next", onPlayNext)
            OptionMenuItem(Icons.Outlined.QueueMusic, "Add to Queue", onAddToQueue)
            OptionMenuItem(Icons.Outlined.Download, "Download", onDownload)
            OptionMenuItem(Icons.Outlined.PlaylistAdd, "Add to Playlist", onAddToPlaylist)
            OptionMenuItem(Icons.Outlined.Share, "Share", onShare)
        }
    }
}

@Composable
private fun OptionMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = title, tint = TextPrimary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(20.dp))
        Text(text = title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
    }
}
