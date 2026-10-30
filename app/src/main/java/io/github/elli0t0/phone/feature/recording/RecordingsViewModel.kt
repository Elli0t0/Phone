package io.github.elli0t0.phone.feature.recording

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.data.recordings.RecordingEntity
import io.github.elli0t0.phone.data.recordings.RecordingsRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class RecordingDateFilter {
    ALL, TODAY, WEEK, MONTH;

    fun labelRes(): Int = when (this) {
        ALL -> R.string.recordings_filter_all
        TODAY -> R.string.recordings_filter_today
        WEEK -> R.string.recordings_filter_week
        MONTH -> R.string.recordings_filter_month
    }
}

data class RecordingDay(val key: Long, val title: String, val items: List<RecordingEntity>)

/**
 * Recordings library, separate from Call recording settings: the
 * settings page keeps its own list and playback, this VM owns the
 * standalone screen's search, day filter, grouping and playback.
 * Playback is copied from RecordingSettingsViewModel on purpose so
 * the settings page stays untouched.
 */
class RecordingsViewModel(
    private val repo: RecordingsRepository,
    private val appContext: Context
) : ViewModel() {
    init {
        viewModelScope.launch(Dispatchers.IO) { runCatching { repo.removeMissingFiles() } }
    }

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(RecordingDateFilter.ALL)
    val searchQuery: StateFlow<String> = query
    val dateFilter: StateFlow<RecordingDateFilter> = filter

    val days: StateFlow<List<RecordingDay>> = combine(repo.recordings, query, filter) { all, q, f ->
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val startOfToday = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val from = when (f) {
            RecordingDateFilter.ALL -> 0L
            RecordingDateFilter.TODAY -> startOfToday
            RecordingDateFilter.WEEK -> now - 7L * 86_400_000L
            RecordingDateFilter.MONTH -> now - 30L * 86_400_000L
        }
        val needle = q.trim()
        all.asSequence()
            .filter { !it.isTest && it.startedAt >= from }
            .filter {
                needle.isEmpty() ||
                    (it.displayName ?: "").contains(needle, ignoreCase = true) ||
                    it.number.contains(needle)
            }
            .groupBy { Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() }
            .toSortedMap(compareByDescending { it })
            .map { (date, items) ->
                RecordingDay(date.toEpochDay(), dayTitle(date), items.sortedByDescending { it.startedAt })
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchChanged(q: String) {
        query.value = q
    }

    fun onFilterChanged(f: RecordingDateFilter) {
        filter.value = f
    }

    private fun dayTitle(date: LocalDate): String {
        val today = LocalDate.now(ZoneId.systemDefault())
        return when (date) {
            today -> appContext.getString(R.string.history_day_today)
            today.minusDays(1) -> appContext.getString(R.string.history_day_yesterday)
            else -> DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault()).format(date)
        }
    }

    private val _playError = MutableStateFlow<String?>(null)
    val playError: StateFlow<String?> = _playError.asStateFlow()

    private var player: MediaPlayer? = null
    private val _playingId = MutableStateFlow<Long?>(null)
    val playingId: StateFlow<Long?> = _playingId.asStateFlow()
    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()
    private var ticker: Job? = null

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
        viewModelScope.launch { repo.remove(entity) }
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
            repository: RecordingsRepository,
            appContext: Context
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RecordingsViewModel(repository, appContext) as T
            }
        }
    }
}
