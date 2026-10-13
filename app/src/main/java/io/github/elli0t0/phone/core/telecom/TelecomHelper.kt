package io.github.elli0t0.phone.core.telecom

import android.content.Context
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat

data class SimAccountInfo(
    val handle: PhoneAccountHandle,
    val label: String,
    val iccId: String?
)

object TelecomHelper {

    fun getCallCapablePhoneAccounts(context: Context): List<SimAccountInfo> {
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            ?: return emptyList()

        return try {
            val accounts = telecomManager.callCapablePhoneAccounts ?: return emptyList()
            accounts.mapIndexed { index, handle ->
                val phoneAccount = telecomManager.getPhoneAccount(handle)
                val label = phoneAccount?.label?.toString()?.takeIf { it.isNotBlank() } ?: "SIM ${index + 1}"
                SimAccountInfo(
                    handle = handle,
                    label = label,
                    iccId = handle.id
                )
            }
        } catch (_: SecurityException) {
            emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
