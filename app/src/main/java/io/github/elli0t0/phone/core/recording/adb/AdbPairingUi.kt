package io.github.elli0t0.phone.core.recording.adb

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.recording.RecordingForegroundService

/** Broadcast action carrying the pairing code typed into the notification. */
const val PAIR_CODE_ACTION = "io.github.elli0t0.phone.action.PAIR_CODE"

/** RemoteInput key for the six-digit pairing code. */
const val PAIR_CODE_KEY = "pair_code"

const val PAIR_NOTIFICATION_ID = 4401
const val PAIR_RESULT_NOTIFICATION_ID = 4402

/**
 * Opens the system Developer options screen, where Wireless debugging
 * lives. No public intent targets the wireless-debugging detail screen,
 * so the user opens it from there.
 */
fun openWirelessDebugging(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    runCatching { context.startActivity(intent) }
}

fun ensureRecordingChannel(context: Context) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        ?: return
    if (manager.getNotificationChannel(RecordingForegroundService.CHANNEL_ID) == null) {
        manager.createNotificationChannel(
            NotificationChannel(
                RecordingForegroundService.CHANNEL_ID,
                context.getString(R.string.recording_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }
}

/**
 * Posts the pairing notification and opens Developer options. The user
 * opens "Pair device with pairing code" in wireless debugging and types
 * the six digits into this notification's reply — the system pairing
 * dialog must stay open, and the notification shade is the only place
 * the code can be typed without dismissing it.
 */
fun postPairNotification(context: Context) {
    ensureRecordingChannel(context)
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        ?: return
    val replyIntent = Intent(context, PairCodeReceiver::class.java).apply {
        action = PAIR_CODE_ACTION
    }
    val replyFlags = PendingIntent.FLAG_UPDATE_CURRENT or
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        }
    val replyPendingIntent = PendingIntent.getBroadcast(context, 0, replyIntent, replyFlags)
    val remoteInput = RemoteInput.Builder(PAIR_CODE_KEY)
        .setLabel(context.getString(R.string.recording_pair_code_hint))
        .build()
    val pairAction = NotificationCompat.Action.Builder(
        0,
        context.getString(R.string.recording_action_pair),
        replyPendingIntent
    ).addRemoteInput(remoteInput).build()
    val settingsIntent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    val settingsPendingIntent = PendingIntent.getActivity(
        context,
        0,
        settingsIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val notification = NotificationCompat.Builder(context, RecordingForegroundService.CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_record)
        .setContentTitle(context.getString(R.string.recording_pair_notification_title))
        .setContentText(context.getString(R.string.recording_pair_notification_text))
        .setContentIntent(settingsPendingIntent)
        .addAction(pairAction)
        .setAutoCancel(true)
        .build()
    manager.notify(PAIR_NOTIFICATION_ID, notification)
    openWirelessDebugging(context)
}

/**
 * Replaces the pairing notification the moment the code is submitted:
 * a plain ongoing "Pairing…" notification with indeterminate progress
 * and no reply action, so the system "sending" spinner stops. It uses
 * the same id, so the result notification later cancels it.
 */
fun postPairingProgressNotification(context: Context) {
    ensureRecordingChannel(context)
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        ?: return
    val notification = NotificationCompat.Builder(context, RecordingForegroundService.CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_record)
        .setContentTitle(context.getString(R.string.recording_pairing_progress))
        .setProgress(0, 0, true)
        .setOngoing(true)
        .build()
    manager.notify(PAIR_NOTIFICATION_ID, notification)
}
