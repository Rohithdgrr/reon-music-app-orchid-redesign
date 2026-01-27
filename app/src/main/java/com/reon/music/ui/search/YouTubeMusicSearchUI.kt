/*
 * REON Music App - YouTube Music Style Search UI
 * Copyright (c) 2024 REON
 * Modern search interface with crash-proof handling
 */

package com.reon.music.ui.search

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.res.painterResource
// Removed explicit ImageRequest builder; use simple URL/string model with Coil
// import coil.ImageRequest
// import coil.request.CachePolicy
import androidx.hilt.navigation.compose.hiltViewModel
import com.reon.music.ui.viewmodels.SearchViewModel
import com.reon.music.ui.viewmodels.SearchUiState
import com.reon.music.ui.viewmodels.SearchFilter as ViewModelFilter
import com.reon.music.core.model.Song
import com.reon.music.core.model.Artist
import com.reon.music.core.model.Album
import com.reon.music.ui.theme.ReonPrimary
import com.reon.music.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun YouTubeMusicStyleSearchScreen(
    onNavigateToPlayer: (String) -> Unit = {},
    onNavigateToLibrary: () -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            // Header with Search Bar
            SearchHeaderSection(
                query = uiState.query,
                onQueryChange = { viewModel.updateQuery(it) },
                onClear = { 
                    viewModel.updateQuery("")
                    scope.launch {
                        delay(100)
                        keyboardController?.hide()
                    }
                },
                isLoading = uiState.isLoading
            )
            
            // Search Filters
            SearchFiltersSection(
                availableFilters = availableFilters,
                selectedFilters = setOf(uiState.activeFilter.name.lowercase()),
                onFilterToggle = { filter ->
                    // Map UI filter to ViewModel filter
                    val newFilter = when (filter.id) {
                        "songs" -> ViewModelFilter.SONGS
                        "artists" -> ViewModelFilter.ARTISTS
                        "albums" -> ViewModelFilter.ALBUMS
                        "movies" -> ViewModelFilter.MOVIES
                        else -> ViewModelFilter.ALL
                    }
                    // Handle filter logic in ViewModel if needed, or just update UI state
                    // For now, let's just use the query update
                }
            )
            
            // Content Area
            when {
                uiState.error != null -> {
                    ErrorSection(
                        error = uiState.error!!,
                        onRetry = { viewModel.search(uiState.query) }
                    )
                }
                uiState.isLoading && uiState.songs.isEmpty() -> {
                    LoadingSection()
                }
                uiState.songs.isNotEmpty() -> {
                    SearchResultsSection(
                        songs = uiState.songs,
                        artists = uiState.artists,
                        albums = uiState.albums,
                        movies = uiState.movies,
                        onSongClick = { song ->
                            onNavigateToPlayer(song.id)
                        },
                        onArtistClick = { artist ->
                            onNavigateToLibrary()
                        },
                        onAlbumClick = { album ->
                            onNavigateToLibrary()
                        }
                    )
                }
                uiState.suggestions.isNotEmpty() && uiState.query.isNotBlank() -> {
                    SuggestionsSection(
                        suggestions = uiState.suggestions.map { 
                            SearchSuggestion(it, SuggestionType.RECENT) 
                        },
                        onSuggestionClick = { suggestion ->
                            viewModel.search(suggestion.text)
                        }
                    )
                }
                uiState.trendingSearches.isNotEmpty() && uiState.query.isBlank() -> {
                    SuggestionsSection(
                        suggestions = uiState.trendingSearches.map { 
                            SearchSuggestion(it, SuggestionType.TRENDING) 
                        },
                        onSuggestionClick = { suggestion ->
                            viewModel.search(suggestion.text)
                        }
                    )
                }
                uiState.query.isNotBlank() && uiState.songs.isEmpty() && !uiState.isLoading -> {
                    EmptyResultsSection(query = uiState.query)
                }
                else -> {
                    WelcomeSection()
                }
            }
        }
    }
}

@Composable
private fun SearchHeaderSection(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // App Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = "REON Music",
                tint = ReonPrimary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "REON Music",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Search Bar
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Search for songs, artists, albums...",
                        color = TextSecondary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = TextSecondary
                    )
                },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(
                            onClick = onClear,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Clear",
                                tint = TextSecondary
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { /* Handle search action */ }
                ),
                shape = RoundedCornerShape(28.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.Black
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ReonPrimary,
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    cursorColor = ReonPrimary
                )
            )
            
            // Loading indicator
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.CenterEnd)
                        .offset(x = (-40).dp, y = 0.dp),
                    color = ReonPrimary,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

@Composable
private fun SearchFiltersSection(
    availableFilters: List<SearchFilter>,
    selectedFilters: Set<String>,
    onFilterToggle: (SearchFilter) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(availableFilters) { filter ->
FilterChip(
                onClick = { onFilterToggle(filter) },
                label = {
                    Text(
                        text = filter.name,
                        color = if (selectedFilters.contains(filter.id)) Color.White else TextSecondary
                    )
                },
                selected = selectedFilters.contains(filter.id),
                leadingIcon = {
                    Icon(
                        imageVector = filter.icon,
                        contentDescription = filter.name,
                        modifier = Modifier.size(16.dp),
                        tint = if (selectedFilters.contains(filter.id)) Color.White else TextSecondary
                    )
                }
            )
        }
    }
}

@Composable
private fun SearchResultsSection(
    songs: List<Song>,
    artists: List<Artist>,
    albums: List<Album>,
    movies: List<Album>,
    onSongClick: (Song) -> Unit,
    onArtistClick: (Artist) -> Unit,
    onAlbumClick: (Album) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (songs.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Songs",
                    icon = Icons.Filled.MusicNote,
                    count = songs.size
                )
            }
            items(songs) { song ->
                SongResultItem(song = song, onClick = { onSongClick(song) })
            }
        }
        
        if (artists.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Artists",
                    icon = Icons.Filled.Person,
                    count = artists.size
                )
            }
            items(artists) { artist ->
                ArtistResultItem(artist = artist, onClick = { onArtistClick(artist) })
            }
        }
        
        if (albums.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Albums",
                    icon = Icons.Filled.Album,
                    count = albums.size
                )
            }
            items(albums) { album ->
                AlbumResultItem(album = album, onClick = { onAlbumClick(album) })
            }
        }
        
        if (movies.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Movies",
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    count = movies.size
                )
            }
            items(movies) { movie ->
                AlbumResultItem(album = movie, onClick = { onAlbumClick(movie) })
            }
        }
    }
}

@Composable
private fun SuggestionsSection(
    suggestions: List<SearchSuggestion>,
    onSuggestionClick: (SearchSuggestion) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Trending Section
        val trending = suggestions.filter { it.type == SuggestionType.TRENDING }
        if (trending.isNotEmpty()) {
            Text(
                text = "Trending Now",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(trending) { suggestion ->
                    TrendingCard(
                        suggestion = suggestion,
                        onClick = { onSuggestionClick(suggestion) }
                    )
                }
            }
        }
        
        // Recent & Personalized Suggestions
        val other = suggestions.filter { it.type != SuggestionType.TRENDING }
        if (other.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Browse",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(other) { suggestion ->
                    SuggestionItem(
                        suggestion = suggestion,
                        onClick = { onSuggestionClick(suggestion) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector,
    count: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ReonPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
        Text(
            text = "$count",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
HorizontalDivider(
                color = Color(0xFFE0E0E0),
                thickness = 1.dp
            )
}

@Composable
private fun SongResultItem(
    song: Song,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(Color(0xFFF8F9FA))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album Art
        AsyncImage(
            model = song.artworkUrl,
            contentDescription = song.title,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(6.dp))
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Song Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${song.artist} • ${song.album}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // Duration
        if (song.duration > 0) {
            Text(
                text = song.formattedDuration(),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun TrendingCard(
    suggestion: SearchSuggestion,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(ReonPrimary, Color(0xFFFF8A65))
                )
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (suggestion.type) {
                    SuggestionType.TRENDING -> Icons.Filled.TrendingUp
                    else -> Icons.Filled.Search
                },
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = suggestion.text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        
        if (suggestion.metadata != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = suggestion.metadata,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun SuggestionItem(
    suggestion: SearchSuggestion,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(Color(0xFFF8F9FA))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (suggestion.type) {
                SuggestionType.RECENT -> Icons.Filled.History
                SuggestionType.ARTIST -> Icons.Filled.Person
                SuggestionType.PERSONALIZED -> Icons.Filled.Recommend
                else -> Icons.Filled.Search
            },
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = suggestion.text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            if (suggestion.metadata != null) {
                Text(
                    text = suggestion.metadata,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun ArtistResultItem(
    artist: Artist,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(Color(0xFFF8F9FA))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = artist.artworkUrl,
            contentDescription = artist.name,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(28.dp))
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${artist.followerCount} followers",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AlbumResultItem(
    album: Album,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(Color(0xFFF8F9FA))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = album.artworkUrl,
            contentDescription = album.name,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(6.dp))
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = album.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


@Composable
private fun LoadingSection() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = ReonPrimary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Searching...",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun ErrorSection(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.SearchOff,
                contentDescription = "Search Error",
                tint = Color(0xFFE53935),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            FilledTonalButton(
                onClick = onRetry,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = ReonPrimary
                )
            ) {
                Text("Retry", color = Color.White)
            }
        }
    }
}

@Composable
private fun EmptyResultsSection(
    query: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.SearchOff,
                contentDescription = "No Results",
                tint = TextSecondary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No results found",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Try searching for \"$query\" with different keywords",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

@Composable
private fun WelcomeSection() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = "REON Music",
                tint = ReonPrimary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Welcome to REON Music",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Search for your favorite songs, artists, albums, and playlists",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}
