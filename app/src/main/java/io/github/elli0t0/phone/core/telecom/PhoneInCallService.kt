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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PhoneInCallService : InCallService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var lookupJob: Job? = null

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call?, state: Int) {
            call?.let {
                if (it.state == Call.STATE_RINGING) {
                    postIncomingCallNotification(it)
                } else {
                    dismissNotification()
                }
            }
        }

        override fun onDetailsChanged(call: Call?, details: Call.Details?) {
            call?.let {
                if (it.state == Call.STATE_RINGING) {
                    postIncomingCallNotification(it)
                }
            }
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        call.registerCallback(callCallback)
        CallRepository.onCallAdded(call)

        if (call.state == Call.STATE_RINGING) {
            postIncomingCallNotification(call)
        } else {
            // Outgoing calls still open the call screen directly
            val intent = Intent(this, CallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            }
            startActivity(intent)
        }

        // Resolve contact name on background thread
        resolveContactNameInBackground(call)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callCallback)
        lookupJob?.cancel()
        dismissNotification()
        CallRepository.onCallRemoved(call)
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        CallRepository.updateAudioState(audioState)
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
                    if (call.state == Call.STATE_RINGING) {
                        postIncomingCallNotification(call, resolvedName)
                    }
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
            // 1. Try number as-is
            val asIs = queryPhoneLookup(rawNumber)
            if (asIs != null) return asIs

            // 2. Try normalized number
            val normalized = PhoneNumberUtils.normalizeNumber(rawNumber)
            if (!normalized.isNullOrBlank() && normalized != rawNumber) {
                val fromNormalized = queryPhoneLookup(normalized)
                if (fromNormalized != null) return fromNormalized
            }
        }

        // 3. Fallback to Call.Details contactDisplayName or callerDisplayName
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

    private fun postIncomingCallNotification(call: Call, cachedDisplayName: String? = null) {
        if (call.state != Call.STATE_RINGING) {
            dismissNotification()
            return
        }

        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channelId = "phone_incoming_call_channel"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Incoming calls",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Incoming call notifications"
                    setSound(null, null)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

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

            val builder = NotificationCompat.Builder(this, channelId)
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

            // On Android 14+, check if USE_FULL_SCREEN_INTENT is granted before setting full-screen intent
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
                }
            }

            notificationManager.notify(NOTIFICATION_ID, builder.build())
        } catch (_: Exception) {
        }
    }

    private fun dismissNotification() {
        dismissNotification(this)
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_ANSWER_CALL = "io.github.elli0t0.phone.action.ANSWER_CALL"
        const val ACTION_DECLINE_CALL = "io.github.elli0t0.phone.action.DECLINE_CALL"

        private const val REQUEST_CODE_CONTENT = 100
        private const val REQUEST_CODE_FULL_SCREEN = 101
        private const val REQUEST_CODE_ANSWER = 102
        private const val REQUEST_CODE_DECLINE = 103

        private var instance: PhoneInCallService? = null

        fun setAudioRoute(route: Int) {
            instance?.setAudioRoute(route)
        }

        fun setMuted(muted: Boolean) {
            instance?.setMuted(muted)
        }

        fun dismissNotification(context: Context) {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
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
        dismissNotification()
    }
}
