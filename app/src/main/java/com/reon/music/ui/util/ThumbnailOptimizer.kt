/*
 * REON Music App - Thumbnail Loading Utilities
 * Copyright (c) 2024 REON
 * High-quality, fast thumbnail loading with aggressive caching
 */

package com.reon.music.ui.util

import android.net.Uri

/**
 * Utility object for YouTube thumbnail URL optimization
 * Provides highest quality thumbnails with fast loading
 */
object ThumbnailOptimizer {
    
    /**
     * Get highest quality YouTube thumbnail URL
     * YouTube thumbnail quality hierarchy (highest to lowest):
     * - maxresdefault.jpg (1920x1080) - Best quality
     * - sddefault.jpg (640x480) - High quality
     * - hqdefault.jpg (480x360) - Medium quality
     * - mqdefault.jpg (320x180) - Low quality
     * - default.jpg (120x90) - Minimal quality
     */
    fun getHighestQualityThumbnail(videoId: String): String {
        return "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg"
    }
    
    /**
     * Get fallback thumbnails in order of quality preference
     * Used if primary URL returns 404
     */
    fun getFallbackThumbnails(videoId: String): List<String> {
        return listOf(
            "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg",
            "https://i.ytimg.com/vi/$videoId/sddefault.jpg",
            "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
            "https://i.ytimg.com/vi/$videoId/mqdefault.jpg",
            "https://i.ytimg.com/vi/$videoId/default.jpg"
        )
    }
    
    /**
     * Optimize existing YouTube thumbnail URL to highest quality
     * Converts small thumbnails (w60-h60, default, etc.) to maxresdefault
     */
    fun optimizeYouTubeThumbnail(url: String?): String? {
        if (url == null || url.isBlank()) return null

        // Validate that it's a proper URL, not a local file path
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return null
        }

        // If it's a YouTube CDN URL
        if (url.contains("ytimg.com") || url.contains("ggpht.com")) {
            // Extract video ID if possible from URL patterns
            val videoIdFromUrl = extractVideoIdFromUrl(url)
            if (videoIdFromUrl != null) {
                // Use highest quality direct URL
                return getHighestQualityThumbnail(videoIdFromUrl)
            }

            // Fallback: optimize current URL string replacements
            return url
                .replace(Regex("w\\d+-h\\d+"), "w1200-h1200")  // Upgrade size parameters
                .replace("mqdefault.jpg", "maxresdefault.jpg")
                .replace("default.jpg", "maxresdefault.jpg")
                .replace("hqdefault.jpg", "maxresdefault.jpg")
                .replace("sddefault.jpg", "maxresdefault.jpg")
                // Ensure we end up with maxresdefault if no filename extension
                .let {
                    if (!it.contains("maxresdefault") && !it.contains(".jpg") && it.endsWith("/")) {
                        "${it}maxresdefault.jpg"
                    } else {
                        it
                    }
                }
        }

        // For other CDNs, return as-is with https upgrade
        return url.replace("http:", "https:")
    }
    
    /**
     * Check if a YouTube video has maxresdefault thumbnail available
     * Note: This requires a network request and should be used sparingly
     * Most music videos from official channels have maxresdefault
     */
    fun hasMaxResDefault(videoId: String): Boolean {
        // maxresdefault is typically available for videos 720p and higher
        // Most official music videos have it, but user uploads may not
        // For now, we assume it exists and let the UI handle 404 fallback
        return true
    }

    /**
     * Extract video ID from various YouTube URL patterns
     */
    private fun extractVideoIdFromUrl(url: String): String? {
        // Pattern: /vi/[videoId]/ or /vi_[videoId]/ or yt_id=[videoId]
        val patterns = listOf(
            Regex("/vi/([a-zA-Z0-9_-]{11})"),
            Regex("/vi_/([a-zA-Z0-9_-]{11})"),
            Regex("vi_id=([a-zA-Z0-9_-]{11})"),
            Regex("id=([a-zA-Z0-9_-]{11})")
        )
        
        for (pattern in patterns) {
            val match = pattern.find(url)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        return null
    }
    
    /**
     * Check if URL is from a known CDN (cached)
     */
    fun isFromCDN(url: String?): Boolean {
        if (url == null) return false
        return url.contains("ytimg.com") || 
               url.contains("ggpht.com") ||
               url.contains("i.ytimg.com") ||
               url.contains("lh3.googleusercontent.com")
    }
    
    /**
     * Add cache-busting query params if needed
     * YouTube CDN usually has aggressive caching, but can force refresh if needed
     */
    fun addCacheParams(url: String?): String? {
        if (url == null || url.isBlank()) return null
        return url  // YouTube CDN is already very stable, no need to add cache params
    }
}
