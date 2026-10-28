package io.github.elli0t0.phone.feature.recording

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.elli0t0.phone.core.recording.BackendStatus
import io.github.elli0t0.phone.core.recording.RecordingController
import io.github.elli0t0.phone.core.recording.RecordingSession
import io.github.elli0t0.phone.core.recording.adb.openWirelessDebugging
import io.github.elli0t0.phone.core.recording.adb.postPairNotification
import io.github.elli0t0.phone.data.settings.model.RecordingBackendKind
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

data class RecordingTestOutcome(val bytes: Long, val durationMs: Long, val error: String?)

class RecordingSettingsViewModel(
    private val controller: RecordingController,
    private val recordingsRepository: RecordingsRepository,
    private val appContext: Context
) : ViewModel() {

    val backendStatus: StateFlow<BackendStatus> = controller.backendStatus
    val backendKind: StateFlow<RecordingBackendKind> = controller.activeBackendKind
    val session: StateFlow<RecordingSession> = controller.state
    val lastError: StateFlow<String?> = controller.lastError
    val recordings: StateFlow<List<RecordingEntity>> = recordingsRepository.recordings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()
    private val _testOutcome = MutableStateFlow<RecordingTestOutcome?>(null)
    val testOutcome: StateFlow<RecordingTestOutcome?> = _testOutcome.asStateFlow()
    private val _playError = MutableStateFlow<String?>(null)
    val playError: StateFlow<String?> = _playError.asStateFlow()

    private var player: MediaPlayer? = null
    private val _playingId = MutableStateFlow<Long?>(null)
    val playingId: StateFlow<Long?> = _playingId.asStateFlow()
    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()
    private var ticker: Job? = null

    fun refresh() {
        viewModelScope.launch { controller.refreshBackendStatus() }
    }

    fun setBackend(kind: RecordingBackendKind) {
        viewModelScope.launch { controller.setBackend(kind) }
    }

    /** Posts the pairing notification and opens Developer options. */
    fun startPairing() {
        viewModelScope.launch {
            postPairNotification(appContext)
        }
    }

    fun openDevSettings() {
        openWirelessDebugging(appContext)
    }

    fun requestPermission() {
        viewModelScope.launch {
            _busy.value = true
            controller.requestBackendPermission()
            _busy.value = false
        }
    }

    fun runTest() {
        viewModelScope.launch {
            _busy.value = true
            _testOutcome.value = null
            val result = controller.runPipelineTest()
            _testOutcome.value = result.fold(
                onSuccess = { RecordingTestOutcome(it.pcmBytes, it.durationMs, null) },
                onFailure = { RecordingTestOutcome(0, 0, it.message) }
            )
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
