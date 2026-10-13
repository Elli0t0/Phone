package io.github.elli0t0.phone.core.telecom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.elli0t0.phone.data.call.CallRepository
import io.github.elli0t0.phone.feature.call.CallActivity

class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            PhoneInCallService.ACTION_DECLINE_CALL -> {
                CallRepository.rejectCall()
                PhoneInCallService.dismissNotification(context)
            }
            PhoneInCallService.ACTION_ANSWER_CALL -> {
                CallRepository.answerCall()
                PhoneInCallService.dismissNotification(context)
                val callIntent = Intent(context, CallActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                }
                context.startActivity(callIntent)
            }
        }
    }
}
