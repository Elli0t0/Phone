package io.github.elli0t0.phone.feature.call.ui

import android.telecom.CallAudioState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.common.PhoneNumberFormatter
import io.github.elli0t0.phone.data.settings.model.AccentPreset
import io.github.elli0t0.phone.core.design.InCallControlActive
import io.github.elli0t0.phone.core.design.InCallControlActiveContent
import io.github.elli0t0.phone.core.design.InCallControlBackground
import io.github.elli0t0.phone.core.design.LiquidGlassEndCall
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.design.callBackgroundBrush
import io.github.elli0t0.phone.core.design.pressScale
import io.github.elli0t0.phone.core.design.rememberLightHaptic
import io.github.elli0t0.phone.data.call.CallState
import io.github.elli0t0.phone.data.call.OngoingCallModel
import io.github.elli0t0.phone.data.settings.model.CallBackground
import io.github.elli0t0.phone.data.settings.model.EndCallAlignment
import io.github.elli0t0.phone.data.settings.model.CallerInfoPosition
import io.github.elli0t0.phone.data.settings.model.ControlButtonSize
import io.github.elli0t0.phone.data.settings.model.ControlGridAlignment
import io.github.elli0t0.phone.data.settings.model.DtmfKeySize
import io.github.elli0t0.phone.data.settings.model.EndCallButtonSize
import io.github.elli0t0.phone.data.settings.model.OngoingCallButton

@Composable
fun OngoingCallScreen(
    callModel: OngoingCallModel,
    onMuteToggle: (Boolean) -> Unit,
    onAudioRouteSelect: (Int) -> Unit,
    onHoldToggle: (Boolean) -> Unit,
    onDtmfDigit: (Char) -> Unit,
    onEndCall: () -> Unit,
    onRequestBluetoothPermission: () -> Unit = {},
    hasBluetoothPermission: Boolean = true,
    callBackground: CallBackground = CallBackground.DEFAULT,
    accentColor: Long = AccentPreset.ONE_UI_BLUE.colorValue,
    // Black scrim over the background (0 = none). Number under the name.
    dim: Float = 0f,
    showNumber: Boolean = true,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    var showKeypad by remember { mutableStateOf(false) }
    val endCallSource = remember { MutableInteractionSource() }
    val lightHaptic = rememberLightHaptic()

    val formattedDuration = remember(callModel.durationSeconds) {
        val mins = callModel.durationSeconds / 60
        val secs = callModel.durationSeconds % 60
        String.format("%02d:%02d", mins, secs)
    }

    val stateText = when (callModel.state) {
        CallState.DIALING, CallState.CONNECTING -> stringResource(id = R.string.call_outgoing)
        CallState.HOLDING -> stringResource(id = R.string.call_holding)
        CallState.DISCONNECTED -> stringResource(id = R.string.call_disconnected)
        else -> formattedDuration
    }

    val hasName = !callModel.displayName.isNullOrBlank() && callModel.displayName != callModel.number
    val formattedNumber = PhoneNumberFormatter.format(callModel.number)
    val titleText = if (hasName) callModel.displayName!! else formattedNumber

    val isSpeaker = (callModel.audioRoute and CallAudioState.ROUTE_SPEAKER) != 0
    val isBluetooth = (callModel.audioRoute and CallAudioState.ROUTE_BLUETOOTH) != 0
    val isBluetoothAvailable =
        (callModel.availableAudioRoutes and CallAudioState.ROUTE_BLUETOOTH) != 0
    val isWiredAvailable =
        (callModel.availableAudioRoutes and CallAudioState.ROUTE_WIRED_HEADSET) != 0
    val isHolding = callModel.state == CallState.HOLDING

    val onSpeakerToggle: () -> Unit = {
        if (isSpeaker) {
            onAudioRouteSelect(
                if (isWiredAvailable) {
                    CallAudioState.ROUTE_WIRED_HEADSET
                } else {
                    CallAudioState.ROUTE_EARPIECE
                }
            )
        } else {
            onAudioRouteSelect(CallAudioState.ROUTE_SPEAKER)
        }
    }
    val onBluetoothToggle: () -> Unit = {
        if (!hasBluetoothPermission) {
            onRequestBluetoothPermission()
        } else if (isBluetooth) {
            onAudioRouteSelect(
                if (isWiredAvailable) {
                    CallAudioState.ROUTE_WIRED_HEADSET
                } else {
                    CallAudioState.ROUTE_EARPIECE
                }
            )
        } else {
            onAudioRouteSelect(CallAudioState.ROUTE_BLUETOOTH)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("ongoing_call_screen")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(callBackgroundBrush(callBackground, accentColor))
        )
        if (dim > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = dim.coerceIn(0f, 1f)))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = spacing.screenHorizontalPadding, vertical = spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Caller info position: top (default), or centered in the
            // space above the controls via a spacer above the info.
            if (LocalAppSettings.current.callerInfoPosition == CallerInfoPosition.CENTER) {
                Spacer(modifier = Modifier.weight(1f))
            }
            // Caller info: name, number, state or duration.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = PhoneDimens.ongoingHeaderTopPadding)
            ) {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = PhoneDimens.ongoingTitleSize
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = spacing.extraSmall)
                )
                if (hasName && showNumber && callModel.number.isNotBlank()) {
                    Text(
                        text = formattedNumber,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = PhoneDimens.ongoingNumberSize),
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = spacing.extraSmall)
                    )
                }
                Text(
                    text = stateText,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = PhoneDimens.ongoingStateSize),
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            // Center: in-call DTMF keypad, or empty space when controls are shown.
            if (showKeypad) {
                InCallKeypadGrid(
                    isMuted = callModel.isMuted,
                    isSpeaker = isSpeaker,
                    onMuteToggle = { onMuteToggle(!callModel.isMuted) },
                    onSpeakerToggle = onSpeakerToggle,
                    onDigit = onDtmfDigit,
                    onClose = { showKeypad = false }
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            // Bottom: control grid + red circular end-call button.
            // The end-call button aligns per the setting; the grid is
            // full-width so it is unaffected.
            val endCallAlignment = when (LocalAppSettings.current.endCallAlignment) {
                EndCallAlignment.LEFT -> Alignment.Start
                EndCallAlignment.CENTER -> Alignment.CenterHorizontally
                EndCallAlignment.RIGHT -> Alignment.End
            }
            Column(
                horizontalAlignment = endCallAlignment,
                verticalArrangement = Arrangement.spacedBy(PhoneDimens.ongoingControlGridSpacing),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!showKeypad) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(PhoneDimens.ongoingControlGridSpacing),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Buttons from the setting, in order, 3 per row.
                        // Bluetooth still hides when no device is available.
                        val visibleButtons = LocalAppSettings.current.ongoingCallButtons.filter { button ->
                            button != OngoingCallButton.BLUETOOTH || isBluetoothAvailable
                        }
                        // Grid alignment: spread across the row (default),
                        // or clustered at the center / start / end.
                        val gridArrangement = when (LocalAppSettings.current.controlGridAlignment) {
                            ControlGridAlignment.SPREAD -> Arrangement.SpaceEvenly
                            ControlGridAlignment.CENTER -> Arrangement.spacedBy(
                                PhoneDimens.ongoingControlGridSpacing,
                                Alignment.CenterHorizontally
                            )
                            ControlGridAlignment.START -> Arrangement.spacedBy(
                                PhoneDimens.ongoingControlGridSpacing,
                                Alignment.Start
                            )
                            ControlGridAlignment.END -> Arrangement.spacedBy(
                                PhoneDimens.ongoingControlGridSpacing,
                                Alignment.End
                            )
                        }
                        visibleButtons.chunked(3).forEach { rowButtons ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = gridArrangement
                            ) {
                                rowButtons.forEach { button ->
                                    when (button) {
                                        OngoingCallButton.MUTE -> InCallControlButton(
                                            iconRes = if (callModel.isMuted) R.drawable.ic_mic_off else R.drawable.ic_mic,
                                            label = if (callModel.isMuted) {
                                                stringResource(id = R.string.call_action_unmute)
                                            } else {
                                                stringResource(id = R.string.call_action_mute)
                                            },
                                            isActive = callModel.isMuted,
                                            onClick = { onMuteToggle(!callModel.isMuted) },
                                            testTag = "mute_button"
                                        )
                                        OngoingCallButton.SPEAKER -> InCallControlButton(
                                            iconRes = R.drawable.ic_volume_up,
                                            label = stringResource(id = R.string.call_action_speaker),
                                            isActive = isSpeaker,
                                            onClick = onSpeakerToggle,
                                            testTag = "speaker_button"
                                        )
                                        OngoingCallButton.BLUETOOTH -> InCallControlButton(
                                            iconRes = R.drawable.ic_bluetooth,
                                            label = stringResource(id = R.string.call_action_bluetooth),
                                            isActive = isBluetooth,
                                            onClick = onBluetoothToggle,
                                            testTag = "bluetooth_button"
                                        )
                                        OngoingCallButton.KEYPAD -> InCallControlButton(
                                            iconRes = R.drawable.ic_dialpad,
                                            label = stringResource(id = R.string.call_action_keypad),
                                            isActive = false,
                                            onClick = { showKeypad = true },
                                            testTag = "keypad_button"
                                        )
                                        OngoingCallButton.HOLD -> InCallControlButton(
                                            iconRes = if (isHolding) R.drawable.ic_play else R.drawable.ic_pause,
                                            label = if (isHolding) {
                                                stringResource(id = R.string.call_action_resume)
                                            } else {
                                                stringResource(id = R.string.call_action_hold)
                                            },
                                            isActive = isHolding,
                                            onClick = { onHoldToggle(!isHolding) },
                                            testTag = "hold_button"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // End call: red circle button, scaled by the size setting.
                val endCallScale = LocalAppSettings.current.endCallButtonSize.scale
                Box(
                    modifier = Modifier
                        .padding(bottom = PhoneDimens.ongoingEndCallBottomPadding)
                        .size(PhoneDimens.ongoingEndCallButtonSize * endCallScale)
                        .clip(CircleShape)
                        .background(LiquidGlassEndCall)
                        .pressScale(endCallSource)
                        .clickable(
                            interactionSource = endCallSource,
                            indication = ripple()
                        ) { lightHaptic(); onEndCall() }
                        .testTag("end_call_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_call_end),
                        contentDescription = stringResource(id = R.string.call_action_end),
                        tint = Color.White,
                        modifier = Modifier.size(PhoneDimens.ongoingEndCallIconSize * endCallScale)
                    )
                }
            }
        }
    }
}

@Composable
private fun InCallControlButton(
    iconRes: Int,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val controlSource = remember { MutableInteractionSource() }
    val lightHaptic = rememberLightHaptic()
    val controlScale = LocalAppSettings.current.controlButtonSize.scale
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PhoneDimens.ongoingControlLabelGap)
    ) {
        Box(
            modifier = Modifier
                .size(PhoneDimens.ongoingControlButtonSize * controlScale)
                .clip(CircleShape)
                .background(if (isActive) InCallControlActive else InCallControlBackground)
                .pressScale(controlSource)
                .clickable(
                    interactionSource = controlSource,
                    indication = ripple()
                ) { lightHaptic(); onClick() }
                .testTag(testTag),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                tint = if (isActive) InCallControlActiveContent else Color.White,
                modifier = Modifier.size(PhoneDimens.ongoingControlIconSize * controlScale)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = PhoneDimens.ongoingControlLabelSize),
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun InCallKeypadGrid(
    isMuted: Boolean,
    isSpeaker: Boolean,
    onMuteToggle: () -> Unit,
    onSpeakerToggle: () -> Unit,
    onDigit: (Char) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keys = listOf(
        listOf('1' to "", '2' to "ABC", '3' to "DEF"),
        listOf('4' to "GHI", '5' to "JKL", '6' to "MNO"),
        listOf('7' to "PQRS", '8' to "TUV", '9' to "WXYZ"),
        listOf('*' to "", '0' to "+", '#' to "")
    )
    val lightHaptic = rememberLightHaptic()
    // Keypad key size scales the key circles and the digit/letters text.
    val keyScale = LocalAppSettings.current.dtmfKeySize.scale

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PhoneDimens.ongoingKeypadRowSpacing)
    ) {
        keys.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(PhoneDimens.ongoingKeypadColumnSpacing)
            ) {
                row.forEach { (digit, letters) ->
                    val keySource = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .size(PhoneDimens.ongoingDtmfKeySize * keyScale)
                            .clip(CircleShape)
                            .background(InCallControlBackground)
                            .pressScale(keySource)
                            .clickable(
                                interactionSource = keySource,
                                indication = ripple()
                            ) { lightHaptic(); onDigit(digit) }
                            .testTag("dtmf_key_$digit"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = digit.toString(),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = PhoneDimens.ongoingDtmfDigitSize * keyScale
                                ),
                                color = Color.White
                            )
                            if (letters.isNotEmpty()) {
                                Text(
                                    text = letters,
                                    style = MaterialTheme.typography.labelMedium.copy(fontSize = PhoneDimens.ongoingDtmfLettersSize * keyScale),
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(PhoneDimens.ongoingKeypadBottomRowGap))
        Row(
            horizontalArrangement = Arrangement.spacedBy(PhoneDimens.ongoingKeypadColumnSpacing)
        ) {
            InCallControlButton(
                iconRes = R.drawable.ic_volume_up,
                label = stringResource(id = R.string.call_action_speaker),
                isActive = isSpeaker,
                onClick = onSpeakerToggle,
                testTag = "keypad_speaker_button"
            )
            InCallControlButton(
                iconRes = R.drawable.ic_close,
                label = stringResource(id = R.string.call_action_hide),
                isActive = false,
                onClick = onClose,
                testTag = "keypad_hide_button"
            )
            InCallControlButton(
                iconRes = if (isMuted) R.drawable.ic_mic_off else R.drawable.ic_mic,
                label = if (isMuted) {
                    stringResource(id = R.string.call_action_unmute)
                } else {
                    stringResource(id = R.string.call_action_mute)
                },
                isActive = isMuted,
                onClick = onMuteToggle,
                testTag = "keypad_mute_button"
            )
        }
    }
}
