package io.github.elli0t0.phone.data.calllog

import android.util.Log
import io.github.elli0t0.phone.data.calllog.model.CallLogEntry
import io.github.elli0t0.phone.data.calllog.model.CallType
import org.json.JSONArray
import org.json.JSONObject

internal fun AndroidCallLogRepository.saveCachedList(entries: List<CallLogEntry>) {
        try {
            val array = JSONArray()
            for (entry in entries) {
                val obj = JSONObject()
                obj.put("id", entry.id)
                obj.put("number", entry.number)
                obj.put("formattedNumber", entry.formattedNumber)
                obj.put("cachedName", entry.cachedName)
                obj.put("contactName", entry.contactName)
                obj.put("contactId", entry.contactId)
                obj.put("contactLookupKey", entry.contactLookupKey)
                obj.put("contactPhotoUri", entry.contactPhotoUri)
                obj.put("type", entry.type.name)
                obj.put("date", entry.date)
                obj.put("durationSeconds", entry.durationSeconds)
                obj.put("simInfo", entry.simInfo)
                array.put(obj)
            }
            cacheFile.writeText(array.toString())
        } catch (e: Exception) {
            Log.w(tag, "Failed to save cached call log", e)
        }
    }

internal fun AndroidCallLogRepository.loadCachedList() {
        try {
            if (!cacheFile.exists()) return
            val text = cacheFile.readText()
            if (text.isBlank()) return
            val array = JSONArray(text)
            val entries = mutableListOf<CallLogEntry>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val type = runCatching { CallType.valueOf(obj.optString("type")) }.getOrNull()
                    ?: CallType.Unknown
                entries.add(
                    CallLogEntry(
                        id = obj.optLong("id"),
                        number = obj.optString("number"),
                        formattedNumber = obj.optString("formattedNumber"),
                        cachedName = obj.optString("cachedName").takeIf { it.isNotEmpty() },
                        contactName = obj.optString("contactName").takeIf { it.isNotEmpty() },
                        contactId = obj.optLong("contactId").takeIf { it != 0L },
                        contactLookupKey = obj.optString("contactLookupKey").takeIf { it.isNotEmpty() },
                        contactPhotoUri = obj.optString("contactPhotoUri").takeIf { it.isNotEmpty() },
                        type = type,
                        date = obj.optLong("date"),
                        durationSeconds = obj.optLong("durationSeconds"),
                        simInfo = obj.optString("simInfo").takeIf { it.isNotEmpty() }
                    )
                )
            }
            // Only fill when nothing fresher arrived meanwhile.
            if (_callLogsFlow.value.isEmpty() && entries.isNotEmpty()) {
                _callLogsFlow.value = entries
                _isLoadingFlow.value = false
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to load cached call log", e)
        }
    }
