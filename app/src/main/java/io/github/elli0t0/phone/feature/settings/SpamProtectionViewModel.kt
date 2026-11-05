package io.github.elli0t0.phone.feature.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.elli0t0.phone.PhoneApplication
import io.github.elli0t0.phone.data.spam.SpamNumberEntity
import io.github.elli0t0.phone.data.spam.SpamRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Spam list state for Settings > Spam protection. The toggles and
// the blocked-call counter live in AppSettings (SettingsViewModel);
// this ViewModel only owns the reported-numbers list.
class SpamProtectionViewModel(
    private val spamRepository: SpamRepository
) : ViewModel() {

    val entries: StateFlow<List<SpamNumberEntity>> =
        spamRepository.spamEntries()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun reportSpam(number: String, reason: String = "") {
        viewModelScope.launch { spamRepository.reportSpam(number, reason) }
    }

    fun removeSpam(entry: SpamNumberEntity) {
        viewModelScope.launch { spamRepository.removeSpam(entry.numberNormalized) }
    }

    companion object {
        fun provideFactory(app: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SpamProtectionViewModel(
                        (app as PhoneApplication).container.spamRepository
                    ) as T
                }
            }
    }
}
