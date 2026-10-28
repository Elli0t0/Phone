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
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PhoneInCallService : InCallService() {

    internal val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    internal var lookupJob: Job? = null
    internal var callStartTimeoutJob: Job? = null

    @Volatile
    internal var isActivityVisible = false

    // Cached call-display setting (default = current behavior). Collected
    // in onCreate; until the first emission arrives the default applies.
    @Volatile
    internal var cachedCallDisplay: CallDisplay = CallDisplay.FULL_SCREEN
    internal var settingsJob: Job? = null

    @Volatile
    internal var currentForegroundId: Int = -1

    internal var proximityWakeLock: PowerManager.WakeLock? = null

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
        // App-side prefix blocking: the platform provider only matches
        // exact numbers, so a ringing call whose digits start with a
        // stored prefix is disconnected here. Only incoming calls, and
        // only while still ringing (the check itself is async).
        if (call.state == Call.STATE_RINGING) {
            serviceScope.launch {
                runCatching {
                    val prefixes = (application as? PhoneApplication)
                        ?.container?.settingsRepository?.settingsFlow?.first()
                        ?.blockedPrefixes ?: emptySet()
                    if (prefixes.isEmpty()) return@runCatching
                    val digits = PhoneNumberFormatter.normalizeDigits(
                        call.details?.handle?.schemeSpecificPart.orEmpty()
                    ).filter { it.isDigit() }
                    if (digits.isEmpty()) return@runCatching
                    val blocked = prefixes.any { prefix ->
                        val prefixDigits = PhoneNumberFormatter.normalizeDigits(prefix)
                            .filter { it.isDigit() }
                        prefixDigits.isNotEmpty() && digits.startsWith(prefixDigits)
                    }
                    if (blocked && call.state == Call.STATE_RINGING) {
                        Log.d(TAG, "Rejecting call matching blocked prefix")
                        call.disconnect()
                    }
                }
            }
        }
        try {
            // Channels first, synchronously, before any notification or lookup.
            CallNotificationChannels.createAll(this)
            call.registerCallback(callCallback)
            CallRepository.onCallAdded(call)
            // Foreground notification synchronously in onCallAdded, before lookups.
            // This single post is the heads-up / full-screen trigger. Do NOT post
            // the same notification again via notify: an update suppresses heads-up.
            startForegroundForCall(call)
            // Outgoing: both activity start AND call notification must run.
            if (isOutgoingState(call.state)) {
                startCallActivity()
                if (isActivityVisible || CallRepository.isCallActivityVisible.value) {
                    transitionToOngoing(call)
                } else {
                    // Call-start HIGH is already foreground from startForegroundForCall.
                    callStartTimeoutJob?.cancel()
                    callStartTimeoutJob = serviceScope.launch {
                        delay(1500L)
                    }
                }
            } else if (call.state == Call.STATE_RINGING) {
                // Incoming: foreground HIGH is already posted, no direct activity start.
                // Heads-up / full-screen is driven by that single foreground post.
            } else {
                handleCallState(call)
            }
            resolveContactNameInBackground(call)
            updateProximityWakeLock()
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
        currentForegroundId = -1
        dismissNotification()
        CallRepository.onCallRemoved(call)
        updateProximityWakeLock()
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        CallRepository.updateAudioState(audioState)
        refreshBluetoothDevices()
        updateProximityWakeLock()
    }

    private fun handleCallActivityResumed() {
        isActivityVisible = true
        callStartTimeoutJob?.cancel()
        callStartTimeoutJob = null
        val call = CallRepository.currentCall.value
        if (call != null && call.state != Call.STATE_RINGING && call.state != Call.STATE_DISCONNECTED && call.state != Call.STATE_DISCONNECTING) {
            transitionToOngoing(call)
        }
    }

    private fun handleCallActivityPaused() {
        isActivityVisible = false
    }

    internal fun dismissNotification() {
        dismissNotification(this)
    }

    companion object {
        internal const val TAG = "PhoneCall"
        internal const val PROXIMITY_WAKE_LOCK_TAG = "Phone:Proximity"
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

        internal const val REQUEST_CODE_CONTENT = 100
        internal const val REQUEST_CODE_FULL_SCREEN = 101
        internal const val REQUEST_CODE_ANSWER = 102
        internal const val REQUEST_CODE_DECLINE = 103
        internal const val REQUEST_CODE_HANG_UP = 104

        internal var instance: PhoneInCallService? = null

        fun setAudioRoute(route: Int) {
            instance?.setAudioRoute(route)
        }

        fun setMuted(muted: Boolean) {
            instance?.setMuted(muted)
        }

        fun refreshBluetoothDevices() {
            instance?.refreshBluetoothDevices()
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
        settingsJob = serviceScope.launch {
            runCatching {
                (application as? PhoneApplication)?.container?.settingsRepository?.settingsFlow
                    ?.collect { cachedCallDisplay = it.callDisplay }
            }
        }
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
        settingsJob?.cancel()
        settingsJob = null
        lookupJob?.cancel()
        callStartTimeoutJob?.cancel()
        callStartTimeoutJob = null
        // Cancel only the children: cancelling the scope itself would
        // poison its root Job forever, so coroutines launched after a
        // rebind would fail silently. The children are all cancelled
        // here, so nothing keeps running past onDestroy.
        serviceScope.coroutineContext[Job]?.cancelChildren()
        releaseProximityWakeLock()
        dismissNotification()
    }
}
