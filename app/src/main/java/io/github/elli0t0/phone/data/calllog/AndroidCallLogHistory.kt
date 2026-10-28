package io.github.elli0t0.phone.data.calllog

import android.content.ContentResolver
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.provider.CallLog
import android.util.Log
import io.github.elli0t0.phone.core.common.PhoneNumberMatcher
import io.github.elli0t0.phone.data.calllog.model.CallHistoryPage
import io.github.elli0t0.phone.data.calllog.model.CallLogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

    // Paged history for one contact. Raw rows are scanned newest-first
    // in chunks; each chunk is filtered with the same normalized matcher
    // the detail screen uses, so formatting variants of the contact's
    // numbers all match. Scanning stops once the page plus one probe
    // match are collected, or the log is exhausted.
internal suspend fun AndroidCallLogRepository.getCallHistoryPageInternal(
        numbers: List<String>,
        limit: Int,
        offset: Int
    ): CallHistoryPage = withContext(Dispatchers.IO) {
        if (numbers.isEmpty() || limit <= 0 || offset < 0) {
            return@withContext CallHistoryPage(emptyList(), false)
        }
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.PHONE_ACCOUNT_ID
        )
        val collected = mutableListOf<CallLogEntry>()
        var skipped = 0
        var rawOffset = 0
        var exhausted = false
        // Same once-per-query badge list as the streaming query.
        val simBadges = simAccountsProvider().map { SimBadge(it.iccId, it.label) }
        while (collected.size <= limit && !exhausted) {
            // Growing LIMIT, no OFFSET: the call-log provider does not
            // honor QUERY_ARG_OFFSET on all devices, and a dropped
            // offset re-reads the same rows, returning duplicate entries
            // (which crashed the list on duplicate keys) or spinning
            // forever when a window held no matches. Each chunk query
            // reads the prefix up to the window end; rows before the
            // window are skipped in memory, so paging never depends on
            // the provider's offset support.
            val queryLimit = rawOffset + AndroidCallLogRepository.HISTORY_CHUNK_SIZE
            val cursor: Cursor? = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val queryArgs = Bundle().apply {
                        putInt(ContentResolver.QUERY_ARG_LIMIT, queryLimit)
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
                    // API 29 and below: no LIMIT in sortOrder (the
                    // provider rejects it). The loop below already stops
                    // at the window end, so no extra cap is needed here.
                    contentResolver.query(
                        CallLog.Calls.CONTENT_URI,
                        projection,
                        null,
                        null,
                        AndroidCallLogRepository.legacyCallLogSortOrder()
                    )
                }
            } catch (e: SecurityException) {
                Log.e(tag, "SecurityException querying call history page", e)
                return@withContext CallHistoryPage(emptyList(), false)
            } catch (e: Exception) {
                Log.e(tag, "Error querying call history page", e)
                return@withContext CallHistoryPage(emptyList(), false)
            }
            if (cursor == null) break
            var processed = 0
            var rowIndex = 0
            cursor.use { c ->
                val idIdx = c.getColumnIndex(CallLog.Calls._ID)
                val numberIdx = c.getColumnIndex(CallLog.Calls.NUMBER)
                val cachedNameIdx = c.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val typeIdx = c.getColumnIndex(CallLog.Calls.TYPE)
                val dateIdx = c.getColumnIndex(CallLog.Calls.DATE)
                val durationIdx = c.getColumnIndex(CallLog.Calls.DURATION)
                val accountIdIdx = c.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID)
                while (c.moveToNext()) {
                    if (rowIndex < rawOffset) {
                        rowIndex++
                        continue
                    }
                    if (processed >= AndroidCallLogRepository.HISTORY_CHUNK_SIZE) break
                    rowIndex++
                    processed++
                    val rawNumber = if (numberIdx >= 0) c.getString(numberIdx) ?: "" else ""
                    if (rawNumber.isBlank() || numbers.none { PhoneNumberMatcher.matches(it, rawNumber) }) {
                        continue
                    }
                    if (skipped < offset) {
                        skipped++
                        continue
                    }
                    collected.add(
                        mapRow(c, idIdx, numberIdx, cachedNameIdx, typeIdx, dateIdx, durationIdx, accountIdIdx, simBadges)
                    )
                    if (collected.size > limit) break
                }
                // The cursor ended before the window filled: no more rows.
                if (collected.size <= limit && rowIndex < rawOffset + AndroidCallLogRepository.HISTORY_CHUNK_SIZE) {
                    exhausted = true
                }
            }
            rawOffset += processed
        }
        val hasMore = collected.size > limit
        CallHistoryPage(
            entries = if (hasMore) collected.dropLast(1) else collected,
            hasMore = hasMore
        )
    }
