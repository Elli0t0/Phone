package io.github.elli0t0.phone.core.telecom

import android.util.Log
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Navigation / call lifecycle log (logcat tag "PhoneNav"). Entries are
 * also kept in a capped in-memory buffer shown in
 * Settings > About > Debug log, so the flow can be inspected on-device
 * without adb.
 */
object PhoneNavLog {
    private const val MAX_ENTRIES = 200
    private val entries = mutableListOf<String>()
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss.SSS", Locale.US)

    @Synchronized
    fun log(message: String) {
        Log.d("PhoneNav", message)
        entries.add("${timeFormat.format(LocalTime.now())} $message")
        while (entries.size > MAX_ENTRIES) entries.removeAt(0)
    }

    @Synchronized
    fun snapshot(): String =
        if (entries.isEmpty()) "No entries yet." else entries.joinToString("\n")
}
