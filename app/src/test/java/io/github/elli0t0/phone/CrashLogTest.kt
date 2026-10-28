package io.github.elli0t0.phone

import io.github.elli0t0.phone.core.common.CrashLog
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashLogTest {

    @Test
    fun reportContainsThreadExceptionAndFrames() {
        val cause = IllegalStateException("inner boom")
        val error = RuntimeException("outer boom", cause)
        val report = CrashLog.buildReport("main", error)
        assertTrue(report.contains("Thread: main"))
        assertTrue(report.contains("RuntimeException: outer boom"))
        assertTrue(report.contains("Caused by: java.lang.IllegalStateException: inner boom"))
        assertTrue(report.contains("CrashLogTest"))
    }
}
