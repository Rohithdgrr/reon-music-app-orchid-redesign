/*
 * REON Music App - Skeleton Loader Components
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 */

package com.reon.music.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Shimmer effect for skeleton loaders
 */
@Composable
fun ShimmerBrush(): Brush {
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
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "translate"
    )
    
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnimation - 300f, translateAnimation - 300f),
        end = Offset(translateAnimation, translateAnimation)
    )
}

/**
 * Skeleton loader for song cards
 */
@Composable
fun SongCardSkeleton(
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.width(150.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(ShimmerBrush())
        )
        
        Spacer(modifier = Modifier.height(10.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(ShimmerBrush())
        )
        
        Spacer(modifier = Modifier.height(6.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(ShimmerBrush())
        )
    }
}

/**
 * Skeleton loader for playlist cards
 */
@Composable
fun PlaylistCardSkeleton(
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.width(160.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(ShimmerBrush())
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(ShimmerBrush())
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(ShimmerBrush())
        )
    }
}

/**
 * Skeleton loader for chart cards
 */
@Composable
fun ChartCardSkeleton(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(300.dp)
            .height(180.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(ShimmerBrush())
    )
}

/**
 * Skeleton loader for list items
 */
@Composable
fun ListItemSkeleton(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(ShimmerBrush())
        )
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(ShimmerBrush())
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(ShimmerBrush())
            )
        }
    }
}

/**
 * Skeleton loader for artist cards
 */
@Composable
fun ArtistCardSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.width(100.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(ShimmerBrush())
        )
        
        Spacer(modifier = Modifier.height(10.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(ShimmerBrush())
        )
    }
}

/**
 * Skeleton loader for section header
 */
@Composable
fun SectionHeaderSkeleton(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(ShimmerBrush())
        )
        
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(ShimmerBrush())
        )
    }
}

