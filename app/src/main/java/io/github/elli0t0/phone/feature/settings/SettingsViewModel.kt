package io.github.elli0t0.phone.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.elli0t0.phone.data.settings.SettingsRepository
import io.github.elli0t0.phone.data.settings.model.AnimationOption
import io.github.elli0t0.phone.data.settings.model.AccentChoice
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
import io.github.elli0t0.phone.data.settings.model.AppBackground
import io.github.elli0t0.phone.data.settings.model.AppSettings
import io.github.elli0t0.phone.data.settings.model.BottomBarSize
import io.github.elli0t0.phone.data.settings.model.Backdrop
import io.github.elli0t0.phone.data.settings.model.DropletStretch
import io.github.elli0t0.phone.data.settings.model.EdgeLight
import io.github.elli0t0.phone.data.settings.model.GlassHapticFeedback
import io.github.elli0t0.phone.data.settings.model.GlassIntensity
import io.github.elli0t0.phone.data.settings.model.HeaderSize
import io.github.elli0t0.phone.data.settings.model.BottomBarStyle
import io.github.elli0t0.phone.data.settings.model.BottomBarChoice
import io.github.elli0t0.phone.data.settings.model.CallBackground
import io.github.elli0t0.phone.data.settings.model.CallDisplay
import io.github.elli0t0.phone.data.settings.model.DefaultSim
import io.github.elli0t0.phone.data.settings.model.CardStyle
import io.github.elli0t0.phone.data.settings.model.KeyStyle
import io.github.elli0t0.phone.data.settings.model.SwitchStyle
import io.github.elli0t0.phone.data.settings.model.AppStyle
import io.github.elli0t0.phone.data.settings.model.KeypadStyle
import io.github.elli0t0.phone.data.settings.model.FontStyle
import io.github.elli0t0.phone.data.settings.model.TransitionStyle
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

    // Single accent picker (0.17.7): one choice drives the
    // dynamicColor / isCustomAccent flags together. The color value
    // itself is kept; Presets/Custom only change which source wins.
    fun onAccentChoiceChanged(choice: AccentChoice) {
        viewModelScope.launch {
            val current = settingsState.value.accentColor
            when (choice) {
                AccentChoice.DYNAMIC -> {
                    settingsRepository.setDynamicColor(true)
                    settingsRepository.setAccentColor(current, false)
                }
                AccentChoice.PRESETS -> {
                    settingsRepository.setDynamicColor(false)
                    settingsRepository.setAccentColor(current, false)
                }
                AccentChoice.CUSTOM -> {
                    settingsRepository.setDynamicColor(false)
                    settingsRepository.setAccentColor(current, true)
                }
            }
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

    // Single bottom-bar picker (0.17.7): one choice drives the
    // bottomBarStyle / glassBottomBar / lensBar fields together.
    // Solid only changes the style; the other two fields are
    // irrelevant while the style is solid.
    fun onBottomBarChoiceChanged(choice: BottomBarChoice) {
        viewModelScope.launch {
            when (choice) {
                BottomBarChoice.LENS -> {
                    settingsRepository.setBottomBarStyle(BottomBarStyle.GLASS)
                    settingsRepository.setGlassBottomBar(true)
                    settingsRepository.setLensBar(true)
                }
                BottomBarChoice.GLASS -> {
                    settingsRepository.setBottomBarStyle(BottomBarStyle.GLASS)
                    settingsRepository.setGlassBottomBar(true)
                    settingsRepository.setLensBar(false)
                }
                BottomBarChoice.SOLID -> {
                    settingsRepository.setBottomBarStyle(BottomBarStyle.SOLID)
                }
            }
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

    fun onShowAdvancedSettingsChanged(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowAdvancedSettings(show)
        }
    }

    fun onGlassBottomBarChanged(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setGlassBottomBar(enabled)
        }
    }

    // Liquid-glass master switch (0.17.7): one switch drives the
    // headers, contact-detail, call-screen and bottom-bar glass
    // flags together.
    fun onGlassMasterChanged(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setGlassHeaders(enabled)
            settingsRepository.setGlassContactDetail(enabled)
            settingsRepository.setGlassCallScreens(enabled)
            settingsRepository.setGlassBottomBar(enabled)
        }
    }

    fun onKeyStyleChanged(style: KeyStyle) {
        viewModelScope.launch {
            settingsRepository.setKeyStyle(style)
        }
    }

    fun onSwitchStyleChanged(style: SwitchStyle) {
        viewModelScope.launch {
            settingsRepository.setSwitchStyle(style)
        }
    }

    fun onAppStyleChanged(style: AppStyle) {
        viewModelScope.launch {
            settingsRepository.setAppStyle(style)
        }
    }

    fun onKeypadStyleChanged(style: KeypadStyle) {
        viewModelScope.launch {
            settingsRepository.setKeypadStyle(style)
        }
    }

    fun onFontStyleChanged(style: FontStyle) {
        viewModelScope.launch {
            settingsRepository.setFontStyle(style)
        }
    }

    fun onTransitionStyleChanged(style: TransitionStyle) {
        viewModelScope.launch {
            settingsRepository.setTransitionStyle(style)
        }
    }

    fun onBottomSheetsChanged(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBottomSheets(enabled)
        }
    }

    fun onSwipeActionsChanged(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSwipeActions(enabled)
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

    fun onAppBackgroundChanged(background: AppBackground) {
        viewModelScope.launch {
            settingsRepository.setAppBackground(background)
        }
    }

    fun onAppBackgroundDimChanged(dim: Float) {
        viewModelScope.launch {
            settingsRepository.setAppBackgroundDim(dim)
        }
    }

    fun onAppBackgroundPhotoChanged(version: Long) {
        viewModelScope.launch {
            settingsRepository.setAppBackgroundPhotoVersion(version)
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

    // Single call-button size (0.17.7): one level drives the end-call,
    // answer, control and keypad sizes together. The four enums share
    // the SMALL / MEDIUM / LARGE names, so the level maps by name.
    fun onCallButtonSizeChanged(size: EndCallButtonSize) {
        viewModelScope.launch {
            settingsRepository.setEndCallButtonSize(size)
            settingsRepository.setAnswerButtonSize(AnswerButtonSize.valueOf(size.name))
            settingsRepository.setControlButtonSize(ControlButtonSize.valueOf(size.name))
            settingsRepository.setDtmfKeySize(DtmfKeySize.valueOf(size.name))
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

    /** JSON backup passthroughs; the launchers below call these off the main thread. */
    suspend fun exportSettings(): String = settingsRepository.exportToJson()
    suspend fun importSettings(json: String): Int = settingsRepository.importFromJson(json)

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
