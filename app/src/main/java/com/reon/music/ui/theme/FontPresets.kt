/*
 * REON Music App - Font Presets
 * Copyright (c) 2024 REON
 * Multiple font options for personalization
 */

package com.reon.music.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Font Preset Data Class
 */
data class FontPreset(
    val id: String,
    val name: String,
    val fontFamily: FontFamily,
    val description: String
)

/**
 * Font Size Preset
 */
enum class FontSizePreset(val displayName: String, val scaleFactor: Float) {
    SMALL("Small", 0.85f),
    MEDIUM("Medium", 1.0f),
    LARGE("Large", 1.15f),
    XLARGE("Extra Large", 1.3f)
}

/**
 * Available Font Presets
 */
object FontPresets {
    
    // Default System Font
    val SystemDefault = FontPreset(
        id = "system_default",
        name = "System Default",
        fontFamily = FontFamily.Default,
        description = "Default system font"
    )
    
    // Note: For production, you would add actual font files to res/font/
    // For now, using system fonts as placeholders
    
    val Roboto = FontPreset(
        id = "roboto",
        name = "Roboto",
        fontFamily = FontFamily.Default, // In production: FontFamily(Font(R.font.roboto_regular))
        description = "Clean and modern sans-serif"
    )
    
    val Inter = FontPreset(
        id = "inter",
        name = "Inter",
        fontFamily = FontFamily.SansSerif,
        description = "Highly readable interface font"
    )
    
    val Poppins = FontPreset(
        id = "poppins",
        name = "Poppins",
        fontFamily = FontFamily.SansSerif,
        description = "Geometric sans-serif with personality"
    )
    
    val Montserrat = FontPreset(
        id = "montserrat",
        name = "Montserrat",
        fontFamily = FontFamily.SansSerif,
        description = "Urban and contemporary"
    )
    
    val OpenSans = FontPreset(
        id = "open_sans",
        name = "Open Sans",
        fontFamily = FontFamily.SansSerif,
        description = "Friendly and approachable"
    )
    
    val Serif = FontPreset(
        id = "serif",
        name = "Serif",
        fontFamily = FontFamily.Serif,
        description = "Classic and elegant"
    )
    
    val Monospace = FontPreset(
        id = "monospace",
        name = "Monospace",
        fontFamily = FontFamily.Monospace,
        description = "Technical and precise"
    )
    
    /**
     * Get all available font presets
     */
    fun getAllPresets(): List<FontPreset> = listOf(
        SystemDefault,
        Roboto,
        Inter,
        Poppins,
        Montserrat,
        OpenSans,
        Serif,
        Monospace
    )
    
    /**
     * Get font preset by ID
     */
    fun getPresetById(id: String): FontPreset? {
        return getAllPresets().find { it.id == id }
    }
    
    /**
     * Create typography with custom font and size
     */
    fun createTypography(
        fontFamily: FontFamily = FontFamily.Default,
        sizePreset: FontSizePreset = FontSizePreset.MEDIUM
    ): Typography {
        val scale = sizePreset.scaleFactor
        
        return Typography(
            displayLarge = TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = (57 * scale).sp,
                lineHeight = (64 * scale).sp,
                letterSpacing = (-0.25 * scale).sp
            ),
            displayMedium = TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = (45 * scale).sp,
                lineHeight = (52 * scale).sp,
                letterSpacing = 0.sp
            ),
            displaySmall = TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = (36 * scale).sp,
                lineHeight = (44 * scale).sp,
                letterSpacing = 0.sp
            ),
            headlineLarge = TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = (32 * scale).sp,
                lineHeight = (40 * scale).sp,
                letterSpacing = 0.sp
            ),
            headlineMedium = TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = (28 * scale).sp,
                lineHeight = (36 * scale).sp,
                letterSpacing = 0.sp
            ),
            headlineSmall = TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = (24 * scale).sp,
                lineHeight = (32 * scale).sp,
                letterSpacing = 0.sp
            ),
            titleLarge = TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = (22 * scale).sp,
                lineHeight = (28 * scale).sp,
                letterSpacing = 0.sp
            ),
            titleMedium = TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = (16 * scale).sp,
                lineHeight = (24 * scale).sp,
                letterSpacing = (0.15 * scale).sp
            ),
            titleSmall = TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = (14 * scale).sp,
                lineHeight = (20 * scale).sp,
                letterSpacing = (0.1 * scale).sp
            ),
            bodyLarge = TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = (16 * scale).sp,
                lineHeight = (24 * scale).sp,
                letterSpacing = (0.5 * scale).sp
            ),
            bodyMedium = TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = (14 * scale).sp,
                lineHeight = (20 * scale).sp,
                letterSpacing = (0.25 * scale).sp
            ),
            bodySmall = TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = (12 * scale).sp,
                lineHeight = (16 * scale).sp,
                letterSpacing = (0.4 * scale).sp
            ),
            labelLarge = TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = (14 * scale).sp,
                lineHeight = (20 * scale).sp,
                letterSpacing = (0.1 * scale).sp
            ),
            labelMedium = TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = (12 * scale).sp,
                lineHeight = (16 * scale).sp,
                letterSpacing = (0.5 * scale).sp
            ),
            labelSmall = TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = (11 * scale).sp,
                lineHeight = (16 * scale).sp,
                letterSpacing = (0.5 * scale).sp
            )
        )
    }
}
