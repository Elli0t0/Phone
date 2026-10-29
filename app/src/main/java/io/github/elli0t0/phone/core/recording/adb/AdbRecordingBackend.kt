package io.github.elli0t0.phone.core.recording.adb

import android.content.Context
import android.net.ConnectivityManager
import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.net.NetworkCapabilities
import android.os.ParcelFileDescriptor
import android.provider.Settings
import com.flyfishxu.kadb.Kadb
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.recording.BackendStatus
import io.github.elli0t0.phone.core.recording.CaptureResult
import io.github.elli0t0.phone.core.recording.RecordingBackend
import io.github.elli0t0.phone.core.recording.WavWriter
import io.github.elli0t0.phone.data.settings.SettingsRepository
import io.github.elli0t0.phone.data.settings.model.RecordingBackendKind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okio.buffer
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Call recording without a second app. The phone pairs with itself over
 * wireless debugging (one-time, via Kadb's SPAKE2/TLS pairing), then this
 * backend runs shell commands over that ADB connection to start Phone's
 * own recorder helper as shell, detached from the ADB session. The helper
 * writes to a MediaStore descriptor the app created, over a local socket,
 * exactly like the Shizuku path — so storage, playback, delete, and
 * share are identical. After a reboot the helper is gone; it is re-armed
 * on demand whenever wireless debugging is on and the phone is on Wi-Fi.
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

    /** True when the last successful probe used the local offline port. */
    private val _offlinePortActive = MutableStateFlow(false)
    val offlinePortActive: StateFlow<Boolean> = _offlinePortActive.asStateFlow()

    /** Endpoint of the last successful probe; reused for the helper bootstrap. */
    private var workingEndpoint: Pair<String, Int>? = null

    /** adb_allowed_connection_time is set once per process, best-effort. */
    private var connectionTimeApplied = false

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
                    _lastError.value = probe.offlineNote
                }
            }
            is Probe.Fail -> {
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
        if (runCatching { helperCommand("STATUS") }.isSuccess) return true
        val stored = workingEndpoint
        if (stored != null) {
            // Reuse the verified endpoint; no second discovery while
            // it still answers — a fresh discovery could return a
            // different stale service.
            val stillAnswers = withContext(Dispatchers.IO) {
                reachable(stored.first, stored.second)
            }
            if (stillAnswers) return ensureHelperAlive(stored.first, stored.second)
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
        val endpoint = AdbDiscovery.discoverPairing(context, 30_000).getOrThrow()
        // Pair as "Phone": Kadb's default name is the OS build's
        // user@host, which shows up as a cryptic entry in Paired devices.
        Kadb.pair(endpoint.first, endpoint.second, code, "Phone")
        refreshStatus()
        Unit
    }

    override suspend fun resetPairing() {
        runCatching { kadbKeyFile(context).delete() }
        ensureKadbIdentity(context)
    }

    override suspend fun startCapture(fd: ParcelFileDescriptor): Result<Unit> = runCatching {
        if (!ensureBound()) {
            throw IllegalStateException("Recorder helper is not running")
        }
        // A helper orphaned by an app restart still holds a capture;
        // stop it first so this session owns the file.
        if (runCatching { helperCommand("STATUS") }.getOrNull() == "RECORDING") {
            runCatching { helperCommand("STOP") }
        }
        val reply = helperCommand("START", fd)
        if (reply != "OK") {
            throw IllegalStateException(reply.removePrefix("ERR ").ifBlank { "Recorder refused to start" })
        }
        Unit
    }

    override suspend fun stopCapture(): Result<CaptureResult> = runCatching {
        val reply = helperCommand("STOP")
        val bytes = reply.removePrefix("OK ").trim().toLongOrNull()
            ?: throw IllegalStateException(reply.ifBlank { "Recorder did not stop" })
        CaptureResult(bytes, WavWriter.durationMs(bytes))
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
                        context.getString(
                            R.string.recording_error_offline_port,
                            "recorder helper did not start"
                        )
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

    private suspend fun ensureHelperAlive(host: String, port: Int): Boolean {
        if (runCatching { helperCommand("STATUS") }.isSuccess) return true
        val hasSetsid = runCatching {
            withContext(Dispatchers.IO) {
                Kadb.create(host, port, connectTimeout = 10_000, socketTimeout = 10_000).use {
                    it.shell("command -v setsid").output.isNotBlank()
                }
            }
        }.getOrDefault(false)
        val sourceDir = context.applicationInfo.sourceDir
        // nohup + setsid detach the helper from the ADB session so it
        // survives wireless debugging being switched off afterwards.
        val command = "CLASSPATH=$sourceDir " +
            (if (hasSetsid) "nohup setsid " else "nohup ") +
            "app_process /system/bin --nice-name=phone_recorder " +
            "io.github.elli0t0.phone.core.recording.adb.RecordingHelperMainKt >/dev/null 2>&1 &"
        runCatching {
            withContext(Dispatchers.IO) {
                Kadb.create(host, port, connectTimeout = 10_000, socketTimeout = 10_000).use { kadb ->
                    kadb.shell(command)
                }
            }
        }
        repeat(32) {
            delay(250)
            if (runCatching { helperCommand("STATUS") }.isSuccess) return true
        }
        return false
    }

    /**
     * One command to the helper over its local socket. For START the
     * app's file descriptor travels with the command (ancillary data),
     * so the helper writes straight into the app's MediaStore file.
     */
    private fun helperCommand(command: String, fd: ParcelFileDescriptor? = null): String {
        val socket = LocalSocket()
        try {
            socket.connect(
                LocalSocketAddress(ADB_RECORDING_SOCKET, LocalSocketAddress.Namespace.ABSTRACT)
            )
            socket.soTimeout = 15_000
            if (fd != null) {
                socket.setFileDescriptorsForSend(arrayOf(fd.fileDescriptor))
            }
            socket.outputStream.write("$command\n".toByteArray(Charsets.US_ASCII))
            socket.outputStream.flush()
            return readLine(socket.inputStream)
        } finally {
            runCatching { socket.close() }
        }
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
