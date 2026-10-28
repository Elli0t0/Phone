package io.github.elli0t0.phone.core.recording.adb

import android.content.Context
import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.os.ParcelFileDescriptor
import com.flyfishxu.kadb.Kadb
import io.github.elli0t0.phone.core.recording.BackendStatus
import io.github.elli0t0.phone.core.recording.CaptureResult
import io.github.elli0t0.phone.core.recording.RecordingBackend
import io.github.elli0t0.phone.core.recording.WavWriter
import io.github.elli0t0.phone.data.settings.model.RecordingBackendKind
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
 */
class AdbRecordingBackend(private val context: Context) : RecordingBackend {

    override val kind = RecordingBackendKind.ADB

    init {
        ensureKadbIdentity(context)
    }

    private val _status = MutableStateFlow(BackendStatus.NOT_RUNNING)
    override val status: StateFlow<BackendStatus> = _status.asStateFlow()

    override suspend fun refreshStatus() {
        _status.value = probeStatus()
    }

    override suspend fun requestPermission(): Boolean {
        // ADB has no permission prompt; pairing is the gate.
        return status.value == BackendStatus.READY
    }

    override suspend fun ensureBound(): Boolean {
        val endpoint = AdbDiscovery.discoverConnect(context).getOrNull() ?: return false
        return ensureHelperAlive(endpoint.first, endpoint.second)
    }

    override suspend fun pair(code: String): Result<Unit> = runCatching {
        val endpoint = AdbDiscovery.discoverPairing(context).getOrThrow()
        // Pair as "Phone": Kadb's default name is the OS build's
        // user@host, which shows up as a cryptic entry in Paired devices.
        Kadb.pair(endpoint.first, endpoint.second, code, "Phone")
        refreshStatus()
        Unit
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

    private suspend fun probeStatus(): BackendStatus {
        val endpoint = AdbDiscovery.discoverConnect(context).getOrNull()
            ?: return BackendStatus.NOT_RUNNING
        val (host, port) = endpoint
        val shellOk = runCatching {
            Kadb.create(host, port).use { kadb -> kadb.shell("id").exitCode == 0 }
        }.getOrElse { return BackendStatus.NOT_PAIRED }
        if (!shellOk) return BackendStatus.NOT_PAIRED
        return if (ensureHelperAlive(host, port)) BackendStatus.READY else BackendStatus.ERROR
    }

    private suspend fun ensureHelperAlive(host: String, port: Int): Boolean {
        if (runCatching { helperCommand("STATUS") }.isSuccess) return true
        val sourceDir = context.applicationInfo.sourceDir
        val command = "CLASSPATH=$sourceDir app_process /system/bin " +
            "io.github.elli0t0.phone.core.recording.adb.RecordingHelperMainKt " +
            ">/dev/null 2>&1 &"
        runCatching {
            Kadb.create(host, port).use { kadb -> kadb.shell(command) }
        }
        repeat(20) {
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
