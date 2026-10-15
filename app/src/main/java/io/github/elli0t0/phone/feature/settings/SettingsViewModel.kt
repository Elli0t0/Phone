package io.github.elli0t0.phone.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.elli0t0.phone.data.settings.SettingsRepository
import io.github.elli0t0.phone.data.settings.model.AnimationOption
import io.github.elli0t0.phone.data.settings.model.AppSettings
import io.github.elli0t0.phone.data.settings.model.BottomBarSize
import io.github.elli0t0.phone.data.settings.model.BottomBarStyle
import io.github.elli0t0.phone.data.settings.model.CallBackground
import io.github.elli0t0.phone.data.settings.model.CallDisplay
import io.github.elli0t0.phone.data.settings.model.DefaultSim
import io.github.elli0t0.phone.data.settings.model.CardStyle
import io.github.elli0t0.phone.data.settings.model.ContactNameFormat
import io.github.elli0t0.phone.data.settings.model.ContactSortOrder
import io.github.elli0t0.phone.data.settings.model.ListDensity
import io.github.elli0t0.phone.data.settings.model.StartTab
import io.github.elli0t0.phone.data.settings.model.ThemeMode
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

    fun onThemeModeChanged(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }
    fun setThemeMode(mode: ThemeMode) = onThemeModeChanged(mode)

    fun onAccentColorSelected(colorValue: Long, isCustom: Boolean = false) {
        viewModelScope.launch {
            settingsRepository.setAccentColor(colorValue, isCustom)
        }
    }
    fun setAccentColor(colorValue: Long, isCustom: Boolean = false) = onAccentColorSelected(colorValue, isCustom)

    fun onDynamicColorChanged(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDynamicColor(enabled)
        }
    }
    fun setDynamicColor(enabled: Boolean) = onDynamicColorChanged(enabled)

    fun onPureBlackDarkModeChanged(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setPureBlackDarkMode(enabled)
        }
    }
    fun setPureBlackDarkMode(enabled: Boolean) = onPureBlackDarkModeChanged(enabled)

    fun onListDensityChanged(density: ListDensity) {
        viewModelScope.launch {
            settingsRepository.setListDensity(density)
        }
    }
    fun setListDensity(density: ListDensity) = onListDensityChanged(density)

    fun onCardStyleChanged(style: CardStyle) {
        viewModelScope.launch {
            settingsRepository.setCardStyle(style)
        }
    }
    fun setCardStyle(style: CardStyle) = onCardStyleChanged(style)

    fun onFontSizeScaleChanged(scale: Float) {
        viewModelScope.launch {
            settingsRepository.setFontSizeScale(scale)
        }
    }
    fun setFontSizeScale(scale: Float) = onFontSizeScaleChanged(scale)

    fun onAnimationOptionChanged(option: AnimationOption) {
        viewModelScope.launch {
            settingsRepository.setAnimationOption(option)
        }
    }
    fun setAnimationOption(option: AnimationOption) = onAnimationOptionChanged(option)

    fun onShowBottomBarLabelsChanged(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowBottomBarLabels(show)
        }
    }
    fun setShowBottomBarLabels(show: Boolean) = onShowBottomBarLabelsChanged(show)

    fun onBottomBarStyleChanged(style: BottomBarStyle) {
        viewModelScope.launch {
            settingsRepository.setBottomBarStyle(style)
        }
    }
    fun setBottomBarStyle(style: BottomBarStyle) = onBottomBarStyleChanged(style)

    fun onBottomBarSizeChanged(size: BottomBarSize) {
        viewModelScope.launch {
            settingsRepository.setBottomBarSize(size)
        }
    }
    fun setBottomBarSize(size: BottomBarSize) = onBottomBarSizeChanged(size)

    fun onShrinkBottomBarOnScrollChanged(shrink: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShrinkBottomBarOnScroll(shrink)
        }
    }
    fun setShrinkBottomBarOnScroll(shrink: Boolean) = onShrinkBottomBarOnScrollChanged(shrink)

    fun onDefaultStartTabChanged(tab: StartTab) {
        viewModelScope.launch {
            settingsRepository.setDefaultStartTab(tab)
        }
    }
    fun setDefaultStartTab(tab: StartTab) = onDefaultStartTabChanged(tab)

    fun onContactSortOrderChanged(order: ContactSortOrder) {
        viewModelScope.launch {
            settingsRepository.setContactSortOrder(order)
        }
    }
    fun setContactSortOrder(order: ContactSortOrder) = onContactSortOrderChanged(order)

    fun onContactNameFormatChanged(format: ContactNameFormat) {
        viewModelScope.launch {
            settingsRepository.setContactNameFormat(format)
        }
    }
    fun setContactNameFormat(format: ContactNameFormat) = onContactNameFormatChanged(format)

    fun onCallDisplayChanged(display: CallDisplay) {
        viewModelScope.launch {
            settingsRepository.setCallDisplay(display)
        }
    }
    fun setCallDisplay(display: CallDisplay) = onCallDisplayChanged(display)

    fun onCallBackgroundChanged(background: CallBackground) {
        viewModelScope.launch {
            settingsRepository.setCallBackground(background)
        }
    }
    fun setCallBackground(background: CallBackground) = onCallBackgroundChanged(background)

    fun onCallBackgroundDimChanged(dim: Float) {
        viewModelScope.launch {
            settingsRepository.setCallBackgroundDim(dim)
        }
    }
    fun setCallBackgroundDim(dim: Float) = onCallBackgroundDimChanged(dim)

    fun onCallBackgroundCustomColorChanged(color: Long) {
        viewModelScope.launch {
            settingsRepository.setCallBackgroundCustomColor(color)
        }
    }
    fun setCallBackgroundCustomColor(color: Long) = onCallBackgroundCustomColorChanged(color)

    fun onShowNumberOnCallScreenChanged(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowNumberOnCallScreen(show)
        }
    }
    fun setShowNumberOnCallScreen(show: Boolean) = onShowNumberOnCallScreenChanged(show)

    fun onDefaultSimChanged(defaultSim: DefaultSim) {
        viewModelScope.launch {
            settingsRepository.setDefaultSim(defaultSim)
        }
    }
    fun setDefaultSim(defaultSim: DefaultSim) = onDefaultSimChanged(defaultSim)

    fun onQuickDeclineMessagesChanged(messages: List<String>) {
        viewModelScope.launch {
            settingsRepository.setQuickDeclineMessages(messages)
        }
    }
    fun setQuickDeclineMessages(messages: List<String>) = onQuickDeclineMessagesChanged(messages)

    fun onResetToDefaults() {
        viewModelScope.launch {
            settingsRepository.resetToDefaults()
        }
    }
    fun resetToDefaults() = onResetToDefaults()

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
