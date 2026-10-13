package io.github.elli0t0.phone.core.telecom

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telephony.PhoneNumberUtils
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.data.call.CallRepository
import io.github.elli0t0.phone.feature.call.CallActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PhoneInCallService : InCallService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var lookupJob: Job? = null
    private var callStartTimeoutJob: Job? = null

    @Volatile
    private var isActivityVisible = false

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call?, state: Int) {
            call?.let { handleCallState(it) }
        }

        override fun onDetailsChanged(call: Call?, details: Call.Details?) {
            call?.let { updateCallNotification(it) }
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        call.registerCallback(callCallback)
        CallRepository.onCallAdded(call)

        resolveContactNameInBackground(call)

        if (isOutgoingState(call.state)) {
            // Outgoing call (NEW, SELECT_PHONE_ACCOUNT, CONNECTING, DIALING):
            // 1. Immediately startActivity of the in-call activity
            startCallActivity()
            // 2. Post notification on HIGH importance "call start" channel with fullScreenIntent
            if (isActivityVisible || CallRepository.isCallActivityVisible.value) {
                cancelCallStartNotification()
                postOngoingCallNotification(call)
            } else {
                postCallStartNotification(call)
                callStartTimeoutJob?.cancel()
                callStartTimeoutJob = serviceScope.launch {
                    delay(1500L)
                    // If activity is not visible after 1.5s, keep HIGH notification so user can tap it
                }
            }
        } else {
            handleCallState(call)
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callCallback)
        lookupJob?.cancel()
        callStartTimeoutJob?.cancel()
        callStartTimeoutJob = null
        dismissNotification()
        CallRepository.onCallRemoved(call)
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        CallRepository.updateAudioState(audioState)
    }

    private fun isOutgoingState(state: Int): Boolean {
        return state == Call.STATE_NEW ||
            state == Call.STATE_SELECT_PHONE_ACCOUNT ||
            state == Call.STATE_CONNECTING ||
            state == Call.STATE_DIALING
    }

    private fun isActiveState(state: Int): Boolean {
        return state == Call.STATE_ACTIVE ||
            state == Call.STATE_HOLDING
    }

    private fun handleCallState(call: Call) {
        when {
            call.state == Call.STATE_RINGING -> {
                // Incoming ringing call: show heads-up banner, full-screen only on lock screen
                cancelCallStartNotification()
                postIncomingCallNotification(call)
            }
            isOutgoingState(call.state) -> {
                startCallActivity()
                if (isActivityVisible || CallRepository.isCallActivityVisible.value) {
                    cancelCallStartNotification()
                    postOngoingCallNotification(call)
                } else {
                    postCallStartNotification(call)
                }
            }
            isActiveState(call.state) -> {
                cancelIncomingNotification()
                startCallActivity()
                if (isActivityVisible || CallRepository.isCallActivityVisible.value) {
                    cancelCallStartNotification()
                    postOngoingCallNotification(call)
                } else {
                    postCallStartNotification(call)
                }
            }
            call.state == Call.STATE_DISCONNECTED || call.state == Call.STATE_DISCONNECTING -> {
                callStartTimeoutJob?.cancel()
                callStartTimeoutJob = null
                dismissNotification()
            }
            else -> {
                if (isActivityVisible || CallRepository.isCallActivityVisible.value) {
                    cancelCallStartNotification()
                    postOngoingCallNotification(call)
                } else {
                    postCallStartNotification(call)
                }
            }
        }
    }

    private fun startCallActivity() {
        startCallActivity(this)
    }

    private fun resolveContactNameInBackground(call: Call) {
        lookupJob?.cancel()
        lookupJob = serviceScope.launch {
            val handle = call.details?.handle
            val rawNumber = handle?.schemeSpecificPart ?: ""
            val resolvedName = resolveName(rawNumber, call)
            if (!resolvedName.isNullOrBlank()) {
                withContext(Dispatchers.Main) {
                    CallRepository.updateDisplayName(resolvedName)
                    updateCallNotification(call, resolvedName)
                }
            }
        }
    }

    private fun resolveName(rawNumber: String, call: Call): String? {
        if (rawNumber.isNotBlank() && ContextCompat.checkSelfPermission(
                this@PhoneInCallService,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val asIs = queryPhoneLookup(rawNumber)
            if (asIs != null) return asIs

            val normalized = PhoneNumberUtils.normalizeNumber(rawNumber)
            if (!normalized.isNullOrBlank() && normalized != rawNumber) {
                val fromNormalized = queryPhoneLookup(normalized)
                if (fromNormalized != null) return fromNormalized
            }
        }

        return call.details?.contactDisplayName?.takeIf { it.isNotBlank() }
            ?: call.details?.callerDisplayName?.takeIf { it.isNotBlank() }
    }

    private fun queryPhoneLookup(number: String): String? {
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx)?.takeIf { it.isNotBlank() } else null
                } else null
            }
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun updateCallNotification(call: Call, cachedDisplayName: String? = null) {
        when {
            call.state == Call.STATE_RINGING -> {
                cancelCallStartNotification()
                postIncomingCallNotification(call, cachedDisplayName)
            }
            isOutgoingState(call.state) || isActiveState(call.state) -> {
                cancelIncomingNotification()
                if (isActivityVisible || CallRepository.isCallActivityVisible.value) {
                    cancelCallStartNotification()
                    postOngoingCallNotification(call, cachedDisplayName)
                } else {
                    postCallStartNotification(call, cachedDisplayName)
                }
            }
            else -> {
                dismissNotification()
            }
        }
    }

    private fun ensureIncomingChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                INCOMING_CHANNEL_ID,
                "Incoming calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming call notifications"
                setSound(null, null)
                enableVibration(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun ensureCallStartChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CALL_START_CHANNEL_ID,
                "Call start",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Outgoing call starting notifications"
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun ensureOngoingChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                notificationManager.deleteNotificationChannel(OLD_ONGOING_CHANNEL_ID)
            } catch (_: Throwable) {
            }

            val channel = NotificationChannel(
                ONGOING_CHANNEL_ID,
                "Ongoing calls",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active and ongoing call notifications"
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun postIncomingCallNotification(call: Call, cachedDisplayName: String? = null) {
        if (call.state != Call.STATE_RINGING) {
            dismissNotification()
            return
        }

        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            ensureIncomingChannel(notificationManager)
            notificationManager.cancel(NOTIFICATION_ONGOING_ID)
            notificationManager.cancel(NOTIFICATION_CALL_START_ID)

            val handle = call.details?.handle
            val rawNumber = handle?.schemeSpecificPart ?: ""
            val currentDisplayName = cachedDisplayName
                ?: CallRepository.ongoingCallModel.value?.displayName
                ?: call.details?.contactDisplayName?.takeIf { it.isNotBlank() }
                ?: call.details?.callerDisplayName?.takeIf { it.isNotBlank() }

            val title = currentDisplayName?.takeIf { it.isNotBlank() }
                ?: rawNumber.takeIf { it.isNotBlank() }
                ?: "Incoming call"
            val contentText = if (currentDisplayName != null && rawNumber.isNotBlank() && currentDisplayName != rawNumber) {
                rawNumber
            } else {
                "Incoming call"
            }

            // Tapping the banner opens the full call screen
            val contentIntent = Intent(this, CallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            }
            val contentPendingIntent = PendingIntent.getActivity(
                this,
                REQUEST_CODE_CONTENT,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Full screen intent for lock screen
            val fullScreenIntent = Intent(this, CallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            }
            val fullScreenPendingIntent = PendingIntent.getActivity(
                this,
                REQUEST_CODE_FULL_SCREEN,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Answer action connects call and opens ongoing call screen
            val answerIntent = Intent(this, CallActivity::class.java).apply {
                action = ACTION_ANSWER_CALL
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            }
            val answerPendingIntent = PendingIntent.getActivity(
                this,
                REQUEST_CODE_ANSWER,
                answerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Decline action rejects call
            val declineIntent = Intent(this, CallActionReceiver::class.java).apply {
                action = ACTION_DECLINE_CALL
            }
            val declinePendingIntent = PendingIntent.getBroadcast(
                this,
                REQUEST_CODE_DECLINE,
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

            val builder = NotificationCompat.Builder(this, INCOMING_CHANNEL_ID)
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

            val canUseFullScreen = if (Build.VERSION.SDK_INT >= 34) {
                try {
                    notificationManager.canUseFullScreenIntent()
                } catch (_: Throwable) {
                    true
                }
            } else {
                true
            }

            if (canUseFullScreen) {
                try {
                    builder.setFullScreenIntent(fullScreenPendingIntent, true)
                } catch (_: SecurityException) {
                } catch (_: Throwable) {
                }
            }

            notificationManager.notify(NOTIFICATION_INCOMING_ID, builder.build())
        } catch (_: Exception) {
        }
    }

    private fun postCallStartNotification(call: Call, cachedDisplayName: String? = null) {
        if (call.state == Call.STATE_DISCONNECTED || call.state == Call.STATE_DISCONNECTING) {
            dismissNotification()
            return
        }

        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            ensureCallStartChannel(notificationManager)

            val handle = call.details?.handle
            val rawNumber = handle?.schemeSpecificPart ?: ""
            val currentDisplayName = cachedDisplayName
                ?: CallRepository.ongoingCallModel.value?.displayName
                ?: call.details?.contactDisplayName?.takeIf { it.isNotBlank() }
                ?: call.details?.callerDisplayName?.takeIf { it.isNotBlank() }

            val title = currentDisplayName?.takeIf { it.isNotBlank() }
                ?: rawNumber.takeIf { it.isNotBlank() }
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
                "$rawNumber • $statusText"
            } else {
                statusText
            }

            // Tapping notification opens the call screen
            val contentIntent = Intent(this, CallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            }
            val contentPendingIntent = PendingIntent.getActivity(
                this,
                REQUEST_CODE_CONTENT,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Full screen intent to the in-call activity so system opens screen when activity start is blocked
            val fullScreenIntent = Intent(this, CallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            }
            val fullScreenPendingIntent = PendingIntent.getActivity(
                this,
                REQUEST_CODE_FULL_SCREEN,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Hang up action
            val hangUpIntent = Intent(this, CallActionReceiver::class.java).apply {
                action = ACTION_HANG_UP_CALL
            }
            val hangUpPendingIntent = PendingIntent.getBroadcast(
                this,
                REQUEST_CODE_HANG_UP,
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

            val builder = NotificationCompat.Builder(this, CALL_START_CHANNEL_ID)
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
                } catch (_: Throwable) {
                    true
                }
            } else {
                true
            }

            if (canUseFullScreen) {
                try {
                    builder.setFullScreenIntent(fullScreenPendingIntent, true)
                } catch (_: SecurityException) {
                } catch (_: Throwable) {
                }
            }

            notificationManager.notify(NOTIFICATION_CALL_START_ID, builder.build())
        } catch (_: Exception) {
        }
    }

    private fun postOngoingCallNotification(call: Call, cachedDisplayName: String? = null) {
        if (call.state == Call.STATE_DISCONNECTED || call.state == Call.STATE_DISCONNECTING) {
            dismissNotification()
            return
        }

        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            ensureOngoingChannel(notificationManager)
            notificationManager.cancel(NOTIFICATION_INCOMING_ID)
            notificationManager.cancel(NOTIFICATION_CALL_START_ID)

            val handle = call.details?.handle
            val rawNumber = handle?.schemeSpecificPart ?: ""
            val currentDisplayName = cachedDisplayName
                ?: CallRepository.ongoingCallModel.value?.displayName
                ?: call.details?.contactDisplayName?.takeIf { it.isNotBlank() }
                ?: call.details?.callerDisplayName?.takeIf { it.isNotBlank() }

            val title = currentDisplayName?.takeIf { it.isNotBlank() }
                ?: rawNumber.takeIf { it.isNotBlank() }
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
                "$rawNumber • $statusText"
            } else {
                statusText
            }

            // Tapping notification opens the call screen
            val contentIntent = Intent(this, CallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            }
            val contentPendingIntent = PendingIntent.getActivity(
                this,
                REQUEST_CODE_CONTENT,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Hang up action
            val hangUpIntent = Intent(this, CallActionReceiver::class.java).apply {
                action = ACTION_HANG_UP_CALL
            }
            val hangUpPendingIntent = PendingIntent.getBroadcast(
                this,
                REQUEST_CODE_HANG_UP,
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

            val builder = NotificationCompat.Builder(this, ONGOING_CHANNEL_ID)
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

            notificationManager.notify(NOTIFICATION_ONGOING_ID, builder.build())
        } catch (_: Exception) {
        }
    }

    private fun handleCallActivityResumed() {
        isActivityVisible = true
        callStartTimeoutJob?.cancel()
        callStartTimeoutJob = null
        cancelCallStartNotification()
        val call = CallRepository.currentCall.value
        if (call != null && call.state != Call.STATE_RINGING && call.state != Call.STATE_DISCONNECTED && call.state != Call.STATE_DISCONNECTING) {
            postOngoingCallNotification(call)
        }
    }

    private fun handleCallActivityPaused() {
        isActivityVisible = false
    }

    private fun cancelCallStartNotification() {
        cancelCallStartNotification(this)
    }

    private fun cancelIncomingNotification() {
        cancelIncomingNotification(this)
    }

    private fun dismissNotification() {
        dismissNotification(this)
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val NOTIFICATION_INCOMING_ID = 1001
        const val NOTIFICATION_ONGOING_ID = 1002
        const val NOTIFICATION_CALL_START_ID = 1003

        const val INCOMING_CHANNEL_ID = "phone_incoming_call_channel"
        const val CALL_START_CHANNEL_ID = "phone_call_start_channel"
        const val ONGOING_CHANNEL_ID = "phone_ongoing_call_channel"
        const val OLD_ONGOING_CHANNEL_ID = "phone_in_call_channel"

        const val ACTION_ANSWER_CALL = "io.github.elli0t0.phone.action.ANSWER_CALL"
        const val ACTION_DECLINE_CALL = "io.github.elli0t0.phone.action.DECLINE_CALL"
        const val ACTION_HANG_UP_CALL = "io.github.elli0t0.phone.action.HANG_UP_CALL"

        private const val REQUEST_CODE_CONTENT = 100
        private const val REQUEST_CODE_FULL_SCREEN = 101
        private const val REQUEST_CODE_ANSWER = 102
        private const val REQUEST_CODE_DECLINE = 103
        private const val REQUEST_CODE_HANG_UP = 104

        private var instance: PhoneInCallService? = null

        fun setAudioRoute(route: Int) {
            instance?.setAudioRoute(route)
        }

        fun setMuted(muted: Boolean) {
            instance?.setMuted(muted)
        }

        fun startCallActivity(context: Context) {
            try {
                val intent = Intent(context, CallActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                }
                context.startActivity(intent)
            } catch (_: Exception) {
            }
        }

        fun onCallActivityResumed(context: Context) {
            instance?.handleCallActivityResumed() ?: run {
                cancelCallStartNotification(context)
            }
        }

        fun onCallActivityPaused(context: Context) {
            instance?.handleCallActivityPaused()
        }

        fun cancelCallStartNotification(context: Context) {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.cancel(NOTIFICATION_CALL_START_ID)
            } catch (_: Exception) {
            }
        }

        fun cancelIncomingNotification(context: Context) {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.cancel(NOTIFICATION_INCOMING_ID)
            } catch (_: Exception) {
            }
        }

        fun dismissNotification(context: Context) {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.cancel(NOTIFICATION_INCOMING_ID)
                notificationManager?.cancel(NOTIFICATION_ONGOING_ID)
                notificationManager?.cancel(NOTIFICATION_CALL_START_ID)
                notificationManager?.cancel(NOTIFICATION_ID)
            } catch (_: Exception) {
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        lookupJob?.cancel()
        callStartTimeoutJob?.cancel()
        callStartTimeoutJob = null
        dismissNotification()
    }
}
