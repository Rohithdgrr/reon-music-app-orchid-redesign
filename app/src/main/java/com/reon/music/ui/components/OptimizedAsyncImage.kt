/*
 * REON Music App - Optimized Image Loading
 * Copyright (c) 2024 REON
 * Memory and battery efficient image loading with caching
 */

package com.reon.music.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest

/**
 * Image quality levels for different use cases
 * Lower quality = faster load, less data, less memory
 */
enum class ImageQuality {
    THUMBNAIL,  // 120x120 - for list items
    MEDIUM,     // 300x300 - for cards
    HIGH        // 544x544 - for now playing screen
}

/**
 * Optimized AsyncImage with:
 * - Memory caching (via Coil)
 * - Disk caching (via Coil)
 * - Shimmer loading placeholder
 * - Fallback icon for errors
 * - Size-based quality selection
 * - Crossfade animation
 */
@Composable
fun OptimizedAsyncImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    quality: ImageQuality = ImageQuality.MEDIUM,
    shape: Shape = RoundedCornerShape(8.dp),
    contentScale: ContentScale = ContentScale.Crop,
    placeholderIcon: ImageVector = Icons.Filled.MusicNote,
    crossfadeDuration: Int = 200
) {
    val context = LocalContext.current
    
    // Build optimized image request
    val imageRequest = remember(imageUrl, quality) {
        ImageRequest.Builder(context)
            .data(getOptimizedUrl(imageUrl, quality))
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(crossfadeDuration)
            .size(getTargetSize(quality))
            .build()
    }
    
    SubcomposeAsyncImage(
        model = imageRequest,
        contentDescription = contentDescription,
        modifier = modifier.clip(shape),
        contentScale = contentScale,
        loading = {
            ShimmerPlaceholder(modifier = Modifier.fillMaxSize())
        },
        error = {
            ErrorPlaceholder(
                icon = placeholderIcon,
                modifier = Modifier.fillMaxSize()
            )
        }
    )
}

/**
 * Get optimized URL based on quality
 * YouTube thumbnails support different quality levels
 */
private fun getOptimizedUrl(url: String?, quality: ImageQuality): String? {
    if (url == null) return null
    
    // YouTube thumbnail optimization
    return when {
        url.contains("ytimg.com") || url.contains("ggpht.com") -> {
            when (quality) {
                ImageQuality.THUMBNAIL -> url.replace(Regex("w\\d+-h\\d+"), "w120-h120")
                    .replace("maxresdefault", "default")
                    .replace("hqdefault", "default")
                ImageQuality.MEDIUM -> url.replace(Regex("w\\d+-h\\d+"), "w300-h300")
                    .replace("maxresdefault", "hqdefault")
                ImageQuality.HIGH -> url.replace(Regex("w\\d+-h\\d+"), "w544-h544")
            }
        }
        else -> url
    }
}

/**
 * Get target size for Coil based on quality
 */
private fun getTargetSize(quality: ImageQuality): coil.size.Size {
    return when (quality) {
        ImageQuality.THUMBNAIL -> coil.size.Size(120, 120)
        ImageQuality.MEDIUM -> coil.size.Size(300, 300)
        ImageQuality.HIGH -> coil.size.Size(544, 544)
    }
}

/**
 * Shimmer effect placeholder during loading
 */
@Composable
private fun ShimmerPlaceholder(modifier: Modifier = Modifier) {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    )
    
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "translate"
    )
    
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnimation - 300f, translateAnimation - 300f),
        end = Offset(translateAnimation, translateAnimation)
    )
    
    Box(
        modifier = modifier.background(brush)
    )
}

/**
 * Error/fallback placeholder with icon
 */
@Composable
private fun ErrorPlaceholder(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
) {
    Box(
        modifier = modifier.background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(32.dp)
        )
    }
}

/**
 * Animated loading indicator with pulsing effect
 */
@Composable
fun PulsingLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 48.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = "Loading",
            tint = color.copy(alpha = alpha),
            modifier = Modifier
                .size(size * scale)
        )
    }
}
