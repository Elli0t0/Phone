package io.github.elli0t0.phone.core.telecom

import android.Manifest
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telephony.PhoneNumberUtils
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.notifications.CallNotificationChannels
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
        try {
            super.onCallAdded(call)
        } catch (e: Exception) {
            Log.e(TAG, "onCallAdded super failed", e)
        }
        val isDefault = DefaultDialerHelper.isDefaultDialer(this)
        Log.d(TAG, "onCallAdded state=${call.state} number=${call.details?.handle?.schemeSpecificPart} isDefaultDialer=$isDefault")
        if (!isDefault) {
            Log.e(TAG, "onCallAdded but app is NOT default dialer, Telecom may not bind service reliably")
        }
        try {
            // Channels first, synchronously, before any notification or lookup.
            CallNotificationChannels.createAll(this)
            call.registerCallback(callCallback)
            CallRepository.onCallAdded(call)
            // Foreground notification synchronously in onCallAdded, before lookups.
            startForegroundForCall(call)
            // Outgoing: both activity start AND call notification must run.
            if (isOutgoingState(call.state)) {
                startCallActivity()
                if (isActivityVisible || CallRepository.isCallActivityVisible.value) {
                    cancelCallStartNotification()
                    postOngoingCallNotification(call)
                } else {
                    postCallStartNotification(call)
                    callStartTimeoutJob?.cancel()
                    callStartTimeoutJob = serviceScope.launch {
                        delay(1500L)
                    }
                }
            } else if (call.state == Call.STATE_RINGING) {
                // Incoming: heads-up notification only, no direct activity start.
                postIncomingCallNotification(call)
            } else {
                handleCallState(call)
            }
            resolveContactNameInBackground(call)
        } catch (e: Exception) {
            Log.e(TAG, "onCallAdded failed", e)
        }
    }

    override fun onCallRemoved(call: Call) {
        Log.d(TAG, "onCallRemoved state=${call.state}")
        try {
            super.onCallRemoved(call)
        } catch (e: Exception) {
            Log.e(TAG, "onCallRemoved super failed", e)
        }
        call.unregisterCallback(callCallback)
        lookupJob?.cancel()
        callStartTimeoutJob?.cancel()
        callStartTimeoutJob = null
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            Log.e(TAG, "stopForeground failed", e)
        }
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
        try {
            when {
                call.state == Call.STATE_RINGING -> {
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
        } catch (e: Exception) {
            Log.e(TAG, "handleCallState failed state=${call.state}", e)
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
        } catch (e: SecurityException) {
            Log.e(TAG, "queryPhoneLookup SecurityException", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "queryPhoneLookup failed", e)
            null
        }
    }

    private fun updateCallNotification(call: Call, cachedDisplayName: String? = null) {
        try {
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
        } catch (e: Exception) {
            Log.e(TAG, "updateCallNotification failed", e)
        }
    }

    private fun isDeviceLocked(): Boolean {
        return try {
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            val keyguardLocked = keyguardManager?.isKeyguardLocked == true
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            val interactive = powerManager?.isInteractive ?: true
            keyguardLocked || !interactive
        } catch (e: Exception) {
            Log.e(TAG, "isDeviceLocked check failed", e)
            false
        }
    }

    private fun startForegroundForCall(call: Call) {
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
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(foregroundId, notification)
                } else {
                    @Suppress("DEPRECATION")
                    startForeground(foregroundId, notification)
                }
                Log.d(TAG, "notification posted foreground id=$foregroundId state=${call.state}")
            } catch (e: Exception) {
                Log.e(TAG, "startForeground failed, falling back to notify", e)
                val notificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.notify(foregroundId, notification)
                Log.d(TAG, "notification posted notify fallback id=$foregroundId state=${call.state}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "startForegroundForCall failed", e)
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
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
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
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun ensureOngoingChannel(notificationManager: NotificationManager) {
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

    private fun buildIncomingNotification(call: Call, cachedDisplayName: String? = null): Notification? {
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
                ?: rawNumber.takeIf { it.isNotBlank() }
                ?: "Incoming call"
            val contentText = if (currentDisplayName != null && rawNumber.isNotBlank() && currentDisplayName != rawNumber) {
                rawNumber
            } else {
                "Incoming call"
            }

            val contentIntent = Intent(this, CallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val contentPendingIntent = PendingIntent.getActivity(
                this,
                REQUEST_CODE_CONTENT,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val fullScreenIntent = Intent(this, CallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val fullScreenPendingIntent = PendingIntent.getActivity(
                this,
                REQUEST_CODE_FULL_SCREEN,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val answerIntent = Intent(this, CallActivity::class.java).apply {
                action = ACTION_ANSWER_CALL
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val answerPendingIntent = PendingIntent.getActivity(
                this,
                REQUEST_CODE_ANSWER,
                answerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

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

            // Full-screen intent only for lock screen. When unlocked, heads-up banner only.
            if (isDeviceLocked()) {
                val canUseFullScreen = if (Build.VERSION.SDK_INT >= 34) {
                    try {
                        notificationManager.canUseFullScreenIntent()
                    } catch (e: Exception) {
                        Log.e(TAG, "canUseFullScreenIntent check failed", e)
                        true
                    }
                } else {
                    true
                }
                if (canUseFullScreen) {
                    try {
                        builder.setFullScreenIntent(fullScreenPendingIntent, true)
                    } catch (e: SecurityException) {
                        Log.e(TAG, "setFullScreenIntent SecurityException", e)
                    } catch (e: Exception) {
                        Log.e(TAG, "setFullScreenIntent failed", e)
                    }
                }
            }
            return builder.build()
        } catch (e: Exception) {
            Log.e(TAG, "buildIncomingNotification failed", e)
            return null
        }
    }

    private fun buildCallStartNotification(call: Call, cachedDisplayName: String? = null): Notification? {
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

            val contentIntent = Intent(this, CallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val contentPendingIntent = PendingIntent.getActivity(
                this,
                REQUEST_CODE_CONTENT,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val fullScreenIntent = Intent(this, CallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val fullScreenPendingIntent = PendingIntent.getActivity(
                this,
                REQUEST_CODE_FULL_SCREEN,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

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
                } catch (e: Exception) {
                    Log.e(TAG, "canUseFullScreenIntent check failed", e)
                    true
                }
            } else {
                true
            }

            if (canUseFullScreen) {
                try {
                    builder.setFullScreenIntent(fullScreenPendingIntent, true)
                } catch (e: SecurityException) {
                    Log.e(TAG, "setFullScreenIntent SecurityException", e)
                } catch (e: Exception) {
                    Log.e(TAG, "setFullScreenIntent failed", e)
                }
            }
            return builder.build()
        } catch (e: Exception) {
            Log.e(TAG, "buildCallStartNotification failed", e)
            return null
        }
    }

    private fun buildOngoingNotification(call: Call, cachedDisplayName: String? = null): Notification? {
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

            val contentIntent = Intent(this, CallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val contentPendingIntent = PendingIntent.getActivity(
                this,
                REQUEST_CODE_CONTENT,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

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

            // Quiet ongoing notification: LOW importance, no heads-up, no full-screen intent.
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
            return builder.build()
        } catch (e: Exception) {
            Log.e(TAG, "buildOngoingNotification failed", e)
            return null
        }
    }

    private fun postIncomingCallNotification(call: Call, cachedDisplayName: String? = null) {
        if (call.state != Call.STATE_RINGING) {
            dismissNotification()
            return
        }
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            notificationManager.cancel(NOTIFICATION_ONGOING_ID)
            notificationManager.cancel(NOTIFICATION_CALL_START_ID)
            val notification = buildIncomingNotification(call, cachedDisplayName) ?: return
            notificationManager.notify(NOTIFICATION_INCOMING_ID, notification)
            Log.d(TAG, "notification posted incoming id=$NOTIFICATION_INCOMING_ID")
        } catch (e: Exception) {
            Log.e(TAG, "postIncomingCallNotification failed", e)
        }
    }

    private fun postCallStartNotification(call: Call, cachedDisplayName: String? = null) {
        if (call.state == Call.STATE_DISCONNECTED || call.state == Call.STATE_DISCONNECTING) {
            dismissNotification()
            return
        }
        try {
            val notification = buildCallStartNotification(call, cachedDisplayName) ?: return
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            notificationManager.notify(NOTIFICATION_CALL_START_ID, notification)
            Log.d(TAG, "notification posted callStart id=$NOTIFICATION_CALL_START_ID")
        } catch (e: Exception) {
            Log.e(TAG, "postCallStartNotification failed", e)
        }
    }

    private fun postOngoingCallNotification(call: Call, cachedDisplayName: String? = null) {
        if (call.state == Call.STATE_DISCONNECTED || call.state == Call.STATE_DISCONNECTING) {
            dismissNotification()
            return
        }
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            notificationManager.cancel(NOTIFICATION_INCOMING_ID)
            notificationManager.cancel(NOTIFICATION_CALL_START_ID)
            val notification = buildOngoingNotification(call, cachedDisplayName) ?: return
            notificationManager.notify(NOTIFICATION_ONGOING_ID, notification)
            Log.d(TAG, "notification posted ongoing id=$NOTIFICATION_ONGOING_ID")
        } catch (e: Exception) {
            Log.e(TAG, "postOngoingCallNotification failed", e)
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
        private const val TAG = "PhoneCall"
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
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                context.startActivity(intent)
                Log.d(TAG, "activity started CallActivity")
            } catch (e: Exception) {
                Log.e(TAG, "startCallActivity failed", e)
            }
        }

        fun onCallActivityResumed(context: Context) {
            Log.d(TAG, "activity resumed CallActivity")
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
            } catch (e: Exception) {
                Log.e(TAG, "cancelCallStartNotification failed", e)
            }
        }

        fun cancelIncomingNotification(context: Context) {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.cancel(NOTIFICATION_INCOMING_ID)
            } catch (e: Exception) {
                Log.e(TAG, "cancelIncomingNotification failed", e)
            }
        }

        fun dismissNotification(context: Context) {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.cancel(NOTIFICATION_INCOMING_ID)
                notificationManager?.cancel(NOTIFICATION_ONGOING_ID)
                notificationManager?.cancel(NOTIFICATION_CALL_START_ID)
                notificationManager?.cancel(NOTIFICATION_ID)
            } catch (e: Exception) {
                Log.e(TAG, "dismissNotification failed", e)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        try {
            CallNotificationChannels.createAll(this)
        } catch (e: Exception) {
            Log.e(TAG, "create channels in onCreate failed", e)
        }
        val isDefault = DefaultDialerHelper.isDefaultDialer(this)
        Log.d(TAG, "service onCreate isDefaultDialer=$isDefault")
        if (!isDefault) {
            Log.e(TAG, "service onCreate but app is NOT default dialer")
        }
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
