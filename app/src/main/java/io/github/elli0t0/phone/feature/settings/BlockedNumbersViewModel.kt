package io.github.elli0t0.phone.feature.settings

import android.app.Application
import android.content.ContentValues
import android.provider.BlockedNumberContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BlockedNumbersUiState(
    val numbers: List<String> = emptyList(),
    val canBlock: Boolean = false,
    val isLoading: Boolean = true
)

// Blocked numbers backed by the platform provider. All operations go through
// BlockedNumberContract, so blocked callers are rejected by the platform
// itself. canCurrentUserBlockNumbers gates every operation: without the
// default dialer role the provider throws SecurityException.
class BlockedNumbersViewModel(
    private val app: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(BlockedNumbersUiState())
    val uiState: StateFlow<BlockedNumbersUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val canBlock = BlockedNumberContract.canCurrentUserBlockNumbers(app)
                    val numbers = if (canBlock) queryNumbers() else emptyList()
                    canBlock to numbers
                }.getOrDefault(false to emptyList())
            }
            _uiState.value = BlockedNumbersUiState(
                numbers = result.second,
                canBlock = result.first,
                isLoading = false
            )
        }
    }

    fun blockNumber(number: String) {
        val trimmed = number.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    val values = ContentValues().apply {
                        put(
                            BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER,
                            trimmed
                        )
                    }
                    app.contentResolver.insert(
                        BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                        values
                    )
                }
            }
            refresh()
        }
    }

    fun unblockNumber(number: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching { BlockedNumberContract.unblock(app, number) }
            }
            refresh()
        }
    }

    private fun queryNumbers(): List<String> {
        val numbers = mutableListOf<String>()
        val cursor = app.contentResolver.query(
            BlockedNumberContract.BlockedNumbers.CONTENT_URI,
            arrayOf(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER),
            null,
            null,
            null
        )
        cursor?.use {
            val index = it.getColumnIndex(
                BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER
            )
            while (it.moveToNext()) {
                if (index >= 0) {
                    it.getString(index)?.takeIf { value -> value.isNotBlank() }?.let(numbers::add)
                }
            }
        }
        return numbers
    }

    companion object {
        fun provideFactory(app: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return BlockedNumbersViewModel(app) as T
                }
            }
    }
}
