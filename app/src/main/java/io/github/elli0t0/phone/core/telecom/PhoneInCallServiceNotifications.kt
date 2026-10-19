package io.github.elli0t0.phone.core.telecom
import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.PhoneApplication
import io.github.elli0t0.phone.core.notifications.CallNotificationChannels
import io.github.elli0t0.phone.core.common.PhoneNumberFormatter
import io.github.elli0t0.phone.data.call.CallRepository
import io.github.elli0t0.phone.data.contacts.ContactNameIndex
import io.github.elli0t0.phone.data.settings.model.CallDisplay
import io.github.elli0t0.phone.feature.call.CallActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun PhoneInCallService.updateCallNotification(call: Call, cachedDisplayName: String? = null) {
    try {
        when {
            call.state == Call.STATE_RINGING -> {
                if (currentForegroundId != NOTIFICATION_INCOMING_ID) {
                    startForegroundForCall(call)
                } else {
                    postIncomingCallNotification(call, cachedDisplayName)
                }
            }
            isOutgoingState(call.state) || isActiveState(call.state) -> {
                if (isActivityVisible || CallRepository.isCallActivityVisible.value) {
                    transitionToOngoing(call, cachedDisplayName)
                } else {
                    postCallStartNotification(call, cachedDisplayName)
                }
            }
            else -> {
                dismissNotification()
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "updateCallNotification failed", e)
    }
}

internal fun PhoneInCallService.publishForeground(id: Int, notification: Notification) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(id, notification)
        } else {
            @Suppress("DEPRECATION")
            startForeground(id, notification)
        }
        currentForegroundId = id
        Log.d(TAG, "notification posted foreground id=$id")
    } catch (e: Exception) {
        Log.e(TAG, "startForeground failed, falling back to notify", e)
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(id, notification)
        Log.d(TAG, "notification posted notify fallback id=$id")
    }
}

internal fun PhoneInCallService.transitionToOngoing(call: Call, cachedDisplayName: String? = null) {
    try {
        val notification = buildOngoingNotification(call, cachedDisplayName) ?: return
        // Replace the foreground notification with LOW ongoing. Cancel alone
        // does NOT update the foreground notification: the HIGH one stays.
        publishForeground(NOTIFICATION_ONGOING_ID, notification)
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(NOTIFICATION_INCOMING_ID)
        notificationManager?.cancel(NOTIFICATION_CALL_START_ID)
        Log.d(TAG, "transitioned foreground to ongoing LOW id=$NOTIFICATION_ONGOING_ID")
    } catch (e: Exception) {
        Log.e(TAG, "transitionToOngoing failed", e)
    }
}

internal fun PhoneInCallService.startForegroundForCall(call: Call) {
    try {
        CallNotificationChannels.createAll(this)
        val notification: Notification? = when {
            call.state == Call.STATE_RINGING -> buildIncomingNotification(call, null)
            isOutgoingState(call.state) -> buildCallStartNotification(call, null)
            isActiveState(call.state) -> buildOngoingNotification(call, null)
            else -> buildCallStartNotification(call, null)
        }
        if (notification == null) {
            Log.e(TAG, "startForegroundForCall built null notification state=${call.state}")
            return
        }
        val foregroundId = when {
            call.state == Call.STATE_RINGING -> NOTIFICATION_INCOMING_ID
            isOutgoingState(call.state) -> NOTIFICATION_CALL_START_ID
            else -> NOTIFICATION_ONGOING_ID
        }
        publishForeground(foregroundId, notification)
    } catch (e: Exception) {
        Log.e(TAG, "startForegroundForCall failed", e)
    }
}

internal fun PhoneInCallService.ensureIncomingChannel(notificationManager: NotificationManager) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            INCOMING_CHANNEL_ID,
            "Incoming calls",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Incoming call notifications"
            setSound(null, null)
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channel)
    }
}

internal fun PhoneInCallService.ensureCallStartChannel(notificationManager: NotificationManager) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            CALL_START_CHANNEL_ID,
            "Call start",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Outgoing call starting notifications"
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channel)
    }
}

internal fun PhoneInCallService.ensureOngoingChannel(notificationManager: NotificationManager) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        try {
            notificationManager.deleteNotificationChannel(OLD_ONGOING_CHANNEL_ID)
        } catch (e: Exception) {
            Log.e(TAG, "delete old ongoing channel failed", e)
        }

        val channel = NotificationChannel(
            ONGOING_CHANNEL_ID,
            "Ongoing calls",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Active and ongoing call notifications"
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channel)
    }
}


internal fun PhoneInCallService.postIncomingCallNotification(call: Call, cachedDisplayName: String? = null) {
    if (call.state != Call.STATE_RINGING) {
        dismissNotification()
        return
    }
    try {
        val notification = buildIncomingNotification(call, cachedDisplayName) ?: return
        if (currentForegroundId == NOTIFICATION_INCOMING_ID) {
            publishForeground(NOTIFICATION_INCOMING_ID, notification)
        } else {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            notificationManager.cancel(NOTIFICATION_ONGOING_ID)
            notificationManager.cancel(NOTIFICATION_CALL_START_ID)
            notificationManager.notify(NOTIFICATION_INCOMING_ID, notification)
            Log.d(TAG, "notification posted incoming id=$NOTIFICATION_INCOMING_ID")
        }
    } catch (e: Exception) {
        Log.e(TAG, "postIncomingCallNotification failed", e)
    }
}

internal fun PhoneInCallService.postCallStartNotification(call: Call, cachedDisplayName: String? = null) {
    if (call.state == Call.STATE_DISCONNECTED || call.state == Call.STATE_DISCONNECTING) {
        dismissNotification()
        return
    }
    try {
        val notification = buildCallStartNotification(call, cachedDisplayName) ?: return
        if (currentForegroundId == NOTIFICATION_CALL_START_ID) {
            publishForeground(NOTIFICATION_CALL_START_ID, notification)
        } else {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            notificationManager.notify(NOTIFICATION_CALL_START_ID, notification)
            Log.d(TAG, "notification posted callStart id=$NOTIFICATION_CALL_START_ID")
        }
    } catch (e: Exception) {
        Log.e(TAG, "postCallStartNotification failed", e)
    }
}

internal fun PhoneInCallService.postOngoingCallNotification(call: Call, cachedDisplayName: String? = null) {
    if (call.state == Call.STATE_DISCONNECTED || call.state == Call.STATE_DISCONNECTING) {
        dismissNotification()
        return
    }
    transitionToOngoing(call, cachedDisplayName)
}

