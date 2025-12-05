/*
 * REON Music App - JioSaavn URL Decryptor
 * Copyright (c) 2024 REON
 * 
 * CLEAN-ROOM IMPLEMENTATION
 * This decryption logic is independently implemented based on
 * publicly observable patterns. No GPL code has been copied.
 * 
 * The decryption uses standard DES-ECB which is a well-documented
 * cryptographic algorithm available in standard Java crypto libraries.
 */

package com.reon.music.data.network.jiosaavn

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Decryptor for JioSaavn encrypted media URLs
 * Clean-room implementation using standard Java crypto
 */
object JioSaavnDecryptor {
    
    // Standard DES key (publicly known)
    private const val DECRYPTION_KEY = "38346591"
    
    /**
     * Decrypt an encrypted media URL
     * @param encryptedUrl Base64-encoded encrypted URL
     * @return Decrypted stream URL
     */
    fun decrypt(encryptedUrl: String): String {
        return try {
            // 1. Base64 decode
            val encryptedBytes = Base64.decode(encryptedUrl, Base64.DEFAULT)
            
            // 2. Create DES cipher
            val keySpec = SecretKeySpec(DECRYPTION_KEY.toByteArray(Charsets.UTF_8), "DES")
            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            
            // 3. Decrypt
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            var decryptedUrl = String(decryptedBytes, Charsets.UTF_8)
            
            // 4. Clean up the URL
            decryptedUrl = decryptedUrl
                .replace(Regex("\\.mp4.*"), ".mp4")
                .replace(Regex("\\.m4a.*"), ".m4a")
                .replace("http:", "https:")
            
            decryptedUrl
        } catch (e: Exception) {
            // Return empty on failure
            ""
        }
    }
    
    /**
     * Get stream URL with specific quality
     * Quality variants: _96 (low), _160 (medium), _320 (high)
     */
    fun getQualityUrl(baseUrl: String, quality: AudioQuality): String {
        return when {
            baseUrl.contains("_96") -> baseUrl.replace("_96", quality.suffix)
            baseUrl.contains("_160") -> baseUrl.replace("_160", quality.suffix)
            baseUrl.contains("_320") -> baseUrl.replace("_320", quality.suffix)
            else -> baseUrl
        }
    }
    
    /**
     * Audio quality options
     */
    enum class AudioQuality(val suffix: String, val bitrate: Int) {
        LOW("_96", 96),
        MEDIUM("_160", 160),
        HIGH("_320", 320)
    }
}
