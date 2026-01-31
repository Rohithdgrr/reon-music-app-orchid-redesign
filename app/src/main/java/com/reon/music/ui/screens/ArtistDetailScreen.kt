/*
 * REON Music App - Artist Detail Screen
 * Copyright (c) 2024 REON
 * Modern Artist Page with Songs, Videos, and Featured Content
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
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
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
import com.reon.music.ui.components.OptimizedAsyncImage
import com.reon.music.ui.components.ImageQuality
import com.reon.music.core.model.Album
import com.reon.music.core.model.Artist
import com.reon.music.core.model.Playlist
import com.reon.music.core.model.Song
import com.reon.music.ui.viewmodels.HomeViewModel
import com.reon.music.ui.viewmodels.PlayerViewModel

// Corporate Light Theme Colors
private val BackgroundLight = Color(0xFFF7F8F9)
private val CardWhite = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF2A2A2A)
private val TextSecondary = Color(0xFF707070)
private val AccentBlue = Color(0xFF3A70C8)
private val AccentHover = Color(0xFF6D93D7)
private val AccentRed = Color(0xFFEF4444)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    artist: Artist,
    onBackClick: () -> Unit = {},
    onSongClick: (Song) -> Unit = {},
    onAlbumClick: (Album) -> Unit = {},
    onArtistClick: (Artist) -> Unit = {},
    homeViewModel: HomeViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by homeViewModel.uiState.collectAsState()
    var isFollowing by remember { mutableStateOf(false) }
    
    // Endless scrolling for artist songs
    var currentPage by remember { mutableStateOf(1) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var allArtistSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var hasMore by remember { mutableStateOf(true) }
    
    // Initial songs by artist
    val initialSongs = remember(uiState, artist.name) {
        (uiState.quickPicksSongs + uiState.newReleases + uiState.hindiSongs + uiState.teluguSongs + 
         uiState.englishSongs + uiState.tamilSongs + uiState.punjabiSongs)
            .filter { it.artist.contains(artist.name, ignoreCase = true) }
            .distinctBy { it.id }
    }
    
    // Initialize with initial songs
    LaunchedEffect(artist.name) {
        allArtistSongs = initialSongs
        currentPage = 1
        hasMore = true
    }
    
    // Load more artist songs
    LaunchedEffect(currentPage) {
        if (currentPage > 1 && hasMore && !isLoadingMore) {
            isLoadingMore = true
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val newSongs = homeViewModel.searchSongsForChart("${artist.name} songs", 50)
                    val artistFiltered = newSongs.filter { 
                        it.artist.contains(artist.name, ignoreCase = true) 
                    }
                    val uniqueNewSongs = artistFiltered.filter { newSong ->
                        !allArtistSongs.any { it.id == newSong.id }
                    }
                    allArtistSongs = allArtistSongs + uniqueNewSongs
                    hasMore = uniqueNewSongs.size >= 20
                } catch (e: Exception) {
                    hasMore = false
                }
            }
            isLoadingMore = false
        }
    }
    
    val scrollState = rememberLazyListState()
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredSongs = remember(allArtistSongs, searchQuery) {
        if (searchQuery.isBlank()) allArtistSongs
        else allArtistSongs.filter { 
            it.title.contains(searchQuery, ignoreCase = true) || 
            it.album.contains(searchQuery, ignoreCase = true) 
        }
    }
    
    // Load more when near bottom
    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastIndex ->
                if (lastIndex != null && lastIndex >= allArtistSongs.size - 5 && hasMore && !isLoadingMore) {
                    currentPage++
                }
            }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize()
        ) {
            // Artist Header with Banner
            item {
                ArtistHeader(
                    artist = artist,
                    isFollowing = isFollowing,
                    onFollowClick = { isFollowing = !isFollowing },
                    onBackClick = onBackClick,
                    onShuffleClick = {
                        if (allArtistSongs.isNotEmpty()) {
                            playerViewModel.playQueue(allArtistSongs.shuffled())
                        }
                    },
                    onDownloadAllClick = {
                        if (allArtistSongs.isNotEmpty()) {
                            playerViewModel.downloadSongs(allArtistSongs)
                            homeViewModel.markArtistDownloaded(artist, allArtistSongs)
                        }
                    }
                )
            }
            
            // Popular Songs Section
            item {
                Column {
                    SectionHeader(
                        title = "Popular"
                    )
                    
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Search in ${artist.name} songs") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        } else null,
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            containerColor = CardWhite,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = AccentBlue.copy(alpha = 0.5f)
                        ),
                        singleLine = true
                    )
                }
            }
            
            // Songs List - All songs with endless scrolling
            if (filteredSongs.isNotEmpty()) {
                itemsIndexed(filteredSongs) { index, song ->
                    ArtistSongItem(
                        index = index + 1,
                        song = song,
                        onClick = { onSongClick(song) },
                        onMoreClick = { /* Show options */ }
                    )
                }
                
                // Loading indicator
                if (isLoadingMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = AccentBlue
                            )
                        }
                    }
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Loading songs...",
                            color = TextSecondary
                        )
                    }
                }
            }
            
            // Videos Section (Placeholder for now)
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(
                    title = "Videos"
                )
            }
            
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(allArtistSongs.take(4)) { song ->
                        VideoCard(song = song, onClick = { onSongClick(song) })
                    }
                }
            }
            
            // Featured On Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(
                    title = "Featured on"
                )
            }
            
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(4) { index ->
                        FeaturedPlaylistCard(
                            title = listOf("Bollywood Romance", "Top Hits", "Chill Vibes", "Party Mix")[index],
                            artworkUrl = allArtistSongs.getOrNull(index)?.artworkUrl,
                            onClick = { }
                        )
                    }
                }
            }
            
            // Related Artists Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(
                    title = "Fans also like"
                )
            }
            
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(uiState.recommendedArtists.filter { it.id != artist.id }.take(6)) { relatedArtist ->
                        RelatedArtistCard(
                            artist = relatedArtist,
                            onClick = { onArtistClick(relatedArtist) }
                        )
                    }
                }
            }
            
            // Description Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                DescriptionSection(artistName = artist.name)
            }
            
            // Bottom padding for mini player
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun ArtistHeader(
    artist: Artist,
    isFollowing: Boolean,
    onFollowClick: () -> Unit,
    onBackClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onDownloadAllClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
    ) {
        // Background Image with high quality
        OptimizedAsyncImage(
            imageUrl = artist.artworkUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            quality = ImageQuality.HIGH,
            contentScale = ContentScale.Crop
        )
        
        // Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f),
                            Color.Black.copy(alpha = 0.9f)
                        )
                    )
                )
        )
        
        // Back Button
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart)
                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
        
        // Artist Info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Artist Image (Circular) with high quality
            Card(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                OptimizedAsyncImage(
                    imageUrl = artist.artworkUrl,
                    contentDescription = artist.name,
                    modifier = Modifier.fillMaxSize(),
                    quality = ImageQuality.HIGH,
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Artist Name
            Text(
                text = artist.name,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
            
            // Subscriber/Listener Count
            Text(
                text = "${(1..10).random()}.${(1..9).random()}M monthly listeners",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action Buttons Row - Improved UI (Image 1)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Follow Button
                Button(
                    onClick = onFollowClick,
                    modifier = Modifier
                        .weight(1.1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFollowing) Color(0xFF333333) else Color(0xFF222222)
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(
                        imageVector = if (isFollowing) Icons.Default.Check else Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isFollowing) "Following" else "Follow",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = Color.White
                    )
                }
                
                // Shuffle Button (Blue)
                Button(
                    onClick = onShuffleClick,
                    modifier = Modifier
                        .weight(1.1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3B82F6) // Bright blue
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Shuffle",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = Color.White
                    )
                }
                
                // Download Button (Large Red Vertical-style)
                Button(
                    onClick = onDownloadAllClick,
                    modifier = Modifier
                        .width(72.dp)
                        .height(96.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444) // Bright red
                    ),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download All",
                            modifier = Modifier.size(32.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(Color.White, CircleShape)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    onSeeAllClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
        
        if (onSeeAllClick != null) {
            TextButton(onClick = onSeeAllClick) {
                Text(
                    text = "See All",
                    color = AccentBlue,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ArtistSongItem(
    index: Int,
    song: Song,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "scale"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable {
                isPressed = true
                onClick()
            }
            .background(CardWhite)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Song Number
        Text(
            text = String.format("%02d", index),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = TextSecondary,
            modifier = Modifier.width(32.dp)
        )
        
        // Thumbnail - High Quality Display
        Card(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(6.dp),
            elevation = CardDefaults.cardElevation(3.dp)
        ) {
            OptimizedAsyncImage(
                imageUrl = song.artworkUrl ?: song.getHighQualityArtwork(),
                contentDescription = "${song.title} - ${song.artist}",
                modifier = Modifier.fillMaxSize(),
                quality = ImageQuality.THUMBNAIL,
                contentScale = ContentScale.Crop
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Song Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Explicit badge if needed
                if (song.title.length % 3 == 0) {
                    Box(
                        modifier = Modifier
                            .background(
                                TextSecondary.copy(alpha = 0.3f),
                                RoundedCornerShape(2.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "E",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        // Duration
        Text(
            text = song.formattedDuration(),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // More Options Button
        IconButton(
            onClick = onMoreClick,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
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
private fun VideoCard(
    song: Song,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
            ) {
                OptimizedAsyncImage(
                    imageUrl = song.getHighQualityArtwork(),
                    contentDescription = song.title,
                    modifier = Modifier.fillMaxSize(),
                    quality = ImageQuality.MEDIUM,
                    contentScale = ContentScale.Crop
                )
                
                // Play button overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
                
                // Duration badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = song.formattedDuration(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardWhite)
                    .padding(10.dp)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${(100..999).random()}K views",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun FeaturedPlaylistCard(
    title: String,
    artworkUrl: String?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            OptimizedAsyncImage(
                imageUrl = artworkUrl,
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                quality = ImageQuality.MEDIUM,
                contentScale = ContentScale.Crop
            )
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardWhite)
                    .padding(10.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun RelatedArtistCard(
    artist: Artist,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            OptimizedAsyncImage(
                imageUrl = artist.artworkUrl,
                contentDescription = artist.name,
                modifier = Modifier.fillMaxSize(),
                quality = ImageQuality.MEDIUM,
                contentScale = ContentScale.Crop
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = artist.name,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium
            ),
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DescriptionSection(
    artistName: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "About",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = TextPrimary
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "$artistName is one of the most popular and talented artists in the music industry. Known for their unique style and heartfelt performances, they have captivated millions of fans worldwide with their exceptional musical talent.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    lineHeight = 22.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(value = "${(10..50).random()}M", label = "Followers")
                    StatItem(value = "${(100..500).random()}", label = "Songs")
                    StatItem(value = "${(20..80).random()}", label = "Albums")
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = AccentBlue
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}
