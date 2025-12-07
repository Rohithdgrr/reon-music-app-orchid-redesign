/*
 * REON Music App - SimpMusic-Inspired Home Screen
 * Copyright (c) 2024 REON
 * Modern, Clean Design with Red Palette Light Theme
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.reon.music.core.model.Album
import com.reon.music.core.model.Artist
import com.reon.music.core.model.Playlist
import com.reon.music.core.model.Song
import com.reon.music.ui.components.*
import com.reon.music.ui.theme.*
import com.reon.music.ui.viewmodels.ChartSection
import com.reon.music.ui.viewmodels.HomeViewModel
import com.reon.music.ui.viewmodels.PlayerViewModel
import java.util.*

// SimpMusic Color Palette
private val BackgroundWhite = Color(0xFFFFFFFF)
private val SurfaceLight = Color(0xFFFAFAFA)
private val TextPrimary = Color(0xFF1C1C1C)
private val TextSecondary = Color(0xFF757575)
private val AccentRed = Color(0xFFE53935)
private val AccentRedSecondary = Color(0xFFFF6B6B)
private val ChipSelectedBg = Color(0xFFE53935)
private val ChipUnselectedBg = Color(0xFFF1F3F4)

// Category colors for gradient bars
private val GradientColors = listOf(
    Color(0xFFFFB3D9), // Pink
    Color(0xFFFFD54F), // Yellow
    Color(0xFF4DD0E1), // Cyan
    Color(0xFF81C784), // Green
    Color(0xFFBA68C8), // Purple
    Color(0xFFFF9800)  // Orange
)

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
    onChartClick: (String, String) -> Unit = { _, _ -> },
    onSettingsClick: () -> Unit = {}
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val scrollState = rememberLazyListState()
    val pullToRefreshState = rememberPullToRefreshState()
    
    // Category filter state
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Relax", "Sleep", "Energize", "Focus", "Workout")
    
    LaunchedEffect(pullToRefreshState.isRefreshing) {
        if (pullToRefreshState.isRefreshing) {
            homeViewModel.refresh()
            pullToRefreshState.endRefresh()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
            .nestedScroll(pullToRefreshState.nestedScrollConnection)
    ) {
        if (uiState.isLoading && !pullToRefreshState.isRefreshing) {
            // Skeleton loaders
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                item { Spacer(modifier = Modifier.height(16.dp)) }
                items(5) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceLight)
                    )
                }
            }
        } else {
            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                // Modern Header
                item {
                    SimpMusicHeader(
                        greeting = getGreeting(),
                        onNotificationsClick = { /* TODO */ },
                        onHistoryClick = { /* TODO */ },
                        onSettingsClick = onSettingsClick
                    )
                }
                
                // Category Chips
                item {
                    CategoryChipsRow(
                        categories = categories,
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it }
                    )
                }
                
                // Welcome Back Card with Radio
                item {
                    WelcomeRadioCard(
                        onRadioClick = {
                            // Enable radio mode with quick picks
                            if (uiState.quickPicksSongs.isNotEmpty()) {
                                playerViewModel.enableRadioMode(uiState.quickPicksSongs)
                            }
                        }
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
                
                // Featured Banner (if available)
                if (uiState.featuredPlaylists.isNotEmpty()) {
                    item {
                        FeaturedBannerSection(
                            playlists = uiState.featuredPlaylists,
                            onPlaylistClick = onPlaylistClick
                        )
                    }
                }
                
                // Trending Charts
                if (uiState.charts.isNotEmpty()) {
                    item {
                        TrendingChartsSection(
                            charts = uiState.charts,
                            onChartClick = { chart ->
                                onChartClick(chart.id, chart.title)
                            }
                        )
                    }
                }
                
                // ===== LANGUAGE SECTIONS =====
                
                // Telugu Songs
                if (uiState.teluguSongs.isNotEmpty()) {
                    item {
                        LanguageSection(
                            title = "🎬 Telugu Hits",
                            songs = uiState.teluguSongs,
                            onSongClick = onSongClick,
                            onSeeAllClick = { onSeeAllClick("telugu") }
                        )
                    }
                }
                
                // Hindi Songs
                if (uiState.hindiSongs.isNotEmpty()) {
                    item {
                        LanguageSection(
                            title = "🇮🇳 Hindi Hits",
                            songs = uiState.hindiSongs,
                            onSongClick = onSongClick,
                            onSeeAllClick = { onSeeAllClick("hindi") }
                        )
                    }
                }
                
                // Tamil Songs
                if (uiState.tamilSongs.isNotEmpty()) {
                    item {
                        LanguageSection(
                            title = "🎵 Tamil Hits",
                            songs = uiState.tamilSongs,
                            onSongClick = onSongClick,
                            onSeeAllClick = { onSeeAllClick("tamil") }
                        )
                    }
                }
                
                // Punjabi Songs
                if (uiState.punjabiSongs.isNotEmpty()) {
                    item {
                        LanguageSection(
                            title = "🎧 Punjabi Hits",
                            songs = uiState.punjabiSongs,
                            onSongClick = onSongClick,
                            onSeeAllClick = { onSeeAllClick("punjabi") }
                        )
                    }
                }
                
                // ST Banjara Songs
                if (uiState.banjaraSongs.isNotEmpty()) {
                    item {
                        LanguageSection(
                            title = "🪘 ST Banjara/Lambadi",
                            songs = uiState.banjaraSongs,
                            onSongClick = onSongClick,
                            onSeeAllClick = { onSeeAllClick("banjara") }
                        )
                    }
                }
                
                // ===== INTERNATIONAL SECTION =====
                
                // International Hits
                if (uiState.internationalHits.isNotEmpty()) {
                    item {
                        LanguageSection(
                            title = "🌍 International Hits",
                            songs = uiState.internationalHits,
                            onSongClick = onSongClick,
                            onSeeAllClick = { onSeeAllClick("international") }
                        )
                    }
                }
                
                // English Songs
                if (uiState.englishSongs.isNotEmpty()) {
                    item {
                        LanguageSection(
                            title = "🎤 English Songs",
                            songs = uiState.englishSongs,
                            onSongClick = onSongClick,
                            onSeeAllClick = { onSeeAllClick("english") }
                        )
                    }
                }
                
                // ===== CURATED COLLECTIONS =====
                
                // All Time Favorites
                if (uiState.allTimeFavorites.isNotEmpty()) {
                    item {
                        ContentSection(
                            title = "❤️ All Time Favorites",
                            songs = uiState.allTimeFavorites,
                            onSongClick = onSongClick,
                            onSeeAllClick = { onSeeAllClick("alltimefavorite") }
                        )
                    }
                }
                
                // Most Listening Songs
                if (uiState.mostListeningSongs.isNotEmpty()) {
                    item {
                        ContentSection(
                            title = "🔥 Most Listening",
                            songs = uiState.mostListeningSongs,
                            onSongClick = onSongClick,
                            onSeeAllClick = { onSeeAllClick("mostlistening") }
                        )
                    }
                }
                
                // Trending Now
                if (uiState.trendingNowSongs.isNotEmpty()) {
                    item {
                        ContentSection(
                            title = "📈 Trending Now",
                            songs = uiState.trendingNowSongs,
                            onSongClick = onSongClick,
                            onSeeAllClick = { onSeeAllClick("trending") }
                        )
                    }
                }
                
                // ===== TOP ARTISTS =====
                
                if (uiState.topArtists.isNotEmpty()) {
                    item {
                        TopArtistsSection(
                            artists = uiState.topArtists,
                            onArtistClick = onArtistClick,
                            onSeeAllClick = { onSeeAllClick("artists") }
                        )
                    }
                }
                
                // ===== GENRE SECTIONS =====
                
                // Romantic Songs
                if (uiState.romanticSongs.isNotEmpty()) {
                    item {
                        ContentSection(
                            title = "💕 Romantic Songs",
                            songs = uiState.romanticSongs,
                            onSongClick = onSongClick,
                            onSeeAllClick = { onSeeAllClick("romantic") }
                        )
                    }
                }
                
                // Party Songs
                if (uiState.partySongs.isNotEmpty()) {
                    item {
                        ContentSection(
                            title = "🎉 Party Hits",
                            songs = uiState.partySongs,
                            onSongClick = onSongClick,
                            onSeeAllClick = { onSeeAllClick("party") }
                        )
                    }
                }
                
                // Sad Songs
                if (uiState.sadSongs.isNotEmpty()) {
                    item {
                        ContentSection(
                            title = "😢 Sad Songs",
                            songs = uiState.sadSongs,
                            onSongClick = onSongClick,
                            onSeeAllClick = { onSeeAllClick("sad") }
                        )
                    }
                }
                
                // LoFi Songs
                if (uiState.lofiSongs.isNotEmpty()) {
                    item {
                        ContentSection(
                            title = "🌙 Lo-Fi & Chill",
                            songs = uiState.lofiSongs,
                            onSongClick = onSongClick,
                            onSeeAllClick = { onSeeAllClick("lofi") }
                        )
                    }
                }
                
                // Devotional Songs
                if (uiState.devotionalSongs.isNotEmpty()) {
                    item {
                        ContentSection(
                            title = "🙏 Devotional",
                            songs = uiState.devotionalSongs,
                            onSongClick = onSongClick,
                            onSeeAllClick = { onSeeAllClick("devotional") }
                        )
                    }
                }
                
                // ===== ARTIST SPOTLIGHTS =====
                
                // Arijit Singh
                if (uiState.arijitSinghSongs.isNotEmpty()) {
                    item {
                        ContentSection(
                            title = "🎤 Arijit Singh",
                            songs = uiState.arijitSinghSongs,
                            onSongClick = onSongClick,
                            onSeeAllClick = { onSeeAllClick("arijitsingh") }
                        )
                    }
                }
                
                // AR Rahman
                if (uiState.arRahmanSongs.isNotEmpty()) {
                    item {
                        ContentSection(
                            title = "🎹 A.R. Rahman",
                            songs = uiState.arRahmanSongs,
                            onSongClick = onSongClick,
                            onSeeAllClick = { onSeeAllClick("arrahman") }
                        )
                    }
                }
                
                // SPB
                if (uiState.spbSongs.isNotEmpty()) {
                    item {
                        ContentSection(
                            title = "🎶 S.P. Balasubrahmanyam",
                            songs = uiState.spbSongs,
                            onSongClick = onSongClick,
                            onSeeAllClick = { onSeeAllClick("spb") }
                        )
                    }
                }
                
                // DSP
                if (uiState.dspSongs.isNotEmpty()) {
                    item {
                        ContentSection(
                            title = "🎧 Devi Sri Prasad",
                            songs = uiState.dspSongs,
                            onSongClick = onSongClick,
                            onSeeAllClick = { onSeeAllClick("dsp") }
                        )
                    }
                }
                
                // Thaman
                if (uiState.thamanSongs.isNotEmpty()) {
                    item {
                        ContentSection(
                            title = "🔊 Thaman S",
                            songs = uiState.thamanSongs,
                            onSongClick = onSongClick,
                            onSeeAllClick = { onSeeAllClick("thaman") }
                        )
                    }
                }
                
                // ===== NEW RELEASES =====
                
                if (uiState.newReleases.isNotEmpty()) {
                    item {
                        ContentSection(
                            title = "🆕 New Releases",
                            songs = uiState.newReleases,
                            onSongClick = onSongClick,
                            onSeeAllClick = { onSeeAllClick("new") }
                        )
                    }
                }
                
                // ===== PLAYLISTS =====
                
                // Featured Playlists
                if (uiState.featuredPlaylists.isNotEmpty()) {
                    item {
                        PlaylistSection(
                            title = "📀 Featured Playlists",
                            playlists = uiState.featuredPlaylists,
                            onPlaylistClick = onPlaylistClick,
                            onSeeAllClick = { onSeeAllClick("featured") }
                        )
                    }
                }
                
                // Telugu Playlists
                if (uiState.teluguPlaylistCollection.isNotEmpty()) {
                    item {
                        PlaylistSection(
                            title = "🎬 Telugu Playlists",
                            playlists = uiState.teluguPlaylistCollection,
                            onPlaylistClick = onPlaylistClick,
                            onSeeAllClick = { onSeeAllClick("teluguplaylists") }
                        )
                    }
                }
                
                // Hindi Playlists
                if (uiState.hindiPlaylistCollection.isNotEmpty()) {
                    item {
                        PlaylistSection(
                            title = "🇮🇳 Hindi Playlists",
                            playlists = uiState.hindiPlaylistCollection,
                            onPlaylistClick = onPlaylistClick,
                            onSeeAllClick = { onSeeAllClick("hindiplaylists") }
                        )
                    }
                }
                
                // Tamil Playlists
                if (uiState.tamilPlaylistCollection.isNotEmpty()) {
                    item {
                        PlaylistSection(
                            title = "🎵 Tamil Playlists",
                            playlists = uiState.tamilPlaylistCollection,
                            onPlaylistClick = onPlaylistClick,
                            onSeeAllClick = { onSeeAllClick("tamilplaylists") }
                        )
                    }
                }
                
                // ===== MORE REGIONAL SONGS =====
                
                // Kannada
                if (uiState.kannadaSongs.isNotEmpty()) {
                    item {
                        LanguageSection(
                            title = "🏔️ Kannada Songs",
                            songs = uiState.kannadaSongs,
                            onSongClick = onSongClick,
                            onSeeAllClick = { onSeeAllClick("kannada") }
                        )
                    }
                }
                
                // Malayalam
                if (uiState.malayalamSongs.isNotEmpty()) {
                    item {
                        LanguageSection(
                            title = "🌴 Malayalam Songs",
                            songs = uiState.malayalamSongs,
                            onSongClick = onSongClick,
                            onSeeAllClick = { onSeeAllClick("malayalam") }
                        )
                    }
                }
                
                // Marathi
                if (uiState.marathiSongs.isNotEmpty()) {
                    item {
                        LanguageSection(
                            title = "🏯 Marathi Songs",
                            songs = uiState.marathiSongs,
                            onSongClick = onSongClick,
                            onSeeAllClick = { onSeeAllClick("marathi") }
                        )
                    }
                }
                
                // Bengali
                if (uiState.bengaliSongs.isNotEmpty()) {
                    item {
                        LanguageSection(
                            title = "🎭 Bengali Songs",
                            songs = uiState.bengaliSongs,
                            onSongClick = onSongClick,
                            onSeeAllClick = { onSeeAllClick("bengali") }
                        )
                    }
                }
                
                // Bhojpuri
                if (uiState.bhojpuriSongs.isNotEmpty()) {
                    item {
                        LanguageSection(
                            title = "🪘 Bhojpuri Songs",
                            songs = uiState.bhojpuriSongs,
                            onSongClick = onSongClick,
                            onSeeAllClick = { onSeeAllClick("bhojpuri") }
                        )
                    }
                }
                
                // Gujarati
                if (uiState.gujaratiSongs.isNotEmpty()) {
                    item {
                        LanguageSection(
                            title = "🦚 Gujarati Songs",
                            songs = uiState.gujaratiSongs,
                            onSongClick = onSongClick,
                            onSeeAllClick = { onSeeAllClick("gujarati") }
                        )
                    }
                }
                
                // Rajasthani
                if (uiState.rajasthaniSongs.isNotEmpty()) {
                    item {
                        LanguageSection(
                            title = "🏜️ Rajasthani Folk",
                            songs = uiState.rajasthaniSongs,
                            onSongClick = onSongClick,
                            onSeeAllClick = { onSeeAllClick("rajasthani") }
                        )
                    }
                }
            }
        }
        
        // Pull to refresh indicator
        PullToRefreshContainer(
            state = pullToRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun SimpMusicHeader(
    greeting: String,
    onNotificationsClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit
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
                    text = "REON MUSIC",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = TextPrimary
                )
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onNotificationsClick) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Notifications",
                        tint = TextPrimary
                    )
                }
                IconButton(onClick = onHistoryClick) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = "Recent Activity",
                        tint = TextPrimary
                    )
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryChipsRow(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            FilterChip(
                selected = category == selectedCategory,
                onClick = { onCategorySelected(category) },
                label = {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (category == selectedCategory) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ChipSelectedBg,
                    selectedLabelColor = Color.White,
                    containerColor = ChipUnselectedBg,
                    labelColor = TextPrimary
                ),
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
private fun WelcomeRadioCard(
    onRadioClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clickable(onClick = onRadioClick)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceLight
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Welcome back,",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "MUSIC TO GET YOU STARTED",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = TextPrimary
                )
            }
            
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(AccentRed, AccentRedSecondary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Radio,
                    contentDescription = "Radio",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickPicksSection(
    songs: List<Song>,
    onSongClick: (Song) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quick picks",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = TextPrimary
            )
        }
        
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(songs.take(10)) { song ->
                QuickPickItem(
                    song = song,
                    gradientColor = GradientColors[songs.indexOf(song) % GradientColors.size],
                    onClick = { onSongClick(song) }
                )
            }
        }
    }
}

@Composable
private fun QuickPickItem(
    song: Song,
    gradientColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
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
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Song info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = TextPrimary,
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
                    text = "🎬 ${song.album}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentRed.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Gradient color bar
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(gradientColor)
        )
    }
}

@Composable
private fun FeaturedBannerSection(
    playlists: List<Playlist>,
    onPlaylistClick: (Playlist) -> Unit
) {
    if (playlists.isEmpty()) return
    
    val featured = playlists.firstOrNull() ?: return
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clickable { onPlaylistClick(featured) }
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (featured.artworkUrl != null) {
                AsyncImage(
                    model = featured.artworkUrl,
                    contentDescription = featured.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(AccentRed, AccentRedSecondary)
                            )
                        )
                )
            }
            
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            )
            
            // Content
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp)
            ) {
                Text(
                    text = featured.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${featured.songCount} tracks",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun TrendingChartsSection(
    charts: List<ChartSection>,
    onChartClick: (ChartSection) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Text(
            text = "Trending",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            color = TextPrimary
        )
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(charts.take(2)) { chart ->
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
    Card(
        modifier = Modifier
            .width(280.dp)
            .height(200.dp)
            .clickable(onClick = onClick)
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val thumbnailUrl = chart.songs.firstOrNull()?.getHighQualityArtwork()
            
            if (thumbnailUrl != null) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = chart.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(AccentRed, AccentRedSecondary)
                            )
                        )
                )
            }
            
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f)
                            )
                        )
                    )
            )
            
            // Content
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = chart.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Chart • YouTube Music",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun LanguageSection(
    title: String,
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onSeeAllClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = TextPrimary
            )
            TextButton(onClick = onSeeAllClick) {
                Text(
                    text = "See All",
                    color = AccentRed,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(songs.take(5)) { song ->
                SongCard(
                    song = song,
                    onClick = { onSongClick(song) }
                )
            }
        }
    }
}

@Composable
private fun SongCard(
    song: Song,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = song.getHighQualityArtwork(),
            contentDescription = song.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = song.title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        // Show artist name
        Text(
            text = song.artist,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        // Show album/movie name if available
        if (song.album.isNotBlank()) {
            Text(
                text = "🎬 ${song.album}",
                style = MaterialTheme.typography.labelSmall,
                color = AccentRed.copy(alpha = 0.8f),
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Top artists",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = TextPrimary
            )
            TextButton(onClick = onSeeAllClick) {
                Text(
                    text = "See All",
                    color = AccentRed,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
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
        AsyncImage(
            model = artist.artworkUrl,
            contentDescription = artist.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = artist.name,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium
            ),
            color = TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun ContentSection(
    title: String,
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onSeeAllClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = TextPrimary
            )
            TextButton(onClick = onSeeAllClick) {
                Text(
                    text = "See All",
                    color = AccentRed,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(songs.take(10)) { song ->
                SongCard(
                    song = song,
                    onClick = { onSongClick(song) }
                )
            }
        }
    }
}

@Composable
private fun PlaylistSection(
    title: String,
    playlists: List<Playlist>,
    onPlaylistClick: (Playlist) -> Unit,
    onSeeAllClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = TextPrimary
            )
            TextButton(onClick = onSeeAllClick) {
                Text(
                    text = "See All",
                    color = AccentRed,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
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
            .width(170.dp)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = playlist.artworkUrl,
            contentDescription = playlist.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
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
