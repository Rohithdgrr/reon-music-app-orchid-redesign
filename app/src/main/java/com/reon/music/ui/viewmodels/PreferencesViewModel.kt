package com.reon.music.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reon.music.core.preferences.MusicSource
import com.reon.music.core.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PreferencesViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _preferredSource = MutableStateFlow(MusicSource.BOTH)
    val preferredSource: StateFlow<MusicSource> = _preferredSource

    init {
        viewModelScope.launch {
            userPreferences.preferredSource.collectLatest {
                _preferredSource.value = it
            }
        }
    }

    fun setPreferredSource(source: MusicSource) {
        viewModelScope.launch {
            userPreferences.setPreferredSource(source)
        }
    }
}
