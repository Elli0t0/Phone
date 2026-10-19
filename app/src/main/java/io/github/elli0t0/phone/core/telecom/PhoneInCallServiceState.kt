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

internal fun PhoneInCallService.refreshBluetoothDevices() {
    try {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val bluetoothManager =
            getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
        val adapter = bluetoothManager?.adapter ?: return
        val names = try {
            adapter.bondedDevices
                ?.mapNotNull { it.name?.takeIf { name -> name.isNotBlank() } }
                ?.sorted()
                ?: emptyList()
        } catch (e: SecurityException) {
            Log.e(TAG, "bondedDevices SecurityException", e)
            emptyList()
        }
        CallRepository.updateBluetoothDevices(names)
    } catch (e: Exception) {
        Log.e(TAG, "refreshBluetoothDevices failed", e)
    }
}

/**
 * Holds the proximity wake lock only while a call is in progress
 * (dialing, connecting, or active) and audio goes to the earpiece.
 * Any other route (speaker, Bluetooth, wired headset) releases it,
 * and so do disconnect, call removal, and service destruction.
 */
internal fun PhoneInCallService.updateProximityWakeLock() {
    try {
        val call = CallRepository.currentCall.value
        // Call carries no audio state; the route lives in the call model,
        // updated from onCallAudioStateChanged.
        val route = CallRepository.ongoingCallModel.value?.audioRoute
            ?: CallAudioState.ROUTE_EARPIECE
        val shouldHold = call != null &&
            (call.state == Call.STATE_DIALING ||
                call.state == Call.STATE_CONNECTING ||
                call.state == Call.STATE_ACTIVE) &&
            route == CallAudioState.ROUTE_EARPIECE
        if (shouldHold) {
            acquireProximityWakeLock()
        } else {
            releaseProximityWakeLock()
        }
    } catch (e: Exception) {
        Log.e(TAG, "updateProximityWakeLock failed", e)
    }
}

internal fun PhoneInCallService.acquireProximityWakeLock() {
    try {
        var lock = proximityWakeLock
        if (lock == null) {
            val powerManager =
                getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
            if (!powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
                Log.d(TAG, "proximity wake lock not supported on this device")
                return
            }
            lock = powerManager.newWakeLock(
                PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                PROXIMITY_WAKE_LOCK_TAG
            )
            lock.setReferenceCounted(false)
            proximityWakeLock = lock
        }
        if (!lock.isHeld) {
            lock.acquire()
            Log.d(TAG, "proximity wake lock acquired")
        }
    } catch (e: SecurityException) {
        Log.e(TAG, "acquireProximityWakeLock SecurityException", e)
    } catch (e: Exception) {
        Log.e(TAG, "acquireProximityWakeLock failed", e)
    }
}

internal fun PhoneInCallService.releaseProximityWakeLock() {
    try {
        val lock = proximityWakeLock ?: return
        if (lock.isHeld) {
            lock.release()
            Log.d(TAG, "proximity wake lock released")
        }
    } catch (e: RuntimeException) {
        Log.e(TAG, "releaseProximityWakeLock RuntimeException", e)
    } catch (e: Exception) {
        Log.e(TAG, "releaseProximityWakeLock failed", e)
    }
}

internal fun PhoneInCallService.isOutgoingState(state: Int): Boolean {
    return state == Call.STATE_NEW ||
        state == Call.STATE_SELECT_PHONE_ACCOUNT ||
        state == Call.STATE_CONNECTING ||
        state == Call.STATE_DIALING
}

internal fun PhoneInCallService.isActiveState(state: Int): Boolean {
    return state == Call.STATE_ACTIVE ||
        state == Call.STATE_HOLDING
}

internal fun PhoneInCallService.handleCallState(call: Call) {
    try {
        when {
            call.state == Call.STATE_RINGING -> {
                // Switch foreground to incoming if not already there.
                if (currentForegroundId != NOTIFICATION_INCOMING_ID) {
                    startForegroundForCall(call)
                } else {
                    postIncomingCallNotification(call)
                }
            }
            isOutgoingState(call.state) -> {
                startCallActivity()
                if (isActivityVisible || CallRepository.isCallActivityVisible.value) {
                    transitionToOngoing(call)
                } else if (currentForegroundId != NOTIFICATION_CALL_START_ID) {
                    startForegroundForCall(call)
                }
            }
            isActiveState(call.state) -> {
                startCallActivity()
                if (isActivityVisible || CallRepository.isCallActivityVisible.value) {
                    transitionToOngoing(call)
                } else if (currentForegroundId != NOTIFICATION_CALL_START_ID) {
                    startForegroundForCall(call)
                }
            }
            call.state == Call.STATE_DISCONNECTED || call.state == Call.STATE_DISCONNECTING -> {
                callStartTimeoutJob?.cancel()
                callStartTimeoutJob = null
                dismissNotification()
            }
            else -> {
                if (isActivityVisible || CallRepository.isCallActivityVisible.value) {
                    transitionToOngoing(call)
                } else if (currentForegroundId != NOTIFICATION_CALL_START_ID) {
                    startForegroundForCall(call)
                }
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "handleCallState failed state=${call.state}", e)
    }
    updateProximityWakeLock()
}

internal fun PhoneInCallService.startCallActivity() {
    startCallActivity(this)
}

internal fun PhoneInCallService.resolveContactNameInBackground(call: Call) {
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

internal fun PhoneInCallService.resolveName(rawNumber: String, call: Call): String? {
    if (rawNumber.isNotBlank() && ContextCompat.checkSelfPermission(
            this@PhoneInCallService,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        // Resolve from the single in-memory contact index: one map lookup,
        // no per-call PhoneLookup query.
        ContactNameIndex.ensureStarted(contentResolver)
        val indexed = ContactNameIndex.resolve(rawNumber)
        if (!indexed?.displayName.isNullOrBlank()) {
            return indexed.displayName
        }
    }

    return call.details?.contactDisplayName?.takeIf { it.isNotBlank() }
        ?: call.details?.callerDisplayName?.takeIf { it.isNotBlank() }
}

