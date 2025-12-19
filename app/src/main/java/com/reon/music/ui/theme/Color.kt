/*
 * REON Music App - Premium Red Light Theme
 * Copyright (c) 2024 REON
 * Modern, Premium Design System
 */

package com.reon.music.ui.theme

import androidx.compose.ui.graphics.Color

// ========================================
// PRIMARY BRAND COLORS - Red Palette
// ========================================
val ReonPrimary = Color(0xFFE53935)          // Main red accent
val ReonPrimaryDark = Color(0xFFC62828)      // Darker red for emphasis
val ReonPrimaryLight = Color(0xFFFFEBEE)     // Light red tint
val ReonPrimaryVariant = Color(0xFFFF5252)   // Vibrant red variant

// NEW: Sunrise/Sunset Palette
val SunriseWhite = Color(0xFFFFFFFF)
val SunriseYellow = Color(0xFFFFD54F)
val SunriseOrange = Color(0xFFFF9800)
val SunriseRed = Color(0xFFE53935)
val SunsetDeepRed = Color(0xFFB71C1C)
val SunsetPurple = Color(0xFF4A148C)
val SunsetGold = Color(0xFFFFD700)
val SunsetPeach = Color(0xFFFFAB91)
val SunrisePeach = Color(0xFFFFAB91) // Added for compatibility

val SunriseGradient = listOf(SunriseWhite, SunriseYellow, SunriseOrange, SunriseRed)
val SunsetGradient = listOf(SunriseRed, SunsetDeepRed, SunsetPurple)
val SunriseSunsetGradient = listOf(SunriseYellow, SunriseOrange, SunriseRed, SunsetDeepRed)

// Keep for compatibility
val ReonGreen = ReonPrimary
val ReonGreenDark = ReonPrimaryDark
val ReonGreenLight = ReonPrimaryLight

// ========================================
// ACCENT COLORS - For Visual Interest
// ========================================
val AccentRed = Color(0xFFE53935)          // Primary red accent
val AccentRedSecondary = Color(0xFFFF6B6B) // Secondary red accent
val AccentOrange = Color(0xFFFF7043)
val AccentPink = Color(0xFFEC407A)
val AccentPurple = Color(0xFFAB47BC)
val AccentBlue = Color(0xFF42A5F5)
val AccentTeal = Color(0xFF26A69A)
val AccentGreen = Color(0xFF66BB6A)
val AccentYellow = Color(0xFFFFCA28)

// ========================================
// BACKGROUND & SURFACE - Clean Light Theme
// ========================================
val ReonBackground = Color(0xFFFFFFFF)       // Pure white background - SimpMusic spec
val ReonSurface = Color(0xFFFAFAFA)          // Light gray surface - SimpMusic spec
val ReonSurfaceVariant = Color(0xFFF8F9FA)   // Subtle gray for cards
val ReonSurfaceElevated = Color(0xFFFFFFFF)  // Elevated surfaces
val ReonSurfaceDim = Color(0xFFF1F3F4)       // Dimmed surface

// Card Backgrounds
val CardBackground = Color(0xFFFFFFFF)
val CardBackgroundHover = Color(0xFFFAFAFA)
val CardBackgroundPressed = Color(0xFFF5F5F5)

// ========================================
// TEXT COLORS - Hierarchy
// ========================================
val ReonOnSurface = Color(0xFF1C1C1C)        // Primary text - SimpMusic spec
val ReonOnSurfaceVariant = Color(0xFF757575) // Secondary text - SimpMusic spec
val ReonOnSurfaceDisabled = Color(0xFF9AA0A6) // Disabled text
val TextPrimary = ReonOnSurface
val TextSecondary = ReonOnSurfaceVariant
val TextTertiary = Color(0xFF80868B)
val TextOnAccent = Color(0xFFFFFFFF)         // White text on colored backgrounds

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
