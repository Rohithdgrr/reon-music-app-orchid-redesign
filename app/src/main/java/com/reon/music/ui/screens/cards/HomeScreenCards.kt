/*
 * REON Music App - Home Screen Card Components
 * Copyright (c) 2024 REON
 * Extracted card components from HomeScreen.kt
 */

package com.reon.music.ui.screens.cards

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reon.music.core.model.Song
import com.reon.music.core.model.Playlist
import com.reon.music.core.model.Artist
import com.reon.music.ui.components.ImageQuality
import com.reon.music.ui.components.OptimizedAsyncImage
import com.reon.music.ui.viewmodels.ChartSection
import kotlin.math.cos
import kotlin.math.sin

// Helper function to convert angle to gradient offsets
internal fun angleToOffset(angle: Float, size: Float = 1000f): Pair<androidx.compose.ui.geometry.Offset, androidx.compose.ui.geometry.Offset> {
    val radians = Math.toRadians(angle.toDouble())
    val x = (size / 2) * cos(radians).toFloat()
    val y = (size / 2) * sin(radians).toFloat()
    return Pair(
        androidx.compose.ui.geometry.Offset(size / 2 - x, size / 2 - y),
        androidx.compose.ui.geometry.Offset(size / 2 + x, size / 2 + y)
    )
}

@Composable
fun RecentSongItem(
    song: Song,
    onClick: () -> Unit,
    isDownloaded: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .shadow(4.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
        ) {
            OptimizedAsyncImage(
                imageUrl = song.getHighQualityArtwork(),
                contentDescription = song.title,
                modifier = Modifier.fillMaxSize(),
                quality = ImageQuality.THUMBNAIL,
                shape = RoundedCornerShape(12.dp)
            )
            
            if (isDownloaded) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(2.dp)
                        .background(Color(0xFFE53935), CircleShape)
                        .padding(2.dp)
                ) {
                    Icon(
                        Icons.Default.DownloadDone,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = song.title,
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = Color(0xFF1A1A1A),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun QuickPickGridItem(
    song: Song,
    isDownloaded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
            OptimizedAsyncImage(
                imageUrl = song.getHighQualityArtwork(),
                contentDescription = song.title,
                modifier = Modifier.fillMaxSize(),
                quality = ImageQuality.MEDIUM,
                shape = RoundedCornerShape(12.dp)
            )
            
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
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
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                ),
                color = Color(0xFF1A1A1A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = song.artist,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = Color(0xFF666666),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CompactSongCard(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(140.dp)
            .height(180.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            OptimizedAsyncImage(
                imageUrl = song.getHighQualityArtwork() ?: song.artworkUrl,
                contentDescription = song.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                quality = ImageQuality.MEDIUM,
                shape = RoundedCornerShape(8.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = song.title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color(0xFF1A1A1A),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = song.artist,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF666666),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(170.dp)
            .clickable(onClick = onClick)
    ) {
        OptimizedAsyncImage(
            imageUrl = playlist.artworkUrl,
            contentDescription = playlist.name,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            quality = ImageQuality.MEDIUM,
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1A1A1A),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ChartCard(
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
                OptimizedAsyncImage(
                    imageUrl = thumbnailUrl,
                    contentDescription = chart.title,
                    modifier = Modifier.fillMaxSize(),
                    quality = ImageQuality.HIGH,
                    shape = RoundedCornerShape(20.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFFE53935), Color(0xFFFF6B6B)),
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
fun ArtistCard(
    artist: Artist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
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
        modifier = modifier
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
                            Color(0xFFE53935).copy(alpha = 0.2f),
                            Color(0xFFE53935).copy(alpha = 0.05f)
                        ),
                        radius = 100f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            OptimizedAsyncImage(
                imageUrl = displayArt,
                contentDescription = artist.name,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                quality = ImageQuality.MEDIUM,
                shape = CircleShape
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
            color = Color(0xFF1A1A1A),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
