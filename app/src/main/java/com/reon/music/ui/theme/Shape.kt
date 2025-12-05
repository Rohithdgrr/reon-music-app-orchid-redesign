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
 * REON Shape Scale
 */
val ReonShapes = Shapes(
    // Extra Small - chips, small buttons
    extraSmall = RoundedCornerShape(4.dp),
    
    // Small - text fields, small cards
    small = RoundedCornerShape(8.dp),
    
    // Medium - cards, dialogs
    medium = RoundedCornerShape(12.dp),
    
    // Large - large cards, sheets
    large = RoundedCornerShape(16.dp),
    
    // Extra Large - modals, bottom sheets
    extraLarge = RoundedCornerShape(24.dp)
)

// Custom shapes
val AlbumArtShape = RoundedCornerShape(12.dp)
val MiniPlayerShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
val BottomSheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
val ChipShape = RoundedCornerShape(50)
val ButtonShape = RoundedCornerShape(24.dp)
