package com.reon.music.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.reon.music.core.preferences.MusicSource
import com.reon.music.ui.viewmodels.PreferencesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(
    preferencesViewModel: PreferencesViewModel = hiltViewModel()
) {
    val preferredSource by preferencesViewModel.preferredSource.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Preferences",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Preferred Music Source",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )

            SourceOptionRow(
                option = MusicSource.JIOSAAVN,
                selected = preferredSource == MusicSource.JIOSAAVN,
                onSelect = { preferencesViewModel.setPreferredSource(MusicSource.JIOSAAVN) }
            )
            SourceOptionRow(
                option = MusicSource.YOUTUBE,
                selected = preferredSource == MusicSource.YOUTUBE,
                onSelect = { preferencesViewModel.setPreferredSource(MusicSource.YOUTUBE) }
            )
            SourceOptionRow(
                option = MusicSource.BOTH,
                selected = preferredSource == MusicSource.BOTH,
                onSelect = { preferencesViewModel.setPreferredSource(MusicSource.BOTH) }
            )
        }
    }
}

@Composable
private fun SourceOptionRow(option: MusicSource, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = option.label, fontSize = 16.sp)
        if (selected) {
            Spacer(modifier = Modifier.weight(1f))
            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}
