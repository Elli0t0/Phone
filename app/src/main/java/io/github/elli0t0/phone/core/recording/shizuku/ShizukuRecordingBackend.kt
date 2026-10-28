package io.github.elli0t0.phone.core.recording.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import io.github.elli0t0.phone.BuildConfig
import io.github.elli0t0.phone.core.recording.BackendStatus
import io.github.elli0t0.phone.core.recording.CaptureResult
import io.github.elli0t0.phone.core.recording.RecordingBackend
import io.github.elli0t0.phone.core.recording.WavWriter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku
import kotlin.coroutines.resume

/**
 * Shizuku backend: binds Phone's own UserService into Shizuku's
 * shell-privileged process and drives it over AIDL. The app process
 * never touches call audio itself. The service stays bound for the
 * app's lifetime once bound; unbinding would leave the privileged
 * process idling anyway, so there is nothing to gain from it.
 */
class ShizukuRecordingBackend(private val context: Context) : RecordingBackend {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _status = MutableStateFlow(BackendStatus.NOT_RUNNING)
    override val status: StateFlow<BackendStatus> = _status.asStateFlow()

    private var service: IRecordingService? = null
    private var bound = false
    private val bindMutex = Mutex()
    private var bindDeferred: CompletableDeferred<Boolean>? = null
    private var bindStarted = false

    private val args = Shizuku.UserServiceArgs(
        ComponentName(context, RecordingUserService::class.java)
    )
        .daemon(false)
        .processNameSuffix("recording")
        .debuggable(BuildConfig.DEBUG)
        .version(SERVICE_VERSION)
        // Explicit tag: the class name is unstable under R8, and the
        // tag is what Shizuku uses to recognize the same service.
        .tag(SERVICE_TAG)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = binder?.let { IRecordingService.Stub.asInterface(it) }
            bound = true
            bindDeferred?.complete(true)
            bindDeferred = null
            bindStarted = false
            scope.launch { refreshStatus() }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            bound = false
            bindDeferred?.complete(false)
            bindDeferred = null
            bindStarted = false
            _status.value = BackendStatus.NOT_RUNNING
        }
    }

    init {
        Shizuku.addBinderReceivedListenerSticky { scope.launch { refreshStatus() } }
        Shizuku.addBinderDeadListener { _status.value = BackendStatus.NOT_RUNNING }
        scope.launch { refreshStatus() }
    }

    override suspend fun refreshStatus() {
        val next = runCatching {
            if (Shizuku.isPreV11()) {
                BackendStatus.ERROR
            } else if (!Shizuku.pingBinder()) {
                if (isShizukuInstalled()) BackendStatus.NOT_RUNNING else BackendStatus.NOT_INSTALLED
            } else if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                BackendStatus.PERMISSION_NEEDED
            } else {
                BackendStatus.READY
            }
        }.getOrElse { BackendStatus.NOT_RUNNING }
        _status.value = next
    }

    private fun isShizukuInstalled(): Boolean = runCatching {
        context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0) != null
    }.getOrDefault(false)

    override suspend fun requestPermission(): Boolean {
        if (runCatching { Shizuku.pingBinder() }.getOrDefault(false) &&
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return suspendCancellableCoroutine { continuation ->
            lateinit var listener: Shizuku.OnRequestPermissionResultListener
            listener = Shizuku.OnRequestPermissionResultListener { code, result ->
                if (code == PERMISSION_REQUEST_CODE) {
                    Shizuku.removeRequestPermissionResultListener(listener)
                    if (continuation.isActive) {
                        continuation.resume(result == PackageManager.PERMISSION_GRANTED)
                    }
                }
            }
            Shizuku.addRequestPermissionResultListener(listener)
            continuation.invokeOnCancellation {
                Shizuku.removeRequestPermissionResultListener(listener)
            }
            runCatching { Shizuku.requestPermission(PERMISSION_REQUEST_CODE) }
                .onFailure {
                    Shizuku.removeRequestPermissionResultListener(listener)
                    if (continuation.isActive) continuation.resume(false)
                }
        }
    }

    override suspend fun ensureBound(): Boolean = bindMutex.withLock {
        if (bound && service != null) return@withLock true
        if (runCatching { Shizuku.getVersion() }.getOrDefault(0) < 10) return@withLock false
        val deferred = bindDeferred ?: CompletableDeferred<Boolean>().also { bindDeferred = it }
        if (!bindStarted) {
            bindStarted = true
            val ok = runCatching { Shizuku.bindUserService(args, connection) }.isSuccess
            if (!ok) {
                bindStarted = false
                bindDeferred = null
                deferred.complete(false)
            }
        }
        // On timeout the callback still lands later and marks the
        // service bound, so the next call succeeds immediately.
        withTimeoutOrNull(BIND_TIMEOUT_MS) { deferred.await() } ?: false
    }

    override suspend fun startCapture(fd: android.os.ParcelFileDescriptor): Result<Unit> {
        if (!ensureBound()) {
            return Result.failure(IllegalStateException("Recorder service is not bound"))
        }
        val svc = service ?: return Result.failure(
            IllegalStateException("Recorder service is not bound")
        )
        return runCatching {
            val code = withContext(Dispatchers.IO) { svc.startRecording(fd) }
            if (code != 0) throw IllegalStateException("Recorder failed to start (code $code)")
        }
    }

    override suspend fun stopCapture(): Result<CaptureResult> {
        val svc = service ?: return Result.failure(
            IllegalStateException("Recorder service is not bound")
        )
        return runCatching {
            val bytes = withContext(Dispatchers.IO) { svc.stopRecording() }
            if (bytes < 0) throw IllegalStateException("Nothing was recording")
            CaptureResult(bytes, WavWriter.durationMs(bytes))
        }
    }

    companion object {
        private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        private const val SERVICE_TAG = "phone-recording"
        private const val SERVICE_VERSION = 1
        private const val PERMISSION_REQUEST_CODE = 1001
        private const val BIND_TIMEOUT_MS = 15_000L
    }
}
