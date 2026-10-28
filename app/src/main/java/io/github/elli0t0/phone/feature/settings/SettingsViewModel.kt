package io.github.elli0t0.phone.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.elli0t0.phone.data.settings.SettingsRepository
import io.github.elli0t0.phone.data.settings.model.AnimationOption
import io.github.elli0t0.phone.data.settings.model.AnswerGesture
import io.github.elli0t0.phone.data.settings.model.AnswerStyle
import io.github.elli0t0.phone.data.settings.model.AnswerSide
import io.github.elli0t0.phone.data.settings.model.CallerInfoPosition
import io.github.elli0t0.phone.data.settings.model.AnswerButtonSize
import io.github.elli0t0.phone.data.settings.model.ControlButtonSize
import io.github.elli0t0.phone.data.settings.model.ControlGridAlignment
import io.github.elli0t0.phone.data.settings.model.DtmfKeySize
import io.github.elli0t0.phone.data.settings.model.IncomingAvatarShape
import io.github.elli0t0.phone.data.settings.model.IncomingAvatarSize
import io.github.elli0t0.phone.data.settings.model.EndCallButtonSize
import io.github.elli0t0.phone.data.settings.model.EndCallAlignment
import io.github.elli0t0.phone.data.settings.model.OngoingCallButton
import io.github.elli0t0.phone.data.settings.model.AppSettings
import io.github.elli0t0.phone.data.settings.model.BottomBarSize
import io.github.elli0t0.phone.data.settings.model.Backdrop
import io.github.elli0t0.phone.data.settings.model.DropletStretch
import io.github.elli0t0.phone.data.settings.model.EdgeLight
import io.github.elli0t0.phone.data.settings.model.GlassHapticFeedback
import io.github.elli0t0.phone.data.settings.model.GlassIntensity
import io.github.elli0t0.phone.data.settings.model.HeaderSize
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

    fun onBottomBarStyleChanged(style: BottomBarStyle) {
        viewModelScope.launch {
            settingsRepository.setBottomBarStyle(style)
        }
    }

    fun onBottomBarSizeChanged(size: BottomBarSize) {
        viewModelScope.launch {
            settingsRepository.setBottomBarSize(size)
        }
    }

    fun onHeaderSizeChanged(size: HeaderSize) {
        viewModelScope.launch {
            settingsRepository.setHeaderSize(size)
        }
    }

    fun onShrinkBottomBarOnScrollChanged(shrink: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShrinkBottomBarOnScroll(shrink)
        }
    }

    fun onGlassBottomBarChanged(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setGlassBottomBar(enabled)
        }
    }

    fun onGlassDialpadChanged(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setGlassDialpad(enabled)
        }
    }

    fun onGlassHeadersChanged(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setGlassHeaders(enabled)
        }
    }

    fun onGlassContactDetailChanged(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setGlassContactDetail(enabled)
        }
    }

    fun onGlassCallScreensChanged(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setGlassCallScreens(enabled)
        }
    }

    fun onGlassIntensityChanged(intensity: GlassIntensity) {
        viewModelScope.launch {
            settingsRepository.setGlassIntensity(intensity)
        }
    }

    fun onGlassBlurAmountChanged(amount: Float) {
        viewModelScope.launch {
            settingsRepository.setGlassBlurAmount(amount)
        }
    }

    fun onGlassTintAmountChanged(amount: Float) {
        viewModelScope.launch {
            settingsRepository.setGlassTintAmount(amount)
        }
    }

    fun onEdgeLightChanged(edgeLight: EdgeLight) {
        viewModelScope.launch {
            settingsRepository.setEdgeLight(edgeLight)
        }
    }

    fun onGlassColorRichnessChanged(richness: Float) {
        viewModelScope.launch {
            settingsRepository.setGlassColorRichness(richness)
        }
    }

    fun onDropletStretchChanged(stretch: DropletStretch) {
        viewModelScope.launch {
            settingsRepository.setDropletStretch(stretch)
        }
    }

    fun onGlassHapticFeedbackChanged(feedback: GlassHapticFeedback) {
        viewModelScope.launch {
            settingsRepository.setGlassHapticFeedback(feedback)
        }
    }

    fun onEdgeRefractionChanged(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setEdgeRefraction(enabled)
        }
    }

    fun onLensBarChanged(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setLensBar(enabled)
        }
    }

    /** Glass Studio reset: every Studio setting back to its default. */
    fun onResetGlassDefaults() {
        onGlassBlurAmountChanged(1.0f)
        onGlassTintAmountChanged(1.0f)
        onEdgeLightChanged(EdgeLight.SOFT)
        onDynamicHighlightsChanged(true)
        onGlassColorRichnessChanged(1.7f)
        onDropletStretchChanged(DropletStretch.NORMAL)
        onGlassHapticFeedbackChanged(GlassHapticFeedback.LIGHT)
        onEdgeRefractionChanged(true)
        onLensBarChanged(true)
        onBackdropChanged(Backdrop.SUBTLE)
    }

    fun onReduceTransparencyChanged(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setReduceTransparency(enabled)
        }
    }

    fun onGlassCardsChanged(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setGlassCards(enabled)
        }
    }

    fun onDynamicHighlightsChanged(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDynamicHighlights(enabled)
        }
    }

    fun onBackdropChanged(backdrop: Backdrop) {
        viewModelScope.launch {
            settingsRepository.setBackdrop(backdrop)
        }
    }

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

    fun onContactNameFormatChanged(format: ContactNameFormat) {
        viewModelScope.launch {
            settingsRepository.setContactNameFormat(format)
        }
    }

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

    fun onCallBackgroundCustomColorChanged(color: Long) {
        viewModelScope.launch {
            settingsRepository.setCallBackgroundCustomColor(color)
        }
    }

    fun onAnswerGestureChanged(gesture: AnswerGesture) {
        viewModelScope.launch {
            settingsRepository.setAnswerGesture(gesture)
        }
    }

    fun onAnswerStyleChanged(style: AnswerStyle) {
        viewModelScope.launch {
            settingsRepository.setAnswerStyle(style)
        }
    }

    fun onSliderOnlyWhenLockedChanged(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSliderOnlyWhenLocked(enabled)
        }
    }

    fun onAnswerSideChanged(side: AnswerSide) {
        viewModelScope.launch {
            settingsRepository.setAnswerSide(side)
        }
    }

    fun onEndCallAlignmentChanged(alignment: EndCallAlignment) {
        viewModelScope.launch {
            settingsRepository.setEndCallAlignment(alignment)
        }
    }

    fun onCallerInfoPositionChanged(position: CallerInfoPosition) {
        viewModelScope.launch {
            settingsRepository.setCallerInfoPosition(position)
        }
    }

    fun onControlGridAlignmentChanged(alignment: ControlGridAlignment) {
        viewModelScope.launch {
            settingsRepository.setControlGridAlignment(alignment)
        }
    }

    fun onEndCallButtonSizeChanged(size: EndCallButtonSize) {
        viewModelScope.launch {
            settingsRepository.setEndCallButtonSize(size)
        }
    }

    fun onAnswerButtonSizeChanged(size: AnswerButtonSize) {
        viewModelScope.launch {
            settingsRepository.setAnswerButtonSize(size)
        }
    }

    fun onShowQuickRepliesChanged(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowQuickReplies(show)
        }
    }

    fun onQuickRepliesBelowButtonsChanged(below: Boolean) { viewModelScope.launch { settingsRepository.setQuickRepliesBelowButtons(below) } }

    fun onControlButtonSizeChanged(size: ControlButtonSize) {
        viewModelScope.launch {
            settingsRepository.setControlButtonSize(size)
        }
    }

    fun onDtmfKeySizeChanged(size: DtmfKeySize) {
        viewModelScope.launch {
            settingsRepository.setDtmfKeySize(size)
        }
    }

    fun onDialpadTonesChanged(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDialpadTones(enabled)
        }
    }

    fun onIncomingAvatarSizeChanged(size: IncomingAvatarSize) {
        viewModelScope.launch {
            settingsRepository.setIncomingAvatarSize(size)
        }
    }

    fun onIncomingAvatarShapeChanged(shape: IncomingAvatarShape) {
        viewModelScope.launch { settingsRepository.setIncomingAvatarShape(shape) }
    }

    fun onOngoingCallButtonsChanged(buttons: List<OngoingCallButton>) {
        viewModelScope.launch {
            settingsRepository.setOngoingCallButtons(buttons)
        }
    }

    fun onShowNumberOnCallScreenChanged(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowNumberOnCallScreen(show)
        }
    }

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
