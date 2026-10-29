package io.github.elli0t0.phone.feature.recording

import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.elli0t0.phone.core.recording.BackendStatus
import io.github.elli0t0.phone.core.recording.PairResult
import io.github.elli0t0.phone.core.recording.RecordingController
import io.github.elli0t0.phone.core.recording.RecordingSession
import io.github.elli0t0.phone.core.recording.adb.openWirelessDebugging
import io.github.elli0t0.phone.core.recording.adb.postPairNotification
import io.github.elli0t0.phone.data.recordings.RecordingEntity
import io.github.elli0t0.phone.data.recordings.RecordingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RecordingTestOutcome(
    val bytes: Long,
    val durationMs: Long,
    val error: String?,
    /** Peak 0..1 for the ADB mic test; null when unmeasured. */
    val peak: Float? = null
)

class RecordingSettingsViewModel(
    private val controller: RecordingController,
    private val recordingsRepository: RecordingsRepository,
    private val appContext: Context
) : ViewModel() {

    val backendStatus: StateFlow<BackendStatus> = controller.backendStatus
    val backendDiagnostic: StateFlow<String?> = controller.backendDiagnostic
    val captureDiagnostic: StateFlow<String?> = controller.lastCaptureDiagnostic
    val pairResult: StateFlow<PairResult?> = controller.pairResult
    val manualPort: StateFlow<Int?> = controller.manualPort
    val detectResult: StateFlow<String?> = controller.detectResult
    val offlineMode: StateFlow<Boolean> = controller.offlineMode
    val offlinePortActive: StateFlow<Boolean> = controller.offlinePortActive
    val keyAccepted: StateFlow<Boolean> = controller.adbKeyAccepted
    val session: StateFlow<RecordingSession> = controller.state
    val lastError: StateFlow<String?> = controller.lastError
    val recordings: StateFlow<List<RecordingEntity>> = recordingsRepository.recordings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()
    private val _testOutcome = MutableStateFlow<RecordingTestOutcome?>(null)
    val testOutcome: StateFlow<RecordingTestOutcome?> = _testOutcome.asStateFlow()
    private val _testing = MutableStateFlow(false)
    val testing: StateFlow<Boolean> = _testing.asStateFlow()
    private val _playError = MutableStateFlow<String?>(null)
    val playError: StateFlow<String?> = _playError.asStateFlow()

    private var player: MediaPlayer? = null
    private val _playingId = MutableStateFlow<Long?>(null)
    val playingId: StateFlow<Long?> = _playingId.asStateFlow()
    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()
    private var ticker: Job? = null

    private var refreshJob: Job? = null

    /** Re-probes when Wi-Fi appears or vanishes. */
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            refresh()
        }

        override fun onLost(network: Network) {
            refresh()
        }
    }

    /** Re-probes when wireless debugging is toggled in system Settings. */
    private val wifiSettingObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            refresh()
        }
    }

    init {
        runCatching {
            val manager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            manager?.registerNetworkCallback(
                NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .build(),
                networkCallback
            )
        }
        runCatching {
            appContext.contentResolver.registerContentObserver(
                Settings.Global.getUriFor("adb_wifi_enabled"),
                false,
                wifiSettingObserver
            )
        }
    }

    fun refresh() {
        // A fresh trigger cancels a stale NSD discovery instead of
        // stacking probes when several triggers fire together.
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch { controller.refreshBackendStatus() }
    }

    fun setManualPort(port: Int?) {
        viewModelScope.launch { controller.setManualPort(port) }
    }

    /** Flipping the switch re-probes at once, so enabling runs the offline step. */
    fun setOfflineMode(enabled: Boolean) {
        viewModelScope.launch {
            controller.setOfflineMode(enabled)
            refresh()
        }
    }

    fun detectPort() {
        viewModelScope.launch {
            _busy.value = true
            controller.detectPort()
            _busy.value = false
        }
    }

    fun resetPairing() {
        viewModelScope.launch {
            _busy.value = true
            controller.resetPairing()
            _busy.value = false
        }
    }

    /** Posts the pairing notification and opens Developer options. */
    fun startPairing() {
        viewModelScope.launch {
            postPairNotification(appContext)
            openWirelessDebugging(appContext)
        }
    }

    fun runTest() {
        viewModelScope.launch {
            _busy.value = true
            _testing.value = true
            _testOutcome.value = null
            val result = controller.runPipelineTest()
            _testOutcome.value = result.fold(
                onSuccess = {
                    RecordingTestOutcome(it.pcmBytes, it.durationMs, null, it.peak)
                },
                onFailure = { RecordingTestOutcome(0, 0, it.message, null) }
            )
            _testing.value = false
            _busy.value = false
        }
    }

    fun stopActive() {
        viewModelScope.launch { controller.stopRecording() }
    }

    fun togglePlay(entity: RecordingEntity) {
        if (_playingId.value == entity.id) {
            stopPlayback()
        } else {
            play(entity)
        }
    }

    private fun play(entity: RecordingEntity) {
        stopPlayback()
        val mp = MediaPlayer()
        try {
            mp.setDataSource(appContext, Uri.parse(entity.uri))
            mp.prepare()
            mp.start()
            mp.setOnCompletionListener { stopPlayback() }
            player = mp
            _playingId.value = entity.id
            _positionMs.value = 0
            _playError.value = null
            ticker = viewModelScope.launch {
                while (true) {
                    delay(500)
                    _positionMs.value = runCatching {
                        player?.currentPosition?.toLong() ?: 0
                    }.getOrDefault(0)
                }
            }
        } catch (e: Exception) {
            runCatching { mp.release() }
            _playingId.value = null
            _playError.value = e.message
        }
    }

    fun seekTo(ms: Int) {
        runCatching { player?.seekTo(ms) }
        _positionMs.value = ms.toLong()
    }

    private fun stopPlayback() {
        ticker?.cancel()
        ticker = null
        runCatching {
            player?.stop()
            player?.release()
        }
        player = null
        _playingId.value = null
        _positionMs.value = 0
    }

    fun delete(entity: RecordingEntity) {
        if (_playingId.value == entity.id) stopPlayback()
        viewModelScope.launch { recordingsRepository.remove(entity) }
    }

    fun share(entity: RecordingEntity, context: Context) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/x-wav"
            putExtra(Intent.EXTRA_STREAM, Uri.parse(entity.uri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, null))
    }

    override fun onCleared() {
        stopPlayback()
        runCatching {
            val manager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            manager?.unregisterNetworkCallback(networkCallback)
        }
        runCatching {
            appContext.contentResolver.unregisterContentObserver(wifiSettingObserver)
        }
        super.onCleared()
    }

    companion object {
        fun provideFactory(
            controller: RecordingController,
            repository: RecordingsRepository,
            appContext: Context
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RecordingSettingsViewModel(controller, repository, appContext) as T
            }
        }
    }
}
