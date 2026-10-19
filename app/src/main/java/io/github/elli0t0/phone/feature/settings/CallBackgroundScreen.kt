package io.github.elli0t0.phone.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.elli0t0.phone.PhoneApplication
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LiquidGlassIconButton
import io.github.elli0t0.phone.core.design.LiquidGlassPill
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.Motion
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.design.liquidGlass
import io.github.elli0t0.phone.data.call.CallState
import io.github.elli0t0.phone.data.call.OngoingCallModel
import io.github.elli0t0.phone.data.settings.model.AnimationOption
import io.github.elli0t0.phone.data.settings.model.AnswerGesture
import io.github.elli0t0.phone.data.settings.model.CallBackground
import io.github.elli0t0.phone.data.settings.model.ControlButtonSize
import io.github.elli0t0.phone.data.settings.model.DtmfKeySize
import io.github.elli0t0.phone.data.settings.model.IncomingAvatarShape
import io.github.elli0t0.phone.data.settings.model.IncomingAvatarSize
import io.github.elli0t0.phone.feature.call.ui.IncomingCallScreen
import io.github.elli0t0.phone.feature.call.ui.OngoingCallScreen
import kotlinx.coroutines.launch

/**
 * Call background studio as a full page: the top half is a live
 * full-screen preview of the real call screen (incoming or ongoing)
 * with a sample caller, and the bottom half holds every option:
 * background style, answer gesture, dim, and number visibility. Every
 * change saves immediately and shows in the preview. Back returns to
 * Settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallBackgroundScreen(
    selected: CallBackground,
    accentColor: Long,
    onSelect: (CallBackground) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val settings = LocalAppSettings.current
    val context = LocalContext.current
    val app = context.applicationContext as PhoneApplication
    // Same instance SettingsScreen uses (same nav entry owner).
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.provideFactory(app.container.settingsRepository)
    )
    var previewOngoing by remember { mutableStateOf(false) }
    var previewFullscreen by remember { mutableStateOf(false) }
    var showCustomColorDialog by remember { mutableStateOf(false) }
    var customColorHexInput by remember { mutableStateOf("") }
    var customColorError by remember { mutableStateOf(false) }
    // The color the brush uses: the custom color when CUSTOM is
    // selected, the accent otherwise (same rule as CallActivity).
    val backgroundColor = if (selected == CallBackground.CUSTOM) {
        settings.callBackgroundCustomColor
    } else {
        accentColor
    }
    // Slider draft: the preview follows the drag live, and the value
    // is written to Settings once on release (not per frame).
    var dimDraft by remember(settings.callBackgroundDim) {
        mutableStateOf(settings.callBackgroundDim)
    }
    val mockCall = remember(previewOngoing) {
        OngoingCallModel(
            id = "preview",
            number = "+1 555 010 1234",
            displayName = "Alex Morgan",
            state = if (previewOngoing) CallState.ACTIVE else CallState.RINGING,
            durationSeconds = 125L
        )
    }
    val quickReplies = listOf(
        stringResource(id = R.string.quick_reply_cant_talk),
        stringResource(id = R.string.quick_reply_call_back),
        stringResource(id = R.string.quick_reply_in_meeting)
    )

    // Custom color: hex dialog, same pattern as the accent picker.
    // Saving picks the custom color and selects the CUSTOM style.
    if (showCustomColorDialog) {
        AlertDialog(
            onDismissRequest = { showCustomColorDialog = false },
            title = { Text(text = stringResource(id = R.string.settings_custom_color_dialog_title)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = customColorHexInput,
                        onValueChange = {
                            customColorHexInput = it
                            customColorError = false
                        },
                        label = { Text(stringResource(id = R.string.settings_hex_code)) },
                        singleLine = true,
                        isError = customColorError
                    )
                    if (customColorError) {
                        Text(
                            text = stringResource(id = R.string.settings_invalid_hex),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = spacing.small)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleaned = customColorHexInput.trim().removePrefix("#")
                        try {
                            val parsedColorLong = when (cleaned.length) {
                                6 -> ("FF$cleaned".toLong(16))
                                8 -> cleaned.toLong(16)
                                else -> throw IllegalArgumentException()
                            }
                            settingsViewModel.onCallBackgroundCustomColorChanged(parsedColorLong)
                            onSelect(CallBackground.CUSTOM)
                            showCustomColorDialog = false
                        } catch (e: Exception) {
                            customColorError = true
                        }
                    }
                ) {
                    Text(text = stringResource(id = R.string.action_save))
                }
            },
            dismissButton = {
                Text(
                    text = stringResource(id = R.string.action_cancel),
                    modifier = Modifier
                        .clickable { showCustomColorDialog = false }
                        .padding(spacing.medium)
                )
            }
        )
    }

    // Full-screen preview: the real call screen edge to edge, exactly
    // as it looks on a call. Back or the close button returns to the studio.
    if (previewFullscreen) {
        BackHandler { previewFullscreen = false }
        Box(modifier = Modifier.fillMaxSize()) {
            CallPreviewContent(
                previewOngoing = previewOngoing,
                mockCall = mockCall,
                selected = selected,
                backgroundColor = backgroundColor,
                dimDraft = dimDraft,
                showNumber = settings.showNumberOnCallScreen,
                answerGesture = settings.answerGesture,
                quickReplies = quickReplies
            )
            IconButton(
                onClick = { previewFullscreen = false },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(spacing.medium)
                    .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                    .testTag("call_preview_close")
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = stringResource(id = R.string.settings_call_preview_close),
                    tint = Color.White
                )
            }
        }
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("call_background_screen"),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.settings_call_background)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("btn_call_background_back")) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = stringResource(id = R.string.cd_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        // One scrolling page: the preview is the first item and
        // scrolls away with the settings, instead of being pinned
        // at 50% height with only the lower half scrolling.
        val listState = rememberLazyListState()
        val scope = rememberCoroutineScope()
        val previewVisible by remember {
            derivedStateOf { listState.firstVisibleItemIndex == 0 }
        }
        val previewHeight = minOf(
            LocalConfiguration.current.screenHeightDp.dp * 0.6f,
            PhoneDimens.studioPreviewMaxHeight
        )
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().testTag("call_background_list"),
                contentPadding = PaddingValues(
                    horizontal = spacing.screenHorizontalPadding,
                    vertical = spacing.small
                ),
                verticalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                // Item 0: phone-shaped preview card + segmented control.
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(spacing.medium),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Glass-framed preview card: the real call
                        // screen with a sample caller. Preview buttons
                        // are inert. Full screen shows it edge to edge.
                        Box(
                            modifier = Modifier
                                .height(previewHeight)
                                .aspectRatio(9f / 19.5f)
                                .liquidGlass(
                                    shape = RoundedCornerShape(PhoneDimens.studioPreviewCornerRadius),
                                    floating = true
                                )
                                .padding(PhoneDimens.studioPreviewFramePadding)
                                .clip(
                                    RoundedCornerShape(
                                        PhoneDimens.studioPreviewCornerRadius -
                                            PhoneDimens.studioPreviewFramePadding
                                    )
                                )
                                .testTag("call_preview_card")
                        ) {
                            CallPreviewContent(
                                previewOngoing = previewOngoing,
                                mockCall = mockCall,
                                selected = selected,
                                backgroundColor = backgroundColor,
                                dimDraft = dimDraft,
                                showNumber = settings.showNumberOnCallScreen,
                                answerGesture = settings.answerGesture,
                                quickReplies = quickReplies
                            )
                            LiquidGlassIconButton(
                                onClick = { previewFullscreen = true },
                                forceDarkGlass = true,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(spacing.small)
                                    .testTag("call_preview_fullscreen")
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_fullscreen),
                                    contentDescription = stringResource(id = R.string.settings_call_preview_fullscreen),
                                    tint = Color.White,
                                    modifier = Modifier.size(PhoneDimens.studioPreviewFullscreenIconSize)
                                )
                            }
                        }
                        // Preview switch: incoming / ongoing.
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SegmentedButton(
                                selected = !previewOngoing,
                                onClick = { previewOngoing = false },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = MaterialTheme.colorScheme.primary,
                                    activeContentColor = MaterialTheme.colorScheme.onPrimary,
                                    activeBorderColor = MaterialTheme.colorScheme.primary,
                                    inactiveContainerColor = Color.Transparent,
                                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                label = { Text(text = stringResource(id = R.string.settings_call_preview_incoming)) },
                                modifier = Modifier.testTag("call_preview_incoming")
                            )
                            SegmentedButton(
                                selected = previewOngoing,
                                onClick = { previewOngoing = true },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = MaterialTheme.colorScheme.primary,
                                    activeContentColor = MaterialTheme.colorScheme.onPrimary,
                                    activeBorderColor = MaterialTheme.colorScheme.primary,
                                    inactiveContainerColor = Color.Transparent,
                                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                label = { Text(text = stringResource(id = R.string.settings_call_preview_ongoing)) },
                                modifier = Modifier.testTag("call_preview_ongoing")
                            )
                        }
                    }
                }

                // Size sections live here (not in CallStudioSections)
                // to keep each file small enough to ship. Each group
                // is a card item in the page.
                // Control button size: how big the ongoing-call buttons are.
                item {
                    StudioGroupCard(title = stringResource(id = R.string.call_bg_control_button_size)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(spacing.small)
                        ) {
                            ControlButtonSize.entries.forEach { size ->
                                StudioOptionChip(
                                    selected = settings.controlButtonSize == size,
                                    onClick = {
                                        settingsViewModel.onControlButtonSizeChanged(size)
                                    },
                                    label = size.label,
                                    testTag = "control_button_size_${size.name.lowercase()}"
                                )
                            }
                        }
                    }
                }

                // Keypad key size: how big the in-call keypad keys are.
                item {
                    StudioGroupCard(title = stringResource(id = R.string.call_bg_keypad_key_size)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(spacing.small)
                        ) {
                            DtmfKeySize.entries.forEach { size ->
                                StudioOptionChip(
                                    selected = settings.dtmfKeySize == size,
                                    onClick = {
                                        settingsViewModel.onDtmfKeySizeChanged(size)
                                    },
                                    label = size.label,
                                    testTag = "dtmf_key_size_${size.name.lowercase()}"
                                )
                            }
                        }
                    }
                }

                // Avatar size: how big the caller avatar is on
                // the incoming call screen.
                item {
                    StudioGroupCard(title = stringResource(id = R.string.call_bg_avatar_size)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(spacing.small)
                        ) {
                            IncomingAvatarSize.entries.forEach { size ->
                                StudioOptionChip(
                                    selected = settings.incomingAvatarSize == size,
                                    onClick = {
                                        settingsViewModel.onIncomingAvatarSizeChanged(size)
                                    },
                                    label = size.label,
                                    testTag = "incoming_avatar_size_${size.name.lowercase()}"
                                )
                            }
                        }
                    }
                }

                // Avatar shape for the caller avatar on the
                // incoming call screen.
                item {
                    StudioGroupCard(title = stringResource(id = R.string.call_bg_avatar_shape)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(spacing.small)
                        ) {
                            IncomingAvatarShape.entries.forEach { shape ->
                                StudioOptionChip(
                                    selected = settings.incomingAvatarShape == shape,
                                    onClick = {
                                        settingsViewModel.onIncomingAvatarShapeChanged(shape)
                                    },
                                    label = shape.label,
                                    testTag = "incoming_avatar_shape_${shape.name.lowercase()}"
                                )
                            }
                        }
                    }
                }

                // Quick replies position: above (default) or below the answer buttons.
                item {
                    StudioGroupCard(title = null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = stringResource(id = R.string.call_bg_quick_replies_below), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            AppSwitch(checked = settings.quickRepliesBelowButtons, onCheckedChange = { settingsViewModel.onQuickRepliesBelowButtonsChanged(it) }, modifier = Modifier.testTag("quick_replies_below_buttons_switch"))
                        }
                    }
                }

                item {
                    CallStudioSections(
                        selected = selected,
                        accentColor = accentColor,
                        onSelect = onSelect,
                        dimDraft = dimDraft,
                        onDimDraftChange = { dimDraft = it },
                        onEditCustomColor = {
                            customColorHexInput = String.format(
                                "#%06X",
                                0xFFFFFF and settings.callBackgroundCustomColor.toInt()
                            )
                            customColorError = false
                            showCustomColorDialog = true
                        }
                    )
                }
            }
            // Floating Preview pill, shown when the preview scrolled
            // off-screen: a small live thumbnail + label. Tap scrolls
            // back to the preview.
            AnimatedVisibility(
                visible = !previewVisible,
                enter = fadeIn() + scaleIn(initialScale = 0.9f),
                exit = fadeOut() + scaleOut(targetScale = 0.9f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = spacing.medium)
            ) {
                LiquidGlassPill(
                    floating = true,
                    modifier = Modifier
                        .clickable { scope.launch { listState.animateScrollToItem(0) } }
                        .testTag("call_preview_pill")
                ) {
                    Box(
                        modifier = Modifier
                            .height(PhoneDimens.studioPreviewThumbnailHeight)
                            .aspectRatio(9f / 19.5f)
                            .clip(RoundedCornerShape(PhoneDimens.studioPreviewThumbnailCornerRadius))
                    ) {
                        ScaledCallPreview(modifier = Modifier.fillMaxSize()) {
                            CallPreviewContent(
                                previewOngoing = previewOngoing,
                                mockCall = mockCall,
                                selected = selected,
                                backgroundColor = backgroundColor,
                                dimDraft = dimDraft,
                                showNumber = settings.showNumberOnCallScreen,
                                answerGesture = settings.answerGesture,
                                quickReplies = quickReplies
                            )
                        }
                    }
                    Text(
                        text = stringResource(id = R.string.settings_call_background),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = spacing.small)
                    )
                }
            }
        }
    }
}

/**
 * One settings group as a solid card: an optional title plus the
 * group's controls. Groups in the Call Studio are card items in
 * the page. Shared with CallStudioSections (same package).
 */
@Composable
fun StudioGroupCard(
    title: String?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(PhoneDimens.studioGroupCardCornerRadius),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(LocalSpacing.current.medium),
            verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.small)
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            content()
        }
    }
}

/**
 * Custom switch: wide 52x28dp track with a large white round thumb,
 * matching the main Settings switch (white thumb on accent track).
 * Shared with SettingsScreen's SettingsSwitch; moves to core/design
 * when SettingsScreen is split.
 */
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Thumb slide respects the Animations setting: ON keeps the
    // default spring, REDUCED shortens to a fade-speed tween,
    // OFF snaps instantly.
    val animationOption = LocalAppSettings.current.animationOption
    val thumbSpec: AnimationSpec<Dp> = when (animationOption) {
        AnimationOption.ON -> spring(stiffness = Spring.StiffnessMediumLow)
        AnimationOption.REDUCED -> tween(
            durationMillis = Motion.scaledDurationMs(200, animationOption),
            easing = Motion.Easings.Standard
        )
        AnimationOption.OFF -> snap()
    }
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) {
            PhoneDimens.settingsSwitchWidth - PhoneDimens.settingsSwitchThumbSize -
                PhoneDimens.settingsSwitchThumbPadding * 2
        } else {
            PhoneDimens.settingsSwitchThumbPadding
        },
        animationSpec = thumbSpec
    )
    Box(
        modifier = modifier
            .size(
                width = PhoneDimens.settingsSwitchWidth,
                height = PhoneDimens.settingsSwitchHeight
            )
            .clip(RoundedCornerShape(PhoneDimens.settingsSwitchCornerPercent))
            .background(
                if (checked) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(PhoneDimens.settingsSwitchThumbSize)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

/**
 * The studio preview: the call screen is laid out at a reference
 * phone size, then scaled proportionally to fit the preview box,
 * so the caller header and call buttons are always visible.
 */
@Composable
private fun ScaledCallPreview(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val scale = minOf(
            maxWidth / PhoneDimens.studioPreviewReferenceWidth,
            maxHeight / PhoneDimens.studioPreviewReferenceHeight
        )
        Box(
            modifier = Modifier
                .size(
                    PhoneDimens.studioPreviewReferenceWidth,
                    PhoneDimens.studioPreviewReferenceHeight
                )
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        ) {
            content()
        }
    }
}

/**
 * The live call preview: the real incoming or ongoing call screen
 * with a sample caller. Preview buttons are inert.
 */
@Composable
private fun CallPreviewContent(
    previewOngoing: Boolean,
    mockCall: OngoingCallModel,
    selected: CallBackground,
    backgroundColor: Long,
    dimDraft: Float,
    showNumber: Boolean,
    answerGesture: AnswerGesture,
    quickReplies: List<String>
) {
    if (previewOngoing) {
        OngoingCallScreen(
            callModel = mockCall,
            onMuteToggle = {},
            onAudioRouteSelect = {},
            onHoldToggle = {},
            onDtmfDigit = {},
            onEndCall = {},
            callBackground = selected,
            accentColor = backgroundColor,
            dim = dimDraft,
            showNumber = showNumber
        )
    } else {
        IncomingCallScreen(
            callModel = mockCall,
            onAnswer = {},
            onDecline = {},
            onDeclineWithMessage = {},
            quickDeclineMessages = quickReplies,
            callBackground = selected,
            accentColor = backgroundColor,
            dim = dimDraft,
            showNumber = showNumber,
            answerGesture = answerGesture
        )
    }
}
