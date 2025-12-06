/*
 * REON Music App
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 */

package com.reon.music.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Download
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Navigation destinations for REON app
 */
sealed class ReonDestination(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : ReonDestination(
        route = "home",
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )
    
    data object Search : ReonDestination(
        route = "search",
        title = "Search",
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search
    )
    
    data object Library : ReonDestination(
        route = "library",
        title = "Library",
        selectedIcon = Icons.Filled.LibraryMusic,
        unselectedIcon = Icons.Outlined.LibraryMusic
    )
    
    data object Downloads : ReonDestination(
        route = "downloads",
        title = "Downloads",
        selectedIcon = Icons.Filled.Download,
        unselectedIcon = Icons.Outlined.Download
    )
    
    // Settings is no longer in bottom nav, but still a valid destination
    data object Settings : ReonDestination(
        route = "settings",
        title = "Settings",
        selectedIcon = Icons.Filled.Home, // Not used in bottom nav
        unselectedIcon = Icons.Outlined.Home
    )
    
    companion object {
        // Bottom nav: Home, Search, Library, Downloads (removed Settings)
        val bottomNavDestinations = listOf(Home, Search, Library, Downloads)
    }
}
