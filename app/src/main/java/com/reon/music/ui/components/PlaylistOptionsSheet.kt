/*
 * REON Music App - Unified Playlist Options Sheet
 * Copyright (c) 2024 REON
 * Reusable bottom sheet for playlist options across all screens
 */

package com.reon.music.ui.components

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.reon.music.data.database.entities.PlaylistEntity

// Color constants
private val BackgroundWhite = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF1C1C1C)
private val TextSecondary = Color(0xFF757575)
private val AccentRed = Color(0xFFE53935)
private val SurfaceLight = Color(0xFFFAFAFA)
private val AccentGreen = Color(0xFF43A047)

/**
 * Unified Playlist Options Bottom Sheet
 * Use this component across all screens for consistent playlist menu behavior
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistOptionsSheet(
    playlist: PlaylistEntity,
    isDownloaded: Boolean = false,
    showEditOption: Boolean = true,
    showDeleteOption: Boolean = true,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onAddToQueue: () -> Unit,
    onDownloadAll: () -> Unit = {},
    onRemoveDownload: () -> Unit = {},
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onShare: () -> Unit
) {
    val context = LocalContext.current
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BackgroundWhite,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .padding(0.dp),
                contentAlignment = Alignment.Center
            ) {
                Divider(
                    modifier = Modifier.fillMaxSize(),
                    color = TextSecondary.copy(alpha = 0.3f)
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Playlist Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Playlist thumbnail or icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (playlist.thumbnailUrl != null) {
                        AsyncImage(
                            model = playlist.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = AccentRed.copy(alpha = 0.1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.QueueMusic,
                                contentDescription = null,
                                tint = AccentRed,
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxSize()
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playlist.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = TextPrimary
                    )
                    Text(
                        text = "${playlist.songCount} songs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    playlist.description?.let { desc ->
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            
            HorizontalDivider(color = SurfaceLight)
            
            // Playback Options
            PlaylistOptionItem(
                icon = Icons.Filled.PlayArrow,
                title = "Play",
                onClick = {
                    onPlay()
                    onDismiss()
                }
            )
            PlaylistOptionItem(
                icon = Icons.Filled.Shuffle,
                title = "Shuffle Play",
                onClick = {
                    onShuffle()
                    onDismiss()
                }
            )
            PlaylistOptionItem(
                icon = Icons.Outlined.QueueMusic,
                title = "Add to Queue",
                onClick = {
                    onAddToQueue()
                    onDismiss()
                }
            )
            
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                color = SurfaceLight
            )
            
            // Download Option
            if (isDownloaded) {
                PlaylistOptionItem(
                    icon = Icons.Filled.DownloadDone,
                    title = "Downloaded",
                    subtitle = "Tap to remove all downloads",
                    iconTint = AccentGreen,
                    onClick = {
                        onRemoveDownload()
                        onDismiss()
                    }
                )
            } else {
                PlaylistOptionItem(
                    icon = Icons.Outlined.Download,
                    title = "Download All",
                    subtitle = "Save for offline playback",
                    onClick = {
                        onDownloadAll()
                        onDismiss()
                    }
                )
            }
            
            // Edit Option
            if (showEditOption) {
                PlaylistOptionItem(
                    icon = Icons.Outlined.Edit,
                    title = "Edit Playlist",
                    onClick = {
                        onEdit()
                        onDismiss()
                    }
                )
            }
            
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                color = SurfaceLight
            )
            
            // Share
            PlaylistOptionItem(
                icon = Icons.Outlined.Share,
                title = "Share",
                onClick = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "Check out my playlist: ${playlist.title}")
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Playlist"))
                    onDismiss()
                }
            )
            
            // Delete Option
            if (showDeleteOption) {
                PlaylistOptionItem(
                    icon = Icons.Outlined.Delete,
                    title = "Delete Playlist",
                    iconTint = AccentRed,
                    textColor = AccentRed,
                    onClick = {
                        onDelete()
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun PlaylistOptionItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    iconTint: Color = TextPrimary,
    textColor: Color = TextPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}
