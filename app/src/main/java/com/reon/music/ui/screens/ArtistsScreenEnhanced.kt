/*
 * REON Music App - Modern Artists Screen (Enhanced with Real Data)
 * Copyright (c) 2024 REON
 * Claymorphism Design with Real Artist Images from Top 500 Channels
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
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.reon.music.core.model.Artist
import com.reon.music.core.model.Song
import com.reon.music.data.network.youtube.IndianMusicChannels
import com.reon.music.ui.viewmodels.HomeViewModel
import com.reon.music.ui.viewmodels.PlayerViewModel
import kotlinx.coroutines.launch

// Claymorphism Color Palette
private val ClayBackground = Color(0xFFF5F5F0)
private val ClayCardLight = Color(0xFFFFFFFF)
private val ClayShadowLight = Color(0xFFE8E8E3)
private val ClayShadowDark = Color(0xFFC0C0B8)
private val TextPrimary = Color(0xFF2C2C2C)
private val TextSecondary = Color(0xFF6B6B6B)
private val AccentRed = Color(0xFFE74C3C)
private val AccentBlue = Color(0xFF3498DB)
private val AccentGold = Color(0xFFF39C12)
private val AccentPurple = Color(0xFF9B59B6)

// Artist Categories with real image URLs from Top 500 channels
private val ARTIST_CATEGORIES = listOf(
    Triple("All", 0xFFE74C3C.toInt(), null),
    Triple("Telugu", 0xFFFF6B35.toInt(), "telugu"),
    Triple("Hindi", 0xFF9C27B0.toInt(), "hindi"),
    Triple("Indian", 0xFF4CAF50.toInt(), "indian"),
    Triple("Tamil", 0xFF2196F3.toInt(), "tamil"),
    Triple("English", 0xFF3F51B5.toInt(), "english"),
    Triple("Punjabi", 0xFFFF9800.toInt(), "punjabi"),
    Triple("Malayalam", 0xFF009688.toInt(), "malayalam")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistsScreenEnhanced(
    onNavigateToHome: () -> Unit = {},
    onArtistClick: (Artist) -> Unit = {},
    onSongClick: (Song) -> Unit = {},
    homeViewModel: HomeViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    
    // Filter state
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    // Real artists data with images
    var artistsWithData by remember { mutableStateOf<List<ArtistWithSongs>>(emptyList()) }
    var featuredArtist by remember { mutableStateOf<ArtistWithSongs?>(null) }
    
    // Load real artist data
    LaunchedEffect(selectedCategory) {
        isLoading = true
        scope.launch {
            try {
                // Get artists from Top 500 channels
                val channels = when (selectedCategory) {
                    "Telugu" -> IndianMusicChannels.TOP_100_CHANNELS.filter { 
                        it.name.contains("telugu", ignoreCase = true) ||
                        it.name.contains("devi", ignoreCase = true) ||
                        it.name.contains("thaman", ignoreCase = true) ||
                        it.name.contains("dsp", ignoreCase = true) ||
                        it.name.contains("manisharma", ignoreCase = true)
                    }
                    "Hindi" -> IndianMusicChannels.TOP_100_CHANNELS.filter {
                        it.name.contains("arijit", ignoreCase = true) ||
                        it.name.contains("pritam", ignoreCase = true) ||
                        it.name.contains("sony", ignoreCase = true) ||
                        it.name.contains("t-series", ignoreCase = true)
                    }
                    "Tamil" -> IndianMusicChannels.TOP_100_CHANNELS.filter {
                        it.name.contains("tamil", ignoreCase = true) ||
                        it.name.contains("rahman", ignoreCase = true) ||
                        it.name.contains("anirudh", ignoreCase = true) ||
                        it.name.contains("thaman", ignoreCase = true) ||
                        it.name.contains("yuvan", ignoreCase = true)
                    }
                    "Punjabi" -> IndianMusicChannels.TOP_100_CHANNELS.filter {
                        it.name.contains("punjabi", ignoreCase = true) ||
                        it.name.contains("bhangra", ignoreCase = true)
                    }
                    "Malayalam" -> IndianMusicChannels.TOP_100_CHANNELS.filter {
                        it.name.contains("malayalam", ignoreCase = true)
                    }
                    else -> IndianMusicChannels.TOP_100_CHANNELS.take(50)
                }.take(20)
                
                // Convert channels to artists with real data
                val artistList = channels.map { channel ->
                    ArtistWithSongs(
                        artist = Artist(
                            id = channel.name.hashCode().toString(),
                            name = channel.name,
                            artworkUrl = null, // Will fetch from search
                            followerCount = (channel.subscribers?.toInt() ?: 0).coerceIn(0, 10_000_000),
                            topSongs = emptyList()
                        ),
                        rank = channel.rank
                    )
                }
                
                // Fetch real songs for each artist
                val artistsWithSongs = artistList.mapNotNull { artistWithRank ->
                    try {
                        val songs = homeViewModel.searchSongsForChart(
                            "${artistWithRank.artist.name} best songs",
                            5
                        )
                        if (songs.isNotEmpty()) {
                            val artwork = songs.firstOrNull()?.getHighQualityArtwork()
                                ?: songs.firstOrNull()?.artworkUrl
                            artistWithRank.copy(
                                artist = artistWithRank.artist.copy(
                                    artworkUrl = artwork,
                                    topSongs = songs
                                )
                            )
                        } else null
                    } catch (e: Exception) {
                        null
                    }
                }
                
                artistsWithData = artistsWithSongs
                featuredArtist = artistsWithSongs.firstOrNull()
            } catch (e: Exception) {
                // Keep empty state
            }
            isLoading = false
        }
    }
    
    // Filter artists by search
    val filteredArtists = remember(artistsWithData, searchQuery) {
        if (searchQuery.isBlank()) artistsWithData
        else artistsWithData.filter { it.artist.name.contains(searchQuery, ignoreCase = true) }
    }
    
    Scaffold(
        topBar = {
            ClayTopAppBarEnhanced(
                title = "Artists",
                onSearchQueryChange = { searchQuery = it },
                searchQuery = searchQuery
            )
        },
        containerColor = ClayBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Section with Featured Artist
            item {
                featuredArtist?.let { featured ->
                    EnhancedFeaturedArtistSection(
                        artistWithSongs = featured,
                        onArtistClick = { onArtistClick(featured.artist) },
                        onPlayClick = {
                            featured.artist.topSongs.firstOrNull()?.let { onSongClick(it) }
                        }
                    )
                }
            }
            
            // Category Filter Chips
            item {
                CategoryFilterChipsEnhanced(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it }
                )
            }
            
            // Stats Section with Real Data
            item {
                EnhancedStatsSection(
                    totalArtists = artistsWithData.size,
                    topGenre = selectedCategory.takeIf { it != "All" } ?: "All Genres",
                    monthlyListeners = artistsWithData.sumOf { it.artist.followerCount.toLong() },
                    totalSongs = artistsWithData.sumOf { it.artist.topSongs.size }
                )
            }
            
            // Section Title
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Top ${selectedCategory} Artists",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp
                        ),
                        color = TextPrimary
                    )
                    
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = AccentRed,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
            
            // Artists Grid with Real Thumbnails
            items(filteredArtists.chunked(2)) { rowArtists ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    rowArtists.forEach { artistWithSongs ->
                        EnhancedClayArtistCard(
                            artistWithSongs = artistWithSongs,
                            onClick = { onArtistClick(artistWithSongs.artist) },
                            onPlayClick = {
                                artistWithSongs.artist.topSongs.firstOrNull()?.let { onSongClick(it) }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    if (rowArtists.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            
            // Empty State
            if (!isLoading && filteredArtists.isEmpty()) {
                item {
                    EnhancedEmptyState(
                        message = "No artists found for $selectedCategory"
                    )
                }
            }
        }
    }
}

data class ArtistWithSongs(
    val artist: Artist,
    val rank: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClayTopAppBarEnhanced(
    title: String,
    onSearchQueryChange: (String) -> Unit,
    searchQuery: String
) {
    var isSearching by remember { mutableStateOf(false) }
    
    TopAppBar(
        title = {
            if (!isSearching) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = TextPrimary
                )
            } else {
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search artists...", color = TextSecondary) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = AccentRed,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    }
                )
            }
        },
        actions = {
            IconButton(
                onClick = { 
                    isSearching = !isSearching
                    if (!isSearching) onSearchQueryChange("")
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(ClayCardLight)
                    .shadow(8.dp, CircleShape)
            ) {
                Icon(
                    imageVector = if (isSearching) Icons.Outlined.Close else Icons.Outlined.Search,
                    contentDescription = if (isSearching) "Close" else "Search",
                    tint = TextPrimary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = ClayBackground
        )
    )
}

@Composable
private fun EnhancedFeaturedArtistSection(
    artistWithSongs: ArtistWithSongs,
    onArtistClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    val isTop500 = IndianMusicChannels.isPriorityChannel(artistWithSongs.artist.name)
    val songsCount = artistWithSongs.artist.topSongs.size
    val totalViews = artistWithSongs.artist.topSongs.sumOf { it.viewCount }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .height(320.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(32.dp),
                    ambientColor = ClayShadowDark.copy(alpha = 0.4f),
                    spotColor = ClayShadowDark.copy(alpha = 0.6f)
                ),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = ClayCardLight)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Real Background Image
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(artistWithSongs.artist.artworkUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = artistWithSongs.artist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = AccentRed)
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(AccentRed.copy(alpha = 0.3f), AccentBlue.copy(alpha = 0.3f))
                                    )
                                )
                        )
                    }
                )
                
                // Gradient Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.6f),
                                    Color.Black.copy(alpha = 0.9f)
                                )
                            )
                        )
                )
                
                // Content
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(24.dp)
                ) {
                    // Rank Badge
                    Box(
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                            .shadow(8.dp, RoundedCornerShape(20.dp))
                            .background(
                                when {
                                    artistWithSongs.rank <= 20 -> Color(0xFFFFD700)
                                    artistWithSongs.rank <= 50 -> Color(0xFFC0C0C0)
                                    artistWithSongs.rank <= 100 -> Color(0xFFCD7F32)
                                    else -> AccentRed
                                },
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "Rank",
                                tint = if (artistWithSongs.rank <= 50) Color.Black else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "#${artistWithSongs.rank} in Top 500",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (artistWithSongs.rank <= 50) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Text(
                        text = "Featured Artist",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium
                    )
                    
                    Text(
                        text = artistWithSongs.artist.name,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 36.sp
                        ),
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$songsCount songs",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatViewCount(totalViews),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        EnhancedClayButton(
                            onClick = onPlayClick,
                            text = "Play Now",
                            icon = Icons.Default.PlayArrow,
                            containerColor = AccentRed,
                            contentColor = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        
                        EnhancedClayButton(
                            onClick = onArtistClick,
                            text = "View Profile",
                            icon = Icons.Default.Person,
                            containerColor = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterChipsEnhanced(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        items(ARTIST_CATEGORIES) { (category, color, _) ->
            val isSelected = category == selectedCategory
            
            ClayFilterChipEnhanced(
                text = category,
                isSelected = isSelected,
                color = Color(color),
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

@Composable
private fun ClayFilterChipEnhanced(
    text: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
    )
    
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = if (isSelected) color else ClayCardLight,
        shadowElevation = if (isSelected) 12.dp else 6.dp,
        modifier = Modifier
            .height(48.dp)
            .scale(scale)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isSelected) Color.White else TextPrimary
            )
        }
    }
}

@Composable
private fun EnhancedStatsSection(
    totalArtists: Int,
    topGenre: String,
    monthlyListeners: Long,
    totalSongs: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        EnhancedClayStatCard(
            value = totalArtists.toString(),
            label = "Artists",
            icon = Icons.Default.Person,
            color = AccentBlue,
            modifier = Modifier.weight(1f)
        )
        EnhancedClayStatCard(
            value = totalSongs.toString(),
            label = "Songs",
            icon = Icons.Default.MusicNote,
            color = AccentPurple,
            modifier = Modifier.weight(1f)
        )
        EnhancedClayStatCard(
            value = formatStatNumber(monthlyListeners),
            label = "Views",
            icon = Icons.Default.Visibility,
            color = AccentGold,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun EnhancedClayStatCard(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = ClayShadowDark.copy(alpha = 0.3f),
                spotColor = ClayShadowDark.copy(alpha = 0.4f)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = ClayCardLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun EnhancedClayArtistCard(
    artistWithSongs: ArtistWithSongs,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isTop500 = IndianMusicChannels.isPriorityChannel(artistWithSongs.artist.name)
    val songsCount = artistWithSongs.artist.topSongs.size
    val totalViews = artistWithSongs.artist.topSongs.sumOf { it.viewCount }
    
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f)
    )
    
    Card(
        modifier = modifier
            .height(220.dp)
            .scale(scale)
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = if (isTop500) AccentRed.copy(alpha = 0.2f) else ClayShadowDark.copy(alpha = 0.3f),
                spotColor = if (isTop500) AccentRed.copy(alpha = 0.3f) else ClayShadowDark.copy(alpha = 0.4f)
            )
            .clickable {
                isPressed = true
                onClick()
            },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = ClayCardLight)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Image Section with Real Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(artistWithSongs.artist.artworkUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = artistWithSongs.artist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = AccentRed,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            AccentRed.copy(alpha = 0.2f),
                                            AccentBlue.copy(alpha = 0.2f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = TextSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                )
                
                // Gradient Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.4f)
                                )
                            )
                        )
                )
                
                // Rank Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .shadow(6.dp, CircleShape)
                        .background(
                            when {
                                artistWithSongs.rank <= 20 -> Color(0xFFFFD700)
                                artistWithSongs.rank <= 50 -> Color(0xFFC0C0C0)
                                artistWithSongs.rank <= 100 -> Color(0xFFCD7F32)
                                else -> AccentRed
                            },
                            CircleShape
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "#${artistWithSongs.rank}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (artistWithSongs.rank <= 50) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Verified Badge
                if (isTop500) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .shadow(6.dp, CircleShape)
                            .background(AccentRed, CircleShape)
                            .size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                // Play Button
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .shadow(8.dp, CircleShape)
                        .background(AccentRed, CircleShape)
                        .size(44.dp)
                        .clickable { onPlayClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            
            // Info Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = artistWithSongs.artist.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$songsCount songs",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatViewCount(totalViews),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
            }
        }
    }
    
    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(100)
            isPressed = false
        }
    }
}

@Composable
private fun EnhancedClayButton(
    onClick: () -> Unit,
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = containerColor,
        shadowElevation = 10.dp,
        modifier = modifier.height(56.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = contentColor
            )
        }
    }
}

@Composable
private fun EnhancedEmptyState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = TextSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

// Helper Functions
private fun formatViewCount(count: Long): String {
    return when {
        count >= 1_000_000_000 -> "${String.format("%.1f", count / 1_000_000_000.0)}B"
        count >= 1_000_000 -> "${String.format("%.1f", count / 1_000_000.0)}M"
        count >= 1_000 -> "${String.format("%.1f", count / 1_000.0)}K"
        else -> count.toString()
    }
}

private fun formatStatNumber(num: Long): String {
    return when {
        num >= 1_000_000_000 -> "${(num / 1_000_000_000.0).toInt()}B"
        num >= 1_000_000 -> "${(num / 1_000_000.0).toInt()}M"
        num >= 1_000 -> "${(num / 1_000.0).toInt()}K"
        else -> num.toString()
    }
}
