package io.github.elli0t0.phone.core.recording

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import io.github.elli0t0.phone.data.call.CallRepository
import io.github.elli0t0.phone.data.call.CallState
import io.github.elli0t0.phone.data.recordings.RecordingEntity
import io.github.elli0t0.phone.data.recordings.RecordingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface RecordingSession {
    data object Idle : RecordingSession
    data class Recording(
        val number: String,
        val displayName: String?,
        val startedAt: Long,
        val isTest: Boolean
    ) : RecordingSession
}

/**
 * Owns a recording session end to end: MediaStore file, privileged
 * capture, foreground-service indicator, and the metadata row. The
 * foreground service is started only from UI-triggered paths (in-call
 * button, settings test), so the Android 12+ background-start ban
 * never applies. A call ending stops a call recording automatically;
 * a stray capture must never outlive its call.
 */
class RecordingController(
    private val context: Context,
    private val backend: RecordingBackend,
    private val recordingsRepository: RecordingsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sessionMutex = Mutex()

    private val _state = MutableStateFlow<RecordingSession>(RecordingSession.Idle)
    val state: StateFlow<RecordingSession> = _state.asStateFlow()
    val backendStatus: StateFlow<BackendStatus> = backend.status
    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private var active: ActiveSession? = null

    private class ActiveSession(
        val uri: Uri,
        val pfd: ParcelFileDescriptor,
        val number: String,
        val displayName: String?,
        val startedAt: Long,
        val isTest: Boolean
    )

    init {
        scope.launch {
            CallRepository.ongoingCallModel.collect { model ->
                val recording = active
                if (recording != null && !recording.isTest &&
                    (model == null || model.state == CallState.DISCONNECTED)
                ) {
                    stopRecording()
                }
            }
        }
    }

    suspend fun refreshBackendStatus() = backend.refreshStatus()

    suspend fun requestBackendPermission(): Boolean {
        val granted = backend.requestPermission()
        backend.refreshStatus()
        return granted
    }

    suspend fun startCallRecording(number: String, displayName: String?): Result<Unit> {
        if (backend.status.value != BackendStatus.READY) {
            backend.refreshStatus()
            if (backend.status.value != BackendStatus.READY) {
                return Result.failure(IllegalStateException("Recorder is not ready"))
            }
        }
        return startSession(number, displayName, isTest = false)
    }

    /** 3-second pipeline check: proves bind + capture + file without a call. */
    suspend fun runPipelineTest(): Result<CaptureResult> {
        if (backend.status.value != BackendStatus.READY) {
            backend.refreshStatus()
            if (backend.status.value != BackendStatus.READY) {
                return Result.failure(IllegalStateException("Recorder is not ready"))
            }
        }
        val started = startSession("", null, isTest = true)
        if (started.isFailure) return Result.failure(started.exceptionOrNull()!!)
        delay(TEST_DURATION_MS)
        return stopSession()
    }

    private suspend fun startSession(
        number: String,
        displayName: String?,
        isTest: Boolean
    ): Result<Unit> = sessionMutex.withLock {
        if (active != null) return@withLock Result.failure(
            IllegalStateException("Already recording")
        )
        runCatching {
            val startedAt = System.currentTimeMillis()
            val (uri, pfd) = createEntry(number, startedAt, isTest)
            val capture = backend.startCapture(pfd)
            if (capture.isFailure) {
                runCatching { pfd.close() }
                runCatching { context.contentResolver.delete(uri, null, null) }
                throw capture.exceptionOrNull()!!
            }
            active = ActiveSession(uri, pfd, number, displayName, startedAt, isTest)
            _state.value = RecordingSession.Recording(number, displayName, startedAt, isTest)
            _lastError.value = null
            context.startForegroundService(
                Intent(context, RecordingForegroundService::class.java)
            )
            Unit
        }
    }

    suspend fun stopRecording(): Result<Unit> {
        val result = stopSession()
        return if (result.isSuccess) {
            Result.success(Unit)
        } else {
            Result.failure(result.exceptionOrNull()!!)
        }
    }

    private suspend fun stopSession(): Result<CaptureResult> = sessionMutex.withLock {
        val session = active ?: return@withLock Result.failure(
            IllegalStateException("Nothing is recording")
        )
        active = null
        _state.value = RecordingSession.Idle
        val capture = backend.stopCapture()
        runCatching { session.pfd.close() }
        runCatching {
            val pendingOff = ContentValues().apply {
                put(MediaStore.Audio.Media.IS_PENDING, 0)
            }
            context.contentResolver.update(session.uri, pendingOff, null, null)
        }
        context.stopService(Intent(context, RecordingForegroundService::class.java))
        capture.onSuccess { result ->
            runCatching {
                recordingsRepository.add(
                    RecordingEntity(
                        uri = session.uri.toString(),
                        number = session.number,
                        displayName = session.displayName,
                        startedAt = session.startedAt,
                        durationMs = result.durationMs,
                        sizeBytes = result.pcmBytes + WavWriter.HEADER_SIZE,
                        isTest = session.isTest
                    )
                )
            }
        }.onFailure { _lastError.value = it.message }
    }

    private fun createEntry(
        number: String,
        startedAt: Long,
        isTest: Boolean
    ): Pair<Uri, ParcelFileDescriptor> {
        val digits = number.filter { it.isDigit() }.take(15).ifBlank { "unknown" }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(startedAt))
        val name = if (isTest) "Phone_test_$stamp.wav" else "Phone_${stamp}_$digits.wav"
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, name)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/x-wav")
            put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/Phone")
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Could not create recording file")
        val pfd = resolver.openFileDescriptor(uri, "w")
            ?: throw IllegalStateException("Could not open recording file").also {
                runCatching { resolver.delete(uri, null, null) }
            }
        return uri to pfd
    }

    companion object {
        private const val TEST_DURATION_MS = 3000L
    }
}
