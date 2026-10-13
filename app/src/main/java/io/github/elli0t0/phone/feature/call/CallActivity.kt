package io.github.elli0t0.phone.feature.call

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.elli0t0.phone.PhoneApplication
import io.github.elli0t0.phone.core.design.PhoneTheme
import io.github.elli0t0.phone.core.telecom.PhoneInCallService
import io.github.elli0t0.phone.data.call.CallRepository
import io.github.elli0t0.phone.data.call.CallState
import io.github.elli0t0.phone.data.settings.model.AppSettings
import io.github.elli0t0.phone.feature.call.ui.IncomingCallScreen
import io.github.elli0t0.phone.feature.call.ui.OngoingCallScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class CallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        turnScreenOnAndShowOverLockScreen()
        handleIntent(intent)

        val app = application as? PhoneApplication
        setContent {
            val settings by app?.container?.settingsRepository?.settingsFlow?.collectAsStateWithLifecycle(
                initialValue = AppSettings()
            ) ?: remember { mutableStateOf(AppSettings()) }

            PhoneTheme(appSettings = settings) {
                val ongoingCall by CallRepository.ongoingCallModel.collectAsStateWithLifecycle()

                // Timer loop when active
                LaunchedEffect(ongoingCall?.state) {
                    if (ongoingCall?.state == CallState.ACTIVE) {
                        var seconds = ongoingCall?.durationSeconds ?: 0L
                        while (isActive) {
                            delay(1000L)
                            seconds++
                            CallRepository.updateDuration(seconds)
                        }
                    }
                }

                // Finish activity when call is removed or disconnected
                LaunchedEffect(ongoingCall) {
                    if (ongoingCall == null) {
                        finish()
                    } else if (ongoingCall?.state == CallState.DISCONNECTED) {
                        delay(600L)
                        finish()
                    }
                }

                val currentModel = ongoingCall
                if (currentModel != null) {
                    if (currentModel.state == CallState.RINGING) {
                        IncomingCallScreen(
                            callModel = currentModel,
                            onAnswer = { CallRepository.answerCall() },
                            onDecline = { CallRepository.rejectCall() }
                        )
                    } else {
                        OngoingCallScreen(
                            callModel = currentModel,
                            onMuteToggle = { muted -> PhoneInCallService.setMuted(muted) },
                            onAudioRouteSelect = { route -> PhoneInCallService.setAudioRoute(route) },
                            onHoldToggle = { hold ->
                                if (hold) CallRepository.holdCall() else CallRepository.unholdCall()
                            },
                            onDtmfDigit = { digit -> CallRepository.playDtmfTone(digit) },
                            onEndCall = { CallRepository.disconnectCall() }
                        )
                    }
                }
            }
        }
    }

    private fun turnScreenOnAndShowOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == PhoneInCallService.ACTION_ANSWER_CALL) {
            CallRepository.answerCall()
            PhoneInCallService.cancelIncomingNotification(this)
        }
    }
}
