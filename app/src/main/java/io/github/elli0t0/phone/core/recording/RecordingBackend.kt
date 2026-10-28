package io.github.elli0t0.phone.core.recording

import android.os.ParcelFileDescriptor
import kotlinx.coroutines.flow.StateFlow

enum class BackendStatus {
    NOT_INSTALLED,
    NOT_RUNNING,
    PERMISSION_NEEDED,
    READY,
    ERROR
}

data class CaptureResult(val pcmBytes: Long, val durationMs: Long)

/**
 * Privileged audio capture, behind an interface so the capture mechanism
 * (Shizuku today, embedded ADB later) can change without touching the
 * UI, the controller, or storage. The app process itself never records:
 * the backend runs capture in a shell-privileged process and writes to
 * a file descriptor the app created.
 */
interface RecordingBackend {
    val status: StateFlow<BackendStatus>
    suspend fun refreshStatus()
    suspend fun requestPermission(): Boolean
    suspend fun ensureBound(): Boolean
    suspend fun startCapture(fd: ParcelFileDescriptor): Result<Unit>
    suspend fun stopCapture(): Result<CaptureResult>
}
