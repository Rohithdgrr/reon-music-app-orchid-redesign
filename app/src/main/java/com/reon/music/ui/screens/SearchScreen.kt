/*
 * REON Music App - Enhanced Search Screen
 * YouTube Music-style search with crash-proof handling
 * Copyright (c) 2024 REON
 */

package com.reon.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.reon.music.ui.search.*
import kotlinx.coroutines.launch

@Composable
fun EnhancedSearchScreen(
    modifier: Modifier = Modifier,
    onNavigateToPlayer: (String) -> Unit = {},
    onNavigateToLibrary: () -> Unit = {}
) {
    YouTubeMusicStyleSearchScreen(
        onNavigateToPlayer = onNavigateToPlayer,
        onNavigateToLibrary = onNavigateToLibrary
    )
}

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    onNavigateToPlayer: (String) -> Unit = {},
    onNavigateToLibrary: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    // Use the new enhanced search system
    YouTubeMusicStyleSearchScreen(
        onNavigateToPlayer = onNavigateToPlayer,
        onNavigateToLibrary = onNavigateToLibrary
    )
}
