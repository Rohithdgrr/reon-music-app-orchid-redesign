package com.reon.music.ui.screens

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SearchViewModel : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query
    fun updateQuery(q: String) { _query.value = q }
    fun performSearch(query: String) { /* placeholder for crash-proof search */ }
}
