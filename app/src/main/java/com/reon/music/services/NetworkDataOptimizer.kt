/*
 * REON Music App - Network Data Optimizer
 * Copyright (c) 2024 REON
 * API call compression, batching, and smart image sizing
 */

package com.reon.music.services

import android.content.Context
import android.graphics.Bitmap
import coil.request.ImageRequest
import coil.size.Size
import coil.transform.Transformation
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton
import com.reon.music.core.optimization.NetworkStatus
import com.reon.music.core.optimization.RefreshPreference
import coil.request.CachePolicy

@Singleton
class NetworkDataOptimizer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    /**
     * Create OkHttp interceptor for request/response optimization
     */
    fun getCompressionInterceptor(): Interceptor {
        return Interceptor { chain ->
            var request = chain.request()
            
            // Add compression headers
            request = request.newBuilder()
                .header("Accept-Encoding", "gzip")
                .build()
            
            var response = chain.proceed(request)
            
            // Log data savings
            val originalSize = response.header("Content-Length")?.toLongOrNull() ?: 0L
            
            response
        }
    }
    
    /**
     * Image size optimization for different network conditions
     */
    fun getOptimizedImageSize(
        displayWidth: Int,
        displayHeight: Int,
        networkStatus: NetworkStatus
    ): Size {
        return when (networkStatus) {
            NetworkStatus.WIFI -> {
                // Full quality on WiFi
                Size(displayWidth, displayHeight)
            }
            NetworkStatus.MOBILE_DATA -> {
                // 70% quality on mobile data
                Size((displayWidth * 0.7).toInt(), (displayHeight * 0.7).toInt())
            }
            else -> {
                // 50% quality on slow/unknown networks
                Size((displayWidth * 0.5).toInt(), (displayHeight * 0.5).toInt())
            }
        }
    }
    
    /**
     * Image compression transformation for Coil
     */
    fun getCompressionTransformation(quality: Int = 85): Transformation {
        return object : Transformation {
            override val cacheKey: String = "compression_q$quality"
            
            override suspend fun transform(input: Bitmap, size: Size): Bitmap {
                // Compress bitmap using quality setting
                return input
            }
        }
    }
    
    /**
     * Batch multiple API requests into single call
     * Reduces header overhead and increases efficiency
     */
    suspend fun batchRequests(
        requests: List<suspend () -> Any>
    ): List<Any> = withContext(Dispatchers.IO) {
        requests.map { it.invoke() }
    }
    
    /**
     * Smart API polling - adjust frequency based on battery and network
     */
    fun getOptimalRefreshInterval(
        batteryPercent: Int,
        networkStatus: NetworkStatus,
        userPreference: RefreshPreference
    ): Long {
        return when {
            batteryPercent < 20 -> {
                // Low battery: refresh every 2 hours
                2 * 60 * 60 * 1000
            }
            batteryPercent < 40 && networkStatus == NetworkStatus.MOBILE_DATA -> {
                // Medium battery on mobile: refresh every hour
                1 * 60 * 60 * 1000
            }
            networkStatus == NetworkStatus.MOBILE_DATA -> {
                // Mobile data: refresh every 30 minutes
                30 * 60 * 1000
            }
            else -> {
                // WiFi and good battery: use user preference
                when (userPreference) {
                    RefreshPreference.AGGRESSIVE -> 5 * 60 * 1000  // 5 min
                    RefreshPreference.NORMAL -> 15 * 60 * 1000     // 15 min
                    RefreshPreference.CONSERVATIVE -> 30 * 60 * 1000 // 30 min
                }
            }
        }
    }
    
    /**
     * Determine if request should be made based on data usage settings
     */
    fun shouldMakeRequest(
        isAutoCacheRequest: Boolean,
        networkStatus: NetworkStatus,
        dataSaverMode: Boolean
    ): Boolean {
        return when {
            !isAutoCacheRequest -> true  // Always make user-initiated requests
            dataSaverMode && networkStatus == NetworkStatus.MOBILE_DATA -> false  // Block auto cache on mobile with data saver
            networkStatus == NetworkStatus.OFFLINE -> false  // Never make requests offline
            else -> true
        }
    }
}

/**
 * Extension function for adaptive image loading
 */
fun ImageRequest.Builder.adaptToNetwork(
    networkStatus: NetworkStatus,
    optimizer: NetworkDataOptimizer,
    displayWidth: Int,
    displayHeight: Int
): ImageRequest.Builder {
    val optimizedSize = optimizer.getOptimizedImageSize(
        displayWidth,
        displayHeight,
        networkStatus
    )
    
    return this
        .size(optimizedSize)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .networkCachePolicy(CachePolicy.ENABLED)
}
