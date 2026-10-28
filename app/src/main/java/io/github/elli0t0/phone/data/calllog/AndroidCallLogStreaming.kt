package io.github.elli0t0.phone.data.calllog

import android.content.ContentResolver
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.CallLog
import android.util.Log
import io.github.elli0t0.phone.data.contacts.ContactNameIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.elli0t0.phone.data.calllog.model.CallLogEntry

internal suspend fun AndroidCallLogRepository.executeStreamingQuery() = withContext(Dispatchers.IO) {
        // Wait for the contact index so the first render already has names:
        // never emit rows with raw numbers while the index is still loading.
        // Falls through on failure/timeout (cached names only).
        ContactNameIndex.ensureStarted(contentResolver)
        ContactNameIndex.ensureBuilt()
        ContactNameIndex.awaitReady()

        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.PHONE_ACCOUNT_ID
        )

        val cursor: Cursor? = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val queryArgs = Bundle().apply {
                    putInt(ContentResolver.QUERY_ARG_LIMIT, AndroidCallLogRepository.STREAMING_LIMIT)
                    putStringArray(
                        ContentResolver.QUERY_ARG_SORT_COLUMNS,
                        arrayOf(CallLog.Calls.DATE)
                    )
                    putInt(
                        ContentResolver.QUERY_ARG_SORT_DIRECTION,
                        ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                    )
                }
                contentResolver.query(CallLog.Calls.CONTENT_URI, projection, queryArgs, null)
            } else {
                // API 29 and below: the provider rejects LIMIT in
                // sortOrder, so query without it and cap in the loop.
                contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    projection,
                    null,
                    null,
                    AndroidCallLogRepository.legacyCallLogSortOrder()
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Error querying CallLog cursor", e)
            throw e
        }

        if (cursor == null) {
            _callLogsFlow.value = emptyList()
            _isLoadingFlow.value = false
            return@withContext
        }

        // Resolve SIM badges once per query, not per row: the active
        // accounts list comes from TelecomHelper via the provider.
        val simBadges = simAccountsProvider().map { SimBadge(it.iccId, it.label) }

        val entries = mutableListOf<CallLogEntry>()
        var emittedFirst50 = false

        cursor.use { c ->
            val idIdx = c.getColumnIndex(CallLog.Calls._ID)
            val numberIdx = c.getColumnIndex(CallLog.Calls.NUMBER)
            val cachedNameIdx = c.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val typeIdx = c.getColumnIndex(CallLog.Calls.TYPE)
            val dateIdx = c.getColumnIndex(CallLog.Calls.DATE)
            val durationIdx = c.getColumnIndex(CallLog.Calls.DURATION)
            val accountIdIdx = c.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID)

            var count = 0
            while (c.moveToNext()) {
                entries.add(
                    mapRow(c, idIdx, numberIdx, cachedNameIdx, typeIdx, dateIdx, durationIdx, accountIdIdx, simBadges)
                )

                count++
                // Fast path: emit first 50 rows immediately so screen renders instantly
                if (count == 50 && !emittedFirst50) {
                    emittedFirst50 = true
                    _callLogsFlow.value = entries.toList()
                    _isLoadingFlow.value = false
                    logFirstEmit()
                }
                // The 500-row cap, enforced here instead of in the query:
                // it also guards OEMs that ignore the QUERY_ARG_LIMIT
                // bundle on newer APIs.
                if (count >= AndroidCallLogRepository.STREAMING_LIMIT) break
            }
        }

        // Full emit of all rows
        _callLogsFlow.value = entries.toList()
        _isLoadingFlow.value = false
        logFirstEmit()
        saveCachedList(entries)
    }

internal fun AndroidCallLogRepository.logFirstEmit() {
        if (!firstEmitLogged) {
            firstEmitLogged = true
            Log.d(
                "PhoneBoot",
                "first call-log emit in ${SystemClock.uptimeMillis() - createdAt}ms"
            )
        }
    }
