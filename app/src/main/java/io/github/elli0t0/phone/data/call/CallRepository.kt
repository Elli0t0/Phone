package io.github.elli0t0.phone.data.call

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.SystemClock
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.VideoProfile
import android.telephony.SmsManager
import android.widget.Toast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.telecom.PhoneNavLog

object CallRepository {
    private val _currentCall = MutableStateFlow<Call?>(null)
    val currentCall: StateFlow<Call?> = _currentCall.asStateFlow()

    private val _ongoingCallModel = MutableStateFlow<OngoingCallModel?>(null)
    val ongoingCallModel: StateFlow<OngoingCallModel?> = _ongoingCallModel.asStateFlow()

    private val _isCallActivityVisible = MutableStateFlow(false)
    val isCallActivityVisible: StateFlow<Boolean> = _isCallActivityVisible.asStateFlow()

    fun setCallActivityVisible(visible: Boolean) {
        _isCallActivityVisible.value = visible
    }

    // Serialized DTMF tones: without the queue, tones were played on
    // click and never stopped, so Android ignored every tone after
    // the first. Only ACTIVE calls play tones; anything queued while
    // the call is not ACTIVE is dropped by the queue itself.
    private val dtmfQueue = DtmfToneQueue(
        player = object : DtmfTonePlayer {
            override fun play(digit: Char) {
                _currentCall.value?.playDtmfTone(digit)
            }

            override fun stop() {
                _currentCall.value?.stopDtmfTone()
            }
        },
        isCallActive = { _currentCall.value?.state == Call.STATE_ACTIVE }
    )

    // Digits typed in this call, shown above the in-call keys.
    private val _typedDtmfDigits = MutableStateFlow("")
    val typedDtmfDigits: StateFlow<String> = _typedDtmfDigits.asStateFlow()

    // Remaining post-dial wait string (";" waits only; "," pauses
    // continue automatically). Null when no wait is pending.
    private val _postDialRemaining = MutableStateFlow<String?>(null)
    val postDialRemaining: StateFlow<String?> = _postDialRemaining.asStateFlow()

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call?, state: Int) {
            if (state == Call.STATE_HOLDING || state == Call.STATE_DISCONNECTED) {
                dtmfQueue.drain()
            }
            updateCallModel()
        }

        override fun onDetailsChanged(call: Call?, details: Call.Details?) {
            updateCallModel()
        }

        override fun onPostDialWait(call: Call?, remaining: String?) {
            if (remaining != null) {
                _postDialRemaining.value = remaining
            }
        }
    }

    fun onCallAdded(call: Call) {
        _currentCall.value = call
        call.registerCallback(callCallback)
        updateCallModel()
    }

    fun onCallRemoved(call: Call) {
        if (_currentCall.value == call) {
            call.unregisterCallback(callCallback)
            _currentCall.value = null
            _ongoingCallModel.value = null
            dtmfQueue.drain()
            _typedDtmfDigits.value = ""
            _postDialRemaining.value = null
        }
    }

    fun updateAudioState(audioState: CallAudioState?) {
        val current = _ongoingCallModel.value ?: return
        if (audioState != null) {
            _ongoingCallModel.value = current.copy(
                isMuted = audioState.isMuted,
                audioRoute = audioState.route,
                availableAudioRoutes = audioState.supportedRouteMask,
                isSpeakerOn = (audioState.route and CallAudioState.ROUTE_SPEAKER) != 0
            )
        }
    }

    fun updateBluetoothDevices(names: List<String>) {
        val current = _ongoingCallModel.value ?: return
        if (current.bluetoothDeviceNames != names) {
            _ongoingCallModel.value = current.copy(bluetoothDeviceNames = names)
        }
    }

    fun updateDuration(durationSeconds: Long) {
        val current = _ongoingCallModel.value ?: return
        if (current.durationSeconds != durationSeconds) {
            _ongoingCallModel.value = current.copy(durationSeconds = durationSeconds)
        }
    }

    fun updateDisplayName(name: String?) {
        val current = _ongoingCallModel.value ?: return
        if (!name.isNullOrBlank()) {
            _ongoingCallModel.value = current.copy(displayName = name)
        }
    }

    fun updatePhotoUri(uri: String?) {
        val current = _ongoingCallModel.value ?: return
        if (!uri.isNullOrBlank() && current.photoUri != uri) {
            _ongoingCallModel.value = current.copy(photoUri = uri)
        }
    }

    fun updateSimLabel(label: String?) {
        val current = _ongoingCallModel.value ?: return
        if (!label.isNullOrBlank() && current.simLabel != label) {
            _ongoingCallModel.value = current.copy(simLabel = label)
        }
    }

    private fun updateCallModel() {
        val call = _currentCall.value
        if (call == null) {
            _ongoingCallModel.value = null
            return
        }

        val handle = call.details?.handle
        val rawNumber = handle?.schemeSpecificPart ?: ""
        val callerDisplayName = call.details?.callerDisplayName
        val contactDisplayName = call.details?.contactDisplayName

        val state = when (call.state) {
            Call.STATE_RINGING -> CallState.RINGING
            Call.STATE_DIALING -> CallState.DIALING
            Call.STATE_CONNECTING -> CallState.CONNECTING
            Call.STATE_ACTIVE -> CallState.ACTIVE
            Call.STATE_HOLDING -> CallState.HOLDING
            Call.STATE_DISCONNECTED -> CallState.DISCONNECTED
            else -> CallState.IDLE
        }

        val currentModel = _ongoingCallModel.value
        val effectiveDisplayName = currentModel?.displayName
            ?: contactDisplayName?.takeIf { it.isNotBlank() }
            ?: callerDisplayName?.takeIf { it.isNotBlank() }

        // Connect time, kept in elapsed-realtime basis so the timer
        // is monotonic: Details.connectTimeMillis is wall-clock
        // epoch, so it is converted once (exact unless the wall
        // clock jumped since connect). Captured once and kept on
        // every rebuild, so activity recreation can never reset it;
        // when Details has no connect time yet, the elapsed clock
        // is captured the moment the call becomes ACTIVE.
        val detailsConnect = call.details?.connectTimeMillis ?: 0L
        val previousConnect = currentModel?.connectTimeMillis ?: 0L
        val connectTimeMillis = when {
            previousConnect != 0L -> previousConnect
            detailsConnect > 0L -> SystemClock.elapsedRealtime() -
                (System.currentTimeMillis() - detailsConnect)
            state == CallState.ACTIVE -> SystemClock.elapsedRealtime()
            else -> 0L
        }

        _ongoingCallModel.value = OngoingCallModel(
            id = call.hashCode().toString(),
            number = rawNumber,
            displayName = effectiveDisplayName,
            state = state,
            durationSeconds = currentModel?.durationSeconds ?: 0L,
            connectTimeMillis = connectTimeMillis,
            isMuted = currentModel?.isMuted ?: false,
            isSpeakerOn = currentModel?.isSpeakerOn ?: false,
            audioRoute = currentModel?.audioRoute ?: CallAudioState.ROUTE_EARPIECE,
            availableAudioRoutes = currentModel?.availableAudioRoutes ?: (CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER),
            photoUri = currentModel?.photoUri,
            simLabel = currentModel?.simLabel
        )
    }

    fun answerCall() {
        PhoneNavLog.log("CallRepository answerCall")
        _currentCall.value?.answer(VideoProfile.STATE_AUDIO_ONLY)
    }

    fun rejectCall() {
        PhoneNavLog.log("CallRepository rejectCall")
        val call = _currentCall.value ?: return
        if (call.state == Call.STATE_RINGING) {
            call.reject(false, null)
        } else {
            call.disconnect()
        }
    }

    /**
     * Declines a ringing call and sends the given text as a reply SMS
     * with SmsManager for the SIM the call came in on (subscription
     * id from the call's PhoneAccountHandle, default SMS sub as a
     * fallback). The system reject-with-message path leaves delivery
     * to the carrier and silently drops it on some devices, so the
     * SMS is sent directly and the result is toasted + logged. A
     * per-part sent intent counts all parts before reporting, so a
     * multi-part message reports once, not per part.
     */
    fun rejectCallWithMessage(context: Context, number: String, message: String) {
        PhoneNavLog.log("CallRepository rejectCallWithMessage")
        val call = _currentCall.value
        if (call != null && call.state == Call.STATE_RINGING) {
            call.reject(false, null)
        } else {
            call?.disconnect()
            return
        }
        if (number.isBlank()) return
        val appContext = context.applicationContext
        val name = _ongoingCallModel.value?.displayName?.takeIf { it.isNotBlank() } ?: number
        val subId = call.details?.accountHandle?.id?.toIntOrNull()
            ?: SmsManager.getDefaultSmsSubscriptionId()
        val sms = if (Build.VERSION.SDK_INT >= 31) {
            appContext.getSystemService(SmsManager::class.java).createForSubscriptionId(subId)
        } else {
            SmsManager.getSmsManagerForSubscriptionId(subId)
        }
        val parts = sms.divideMessage(message)
        if (parts.isEmpty()) return
        val action = "io.github.elli0t0.phone.QUICK_REPLY_SENT_${System.nanoTime()}"
        var remaining = parts.size
        var failed = false
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                if (resultCode != Activity.RESULT_OK) failed = true
                if (--remaining == 0) {
                    try {
                        appContext.unregisterReceiver(this)
                    } catch (_: Exception) {
                        // Already unregistered on the send-failure path.
                    }
                    val text = if (!failed) {
                        appContext.getString(R.string.quick_reply_sent, name)
                    } else {
                        appContext.getString(R.string.quick_reply_failed)
                    }
                    Toast.makeText(appContext, text, Toast.LENGTH_SHORT).show()
                    PhoneNavLog.log("CallRepository quick reply SMS ${if (!failed) "sent" else "failed"}")
                }
            }
        }
        if (Build.VERSION.SDK_INT >= 33) {
            appContext.registerReceiver(receiver, IntentFilter(action), Context.RECEIVER_NOT_EXPORTED)
        } else {
            appContext.registerReceiver(receiver, IntentFilter(action))
        }
        val sentIntents = parts.mapIndexed { index, _ ->
            PendingIntent.getBroadcast(
                appContext,
                index,
                Intent(action),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        try {
            sms.sendMultipartTextMessage(number, null, parts, sentIntents, null)
        } catch (e: Exception) {
            try {
                appContext.unregisterReceiver(receiver)
            } catch (_: Exception) {
                // Never registered or already unregistered.
            }
            Toast.makeText(appContext, R.string.quick_reply_failed, Toast.LENGTH_SHORT).show()
            PhoneNavLog.log("CallRepository quick reply SMS failed: ${e.message}")
        }
    }

    /** System reply path, used only when SEND_SMS is denied. */
    fun rejectCallWithMessageFallback(message: String) {
        PhoneNavLog.log("CallRepository rejectCallWithMessageFallback")
        val call = _currentCall.value ?: return
        if (call.state == Call.STATE_RINGING) {
            call.reject(true, message)
        } else {
            call.disconnect()
        }
    }

    fun disconnectCall() {
        PhoneNavLog.log("CallRepository disconnectCall")
        _currentCall.value?.disconnect()
    }

    fun holdCall() {
        _currentCall.value?.hold()
    }

    fun unholdCall() {
        _currentCall.value?.unhold()
    }

    /** Queues one DTMF tone and appends it to this call's typed digits. */
    fun sendDtmf(digit: Char) {
        if (_currentCall.value?.state != Call.STATE_ACTIVE) return
        _typedDtmfDigits.value += digit
        dtmfQueue.send(digit)
    }

    /** Clears the typed-digits display and drops any tones still queued. */
    fun clearDtmfDigits() {
        _typedDtmfDigits.value = ""
        dtmfQueue.drain()
    }

    /** Drops queued tones and stops the tone playing right now. */
    fun drainDtmf() {
        dtmfQueue.drain()
    }

    /** Continues (true) or cancels (false) a post-dial wait. */
    fun postDialContinue(continueAfter: Boolean) {
        _currentCall.value?.postDialContinue(continueAfter)
        _postDialRemaining.value = null
    }
}
