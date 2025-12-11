/*
 * REON Music App - Optimization Settings UI
 * Copyright (c) 2024 REON
 * User-facing optimization controls and monitoring
 */

package com.reon.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.reon.music.ui.viewmodels.OptimizationViewModel
import com.reon.music.core.optimization.BatteryStatus
import com.reon.music.core.optimization.NetworkStatus
import com.reon.music.core.optimization.CacheStats

@Composable
fun OptimizationSettingsScreen(
    viewModel: OptimizationViewModel = hiltViewModel()
) {
    val cacheStats by viewModel.cacheStats.collectAsState()
    val batteryStatus by viewModel.batteryStatus.collectAsState()
    val networkStatus by viewModel.networkStatus.collectAsState()
    val dataSaverMode by viewModel.dataSaverMode.collectAsState()
    val animationQuality by viewModel.animationQuality.collectAsState()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Text(
                "Optimization & Performance",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        // Current Status Cards
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Battery Status Card
                StatusCard(
                    modifier = Modifier.weight(1f),
                    title = "Battery",
                    value = when (batteryStatus) {
                        BatteryStatus.HEALTHY -> "Healthy"
                        BatteryStatus.LOW -> "Low"
                        BatteryStatus.CRITICAL -> "Critical"
                    },
                    icon = Icons.Default.BatteryChargingFull,
                    color = when (batteryStatus) {
                        BatteryStatus.HEALTHY -> Color(0xFF4CAF50)
                        BatteryStatus.LOW -> Color(0xFFFFC107)
                        BatteryStatus.CRITICAL -> Color(0xFFf44336)
                    }
                )
                
                // Network Status Card
                StatusCard(
                    modifier = Modifier.weight(1f),
                    title = "Network",
                    value = when (networkStatus) {
                        NetworkStatus.WIFI -> "WiFi"
                        NetworkStatus.MOBILE_DATA -> "Mobile"
                        NetworkStatus.OFFLINE -> "Offline"
                        NetworkStatus.UNKNOWN -> "Unknown"
                    },
                    icon = Icons.Default.CloudQueue,
                    color = when (networkStatus) {
                        NetworkStatus.WIFI -> Color(0xFF2196F3)
                        NetworkStatus.MOBILE_DATA -> Color(0xFF9C27B0)
                        else -> Color(0xFF757575)
                    }
                )
                
                // Data Saver Status Card
                StatusCard(
                    modifier = Modifier.weight(1f),
                    title = "Data Saver",
                    value = if (dataSaverMode) "On" else "Off",
                    icon = Icons.Default.DataUsage,
                    color = if (dataSaverMode) Color(0xFF4CAF50) else Color(0xFF757575)
                )
            }
        }
        
        // Cache Management Section
        item {
            SectionHeader("Cache Management", Icons.Default.Storage)
        }
        
        item {
            CacheStatsCard(cacheStats)
        }
        
        item {
            OptimizationButton(
                title = "Clear Cache",
                description = "Delete temporary files to free up space",
                icon = Icons.Default.DeleteOutline,
                onClick = { viewModel.clearCache() }
            )
        }
        
        // Data Saver Settings
        item {
            SectionHeader("Data Saver", Icons.Default.Language)
        }
        
        item {
            SettingsSwitchItem(
                title = "Data Saver Mode",
                description = "Reduce image quality and API calls on mobile data",
                icon = Icons.Default.DataUsage,
                isChecked = dataSaverMode,
                onCheckedChange = { viewModel.setDataSaverMode(it) }
            )
        }
        
        item {
            SettingsSwitchItem(
                title = "WiFi-Only Sync",
                description = "Only sync and download over WiFi",
                icon = Icons.Default.Wifi,
                isChecked = true,
                onCheckedChange = { /* TODO: Implement WiFi only sync */ }
            )
        }
        
        // Animation Quality
        item {
            SectionHeader("Animation Quality", Icons.Default.Animation)
        }
        
        item {
            AnimationQualitySelector(
                currentQuality = animationQuality,
                onQualityChange = { viewModel.setAnimationQuality(it) }
            )
        }
        
        // Memory Settings
        item {
            SectionHeader("Memory", Icons.Default.Memory)
        }
        
        item {
            SettingItem(
                title = "Image Cache Size",
                description = "Set maximum image cache size",
                icon = Icons.Default.Image
            )
        }
        
        item {
            DropdownSettingItem(
                title = "Cache Size Limit",
                selectedValue = "100 MB",
                options = listOf("50 MB", "100 MB", "200 MB", "500 MB"),
                onSelectionChange = { /* TODO: Implement cache size limit */ }
            )
        }
        
        // Advanced Settings
        item {
            SectionHeader("Advanced", Icons.Default.Settings)
        }
        
        item {
            SettingsSwitchItem(
                title = "Lazy Loading",
                description = "Load screens only when needed",
                icon = Icons.Default.Layers,
                isChecked = true,
                onCheckedChange = { /* TODO: Implement lazy loading toggle */ }
            )
        }
        
        item {
            SettingsSwitchItem(
                title = "Auto-Cleanup",
                description = "Automatically clean cache every 6 hours",
                icon = Icons.Default.CleaningServices,
                isChecked = true,
                onCheckedChange = { /* TODO: Implement auto-cleanup toggle */ }
            )
        }
        
        item {
            OptimizationButton(
                title = "Run Optimization",
                description = "Manually run all optimizations now",
                icon = Icons.Default.Bolt,
                onClick = { viewModel.runFullOptimization() }
            )
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatusCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Text(
                title,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium
            )
            Text(
                value,
                fontSize = 12.sp,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CacheStatsCard(stats: CacheStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Cache", fontWeight = FontWeight.Medium)
                Text("${stats.totalCacheMB} MB", fontWeight = FontWeight.Bold)
            }
            
            LinearProgressIndicator(
                progress = { (stats.percentUsed / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = when {
                    stats.percentUsed > 80 -> Color(0xFFf44336)
                    stats.percentUsed > 60 -> Color(0xFFFFC107)
                    else -> Color(0xFF4CAF50)
                }
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total: ${stats.totalCacheMB} MB", fontSize = 10.sp)
                Text("Usage: ${stats.percentUsed}%", fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun AnimationQualitySelector(
    currentQuality: String,
    onQualityChange: (String) -> Unit
) {
    val qualities = listOf("Low", "Medium", "High")
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Animation Quality", fontWeight = FontWeight.Medium)
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            qualities.forEach { quality ->
                Button(
                    onClick = { onQualityChange(quality) },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentQuality == quality)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surface
                    )
                ) {
                    Text(quality, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun OptimizationButton(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            title,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun SettingItem(
    title: String,
    description: String,
    icon: ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium)
                Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    description: String,
    icon: ImageVector,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!isChecked) }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium)
                Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun DropdownSettingItem(
    title: String,
    selectedValue: String,
    options: List<String>,
    onSelectionChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontWeight = FontWeight.Medium)
            
            Box {
                Text(
                    selectedValue,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onSelectionChange(option)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
