/*
 * REON Music App - Crash Analytics (Sentry Integration)
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 * 
 * Only enabled in Full/Paid version, not in FOSS
 */

package com.reon.music.services

import android.content.Context
import android.util.Log
import com.reon.music.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Crash Analytics Manager
 * Handles crash reporting and performance monitoring
 * Only active in Full version (not FOSS)
 */
@Singleton
class CrashAnalyticsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "CrashAnalytics"
        
        // Sentry DSN would be stored in BuildConfig for Full flavor
        // private const val SENTRY_DSN = BuildConfig.SENTRY_DSN
    }
    
    private var isEnabled: Boolean = false
    private var userId: String? = null
    
    /**
     * Initialize crash reporting
     * Only initializes if app is Full version and user consented
     */
    fun initialize(consentGiven: Boolean) {
        // Only enable in Full flavor, not FOSS
        if (!isFossVersion() && consentGiven) {
            try {
                // In production, would initialize Sentry here:
                // SentryAndroid.init(context) { options ->
                //     options.dsn = SENTRY_DSN
                //     options.isEnableAutoSessionTracking = true
                //     options.tracesSampleRate = 0.3
                //     options.isAttachStacktrace = true
                // }
                
                isEnabled = true
                Log.d(TAG, "Crash analytics initialized")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize crash analytics", e)
            }
        } else {
            Log.d(TAG, "Crash analytics disabled (FOSS or no consent)")
        }
    }
    
    /**
     * Set user identifier (anonymized)
     */
    fun setUserId(id: String) {
        if (!isEnabled) return
        
        userId = id
        // Sentry.setUser(User().apply { this.id = id })
    }
    
    /**
     * Log a non-fatal error
     */
    fun logError(throwable: Throwable, context: Map<String, Any> = emptyMap()) {
        if (!isEnabled) {
            Log.e(TAG, "Error (not sent - analytics disabled)", throwable)
            return
        }
        
        // Would send to Sentry in production:
        // Sentry.captureException(throwable) { scope ->
        //     context.forEach { (key, value) ->
        //         scope.setExtra(key, value.toString())
        //     }
        // }
        
        Log.e(TAG, "Error logged: ${throwable.message}", throwable)
    }
    
    /**
     * Log a message/breadcrumb
     */
    fun logMessage(message: String, level: LogLevel = LogLevel.INFO) {
        if (!isEnabled) {
            Log.d(TAG, "Message (not sent): $message")
            return
        }
        
        // Would add breadcrumb to Sentry:
        // Sentry.addBreadcrumb(Breadcrumb().apply {
        //     this.message = message
        //     this.level = level.toSentryLevel()
        // })
        
        when (level) {
            LogLevel.DEBUG -> Log.d(TAG, message)
            LogLevel.INFO -> Log.i(TAG, message)
            LogLevel.WARNING -> Log.w(TAG, message)
            LogLevel.ERROR -> Log.e(TAG, message)
        }
    }
    
    /**
     * Set custom tag
     */
    fun setTag(key: String, value: String) {
        if (!isEnabled) return
        // Sentry.setTag(key, value)
    }
    
    /**
     * Start a performance transaction
     */
    fun startTransaction(name: String, operation: String): TransactionHandle {
        if (!isEnabled) return TransactionHandle.NOOP
        
        // Would create Sentry transaction:
        // val transaction = Sentry.startTransaction(name, operation)
        // return SentryTransactionHandle(transaction)
        
        return TransactionHandle.NOOP
    }
    
    /**
     * Check if current build is FOSS version
     */
    private fun isFossVersion(): Boolean {
        // In production, this would check BuildConfig.FLAVOR
        // return BuildConfig.FLAVOR == "foss"
        return false // Default to non-FOSS for development
    }
    
    /**
     * Disable analytics
     */
    fun disable() {
        isEnabled = false
        // Sentry.close()
    }
}

enum class LogLevel {
    DEBUG, INFO, WARNING, ERROR
}

/**
 * Handle for performance transactions
 */
interface TransactionHandle {
    fun setTag(key: String, value: String)
    fun finish()
    
    companion object {
        val NOOP = object : TransactionHandle {
            override fun setTag(key: String, value: String) {}
            override fun finish() {}
        }
    }
}
