/*
 * REON Music App - Source Toggle Component
 * Copyright (c) 2024 REON
 * Clean-room implementation - No GPL code included
 */

package com.reon.music.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reon.music.core.preferences.MusicSource

@Composable
fun SourceToggle(
    currentSource: MusicSource,
    onSourceSelected: (MusicSource) -> Unit,
    modifier: Modifier = Modifier,
    useIcons: Boolean = true
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(50)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SourceOption(
                selected = currentSource == MusicSource.JIOSAAVN,
                label = "JS",
                fullLabel = "JioSaavn",
                onClick = { onSourceSelected(MusicSource.JIOSAAVN) },
                useIcons = useIcons
            )
            
            SourceOption(
                selected = currentSource == MusicSource.YOUTUBE,
                label = "YT",
                fullLabel = "YouTube",
                onClick = { onSourceSelected(MusicSource.YOUTUBE) },
                useIcons = useIcons
            )
            
            SourceOption(
                selected = currentSource == MusicSource.BOTH,
                label = "All",
                fullLabel = "Both",
                onClick = { onSourceSelected(MusicSource.BOTH) },
                useIcons = useIcons
            )
        }
    }
}

@Composable
private fun SourceOption(
    selected: Boolean,
    label: String,
    fullLabel: String,
    onClick: () -> Unit,
    useIcons: Boolean
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "bgColor"
    )
    
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "contentColor"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (useIcons) label else fullLabel,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp
            ),
            color = contentColor
        )
    }
}
