/*
 * REON Music App - Spotify Canvas Component
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 */

package com.reon.music.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Spotify Canvas - Animated album art background
 * Creates a blurred, animated background from album artwork
 */
@Composable
fun SpotifyCanvas(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    blurRadius: Float = 50f,
    opacity: Float = 0.6f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "canvas")
    
    // Subtle scale animation
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Box(modifier = modifier.fillMaxSize()) {
        if (imageUrl != null) {
            // Blurred background image
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .blur(blurRadius.dp)
            )
            
            // Gradient overlay for better text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = opacity),
                                Color.Black.copy(alpha = opacity * 0.8f),
                                Color.Black.copy(alpha = opacity * 0.6f)
                            )
                        )
                    )
            )
        } else {
            // Fallback gradient when no image
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )
        }
    }
}

/**
 * Glassmorphism effect for overlays
 */
@Composable
fun GlassmorphismBox(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.05f)
                    )
                )
            )
    ) {
        content()
    }
}

