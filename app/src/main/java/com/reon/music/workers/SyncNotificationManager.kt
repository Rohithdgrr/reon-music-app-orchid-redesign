/*
 * REON Music App - Sync Notification Manager
 * Copyright (c) 2024 REON
 * Manages notifications for content sync updates
 */

package com.reon.music.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.reon.music.R

/**
 * Handles notifications for content sync
 */
object SyncNotificationManager {

    private const val CHANNEL_ID = "content_sync_channel"
    private const val CHANNEL_NAME = "Content Updates"
    private const val NOTIFICATION_ID = 1001

    /**
     * Create notification channel (required for Android O+)
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for content sync and updates"
                setShowBadge(false)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Show sync in progress notification
     */
    fun showSyncInProgress(context: Context) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Syncing content...")
            .setContentText("Updating charts, playlists, and new releases")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    /**
     * Show sync completed notification
     */
    fun showSyncCompleted(
        context: Context,
        chartsUpdated: Int,
        playlistsUpdated: Int,
        newReleasesUpdated: Int
    ) {
        val totalUpdates = chartsUpdated + playlistsUpdated + newReleasesUpdated

        if (totalUpdates == 0) {
            // No updates, dismiss notification
            dismissNotification(context)
            return
        }

        val contentText = buildString {
            if (chartsUpdated > 0) append("$chartsUpdated charts, ")
            if (playlistsUpdated > 0) append("$playlistsUpdated playlists, ")
            if (newReleasesUpdated > 0) append("$newReleasesUpdated new releases")
        }.trimEnd(',', ' ')

        // Create intent to open app
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("New content available!")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    /**
     * Show sync failed notification
     */
    fun showSyncFailed(context: Context, error: String? = null) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Sync failed")
            .setContentText(error ?: "Failed to update content. Will retry later.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    /**
     * Dismiss notification
     */
    fun dismissNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}
