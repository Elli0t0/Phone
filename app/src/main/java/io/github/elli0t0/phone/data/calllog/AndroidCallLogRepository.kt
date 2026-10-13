package io.github.elli0t0.phone.data.calllog

import android.content.ContentResolver
import android.content.ContentUris
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.provider.ContactsContract
import io.github.elli0t0.phone.core.common.PhoneNumberFormatter
import io.github.elli0t0.phone.data.calllog.model.CallLogEntry
import io.github.elli0t0.phone.data.calllog.model.CallType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class AndroidCallLogRepository(
    private val contentResolver: ContentResolver
) : CallLogRepository {

    private data class ResolvedContact(
        val displayName: String?,
        val contactId: Long?,
        val lookupKey: String?,
        val photoUri: String?
    )

    override fun getCallLogsFlow(): Flow<List<CallLogEntry>> = callbackFlow {
        fun sendCurrentLogs() {
            try {
                val logs = queryCallLogs()
                trySend(logs)
            } catch (_: SecurityException) {
                trySend(emptyList())
            } catch (_: Exception) {
                trySend(emptyList())
            }
        }

        // Send initial load
        sendCurrentLogs()

        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                sendCurrentLogs()
            }
        }

        try {
            contentResolver.registerContentObserver(
                CallLog.Calls.CONTENT_URI,
                true,
                observer
            )
        } catch (_: SecurityException) {
            // Permission not granted yet
        }

        awaitClose {
            try {
                contentResolver.unregisterContentObserver(observer)
            } catch (_: Exception) {
                // Ignore cleanup errors
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun queryCallLogs(): List<CallLogEntry> {
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.PHONE_ACCOUNT_ID
        )

        val sortOrder = "${CallLog.Calls.DATE} DESC LIMIT 500"
        val resolvedContactsCache = mutableMapOf<String, ResolvedContact>()
        val result = mutableListOf<CallLogEntry>()

        val cursor = contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        ) ?: return emptyList()

        cursor.use { c ->
            val idIdx = c.getColumnIndex(CallLog.Calls._ID)
            val numberIdx = c.getColumnIndex(CallLog.Calls.NUMBER)
            val cachedNameIdx = c.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val typeIdx = c.getColumnIndex(CallLog.Calls.TYPE)
            val dateIdx = c.getColumnIndex(CallLog.Calls.DATE)
            val durationIdx = c.getColumnIndex(CallLog.Calls.DURATION)
            val accountIdIdx = c.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID)

            while (c.moveToNext()) {
                val id = if (idIdx >= 0) c.getLong(idIdx) else 0L
                val rawNumber = if (numberIdx >= 0) c.getString(numberIdx) ?: "" else ""
                val cachedName = if (cachedNameIdx >= 0) c.getString(cachedNameIdx) else null
                val typeCode = if (typeIdx >= 0) c.getInt(typeIdx) else 0
                val date = if (dateIdx >= 0) c.getLong(dateIdx) else 0L
                val duration = if (durationIdx >= 0) c.getLong(durationIdx) else 0L
                val phoneAccountId = if (accountIdIdx >= 0) c.getString(accountIdIdx) else null

                val callType = mapCallType(typeCode)
                val resolvedContact = if (rawNumber.isNotBlank()) {
                    resolvedContactsCache.getOrPut(rawNumber) {
                        resolveContactByNumber(rawNumber)
                    }
                } else null

                val simInfo = phoneAccountId?.let { "SIM ${it.take(2)}" }

                result.add(
                    CallLogEntry(
                        id = id,
                        number = rawNumber,
                        formattedNumber = PhoneNumberFormatter.format(rawNumber),
                        cachedName = cachedName,
                        contactName = resolvedContact?.displayName,
                        contactId = resolvedContact?.contactId,
                        contactLookupKey = resolvedContact?.lookupKey,
                        contactPhotoUri = resolvedContact?.photoUri,
                        type = callType,
                        date = date,
                        durationSeconds = duration,
                        simInfo = simInfo
                    )
                )
            }
        }

        return result
    }

    private fun resolveContactByNumber(number: String): ResolvedContact {
        if (number.isBlank()) return ResolvedContact(null, null, null, null)

        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(number)
        )
        val projection = arrayOf(
            ContactsContract.PhoneLookup._ID,
            ContactsContract.PhoneLookup.LOOKUP_KEY,
            ContactsContract.PhoneLookup.DISPLAY_NAME,
            ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI
        )

        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(uri, projection, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val idIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup._ID)
                val lookupKeyIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.LOOKUP_KEY)
                val nameIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                val photoIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI)

                return ResolvedContact(
                    contactId = if (idIdx >= 0) cursor.getLong(idIdx) else null,
                    lookupKey = if (lookupKeyIdx >= 0) cursor.getString(lookupKeyIdx) else null,
                    displayName = if (nameIdx >= 0) cursor.getString(nameIdx) else null,
                    photoUri = if (photoIdx >= 0) cursor.getString(photoIdx) else null
                )
            }
        } catch (_: Exception) {
            // Permission or resolution fallback
        } finally {
            cursor?.close()
        }

        return ResolvedContact(null, null, null, null)
    }

    private fun mapCallType(typeCode: Int): CallType {
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

    override suspend fun deleteCall(id: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val uri = ContentUris.withAppendedId(CallLog.Calls.CONTENT_URI, id)
            val deletedRows = contentResolver.delete(uri, null, null)
            deletedRows > 0
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }
}
