@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
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
import com.reon.music.ui.viewmodels.LibraryViewModel
import com.reon.music.ui.viewmodels.PlayerViewModel
import com.reon.music.services.DownloadStatus
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

// Helper function to convert angle to gradient offsets
private fun angleToOffset(angle: Float, size: Float = 1000f): Pair<Offset, Offset> {
    val radians = Math.toRadians(angle.toDouble())
    val x = (size / 2) * cos(radians).toFloat()
    val y = (size / 2) * sin(radians).toFloat()
    return Pair(
        Offset(size / 2 - x, size / 2 - y),
        Offset(size / 2 + x, size / 2 + y)
    )
}

@Composable
private fun infoChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    AssistChip(
        onClick = {},
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
        },
        leadingIcon = if (icon != null) {
            { Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(14.dp)) }
        } else null,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = ChipUnselectedBg,
            labelColor = TextSecondary,
            leadingIconContentColor = TextSecondary
        ),
        border = null
    )
}

@Composable
private fun MinimalSeeAllButton(onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        modifier = Modifier.height(28.dp),
        colors = ButtonDefaults.textButtonColors(containerColor = Color.Transparent)
    ) {
        Text(
            text = "See All",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            ),
            color = AccentRed
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = AccentRed,
            modifier = Modifier.size(14.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    val libraryViewModel: LibraryViewModel = hiltViewModel()
    val libraryState by libraryViewModel.uiState.collectAsState()
    val downloadProgressMap by playerViewModel.downloadProgress.collectAsState()
    val scrollState = rememberLazyListState()
    val pullToRefreshState = rememberPullToRefreshState()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val downloadedSongIds = remember(libraryState.downloadedSongs) {
        libraryState.downloadedSongs.map { it.id }.toSet()
    }

    // Aggregate Top Artist Spotlights
    val artistSpotlights = remember(uiState) {
        val spotlights = mutableListOf<Pair<Artist, List<Song>>>()
        
        fun addSpotlight(name: String, songs: List<Song>, fallbackId: String = name) {
            if (songs.isNotEmpty()) {
                val artist = uiState.topArtists.find { it.name.equals(name, true) || it.name.contains(name, true) }
                    ?: Artist(fallbackId, name, songs.firstOrNull()?.getHighQualityArtwork())
                spotlights.add(artist to songs)
            }
        }
        
        addSpotlight("Arijit Singh", uiState.arijitSinghSongs)
        addSpotlight("A.R. Rahman", uiState.arRahmanSongs)
        addSpotlight("Shreya Ghoshal", uiState.shreyaGhoshalSongs)
        addSpotlight("Sid Sriram", uiState.sidSriram)
        addSpotlight("Anirudh Ravichander", uiState.anirudhSongs)
        addSpotlight("Badshah", uiState.badshah)
        addSpotlight("Yo Yo Honey Singh", uiState.honeysingh)
        addSpotlight("Kanika Kapoor", uiState.kanikKapoor)
        addSpotlight("S.P. Balasubrahmanyam", uiState.spbSongs)
        addSpotlight("Pritam", uiState.pritam)
        addSpotlight("Harris Jayaraj", uiState.harishJeyaraj)
        addSpotlight("Devi Sri Prasad", uiState.dspSongs)
        addSpotlight("Thaman S", uiState.thamanSongs)
        
        spotlights
    }
    
    // Category filter state (vertical filters)
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf(
        "All", "Telugu", "Hindi", "Tamil", "Indian", "International",
        "Love", "Sad", "Party", "Happy", "Motivation", "Chill", "Random", "Genre"
    )
    
    // Song options sheet state
    var showSongOptions by remember { mutableStateOf(false) }
    var selectedSong by remember { mutableStateOf<Song?>(null) }
    
    LaunchedEffect(pullToRefreshState.isRefreshing) {
        if (pullToRefreshState.isRefreshing) {
            homeViewModel.refresh()
            pullToRefreshState.endRefresh()
        }
    }

    // Compute most listened list in composable context (outside LazyListScope)
    val mostListened = remember(uiState.mostPlayedSongs, uiState.quickPicksSongs) {
        when {
            uiState.mostPlayedSongs.isNotEmpty() -> uiState.mostPlayedSongs
            uiState.quickPicksSongs.isNotEmpty() -> uiState.quickPicksSongs
            else -> emptyList()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
            .nestedScroll(pullToRefreshState.nestedScrollConnection)
    ) {
        if (uiState.isLoading && !pullToRefreshState.isRefreshing) {
            // Enhanced skeleton loaders with shimmer effect
            HomeScreenSkeleton(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp)
            )
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
                        onSettingsClick = onSettingsClick
                    )
                }
                
                // Removed: History shortcut + vertical filters
                // Now showing content directly without filter section
                
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
                
                // Quick Picks Section (Most listened)
                if (mostListened.isNotEmpty()) {
                    item {
                        QuickPicksSection(
                            songs = mostListened,
                            downloadedSongIds = downloadedSongIds,
                            downloadProgressMap = downloadProgressMap,
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
                
                // Trending Charts (include Telugu & Tamil if present)
                if (uiState.charts.isNotEmpty()) {
                    item {
                        val trendingCharts = remember(uiState.charts) {
                            val telugu = uiState.charts.firstOrNull { it.id.equals("telugu", true) }
                            val tamil = uiState.charts.firstOrNull { it.id.equals("tamil", true) }
                            val base = uiState.charts.take(2)
                            buildList {
                                addAll(base)
                                telugu?.let { if (!contains(it)) add(it) }
                                tamil?.let { if (!contains(it)) add(it) }
                            }
                        }
                        TrendingChartsSection(
                            charts = trendingCharts,
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
                            title = "Telugu Hits",
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
                            title = "Hindi Hits",
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
                            title = "Tamil Hits",
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
                            title = "Punjabi Hits",
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
                            title = "ST Banjara/Lambadi",
                            songs = uiState.banjaraSongs,
                            onSongClick = onSongClick,
                            onSeeAllClick = { onSeeAllClick("banjara") }
                        )
                    }
                }
                
                // Telugu Playlists (YouTube-only)
                if (uiState.teluguPlaylistsYoutube.isNotEmpty()) {
                    item {
                        PlaylistCarouselSection(
                            title = "Telugu Playlists",
                            playlists = uiState.teluguPlaylistsYoutube,
                            onPlaylistClick = onPlaylistClick,
                            onSeeAllClick = { onSeeAllClick("telugu-playlists") }
                        )
                    }
                }
                
                // Telugu Songs (YouTube-only)
                if (uiState.teluguSongsYoutube.isNotEmpty()) {
                    item {
                        LanguageSection(
                            title = "Telugu (YouTube)",
                            songs = uiState.teluguSongsYoutube,
                            onSongClick = onSongClick,
                            onSeeAllClick = { onSeeAllClick("telugu-youtube") }
                        )
                    }
                }
                
                // Indian Playlists (YouTube-only)
                if (uiState.indianPlaylistsYoutube.isNotEmpty()) {
                    item {
                        PlaylistCarouselSection(
                            title = "Indian Playlists",
                            playlists = uiState.indianPlaylistsYoutube,
                            onPlaylistClick = onPlaylistClick,
                            onSeeAllClick = { onSeeAllClick("indian-playlists") }
                        )
                    }
                }
                
                // Indian Songs (YouTube-only)
                if (uiState.indianSongsYoutube.isNotEmpty()) {
                    item {
                        LanguageSection(
                            title = "Indian (YouTube)",
                            songs = uiState.indianSongsYoutube,
                            onSongClick = onSongClick,
                            onSeeAllClick = { onSeeAllClick("indian-youtube") }
                        )
                    }
                }
                
                // ===== INTERNATIONAL SECTION =====
                
                // International Hits
                if (uiState.internationalHits.isNotEmpty()) {
                    item {
                        LanguageSection(
                            title = "International Hits",
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
                            title = "English Songs",
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
                            title = "All Time Favorites",
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
                            title = "Most Listening",
                            songs = uiState.mostListeningSongs,
                            onSongClick = onSongClick,
                            onSeeAllClick = { onSeeAllClick("mostlistening") }
                        )
                    }
                }
                
                // Trending Now (prioritize Banjara ST songs first)
                if (uiState.trendingNowSongs.isNotEmpty()) {
                    item {
                        val trendingSongs = remember(uiState.trendingNowSongs) {
                            uiState.trendingNowSongs.sortedByDescending { song ->
                                song.title.contains("banjara", ignoreCase = true) ||
                                        song.album.contains("banjara", ignoreCase = true)
                            }
                        }
                        ContentSection(
                            title = "Trending Now",
                            songs = trendingSongs,
                            onSongClick = onSongClick,
                            onSeeAllClick = { onSeeAllClick("trending") }
                        )
                    }
                }
                
                // ===== FEATURED ARTIST =====
                
                // ===== TOP ARTISTS SPOTLIGHTS =====
                
                if (artistSpotlights.isNotEmpty()) {
                    item {
                        TopArtistsSpotlightPager(
                            spotlights = artistSpotlights,
                            onSongClick = onSongClick,
                            onArtistClick = onArtistClick,
                            onPlayAll = { songs -> playerViewModel.playQueue(songs) },
                            onShuffle = { songs -> playerViewModel.playQueue(songs.shuffled()) }
                        )
                    }
                }
                
                // ===== GENRE SECTIONS =====
                
                // Romantic Songs
                if (uiState.romanticSongs.isNotEmpty()) {
                    item {
                        ContentSection(
                            title = "Romantic Songs",
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
                            title = "Party Hits",
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
                            title = "Sad Songs",
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
                            title = "Lo-Fi & Chill",
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
                            title = "Devotional",
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
                            title = "Arijit Singh",
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
                            title = "A.R. Rahman",
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
                            title = "S.P. Balasubrahmanyam",
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
                            title = "Devi Sri Prasad",
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
                            title = "Thaman S",
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
                            title = "New Releases",
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
                            title = "Featured Playlists",
                            playlists = uiState.featuredPlaylists,
                            onPlaylistClick = onPlaylistClick,
                            onSeeAllClick = { onSeeAllClick("featured") }
                        )
                    }
                }
                
//                // Hindi Playlists
//                if (uiState.hindiPlaylistCollection.isNotEmpty()) {
//                    item {
//                        PlaylistSection(
//                            title = "Hindi Playlists",
//                            playlists = uiState.hindiPlaylistCollection,
//                            onPlaylistClick = onPlaylistClick,
//                            onSeeAllClick = { onSeeAllClick("hindiplaylists") }
//                        )
//                    }
//                }
                
//                // Tamil Playlists
//                if (uiState.tamilPlaylistCollection.isNotEmpty()) {
//                    item {
//                        PlaylistSection(
//                            title = "Tamil Playlists",
//                            playlists = uiState.tamilPlaylistCollection,
//                            onPlaylistClick = onPlaylistClick,
//                            onSeeAllClick = { onSeeAllClick("tamilplaylists") }
//                        )
//                    }
//                }
                
//                // Telugu Playlists
//                if (uiState.teluguPlaylistCollection.isNotEmpty()) {
//                    item {
//                        PlaylistSection(
//                            title = "Telugu Playlists",
//                            playlists = uiState.teluguPlaylistCollection,
//                            onPlaylistClick = onPlaylistClick,
//                            onSeeAllClick = { onSeeAllClick("teluguplaylists") }
//                        )
//                    }
//                }
                
                // ===== MORE REGIONAL SONGS =====
                
                // Kannada
                if (uiState.kannadaSongs.isNotEmpty()) {
                    item {
                        LanguageSection(
                            title = "Kannada Songs",
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
                            title = "Malayalam Songs",
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
                            title = "Marathi Songs",
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
                            title = "Bengali Songs",
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
                            title = "Bhojpuri Songs",
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
                            title = "Gujarati Songs",
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
                            title = "Rajasthani Folk",
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
    
    // Song Options Bottom Sheet
    if (showSongOptions && selectedSong != null) {
        SongOptionsSheet(
            song = selectedSong!!,
            isDownloaded = false,
            isLiked = false,
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
            onAddToPlaylist = {
                // TODO: Show playlist picker
                showSongOptions = false
            },
            onDownload = {
                // TODO: Start download
                showSongOptions = false
            },
            onShare = {
                val sendIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    putExtra(android.content.Intent.EXTRA_TEXT, 
                        "Listen to ${selectedSong!!.title} by ${selectedSong!!.artist}")
                    type = "text/plain"
                }
                context.startActivity(android.content.Intent.createChooser(sendIntent, "Share Song"))
                showSongOptions = false
            }
        )
    }
}

@Composable
private fun SimpMusicHeader(
    greeting: String,
    onNotificationsClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "REON MUSIC",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceLight)
            ) {
                IconButton(
                    onClick = onNotificationsClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Notifications",
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Divider(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp),
                    color = ChipUnselectedBg
                )
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryListVertical(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    onHistoryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Filters",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            TextButton(onClick = onHistoryClick) {
                Icon(Icons.Default.History, contentDescription = null, tint = AccentRed)
                Spacer(modifier = Modifier.width(6.dp))
                Text("History", color = AccentRed, fontWeight = FontWeight.SemiBold)
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 140.dp, max = 240.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun WelcomeRadioCard(
    onRadioClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "welcomeScale"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .scale(scale)
            .clickable(onClick = onRadioClick)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        isPressed = event.type == androidx.compose.ui.input.pointer.PointerEventType.Press
                    }
                }
            }
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = AccentRed.copy(0.2f),
                spotColor = AccentRed.copy(0.3f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = BackgroundWhite
        ),
        border = BorderStroke(1.5.dp, AccentRed.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Welcome back,",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "MUSIC TO GET YOU STARTED",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    ),
                    color = TextPrimary
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(AccentRed, AccentRedSecondary),
                            start = angleToOffset(45f).first,
                            end = angleToOffset(45f).second
                        )
                    )
                    .shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        ambientColor = AccentRed.copy(0.3f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Radio,
                    contentDescription = "Radio",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickPicksSection(
    songs: List<Song>,
    downloadedSongIds: Set<String>,
    downloadProgressMap: Map<String, com.reon.music.services.DownloadProgress>,
    onSongClick: (Song) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quick Picks · Most Played",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                ),
                color = TextPrimary
            )
            Text(
                text = "Based on your recent listening history",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
        }
        
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(songs.take(10)) { song ->
                val dp = downloadProgressMap[song.id]
                val isDownloading = dp?.status == DownloadStatus.DOWNLOADING || dp?.status == DownloadStatus.QUEUED
                val progressPercent = dp?.progress ?: 0
                val isDownloaded = downloadedSongIds.contains(song.id)
                QuickPickItem(
                    song = song,
                    gradientColor = GradientColors[songs.indexOf(song) % GradientColors.size],
                    isDownloaded = isDownloaded,
                    isDownloading = isDownloading,
                    downloadProgress = progressPercent,
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
    isDownloaded: Boolean = false,
    isDownloading: Boolean = false,
    downloadProgress: Int = 0,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val backgroundColor by animateColorAsState(
        targetValue = if (isPressed) SurfaceLight else BackgroundWhite,
        animationSpec = tween(200),
        label = "bgColor"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(12.dp)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        isPressed = event.type == androidx.compose.ui.input.pointer.PointerEventType.Press
                    }
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album art thumbnail with shadow
        Box(
            modifier = Modifier
                .size(92.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(12.dp),
                    ambientColor = Color.Black.copy(0.1f)
                )
                .clip(RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = song.getHighQualityArtwork(),
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.2f)),
                            start = angleToOffset(45f).first,
                            end = angleToOffset(45f).second
                        )
                    )
            )
        }
        
        Spacer(modifier = Modifier.width(14.dp))
        
        // Song info
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
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Show album/movie name if available
            if (song.album.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🎬 ${song.album}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentRed.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )
            }
            if (song.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = song.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                infoChip(label = song.language.takeIf { it.isNotBlank() } ?: "Unknown Language")
                infoChip(label = song.genre.takeIf { it.isNotBlank() } ?: "${song.type.ifBlank { "Track" }}")
                if (song.channelName.isNotBlank()) {
                    infoChip(label = "Channel · ${song.channelName}")
                }
                if (song.is320kbps) {
                    infoChip(label = "320 kbps")
                } else if (song.quality.isNotBlank()) {
                    infoChip(label = song.quality.uppercase(Locale.US))
                }
                if (isDownloading) {
                    infoChip(label = "${downloadProgress.coerceIn(0, 100)}%")
                } else if (isDownloaded) {
                    infoChip(label = "Offline")
                }
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Gradient color bar with rounded ends
        Box(
            modifier = Modifier
                .width(5.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(2.5.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(gradientColor, gradientColor.copy(alpha = 0.6f))
                    )
                )
                .shadow(elevation = 4.dp)
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
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "bannerScale"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .scale(scale)
            .clickable { onPlaylistClick(featured) }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        isPressed = event.type == androidx.compose.ui.input.pointer.PointerEventType.Press
                    }
                }
            }
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(0.15f)
            ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (featured.artworkUrl != null) {
                AsyncImage(
                    model = featured.artworkUrl,
                    contentDescription = featured.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(AccentRed, AccentRedSecondary),
                                start = angleToOffset(45f).first,
                                end = angleToOffset(45f).second
                            )
                        )
                )
            }
            
            // Enhanced gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.3f),
                                Color.Black.copy(alpha = 0.8f)
                            ),
                            startY = 50f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )
            
            // Content
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
            ) {
                Text(
                    text = featured.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp
                    ),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .width(IntrinsicSize.Max)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${featured.songCount} tracks",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
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
            .padding(vertical = 20.dp)
    ) {
        Text(
            text = "Trending",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp
            ),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            color = TextPrimary
        )
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(charts.distinctBy { it.id }) { chart ->
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
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "chartScale"
    )
    
    Card(
        modifier = Modifier
            .width(300.dp)
            .height(220.dp)
            .scale(scale)
            .clickable(onClick = onClick)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        isPressed = event.type == androidx.compose.ui.input.pointer.PointerEventType.Press
                    }
                }
            }
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(0.1f)
            ),
        shape = RoundedCornerShape(20.dp)
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
                            brush = Brush.linearGradient(
                                colors = listOf(AccentRed, AccentRedSecondary),
                                start = angleToOffset(45f).first,
                                end = angleToOffset(45f).second
                            )
                        )
                )
            }
            
            // Enhanced gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.4f),
                                Color.Black.copy(alpha = 0.8f)
                            ),
                            startY = 100f,
                            endY = Float.POSITIVE_INFINITY
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
                    text = chart.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    ),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                        .width(IntrinsicSize.Max)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Chart",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}


@Composable
private fun SongCard(
    song: Song,
    onClick: () -> Unit,
    onMoreClick: (() -> Unit)? = null,
    isRectangular: Boolean = false
) {
    // Animation state for hover effect
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "cardScale"
    )
    
    Column(
        modifier = Modifier
            .width(if (isRectangular) 220.dp else 190.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            isPressed = event.type == androidx.compose.ui.input.pointer.PointerEventType.Press
                        }
                    }
                }
        ) {
            // Consistent rounded rectangle shape for all cards in a row
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (isRectangular) 16f/9f else 1f)
                    .clip(RoundedCornerShape(20.dp))
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = Color.Black.copy(0.15f),
                        spotColor = Color.Black.copy(0.25f)
                    )
            ) {
                AsyncImage(
                    model = song.getHighQualityArtwork(),
                    contentDescription = song.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Gradient overlay for depth
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.35f)
                                ),
                                radius = 500f
                            )
                        )
                )
            }
            
            // Quality badge (HD/4K/320kbps) - Top Left
            if (song.quality.contains("4K", ignoreCase = true) || 
                song.quality.contains("HD", ignoreCase = true) || 
                song.is320kbps) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(
                            color = AccentRed,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 3.dp)
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
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Duration badge with enhanced styling - Bottom Right
            if (song.duration > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = formatDuration(song.duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            // More options button
            if (onMoreClick != null) {
                IconButton(
                    onClick = onMoreClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More Options",
                        tint = Color.White,
                        modifier = Modifier
                            .background(
                                color = Color.Black.copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                            .padding(5.dp)
                            .size(18.dp)
                    )
                }
            }
            
            // Play indicator icon for visual appeal
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp)
                    .background(
                        color = AccentRed.copy(alpha = 0.8f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Song title
        Text(
            text = song.title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontSize = 13.sp
        )
        
        // Artist/Channel name
        Text(
            text = if (song.channelName.isNotBlank()) song.channelName else song.artist,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 11.sp
        )
        
        // View count (YouTube-style)
        if (song.viewCount > 0) {
            Text(
                text = "${formatCount(song.viewCount)} views",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 10.sp
            )
        }
        
        // Album/Movie name if available
        if (song.album.isNotBlank() || song.movieName.isNotBlank()) {
            val displayName = if (song.movieName.isNotBlank()) song.movieName else song.album
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentRed.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 10.sp,
                    modifier = Modifier.weight(1f, fill = false)
                )
                
                // Language badge
                if (song.language.isNotBlank()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "• ${song.language.take(3).uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary.copy(alpha = 0.7f),
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

// Helper function to format duration
private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", minutes, secs)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TopArtistsSpotlightPager(
    spotlights: List<Pair<Artist, List<Song>>>,
    onSongClick: (Song) -> Unit,
    onArtistClick: (Artist) -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    onShuffle: (List<Song>) -> Unit
) {
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { spotlights.size })
    
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
                text = "Top Artists",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = AccentRed
                )
            )
            
            // Pager Indicator
            if (spotlights.size > 1) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(spotlights.size) { iteration ->
                        val color = if (pagerState.currentPage == iteration) AccentRed else ChipUnselectedBg
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(color)
                                .size(if (pagerState.currentPage == iteration) 8.dp else 6.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 20.dp),
            pageSpacing = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val (artist, songs) = spotlights[page]
            val backgroundArtwork = artist.artworkUrl
                ?: songs.firstOrNull()?.getHighQualityArtwork()
                ?: songs.firstOrNull()?.artworkUrl

            Column(modifier = Modifier.fillMaxWidth()) {
                // Large Artist Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clickable { onArtistClick(artist) },
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (backgroundArtwork != null) {
                            AsyncImage(
                                model = backgroundArtwork,
                                contentDescription = artist.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.linearGradient(
                                            listOf(AccentRed, AccentRedSecondary)
                                        )
                                    )
                            )
                        }

                        // Gradient Overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.35f),
                                            Color.Black.copy(alpha = 0.75f)
                                        )
                                    )
                                )
                        )

                        // Card Content
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(20.dp)
                        ) {
                            Text(
                                text = artist.name,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${songs.size} hand-picked tracks",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                AssistChip(
                                    onClick = { onPlayAll(songs) },
                                    label = {
                                        Text(
                                            text = "Play All",
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = AccentRed.copy(alpha = 0.85f),
                                        labelColor = Color.White,
                                        leadingIconContentColor = Color.White
                                    ),
                                    border = null
                                )

                                AssistChip(
                                    onClick = { onShuffle(songs) },
                                    label = {
                                        Text(
                                            text = "Shuffle",
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Shuffle,
                                            contentDescription = null,
                                            tint = Color.White
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = Color.White.copy(alpha = 0.2f),
                                        labelColor = Color.White,
                                        leadingIconContentColor = Color.White
                                    ),
                                    border = null
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Songs Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(songs.take(8)) { song ->
                        SongCard(
                            song = song,
                            onClick = { onSongClick(song) }
                        )
                    }
                }
            }
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
            .padding(vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Top artists",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                ),
                color = TextPrimary
            )
            MinimalSeeAllButton(onClick = onSeeAllClick)
        }
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
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
    val displayArt = artist.artworkUrl ?: artist.topSongs.firstOrNull()?.getHighQualityArtwork()
        ?: artist.topSongs.firstOrNull()?.artworkUrl
    
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "artistScale"
    )
    
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        isPressed = event.type == androidx.compose.ui.input.pointer.PointerEventType.Press
                    }
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
                .shadow(
                    elevation = 8.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(0.15f),
                    spotColor = Color.Black.copy(0.25f)
                )
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AccentRed.copy(alpha = 0.2f),
                            AccentRed.copy(alpha = 0.05f)
                        ),
                        radius = 100f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = displayArt,
                contentDescription = artist.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
            
            // Overlay with gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.25f)
                            ),
                            radius = 100f
                        )
                    )
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = artist.name,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold
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
            .padding(vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                ),
                color = TextPrimary
            )
            MinimalSeeAllButton(onClick = onSeeAllClick)
        }
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
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
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = TextPrimary
            )
            MinimalSeeAllButton(onClick = onSeeAllClick)
        }
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(songs.take(8)) { song ->
                CompactSongCard(
                    song = song,
                    onClick = { onSongClick(song) }
                )
            }
        }
    }
}

@Composable
private fun PlaylistCarouselSection(
    title: String,
    playlists: List<Playlist>,
    onPlaylistClick: (Playlist) -> Unit,
    onSeeAllClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp
                ),
                color = TextPrimary
            )
            MinimalSeeAllButton(onClick = onSeeAllClick)
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
private fun CompactSongCard(
    song: Song,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(180.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            AsyncImage(
                model = song.getHighQualityArtwork() ?: song.artworkUrl,
                contentDescription = song.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = song.title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = song.artist,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 10.sp
            )
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
            TextButton(
                onClick = onSeeAllClick,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(AccentRed.copy(alpha = 0.1f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "See All",
                    color = AccentRed,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = AccentRed,
                    modifier = Modifier
                        .size(16.dp)
                        .padding(start = 4.dp)
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
