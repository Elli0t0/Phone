package io.github.elli0t0.phone.core.recording

import android.os.ParcelFileDescriptor
import io.github.elli0t0.phone.data.settings.model.RecordingBackendKind
import kotlinx.coroutines.flow.StateFlow

enum class BackendStatus {
    NOT_INSTALLED,
    NOT_RUNNING,
    PERMISSION_NEEDED,
    NOT_PAIRED,
    READY,
    ERROR
}

data class CaptureResult(val pcmBytes: Long, val durationMs: Long)

/**
 * Privileged audio capture, behind an interface so the capture mechanism
 * can change without touching the UI, the controller, or storage. The app
 * process itself never records: the backend runs capture in a
 * shell-privileged process and writes to a file descriptor the app
 * created. Backends: Shizuku (a second app) and built-in ADB (the phone
 * pairs with itself over wireless debugging, no second app).
 */
interface RecordingBackend {
    val kind: RecordingBackendKind
    val status: StateFlow<BackendStatus>
    suspend fun refreshStatus()
    suspend fun requestPermission(): Boolean
    suspend fun ensureBound(): Boolean
    suspend fun startCapture(fd: ParcelFileDescriptor): Result<Unit>
    suspend fun stopCapture(): Result<CaptureResult>
    /** One-time wireless-debugging pairing; only the ADB backend supports it. */
    suspend fun pair(code: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("Pairing not supported"))
}
