/*
 * REON Music App - Theme System
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 */

package com.reon.music.ui.theme

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult

// Light theme colors
private val LightColorScheme = lightColorScheme(
    primary = ReonGreen,
    onPrimary = Color.White,
    primaryContainer = ReonGreenLight,
    onPrimaryContainer = ReonGreenDark,
    secondary = Color(0xFF625B71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    error = ReonError,
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = ReonBackground,
    onBackground = ReonOnSurface,
    surface = ReonSurface,
    onSurface = ReonOnSurface,
    surfaceVariant = ReonSurfaceVariant,
    onSurfaceVariant = ReonOnSurfaceVariant,
    outline = ReonOutline,
    outlineVariant = ReonOutlineVariant
)

// Dark theme colors
private val DarkColorScheme = darkColorScheme(
    primary = ReonGreen,
    onPrimary = Color.Black,
    primaryContainer = ReonGreenDark,
    onPrimaryContainer = ReonGreenLight,
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1C1C),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F)
)

// AMOLED Dark theme (pure black)
private val AmoledDarkColorScheme = DarkColorScheme.copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = Color(0xFF0A0A0A)
)

/**
 * Generate dynamic color scheme from album art
 */
@Composable
fun rememberDynamicColorScheme(
    imageUrl: String?,
    useDarkTheme: Boolean
): ColorScheme {
    val context = LocalContext.current
    var palette by remember { mutableStateOf<Palette?>(null) }
    
    LaunchedEffect(imageUrl) {
        if (imageUrl == null) {
            palette = null
            return@LaunchedEffect
        }
        
        try {
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .allowHardware(false)
                .build()
            
            val result = context.imageLoader.execute(request)
            if (result is SuccessResult) {
                val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                if (bitmap != null) {
                    palette = Palette.from(bitmap).generate()
                }
            }
        } catch (e: Exception) {
            palette = null
        }
    }
    
    return when {
        palette != null && useDarkTheme -> createDynamicDarkScheme(palette!!)
        palette != null -> createDynamicLightScheme(palette!!)
        useDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
}

private fun createDynamicDarkScheme(palette: Palette): ColorScheme {
    val dominantSwatch = palette.dominantSwatch
    val vibrantSwatch = palette.vibrantSwatch ?: dominantSwatch
    val mutedSwatch = palette.mutedSwatch ?: dominantSwatch
    
    val primary = vibrantSwatch?.rgb?.let { Color(it) } ?: DarkColorScheme.primary
    val secondary = mutedSwatch?.rgb?.let { Color(it) } ?: DarkColorScheme.secondary
    
    return DarkColorScheme.copy(
        primary = primary,
        primaryContainer = primary.copy(alpha = 0.3f),
        secondary = secondary,
        secondaryContainer = secondary.copy(alpha = 0.3f)
    )
}

private fun createDynamicLightScheme(palette: Palette): ColorScheme {
    val dominantSwatch = palette.dominantSwatch
    val vibrantSwatch = palette.darkVibrantSwatch ?: dominantSwatch
    val mutedSwatch = palette.darkMutedSwatch ?: dominantSwatch
    
    val primary = vibrantSwatch?.rgb?.let { Color(it) } ?: LightColorScheme.primary
    val secondary = mutedSwatch?.rgb?.let { Color(it) } ?: LightColorScheme.secondary
    
    return LightColorScheme.copy(
        primary = primary,
        primaryContainer = primary.copy(alpha = 0.2f),
        secondary = secondary,
        secondaryContainer = secondary.copy(alpha = 0.2f)
    )
}

/**
 * REON Theme
 */
@Composable
fun ReonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    dynamicColor: Boolean = false,
    dynamicColorImageUrl: String? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && dynamicColorImageUrl != null -> {
            rememberDynamicColorScheme(dynamicColorImageUrl, darkTheme)
        }
        pureBlack && darkTheme -> AmoledDarkColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    // Animate color transitions
    val animatedColorScheme = colorScheme.copy(
        primary = animateColorAsState(colorScheme.primary, tween(300), label = "primary").value,
        secondary = animateColorAsState(colorScheme.secondary, tween(300), label = "secondary").value,
        background = animateColorAsState(colorScheme.background, tween(300), label = "background").value,
        surface = animateColorAsState(colorScheme.surface, tween(300), label = "surface").value
    )
    
    MaterialTheme(
        colorScheme = animatedColorScheme,
        typography = ReonTypography,
        content = content
    )
}
