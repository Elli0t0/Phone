package io.github.elli0t0.phone.feature.call.ui

import android.telecom.CallAudioState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.FrostedGlassCard
import io.github.elli0t0.phone.core.design.LiquidGlassActive
import io.github.elli0t0.phone.core.design.LiquidGlassActiveContent
import io.github.elli0t0.phone.core.design.LiquidGlassBackgroundBottom
import io.github.elli0t0.phone.core.design.LiquidGlassBackgroundTop
import io.github.elli0t0.phone.core.design.LiquidGlassBorder
import io.github.elli0t0.phone.core.design.LiquidGlassDockShape
import io.github.elli0t0.phone.core.design.LiquidGlassEndCall
import io.github.elli0t0.phone.core.design.LiquidGlassGlowPrimary
import io.github.elli0t0.phone.core.design.LiquidGlassGlowSecondary
import io.github.elli0t0.phone.core.design.LiquidGlassHold
import io.github.elli0t0.phone.core.design.LiquidGlassOnSurface
import io.github.elli0t0.phone.core.design.LiquidGlassOnSurfaceFaint
import io.github.elli0t0.phone.core.design.LiquidGlassOnSurfaceMuted
import io.github.elli0t0.phone.core.design.LiquidGlassPillShape
import io.github.elli0t0.phone.core.design.LiquidGlassSurfaceStrong
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.SquircleShape
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
    var showAudioPicker by remember { mutableStateOf(false) }

    if (showAudioPicker) {
        AudioRoutePickerDialog(
            callModel = callModel,
            hasBluetoothPermission = hasBluetoothPermission,
            onRequestBluetoothPermission = {
                showAudioPicker = false
                onRequestBluetoothPermission()
            },
            onRouteSelected = { route ->
                showAudioPicker = false
                onAudioRouteSelect(route)
            },
            onDismiss = { showAudioPicker = false }
        )
    }

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
    val titleText = if (hasName) callModel.displayName!! else callModel.number
    val initials = when {
        hasName -> callModel.displayName!!.trim().first().uppercaseChar().toString()
        callModel.number.isNotBlank() -> callModel.number.trim().first().toString()
        else -> "#"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("ongoing_call_screen")
    ) {
        // Frosted background: dark gradient with soft color glows behind the glass.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(LiquidGlassBackgroundTop, LiquidGlassBackgroundBottom)
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(360.dp)
                .offset(x = (-80).dp, y = (-60).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            LiquidGlassGlowPrimary.copy(alpha = 0.38f),
                            LiquidGlassGlowPrimary.copy(alpha = 0f)
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(340.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 90.dp, y = 120.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            LiquidGlassGlowSecondary.copy(alpha = 0.30f),
                            LiquidGlassGlowSecondary.copy(alpha = 0f)
                        )
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
            // Caller info: squircle avatar, name, number, state.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .clip(SquircleShape)
                        .background(LiquidGlassSurfaceStrong)
                        .border(1.dp, LiquidGlassBorder, SquircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                        color = LiquidGlassOnSurface
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    ),
                    color = LiquidGlassOnSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = spacing.extraSmall)
                )
                if (hasName && callModel.number.isNotBlank()) {
                    Text(
                        text = callModel.number,
                        style = MaterialTheme.typography.bodyLarge,
                        color = LiquidGlassOnSurfaceMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = spacing.extraSmall)
                    )
                }
                Text(
                    text = stateText,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (callModel.state == CallState.HOLDING) LiquidGlassHold else LiquidGlassOnSurfaceFaint
                )
            }

            // Center: in-call DTMF keypad, or empty space when the dock is shown.
            if (showKeypad) {
                InCallKeypadGrid(
                    onDigit = onDtmfDigit,
                    onClose = { showKeypad = false }
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            // Bottom: floating glass dock + prominent pill end-call button.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!showKeypad) {
                    FrostedGlassCard(
                        shape = LiquidGlassDockShape,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Mute
                            GlassDockButton(
                                iconRes = if (callModel.isMuted) R.drawable.ic_mic_off else R.drawable.ic_mic,
                                contentDescription = if (callModel.isMuted) stringResource(id = R.string.call_action_unmute) else stringResource(id = R.string.call_action_mute),
                                isActive = callModel.isMuted,
                                onClick = { onMuteToggle(!callModel.isMuted) },
                                testTag = "mute_button"
                            )

                            // Keypad
                            GlassDockButton(
                                iconRes = R.drawable.ic_dialpad,
                                contentDescription = stringResource(id = R.string.call_action_keypad),
                                isActive = false,
                                onClick = { showKeypad = true },
                                testTag = "keypad_button"
                            )

                            // Audio output: opens route picker (Earpiece / Speaker / Wired / Bluetooth)
                            val isSpeaker = callModel.isSpeakerOn || (callModel.audioRoute and CallAudioState.ROUTE_SPEAKER) != 0
                            val isBluetooth = (callModel.audioRoute and CallAudioState.ROUTE_BLUETOOTH) != 0
                            val isWired = (callModel.audioRoute and CallAudioState.ROUTE_WIRED_HEADSET) != 0
                            val audioIcon = if (isBluetooth) {
                                R.drawable.ic_bluetooth
                            } else if (isSpeaker) {
                                R.drawable.ic_volume_up
                            } else if (isWired) {
                                R.drawable.ic_headset
                            } else {
                                R.drawable.ic_phone
                            }
                            val audioLabel = if (isBluetooth) {
                                stringResource(id = R.string.call_action_bluetooth)
                            } else if (isSpeaker) {
                                stringResource(id = R.string.call_action_speaker)
                            } else if (isWired) {
                                stringResource(id = R.string.call_audio_wired)
                            } else {
                                stringResource(id = R.string.call_action_earpiece)
                            }

                            GlassDockButton(
                                iconRes = audioIcon,
                                contentDescription = audioLabel,
                                isActive = isSpeaker || isBluetooth || isWired,
                                onClick = { showAudioPicker = true },
                                testTag = "speaker_button"
                            )

                            // Hold
                            val isHolding = callModel.state == CallState.HOLDING
                            GlassDockButton(
                                iconRes = if (isHolding) R.drawable.ic_play else R.drawable.ic_pause,
                                contentDescription = if (isHolding) stringResource(id = R.string.call_action_resume) else stringResource(id = R.string.call_action_hold),
                                isActive = isHolding,
                                onClick = { onHoldToggle(!isHolding) },
                                testTag = "hold_button"
                            )
                        }
                    }
                }

                // End call: bottom-anchored pill button.
                Box(
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .height(64.dp)
                        .width(176.dp)
                        .clip(LiquidGlassPillShape)
                        .background(LiquidGlassEndCall)
                        .clickable(onClick = onEndCall)
                        .testTag("end_call_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_call_end),
                            contentDescription = null,
                            tint = LiquidGlassOnSurface,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = stringResource(id = R.string.call_action_end),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = LiquidGlassOnSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassDockButton(
    iconRes: Int,
    contentDescription: String,
    isActive: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(60.dp)
            .clip(SquircleShape)
            .background(if (isActive) LiquidGlassActive else LiquidGlassSurfaceStrong)
            .border(1.dp, LiquidGlassBorder, SquircleShape)
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            tint = if (isActive) LiquidGlassActiveContent else LiquidGlassOnSurface,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun InCallKeypadGrid(
    onDigit: (Char) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keys = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
        listOf('*', '0', '#')
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        keys.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                row.forEach { digit ->
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(SquircleShape)
                            .background(LiquidGlassSurfaceStrong)
                            .border(1.dp, LiquidGlassBorder, SquircleShape)
                            .clickable { onDigit(digit) }
                            .testTag("dtmf_key_$digit"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = digit.toString(),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = LiquidGlassOnSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(id = R.string.action_cancel),
            color = LiquidGlassOnSurfaceFaint,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClose)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun AudioRoutePickerDialog(
    callModel: OngoingCallModel,
    hasBluetoothPermission: Boolean,
    onRequestBluetoothPermission: () -> Unit,
    onRouteSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val spacing = LocalSpacing.current
    val available = callModel.availableAudioRoutes
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.call_audio_output),
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if ((available and CallAudioState.ROUTE_EARPIECE) != 0) {
                    AudioRouteRow(
                        label = stringResource(id = R.string.call_action_earpiece),
                        iconRes = R.drawable.ic_phone,
                        isSelected = (callModel.audioRoute and CallAudioState.ROUTE_EARPIECE) != 0,
                        onClick = { onRouteSelected(CallAudioState.ROUTE_EARPIECE) },
                        testTag = "audio_route_earpiece"
                    )
                    Spacer(modifier = Modifier.height(spacing.small))
                }
                if ((available and CallAudioState.ROUTE_SPEAKER) != 0) {
                    AudioRouteRow(
                        label = stringResource(id = R.string.call_action_speaker),
                        iconRes = R.drawable.ic_volume_up,
                        isSelected = (callModel.audioRoute and CallAudioState.ROUTE_SPEAKER) != 0,
                        onClick = { onRouteSelected(CallAudioState.ROUTE_SPEAKER) },
                        testTag = "audio_route_speaker"
                    )
                    Spacer(modifier = Modifier.height(spacing.small))
                }
                if ((available and CallAudioState.ROUTE_WIRED_HEADSET) != 0) {
                    AudioRouteRow(
                        label = stringResource(id = R.string.call_audio_wired),
                        iconRes = R.drawable.ic_headset,
                        isSelected = (callModel.audioRoute and CallAudioState.ROUTE_WIRED_HEADSET) != 0,
                        onClick = { onRouteSelected(CallAudioState.ROUTE_WIRED_HEADSET) },
                        testTag = "audio_route_wired"
                    )
                    Spacer(modifier = Modifier.height(spacing.small))
                }
                if ((available and CallAudioState.ROUTE_BLUETOOTH) != 0) {
                    val isBluetoothActive = (callModel.audioRoute and CallAudioState.ROUTE_BLUETOOTH) != 0
                    if (callModel.bluetoothDeviceNames.isNotEmpty()) {
                        callModel.bluetoothDeviceNames.forEach { name ->
                            AudioRouteRow(
                                label = name,
                                iconRes = R.drawable.ic_bluetooth,
                                isSelected = isBluetoothActive,
                                onClick = { onRouteSelected(CallAudioState.ROUTE_BLUETOOTH) },
                                testTag = "audio_route_bluetooth_$name"
                            )
                            Spacer(modifier = Modifier.height(spacing.small))
                        }
                    } else if (!hasBluetoothPermission) {
                        AudioRouteRow(
                            label = stringResource(id = R.string.call_audio_bluetooth_grant),
                            iconRes = R.drawable.ic_bluetooth,
                            isSelected = isBluetoothActive,
                            onClick = onRequestBluetoothPermission,
                            testTag = "audio_route_bluetooth_grant"
                        )
                        Spacer(modifier = Modifier.height(spacing.small))
                    } else {
                        AudioRouteRow(
                            label = stringResource(id = R.string.call_action_bluetooth),
                            iconRes = R.drawable.ic_bluetooth,
                            isSelected = isBluetoothActive,
                            onClick = { onRouteSelected(CallAudioState.ROUTE_BLUETOOTH) },
                            testTag = "audio_route_bluetooth"
                        )
                        Spacer(modifier = Modifier.height(spacing.small))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun AudioRouteRow(
    label: String,
    iconRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.small, vertical = spacing.small)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(spacing.medium))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(
                painter = painterResource(id = R.drawable.ic_check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
