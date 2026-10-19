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

internal fun PhoneInCallService.buildIncomingNotification(call: Call, cachedDisplayName: String? = null): Notification? {
    try {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return null
        ensureIncomingChannel(notificationManager)

        val handle = call.details?.handle
        val rawNumber = handle?.schemeSpecificPart ?: ""
        val currentDisplayName = cachedDisplayName
            ?: CallRepository.ongoingCallModel.value?.displayName
            ?: call.details?.contactDisplayName?.takeIf { it.isNotBlank() }
            ?: call.details?.callerDisplayName?.takeIf { it.isNotBlank() }

        val title = currentDisplayName?.takeIf { it.isNotBlank() }
            ?: PhoneNumberFormatter.format(rawNumber).takeIf { it.isNotBlank() }
            ?: "Incoming call"
        val contentText = if (currentDisplayName != null && rawNumber.isNotBlank() && currentDisplayName != rawNumber) {
            PhoneNumberFormatter.format(rawNumber)
        } else {
            "Incoming call"
        }

        val contentIntent = Intent(this, CallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            PhoneInCallService.REQUEST_CODE_CONTENT,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val fullScreenIntent = Intent(this, CallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            PhoneInCallService.REQUEST_CODE_FULL_SCREEN,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val answerIntent = Intent(this, CallActivity::class.java).apply {
            action = PhoneInCallService.ACTION_ANSWER_CALL
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val answerPendingIntent = PendingIntent.getActivity(
            this,
            PhoneInCallService.REQUEST_CODE_ANSWER,
            answerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val declineIntent = Intent(this, CallActionReceiver::class.java).apply {
            action = PhoneInCallService.ACTION_DECLINE_CALL
        }
        val declinePendingIntent = PendingIntent.getBroadcast(
            this,
            PhoneInCallService.REQUEST_CODE_DECLINE,
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val callerPerson = Person.Builder()
            .setName(title)
            .setImportant(true)
            .build()

        val callStyle = NotificationCompat.CallStyle.forIncomingCall(
            callerPerson,
            declinePendingIntent,
            answerPendingIntent
        )

        val builder = NotificationCompat.Builder(this, PhoneInCallService.INCOMING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_phone)
            .setContentTitle(title)
            .setContentText(contentText)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(callStyle)

        // Full-screen intent only when the call-display setting is
        // Full screen. With Pop-up the notification shows heads-up /
        // banner only and never takes over the screen. Gating it to
        // locked removes the heads-up pop on unlocked devices.
        val canUseFullScreen = if (Build.VERSION.SDK_INT >= 34) {
            try {
                notificationManager.canUseFullScreenIntent()
            } catch (e: Exception) {
                Log.e(PhoneInCallService.TAG, "canUseFullScreenIntent check failed", e)
                true
            }
        } else {
            true
        }
        if (canUseFullScreen && cachedCallDisplay == CallDisplay.FULL_SCREEN) {
            try {
                builder.setFullScreenIntent(fullScreenPendingIntent, true)
            } catch (e: SecurityException) {
                Log.e(PhoneInCallService.TAG, "setFullScreenIntent SecurityException", e)
            } catch (e: Exception) {
                Log.e(PhoneInCallService.TAG, "setFullScreenIntent failed", e)
            }
        }
        return builder.build()
    } catch (e: Exception) {
        Log.e(PhoneInCallService.TAG, "buildIncomingNotification failed", e)
        return null
    }
}

internal fun PhoneInCallService.buildCallStartNotification(call: Call, cachedDisplayName: String? = null): Notification? {
    try {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return null
        ensureCallStartChannel(notificationManager)

        val handle = call.details?.handle
        val rawNumber = handle?.schemeSpecificPart ?: ""
        val currentDisplayName = cachedDisplayName
            ?: CallRepository.ongoingCallModel.value?.displayName
            ?: call.details?.contactDisplayName?.takeIf { it.isNotBlank() }
            ?: call.details?.callerDisplayName?.takeIf { it.isNotBlank() }

        val title = currentDisplayName?.takeIf { it.isNotBlank() }
            ?: PhoneNumberFormatter.format(rawNumber).takeIf { it.isNotBlank() }
            ?: "Call"

        val statusText = when (call.state) {
            Call.STATE_DIALING -> "Dialing"
            Call.STATE_CONNECTING -> "Connecting"
            Call.STATE_SELECT_PHONE_ACCOUNT -> "Selecting SIM"
            Call.STATE_HOLDING -> "On hold"
            Call.STATE_ACTIVE -> "Active call"
            else -> "Calling"
        }

        val contentText = if (currentDisplayName != null && rawNumber.isNotBlank() && currentDisplayName != rawNumber) {
            "${PhoneNumberFormatter.format(rawNumber)} • $statusText"
        } else {
            statusText
        }

        val contentIntent = Intent(this, CallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            PhoneInCallService.REQUEST_CODE_CONTENT,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val fullScreenIntent = Intent(this, CallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            PhoneInCallService.REQUEST_CODE_FULL_SCREEN,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val hangUpIntent = Intent(this, CallActionReceiver::class.java).apply {
            action = PhoneInCallService.ACTION_HANG_UP_CALL
        }
        val hangUpPendingIntent = PendingIntent.getBroadcast(
            this,
            PhoneInCallService.REQUEST_CODE_HANG_UP,
            hangUpIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val callerPerson = Person.Builder()
            .setName(title)
            .setImportant(true)
            .build()

        val callStyle = NotificationCompat.CallStyle.forOngoingCall(
            callerPerson,
            hangUpPendingIntent
        )

        val builder = NotificationCompat.Builder(this, PhoneInCallService.CALL_START_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_phone)
            .setContentTitle(title)
            .setContentText(contentText)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(callStyle)

        val canUseFullScreen = if (Build.VERSION.SDK_INT >= 34) {
            try {
                notificationManager.canUseFullScreenIntent()
            } catch (e: Exception) {
                Log.e(PhoneInCallService.TAG, "canUseFullScreenIntent check failed", e)
                true
            }
        } else {
            true
        }

        if (canUseFullScreen) {
            try {
                builder.setFullScreenIntent(fullScreenPendingIntent, true)
            } catch (e: SecurityException) {
                Log.e(PhoneInCallService.TAG, "setFullScreenIntent SecurityException", e)
            } catch (e: Exception) {
                Log.e(PhoneInCallService.TAG, "setFullScreenIntent failed", e)
            }
        }
        return builder.build()
    } catch (e: Exception) {
        Log.e(PhoneInCallService.TAG, "buildCallStartNotification failed", e)
        return null
    }
}

internal fun PhoneInCallService.buildOngoingNotification(call: Call, cachedDisplayName: String? = null): Notification? {
    try {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return null
        ensureOngoingChannel(notificationManager)

        val handle = call.details?.handle
        val rawNumber = handle?.schemeSpecificPart ?: ""
        val currentDisplayName = cachedDisplayName
            ?: CallRepository.ongoingCallModel.value?.displayName
            ?: call.details?.contactDisplayName?.takeIf { it.isNotBlank() }
            ?: call.details?.callerDisplayName?.takeIf { it.isNotBlank() }

        val title = currentDisplayName?.takeIf { it.isNotBlank() }
            ?: PhoneNumberFormatter.format(rawNumber).takeIf { it.isNotBlank() }
            ?: "Call"

        val statusText = when (call.state) {
            Call.STATE_DIALING -> "Dialing"
            Call.STATE_CONNECTING -> "Connecting"
            Call.STATE_SELECT_PHONE_ACCOUNT -> "Selecting SIM"
            Call.STATE_HOLDING -> "On hold"
            Call.STATE_ACTIVE -> "Active call"
            else -> "Ongoing call"
        }

        val contentText = if (currentDisplayName != null && rawNumber.isNotBlank() && currentDisplayName != rawNumber) {
            "${PhoneNumberFormatter.format(rawNumber)} • $statusText"
        } else {
            statusText
        }

        val contentIntent = Intent(this, CallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            PhoneInCallService.REQUEST_CODE_CONTENT,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val hangUpIntent = Intent(this, CallActionReceiver::class.java).apply {
            action = PhoneInCallService.ACTION_HANG_UP_CALL
        }
        val hangUpPendingIntent = PendingIntent.getBroadcast(
            this,
            PhoneInCallService.REQUEST_CODE_HANG_UP,
            hangUpIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val callerPerson = Person.Builder()
            .setName(title)
            .setImportant(true)
            .build()

        val callStyle = NotificationCompat.CallStyle.forOngoingCall(
            callerPerson,
            hangUpPendingIntent
        )

        // Quiet ongoing notification: LOW importance, no heads-up, no full-screen intent.
        val builder = NotificationCompat.Builder(this, PhoneInCallService.ONGOING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_phone)
            .setContentTitle(title)
            .setContentText(contentText)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(callStyle)

        val connectTime = call.details?.connectTimeMillis ?: 0L
        if (connectTime > 0L && call.state == Call.STATE_ACTIVE) {
            builder.setWhen(connectTime)
            builder.setUsesChronometer(true)
            builder.setShowWhen(true)
        } else {
            builder.setUsesChronometer(false)
            builder.setShowWhen(false)
        }
        return builder.build()
    } catch (e: Exception) {
        Log.e(PhoneInCallService.TAG, "buildOngoingNotification failed", e)
        return null
    }
}

