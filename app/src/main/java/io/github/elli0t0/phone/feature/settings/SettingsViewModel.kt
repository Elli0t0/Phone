package io.github.elli0t0.phone.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.elli0t0.phone.data.settings.SettingsRepository
import io.github.elli0t0.phone.data.settings.model.AnimationOption
import io.github.elli0t0.phone.data.settings.model.AppSettings
import io.github.elli0t0.phone.data.settings.model.CardStyle
import io.github.elli0t0.phone.data.settings.model.ContactNameFormat
import io.github.elli0t0.phone.data.settings.model.ContactSortOrder
import io.github.elli0t0.phone.data.settings.model.ListDensity
import io.github.elli0t0.phone.data.settings.model.StartTab
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settingsState: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    fun onAccentColorSelected(colorValue: Long, isCustom: Boolean = false) {
        viewModelScope.launch {
            settingsRepository.setAccentColor(colorValue, isCustom)
        }
    }

    fun onDynamicColorChanged(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDynamicColor(enabled)
        }
    }

    fun onPureBlackDarkModeChanged(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setPureBlackDarkMode(enabled)
        }
    }

    fun onListDensityChanged(density: ListDensity) {
        viewModelScope.launch {
            settingsRepository.setListDensity(density)
        }
    }

    fun onCardStyleChanged(style: CardStyle) {
        viewModelScope.launch {
            settingsRepository.setCardStyle(style)
        }
    }

    fun onFontSizeScaleChanged(scale: Float) {
        viewModelScope.launch {
            settingsRepository.setFontSizeScale(scale)
        }
    }

    fun onAnimationOptionChanged(option: AnimationOption) {
        viewModelScope.launch {
            settingsRepository.setAnimationOption(option)
        }
    }

    fun onShowBottomBarLabelsChanged(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowBottomBarLabels(show)
        }
    }

    fun onDefaultStartTabChanged(tab: StartTab) {
        viewModelScope.launch {
            settingsRepository.setDefaultStartTab(tab)
        }
    }

    fun onContactSortOrderChanged(order: ContactSortOrder) {
        viewModelScope.launch {
            settingsRepository.setContactSortOrder(order)
        }
    }

    fun onContactNameFormatChanged(format: ContactNameFormat) {
        viewModelScope.launch {
            settingsRepository.setContactNameFormat(format)
        }
    }

    fun onResetToDefaults() {
        viewModelScope.launch {
            settingsRepository.resetToDefaults()
        }
    }

    companion object {
        fun provideFactory(
            repository: SettingsRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(repository) as T
            }
        }
    }
}
