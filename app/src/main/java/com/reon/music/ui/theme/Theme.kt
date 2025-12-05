/*
 * REON Music App - Theme Configuration
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 */

package com.reon.music.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Light Color Scheme for REON
 * Professional light theme with green accent
 */
private val ReonLightColorScheme = lightColorScheme(
    // Primary
    primary = ReonGreen,
    onPrimary = Color.White,
    primaryContainer = ReonGreenLight,
    onPrimaryContainer = ReonGreenDark,
    
    // Secondary (use primary as accent)
    secondary = ReonGreen,
    onSecondary = Color.White,
    secondaryContainer = ReonGreenLight,
    onSecondaryContainer = ReonGreenDark,
    
    // Tertiary
    tertiary = ReonGreen,
    onTertiary = Color.White,
    tertiaryContainer = ReonGreenLight,
    onTertiaryContainer = ReonGreenDark,
    
    // Background & Surface
    background = ReonBackground,
    onBackground = ReonOnSurface,
    surface = ReonSurface,
    onSurface = ReonOnSurface,
    surfaceVariant = ReonSurfaceVariant,
    onSurfaceVariant = ReonOnSurfaceVariant,
    
    // Outline
    outline = ReonOutline,
    outlineVariant = ReonOutlineVariant,
    
    // Error
    error = ReonError,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFFB91C1C),
    
    // Inverse
    inverseSurface = ReonOnSurface,
    inverseOnSurface = ReonSurface,
    inversePrimary = ReonGreenLight,
    
    // Surface tint
    surfaceTint = ReonGreen,
    
    // Scrim
    scrim = Color.Black.copy(alpha = 0.32f)
)

/**
 * REON Theme - Light Only
 */
@Composable
fun ReonTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = ReonLightColorScheme
    val view = LocalView.current
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = ReonBackground.toArgb()
            window.navigationBarColor = ReonSurface.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ReonTypography,
        shapes = ReonShapes,
        content = content
    )
}
