package io.github.elli0t0.phone.core.telecom

import android.os.Build
import android.telecom.Call
import android.telecom.CallResponse
import android.telecom.CallScreeningService
import android.telecom.TelecomManager
import android.util.Log
import io.github.elli0t0.phone.PhoneApplication
import io.github.elli0t0.phone.data.spam.SpamDecider
import io.github.elli0t0.phone.data.spam.SpamDecision
import io.github.elli0t0.phone.data.spam.SpamMatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Offline call screening (no server, no uploads). The system binds
 * this when the app is the default dialer or holds
 * RoleManager.ROLE_CALL_SCREENING (requestable from Settings >
 * Spam protection). Only incoming calls are screened, and only on
 * API 29+, where the call direction is known.
 *
 * Order: contacts are NEVER screened; then the user-reported spam
 * list, then the app-side blocked prefixes (same logic as the
 * InCallService fallback), then the hidden-number setting; then
 * "silence unknown callers" rings silently instead of rejecting.
 * A blocked call is rejected with no notification; the platform
 * still writes it to the call log, so it appears in Recents.
 *
 * The decision must reach respondToCall within ~3 s, so it runs
 * against the in-memory spam cache and a single PhoneLookup query,
 * with a 2.5 s timeout. Any exception or timeout responds ALLOW —
 * screening must never eat a real call.
 */
class PhoneCallScreeningService : CallScreeningService() {

    override fun onScreenCall(details: Call.Details) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            respondAllow(details)
            return
        }
        val decision = runBlocking {
            withTimeoutOrNull(2500L) {
                withContext(Dispatchers.IO) { decide(details) }
            }
        } ?: SpamDecision.ALLOW
        try {
            when (decision) {
                SpamDecision.BLOCK -> {
                    runBlocking {
                        runCatching {
                            (application as? PhoneApplication)?.container
                                ?.spamRepository?.incrementBlockedCount()
                        }
                    }
                    respondToCall(
                        details,
                        CallResponse.Builder()
                            .setDisallowCall(true)
                            .setRejectCall(true)
                            .setSkipNotification(true)
                            .build()
                    )
                    PhoneNavLog.log("screening BLOCK number=${details.handle?.schemeSpecificPart}")
                }
                SpamDecision.SILENCE -> {
                    respondToCall(
                        details,
                        CallResponse.Builder().setSilenceCall(true).build()
                    )
                    PhoneNavLog.log("screening SILENCE number=${details.handle?.schemeSpecificPart}")
                }
                SpamDecision.ALLOW -> respondAllow(details)
            }
        } catch (e: Exception) {
            Log.e(TAG, "respondToCall failed", e)
            runCatching { respondAllow(details) }
        }
    }

    private suspend fun decide(details: Call.Details): SpamDecision {
        return try {
            if (details.callDirection != Call.Details.DIRECTION_INCOMING) {
                return SpamDecision.ALLOW
            }
            val app = application as? PhoneApplication ?: return SpamDecision.ALLOW
            val current = runCatching {
                kotlinx.coroutines.flow.first(app.container.settingsRepository.settingsFlow)
            }.getOrNull() ?: return SpamDecision.ALLOW
            val number = details.handle?.schemeSpecificPart.orEmpty()
            val spamRepository = app.container.spamRepository
            // handlePresentation is an API 29+ getter; runCatching
            // also covers a NoSuchMethodError on any device that
            // lacks it, so screening can never crash the service.
            val presentation = runCatching { details.handlePresentation }
                .getOrDefault(TelecomManager.PRESENTATION_ALLOWED)
            SpamDecider.decide(
                blockKnownSpam = current.spamBlockKnown,
                blockHiddenNumbers = current.spamBlockHidden,
                silenceUnknownCallers = current.spamSilenceUnknown,
                isSpam = number.isNotBlank() && spamRepository.isSpam(number),
                prefixBlocked = SpamMatcher.matchesPrefix(current.blockedPrefixes, number),
                isHiddenNumber = SpamMatcher.isHiddenNumber(number, presentation),
                hasNumber = number.isNotBlank(),
                inContacts = spamRepository.isInContacts(contentResolver, number)
            )
        } catch (e: Exception) {
            Log.e(TAG, "screening decide failed, allowing", e)
            SpamDecision.ALLOW
        }
    }

    private fun respondAllow(details: Call.Details) {
        runCatching {
            respondToCall(details, CallResponse.Builder().build())
        }
    }

    companion object {
        private const val TAG = "PhoneScreening"
    }
}
