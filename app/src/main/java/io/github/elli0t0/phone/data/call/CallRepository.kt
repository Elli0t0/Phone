package io.github.elli0t0.phone.data.call

import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.VideoProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object CallRepository {
    private val _currentCall = MutableStateFlow<Call?>(null)
    val currentCall: StateFlow<Call?> = _currentCall.asStateFlow()

    private val _ongoingCallModel = MutableStateFlow<OngoingCallModel?>(null)
    val ongoingCallModel: StateFlow<OngoingCallModel?> = _ongoingCallModel.asStateFlow()

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call?, state: Int) {
            updateCallModel()
        }

        override fun onDetailsChanged(call: Call?, details: Call.Details?) {
            updateCallModel()
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

    fun updateDuration(durationSeconds: Long) {
        val current = _ongoingCallModel.value ?: return
        _ongoingCallModel.value = current.copy(durationSeconds = durationSeconds)
    }

    fun updateDisplayName(name: String?) {
        val current = _ongoingCallModel.value ?: return
        if (!name.isNullOrBlank()) {
            _ongoingCallModel.value = current.copy(displayName = name)
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

        _ongoingCallModel.value = OngoingCallModel(
            id = call.hashCode().toString(),
            number = rawNumber,
            displayName = effectiveDisplayName,
            state = state,
            durationSeconds = currentModel?.durationSeconds ?: 0L,
            isMuted = currentModel?.isMuted ?: false,
            isSpeakerOn = currentModel?.isSpeakerOn ?: false,
            audioRoute = currentModel?.audioRoute ?: CallAudioState.ROUTE_EARPIECE,
            availableAudioRoutes = currentModel?.availableAudioRoutes ?: (CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER)
        )
    }

    fun answerCall() {
        _currentCall.value?.answer(VideoProfile.STATE_AUDIO_ONLY)
    }

    fun rejectCall() {
        val call = _currentCall.value ?: return
        if (call.state == Call.STATE_RINGING) {
            call.reject(false, null)
        } else {
            call.disconnect()
        }
    }

    fun disconnectCall() {
        _currentCall.value?.disconnect()
    }

    fun holdCall() {
        _currentCall.value?.hold()
    }

    fun unholdCall() {
        _currentCall.value?.unhold()
    }

    fun playDtmfTone(digit: Char) {
        _currentCall.value?.playDtmfTone(digit)
    }

    fun stopDtmfTone() {
        _currentCall.value?.stopDtmfTone()
    }
}
