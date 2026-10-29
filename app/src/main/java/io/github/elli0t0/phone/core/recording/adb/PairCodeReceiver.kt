package io.github.elli0t0.phone.core.recording.adb

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import io.github.elli0t0.phone.PhoneApplication
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.recording.PairResult
import io.github.elli0t0.phone.core.recording.RecordingForegroundService
import kotlinx.coroutines.launch

/**
 * Receives the six-digit pairing code typed into the pairing
 * notification and runs the pairing. The result is posted as a
 * notification because the user is in system Settings at that point,
 * not in Phone.
 *
 * The code submission is acknowledged immediately: the notification is
 * replaced with an ongoing "Pairing…" one (so the system "sending"
 * spinner stops) and the work runs in the application scope, not in
 * goAsync — discovery alone takes up to 30 s, far past its ~10 s
 * budget. The pending result is finished right after launching.
 */
class PairCodeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != PAIR_CODE_ACTION) return
        val code = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(PAIR_CODE_KEY)?.toString()?.trim()
        if (code.isNullOrBlank()) return
        postPairingProgressNotification(context)
        val app = context.applicationContext as PhoneApplication
        val pending = goAsync()
        app.applicationScope.launch {
            val controller = app.container.recordingController
            val result = controller.pairAdb(code)
            // In-app feedback too, not only the notification: the
            // settings screen shows this line when the user returns.
            controller.publishPairResult(
                if (result.isSuccess) {
                    PairResult.Paired
                } else {
                    PairResult.Failed(
                        result.exceptionOrNull()?.message
                            ?: context.getString(R.string.recording_pair_failed_text)
                    )
                }
            )
            controller.refreshBackendStatus()
            postResult(context, result.isSuccess, result.exceptionOrNull()?.message)
        }
        pending.finish()
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
