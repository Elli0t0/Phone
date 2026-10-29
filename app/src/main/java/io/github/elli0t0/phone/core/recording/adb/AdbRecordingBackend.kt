package io.github.elli0t0.phone.core.recording.adb

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.ParcelFileDescriptor
import android.provider.Settings
import com.flyfishxu.kadb.Kadb
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.recording.BackendStatus
import io.github.elli0t0.phone.core.recording.CaptureResult
import io.github.elli0t0.phone.core.recording.CaptureSource
import io.github.elli0t0.phone.core.recording.RecordingBackend
import io.github.elli0t0.phone.core.recording.WavWriter
import io.github.elli0t0.phone.data.settings.SettingsRepository
import io.github.elli0t0.phone.data.settings.model.RecordingBackendKind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import okio.buffer
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

/**
 * Call recording without a second app. The phone pairs with itself over
 * wireless debugging (one-time, via Kadb's SPAKE2/TLS pairing), then this
 * backend runs shell commands over that ADB connection to start Phone's
 * own recorder helper as shell, detached from the ADB session. The
 * helper streams raw PCM to the app over loopback TCP (an abstract
 * local socket owned by the shell SELinux domain was denied to the
 * app); the app writes the WAV into the MediaStore descriptor it
 * created, exactly like the Shizuku path — so storage, playback,
 * delete, and share are identical. After a reboot the helper is gone;
 * it is re-armed on demand whenever wireless debugging is on and the
 * phone is on Wi-Fi.
 *
 * Diagnostics: probe() checks Wi-Fi, the wireless-debugging setting,
 * NSD discovery, then the ADB connect in order, and reports a specific
 * message for each failure instead of mapping everything to NOT_PAIRED.
 */
private const val OFFLINE_ADB_HOST = "127.0.0.1"
private const val OFFLINE_ADB_PORT = 5555

class AdbRecordingBackend(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) : RecordingBackend {

    override val kind = RecordingBackendKind.ADB

    init {
        ensureKadbIdentity(context)
    }

    private val _status = MutableStateFlow(BackendStatus.NOT_RUNNING)
    override val status: StateFlow<BackendStatus> = _status.asStateFlow()

    /** Specific reason for the last non-ready probe; null when healthy. */
    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    /**
     * True when the latest probe connected over ADB, i.e. the pairing
     * key is accepted. False after any failed probe and after a reset:
     * a failed probe proves nothing about the key either way, so only
     * a successful connect counts. The settings screen shows "Paired"
     * from this, not from the status alone.
     */
    private val _keyAccepted = MutableStateFlow(false)
    val keyAccepted: StateFlow<Boolean> = _keyAccepted.asStateFlow()

    /** True when the last successful probe used the local offline port. */
    private val _offlinePortActive = MutableStateFlow(false)
    val offlinePortActive: StateFlow<Boolean> = _offlinePortActive.asStateFlow()

    /** Endpoint of the last successful probe; reused for the helper bootstrap. */
    private var workingEndpoint: Pair<String, Int>? = null

    /** adb_allowed_connection_time is set once per process, best-effort. */
    private var connectionTimeApplied = false

    /** Active TCP capture: the socket, the WAV stream, the reader job, the byte count. */
    private val captureScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var captureSocket: Socket? = null
    private var captureOut: FileOutputStream? = null
    private var captureJob: Job? = null
    private val captureBytes = AtomicLong(0)

    /** Peak amplitude of the running capture (0..1), for the live
     * in-call meter. Updated per chunk by the reader; the controller
     * exposes it, the meter reads it. Zero when nothing records. */
    private val _captureLevel = MutableStateFlow(0f)
    val captureLevel: StateFlow<Float> = _captureLevel.asStateFlow()

    /** Finished-capture peaks, read by stopCapture after the reader
     * joined (the join is the happens-before edge). */
    private var capturePeak = 0f
    private var captureFirstSecondPeak = 0f
    private var captureSource = CaptureSource.CALL

    /** Diagnostic line of the last finished capture (source, sample
     * rate, peaks, fallback failure); shown under Recorder status
     * and copied by Copy details. Not cleared by status refreshes. */
    private val _captureDiagnostic = MutableStateFlow<String?>(null)
    val captureDiagnostic: StateFlow<String?> = _captureDiagnostic.asStateFlow()

    /** Message from the last failed helper-socket connect, for diagnostics. */
    private var lastSocketError: String? = null

    private sealed interface Probe {
        data class Ok(
            val host: String,
            val port: Int,
            val offline: Boolean = false,
            val offlineNote: String? = null
        ) : Probe
        data class Fail(val status: BackendStatus, val message: String) : Probe
    }

    override suspend fun refreshStatus() {
        when (val probe = probe()) {
            is Probe.Ok -> {
                _keyAccepted.value = true
                workingEndpoint = probe.host to probe.port
                // The offline port is never persisted as last-known: it
                // must not leak into the Wi-Fi path when the switch is off.
                if (!probe.offline) settingsRepository.setAdbLastKnownPort(probe.port)
                _offlinePortActive.value = probe.offline
                if (ensureHelperAlive(probe.host, probe.port)) {
                    _status.value = BackendStatus.READY
                    _lastError.value = probe.offlineNote
                } else {
                    _status.value = BackendStatus.ERROR
                    // ensureHelperAlive left the helper diagnostics in
                    // _lastError; keep them (plus any offline note) so
                    // Recorder status and Copy details show the reason
                    // instead of a bare "helper could not start".
                    val details = listOfNotNull(
                        probe.offlineNote,
                        _lastError.value
                    ).joinToString("\n").takeIf { it.isNotBlank() }
                    _lastError.value = details
                }
            }
            is Probe.Fail -> {
                _keyAccepted.value = false
                workingEndpoint = null
                _offlinePortActive.value = false
                _status.value = probe.status
                _lastError.value = probe.message
            }
        }
    }

    override suspend fun requestPermission(): Boolean {
        // ADB has no permission prompt; pairing is the gate.
        return status.value == BackendStatus.READY
    }

    override suspend fun ensureBound(): Boolean {
        // A helper already running (stored port/token) is reused
        // without any Kadb round-trip, so it survives app restarts.
        val stored = settingsRepository.settingsFlow.first()
        val storedPort = stored.adbHelperPort
        val storedToken = stored.adbHelperToken
        if (storedPort != null && storedToken != null &&
            helperStatus(storedPort, storedToken) != null
        ) {
            return true
        }
        val storedEndpoint = workingEndpoint
        if (storedEndpoint != null) {
            // Reuse the verified endpoint; no second discovery while
            // it still answers — a fresh discovery could return a
            // different stale service.
            val stillAnswers = withContext(Dispatchers.IO) {
                reachable(storedEndpoint.first, storedEndpoint.second)
            }
            if (stillAnswers) return ensureHelperAlive(storedEndpoint.first, storedEndpoint.second)
        }
        // Stored endpoint missing or dead: prefer the local offline port
        // when the switch is on, before falling back to Wi-Fi selection.
        if (settingsRepository.settingsFlow.first().adbOfflineMode &&
            withContext(Dispatchers.IO) { reachable(OFFLINE_ADB_HOST, OFFLINE_ADB_PORT) }
        ) {
            val offline = OFFLINE_ADB_HOST to OFFLINE_ADB_PORT
            workingEndpoint = offline
            if (ensureHelperAlive(offline.first, offline.second)) return true
        }
        // Stored endpoint missing or dead: one fresh selection.
        val endpoint = withContext(Dispatchers.IO) { selectEndpoint() }.endpoint
            ?: return false
        workingEndpoint = endpoint
        if (ensureHelperAlive(endpoint.first, endpoint.second)) {
            settingsRepository.setAdbLastKnownPort(endpoint.second)
            return true
        }
        return false
    }

    override suspend fun pair(code: String): Result<Unit> = runCatching {
        // A failed discovery means the pairing service was not found
        // within 30 s: tell the user to open the system dialog first
        // instead of surfacing NSD internals.
        val endpoint = runCatching {
            AdbDiscovery.discoverPairing(context, 30_000).getOrThrow()
        }.getOrElse {
            throw IllegalStateException(
                context.getString(R.string.recording_pair_open_first)
            )
        }
        // Pair as "Phone": Kadb's default name is the OS build's
        // user@host, which shows up as a cryptic entry in Paired devices.
        Kadb.pair(endpoint.first, endpoint.second, code, "Phone")
        refreshStatus()
        Unit
    }

    override suspend fun resetPairing() {
        runCatching { kadbKeyFile(context).delete() }
        ensureKadbIdentity(context)
        _keyAccepted.value = false
    }

    override suspend fun startCapture(
        fd: ParcelFileDescriptor,
        source: CaptureSource
    ): Result<Unit> = runCatching {
        if (!ensureBound()) {
            throw IllegalStateException(
                _lastError.value?.takeIf { it.isNotBlank() }
                    ?: "Recorder helper is not running"
            )
        }
        val settings = settingsRepository.settingsFlow.first()
        val port = settings.adbHelperPort
        val token = settings.adbHelperToken
        if (port == null || token == null) {
            throw IllegalStateException("Recorder helper is not running")
        }
        // A helper orphaned by an app restart still holds a capture;
        // the helper preempts it on START, so this session owns the file.
        // CALL is the default; MIC only for the settings-page test,
        // because VOICE_CALL is silent outside an active call.
        val command = if (source == CaptureSource.MIC) "START MIC" else "START CALL"
        val socket = withContext(Dispatchers.IO) {
            val s = Socket()
            s.connect(InetSocketAddress(OFFLINE_ADB_HOST, port), 1500)
            s.soTimeout = 15_000
            s.getOutputStream().write("$token\n$command\n".toByteArray(Charsets.US_ASCII))
            s.getOutputStream().flush()
            val reply = readLine(s.getInputStream())
            if (reply != "OK") {
                runCatching { s.close() }
                throw IllegalStateException(
                    reply.removePrefix("ERR ").ifBlank { "Recorder refused to start" }
                )
            }
            s
        }
        // The helper now streams raw PCM on this socket; the app
        // writes it into its own MediaStore file with a placeholder
        // header, patched on stop — the old FD-passing path is gone.
        val out = FileOutputStream(fd.fileDescriptor)
        out.write(WavWriter.header(0))
        out.flush()
        captureSocket = socket
        captureOut = out
        captureBytes.set(0)
        captureSource = source
        capturePeak = 0f
        captureFirstSecondPeak = 0f
        _captureLevel.value = 0f
        captureJob = captureScope.launch {
            val buffer = ByteArray(8192)
            var peak = 0f
            var firstPeakSample = 0
            var firstSecondSamples = 0
            try {
                while (true) {
                    val read = socket.getInputStream().read(buffer)
                    if (read == -1) break
                    if (read > 0) {
                        out.write(buffer, 0, read)
                        captureBytes.addAndGet(read.toLong())
                        // 16-bit little-endian samples: overall peak,
                        // first-second peak, and the live meter level.
                        var chunkPeak = 0
                        var i = 0
                        while (i + 1 < read) {
                            val sample = (buffer[i].toInt() and 0xFF) or
                                (buffer[i + 1].toInt() shl 8)
                            val abs = if (sample < 0) -sample else sample
                            if (abs > chunkPeak) chunkPeak = abs
                            if (firstSecondSamples < WavWriter.SAMPLE_RATE) {
                                if (abs > firstPeakSample) firstPeakSample = abs
                                firstSecondSamples++
                            }
                            i += 2
                        }
                        val chunkLevel = chunkPeak / 32768f
                        if (chunkLevel > peak) peak = chunkLevel
                        _captureLevel.value = chunkLevel
                    }
                }
            } catch (_: Exception) {
                // Socket closed by stopCapture, or the helper died:
                // bytes so far are still a valid recording.
            } finally {
                capturePeak = peak
                captureFirstSecondPeak = firstPeakSample / 32768f
            }
        }
        Unit
    }

    override suspend fun stopCapture(): Result<CaptureResult> = runCatching {
        val socket = captureSocket
            ?: throw IllegalStateException("Nothing is recording")
        val out = captureOut
        try {
            // Closing the socket ends the helper's capture (its watcher
            // sees EOF); wait briefly for the reader to drain, then
            // patch the placeholder header with the real sizes.
            runCatching { socket.close() }
            withTimeoutOrNull(2000) { captureJob?.join() }
            val bytes = captureBytes.get()
            if (out != null) {
                out.flush()
                val channel = out.channel
                channel.position(0)
                val header = ByteBuffer.wrap(WavWriter.header(bytes.toInt()))
                while (header.hasRemaining()) {
                    channel.write(header)
                }
                out.flush()
            }
            val result = CaptureResult(
                pcmBytes = bytes,
                durationMs = WavWriter.durationMs(bytes),
                peak = capturePeak,
                firstSecondPeak = captureFirstSecondPeak,
                source = captureSource,
                sampleRate = WavWriter.SAMPLE_RATE,
                fallbackError = readFallbackError()
            )
            _captureDiagnostic.value = result.diagnostic()
            result
        } finally {
            // The ParcelFileDescriptor itself stays open: the
            // controller owns and closes it after stop.
            captureSocket = null
            captureOut = null
            captureJob = null
            captureBytes.set(0)
            _captureLevel.value = 0f
        }
    }

    /**
     * Best-effort read of a FALLBACK_FAILED line from the helper log,
     * attributed to this capture only: lines after the last
     * SESSION_START belong to the current capture (the log file is
     * truncated on every helper launch). Capped at 3 s and null on
     * any failure, so a dead ADB link can never stall a stop.
     */
    private suspend fun readFallbackError(): String? {
        val endpoint = workingEndpoint ?: return null
        return withTimeoutOrNull(3000) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val log = kadbShell(endpoint.first, endpoint.second, "tail -50 $HELPER_LOG_PATH")
                    val lines = log.lines()
                    val sessionStart = lines.indexOfLast { it.startsWith("SESSION_START") }
                    val recent = if (sessionStart >= 0) lines.drop(sessionStart + 1) else lines
                    recent.lastOrNull { it.startsWith("FALLBACK_FAILED:") }
                        ?.removePrefix("FALLBACK_FAILED:")?.trim()?.takeIf { it.isNotBlank() }
                }.getOrNull()
            }
        }
    }

    /**
     * Checks, in order: the local offline port first when the switch is
     * on (no Wi-Fi checks), then Wi-Fi, the wireless-debugging setting,
     * endpoint selection (manual port, getprop port, NSD newest-first,
     * last known port — every candidate TCP-checked), then the ADB
     * connect on the ONE verified endpoint. Only an auth failure maps
     * to NOT_PAIRED; anything else keeps its own message so the real
     * reason is visible.
     */
    private suspend fun probe(): Probe = withContext(Dispatchers.IO) {
        val offlineMode = settingsRepository.settingsFlow.first().adbOfflineMode
        if (offlineMode && reachable(OFFLINE_ADB_HOST, OFFLINE_ADB_PORT)) {
            // Local offline port first: no Wi-Fi or wireless-debugging checks.
            tryOfflinePort()?.let { return@withContext it }
            // Reachable but unusable for a non-auth reason: fall through
            // to the Wi-Fi path instead of failing outright.
        }
        if (!isWifi()) {
            return@withContext Probe.Fail(
                BackendStatus.NOT_RUNNING,
                if (offlineMode) {
                    context.getString(R.string.recording_error_offline_needs_wifi)
                } else {
                    context.getString(R.string.recording_error_no_wifi)
                }
            )
        }
        if (Settings.Global.getInt(context.contentResolver, "adb_wifi_enabled", 0) != 1) {
            return@withContext Probe.Fail(
                BackendStatus.NOT_RUNNING,
                context.getString(R.string.recording_error_wd_off)
            )
        }
        val selection = selectEndpoint()
        val endpoint = selection.endpoint
            ?: return@withContext Probe.Fail(
                BackendStatus.ERROR,
                portNotFoundMessage(selection.attempts)
            )
        val (host, port) = endpoint
        var last: Throwable? = null
        try {
            val ok = withTimeout(10_000) {
                Kadb.create(host, port, connectTimeout = 10_000, socketTimeout = 10_000).use { kadb ->
                    val response = kadb.shell("id")
                    if (response.exitCode == 0) {
                        if (!connectionTimeApplied) {
                            runCatching { kadb.shell("settings put global adb_allowed_connection_time 0") }
                            connectionTimeApplied = true
                        }
                        true
                    } else {
                        last = IllegalStateException(
                            response.output.ifBlank { response.errorOutput }.ifBlank { "shell id failed" }
                        )
                        false
                    }
                }
            }
            if (ok) {
                // Healthy Wi-Fi probe with the switch on and the offline
                // port still closed: ask adbd to listen locally, then use it.
                if (offlineMode && !reachable(OFFLINE_ADB_HOST, OFFLINE_ADB_PORT)) {
                    val note = enableOfflinePort(host, port)
                    if (note == null) {
                        return@withContext Probe.Ok(OFFLINE_ADB_HOST, OFFLINE_ADB_PORT, offline = true)
                    }
                    return@withContext Probe.Ok(host, port, offlineNote = note)
                }
                return@withContext Probe.Ok(host, port)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            last = e
        }
        val message = last?.message.orEmpty()
        val lower = message.lowercase()
        if (lower.contains("auth") || lower.contains("unauthorized") || lower.contains("handshake")) {
            Probe.Fail(
                BackendStatus.NOT_PAIRED,
                context.getString(R.string.recording_error_key_not_accepted)
            )
        } else {
            Probe.Fail(
                BackendStatus.ERROR,
                message.ifBlank { last?.toString().orEmpty() }
            )
        }
    }

    /**
     * Tries the local offline port. Returns Ok on success,
     * Fail(NOT_PAIRED) on an auth failure, or null when the port is
     * reachable but unusable for another reason (the caller falls back
     * to the Wi-Fi path). The wireless-debugging connection-time
     * setting is not applied here: it governs wireless sessions, not
     * the plain local port.
     */
    private suspend fun tryOfflinePort(): Probe? {
        var last: Throwable? = null
        try {
            val ok = withTimeout(10_000) {
                Kadb.create(OFFLINE_ADB_HOST, OFFLINE_ADB_PORT, connectTimeout = 10_000, socketTimeout = 10_000).use { kadb ->
                    kadb.shell("id").exitCode == 0
                }
            }
            if (ok) return Probe.Ok(OFFLINE_ADB_HOST, OFFLINE_ADB_PORT, offline = true)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            last = e
        }
        val lower = last?.message.orEmpty().lowercase()
        if (lower.contains("auth") || lower.contains("unauthorized") || lower.contains("handshake")) {
            return Probe.Fail(
                BackendStatus.NOT_PAIRED,
                context.getString(R.string.recording_error_key_not_accepted)
            )
        }
        return null
    }

    /**
     * Asks adbd, over the verified Wi-Fi connection, to listen on the
     * local offline port (the tcpip service: the same request a host
     * `adb tcpip` sends), then waits up to 8 s for the port and
     * re-starts the helper through it. Returns null on success, else a
     * reason for Copy details. adbd restarts on tcpip, so a dropped
     * connection or an empty reply is expected, not an error — and if
     * the open itself failed but the port still comes up, the request
     * evidently landed, so that still counts as success.
     */
    private suspend fun enableOfflinePort(wifiHost: String, wifiPort: Int): String? =
        withContext(Dispatchers.IO) {
            var sendError: String? = null
            try {
                Kadb.create(wifiHost, wifiPort, connectTimeout = 10_000, socketTimeout = 10_000).use { kadb ->
                    kadb.open("tcpip:$OFFLINE_ADB_PORT").use { stream ->
                        runCatching { stream.source.buffer().readUtf8() }
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                sendError = e.message.orEmpty().ifBlank { e.toString() }
            }
            repeat(32) {
                delay(250)
                if (reachable(OFFLINE_ADB_HOST, OFFLINE_ADB_PORT)) {
                    return@withContext if (ensureHelperAlive(OFFLINE_ADB_HOST, OFFLINE_ADB_PORT)) {
                        null
                    } else {
                        // The helper diagnostics (already in lastError)
                        // travel with the note so they show under
                        // Recorder status instead of a bare failure.
                        val detail = _lastError.value
                        context.getString(
                            R.string.recording_error_offline_port,
                            "recorder helper did not start"
                        ) + if (detail != null) "\n$detail" else ""
                    }
                }
            }
            context.getString(
                R.string.recording_error_offline_port,
                sendError ?: "port $OFFLINE_ADB_PORT did not open"
            )
        }

    /** One verified endpoint, or null plus the full attempt list. */
    private data class Selection(
        val endpoint: Pair<String, Int>?,
        val attempts: String
    )

    /** Quick TCP check: true when something actually listens on host:port. */
    private fun reachable(host: String, port: Int): Boolean =
        tcpOutcome(host, port) == null

    /** Null when reachable, else a short reason (refused/timeout/failed). */
    private fun tcpOutcome(host: String, port: Int): String? = runCatching {
        Socket().use {
            it.connect(InetSocketAddress(host, port), 1500)
        }
        null
    }.getOrElse { e ->
        when (e) {
            is java.net.ConnectException -> "refused"
            is java.net.SocketTimeoutException -> "timeout"
            else -> "failed"
        }
    }

    /** Wireless debugging's current TLS port, best-effort: 0/blank/absent means none. */
    private fun getpropPort(): Int? = runCatching {
        val process = ProcessBuilder("getprop", "service.adb.tls.port").start()
        val text = process.inputStream.bufferedReader().readText().trim()
        process.waitFor()
        text.toIntOrNull()?.takeIf { it != 0 }
    }.getOrNull()

    /**
     * Endpoint selection in priority order: manual port, the port
     * wireless debugging reports via getprop, NSD services newest-first,
     * then the last known working port. Every candidate is TCP-checked
     * (loopback first, then NSD hosts) before Kadb ever sees it, so a
     * stale port fails fast here instead of surfacing as a refused
     * Kadb connection. The manual port always wins over NSD when it
     * answers.
     */
    private suspend fun selectEndpoint(): Selection {
        val settings = settingsRepository.settingsFlow.first()
        val manual = settings.adbManualPort
        val prop = getpropPort()
        val nsdServices = AdbDiscovery.discoverConnectAll(context)
        val nsdHosts = nsdServices.map { it.first }.asReversed().distinct()
        val lastKnown = settings.adbLastKnownPort
        val parts = mutableListOf<String>()
        if (manual == null) parts.add("manual (not set)")
        if (prop == null) parts.add("getprop (none)")
        if (nsdServices.isEmpty()) parts.add("NSD (none found)")
        if (lastKnown == null) parts.add("last-known (none)")

        // Ordered unique ports; first occurrence wins (priority order).
        val ports = mutableListOf<Pair<String, Int>>()
        if (manual != null) ports.add("manual" to manual)
        if (prop != null) ports.add("getprop" to prop)
        for ((_, port) in nsdServices.asReversed()) ports.add("NSD" to port)
        if (lastKnown != null) ports.add("last-known" to lastKnown)
        for ((source, port) in ports.distinctBy { it.second }) {
            val hostAttempts = mutableListOf<String>()
            for (host in listOf("127.0.0.1") + nsdHosts) {
                val outcome = tcpOutcome(host, port)
                if (outcome == null) {
                    return Selection(host to port, parts.joinToString(", "))
                }
                hostAttempts.add("@$host ($outcome)")
            }
            parts.add("$source $port ${hostAttempts.joinToString(", ")}")
        }
        return Selection(null, parts.joinToString(", "))
    }

    private fun portNotFoundMessage(attempts: String): String =
        "tried: $attempts\n${context.getString(R.string.recording_error_port_not_found)}"

    /**
     * Runs endpoint selection only (no pairing, no Kadb): the port the
     * Detect button fills into the manual-port field.
     */
    suspend fun detectPort(): Result<Int> = withContext(Dispatchers.IO) {
        val selection = selectEndpoint()
        selection.endpoint?.let { Result.success(it.second) }
            ?: Result.failure(IllegalStateException(portNotFoundMessage(selection.attempts)))
    }

    private fun isWifi(): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        return manager.allNetworks.any { network ->
            manager.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        }
    }

    /**
     * Makes sure the recorder helper answers on loopback TCP. First
     * reuses the stored port/token (an already running helper survives
     * app restarts); otherwise kills any stale helper, launches a fresh
     * one on a new random port/token over the ADB endpoint, and polls
     * STATUS up to 8 s. On failure the helper's log, its process line,
     * and the last socket error go into lastError, so Recorder status
     * and Copy details show the reason instead of a bare failure.
     */
    private suspend fun ensureHelperAlive(host: String, port: Int): Boolean {
        val stored = settingsRepository.settingsFlow.first()
        val storedPort = stored.adbHelperPort
        val storedToken = stored.adbHelperToken
        if (storedPort != null && storedToken != null &&
            helperStatus(storedPort, storedToken) != null
        ) {
            _lastError.value = null
            return true
        }
        runCatching {
            withContext(Dispatchers.IO) {
                kadbShell(host, port, "pkill -f phone_recorder")
            }
        }
        val helperPort = Random.nextInt(20000, 60001)
        val helperToken = randomHexToken()
        settingsRepository.setAdbHelperPort(helperPort)
        settingsRepository.setAdbHelperToken(helperToken)
        val hasSetsid = runCatching {
            withContext(Dispatchers.IO) {
                kadbShell(host, port, "command -v setsid").isNotBlank()
            }
        }.getOrDefault(false)
        val sourceDir = context.applicationInfo.sourceDir
        // nohup + setsid detach the helper from the ADB session so it
        // survives wireless debugging being switched off afterwards.
        // Output goes to a file, not /dev/null, so a helper that never
        // answers can be diagnosed from its own stderr.
        val command = "CLASSPATH=$sourceDir " +
            (if (hasSetsid) "nohup setsid " else "nohup ") +
            "app_process /system/bin --nice-name=phone_recorder " +
            "io.github.elli0t0.phone.core.recording.adb.RecordingHelperMainKt " +
            "$helperPort $helperToken >$HELPER_LOG_PATH 2>&1 &"
        runCatching {
            withContext(Dispatchers.IO) {
                kadbShell(host, port, command)
            }
        }
        repeat(32) {
            delay(250)
            if (helperStatus(helperPort, helperToken) != null) {
                _lastError.value = null
                return true
            }
        }
        _lastError.value = helperDiagnostics(host, port)
        return false
    }

    /** STATUS reply ("IDLE"/"RECORDING"), or null when the helper does not answer. */
    private suspend fun helperStatus(port: Int, token: String): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val socket = Socket()
                try {
                    socket.connect(InetSocketAddress(OFFLINE_ADB_HOST, port), 1500)
                    socket.soTimeout = 5000
                    socket.getOutputStream().write(
                        "$token\nSTATUS\n".toByteArray(Charsets.US_ASCII)
                    )
                    socket.getOutputStream().flush()
                    readLine(socket.getInputStream()).takeIf { it.isNotBlank() }
                } finally {
                    runCatching { socket.close() }
                }
            }.onFailure { e ->
                lastSocketError = e.message?.takeIf { it.isNotBlank() } ?: e.toString()
            }.getOrNull()
        }

    /** One Kadb shell command's stdout (stderr when stdout is blank). */
    private fun kadbShell(host: String, port: Int, command: String): String {
        val response = Kadb.create(host, port, connectTimeout = 10_000, socketTimeout = 10_000).use { kadb ->
            kadb.shell(command)
        }
        return response.output.ifBlank { response.errorOutput }
    }

    /**
     * Helper diagnostics for lastError: the helper's log file plus its
     * process line, read back over Kadb, truncated to 1500 chars, plus
     * the message from the failed socket connect.
     */
    private suspend fun helperDiagnostics(host: String, port: Int): String =
        withContext(Dispatchers.IO) {
            val raw = runCatching {
                kadbShell(
                    host,
                    port,
                    "cat $HELPER_LOG_PATH; echo ---; ps -A | grep phone_recorder"
                )
            }.getOrElse { e ->
                "could not read helper log: ${e.message ?: e.toString()}"
            }
            val text = raw.take(1500)
            val socketError = lastSocketError
            if (socketError != null) "$text\n$socketError" else text
        }

    /** 24 random bytes rendered as hex (48 chars), per helper launch. */
    private fun randomHexToken(): String {
        val bytes = ByteArray(24)
        Random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it.toInt() and 0xFF) }
    }

    private fun readLine(input: java.io.InputStream): String {
        val bytes = ByteArrayOutputStream()
        while (true) {
            val b = input.read()
            if (b == -1 || b == '\n'.code) break
            if (b != '\r'.code) bytes.write(b)
        }
        return String(bytes.toByteArray(), Charsets.US_ASCII)
    }
}
