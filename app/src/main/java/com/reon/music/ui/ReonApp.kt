/*
 * REON Music App - Main App Composable
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 */

package com.reon.music.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.reon.music.ui.components.MiniPlayer
import com.reon.music.ui.components.ReonBottomNavigation
import com.reon.music.ui.navigation.ReonDestination
import com.reon.music.ui.screens.DownloadsScreen
import com.reon.music.ui.screens.HomeScreen
import com.reon.music.ui.screens.LibraryScreen
import com.reon.music.ui.screens.NowPlayingScreen
import com.reon.music.ui.screens.SearchScreen
import com.reon.music.ui.screens.SettingsScreen
import com.reon.music.ui.viewmodels.PlayerViewModel

/**
 * Main App Composable
 * Contains navigation, mini player, and now playing screen
 */
@Composable
fun ReonApp(
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    val playerState by playerViewModel.playerState.collectAsState()
    val playerUiState by playerViewModel.uiState.collectAsState()
    val currentPosition by playerViewModel.currentPosition.collectAsState()
    
    var showNowPlaying by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Handle back button when NowPlaying screen is visible
    BackHandler(enabled = showNowPlaying) {
        showNowPlaying = false
    }
    
    // Show errors in snackbar
    LaunchedEffect(playerUiState.error) {
        playerUiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            playerViewModel.clearError()
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                if (!showNowPlaying) {
                    ReonBottomNavigation(
                        destinations = ReonDestination.bottomNavDestinations,
                        currentDestination = navBackStackEntry?.destination,
                        onNavigate = { destination ->
                            navController.navigate(destination.route) {
                                popUpTo(ReonDestination.Home.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            },
            snackbarHost = {
                SnackbarHost(snackbarHostState)
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = ReonDestination.Home.route
                ) {
                    composable(ReonDestination.Home.route) {
                        HomeScreen(
                            playerViewModel = playerViewModel,
                            onSettingsClick = {
                                navController.navigate(ReonDestination.Settings.route)
                            }
                        )
                    }
                    composable(ReonDestination.Search.route) {
                        SearchScreen(playerViewModel = playerViewModel)
                    }
                    composable(ReonDestination.Library.route) {
                        LibraryScreen(playerViewModel = playerViewModel)
                    }
                    composable(ReonDestination.Downloads.route) {
                        DownloadsScreen(playerViewModel = playerViewModel)
                    }
                    composable(ReonDestination.Settings.route) {
                        SettingsScreen()
                    }
                }
                
                // Mini Player - shows at bottom when song is playing
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 0.dp)
                ) {
                    MiniPlayer(
                        playerState = playerState,
                        currentPosition = currentPosition,
                        isLoading = playerUiState.isLoading,
                        onPlayPause = { playerViewModel.togglePlayPause() },
                        onNext = { playerViewModel.skipToNext() },
                        onClick = { showNowPlaying = true }
                    )
                }
            }
        }
        
        // Full Now Playing Screen (overlay)
        AnimatedVisibility(
            visible = showNowPlaying,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            NowPlayingScreen(
                playerViewModel = playerViewModel,
                onDismiss = { showNowPlaying = false }
            )
        }
    }
}
