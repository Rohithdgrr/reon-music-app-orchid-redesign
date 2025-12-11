/*
 * REON Music App - Optimization ViewModel
 * Copyright (c) 2024 REON
 * State management for optimization settings and monitoring
 */

package com.reon.music.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.reon.music.services.*
import javax.inject.Inject
import com.reon.music.core.optimization.BatteryStatus
import com.reon.music.core.optimization.NetworkStatus
import com.reon.music.core.optimization.CacheStats
import com.reon.music.core.optimization.CacheType

@HiltViewModel
class OptimizationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appOptimizer: AppOptimizer,
    private val batteryOptimizer: BatteryOptimizer,
    private val cacheManager: SmartCacheManager,
    private val networkOptimizer: NetworkDataOptimizer
) : ViewModel() {
    
    private val _cacheStats = MutableStateFlow(
        CacheStats(0, 0, 0, 0, 0)
    )
    val cacheStats: StateFlow<CacheStats> = _cacheStats.asStateFlow()
    
    private val _batteryStatus = MutableStateFlow<BatteryStatus>(BatteryStatus.HEALTHY)
    val batteryStatus: StateFlow<BatteryStatus> = _batteryStatus.asStateFlow()
    
    private val _networkStatus = MutableStateFlow<NetworkStatus>(NetworkStatus.UNKNOWN)
    val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()
    
    private val _dataSaverMode = MutableStateFlow(false)
    val dataSaverMode: StateFlow<Boolean> = _dataSaverMode.asStateFlow()
    
    private val _animationQuality = MutableStateFlow("Medium")
    val animationQuality: StateFlow<String> = _animationQuality.asStateFlow()
    
    private val _optimizationProgress = MutableStateFlow(0)
    val optimizationProgress: StateFlow<Int> = _optimizationProgress.asStateFlow()
    
    init {
        viewModelScope.launch {
            initializeOptimization()
        }
    }
    
    /**
     * Initialize all optimization systems
     */
    private suspend fun initializeOptimization() {
        // Start monitoring
        updateBatteryStatus()
        updateNetworkStatus()
        updateCacheStats()
        
        // Start background optimizations
        batteryOptimizer.scheduleBackgroundSync()
    }
    
    /**
     * Update battery status and adjust optimization level
     */
    fun updateBatteryStatus() {
        viewModelScope.launch {
            val status = batteryOptimizer.updateBatteryStatus()
            _batteryStatus.value = status
        }
    }
    
    /**
     * Update network status
     */
    fun updateNetworkStatus() {
        viewModelScope.launch {
            val status = appOptimizer.getNetworkStatus()
            _networkStatus.value = status
        }
    }
    
    /**
     * Update cache statistics
     */
    fun updateCacheStats() {
        viewModelScope.launch {
            val stats = cacheManager.getCacheStats()
            _cacheStats.value = stats
        }
    }
    
    /**
     * Clear all cache
     */
    fun clearCache() {
        viewModelScope.launch {
            _optimizationProgress.value = 10
            cacheManager.clearSpecificCache(CacheType.ALL)
            _optimizationProgress.value = 100
            
            // Update stats after clearing
            updateCacheStats()
            
            // Reset progress after delay
            delay(500)
            _optimizationProgress.value = 0
        }
    }
    
    /**
     * Clear specific cache type
     */
    fun clearCacheType(type: CacheType) {
        viewModelScope.launch {
            cacheManager.clearSpecificCache(type)
            updateCacheStats()
        }
    }
    
    /**
     * Toggle data saver mode
     */
    fun setDataSaverMode(enabled: Boolean) {
        _dataSaverMode.value = enabled
    }
    
    /**
     * Set animation quality level
     */
    fun setAnimationQuality(quality: String) {
        _animationQuality.value = quality
    }
    
    /**
     * Run full optimization
     */
    fun runFullOptimization() {
        viewModelScope.launch {
            _optimizationProgress.value = 10
            
            // Clean cache
            _optimizationProgress.value = 30
            cacheManager.cleanCacheIfNeeded()
            
            // Update battery optimization
            _optimizationProgress.value = 50
            batteryOptimizer.scheduleBackgroundSync()
            
            // Update network optimization
            _optimizationProgress.value = 70
            updateNetworkStatus()
            
            // Update stats
            _optimizationProgress.value = 90
            updateCacheStats()
            
            _optimizationProgress.value = 100
            delay(500)
            _optimizationProgress.value = 0
        }
    }
    
    /**
     * Get optimization recommendation based on current state
     */
    fun getOptimizationRecommendation(): String {
        return when {
            _batteryStatus.value == BatteryStatus.CRITICAL -> {
                "Battery is critical! Enable extreme power saving mode."
            }
            _batteryStatus.value == BatteryStatus.LOW -> {
                "Battery is low. Consider enabling data saver mode."
            }
            _cacheStats.value.percentUsed > 80 -> {
                "Cache is getting full. Clear some cache to improve performance."
            }
            _networkStatus.value == NetworkStatus.MOBILE_DATA && !_dataSaverMode.value -> {
                "You're on mobile data. Enable data saver to reduce usage."
            }
            else -> "Everything optimized! ✓"
        }
    }
}
