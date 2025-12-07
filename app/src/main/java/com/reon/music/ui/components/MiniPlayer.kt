/*
 * REON Music App - Enhanced Mini Player Component
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 */

package com.reon.music.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.reon.music.playback.PlayerState

// Pure White Theme Colors - Matching reference design
private val MiniPlayerBackground = Color(0xFFFFFFFF)
private val AccentGreen = Color(0xFF1DB954) // Spotify-style green for play button
private val TextPrimary = Color(0xFF1C1C1C)
private val TextSecondary = Color(0xFF757575)
private val ProgressTrackColor = Color(0xFFE0E0E0)

/**
 * Enhanced Mini Player Component - Separated from Bottom Navigation
 * Features: Like button, Previous/Next, Swipe gestures, Progress bar, Smooth animations
 * Matches reference design with green play button and white background
 */
@Composable
fun MiniPlayer(
    playerState: PlayerState,
    currentPosition: Long,
    isLoading: Boolean,
    isLiked: Boolean = false,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit = {},
    onLikeClick: () -> Unit = {},
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val song = playerState.currentSong
    
    // Animation for like button
    var isLikeAnimating by remember { mutableStateOf(false) }
    val likeScale by animateFloatAsState(
        targetValue = if (isLikeAnimating) 1.3f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "like_scale"
    )
    
    // Swipe gesture tracking
    var swipeOffset by remember { mutableFloatStateOf(0f) }
    
    AnimatedVisibility(
        visible = song != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Column {
            // Mini Player Card with pure white background
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(0.dp))
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                when {
                                    swipeOffset > 100f -> onPrevious()
                                    swipeOffset < -100f -> onNext()
                                }
                                swipeOffset = 0f
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                swipeOffset += dragAmount
                            }
                        )
                    }
                    .clickable(onClick = onClick),
                color = MiniPlayerBackground,
                shape = RoundedCornerShape(0.dp)
            ) {
                Column {
                    // Progress bar at top - Green gradient
                    val progress = if (playerState.duration > 0) {
                        currentPosition.toFloat() / playerState.duration
                    } else 0f
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(ProgressTrackColor)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress.coerceIn(0f, 1f))
                                .height(3.dp)
                                .background(AccentGreen)
                        )
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Album art with rounded corners
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF5F5F5)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (song?.artworkUrl != null) {
                                AsyncImage(
                                    model = song.getHighQualityArtwork(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        // Song info with duration
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = song?.title ?: "",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = TextPrimary
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = song?.artist ?: "",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = TextSecondary,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (playerState.duration > 0) {
                                    Text(
                                        text = " • ${formatDuration(currentPosition)}/${formatDuration(playerState.duration)}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp
                                        ),
                                        color = TextSecondary.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Like Button with animation - outlined heart
                        IconButton(
                            onClick = {
                                isLikeAnimating = true
                                onLikeClick()
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (isLiked) "Unlike" else "Like",
                                tint = if (isLiked) Color(0xFFE53935) else TextSecondary,
                                modifier = Modifier
                                    .size(22.dp)
                                    .graphicsLayer {
                                        scaleX = likeScale
                                        scaleY = likeScale
                                    }
                            )
                        }
                        
                        // Previous Button
                        IconButton(
                            onClick = onPrevious,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                tint = TextPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        
                        // Play/Pause Button - Green circle like reference
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(AccentGreen)
                                .clickable(onClick = onPlayPause),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    imageVector = if (playerState.isPlaying) 
                                        Icons.Default.Pause 
                                    else 
                                        Icons.Default.PlayArrow,
                                    contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                                    modifier = Modifier.size(28.dp),
                                    tint = Color.White
                                )
                            }
                        }
                        
                        // Next Button
                        IconButton(
                            onClick = onNext,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = TextPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Reset like animation
    LaunchedEffect(isLikeAnimating) {
        if (isLikeAnimating) {
            kotlinx.coroutines.delay(200)
            isLikeAnimating = false
        }
    }
}

/**
 * Format duration in mm:ss
 */
private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
