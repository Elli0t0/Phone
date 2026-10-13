package io.github.elli0t0.phone.core.telecom

import android.content.Intent
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import io.github.elli0t0.phone.data.call.CallRepository
import io.github.elli0t0.phone.feature.call.CallActivity

class PhoneInCallService : InCallService() {

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallRepository.onCallAdded(call)

        // Launch CallActivity
        val intent = Intent(this, CallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        }
        startActivity(intent)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        CallRepository.onCallRemoved(call)
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        CallRepository.updateAudioState(audioState)
    }

    companion object {
        private var instance: PhoneInCallService? = null

        fun setAudioRoute(route: Int) {
            instance?.setAudioRoute(route)
        }

        fun setMuted(muted: Boolean) {
            instance?.setMuted(muted)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}
