package io.github.elli0t0.phone.feature.recents

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RecentsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(RecentsUiState())
    val uiState: StateFlow<RecentsUiState> = _uiState.asStateFlow()
}
