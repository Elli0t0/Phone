package io.github.elli0t0.phone.core.telecom

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager

/** One active SIM, identified by the subscription id USSD needs. */
data class UssdSim(
    val subscriptionId: Int,
    val label: String
)

/**
 * Runs carrier supplementary-service (MMI/USSD) codes such as *43# on a
 * chosen SIM via [TelephonyManager.sendUssdRequest], so the carrier
 * response is shown in-app instead of opening the dialer.
 */
object UssdHelper {

    fun getActiveSims(context: Context): List<UssdSim> {
        return try {
            val subscriptions = SubscriptionManager.from(context).activeSubscriptionInfoList
                ?: return emptyList()
            subscriptions.mapIndexed { index, info ->
                val label = info.displayName?.toString()?.takeIf { it.isNotBlank() }
                    ?: "SIM ${index + 1}"
                UssdSim(subscriptionId = info.subscriptionId, label = label)
            }
        } catch (_: SecurityException) {
            emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Starts [code] on [subscriptionId]. Returns false when the request
     * could not be started (no telephony, no permission) — the caller
     * falls back to ACTION_DIAL. Carrier answers arrive on [onResponse]
     * / [onCarrierError] on the main thread.
     */
    fun sendUssdRequest(
        context: Context,
        subscriptionId: Int,
        code: String,
        onResponse: (String) -> Unit,
        onCarrierError: (Int) -> Unit
    ): Boolean {
        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                ?: return false
        return try {
            telephonyManager.createForSubscriptionId(subscriptionId).sendUssdRequest(
                code,
                object : TelephonyManager.UssdResponseCallback() {
                    override fun onReceiveUssdResponse(
                        telephonyManager: TelephonyManager,
                        request: String,
                        response: CharSequence
                    ) {
                        onResponse(response.toString())
                    }

                    override fun onReceiveUssdResponseFailed(
                        telephonyManager: TelephonyManager,
                        request: String,
                        failureReason: Int
                    ) {
                        onCarrierError(failureReason)
                    }
                },
                Handler(Looper.getMainLooper())
            )
            true
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }
}
