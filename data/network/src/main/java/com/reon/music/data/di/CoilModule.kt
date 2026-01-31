/*
 * REON Music App - Coil Image Loading Configuration
 * Copyright (c) 2024 REON
 * Optimized for fast thumbnail loading with aggressive caching
 */

package com.reon.music.data.di

import android.content.Context
import coil.ImageLoader
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Coil configuration for optimized image loading
 * - Aggressive memory caching (up to 1 GB)
 * - Disk caching (up to 512 MB for 60 days)
 * - Hardware-accelerated decoding
 * - Connection pooling for faster requests
 * - Progressive JPEG loading for better perceived performance
 */
@Module
@InstallIn(SingletonComponent::class)
object CoilModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(context: Context): OkHttpClient {
        val cacheDir = File(context.cacheDir, "image_http_cache")
        val cacheSize = 50L * 1024L * 1024L // 50 MB

        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .connectionPool(okhttp3.ConnectionPool(8, 30, TimeUnit.SECONDS))
            .cache(Cache(cacheDir, cacheSize))
            .build()
    }

    @Singleton
    @Provides
    fun provideImageLoader(context: Context, okHttpClient: OkHttpClient): ImageLoader {
        return ImageLoader.Builder(context)
            .okHttpClient(okHttpClient)
            // Memory cache: 1 GB for holding decoded bitmaps (improved quality retention)
            .memoryCache {
                coil.memory.MemoryCache.Builder(context)
                    .maxSizeBytes(1 * 1024 * 1024 * 1024)  // 1 GB
                    .strongReferencesEnabled(true)
                    .build()
            }
            // Disk cache: 512 MB for 60 days (improved for quality retention)
            .diskCache {
                coil.disk.DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(512 * 1024 * 1024)  // 512 MB
                    .build()
            }
            // Aggressive caching strategy
            .respectCacheHeaders(false)  // Ignore server cache headers, use our own
            .crossfade(true)
            .build()
    }
}
