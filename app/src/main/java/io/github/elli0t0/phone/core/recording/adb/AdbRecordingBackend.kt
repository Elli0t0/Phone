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
import java.io.ByteArrayOutputStream

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

    /** Endpoint of the last successful probe; reused for the helper bootstrap. */
    private var workingEndpoint: Pair<String, Int>? = null

    /** adb_allowed_connection_time is set once per process, best-effort. */
    private var connectionTimeApplied = false

    private sealed interface Probe {
        data class Ok(val host: String, val port: Int) : Probe
        data class Fail(val status: BackendStatus, val message: String) : Probe
    }

    override suspend fun refreshStatus() {
        when (val probe = probe()) {
            is Probe.Ok -> {
                workingEndpoint = probe.host to probe.port
                if (ensureHelperAlive(probe.host, probe.port)) {
                    _status.value = BackendStatus.READY
                    _lastError.value = null
                } else {
                    _status.value = BackendStatus.ERROR
                    _lastError.value = null
                }
            }
            is Probe.Fail -> {
                workingEndpoint = null
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
        val endpoint = workingEndpoint
            ?: AdbDiscovery.discoverConnect(context, 10_000).getOrNull()
            ?: return false
        if (ensureHelperAlive(endpoint.first, endpoint.second)) return true
        if (endpoint == workingEndpoint) {
            // The probed endpoint went stale (wireless debugging was
            // toggled); discover once more before giving up.
            workingEndpoint = null
            val fresh = AdbDiscovery.discoverConnect(context, 10_000).getOrNull()
                ?: return false
            return ensureHelperAlive(fresh.first, fresh.second)
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
     * Checks, in order: Wi-Fi, the wireless-debugging setting, NSD
     * discovery, then the ADB connect (loopback first, NSD host second).
     * Only an auth failure maps to NOT_PAIRED; anything else keeps its
     * own message so the real reason is visible.
     */
    private suspend fun probe(): Probe = withContext(Dispatchers.IO) {
        if (!isWifi()) {
            return@withContext Probe.Fail(
                BackendStatus.NOT_RUNNING,
                context.getString(R.string.recording_error_no_wifi)
            )
        }
        if (Settings.Global.getInt(context.contentResolver, "adb_wifi_enabled", 0) != 1) {
            return@withContext Probe.Fail(
                BackendStatus.NOT_RUNNING,
                context.getString(R.string.recording_error_wd_off)
            )
        }
        val nsd = AdbDiscovery.discoverConnect(context, 10_000).getOrNull()
        val port = nsd?.second
            ?: settingsRepository.settingsFlow.first().adbManualPort
            ?: return@withContext Probe.Fail(
                BackendStatus.NOT_RUNNING,
                context.getString(R.string.recording_error_service_not_found)
            )
        var last: Throwable? = null
        for (host in listOfNotNull("127.0.0.1", nsd?.first).distinct()) {
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
                if (ok) return@withContext Probe.Ok(host, port)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                last = e
            }
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
