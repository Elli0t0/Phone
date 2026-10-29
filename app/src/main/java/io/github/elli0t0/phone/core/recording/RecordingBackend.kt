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

/** Capture input. CALL is the default for real recordings; MIC is used
 * only by the settings-page test, because VOICE_CALL returns audio
 * solely during an active call and a test without one is silent. */
enum class CaptureSource {
    CALL,
    MIC
}

/**
 * One finished capture. Peak values are fractions 0..1 (max absolute
 * 16-bit sample / 32768); null when the backend does not measure them
 * (Shizuku), so a null peak must never read as silence. The
 * diagnostic line carries everything Copy details needs: the source
 * tried, the sample rate, the first-second peak, and any fallback
 * failure text.
 */
data class CaptureResult(
    val pcmBytes: Long,
    val durationMs: Long,
    val peak: Float? = null,
    val firstSecondPeak: Float? = null,
    val source: CaptureSource = CaptureSource.CALL,
    val sampleRate: Int = WavWriter.SAMPLE_RATE,
    val fallbackError: String? = null
) {
    fun diagnostic(): String = buildString {
        append("source=${source.name.lowercase()} sampleRate=$sampleRate")
        if (firstSecondPeak != null) {
            append(" firstSecondPeak=${(firstSecondPeak * 100).toInt()}%")
        }
        if (peak != null) append(" peak=${(peak * 100).toInt()}%")
        if (fallbackError != null) append("\nfallback: $fallbackError")
    }
}

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
    suspend fun startCapture(fd: ParcelFileDescriptor, source: CaptureSource = CaptureSource.CALL): Result<Unit>
    suspend fun stopCapture(): Result<CaptureResult>
    /** One-time wireless-debugging pairing; only the ADB backend supports it. */
    suspend fun pair(code: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("Pairing not supported"))

    /** ADB overrides: deletes the persisted identity so the next pairing starts fresh. */
    suspend fun resetPairing() {}
}
