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
        // createNotificationChannel does NOT update importance of an existing channel.
        // If an old channel has the wrong importance, heads-up never triggers.
        // Delete and recreate when importance mismatches.
        createOrFix(
            notificationManager,
            INCOMING_CHANNEL_ID,
            "Incoming calls",
            NotificationManager.IMPORTANCE_HIGH,
            "Incoming call notifications",
            true
        )
        createOrFix(
            notificationManager,
            CALL_START_CHANNEL_ID,
            "Call start",
            NotificationManager.IMPORTANCE_HIGH,
            "Outgoing call starting notifications",
            false
        )
        createOrFix(
            notificationManager,
            ONGOING_CHANNEL_ID,
            "Ongoing calls",
            NotificationManager.IMPORTANCE_LOW,
            "Active and ongoing call notifications",
            false
        )
    }

    private fun createOrFix(
        notificationManager: NotificationManager,
        id: String,
        name: String,
        importance: Int,
        description: String,
        vibrate: Boolean
    ) {
        try {
            val existing = notificationManager.getNotificationChannel(id)
            if (existing != null && existing.importance != importance) {
                notificationManager.deleteNotificationChannel(id)
            }
        } catch (_: Throwable) {
        }
        val channel = NotificationChannel(id, name, importance).apply {
            this.description = description
            setSound(null, null)
            enableVibration(vibrate)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channel)
    }
}
