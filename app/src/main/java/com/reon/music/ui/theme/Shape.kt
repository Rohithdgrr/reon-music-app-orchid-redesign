/*
 * REON Music App - Shapes
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 */

package com.reon.music.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * REON Shape Scale - Sunset Redesign
 * Very rounded corners as per image reference
 */
val ReonShapes = Shapes(
    // Extra Small - chips, small buttons
    extraSmall = RoundedCornerShape(8.dp),
    
    // Small - text fields, small cards
    small = RoundedCornerShape(12.dp),
    
    // Medium - cards, dialogs
    medium = RoundedCornerShape(20.dp),
    
    // Large - large cards, sheets
    large = RoundedCornerShape(28.dp),
    
    // Extra Large - modals, bottom sheets
    extraLarge = RoundedCornerShape(32.dp)
)

// Custom shapes
val AlbumArtShape = RoundedCornerShape(20.dp)
val MiniPlayerShape = RoundedCornerShape(24.dp)
val BottomSheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
val ChipShape = RoundedCornerShape(50)
val ButtonShape = RoundedCornerShape(28.dp)
