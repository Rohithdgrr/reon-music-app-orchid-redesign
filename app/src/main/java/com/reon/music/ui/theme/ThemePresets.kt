/*
 * REON Music App - Theme Presets
 * Copyright (c) 2024 REON
 * Multiple theme options for personalization
 */

package com.reon.music.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Theme Preset Data Class
 */
data class ThemePreset(
    val id: String,
    val name: String,
    val iconName: String,  // Icon identifier instead of emoji
    val lightScheme: ColorScheme,
    val darkScheme: ColorScheme,
    val description: String
)

/**
 * Available Theme Presets
 */
object ThemePresets {
    
    // Classic REON Green Theme (Default)
    val ClassicGreen = ThemePreset(
        id = "classic_green",
        name = "Classic Green",
        iconName = "forest",
        description = "Original REON theme with vibrant green",
        lightScheme = lightColorScheme(
            primary = Color(0xFF1DB954),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE8F5E9),
            onPrimaryContainer = Color(0xFF17A34A),
            secondary = Color(0xFF625B71),
            onSecondary = Color.White,
            background = Color(0xFFFAFAFA),
            onBackground = Color(0xFF1A1A1A),
            surface = Color.White,
            onSurface = Color(0xFF1A1A1A),
            surfaceVariant = Color(0xFFF5F5F5),
            onSurfaceVariant = Color(0xFF666666),
            outline = Color(0xFFE0E0E0)
        ),
        darkScheme = darkColorScheme(
            primary = Color(0xFF1DB954),
            onPrimary = Color.Black,
            primaryContainer = Color(0xFF17A34A),
            onPrimaryContainer = Color(0xFFE8F5E9),
            secondary = Color(0xFFCCC2DC),
            onSecondary = Color(0xFF332D41),
            background = Color(0xFF121212),
            onBackground = Color(0xFFE6E1E5),
            surface = Color(0xFF1C1C1C),
            onSurface = Color(0xFFE6E1E5),
            surfaceVariant = Color(0xFF2A2A2A),
            onSurfaceVariant = Color(0xFFCAC4D0),
            outline = Color(0xFF938F99)
        )
    )
    
    // Ocean Blue Theme
    val OceanBlue = ThemePreset(
        id = "ocean_blue",
        name = "Ocean Blue",
        iconName = "water",
        description = "Calm and professional blue tones",
        lightScheme = lightColorScheme(
            primary = Color(0xFF0077BE),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE3F2FD),
            onPrimaryContainer = Color(0xFF004C8C),
            secondary = Color(0xFF00ACC1),
            onSecondary = Color.White,
            background = Color(0xFFF8FBFF),
            onBackground = Color(0xFF1A1C1E),
            surface = Color.White,
            onSurface = Color(0xFF1A1C1E),
            surfaceVariant = Color(0xFFF0F4F8),
            onSurfaceVariant = Color(0xFF5F6368),
            outline = Color(0xFFDAE2E8)
        ),
        darkScheme = darkColorScheme(
            primary = Color(0xFF4FC3F7),
            onPrimary = Color(0xFF003258),
            primaryContainer = Color(0xFF004C8C),
            onPrimaryContainer = Color(0xFFE3F2FD),
            secondary = Color(0xFF4DD0E1),
            onSecondary = Color(0xFF003D47),
            background = Color(0xFF0A1929),
            onBackground = Color(0xFFE1E3E5),
            surface = Color(0xFF132F4C),
            onSurface = Color(0xFFE1E3E5),
            surfaceVariant = Color(0xFF1E3A52),
            onSurfaceVariant = Color(0xFFB2BAC2),
            outline = Color(0xFF6F7E8C)
        )
    )
    
    // Sunset Orange Theme
    val SunsetOrange = ThemePreset(
        id = "sunset_orange",
        name = "Sunset Orange",
        iconName = "sun",
        description = "Warm and energetic orange hues",
        lightScheme = lightColorScheme(
            primary = Color(0xFFFF6B35),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFEDE8),
            onPrimaryContainer = Color(0xFFD84315),
            secondary = Color(0xFFFFA726),
            onSecondary = Color.White,
            background = Color(0xFFFFFBF7),
            onBackground = Color(0xFF1F1B16),
            surface = Color.White,
            onSurface = Color(0xFF1F1B16),
            surfaceVariant = Color(0xFFFFF3E0),
            onSurfaceVariant = Color(0xFF6B5D4F),
            outline = Color(0xFFE0D4C8)
        ),
        darkScheme = darkColorScheme(
            primary = Color(0xFFFFB74D),
            onPrimary = Color(0xFF4A2800),
            primaryContainer = Color(0xFFD84315),
            onPrimaryContainer = Color(0xFFFFEDE8),
            secondary = Color(0xFFFFCC80),
            onSecondary = Color(0xFF4A3800),
            background = Color(0xFF1A1410),
            onBackground = Color(0xFFEDE0D4),
            surface = Color(0xFF2B2218),
            onSurface = Color(0xFFEDE0D4),
            surfaceVariant = Color(0xFF3D3027),
            onSurfaceVariant = Color(0xFFD7C2B0),
            outline = Color(0xFF9F8D7D)
        )
    )
    
    // Purple Haze Theme
    val PurpleHaze = ThemePreset(
        id = "purple_haze",
        name = "Purple Haze",
        iconName = "star",
        description = "Rich and creative purple shades",
        lightScheme = lightColorScheme(
            primary = Color(0xFF7C4DFF),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFEDE7F6),
            onPrimaryContainer = Color(0xFF5E35B1),
            secondary = Color(0xFFAB47BC),
            onSecondary = Color.White,
            background = Color(0xFFFAF8FF),
            onBackground = Color(0xFF1C1B1F),
            surface = Color.White,
            onSurface = Color(0xFF1C1B1F),
            surfaceVariant = Color(0xFFF3EEFF),
            onSurfaceVariant = Color(0xFF655F71),
            outline = Color(0xFFE0D9EC)
        ),
        darkScheme = darkColorScheme(
            primary = Color(0xFFB388FF),
            onPrimary = Color(0xFF3700B3),
            primaryContainer = Color(0xFF5E35B1),
            onPrimaryContainer = Color(0xFFEDE7F6),
            secondary = Color(0xFFCE93D8),
            onSecondary = Color(0xFF4A148C),
            background = Color(0xFF1A1625),
            onBackground = Color(0xFFE6E1E5),
            surface = Color(0xFF2B2438),
            onSurface = Color(0xFFE6E1E5),
            surfaceVariant = Color(0xFF3D3347),
            onSurfaceVariant = Color(0xFFD0C4DD),
            outline = Color(0xFF9A8FA8)
        )
    )
    
    // Rose Gold Theme
    val RoseGold = ThemePreset(
        id = "rose_gold",
        name = "Rose Gold",
        iconName = "flower",
        description = "Elegant and sophisticated pink tones",
        lightScheme = lightColorScheme(
            primary = Color(0xFFE91E63),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFCE4EC),
            onPrimaryContainer = Color(0xFFC2185B),
            secondary = Color(0xFFFF6F91),
            onSecondary = Color.White,
            background = Color(0xFFFFFAFA),
            onBackground = Color(0xFF1F1A1B),
            surface = Color.White,
            onSurface = Color(0xFF1F1A1B),
            surfaceVariant = Color(0xFFFFF0F3),
            onSurfaceVariant = Color(0xFF6B5F61),
            outline = Color(0xFFE8D8DB)
        ),
        darkScheme = darkColorScheme(
            primary = Color(0xFFFF80AB),
            onPrimary = Color(0xFF5F0028),
            primaryContainer = Color(0xFFC2185B),
            onPrimaryContainer = Color(0xFFFCE4EC),
            secondary = Color(0xFFFF9EB7),
            onSecondary = Color(0xFF5F0028),
            background = Color(0xFF1A1416),
            onBackground = Color(0xFFEDE0E2),
            surface = Color(0xFF2B2224),
            onSurface = Color(0xFFEDE0E2),
            surfaceVariant = Color(0xFF3D3033),
            onSurfaceVariant = Color(0xFFD7C2C6),
            outline = Color(0xFF9F8D91)
        )
    )
    
    // Forest Green Theme
    val ForestGreen = ThemePreset(
        id = "forest_green",
        name = "Forest Green",
        iconName = "park",
        description = "Natural and calming green tones",
        lightScheme = lightColorScheme(
            primary = Color(0xFF2E7D32),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE8F5E9),
            onPrimaryContainer = Color(0xFF1B5E20),
            secondary = Color(0xFF66BB6A),
            onSecondary = Color.White,
            background = Color(0xFFF8FFF8),
            onBackground = Color(0xFF1A1C1A),
            surface = Color.White,
            onSurface = Color(0xFF1A1C1A),
            surfaceVariant = Color(0xFFF1F8F1),
            onSurfaceVariant = Color(0xFF5F6360),
            outline = Color(0xFFDCE8DC)
        ),
        darkScheme = darkColorScheme(
            primary = Color(0xFF81C784),
            onPrimary = Color(0xFF003300),
            primaryContainer = Color(0xFF1B5E20),
            onPrimaryContainer = Color(0xFFE8F5E9),
            secondary = Color(0xFFA5D6A7),
            onSecondary = Color(0xFF003300),
            background = Color(0xFF0F1410),
            onBackground = Color(0xFFE1E8E1),
            surface = Color(0xFF1E2420),
            onSurface = Color(0xFFE1E8E1),
            surfaceVariant = Color(0xFF2F3530),
            onSurfaceVariant = Color(0xFFC2D0C2),
            outline = Color(0xFF8C9A8C)
        )
    )
    
    // Midnight Black Theme (AMOLED)
    val MidnightBlack = ThemePreset(
        id = "midnight_black",
        name = "Midnight Black",
        iconName = "night",
        description = "Pure black for AMOLED displays",
        lightScheme = lightColorScheme(
            primary = Color(0xFF424242),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE0E0E0),
            onPrimaryContainer = Color(0xFF212121),
            secondary = Color(0xFF757575),
            onSecondary = Color.White,
            background = Color(0xFFFAFAFA),
            onBackground = Color(0xFF1A1A1A),
            surface = Color.White,
            onSurface = Color(0xFF1A1A1A),
            surfaceVariant = Color(0xFFF5F5F5),
            onSurfaceVariant = Color(0xFF616161),
            outline = Color(0xFFE0E0E0)
        ),
        darkScheme = darkColorScheme(
            primary = Color(0xFFBBBBBB),
            onPrimary = Color.Black,
            primaryContainer = Color(0xFF424242),
            onPrimaryContainer = Color(0xFFE0E0E0),
            secondary = Color(0xFF9E9E9E),
            onSecondary = Color.Black,
            background = Color.Black,
            onBackground = Color(0xFFE3E3E3),
            surface = Color.Black,
            onSurface = Color(0xFFE3E3E3),
            surfaceVariant = Color(0xFF0A0A0A),
            onSurfaceVariant = Color(0xFFCACACA),
            outline = Color(0xFF616161)
        )
    )
    
    // Crimson Red Theme
    val CrimsonRed = ThemePreset(
        id = "crimson_red",
        name = "Crimson Red",
        iconName = "heart",
        description = "Bold and passionate red theme",
        lightScheme = lightColorScheme(
            primary = Color(0xFFD32F2F),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFEBEE),
            onPrimaryContainer = Color(0xFFB71C1C),
            secondary = Color(0xFFEF5350),
            onSecondary = Color.White,
            background = Color(0xFFFFFAFA),
            onBackground = Color(0xFF1F1A1A),
            surface = Color.White,
            onSurface = Color(0xFF1F1A1A),
            surfaceVariant = Color(0xFFFFF5F5),
            onSurfaceVariant = Color(0xFF6B5F5F),
            outline = Color(0xFFE8D8D8)
        ),
        darkScheme = darkColorScheme(
            primary = Color(0xFFEF5350),
            onPrimary = Color(0xFF5F0000),
            primaryContainer = Color(0xFFB71C1C),
            onPrimaryContainer = Color(0xFFFFEBEE),
            secondary = Color(0xFFE57373),
            onSecondary = Color(0xFF5F0000),
            background = Color(0xFF1A1010),
            onBackground = Color(0xFFEDE0E0),
            surface = Color(0xFF2B1E1E),
            onSurface = Color(0xFFEDE0E0),
            surfaceVariant = Color(0xFF3D2C2C),
            onSurfaceVariant = Color(0xFFD7C2C2),
            outline = Color(0xFF9F8D8D)
        )
    )
    
    /**
     * Get all available theme presets
     */
    fun getAllPresets(): List<ThemePreset> = listOf(
        ClassicGreen,
        OceanBlue,
        SunsetOrange,
        PurpleHaze,
        RoseGold,
        ForestGreen,
        MidnightBlack,
        CrimsonRed
    )
    
    /**
     * Get theme preset by ID
     */
    fun getPresetById(id: String): ThemePreset? {
        return getAllPresets().find { it.id == id }
    }
}
