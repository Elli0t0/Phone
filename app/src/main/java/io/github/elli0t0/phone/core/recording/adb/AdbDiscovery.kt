package io.github.elli0t0.phone.core.recording.adb

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** Abstract-namespace socket the shell-side recorder helper listens on. */
internal const val ADB_RECORDING_SOCKET = "phone-recording"

private const val PAIRING_SERVICE_TYPE = "_adb-tls-pairing._tcp"
private const val CONNECT_SERVICE_TYPE = "_adb-tls-connect._tcp"

/**
 * Finds the phone's own wireless-debugging services via NSD, the same
 * discovery Shizuku uses. The pairing service only exists while the
 * system "Pair device with pairing code" dialog is open; the connect
 * service exists while wireless debugging is on.
 */
object AdbDiscovery {

    suspend fun discoverPairing(
        context: Context,
        timeoutMs: Long = 15_000
    ): Result<Pair<String, Int>> =
        discover(context, PAIRING_SERVICE_TYPE, timeoutMs)

    suspend fun discoverConnect(
        context: Context,
        timeoutMs: Long = 8_000
    ): Result<Pair<String, Int>> =
        discover(context, CONNECT_SERVICE_TYPE, timeoutMs)

    private suspend fun discover(
        context: Context,
        serviceType: String,
        timeoutMs: Long
    ): Result<Pair<String, Int>> {
        val nsd = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
            ?: return Result.failure(IllegalStateException("NSD unavailable"))
        // NsdManager delivers callbacks on the main thread.
        val found = withContext(Dispatchers.Main) {
            withTimeoutOrNull(timeoutMs) {
                suspendCancellableCoroutine<Pair<String, Int>?> { cont ->
                    var done = false
                    lateinit var discoveryListener: NsdManager.DiscoveryListener
                    val resolveListener = object : NsdManager.ResolveListener {
                        override fun onServiceResolved(info: NsdServiceInfo) {
                            if (done) return
                            val host = info.host?.hostAddress
                                ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                    info.hostAddresses.firstOrNull()?.hostAddress
                                } else {
                                    null
                                }
                            if (host != null && info.port > 0) {
                                done = true
                                runCatching { nsd.stopServiceDiscovery(discoveryListener) }
                                cont.resume(host to info.port)
                            }
                        }

                        override fun onResolveFailed(
                            serviceInfo: NsdServiceInfo,
                            errorCode: Int
                        ) {
                            // Another service may still resolve; keep waiting.
                        }
                    }
                    discoveryListener = object : NsdManager.DiscoveryListener {
                        override fun onStartDiscoveryFailed(
                            serviceType: String,
                            errorCode: Int
                        ) {
                            if (!done) {
                                done = true
                                cont.resume(null)
                            }
                        }

                        override fun onStopDiscoveryFailed(
                            serviceType: String,
                            errorCode: Int
                        ) {
                            // Nothing to do; discovery is already stopping.
                        }

                        override fun onDiscoveryStarted(serviceType: String) = Unit
                        override fun onDiscoveryStopped(serviceType: String) = Unit

                        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                            if (!done) {
                                runCatching { nsd.resolveService(serviceInfo, resolveListener) }
                            }
                        }

                        override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit
                    }
                    cont.invokeOnCancellation {
                        runCatching { nsd.stopServiceDiscovery(discoveryListener) }
                    }
                    nsd.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
                }
            }
        }
        return found?.let { Result.success(it) }
            ?: Result.failure(IllegalStateException("ADB service not found: $serviceType"))
    }
}
