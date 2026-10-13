package io.github.elli0t0.phone.core.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * Creates the call notification channels at app start.
 * Channel IDs match the ones used by PhoneInCallService.
 */
object CallNotificationChannels {
    const val INCOMING_CHANNEL_ID = "phone_incoming_call_channel"
    const val CALL_START_CHANNEL_ID = "phone_call_start_channel"
    const val ONGOING_CHANNEL_ID = "phone_ongoing_call_channel"
    const val OLD_ONGOING_CHANNEL_ID = "phone_in_call_channel"

    fun createAll(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        try {
            notificationManager.deleteNotificationChannel(OLD_ONGOING_CHANNEL_ID)
        } catch (_: Throwable) {
        }
        val incoming = NotificationChannel(
            INCOMING_CHANNEL_ID,
            "Incoming calls",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Incoming call notifications"
            setSound(null, null)
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val callStart = NotificationChannel(
            CALL_START_CHANNEL_ID,
            "Call start",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Outgoing call starting notifications"
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val ongoing = NotificationChannel(
            ONGOING_CHANNEL_ID,
            "Ongoing calls",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Active and ongoing call notifications"
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(incoming)
        notificationManager.createNotificationChannel(callStart)
        notificationManager.createNotificationChannel(ongoing)
    }
}
