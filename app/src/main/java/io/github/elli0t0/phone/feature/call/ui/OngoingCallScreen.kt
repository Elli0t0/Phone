package io.github.elli0t0.phone.feature.call.ui

import android.telecom.CallAudioState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.common.PhoneNumberFormatter
import io.github.elli0t0.phone.core.design.InCallControlActive
import io.github.elli0t0.phone.core.design.InCallControlActiveContent
import io.github.elli0t0.phone.core.design.InCallControlBackground
import io.github.elli0t0.phone.core.design.InCallGradientBottom
import io.github.elli0t0.phone.core.design.InCallGradientTop
import io.github.elli0t0.phone.core.design.LiquidGlassEndCall
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.data.call.CallState
import io.github.elli0t0.phone.data.call.OngoingCallModel

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
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    var showKeypad by remember { mutableStateOf(false) }

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
                .background(
                    Brush.verticalGradient(
                        colors = listOf(InCallGradientTop, InCallGradientBottom)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = spacing.screenHorizontalPadding, vertical = spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Caller info: name, number, state or duration.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = spacing.extraSmall)
                )
                if (hasName && callModel.number.isNotBlank()) {
                    Text(
                        text = formattedNumber,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = spacing.extraSmall)
                    )
                }
                Text(
                    text = stateText,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!showKeypad) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            InCallControlButton(
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
                            InCallControlButton(
                                iconRes = R.drawable.ic_volume_up,
                                label = stringResource(id = R.string.call_action_speaker),
                                isActive = isSpeaker,
                                onClick = onSpeakerToggle,
                                testTag = "speaker_button"
                            )
                            if (isBluetoothAvailable) {
                                InCallControlButton(
                                    iconRes = R.drawable.ic_bluetooth,
                                    label = stringResource(id = R.string.call_action_bluetooth),
                                    isActive = isBluetooth,
                                    onClick = onBluetoothToggle,
                                    testTag = "bluetooth_button"
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            InCallControlButton(
                                iconRes = R.drawable.ic_dialpad,
                                label = stringResource(id = R.string.call_action_keypad),
                                isActive = false,
                                onClick = { showKeypad = true },
                                testTag = "keypad_button"
                            )
                            InCallControlButton(
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

                // End call: red circle button.
                Box(
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(LiquidGlassEndCall)
                        .clickable(onClick = onEndCall)
                        .testTag("end_call_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_call_end),
                        contentDescription = stringResource(id = R.string.call_action_end),
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
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
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(if (isActive) InCallControlActive else InCallControlBackground)
                .clickable(onClick = onClick)
                .testTag(testTag),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                tint = if (isActive) InCallControlActiveContent else Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
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

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        keys.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                row.forEach { (digit, letters) ->
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(InCallControlBackground)
                            .clickable { onDigit(digit) }
                            .testTag("dtmf_key_$digit"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = digit.toString(),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 26.sp
                                ),
                                color = Color.White
                            )
                            if (letters.isNotEmpty()) {
                                Text(
                                    text = letters,
                                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(32.dp)
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
