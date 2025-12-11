/*
 * REON Music App - Optimization Models
 * Copyright (c) 2024 REON
 * Shared models for optimization features
 */

package com.reon.music.core.optimization

/**
 * Battery status enum
 */
enum class BatteryStatus {
    HEALTHY,
    LOW,
    CRITICAL
}

/**
 * Network status enum
 */
enum class NetworkStatus {
    WIFI,
    MOBILE_DATA,
    OFFLINE,
    UNKNOWN
}

/**
 * Optimization mode enum
 */
enum class OptimizationMode {
    NORMAL,
    CONSERVATIVE,
    EXTREME
}

/**
 * Cache type enum
 */
enum class CacheType {
    IMAGES,
    NETWORK,
    DATABASE,
    ALL
}

/**
 * Cache statistics data class
 */
data class CacheStats(
    val imageCacheMB: Long,
    val httpCacheMB: Long,
    val databaseCacheMB: Long,
    val totalCacheMB: Long,
    val percentUsed: Long
)

/**
 * Refresh preference enum
 */
enum class RefreshPreference {
    AGGRESSIVE,
    NORMAL,
    CONSERVATIVE
}
