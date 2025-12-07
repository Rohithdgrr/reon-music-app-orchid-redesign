/*
 * REON Music App - Bottom Navigation
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 */

package com.reon.music.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.reon.music.ui.navigation.ReonDestination

// Clean White Theme Colors
private val NavBarBackground = Color(0xFFFFFFFF)
private val NavBarActiveColor = Color(0xFF1C1C1C)  // Dark for active
private val NavBarInactiveColor = Color(0xFF9E9E9E)  // Gray for inactive
private val NavBarIndicatorColor = Color(0xFFF5F5F5)  // Light gray indicator

/**
 * Bottom Navigation Bar for REON
 * Clean white design with subtle animations
 */
@Composable
fun ReonBottomNavigation(
    destinations: List<ReonDestination>,
    currentDestination: NavDestination?,
    onNavigate: (ReonDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.height(64.dp),
        containerColor = NavBarBackground,
        contentColor = NavBarActiveColor,
        tonalElevation = 0.dp
    ) {
        destinations.forEach { destination ->
            val selected = currentDestination?.hierarchy?.any { 
                it.route == destination.route 
            } == true
            
            val scale by animateFloatAsState(
                targetValue = if (selected) 1.05f else 1f,
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "scale"
            )
            
            val iconColor by animateColorAsState(
                targetValue = if (selected) NavBarActiveColor else NavBarInactiveColor,
                label = "iconColor"
            )
            
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(destination) },
                icon = {
                    Icon(
                        imageVector = if (selected) 
                            destination.selectedIcon 
                        else 
                            destination.unselectedIcon,
                        contentDescription = destination.title,
                        modifier = Modifier
                            .size(24.dp)
                            .scale(scale),
                        tint = iconColor
                    )
                },
                label = {
                    Text(
                        text = destination.title,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = iconColor
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = NavBarActiveColor,
                    selectedTextColor = NavBarActiveColor,
                    unselectedIconColor = NavBarInactiveColor,
                    unselectedTextColor = NavBarInactiveColor,
                    indicatorColor = NavBarIndicatorColor
                )
            )
        }
    }
}
