package io.github.elli0t0.phone.core.recording

import io.github.elli0t0.phone.core.common.PhoneNumberMatcher
import io.github.elli0t0.phone.data.settings.model.AutoRecordMode

/**
 * Pure auto-record filter (step 6): decides whether a call is
 * recorded automatically. JVM-testable: no Android types, and the
 * number comparison is injectable. The caller computes isContact
 * (contacts permission + ContactNameIndex); this function never
 * reads a display name, which can be carrier CNAP text.
 */
object AutoRecordPolicy {

    fun shouldAutoRecord(
        mode: AutoRecordMode,
        number: String?,
        isContact: Boolean,
        excluded: Set<String>,
        matches: (String, String) -> Boolean = { a, b -> PhoneNumberMatcher.matches(a, b) }
    ): Boolean = when (mode) {
        AutoRecordMode.ALL -> true
        AutoRecordMode.CONTACTS_ONLY -> isContact
        AutoRecordMode.UNKNOWN_ONLY -> !isContact
        AutoRecordMode.EXCEPT_LIST ->
            number.isNullOrBlank() || excluded.none { matches(it, number) }
    }
}
