package io.github.elli0t0.phone.core.recording

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import io.github.elli0t0.phone.MainActivity
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.recording.adb.AdbRecordingBackend
import io.github.elli0t0.phone.core.recording.CaptureSource
import io.github.elli0t0.phone.data.call.CallRepository
import io.github.elli0t0.phone.data.call.CallState
import io.github.elli0t0.phone.data.recordings.RecordingEntity
import io.github.elli0t0.phone.data.recordings.RecordingsRepository
import io.github.elli0t0.phone.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    private val adbBackend: AdbRecordingBackend,
    private val settingsRepository: SettingsRepository,
    private val recordingsRepository: RecordingsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sessionMutex = Mutex()

    private val _state = MutableStateFlow<RecordingSession>(RecordingSession.Idle)
    val state: StateFlow<RecordingSession> = _state.asStateFlow()

    /** Status of the built-in ADB backend, the only backend. */
    val backendStatus: StateFlow<BackendStatus> = adbBackend.status

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
     * in-call meter reads it. */
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

    /** Auto-record switch from Settings (0.17.7). */
    val autoRecordCalls: StateFlow<Boolean> = settingsRepository.settingsFlow
        .map { it.autoRecordCalls }
        .stateIn(scope, SharingStarted.Eagerly, false)

    suspend fun setAutoRecordCalls(enabled: Boolean) {
        settingsRepository.setAutoRecordCalls(enabled)
    }

    /**
     * Id of the call auto-record already handled (started or failed):
     * a manual stop mid-call must not trigger a restart, and a failed
     * auto start must not retry, in the same call. Cleared when the
     * call model goes away.
     */
    private var autoHandledCallId: String? = null

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
        adbBackend.resetPairing()
        refreshBackendStatus()
    }

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
        // One-off cleanup of short silent rows kept by older
        // versions: the files themselves were already short and
        // silent, so only the rows are dropped.
        scope.launch {
            runCatching {
                recordingsRepository.recordings.first()
                    .filter { !it.isTest && it.isSilent && it.durationMs < MIN_KEEP_MS }
                    .forEach { recordingsRepository.remove(it) }
            }
        }
        scope.launch {
            CallRepository.ongoingCallModel.collect { model ->
                if (model == null) autoHandledCallId = null
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
                // Auto-record (0.17.7): on connect, with nothing
                // recording and no armed request, start a normal
                // (non-test) session for this call id. The id is
                // claimed before launching so a manual stop, or a
                // failed start, never restarts it in the same call.
                if (model != null && model.state == CallState.ACTIVE &&
                    active == null && pendingCall == null &&
                    autoRecordCalls.value && model.id != autoHandledCallId
                ) {
                    autoHandledCallId = model.id
                    scope.launch {
                        if (adbBackend.status.value != BackendStatus.READY) {
                            adbBackend.refreshStatus()
                        }
                        startSession(model.number, model.displayName, isTest = false)
                            .onFailure {
                                // A session that appeared meanwhile
                                // means a manual start won the race:
                                // that is not a failure to report.
                                if (active == null) {
                                    _lastError.value = it.message
                                    postAutoRecordFailedNotification()
                                }
                            }
                    }
                }
            }
        }
    }

    suspend fun refreshBackendStatus() = adbBackend.refreshStatus()

    suspend fun pairAdb(code: String): Result<Unit> = adbBackend.pair(code)

    suspend fun startCallRecording(number: String, displayName: String?): Result<Unit> {
        if (adbBackend.status.value != BackendStatus.READY) {
            adbBackend.refreshStatus()
            if (adbBackend.status.value != BackendStatus.READY) {
                return Result.failure(IllegalStateException(notReadyMessage()))
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
     * call. The backend records from the microphone (VOICE_CALL is
     * silent outside an active call, which is exactly what the old
     * test measured).
     */
    suspend fun runPipelineTest(): Result<CaptureResult> {
        if (adbBackend.status.value != BackendStatus.READY) {
            adbBackend.refreshStatus()
            if (adbBackend.status.value != BackendStatus.READY) {
                // The test button shows the exact reason, not a bare
                // "not ready": the ADB probe diagnostic.
                val detail = backendDiagnostic.value?.takeIf { it.isNotBlank() }
                return Result.failure(
                    IllegalStateException(detail ?: "Recorder is not ready")
                )
            }
        }
        val started = startSession("", null, isTest = true, source = CaptureSource.MIC)
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
            val capture = adbBackend.startCapture(pfd, source)
            if (capture.isFailure) {
                runCatching { pfd.close() }
                runCatching { context.contentResolver.delete(uri, null, null) }
                throw capture.exceptionOrNull()!!
            }
            active = ActiveSession(uri, pfd, number, displayName, startedAt, isTest)
            _state.value = RecordingSession.Recording(number, displayName, startedAt, isTest)
            _lastError.value = null
            _lastRecordingSilent.value = false
            try {
                context.startForegroundService(
                    Intent(context, RecordingForegroundService::class.java)
                )
            } catch (e: Exception) {
                // The service failed to start: roll the session back
                // completely so no orphan entry, descriptor, or
                // backend capture is left behind.
                runCatching { pfd.close() }
                runCatching { context.contentResolver.delete(uri, null, null) }
                runCatching { adbBackend.stopCapture() }
                active = null
                _state.value = RecordingSession.Idle
                throw e
            }
            Unit
        }
    }.also { result ->
        // A started non-test session marks its call handled for
        // auto-record: a manual start (any path) must not be followed
        // by an auto start later in the same call.
        if (result.isSuccess && !isTest) {
            CallRepository.ongoingCallModel.value?.id?.let { autoHandledCallId = it }
        }
    }

    /**
     * Low-priority failure note for a failed auto start, on the
     * existing recording channel. Tapping it opens the Call
     * recording page.
     */
    /**
     * Post-reboot check (BootReceiver): wait for the system to
     * settle, then warn if auto-record is on but the recorder
     * is not ready. The auto-record flag is read AFTER the delay
     * so the flow carries its real stored value by then.
     */
    suspend fun checkReadyAfterBoot() {
        delay(BOOT_CHECK_DELAY_MS)
        if (!autoRecordCalls.value) return
        adbBackend.refreshStatus()
        if (adbBackend.status.value != BackendStatus.READY) postAutoRecordFailedNotification()
    }

    private fun hasWifi(): Boolean = runCatching {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        @Suppress("DEPRECATION")
        cm.allNetworks.any { cm.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true }
    }.getOrDefault(true)

    private fun notReadyMessage() =
        if (hasWifi()) "Recorder is not ready" else context.getString(R.string.recording_needs_wifi)

    private fun postAutoRecordFailedNotification() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(RecordingForegroundService.CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    RecordingForegroundService.CHANNEL_ID,
                    context.getString(R.string.recording_notification_channel),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
        val openIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_OPEN_RECORDING, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            AUTO_RECORD_NOTIFICATION_ID,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, RecordingForegroundService.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_phone)
            .setContentTitle(context.getString(R.string.settings_call_recording))
            .setContentText(context.getString(if (hasWifi()) R.string.recording_auto_failed else R.string.recording_needs_wifi))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        manager.notify(AUTO_RECORD_NOTIFICATION_ID, notification)
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
        val capture = adbBackend.stopCapture()
        runCatching { session.pfd.close() }
        runCatching {
            val pendingOff = ContentValues().apply {
                put(MediaStore.Audio.Media.IS_PENDING, 0)
            }
            context.contentResolver.update(session.uri, pendingOff, null, null)
        }
        context.stopService(Intent(context, RecordingForegroundService::class.java))
        capture.onSuccess { result ->
            // A null peak means the backend does not measure:
            // never read as silence. Below 1% the file is kept but
            // badged Silent, and a non-test stop raises the in-call
            // silent message.
            val silent = result.peak != null && result.peak < 0.01f
            _lastCaptureDiagnostic.value = result.diagnostic()
            // Short silent (or unmeasured) non-test capture: delete
            // the file and keep no row for it.
            if (!session.isTest && result.durationMs < MIN_KEEP_MS && (silent || result.peak == null)) {
                runCatching { context.contentResolver.delete(session.uri, null, null) }
                return@onSuccess
            }
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
        private const val AUTO_RECORD_NOTIFICATION_ID = 4403
        /** Non-test captures shorter than this that are silent (or
         * unmeasured) are deleted instead of kept. */
        const val MIN_KEEP_MS = 2000L
        private const val BOOT_CHECK_DELAY_MS = 60_000L
    }
}
