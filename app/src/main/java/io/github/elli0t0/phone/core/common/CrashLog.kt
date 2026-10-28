package io.github.elli0t0.phone.core.common

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Last-crash report. The default uncaught-exception handler saves the
 * stack trace to the app's files dir, then the crash proceeds normally
 * (the previous handler still runs). The report is shown once on the
 * next launch so a crash can be diagnosed without logcat access: one
 * file, overwritten by the next crash, deleted once copied or dismissed.
 */
object CrashLog {
    private const val FILE_NAME = "last_crash.txt"

    fun install(context: Context) {
        val appContext = context.applicationContext
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                File(appContext.filesDir, FILE_NAME).writeText(
                    buildReport(thread.name, throwable)
                )
            } catch (_: Exception) {
                // Never mask the original crash.
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun read(context: Context): String? {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return null
        return try {
            file.readText()
        } catch (_: Exception) {
            null
        }
    }

    fun clear(context: Context) {
        try {
            File(context.filesDir, FILE_NAME).delete()
        } catch (_: Exception) {
            // Best effort only.
        }
    }

    internal fun buildReport(threadName: String, throwable: Throwable): String {
        val writer = StringWriter()
        val printer = PrintWriter(writer)
        printer.println("Thread: $threadName")
        printer.println("Time: ${java.util.Date()}")
        printer.println("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        printer.println()
        throwable.printStackTrace(printer)
        printer.flush()
        return writer.toString()
    }
}
