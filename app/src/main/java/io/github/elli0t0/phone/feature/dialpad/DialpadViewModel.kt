package io.github.elli0t0.phone.feature.dialpad

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DialpadViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DialpadUiState())
    val uiState: StateFlow<DialpadUiState> = _uiState.asStateFlow()
}
