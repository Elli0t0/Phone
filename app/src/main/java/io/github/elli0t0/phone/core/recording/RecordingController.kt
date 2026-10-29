package io.github.elli0t0.phone.core.recording

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import io.github.elli0t0.phone.core.recording.adb.AdbRecordingBackend
import io.github.elli0t0.phone.core.recording.CaptureSource
import io.github.elli0t0.phone.data.call.CallRepository
import io.github.elli0t0.phone.data.call.CallState
import io.github.elli0t0.phone.data.recordings.RecordingEntity
import io.github.elli0t0.phone.data.recordings.RecordingsRepository
import io.github.elli0t0.phone.data.settings.SettingsRepository
import io.github.elli0t0.phone.data.settings.model.RecordingBackendKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

/** Result of one pairing attempt, shown in-app as well as by notification. */
sealed interface PairResult {
    data object Paired : PairResult
    data class Failed(val message: String) : PairResult
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
    private val shizukuBackend: RecordingBackend,
    private val adbBackend: AdbRecordingBackend,
    private val settingsRepository: SettingsRepository,
    private val recordingsRepository: RecordingsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sessionMutex = Mutex()

    private val _state = MutableStateFlow<RecordingSession>(RecordingSession.Idle)
    val state: StateFlow<RecordingSession> = _state.asStateFlow()

    /** The backend the user picked in Settings; Shizuku until they choose ADB. */
    val activeBackendKind: StateFlow<RecordingBackendKind> = settingsRepository.settingsFlow
        .map { it.recordingBackend }
        .stateIn(scope, SharingStarted.Eagerly, RecordingBackendKind.SHIZUKU)

    /** Status of whichever backend is currently selected. */
    val backendStatus: StateFlow<BackendStatus> = activeBackendKind
        .flatMapLatest { kind -> backendFor(kind).status }
        .stateIn(scope, SharingStarted.Eagerly, BackendStatus.NOT_RUNNING)

    /** Diagnostic detail for the last ADB probe; null when healthy. */
    val backendDiagnostic: StateFlow<String?> = adbBackend.lastError

    /** Diagnostic line of the last finished capture (source, sample
     * rate, peaks, fallback failure); shown under Recorder status. */
    private val _lastCaptureDiagnostic = MutableStateFlow<String?>(null)
    val lastCaptureDiagnostic: StateFlow<String?> = _lastCaptureDiagnostic.asStateFlow()

    /** True when the last finished non-test recording was silent
     * (peak below 1%); the in-call UI shows the silent message from
     * this. Reset on every new start so it fires once per stop. */
    private val _lastRecordingSilent = MutableStateFlow(false)
    val lastRecordingSilent: StateFlow<Boolean> = _lastRecordingSilent.asStateFlow()

    /** Live capture level (0..1) from the ADB backend's reader; the
     * in-call meter reads it. Shizuku stays at 0 (it does not stream
     * PCM through the app). */
    val recordingLevel: StateFlow<Float> = adbBackend.captureLevel

    /** True while a call recording is armed: tapped before the call
     * was ACTIVE, waiting for the connect to start capture. */
    private val _pendingRecording = MutableStateFlow(false)
    val pendingRecording: StateFlow<Boolean> = _pendingRecording.asStateFlow()

    /** Armed call-recording request (number, display name), set by
     * startCallRecording before ACTIVE and consumed by the collector
     * below. Volatile: written from UI callers, read on the IO scope. */
    @Volatile
    private var pendingCall: Pair<String, String?>? = null

    /** Latest pairing attempt result, published by PairCodeReceiver. */
    private val _pairResult = MutableStateFlow<PairResult?>(null)
    val pairResult: StateFlow<PairResult?> = _pairResult.asStateFlow()

    fun publishPairResult(result: PairResult) {
        _pairResult.value = result
    }

    /** Optional manual ADB port from Settings; it always wins over NSD discovery. */
    val manualPort: StateFlow<Int?> = settingsRepository.settingsFlow
        .map { it.adbManualPort }
        .stateIn(scope, SharingStarted.Eagerly, null)

    /** Offline recording switch from Settings. */
    val offlineMode: StateFlow<Boolean> = settingsRepository.settingsFlow
        .map { it.adbOfflineMode }
        .stateIn(scope, SharingStarted.Eagerly, false)

    /** True when the last successful ADB probe used the local offline port. */
    val offlinePortActive: StateFlow<Boolean> = adbBackend.offlinePortActive

    /** True when the latest ADB probe connected, i.e. the key is accepted. */
    val adbKeyAccepted: StateFlow<Boolean> = adbBackend.keyAccepted

    /** Latest Detect-port attempt: the attempt list when nothing answered, null on success. */
    private val _detectResult = MutableStateFlow<String?>(null)
    val detectResult: StateFlow<String?> = _detectResult.asStateFlow()

    /** Runs endpoint selection and, on success, fills the manual-port field and re-probes. */
    suspend fun detectPort() {
        val result = adbBackend.detectPort()
        if (result.isSuccess) {
            _detectResult.value = null
            setManualPort(result.getOrThrow())
            refreshBackendStatus()
        } else {
            _detectResult.value = result.exceptionOrNull()?.message
        }
    }

    suspend fun setManualPort(port: Int?) {
        settingsRepository.setAdbManualPort(port)
    }

    suspend fun setOfflineMode(enabled: Boolean) {
        settingsRepository.setAdbOfflineMode(enabled)
    }

    suspend fun resetPairing() {
        activeBackend().resetPairing()
        refreshBackendStatus()
    }

    private fun backendFor(kind: RecordingBackendKind): RecordingBackend =
        if (kind == RecordingBackendKind.ADB) adbBackend else shizukuBackend

    private fun activeBackend(): RecordingBackend = backendFor(activeBackendKind.value)
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
                // Armed recording: the tap landed before ACTIVE. The
                // duration must not count dialing/ringing, so capture
                // starts only once the call connects; a disconnect
                // before that drops the request silently.
                val pending = pendingCall
                if (pending != null) {
                    if (model == null || model.state == CallState.DISCONNECTED) {
                        pendingCall = null
                        _pendingRecording.value = false
                    } else if (model.state == CallState.ACTIVE) {
                        pendingCall = null
                        _pendingRecording.value = false
                        startSession(pending.first, pending.second, isTest = false)
                            .onFailure { _lastError.value = it.message }
                    }
                }
            }
        }
    }

    suspend fun refreshBackendStatus() = activeBackend().refreshStatus()

    suspend fun requestBackendPermission(): Boolean {
        val granted = activeBackend().requestPermission()
        activeBackend().refreshStatus()
        return granted
    }

    suspend fun setBackend(kind: RecordingBackendKind) {
        settingsRepository.setRecordingBackend(kind)
    }

    suspend fun pairAdb(code: String): Result<Unit> = activeBackend().pair(code)

    suspend fun startCallRecording(number: String, displayName: String?): Result<Unit> {
        if (activeBackend().status.value != BackendStatus.READY) {
            activeBackend().refreshStatus()
            if (activeBackend().status.value != BackendStatus.READY) {
                return Result.failure(IllegalStateException("Recorder is not ready"))
            }
        }
        // Tapped before the call connected: arm instead of recording
        // dialing/ringing silence — capture (and its duration) starts
        // only once the call is ACTIVE, via the collector above.
        // Tapping again while armed disarms (the button toggles).
        val callState = CallRepository.ongoingCallModel.value?.state
        if (callState != null && callState != CallState.ACTIVE) {
            return sessionMutex.withLock {
                if (pendingCall != null) {
                    pendingCall = null
                    _pendingRecording.value = false
                } else {
                    pendingCall = number to displayName
                    _pendingRecording.value = true
                }
                Result.success(Unit)
            }
        }
        return startSession(number, displayName, isTest = false)
    }

    /**
     * 3-second pipeline check: proves bind + capture + file without a
     * call. The ADB backend records from the microphone (VOICE_CALL is
     * silent outside an active call, which is exactly what the old
     * test measured); Shizuku keeps its old call-source behavior.
     */
    suspend fun runPipelineTest(): Result<CaptureResult> {
        if (activeBackend().status.value != BackendStatus.READY) {
            activeBackend().refreshStatus()
            if (activeBackend().status.value != BackendStatus.READY) {
                // The test button shows the exact reason, not a bare
                // "not ready": the ADB diagnostic when ADB is selected.
                val detail = if (activeBackendKind.value == RecordingBackendKind.ADB) {
                    backendDiagnostic.value?.takeIf { it.isNotBlank() }
                } else {
                    null
                }
                return Result.failure(
                    IllegalStateException(detail ?: "Recorder is not ready")
                )
            }
        }
        val source = if (activeBackendKind.value == RecordingBackendKind.ADB) {
            CaptureSource.MIC
        } else {
            CaptureSource.CALL
        }
        val started = startSession("", null, isTest = true, source = source)
        if (started.isFailure) return Result.failure(started.exceptionOrNull()!!)
        delay(TEST_DURATION_MS)
        return stopSession()
    }

    private suspend fun startSession(
        number: String,
        displayName: String?,
        isTest: Boolean,
        source: CaptureSource = CaptureSource.CALL
    ): Result<Unit> = sessionMutex.withLock {
        if (active != null) return@withLock Result.failure(
            IllegalStateException("Already recording")
        )
        runCatching {
            val startedAt = System.currentTimeMillis()
            val (uri, pfd) = createEntry(number, startedAt, isTest)
            val capture = activeBackend().startCapture(pfd, source)
            if (capture.isFailure) {
                runCatching { pfd.close() }
                runCatching { context.contentResolver.delete(uri, null, null) }
                throw capture.exceptionOrNull()!!
            }
            active = ActiveSession(uri, pfd, number, displayName, startedAt, isTest)
            _state.value = RecordingSession.Recording(number, displayName, startedAt, isTest)
            _lastError.value = null
            _lastRecordingSilent.value = false
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
        val capture = activeBackend().stopCapture()
        runCatching { session.pfd.close() }
        runCatching {
            val pendingOff = ContentValues().apply {
                put(MediaStore.Audio.Media.IS_PENDING, 0)
            }
            context.contentResolver.update(session.uri, pendingOff, null, null)
        }
        context.stopService(Intent(context, RecordingForegroundService::class.java))
        capture.onSuccess { result ->
            // A null peak means the backend does not measure (Shizuku):
            // never read as silence. Below 1% the file is kept but
            // badged Silent, and a non-test stop raises the in-call
            // silent message.
            val silent = result.peak != null && result.peak < 0.01f
            _lastCaptureDiagnostic.value = result.diagnostic()
            if (!session.isTest) _lastRecordingSilent.value = silent
            runCatching {
                recordingsRepository.add(
                    RecordingEntity(
                        uri = session.uri.toString(),
                        number = session.number,
                        displayName = session.displayName,
                        startedAt = session.startedAt,
                        durationMs = result.durationMs,
                        sizeBytes = result.pcmBytes + WavWriter.HEADER_SIZE,
                        isTest = session.isTest,
                        isSilent = silent
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
