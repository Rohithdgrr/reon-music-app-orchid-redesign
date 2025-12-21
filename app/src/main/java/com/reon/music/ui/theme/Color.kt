/*
 * REON Music App - Premium Red Light Theme
 * Copyright (c) 2024 REON
 * Modern, Premium Design System
 */

package com.reon.music.ui.theme

import androidx.compose.ui.graphics.Color

// ========================================
// PRIMARY BRAND COLORS - Sunset Redesign
// ========================================
val ReonPrimary = Color(0xFFFF7043)          // Sunrise Orange
val ReonPrimaryDark = Color(0xFFBF360C)      // Deep Sunset Red
val ReonPrimaryLight = Color(0xFFFFAB91)     // Peach
val ReonPrimaryVariant = Color(0xFFFF5722)   // Vibrant Orange

// Sunset Redesign Specific Palette
val SunsetDeepBrown = Color(0xFF1A0F0F)
val SunsetWarmBrown = Color(0xFF2D1B1B)
val SunsetOrange = Color(0xFFFF7043)
val SunsetPeach = Color(0xFFFFAB91)
val SunsetGold = Color(0xFFFFD540)
val SunsetWhite = Color(0xFFFFEFEC)

val SunsetBackgroundGradient = listOf(Color(0xFF3D1910), Color(0xFF1A0F0F))
val SunsetCardGradient = listOf(Color(0x4DFFAB91), Color(0x1AFF7043)) // Semi-transparent peach to orange

// ========================================
// BACKGROUND & SURFACE - Sunset Theme
// ========================================
val ReonBackground = SunsetWarmBrown
val ReonSurface = Color(0xFF352424)
val ReonSurfaceVariant = Color(0xFF3D2B2B)
val ReonSurfaceElevated = Color(0xFF453232)
val ReonSurfaceDim = Color(0xFF251616)

// Card Backgrounds
val CardBackground = Color(0x1AFFFFFF) // Glassmorphism effect
val CardBackgroundHover = Color(0x33FFFFFF)
val CardBackgroundPressed = Color(0x4DFFFFFF)

// ========================================
// TEXT COLORS - Hierarchy
// ========================================
val ReonOnSurface = Color(0xFFFFEFEC)        // Cream white
val ReonOnSurfaceVariant = Color(0xFFD7CCC8) // Light warm gray
val ReonOnSurfaceDisabled = Color(0xFF8D6E63) // Muted warm brown
val TextPrimary = ReonOnSurface
val TextSecondary = ReonOnSurfaceVariant
val TextTertiary = Color(0xFFA1887F)
val TextOnAccent = Color(0xFF1A0F0F)         // Dark text on bright accents


// ========================================
// OUTLINE & DIVIDER
// ========================================
val ReonOutline = Color(0xFFE8EAED)          // Borders
val ReonOutlineVariant = Color(0xFFF1F3F4)   // Subtle borders
val DividerColor = Color(0xFFF1F3F4)

// ========================================
// SEMANTIC COLORS
// ========================================
val ReonError = Color(0xFFDC3545)
val ReonWarning = Color(0xFFFFC107)
val ReonInfo = Color(0xFF17A2B8)
val ReonSuccess = Color(0xFF28A745)

// ========================================
// CATEGORY CARD COLORS (SimpMusic-style)
// ========================================
val CategoryFavorite = Color(0xFFFFB3D9)     // Pink - Favorite
val CategoryFollowed = Color(0xFFFFD54F)     // Yellow - Followed
val CategoryMostPlayed = Color(0xFF4DD0E1)   // Cyan - Most Played
val CategoryDownloaded = Color(0xFF81C784)   // Green - Downloaded

val CategoryFavoriteIcon = Color(0xFFD32F2F)
val CategoryFollowedIcon = Color(0xFFFBC02D)
val CategoryMostPlayedIcon = Color(0xFF00ACC1)
val CategoryDownloadedIcon = Color(0xFF43A047)

// ========================================
// PLAYER COLORS
// ========================================
val ReonPlayerBackground = Color(0xFFFFFFFF)
val ReonSeekbarTrack = Color(0xFFE0E0E0)
val ReonSeekbarProgress = ReonPrimary
val ReonSeekbarThumb = ReonPrimary

// ========================================
// GRADIENT PRESETS
// ========================================
val GradientRedOrange = listOf(Color(0xFFE53935), Color(0xFFFF7043))
val GradientPinkPurple = listOf(Color(0xFFEC407A), Color(0xFFAB47BC))
val GradientBlueTeal = listOf(Color(0xFF42A5F5), Color(0xFF26A69A))
val GradientOrangeYellow = listOf(Color(0xFFFF7043), Color(0xFFFFCA28))

// ========================================
// CHIP COLORS
// ========================================
val ChipSelectedBackground = ReonPrimary
val ChipSelectedText = Color.White
val ChipUnselectedBackground = Color(0xFFF1F3F4)
val ChipUnselectedText = ReonOnSurface

// ========================================
// SHADOW COLORS
// ========================================
val ShadowLight = Color(0x1A000000)          // 10% black
val ShadowMedium = Color(0x29000000)         // 16% black
val ShadowDark = Color(0x3D000000)           // 24% black

// ========================================
// OVERLAY COLORS
// ========================================
val OverlayLight = Color(0x33FFFFFF)         // 20% white
val OverlayDark = Color(0x66000000)          // 40% black
val OverlayScrim = Color(0x80000000)         // 50% black

// ========================================
// BADGE COLORS
// ========================================
val BadgeNew = Color(0xFFE53935)
val BadgeLive = Color(0xFFFF1744)
val BadgePremium = Color(0xFFFFD700)
val BadgeOffline = Color(0xFF4CAF50)
