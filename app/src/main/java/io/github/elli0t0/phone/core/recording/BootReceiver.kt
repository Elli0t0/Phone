package io.github.elli0t0.phone.core.recording

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.elli0t0.phone.PhoneApplication
import kotlinx.coroutines.launch

/**
 * After a reboot the recorder helper is gone until Wi-Fi +
 * wireless debugging come back. If auto-record is on, check
 * once the system has settled and warn when recording cannot
 * start, so the first call after reboot is not silently missed.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as? PhoneApplication ?: return
        app.applicationScope.launch {
            runCatching { app.container.recordingController.checkReadyAfterBoot() }
        }
    }
}
