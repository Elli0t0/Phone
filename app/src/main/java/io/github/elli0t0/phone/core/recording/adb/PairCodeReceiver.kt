package io.github.elli0t0.phone.core.recording.adb

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import io.github.elli0t0.phone.PhoneApplication
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.recording.RecordingForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives the six-digit pairing code typed into the pairing
 * notification and runs the pairing. The result is posted as a
 * notification because the user is in system Settings at that point,
 * not in Phone.
 */
class PairCodeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != PAIR_CODE_ACTION) return
        val code = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(PAIR_CODE_KEY)?.toString()?.trim()
        if (code.isNullOrBlank()) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as PhoneApplication
                val result = app.container.recordingController.pairAdb(code)
                postResult(context, result.isSuccess, result.exceptionOrNull()?.message)
            } finally {
                pending.finish()
            }
        }
    }

    private fun postResult(context: Context, success: Boolean, error: String?) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        manager.cancel(PAIR_NOTIFICATION_ID)
        val notification = NotificationCompat.Builder(context, RecordingForegroundService.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_record)
            .setContentTitle(
                context.getString(
                    if (success) {
                        R.string.recording_paired
                    } else {
                        R.string.recording_pair_failed
                    }
                )
            )
            .setContentText(
                if (success) {
                    context.getString(R.string.recording_paired_text)
                } else {
                    error ?: context.getString(R.string.recording_pair_failed_text)
                }
            )
            .setAutoCancel(true)
            .build()
        manager.notify(PAIR_RESULT_NOTIFICATION_ID, notification)
    }
}
