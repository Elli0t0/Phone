package io.github.elli0t0.phone.data.calllog.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.github.elli0t0.phone.R

enum class CallType(
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int
) {
    Incoming(R.string.call_type_incoming, R.drawable.ic_call_incoming),
    Outgoing(R.string.call_type_outgoing, R.drawable.ic_call_outgoing),
    Missed(R.string.call_type_missed, R.drawable.ic_call_missed),
    Rejected(R.string.call_type_rejected, R.drawable.ic_call_missed),
    Voicemail(R.string.call_type_voicemail, R.drawable.ic_phone),
    Blocked(R.string.call_type_blocked, R.drawable.ic_call_missed),
    Unknown(R.string.call_type_unknown, R.drawable.ic_phone)
}
