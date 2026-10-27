package io.github.elli0t0.phone.core.telecom

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telecom.TelecomManager
import android.telephony.PhoneNumberUtils
import io.github.elli0t0.phone.core.common.PhoneNumberFormatter
import androidx.core.content.ContextCompat

/**
 * Handles placing calls or handing off to the system dialer.
 *
 * TelecomManager vs ACTION_CALL:
 * We use TelecomManager.placeCall when CALL_PHONE permission is granted and telecom service
 * is available. On Android 6.0+ (API 23+), TelecomManager.placeCall is the official system
 * telecommunications API designed to initiate outgoing calls directly through the active PhoneAccount.
 * If TelecomManager throws a SecurityException or is unavailable, or on older SDKs, ACTION_CALL is used.
 *
 * If CALL_PHONE permission is NOT granted, we fall back to ACTION_DIAL, which opens the system dialer
 * pre-populated with the number and requires no runtime permission.
 *
 * Handling USSD and Special Characters ('*' and '#'):
 * Telephone URIs use the "tel:" scheme. In RFC 3966 and Android Uri parsing, '#' is the URI fragment
 * delimiter. If '#' is passed unencoded (e.g. "tel:*140#"), the Uri parser treats "#" as a fragment,
 * stripping it and causing USSD codes to fail. Therefore, we use:
 *   Uri.parse("tel:" + Uri.encode(number))
 * which converts '#' to '%23' and preserves '*' and digits. Android telephony decodes '%23' back to '#'
 * before sending to the cellular baseband/RIL.
 */
object CallManager {

    fun placeCall(
        context: Context,
        rawNumber: String,
        phoneAccountHandle: android.telecom.PhoneAccountHandle? = null
    ) {
        val trimmed = PhoneNumberFormatter.normalizeDigits(rawNumber.trim())
        if (trimmed.isEmpty()) return

        val encodedUri = Uri.parse("tel:" + Uri.encode(trimmed))

        // Emergency numbers never go through this app's InCall routing,
        // a SIM picker, or a saved-SIM lookup: hand them straight to
        // the system dialer.
        if (PhoneNumberUtils.isEmergencyNumber(trimmed)) {
            try {
                val dialIntent = Intent(Intent.ACTION_DIAL, encodedUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(dialIntent)
            } catch (_: Exception) {
                // ActivityNotFound or restricted device
            }
            return
        }
        val hasCallPhonePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        if (hasCallPhonePermission) {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            if (telecomManager != null) {
                try {
                    val extras = if (phoneAccountHandle != null) {
                        android.os.Bundle().apply {
                            putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle)
                        }
                    } else null
                    telecomManager.placeCall(encodedUri, extras)
                    return
                } catch (_: SecurityException) {
                    // Fallback to ACTION_CALL intent
                } catch (_: Exception) {
                    // Fallback to ACTION_CALL intent
                }
            }

            // Fallback: ACTION_CALL
            try {
                val callIntent = Intent(Intent.ACTION_CALL, encodedUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(callIntent)
                return
            } catch (_: Exception) {
                // If ACTION_CALL fails, fallback to ACTION_DIAL
            }
        }

        // Fallback: ACTION_DIAL (always safe, no permission needed)
        try {
            val dialIntent = Intent(Intent.ACTION_DIAL, encodedUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(dialIntent)
        } catch (_: Exception) {
            // ActivityNotFound or restricted device
        }
    }
}
