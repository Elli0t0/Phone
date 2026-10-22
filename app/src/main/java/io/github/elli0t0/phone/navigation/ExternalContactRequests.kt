package io.github.elli0t0.phone.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * One-shot contact requests arriving from outside the nav graph:
 * system INSERT / INSERT_OR_EDIT intents received by MainActivity.
 * PhoneNavHost collects the flow, routes to the matching in-app
 * screen, then consumes the request.
 */
sealed interface ExternalContactRequest {
    data class Create(
        val name: String = "",
        val phone: String = "",
        val email: String = ""
    ) : ExternalContactRequest

    data class Append(
        val contactId: Long,
        val lookupKey: String,
        val phone: String
    ) : ExternalContactRequest
}

object ExternalContactRequests {
    private val _flow = MutableStateFlow<ExternalContactRequest?>(null)
    val flow: StateFlow<ExternalContactRequest?> = _flow.asStateFlow()

    fun post(request: ExternalContactRequest) {
        _flow.value = request
    }

    fun consume() {
        _flow.value = null
    }
}
