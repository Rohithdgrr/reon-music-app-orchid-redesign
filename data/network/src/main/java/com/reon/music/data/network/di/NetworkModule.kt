/*
 * REON Music App - Network Module
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 */

package com.reon.music.data.network.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }
    
    @Provides
    @Singleton
    fun provideHttpClient(
        json: Json,
        @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context
    ): HttpClient = HttpClient(OkHttp) {
        expectSuccess = false
        
        engine {
            config {
                connectTimeout(30, TimeUnit.SECONDS)
                readTimeout(30, TimeUnit.SECONDS)
                writeTimeout(30, TimeUnit.SECONDS)
                
                // Add cache for API responses (50MB)
                val cacheDir = java.io.File(context.cacheDir, "http_cache")
                val cacheSize = 50L * 1024L * 1024L // 50 MB
                cache(okhttp3.Cache(cacheDir, cacheSize))
            }
        }
        
        install(ContentNegotiation) {
            json(json)
        }
        
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.INFO // Enabled for debugging
        }
        
        install(HttpTimeout) {
            requestTimeoutMillis = 60000
            connectTimeoutMillis = 30000
            socketTimeoutMillis = 60000
        }
        
        defaultRequest {
            headers.append("Accept", "application/json")
            // Add cache control headers for better caching
            headers.append("Cache-Control", "max-age=300, stale-while-revalidate=600")
        }
    }
}
