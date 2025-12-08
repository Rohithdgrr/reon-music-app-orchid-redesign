/*
 * REON Music App - Playlist Detail Screen
 * Copyright (c) 2024 REON
 * Displays playlist songs with full functionality
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.reon.music.core.model.Playlist
import com.reon.music.core.model.Song
import com.reon.music.ui.viewmodels.HomeViewModel
import com.reon.music.ui.viewmodels.PlayerViewModel

// Premium Light Theme Colors
private val BackgroundLight = Color(0xFFFFFFFF)
private val CardWhite = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF1A1A1A)
private val TextSecondary = Color(0xFF5F6368)
private val AccentBlue = Color(0xFF42A5F5)
private val AccentRed = Color(0xFFE53935)

@Composable
fun PlaylistDetailScreen(
    playlist: com.reon.music.core.model.Playlist,
    onBackClick: () -> Unit = {},
    onSongClick: (Song) -> Unit = {},
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    // Reuse ChartDetailScreen with playlist ID as chart type
    ChartDetailScreen(
        chartTitle = playlist.name,
        chartType = playlist.id,
        onBackClick = onBackClick,
        onSongClick = onSongClick,
        playerViewModel = playerViewModel
    )
}

