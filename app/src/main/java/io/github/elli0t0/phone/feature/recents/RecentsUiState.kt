package io.github.elli0t0.phone.feature.recents

import androidx.annotation.StringRes
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.data.calllog.util.DaySection
import io.github.elli0t0.phone.data.contacts.model.ContactSummary

enum class CallFilter(@StringRes val titleRes: Int) {
    ALL(R.string.filter_all),
    MISSED(R.string.filter_missed),
    INCOMING(R.string.filter_incoming),
    OUTGOING(R.string.filter_outgoing)
}

data class RecentsUiState(
    val daySections: List<DaySection> = emptyList(),
    val favorites: List<ContactSummary> = emptyList(),
    val selectedFilter: CallFilter = CallFilter.ALL,
    val searchQuery: String = "",
    val expandedCallId: Long? = null,
    val isSearching: Boolean = false,
    val isLoading: Boolean = false,
    val userMessageRes: Int? = null,
    val errorMessage: String? = null
)

