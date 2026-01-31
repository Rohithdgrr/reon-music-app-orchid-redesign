/*
 * REON Music App - Now Playing Screen Components
 * Copyright (c) 2024 REON
 * Extracted components from NowPlayingScreen.kt
 */

package com.reon.music.ui.screens.nowplaying

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.reon.music.core.model.Song
import kotlinx.coroutines.delay
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Downloading

@Composable
fun QueueSongItem(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album art
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            com.reon.music.ui.components.OptimizedAsyncImage(
                imageUrl = song.getHighQualityArtwork(),
                contentDescription = song.title,
                modifier = Modifier.fillMaxSize(),
                quality = com.reon.music.ui.components.ImageQuality.THUMBNAIL,
                shape = RoundedCornerShape(8.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Song info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (isPlaying) Color(0xFFE53935) else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // Playing indicator
        if (isPlaying) {
            EqualizerAnimation(
                modifier = Modifier.size(24.dp),
                barColor = Color(0xFFE53935)
            )
        }
    }
}

@Composable
fun EqualizerAnimation(
    modifier: Modifier = Modifier,
    barColor: Color = Color(0xFFE53935)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer")
    
    val bar1Height by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val bar2Height by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val bar3Height by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf<Float>(bar1Height, bar2Height, bar3Height).forEach { height ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(20.dp * height)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor)
            )
        }
    }
}

@Composable
fun OptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold
        ),
        color = Color.White,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun DownloadButton(
    song: Song?,
    downloadProgress: Int?,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(enabled = !isDownloaded && !isDownloading) { onDownloadClick() },
        contentAlignment = Alignment.Center
    ) {
        when {
            isDownloaded -> {
                // Filled download icon with checkmark overlay
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Background circle with accent color
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFFE53935).copy(alpha = 0.15f))
                    )
                    // Filled download icon
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = "Downloaded",
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            isDownloading -> {
                // Show download progress without loading animation
                DownloadProgressIndicator(progress = downloadProgress ?: 0)
            }
            else -> {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Download",
                    tint = Color(0xFF666666),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun DownloadProgressIndicator(progress: Int) {
    val primaryColor = Color(0xFFE53935)
    val trackColor = Color(0xFFE0E0E0)
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(40.dp)
    ) {
        // Progress ring without loading animation
        androidx.compose.foundation.Canvas(modifier = Modifier.size(32.dp)) {
            // Draw track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
            )
            // Draw progress
            drawArc(
                color = primaryColor,
                startAngle = -90f,
                sweepAngle = (progress / 100f) * 360f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
            )
        }
        // Download icon in center
        Icon(
            imageVector = Icons.Default.Download,
            contentDescription = "Downloading",
            tint = primaryColor,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun AnimatedCheckmark() {
    var animationPlayed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "checkmark_scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(200),
        label = "checkmark_alpha"
    )

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(50)
        animationPlayed = true
    }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(200)),
        exit = fadeOut(animationSpec = tween(200))
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .scale(scale)
                .alpha(alpha)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 3f
                val checkmarkPath = Path().apply {
                    moveTo(size.width * 0.25f, size.height * 0.5f)
                    lineTo(size.width * 0.45f, size.height * 0.7f)
                    lineTo(size.width * 0.75f, size.height * 0.3f)
                }
                drawPath(
                    path = checkmarkPath,
                    color = Color(0xFF4CAF50),
                    style = Stroke(width = strokeWidth)
                )
            }
        }
    }
}
