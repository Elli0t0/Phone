package io.github.elli0t0.phone.core.telecom

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import io.github.elli0t0.phone.data.favorites.ContactSimRepository
import io.github.elli0t0.phone.data.settings.model.DefaultSim
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface SimCallResolution {
    data class UseSim(val sim: SimAccountInfo) : SimCallResolution
    data object ShowDialog : SimCallResolution
}

object SimCallHelper {

    /**
     * Resolves which SIM to use for a call to [number].
     * - Single SIM: caller places the call directly, no dialog.
     * - Saved SIM for this contact and SIM still present: use it, no dialog.
     * - Otherwise the default SIM setting: SIM 1 / SIM 2 uses that
     *   account (by order), Ask every time shows the dialog.
     * - Default SIM missing (e.g. only one account left): show the dialog.
     * lookupKey may be null; then it is resolved from the number via PhoneLookup.
     */
    suspend fun resolveSim(
        context: Context,
        number: String,
        lookupKey: String?,
        simAccounts: List<SimAccountInfo>,
        contactSimRepository: ContactSimRepository,
        defaultSim: DefaultSim = DefaultSim.ASK
    ): SimCallResolution = withContext(Dispatchers.IO) {
        if (simAccounts.size <= 1) {
            return@withContext SimCallResolution.ShowDialog
        }
        val key = lookupKey?.takeIf { it.isNotBlank() } ?: lookupKeyForNumber(context, number)
        if (!key.isNullOrBlank()) {
            val savedSimId = try {
                contactSimRepository.getSimId(key)
            } catch (_: Exception) {
                null
            }
            if (!savedSimId.isNullOrBlank()) {
                val sim = simAccounts.firstOrNull { it.handle.id == savedSimId }
                if (sim != null) {
                    return@withContext SimCallResolution.UseSim(sim)
                }
                // Saved SIM gone: fall through to the default SIM.
            }
        }
        val defaultAccount = when (defaultSim) {
            DefaultSim.SIM_1 -> simAccounts.getOrNull(0)
            DefaultSim.SIM_2 -> simAccounts.getOrNull(1)
            DefaultSim.ASK -> null
        }
        if (defaultAccount != null) {
            SimCallResolution.UseSim(defaultAccount)
        } else {
            SimCallResolution.ShowDialog
        }
    }

    private fun lookupKeyForNumber(context: Context, number: String): String? {
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup.LOOKUP_KEY)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(ContactsContract.PhoneLookup.LOOKUP_KEY)
                    if (idx >= 0) cursor.getString(idx)?.takeIf { it.isNotBlank() } else null
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }
}
