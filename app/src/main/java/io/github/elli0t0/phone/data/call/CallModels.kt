package io.github.elli0t0.phone.data.call

import android.telecom.Call
import android.telecom.CallAudioState

enum class CallState {
    IDLE,
    CONNECTING,
    DIALING,
    RINGING,
    ACTIVE,
    HOLDING,
    DISCONNECTED
}

data class OngoingCallModel(
    val id: String,
    val number: String,
    val displayName: String?,
    val state: CallState,
    val durationSeconds: Long = 0L,
    // Connect time in elapsed-realtime basis (see CallRepository):
    // 0 while the call has not connected yet.
    val connectTimeMillis: Long = 0L,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val audioRoute: Int = CallAudioState.ROUTE_EARPIECE,
    val availableAudioRoutes: Int = CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER,
    val bluetoothDeviceNames: List<String> = emptyList(),
    val photoUri: String? = null,
    val simLabel: String? = null
)
