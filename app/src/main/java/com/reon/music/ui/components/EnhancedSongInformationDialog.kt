/*
 * REON Music App - Enhanced Song Information Dialog
 * Copyright (c) 2024 REON
 * Comprehensive metadata display with real data
 */

package com.reon.music.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.reon.music.core.model.Song
import com.reon.music.data.network.youtube.IndianMusicChannels
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

// Claymorphism Colors
private val ClayBackground = Color(0xFFF5F5F0)
private val ClayCardLight = Color(0xFFFFFFFF)
private val ClayShadowDark = Color(0xFFC0C0B8)
private val TextPrimary = Color(0xFF2C2C2C)
private val TextSecondary = Color(0xFF6B6B6B)
private val AccentRed = Color(0xFFE74C3C)
private val AccentBlue = Color(0xFF3498DB)
private val AccentGold = Color(0xFFF39C12)
private val AccentGreen = Color(0xFF27AE60)
private val AccentPurple = Color(0xFF9B59B6)

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun EnhancedSongInformationDialog(
    song: Song,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .padding(16.dp)
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(32.dp),
                    ambientColor = ClayShadowDark.copy(alpha = 0.4f),
                    spotColor = ClayShadowDark.copy(alpha = 0.6f)
                ),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = ClayCardLight)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with Song Image
                item {
                    SongInfoHeader(song = song)
                }
                
                // Basic Information Section
                item {
                    InfoSection(
                        title = "Basic Information",
                        icon = Icons.Default.Info,
                        color = AccentBlue
                    ) {
                        InfoRow(label = "Title", value = song.title)
                        InfoRow(label = "Artist", value = song.artist)
                        InfoRow(label = "Album", value = song.album.takeIf { it.isNotBlank() } ?: "Single")
                        InfoRow(label = "Duration", value = song.formattedDuration())
                        InfoRow(label = "Year", value = song.year.takeIf { it.isNotBlank() } ?: "Unknown")
                        InfoRow(label = "Genre", value = song.genre.takeIf { it.isNotBlank() } ?: "Music")
                        InfoRow(label = "Type", value = song.type.takeIf { it.isNotBlank() } ?: "Track")
                        InfoRow(
                            label = "Language", 
                            value = song.language.takeIf { it.isNotBlank() } 
                                ?: detectLanguageFromTitle(song.title)
                        )
                        InfoRow(
                            label = "Category", 
                            value = song.extras["category"] ?: "Music Video"
                        )
                    }
                }
                
                // Movie/Track Information
                if (song.movieName.isNotBlank() || song.movieGenre.isNotBlank()) {
                    item {
                        InfoSection(
                            title = "Movie/Film Information",
                            icon = Icons.Default.Movie,
                            color = AccentPurple
                        ) {
                            if (song.movieName.isNotBlank()) {
                                InfoRow(label = "Movie Name", value = song.movieName)
                            }
                            if (song.movieGenre.isNotBlank()) {
                                InfoRow(label = "Movie Genre", value = song.movieGenre)
                            }
                            if (song.heroName.isNotBlank()) {
                                InfoRow(label = "Hero", value = song.heroName)
                            }
                            if (song.heroineName.isNotBlank()) {
                                InfoRow(label = "Heroine", value = song.heroineName)
                            }
                            if (song.director.isNotBlank()) {
                                InfoRow(label = "Director", value = song.director)
                            }
                            if (song.producer.isNotBlank()) {
                                InfoRow(label = "Producer", value = song.producer)
                            }
                        }
                    }
                }
                
                // YouTube/Channel Information
                item {
                    InfoSection(
                        title = "YouTube Channel Information",
                        icon = Icons.Default.VideoLibrary,
                        color = AccentRed
                    ) {
                        InfoRow(
                            label = "Channel", 
                            value = song.channelName.takeIf { it.isNotBlank() } ?: "Unknown Channel"
                        )
                        InfoRow(label = "Channel ID", value = song.channelId.takeIf { it.isNotBlank() } ?: "N/A")
                        InfoRow(
                            label = "Subscribers", 
                            value = formatCount(song.channelSubscriberCount)
                        )
                        InfoRow(label = "Views", value = formatCount(song.viewCount))
                        InfoRow(label = "Likes", value = formatCount(song.likeCount))
                        InfoRow(
                            label = "Upload Date", 
                            value = formatUploadDate(song.uploadDate)
                        )
                        InfoRow(
                            label = "Quality", 
                            value = song.quality.takeIf { it.isNotBlank() } ?: "Standard"
                        )
                        
                        // Verified badge if from Top 500
                        val channel = IndianMusicChannels.getChannelByName(song.channelName)
                        if (channel != null) {
                            RankInfoRow(
                                label = "Channel Rank",
                                rank = channel.rank,
                                isVerified = true
                            )
                        }
                    }
                }
                
                // Studios & Labels
                item {
                    InfoSection(
                        title = "Studios & Labels",
                        icon = Icons.Default.Business,
                        color = AccentGold
                    ) {
                        InfoRow(
                            label = "Music Label", 
                            value = song.extras["musicLabel"] ?: "Independent"
                        )
                        InfoRow(
                            label = "Studio", 
                            value = song.extras["studio"] ?: "Not Specified"
                        )
                        InfoRow(
                            label = "Production", 
                            value = song.extras["production"] ?: "Not Specified"
                        )
                        InfoRow(
                            label = "Publisher", 
                            value = song.extras["publisher"] ?: "Not Specified"
                        )
                        InfoRow(
                            label = "Distributor", 
                            value = song.extras["distributor"] ?: "YouTube"
                        )
                    }
                }
                
                // Region & Language Details
                item {
                    InfoSection(
                        title = "Region & Language",
                        icon = Icons.Default.Public,
                        color = AccentGreen
                    ) {
                        val region = song.extras["region"] ?: detectRegion(song.language, song.title)
                        val state = song.extras["state"] ?: detectState(region)
                        val language = song.language.takeIf { it.isNotBlank() } 
                            ?: detectLanguageFromTitle(song.title)
                        
                        InfoRow(label = "Language", value = language)
                        InfoRow(label = "Region", value = region)
                        InfoRow(label = "State", value = state)
                        
                        // Hashtags/Tags
                        val tags = song.extras["tags"] ?: generateTags(song)
                        if (tags.isNotBlank()) {
                            TagsRow(label = "Tags/Hashtags", tags = tags)
                        }
                    }
                }
                
                // Technical Details
                item {
                    InfoSection(
                        title = "Technical Details",
                        icon = Icons.Default.Settings,
                        color = Color(0xFF607D8B)
                    ) {
                        InfoRow(
                            label = "Audio Quality", 
                            value = if (song.is320kbps) "320kbps High Quality" else "Standard"
                        )
                        InfoRow(label = "Source", value = song.source.capitalize())
                        InfoRow(label = "Video ID", value = song.id.take(12) + "...")
                        InfoRow(
                            label = "Has Lyrics", 
                            value = if (song.hasLyrics) "Yes" else "No"
                        )
                    }
                }
                
                // Description
                if (song.description.isNotBlank()) {
                    item {
                        InfoSection(
                            title = "Description",
                            icon = Icons.Default.Description,
                            color = Color(0xFF795548)
                        ) {
                            Text(
                                text = song.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
                
                // Close Button
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(8.dp, RoundedCornerShape(28.dp)),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentRed
                        )
                    ) {
                        Text(
                            text = "Close",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SongInfoHeader(song: Song) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Song Image with Claymorphism
        Card(
            modifier = Modifier
                .size(200.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(24.dp)
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ClayCardLight)
        ) {
            AsyncImage(
                model = song.getHighQualityArtwork() ?: song.artworkUrl,
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Song Title
        Text(
            text = song.title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp
            ),
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        
        // Artist
        Text(
            text = song.artist,
            style = MaterialTheme.typography.titleMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Stats Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (song.viewCount > 0) {
                StatChip(
                    icon = Icons.Default.Visibility,
                    value = formatCountCompact(song.viewCount),
                    color = AccentBlue
                )
            }
            if (song.likeCount > 0) {
                StatChip(
                    icon = Icons.Default.ThumbUp,
                    value = formatCountCompact(song.likeCount),
                    color = AccentRed
                )
            }
            StatChip(
                icon = Icons.Default.Schedule,
                value = song.formattedDuration(),
                color = AccentGold
            )
        }
    }
}

@Composable
private fun InfoSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Section Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = color
            )
        }
        
        // Content
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ClayBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = TextPrimary,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.6f)
        )
    }
}

@Composable
private fun RankInfoRow(label: String, rank: Int, isVerified: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            val badgeColor = when {
                rank <= 20 -> Color(0xFFFFD700)
                rank <= 50 -> Color(0xFFC0C0C0)
                rank <= 100 -> Color(0xFFCD7F32)
                else -> AccentRed
            }
            
            Box(
                modifier = Modifier
                    .background(badgeColor, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = if (rank <= 50) Color.Black else Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "#$rank",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (rank <= 50) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            if (isVerified) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = "Verified",
                    tint = AccentRed,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsRow(label: String, tags: String) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tags.split(",").forEach { tag ->
                val trimmedTag = tag.trim()
                if (trimmedTag.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = AccentBlue.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "#$trimmedTag",
                            style = MaterialTheme.typography.labelMedium,
                            color = AccentBlue,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = color
            )
        }
    }
}

// Helper Functions
private fun formatCount(count: Long): String {
    return when {
        count >= 1_000_000_000 -> "${String.format("%.2f", count / 1_000_000_000.0)} Billion"
        count >= 1_000_000 -> "${String.format("%.2f", count / 1_000_000.0)} Million"
        count >= 1_000 -> "${String.format("%.1f", count / 1_000.0)}K"
        else -> count.toString()
    }
}

private fun formatCountCompact(count: Long): String {
    return when {
        count >= 1_000_000_000 -> "${String.format("%.1f", count / 1_000_000_000.0)}B"
        count >= 1_000_000 -> "${String.format("%.1f", count / 1_000_000.0)}M"
        count >= 1_000 -> "${String.format("%.1f", count / 1_000.0)}K"
        else -> count.toString()
    }
}

private fun formatUploadDate(dateString: String): String {
    if (dateString.isBlank()) return "Unknown"
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        date?.let { outputFormat.format(it) } ?: dateString
    } catch (e: Exception) {
        dateString
    }
}

private fun detectLanguageFromTitle(title: String): String {
    return when {
        title.contains(Regex("[\u0C00-\u0C7F]")) -> "Telugu"
        title.contains(Regex("[\u0900-\u097F]")) -> "Hindi"
        title.contains(Regex("[\u0B80-\u0BFF]")) -> "Tamil"
        title.contains(Regex("[\u0D00-\u0D7F]")) -> "Malayalam"
        title.contains(Regex("[\u0C80-\u0CFF]")) -> "Kannada"
        title.contains(Regex("[\u0A00-\u0A7F]")) -> "Punjabi"
        title.contains(Regex("[\u0980-\u09FF]")) -> "Bengali"
        else -> "English"
    }
}

private fun detectRegion(language: String, title: String): String {
    return when (language.lowercase()) {
        "telugu" -> "South India"
        "hindi" -> "North India"
        "tamil" -> "South India"
        "malayalam" -> "South India"
        "kannada" -> "South India"
        "punjabi" -> "North India"
        "bengali" -> "East India"
        "marathi" -> "West India"
        "gujarati" -> "West India"
        else -> "International"
    }
}

private fun detectState(region: String): String {
    return when (region) {
        "South India" -> "Andhra Pradesh / Telangana / Tamil Nadu / Kerala / Karnataka"
        "North India" -> "Delhi / Punjab / Haryana / UP"
        "East India" -> "West Bengal / Odisha"
        "West India" -> "Maharashtra / Gujarat"
        else -> "Various"
    }
}

private fun generateTags(song: Song): String {
    val tags = mutableListOf<String>()
    
    // Add based on content
    if (song.movieName.isNotBlank()) tags.add("Movie Song")
    if (song.artist.isNotBlank()) tags.add(song.artist.split(",")[0].trim())
    if (song.language.isNotBlank()) tags.add(song.language)
    if (song.genre.isNotBlank()) tags.add(song.genre)
    
    // Add generic tags
    tags.addAll(listOf("Music", "Audio", "Video", "Entertainment"))
    
    return tags.joinToString(", ")
}

private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}
