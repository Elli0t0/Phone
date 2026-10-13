package io.github.elli0t0.phone.feature.call

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
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
        Log.d("PhoneCall", "activity onCreate CallActivity action=${intent?.action}")
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
                            onEndCall = { CallRepository.disconnectCall() },
                            onRequestBluetoothPermission = {
                                bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                            },
                            hasBluetoothPermission = hasBluetoothPermission
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

    override fun onDestroy() {
        super.onDestroy()
        CallRepository.setCallActivityVisible(false)
        PhoneInCallService.onCallActivityPaused(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
        CallRepository.setCallActivityVisible(true)
        PhoneInCallService.onCallActivityResumed(this)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == PhoneInCallService.ACTION_ANSWER_CALL) {
            CallRepository.answerCall()
            PhoneInCallService.cancelIncomingNotification(this)
            PhoneInCallService.onCallActivityResumed(this)
        }
    }
}
