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
            call?.let { updateCallNotification(it) }
        }

        override fun onDetailsChanged(call: Call?, details: Call.Details?) {
            call?.let { updateCallNotification(it) }
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        call.registerCallback(callCallback)
        CallRepository.onCallAdded(call)

        // Launch CallActivity
        val intent = Intent(this, CallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        }
        startActivity(intent)

        // Show initial notification
        updateCallNotification(call)

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

    private fun updateCallNotification(call: Call, cachedDisplayName: String? = null) {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channelId = "phone_in_call_channel"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Calls",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Incoming and active calls"
                    setSound(null, null)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val handle = call.details?.handle
            val rawNumber = handle?.schemeSpecificPart ?: ""
            val currentDisplayName = cachedDisplayName
                ?: CallRepository.ongoingCallModel.value?.displayName
                ?: call.details?.contactDisplayName?.takeIf { it.isNotBlank() }
                ?: call.details?.callerDisplayName?.takeIf { it.isNotBlank() }

            val title = currentDisplayName?.takeIf { it.isNotBlank() } ?: rawNumber.takeIf { it.isNotBlank() } ?: "Call"
            val contentText = if (currentDisplayName != null && rawNumber.isNotBlank() && currentDisplayName != rawNumber) {
                rawNumber
            } else {
                if (call.state == Call.STATE_RINGING) "Incoming call" else "Active call"
            }

            val intent = Intent(this, CallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_phone)
                .setContentTitle(title)
                .setContentText(contentText)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)

            if (call.state == Call.STATE_RINGING) {
                builder.setFullScreenIntent(pendingIntent, true)
            }

            notificationManager.notify(NOTIFICATION_ID, builder.build())
        } catch (_: Exception) {
        }
    }

    private fun dismissNotification() {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.cancel(NOTIFICATION_ID)
        } catch (_: Exception) {
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private var instance: PhoneInCallService? = null

        fun setAudioRoute(route: Int) {
            instance?.setAudioRoute(route)
        }

        fun setMuted(muted: Boolean) {
            instance?.setMuted(muted)
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
