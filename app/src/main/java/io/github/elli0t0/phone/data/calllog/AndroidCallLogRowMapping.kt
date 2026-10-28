package io.github.elli0t0.phone.data.calllog

import android.database.Cursor
import android.provider.CallLog
import io.github.elli0t0.phone.core.common.PhoneNumberFormatter
import io.github.elli0t0.phone.data.calllog.model.CallLogEntry
import io.github.elli0t0.phone.data.calllog.model.CallType
import io.github.elli0t0.phone.data.contacts.ContactNameIndex

    // One cursor row to a CallLogEntry. Shared by the full-log query
    // and the paged history query so both map rows identically.
internal fun mapRow(
        c: Cursor,
        idIdx: Int,
        numberIdx: Int,
        cachedNameIdx: Int,
        typeIdx: Int,
        dateIdx: Int,
        durationIdx: Int,
        accountIdIdx: Int,
        sims: List<SimBadge>
    ): CallLogEntry {
        val rawNumber = if (numberIdx >= 0) c.getString(numberIdx) ?: "" else ""
        val cachedName = if (cachedNameIdx >= 0) c.getString(cachedNameIdx) else null
        val phoneAccountId = if (accountIdIdx >= 0) c.getString(accountIdIdx) else null
        // Resolve from the in-memory index: one map lookup, no queries.
        val indexed = if (rawNumber.isNotBlank()) ContactNameIndex.resolve(rawNumber) else null
        return CallLogEntry(
            id = if (idIdx >= 0) c.getLong(idIdx) else 0L,
            number = rawNumber,
            formattedNumber = PhoneNumberFormatter.format(rawNumber),
            cachedName = cachedName,
            contactName = indexed?.displayName ?: cachedName,
            contactId = indexed?.contactId,
            contactLookupKey = indexed?.lookupKey,
            contactPhotoUri = indexed?.photoUri,
            type = mapCallType(if (typeIdx >= 0) c.getInt(typeIdx) else 0),
            date = if (dateIdx >= 0) c.getLong(dateIdx) else 0L,
            durationSeconds = if (durationIdx >= 0) c.getLong(durationIdx) else 0L,
            simInfo = resolveSimBadge(phoneAccountId, sims)
        )
    }

internal fun mapCallType(typeCode: Int): CallType {
        return when (typeCode) {
            CallLog.Calls.INCOMING_TYPE -> CallType.Incoming
            CallLog.Calls.OUTGOING_TYPE -> CallType.Outgoing
            CallLog.Calls.MISSED_TYPE -> CallType.Missed
            CallLog.Calls.VOICEMAIL_TYPE -> CallType.Voicemail
            CallLog.Calls.REJECTED_TYPE -> CallType.Rejected
            CallLog.Calls.BLOCKED_TYPE -> CallType.Blocked
            else -> CallType.Unknown
        }
    }
