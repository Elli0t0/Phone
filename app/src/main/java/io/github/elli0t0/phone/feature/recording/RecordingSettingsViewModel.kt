package io.github.elli0t0.phone.feature.recording

import android.content.Context
import android.database.ContentObserver
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.elli0t0.phone.core.recording.BackendStatus
import io.github.elli0t0.phone.core.recording.PairResult
import io.github.elli0t0.phone.core.recording.RecordingCleanup
import io.github.elli0t0.phone.core.recording.RecordingController
import io.github.elli0t0.phone.core.recording.RecordingSession
import io.github.elli0t0.phone.core.recording.adb.openWirelessDebugging
import io.github.elli0t0.phone.core.recording.adb.postPairNotification
import io.github.elli0t0.phone.data.recordings.RecordingStats
import io.github.elli0t0.phone.data.recordings.RecordingsRepository
import io.github.elli0t0.phone.data.settings.SettingsRepository
import io.github.elli0t0.phone.data.settings.model.AutoRecordMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RecordingTestOutcome(
    val bytes: Long,
    val durationMs: Long,
    val error: String?,
    /** Peak 0..1 for the ADB mic test; null when unmeasured. */
    val peak: Float? = null
)

/** A picked retention period that still needs its delete confirm. */
data class RetentionConfirm(val days: Int, val count: Int)

class RecordingSettingsViewModel(
    private val controller: RecordingController,
    private val recordingsRepository: RecordingsRepository,
    private val appContext: Context,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val backendStatus: StateFlow<BackendStatus> = controller.backendStatus
    val backendDiagnostic: StateFlow<String?> = controller.backendDiagnostic
    val captureDiagnostic: StateFlow<String?> = controller.lastCaptureDiagnostic
    val pairResult: StateFlow<PairResult?> = controller.pairResult
    val manualPort: StateFlow<Int?> = controller.manualPort
    val detectResult: StateFlow<String?> = controller.detectResult
    val offlineMode: StateFlow<Boolean> = controller.offlineMode
    val autoRecordCalls: StateFlow<Boolean> = controller.autoRecordCalls
    val offlinePortActive: StateFlow<Boolean> = controller.offlinePortActive
    val keyAccepted: StateFlow<Boolean> = controller.adbKeyAccepted
    val session: StateFlow<RecordingSession> = controller.state
    val lastError: StateFlow<String?> = controller.lastError
    val recordingStats: StateFlow<RecordingStats> = recordingsRepository.stats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RecordingStats(0, 0))

    val retentionDays: StateFlow<Int> = settingsRepository.settingsFlow
        .map { it.recordingRetentionDays }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val autoRecordMode: StateFlow<AutoRecordMode> = settingsRepository.settingsFlow
        .map { it.autoRecordMode }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AutoRecordMode.ALL)

    val autoRecordExcluded: StateFlow<Set<String>> = settingsRepository.settingsFlow
        .map { it.autoRecordExcluded }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val _retentionConfirm = MutableStateFlow<RetentionConfirm?>(null)
    val retentionConfirm: StateFlow<RetentionConfirm?> = _retentionConfirm.asStateFlow()

    fun onRetentionPicked(days: Int) {
        viewModelScope.launch {
            if (days == 0) {
                settingsRepository.setRecordingRetentionDays(0)
                return@launch
            }
            val cutoff = System.currentTimeMillis() - days * 86_400_000L
            val count = recordingsRepository.countOlderThanUnstarred(cutoff)
            if (count > 0) {
                _retentionConfirm.value = RetentionConfirm(days, count)
            } else {
                settingsRepository.setRecordingRetentionDays(days)
            }
        }
    }

    fun confirmRetention() {
        val pending = _retentionConfirm.value ?: return
        _retentionConfirm.value = null
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.setRecordingRetentionDays(pending.days)
            runCatching {
                RecordingCleanup(settingsRepository, recordingsRepository).runIfDue(force = true)
            }
        }
    }

    fun dismissRetentionConfirm() {
        _retentionConfirm.value = null
    }

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()
    private val _testOutcome = MutableStateFlow<RecordingTestOutcome?>(null)
    val testOutcome: StateFlow<RecordingTestOutcome?> = _testOutcome.asStateFlow()
    private val _testing = MutableStateFlow(false)
    val testing: StateFlow<Boolean> = _testing.asStateFlow()

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

    fun setAutoRecordCalls(enabled: Boolean) {
        viewModelScope.launch { controller.setAutoRecordCalls(enabled) }
    }

    fun setAutoRecordMode(mode: AutoRecordMode) {
        viewModelScope.launch { settingsRepository.setAutoRecordMode(mode) }
    }

    fun addExcludedNumber(number: String) {
        val trimmed = number.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val current = settingsRepository.settingsFlow.first().autoRecordExcluded
            settingsRepository.setAutoRecordExcluded(current + trimmed)
        }
    }

    fun removeExcludedNumber(number: String) {
        viewModelScope.launch {
            val current = settingsRepository.settingsFlow.first().autoRecordExcluded
            settingsRepository.setAutoRecordExcluded(current - number)
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

    override fun onCleared() {
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
            appContext: Context,
            settingsRepository: SettingsRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RecordingSettingsViewModel(controller, repository, appContext, settingsRepository) as T
            }
        }
    }
}
