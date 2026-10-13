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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.elli0t0.phone.R
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1C1B1F))
            .padding(horizontal = spacing.screenHorizontalPadding, vertical = spacing.large)
            .testTag("ongoing_call_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Caller Info Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 40.dp)
        ) {
            val hasName = !callModel.displayName.isNullOrBlank() && callModel.displayName != callModel.number
            val titleText = if (hasName) callModel.displayName!! else callModel.number

            Text(
                text = titleText,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                ),
                color = Color.White,
                modifier = Modifier.padding(bottom = spacing.extraSmall)
            )

            if (hasName && callModel.number.isNotBlank()) {
                Text(
                    text = callModel.number,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = spacing.extraSmall)
                )
            }

            Text(
                text = stateText,
                style = MaterialTheme.typography.titleMedium,
                color = if (callModel.state == CallState.HOLDING) Color(0xFFFFB74D) else Color.White.copy(alpha = 0.7f)
            )
        }

        // Center: Controls or In-Call DTMF Keypad
        if (showKeypad) {
            InCallKeypadGrid(
                onDigit = onDtmfDigit,
                onClose = { showKeypad = false }
            )
        } else {
            // Action Grid: Mute, Keypad, Audio Route, Hold
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Mute
                    InCallActionButton(
                        iconRes = if (callModel.isMuted) R.drawable.ic_mic_off else R.drawable.ic_mic,
                        label = if (callModel.isMuted) stringResource(id = R.string.call_action_unmute) else stringResource(id = R.string.call_action_mute),
                        isActive = callModel.isMuted,
                        onClick = { onMuteToggle(!callModel.isMuted) },
                        testTag = "mute_button"
                    )

                    // Keypad
                    InCallActionButton(
                        iconRes = R.drawable.ic_dialpad,
                        label = stringResource(id = R.string.call_action_keypad),
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

                    InCallActionButton(
                        iconRes = audioIcon,
                        label = audioLabel,
                        isActive = isSpeaker || isBluetooth || isWired,
                        onClick = { showAudioPicker = true },
                        testTag = "speaker_button"
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Hold
                    val isHolding = callModel.state == CallState.HOLDING
                    InCallActionButton(
                        iconRes = if (isHolding) R.drawable.ic_play else R.drawable.ic_pause,
                        label = if (isHolding) stringResource(id = R.string.call_action_resume) else stringResource(id = R.string.call_action_hold),
                        isActive = isHolding,
                        onClick = { onHoldToggle(!isHolding) },
                        testTag = "hold_button"
                    )
                }
            }
        }

        // Bottom: End Call Button (Big Red Circle)
        IconButton(
            onClick = onEndCall,
            modifier = Modifier
                .padding(bottom = 40.dp)
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFFE53935))
                .testTag("end_call_button")
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_call_end),
                contentDescription = stringResource(id = R.string.call_action_end),
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
private fun InCallActionButton(
    iconRes: Int,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val bgColor = if (isActive) Color.White else Color(0xFF333333)
    val contentColor = if (isActive) Color(0xFF1C1B1F) else Color.White

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(bgColor)
                .clickable(onClick = onClick)
                .testTag(testTag),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(spacing.extraSmall))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.85f)
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
                            .clip(CircleShape)
                            .background(Color(0xFF333333))
                            .clickable { onDigit(digit) }
                            .testTag("dtmf_key_$digit"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = digit.toString(),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(id = R.string.action_cancel),
            color = Color.White.copy(alpha = 0.7f),
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
