/*
 * REON Music App - Enhanced Home Screen
 * Copyright (c) 2024 REON
 * Modern, Minimalistic Design - YouTube Music Backend
 */

package com.reon.music.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.reon.music.core.model.Album
import com.reon.music.core.model.Artist
import com.reon.music.core.model.Playlist
import com.reon.music.core.model.Song
import com.reon.music.ui.viewmodels.ChartSection
import com.reon.music.ui.viewmodels.Genre
import com.reon.music.ui.viewmodels.HomeViewModel
import com.reon.music.ui.viewmodels.PlayerViewModel
import java.util.*

// Modern Color Palette
private val AccentGreen = Color(0xFF1DB954)
private val AccentPurple = Color(0xFF8B5CF6)
private val AccentBlue = Color(0xFF3B82F6)
private val AccentPink = Color(0xFFEC4899)
private val AccentOrange = Color(0xFFF97316)
private val AccentRed = Color(0xFFEF4444)
private val AccentYellow = Color(0xFFF59E0B)
private val AccentTeal = Color(0xFF14B8A6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    onSongClick: (Song) -> Unit = { playerViewModel.playSong(it) },
    onAlbumClick: (Album) -> Unit = {},
    onArtistClick: (Artist) -> Unit = {},
    onPlaylistClick: (Playlist) -> Unit = {},
    onSeeAllClick: (String) -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val scrollState = rememberLazyListState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Header with Greeting
            item {
                ModernHeader(
                    greeting = getGreeting(),
                    onRefresh = { homeViewModel.refresh() },
                    onSettings = onSettingsClick
                )
            }
            
            // Quick Picks Section
            if (uiState.quickPicksSongs.isNotEmpty()) {
                item {
                    QuickPicksSection(
                        songs = uiState.quickPicksSongs,
                        onSongClick = onSongClick
                    )
                }
            }
            
            // ST Banjara / Lambadi Songs - Special Section
            if (uiState.banjaraSongs.isNotEmpty()) {
                item {
                    BanjaraSongsSection(
                        songs = uiState.banjaraSongs,
                        onSongClick = onSongClick,
                        onSeeAllClick = { onSeeAllClick("banjara") }
                    )
                }
            }
            
            // Charts Section - Modern Gradient Cards
            if (uiState.charts.isNotEmpty()) {
                item {
                    ModernChartsSection(
                        charts = uiState.charts,
                        onChartClick = { /* Navigate to chart */ }
                    )
                }
            }
            
            // Genre Selection - Large, Modern Chips
            item {
                ModernGenreSection(
                    genres = uiState.genres,
                    selectedGenre = uiState.selectedGenre,
                    onGenreSelect = { homeViewModel.selectGenre(it) }
                )
            }
            
            // Genre Songs
            if (uiState.genreSongs.isNotEmpty() && uiState.selectedGenre != null) {
                item {
                    GenreSongsSection(
                        genre = uiState.selectedGenre!!,
                        songs = uiState.genreSongs,
                        onSongClick = onSongClick
                    )
                }
            }
            
            // Featured Playlists
            if (uiState.featuredPlaylists.isNotEmpty()) {
                item {
                    ModernPlaylistSection(
                        title = "Featured Playlists",
                        emoji = "🔥",
                        playlists = uiState.featuredPlaylists,
                        onPlaylistClick = onPlaylistClick,
                        onSeeAllClick = { onSeeAllClick("featured") }
                    )
                }
            }
            
            // Mood Playlists
            if (uiState.moodPlaylists.isNotEmpty()) {
                item {
                    ModernPlaylistSection(
                        title = "Mood & Vibes",
                        emoji = "🎭",
                        playlists = uiState.moodPlaylists,
                        onPlaylistClick = onPlaylistClick,
                        onSeeAllClick = { onSeeAllClick("mood") }
                    )
                }
            }
            
            // Top Artists
            if (uiState.recommendedArtists.isNotEmpty()) {
                item {
                    TopArtistsSection(
                        artists = uiState.recommendedArtists,
                        onArtistClick = onArtistClick,
                        onSeeAllClick = { onSeeAllClick("artists") }
                    )
                }
            }
            
            // Telugu Hits
            if (uiState.teluguSongs.isNotEmpty()) {
                item {
                    ModernSongSection(
                        title = "Telugu Hits",
                        emoji = "🎬",
                        subtitle = "Latest from Tollywood",
                        songs = uiState.teluguSongs,
                        onSongClick = onSongClick,
                        onSeeAllClick = { onSeeAllClick("telugu") }
                    )
                }
            }
            
            // New Releases
            if (uiState.newReleases.isNotEmpty()) {
                item {
                    ModernSongSection(
                        title = "New Releases",
                        emoji = "✨",
                        subtitle = "Fresh music for you",
                        songs = uiState.newReleases,
                        onSongClick = onSongClick,
                        onSeeAllClick = { onSeeAllClick("new") }
                    )
                }
            }
            
            // Hindi Hits
            if (uiState.hindiSongs.isNotEmpty()) {
                item {
                    ModernSongSection(
                        title = "Hindi Hits",
                        emoji = "🇮🇳",
                        subtitle = "Bollywood's finest",
                        songs = uiState.hindiSongs,
                        onSongClick = onSongClick,
                        onSeeAllClick = { onSeeAllClick("hindi") }
                    )
                }
            }
            
            // English Hits
            if (uiState.englishSongs.isNotEmpty()) {
                item {
                    ModernSongSection(
                        title = "English Hits",
                        emoji = "🌍",
                        subtitle = "Global chart-toppers",
                        songs = uiState.englishSongs,
                        onSongClick = onSongClick,
                        onSeeAllClick = { onSeeAllClick("english") }
                    )
                }
            }
            
            // Trending Albums
            if (uiState.trendingAlbums.isNotEmpty()) {
                item {
                    ModernAlbumSection(
                        title = "Trending Albums",
                        emoji = "💿",
                        albums = uiState.trendingAlbums,
                        onAlbumClick = onAlbumClick,
                        onSeeAllClick = { onSeeAllClick("albums") }
                    )
                }
            }
        }
        
        // Loading Indicator
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = AccentRed,
                    strokeWidth = 3.dp
                )
            }
        }
    }
}

@Composable
private fun ModernHeader(
    greeting: String,
    onRefresh: () -> Unit,
    onSettings: () -> Unit
) {
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
            Column {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "What do you want to listen to?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Refresh Button
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                
                // Settings Button
                IconButton(
                    onClick = onSettings,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // YouTube Music Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Red.copy(alpha = 0.1f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = null,
                tint = Color.Red,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "YouTube Music",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.Red
            )
        }
    }
}

@Composable
private fun QuickPicksSection(
    songs: List<Song>,
    onSongClick: (Song) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = "Quick Picks",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(songs.take(6)) { song ->
                QuickPickCard(song = song, onClick = { onSongClick(song) })
            }
        }
    }
}

@Composable
private fun QuickPickCard(
    song: Song,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.getHighQualityArtwork(),
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
            )
            
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
        }
    }
}

@Composable
private fun BanjaraSongsSection(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onSeeAllClick: () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        SectionHeader(
            emoji = "🪕",
            title = "ST Banjara Lambadi",
            subtitle = "Traditional tribal music",
            onSeeAllClick = onSeeAllClick
        )
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(songs.take(10)) { song ->
                ModernSongCard(
                    song = song,
                    onClick = { onSongClick(song) },
                    accentColor = AccentOrange
                )
            }
        }
    }
}

@Composable
private fun ModernChartsSection(
    charts: List<ChartSection>,
    onChartClick: (ChartSection) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📊",
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Charts",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(charts) { chart ->
                ChartCard(
                    chart = chart,
                    onClick = { onChartClick(chart) }
                )
            }
        }
    }
}

@Composable
private fun ChartCard(
    chart: ChartSection,
    onClick: () -> Unit
) {
    val gradientColors = when {
        chart.title.contains("Hindi") -> listOf(AccentOrange, AccentRed)
        chart.title.contains("Telugu") -> listOf(AccentPurple, AccentPink)
        chart.title.contains("English") -> listOf(AccentBlue, AccentTeal)
        chart.title.contains("Tamil") -> listOf(AccentPink, AccentPurple)
        else -> listOf(AccentGreen, AccentBlue)
    }
    
    val emoji = when {
        chart.title.contains("Hindi") -> "🇮🇳"
        chart.title.contains("Telugu") -> "🎬"
        chart.title.contains("English") -> "🌍"
        chart.title.contains("Tamil") -> "🎭"
        else -> "📊"
    }
    
    Card(
        modifier = Modifier
            .width(280.dp)
            .height(160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradientColors))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = emoji,
                            fontSize = 28.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = chart.title.replace(Regex("^[^A-Za-z]+"), "").trim(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                        Text(
                            text = "${chart.songs.size} tracks",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Top songs preview
                Row(
                    horizontalArrangement = Arrangement.spacedBy((-12).dp)
                ) {
                    chart.songs.take(4).forEach { song ->
                        AsyncImage(
                            model = song.getHighQualityArtwork(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .border(2.dp, Color.White, CircleShape)
                        )
                    }
                    if (chart.songs.size > 4) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.3f))
                                .border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+${chart.songs.size - 4}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernGenreSection(
    genres: List<Genre>,
    selectedGenre: Genre?,
    onGenreSelect: (Genre) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🎵",
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Browse Genres",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(genres) { genre ->
                GenreChip(
                    genre = genre,
                    isSelected = selectedGenre?.id == genre.id,
                    onClick = { onGenreSelect(genre) }
                )
            }
        }
    }
}

@Composable
private fun GenreChip(
    genre: Genre,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "scale"
    )
    
    val genreColor = Color(genre.color)
    
    Card(
        modifier = Modifier
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Box(
            modifier = Modifier
                .background(
                    if (isSelected) {
                        Brush.linearGradient(
                            listOf(genreColor, genreColor.copy(alpha = 0.7f))
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                )
                .padding(horizontal = 20.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = genre.emoji,
                    fontSize = 20.sp
                )
                Text(
                    text = genre.name,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun GenreSongsSection(
    genre: Genre,
    songs: List<Song>,
    onSongClick: (Song) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = "${genre.emoji} ${genre.name} Songs",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(songs.take(10)) { song ->
                ModernSongCard(
                    song = song,
                    onClick = { onSongClick(song) },
                    accentColor = Color(genre.color)
                )
            }
        }
    }
}

@Composable
private fun ModernSongSection(
    title: String,
    emoji: String,
    subtitle: String,
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onSeeAllClick: () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        SectionHeader(
            emoji = emoji,
            title = title,
            subtitle = subtitle,
            onSeeAllClick = onSeeAllClick
        )
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(songs.take(10)) { song ->
                ModernSongCard(
                    song = song,
                    onClick = { onSongClick(song) }
                )
            }
        }
    }
}

@Composable
private fun ModernSongCard(
    song: Song,
    onClick: () -> Unit,
    accentColor: Color = AccentGreen
) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
        ) {
            AsyncImage(
                model = song.getHighQualityArtwork(),
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Play button overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(40.dp)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        Text(
            text = song.title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        Text(
            text = song.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ModernPlaylistSection(
    title: String,
    emoji: String,
    playlists: List<Playlist>,
    onPlaylistClick: (Playlist) -> Unit,
    onSeeAllClick: () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        SectionHeader(
            emoji = emoji,
            title = title,
            onSeeAllClick = onSeeAllClick
        )
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(playlists.take(8)) { playlist ->
                PlaylistCard(
                    playlist = playlist,
                    onClick = { onPlaylistClick(playlist) }
                )
            }
        }
    }
}

@Composable
private fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
        ) {
            AsyncImage(
                model = playlist.artworkUrl,
                contentDescription = playlist.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f)
                            ),
                            startY = 150f
                        )
                    )
            )
            
            // Song count badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = "${playlist.songCount} songs",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        if (playlist.description.isNotEmpty()) {
            Text(
                text = playlist.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TopArtistsSection(
    artists: List<Artist>,
    onArtistClick: (Artist) -> Unit,
    onSeeAllClick: () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        SectionHeader(
            emoji = "🎤",
            title = "Top Artists",
            subtitle = "Popular artists for you",
            onSeeAllClick = onSeeAllClick
        )
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(artists.take(10)) { artist ->
                ArtistCard(
                    artist = artist,
                    onClick = { onArtistClick(artist) }
                )
            }
        }
    }
}

@Composable
private fun ArtistCard(
    artist: Artist,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
        ) {
            AsyncImage(
                model = artist.artworkUrl,
                contentDescription = artist.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Gradient border effect
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 3.dp,
                        brush = Brush.linearGradient(
                            listOf(AccentPink, AccentPurple)
                        ),
                        shape = CircleShape
                    )
            )
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        Text(
            text = artist.name,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ModernAlbumSection(
    title: String,
    emoji: String,
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit,
    onSeeAllClick: () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        SectionHeader(
            emoji = emoji,
            title = title,
            onSeeAllClick = onSeeAllClick
        )
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(albums.take(8)) { album ->
                AlbumCard(
                    album = album,
                    onClick = { onAlbumClick(album) }
                )
            }
        }
    }
}

@Composable
private fun AlbumCard(
    album: Album,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .shadow(6.dp, RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = album.artworkUrl,
                contentDescription = album.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        Text(
            text = album.name,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        Text(
            text = album.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SectionHeader(
    emoji: String,
    title: String,
    subtitle: String? = null,
    onSeeAllClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = emoji,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        }
        
        if (onSeeAllClick != null) {
            TextButton(onClick = onSeeAllClick) {
                Text(
                    text = "See All",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = AccentRed
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = AccentRed,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun getGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..20 -> "Good Evening"
        else -> "Good Night"
    }
}
