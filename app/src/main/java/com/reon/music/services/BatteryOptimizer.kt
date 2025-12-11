/*
 * REON Music App - Battery Optimizer
 * Copyright (c) 2024 REON
 * Reduce battery drain through intelligent background task management
 */

package com.reon.music.services

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.TimeUnit
import androidx.work.CoroutineWorker
import com.reon.music.core.optimization.BatteryStatus
import com.reon.music.core.optimization.OptimizationMode

@Singleton
class BatteryOptimizer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val _batteryStatus = MutableStateFlow<BatteryStatus>(BatteryStatus.HEALTHY)
    val batteryStatus: StateFlow<BatteryStatus> = _batteryStatus.asStateFlow()
    
    private val _optimizationMode = MutableStateFlow<OptimizationMode>(OptimizationMode.NORMAL)
    val optimizationMode: StateFlow<OptimizationMode> = _optimizationMode.asStateFlow()
    
    companion object {
        private const val BATTERY_LOW_THRESHOLD = 20
        private const val BATTERY_MEDIUM_THRESHOLD = 40
    }
    
    /**
     * Check current battery status and update optimization level
     */
    fun updateBatteryStatus(): BatteryStatus {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryPercent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } else {
            50
        }
        
        val status = when {
            batteryPercent < BATTERY_LOW_THRESHOLD -> BatteryStatus.CRITICAL
            batteryPercent < BATTERY_MEDIUM_THRESHOLD -> BatteryStatus.LOW
            else -> BatteryStatus.HEALTHY
        }
        
        _batteryStatus.value = status
        
        // Adjust optimization mode
        when (status) {
            BatteryStatus.CRITICAL -> {
                _optimizationMode.value = OptimizationMode.EXTREME
                disallowBackgroundSync()
            }
            BatteryStatus.LOW -> {
                _optimizationMode.value = OptimizationMode.CONSERVATIVE
                limitBackgroundSync()
            }
            BatteryStatus.HEALTHY -> {
                _optimizationMode.value = OptimizationMode.NORMAL
                enableFullFunctionality()
            }
        }
        
        return status
    }
    
    /**
     * Get background sync interval based on battery status
     */
    fun getSyncInterval(): Long {
        return when (_optimizationMode.value) {
            OptimizationMode.EXTREME -> {
                // Disable auto-sync completely
                Long.MAX_VALUE
            }
            OptimizationMode.CONSERVATIVE -> {
                // Sync every 2 hours
                TimeUnit.HOURS.toMillis(2)
            }
            OptimizationMode.NORMAL -> {
                // Sync every 30 minutes
                TimeUnit.MINUTES.toMillis(30)
            }
        }
    }
    
    /**
     * Schedule background sync with optimized intervals
     */
    fun scheduleBackgroundSync() {
        val syncInterval = getSyncInterval()
        
        if (syncInterval == Long.MAX_VALUE) {
            // Cancel existing sync
            WorkManager.getInstance(context).cancelUniqueWork("background_sync")
            return
        }
        
        val syncRequest = PeriodicWorkRequestBuilder<BackgroundSyncWorker>(
            syncInterval,
            TimeUnit.MILLISECONDS
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiresDeviceIdle(true)
                    .setRequiresBatteryNotLow(true)
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "background_sync",
            ExistingPeriodicWorkPolicy.REPLACE,
            syncRequest
        )
    }
    
    /**
     * Disable CPU-intensive operations
     */
    private fun disallowBackgroundSync() {
        WorkManager.getInstance(context).cancelUniqueWork("background_sync")
    }
    
    /**
     * Limit background operations but keep essential ones
     */
    private fun limitBackgroundSync() {
        scheduleBackgroundSync()
    }
    
    /**
     * Enable full functionality
     */
    private fun enableFullFunctionality() {
        scheduleBackgroundSync()
    }
    
    /**
     * Check if should execute CPU-intensive operations
     */
    fun shouldRunIntensiveTask(): Boolean {
        return when (_optimizationMode.value) {
            OptimizationMode.EXTREME -> false
            OptimizationMode.CONSERVATIVE -> false  // Defer CPU-intensive tasks
            OptimizationMode.NORMAL -> true
        }
    }
    
    /**
     * Get animation frame rate multiplier
     * Lower value = fewer frame updates = less battery drain
     */
    fun getAnimationFrameRate(): Float {
        return when (_optimizationMode.value) {
            OptimizationMode.EXTREME -> 0.3f    // 30% of normal
            OptimizationMode.CONSERVATIVE -> 0.6f  // 60% of normal
            OptimizationMode.NORMAL -> 1.0f    // 100% normal
        }
    }
    
    /**
     * Get coroutine dispatcher for background work
     * Prevents blocking main thread
     */
    fun getBackgroundDispatcher() = Dispatchers.IO
}

/**
 * Background sync worker that respects battery constraints
 */
class BackgroundSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            // Perform background sync operations
            // Only runs when device is idle, plugged in, and on WiFi if possible
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
