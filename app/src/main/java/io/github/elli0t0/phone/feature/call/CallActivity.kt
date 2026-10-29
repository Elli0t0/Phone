package io.github.elli0t0.phone.feature.call

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import kotlinx.coroutines.launch
import io.github.elli0t0.phone.core.recording.BackendStatus
import io.github.elli0t0.phone.core.recording.RecordingSession
import io.github.elli0t0.phone.core.recording.adb.openWirelessDebugging
import io.github.elli0t0.phone.core.recording.adb.postPairNotification
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import io.github.elli0t0.phone.PhoneApplication
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.GlassSurface
import io.github.elli0t0.phone.core.design.LocalHazeState
import io.github.elli0t0.phone.core.design.Motion
import io.github.elli0t0.phone.core.design.PhoneTheme
import io.github.elli0t0.phone.core.telecom.PhoneInCallService
import io.github.elli0t0.phone.core.telecom.PhoneNavLog
import io.github.elli0t0.phone.data.call.CallRepository
import io.github.elli0t0.phone.data.call.CallState
import io.github.elli0t0.phone.data.settings.model.CallBackground
import io.github.elli0t0.phone.data.settings.model.AppSettings
import io.github.elli0t0.phone.feature.call.ui.IncomingCallScreen
import io.github.elli0t0.phone.feature.call.ui.OngoingCallScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class CallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("PhoneCall", "activity onCreate CallActivity action=${intent?.action}")
        PhoneNavLog.log("CallActivity onCreate taskId=$taskId isFinishing=$isFinishing")
        try {
            val isDefault = io.github.elli0t0.phone.core.telecom.DefaultDialerHelper.isDefaultDialer(this)
            Log.d("PhoneCall", "activity onCreate isDefaultDialer=$isDefault")
        } catch (e: Exception) {
            Log.e("PhoneCall", "isDefaultDialer check failed", e)
        }
        turnScreenOnAndShowOverLockScreen()
        handleIntent(intent)

        val app = application as? PhoneApplication
        setContent {
            val settings by app?.container?.settingsRepository?.settingsFlow?.collectAsStateWithLifecycle(
                initialValue = AppSettings()
            ) ?: remember { mutableStateOf(AppSettings()) }

            // BLUETOOTH_CONNECT only when the user opens the audio route picker
            // and Bluetooth is available. Not requested at call start.
            var hasBluetoothPermission by remember {
                mutableStateOf(
                    ContextCompat.checkSelfPermission(
                        this@CallActivity,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                )
            }
            val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { granted ->
                hasBluetoothPermission = granted
                if (granted) {
                    PhoneInCallService.refreshBluetoothDevices()
                }
            }

            val quickDeclineMessages = listOf(
                R.string.quick_reply_cant_talk,
                R.string.quick_reply_call_back,
                R.string.quick_reply_in_meeting
            ).mapIndexed { index, res ->
                settings.quickDeclineOverrides.getOrNull(index)
                    ?.takeIf { it.isNotBlank() }
                    ?: getString(res)
            }

            PhoneTheme(appSettings = settings) {
                val ongoingCall by CallRepository.ongoingCallModel.collectAsStateWithLifecycle()
                val dtmfDigits by CallRepository.typedDtmfDigits.collectAsStateWithLifecycle()
                val postDialRemaining by CallRepository.postDialRemaining.collectAsStateWithLifecycle()

                // Recording state for the in-call record button. The
                // fallbacks only apply when the app container is
                // unavailable; in practice it always is.
                val recordingController = app?.container?.recordingController
                val recordingSession by (recordingController?.state
                    ?: remember { MutableStateFlow<RecordingSession>(RecordingSession.Idle) })
                    .collectAsStateWithLifecycle()
                val recordingBackendStatus by (recordingController?.backendStatus
                    ?: remember { MutableStateFlow(BackendStatus.NOT_RUNNING) })
                    .collectAsStateWithLifecycle()
                val recordingLevel by (recordingController?.recordingLevel
                    ?: remember { MutableStateFlow(0f) })
                    .collectAsStateWithLifecycle()
                val recordingArmed by (recordingController?.pendingRecording
                    ?: remember { MutableStateFlow(false) })
                    .collectAsStateWithLifecycle()
                val lastRecordingSilent by (recordingController?.lastRecordingSilent
                    ?: remember { MutableStateFlow(false) })
                    .collectAsStateWithLifecycle()
                val recordingScope = rememberCoroutineScope()
                var showRecordingNeeded by remember { mutableStateOf(false) }
                var recordingError by remember { mutableStateOf<String?>(null) }

                // A stop that measured silence raises the same dialog
                // as a start failure, so the user learns right away
                // instead of discovering a silent file later. The
                // controller resets the flag on every new start, so
                // this fires once per silent stop.
                val context = LocalContext.current
                LaunchedEffect(lastRecordingSilent) {
                    if (lastRecordingSilent) {
                        recordingError = context.getString(R.string.recording_silent_message)
                    }
                }

                // Post-dial wait (";" only; "," pauses continue on
                // their own): a glass dialog asking whether to send
                // the remaining tones. Dismissing counts as Cancel.
                if (postDialRemaining != null) {
                    PostDialDialog(
                        remaining = postDialRemaining!!,
                        onSend = { CallRepository.postDialContinue(true) },
                        onCancel = { CallRepository.postDialContinue(false) }
                    )
                }

                // Recording setup: the record button never dead-ends.
                // Each state offers the one action that moves it
                // forward; the full setup lives in Settings.
                if (showRecordingNeeded) {
                    RecordingStatusDialog(
                        title = stringResource(id = R.string.recording_dialog_title),
                        body = stringResource(
                            id = when (recordingBackendStatus) {
                                BackendStatus.NOT_PAIRED -> R.string.recording_needed_adb_not_paired
                                BackendStatus.NOT_RUNNING -> R.string.recording_needed_adb_not_running
                                else -> R.string.recording_needed_adb_error
                            }
                        ),
                        actionLabel = when (recordingBackendStatus) {
                            BackendStatus.NOT_PAIRED ->
                                stringResource(id = R.string.recording_action_pair)
                            BackendStatus.NOT_RUNNING ->
                                stringResource(id = R.string.recording_action_wireless_debugging)
                            else -> null
                        },
                        onAction = {
                            when (recordingBackendStatus) {
                                BackendStatus.NOT_PAIRED -> {
                                    postPairNotification(applicationContext)
                                    showRecordingNeeded = false
                                }
                                BackendStatus.NOT_RUNNING -> {
                                    openWirelessDebugging(applicationContext)
                                    showRecordingNeeded = false
                                }
                                else -> {
                                    showRecordingNeeded = false
                                }
                            }
                        },
                        onDismiss = { showRecordingNeeded = false }
                    )
                }
                if (recordingError != null) {
                    RecordingStatusDialog(
                        title = stringResource(id = R.string.recording_dialog_title),
                        // A blank or bare "unknown" message says
                        // nothing: show the actionable setup error
                        // instead of the raw text.
                        body = recordingError?.takeIf {
                            it.isNotBlank() && it.trim() != "unknown"
                        } ?: stringResource(id = R.string.recording_needed_adb_error),
                        actionLabel = null,
                        onAction = null,
                        onDismiss = { recordingError = null }
                    )
                }

                // Timer loop when active, keyed on state only: the
                // keypad is screen state, so opening it must not
                // restart or stop the loop. Duration is derived
                // from the connect time on every tick, so activity
                // recreation, screen-off and backgrounding can
                // neither reset nor lag it. Ticks align to
                // whole-second boundaries, so no second is ever
                // shown twice; the first tick corrects any stale
                // value immediately.
                LaunchedEffect(ongoingCall?.state) {
                    if (ongoingCall?.state == CallState.ACTIVE) {
                        while (isActive) {
                            val connect = CallRepository.ongoingCallModel.value?.connectTimeMillis ?: 0L
                            val now = SystemClock.elapsedRealtime()
                            val elapsed = if (connect > 0L) {
                                (now - connect).coerceAtLeast(0L)
                            } else {
                                0L
                            }
                            CallRepository.updateDuration(elapsed / 1000L)
                            delay(1000L - (elapsed % 1000L))
                        }
                    }
                }

                // Finish activity when call is removed or disconnected.
                // On disconnect, wait out the Motion end-call exit
                // animation, then finish as the fade completes.
                LaunchedEffect(ongoingCall) {
                    if (ongoingCall == null) {
                        finish()
                    } else if (ongoingCall?.state == CallState.DISCONNECTED) {
                        delay(Motion.innerDurationMs(settings.animationOption).toLong())
                        finish()
                    }
                }

                val currentModel = ongoingCall
                // Real glass blur, mirroring MainActivity: without a root
                // HazeState the call screens' glass falls back to flat
                // opaque. The provider + source wrap only the call
                // screens, so the post-dial dialog keeps its current look.
                val hazeState = remember { HazeState() }
                if (currentModel != null) {
                    CompositionLocalProvider(LocalHazeState provides hazeState) {
                        Box(
                            modifier = Modifier.fillMaxSize().hazeSource(hazeState)
                        ) {
                            // Incoming entrance, incoming -> ongoing, and the
                            // end-call exit all run through Motion (On / Reduced
                            // / Off). The exit fade plays while the effect above
                            // waits out the same duration.
                            AnimatedVisibility(
                                visible = currentModel.state != CallState.DISCONNECTED,
                                enter = EnterTransition.None,
                                // Plain fade, timed to innerDurationMs:
                                // the finish() delay below waits out
                                // exactly this exit, so it must fade to
                                // zero (never a parallax dim that cuts).
                                exit = fadeOut(
                                    animationSpec = tween(
                                        durationMillis = Motion.innerDurationMs(settings.animationOption)
                                    )
                                ),
                                label = "call_end"
                            ) {
                                AnimatedContent(
                                    targetState = currentModel.state == CallState.RINGING,
                                    transitionSpec = {
                                        Motion.innerContentTransform(settings.animationOption, settings.effectiveStyle.transitions, 1)
                                    },
                                    label = "call_state"
                                ) { ringing ->
                                    if (ringing) {
                                        IncomingCallScreen(
                                            callModel = currentModel,
                                            onAnswer = {
                                                CallRepository.answerCall()
                                                dismissKeyguardAfterAnswer()
                                            },
                                            onDecline = { CallRepository.rejectCall() },
                                            onDeclineWithMessage = { CallRepository.rejectCallWithMessage(it) },
                                            quickDeclineMessages = quickDeclineMessages,
                                            callBackground = settings.callBackground,
                                            accentColor = if (settings.callBackground == CallBackground.CUSTOM) {
                                                settings.callBackgroundCustomColor
                                            } else {
                                                settings.accentColor
                                            },
                                            dim = settings.callBackgroundDim,
                                            showNumber = settings.showNumberOnCallScreen,
                                            answerStyle = settings.answerStyle
                                        )
                                    } else {
                                        OngoingCallScreen(
                                            callModel = currentModel,
                                            onMuteToggle = { muted -> PhoneInCallService.setMuted(muted) },
                                            onAudioRouteSelect = { route -> PhoneInCallService.setAudioRoute(route) },
                                            onHoldToggle = { hold ->
                                                if (hold) CallRepository.holdCall() else CallRepository.unholdCall()
                                            },
                                            onDtmfDigit = { digit -> CallRepository.sendDtmf(digit) },
                                            dtmfDigits = dtmfDigits,
                                            onClearDtmfDigits = { CallRepository.clearDtmfDigits() },
                                            onKeypadHidden = { CallRepository.drainDtmf() },
                                            onEndCall = { CallRepository.disconnectCall() },
                                            onRequestBluetoothPermission = {
                                                bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                                            },
                                            hasBluetoothPermission = hasBluetoothPermission,
                                            callBackground = settings.callBackground,
                                            accentColor = if (settings.callBackground == CallBackground.CUSTOM) {
                                                settings.callBackgroundCustomColor
                                            } else {
                                                settings.accentColor
                                            },
                                            dim = settings.callBackgroundDim,
                                            showNumber = settings.showNumberOnCallScreen,
                                            isRecording = recordingSession is RecordingSession.Recording,
                                            recordingLevel = recordingLevel,
                                            recordingArmed = recordingArmed,
                                            onRecordToggle = {
                                                recordingScope.launch {
                                                    val controller = recordingController
                                                    if (controller != null) {
                                                        when (recordingSession) {
                                                            is RecordingSession.Recording -> controller.stopRecording()
                                                            else -> {
                                                                val result = controller.startCallRecording(
                                                                    currentModel.number,
                                                                    currentModel.displayName
                                                                )
                                                                if (result.isFailure) {
                                                                    if (controller.backendStatus.value == BackendStatus.READY) {
                                                                        recordingError = result.exceptionOrNull()?.message
                                                                    } else {
                                                                        showRecordingNeeded = true
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun turnScreenOnAndShowOverLockScreen() {
        // Show over the lock screen and turn it on, but do NOT dismiss
        // the keyguard here: that would unlock the phone while a call
        // is still ringing in the user's pocket. Dismissal happens only
        // after the user explicitly answers (see below). The pre-O_MR1
        // branch is gone: minSdk is 29, so it could never run.
        setShowWhenLocked(true)
        setTurnScreenOn(true)
    }

    // Dismiss the keyguard only once the user has committed to answering.
    private fun dismissKeyguardAfterAnswer() {
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        keyguardManager?.requestDismissKeyguard(this, null)
    }

    override fun onResume() {
        super.onResume()
        Log.d("PhoneCall", "activity resumed CallActivity")
        CallRepository.setCallActivityVisible(true)
        PhoneInCallService.onCallActivityResumed(this)
    }

    override fun onPause() {
        super.onPause()
        CallRepository.setCallActivityVisible(false)
        PhoneInCallService.onCallActivityPaused(this)
    }

    override fun onStart() {
        super.onStart()
        PhoneNavLog.log("CallActivity onStart taskId=$taskId isFinishing=$isFinishing")
    }

    override fun onStop() {
        super.onStop()
        PhoneNavLog.log("CallActivity onStop taskId=$taskId isFinishing=$isFinishing")
        CallRepository.drainDtmf()
    }

    override fun onDestroy() {
        super.onDestroy()
        PhoneNavLog.log("CallActivity onDestroy taskId=$taskId isFinishing=$isFinishing")
        CallRepository.setCallActivityVisible(false)
        PhoneInCallService.onCallActivityPaused(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        PhoneNavLog.log("CallActivity onNewIntent taskId=$taskId isFinishing=$isFinishing")
        setIntent(intent)
        handleIntent(intent)
        CallRepository.setCallActivityVisible(true)
        PhoneInCallService.onCallActivityResumed(this)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == PhoneInCallService.ACTION_ANSWER_CALL) {
            CallRepository.answerCall()
            dismissKeyguardAfterAnswer()
            PhoneInCallService.cancelIncomingNotification(this)
            PhoneInCallService.onCallActivityResumed(this)
        }
    }
}

/**
 * Glass post-dial dialog: the call hit a ";" wait and is asking
 * whether to send the remaining tones.
 */
@Composable
private fun PostDialDialog(
    remaining: String,
    onSend: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
        GlassSurface(
            shape = RoundedCornerShape(20.dp),
            dark = true,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.post_dial_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = remaining,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onCancel) {
                        Text(text = stringResource(id = R.string.action_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onSend,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(text = stringResource(id = R.string.post_dial_send))
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordingStatusDialog(
    title: String,
    body: String,
    actionLabel: String?,
    onAction: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        GlassSurface(
            shape = RoundedCornerShape(20.dp),
            dark = true,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(id = R.string.action_cancel))
                    }
                    if (actionLabel != null && onAction != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onAction,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(text = actionLabel)
                        }
                    }
                }
            }
        }
    }
}
