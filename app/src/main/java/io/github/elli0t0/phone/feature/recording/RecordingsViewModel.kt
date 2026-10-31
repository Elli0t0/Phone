package io.github.elli0t0.phone.feature.recording

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.common.PhoneNumberMatcher
import io.github.elli0t0.phone.core.recording.RecordingCleanup
import io.github.elli0t0.phone.data.recordings.RecordingEntity
import io.github.elli0t0.phone.data.recordings.RecordingStats
import io.github.elli0t0.phone.data.recordings.RecordingsRepository
import io.github.elli0t0.phone.data.settings.SettingsRepository
import io.github.elli0t0.phone.data.settings.model.RecordingSort
import java.io.File
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
 * Recordings library: search, day filter, sort, selection, star,
 * rename/note and playback. The Call recording settings page links
 * here instead of keeping its own list.
 */
class RecordingsViewModel(
    private val repo: RecordingsRepository,
    private val settingsRepository: SettingsRepository,
    private val appContext: Context
) : ViewModel() {
    init {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.removeMissingFiles() }
            runCatching {
                RecordingCleanup(settingsRepository, repo).runIfDue()
            }
        }
    }

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(RecordingDateFilter.ALL)
    val searchQuery: StateFlow<String> = query
    val dateFilter: StateFlow<RecordingDateFilter> = filter

    val sort: StateFlow<RecordingSort> = settingsRepository.settingsFlow
        .map { it.recordingsSort }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RecordingSort.NEWEST)

    fun setSort(sort: RecordingSort) {
        viewModelScope.launch { settingsRepository.setRecordingsSort(sort) }
    }

    private val filtered: StateFlow<List<RecordingEntity>> = combine(repo.recordings, query, filter) { all, q, f ->
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
        all.filter { !it.isTest && it.startedAt >= from }
            .filter {
                needle.isEmpty() ||
                    (it.displayName ?: "").contains(needle, ignoreCase = true) ||
                    (it.title ?: "").contains(needle, ignoreCase = true) ||
                    (it.note ?: "").contains(needle, ignoreCase = true) ||
                    // Number search ignores spaces, + and formatting:
                    // "13 3322" matches +981333223782 (same matcher
                    // as Recents and contact search).
                    PhoneNumberMatcher.matchesQuery(it.number, needle)
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val days: StateFlow<List<RecordingDay>> = combine(filtered, sort) { items, s ->
        when (s) {
            RecordingSort.NEWEST -> items.groupDays(ascending = false)
            RecordingSort.OLDEST -> items.groupDays(ascending = true)
            else -> emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val flatItems: StateFlow<List<RecordingEntity>> = combine(filtered, sort) { items, s ->
        when (s) {
            RecordingSort.LONGEST -> items.sortedByDescending { it.durationMs }
            RecordingSort.LARGEST -> items.sortedByDescending { it.sizeBytes }
            RecordingSort.STARRED_FIRST -> items.sortedWith(
                compareByDescending<RecordingEntity> { it.starred }.thenByDescending { it.startedAt }
            )
            else -> emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<RecordingStats> = repo.stats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RecordingStats(0, 0))

    fun onSearchChanged(q: String) {
        query.value = q
        clearSelection()
    }

    fun onFilterChanged(f: RecordingDateFilter) {
        filter.value = f
        clearSelection()
    }

    private fun List<RecordingEntity>.groupDays(ascending: Boolean): List<RecordingDay> {
        val zone = ZoneId.systemDefault()
        val grouped = groupBy { Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() }
        val sorted = if (ascending) {
            grouped.toSortedMap(compareBy { it })
        } else {
            grouped.toSortedMap(compareByDescending { it })
        }
        return sorted.map { (date, items) ->
            RecordingDay(
                date.toEpochDay(),
                dayTitle(date),
                if (ascending) items.sortedBy { it.startedAt } else items.sortedByDescending { it.startedAt }
            )
        }
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

    private val speeds = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)
    private val _speed = MutableStateFlow(1f)
    val speed: StateFlow<Float> = _speed.asStateFlow()

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
            player = mp
            mp.setOnCompletionListener { stopPlayback() }
            mp.start()
            applySpeed()
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
            player = null
            _playingId.value = null
            _playError.value = e.message
        }
    }

    fun seekTo(ms: Int) {
        runCatching { player?.seekTo(ms) }
        _positionMs.value = ms.toLong()
    }

    fun skip(deltaMs: Int) {
        val p = player ?: return
        val d = p.duration
        if (d <= 0) return
        seekTo((p.currentPosition + deltaMs).coerceIn(0, d))
    }

    fun cycleSpeed() {
        val next = speeds[(speeds.indexOf(_speed.value) + 1) % speeds.size]
        _speed.value = next
        applySpeed()
    }

    private fun applySpeed() {
        val p = player ?: return
        if (p.isPlaying) runCatching { p.playbackParams = p.playbackParams.setSpeed(_speed.value) }
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

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    fun toggleSelect(id: Long) {
        _selectedIds.value = if (id in _selectedIds.value) {
            _selectedIds.value - id
        } else {
            _selectedIds.value + id
        }
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun selectAll(ids: Collection<Long>) {
        _selectedIds.value = ids.toSet()
    }

    fun deleteSelected() {
        val visible = (days.value.flatMap { it.items } + flatItems.value).map { it.id }.toSet()
        val ids = _selectedIds.value.intersect(visible)
        if (ids.isEmpty()) return
        val playing = _playingId.value
        if (playing != null && playing in ids) stopPlayback()
        viewModelScope.launch {
            val entities = repo.recordings.first().filter { it.id in ids }
            repo.removeAll(entities)
            _selectedIds.value = emptySet()
        }
    }

    fun toggleStarred(entity: RecordingEntity) {
        viewModelScope.launch { repo.setStarred(entity.id, !entity.starred) }
    }

    fun saveTitleNote(entity: RecordingEntity, title: String, note: String) {
        viewModelScope.launch {
            repo.setTitleNote(entity.id, title.ifBlank { null }, note.ifBlank { null })
        }
    }

    fun share(entity: RecordingEntity, context: Context) {
        val rawUri = Uri.parse(entity.uri)
        // Private recordings are file://: other apps cannot open
        // them directly, so share through the FileProvider instead.
        // The grant flag below gives the receiver read access.
        val shareUri = if (rawUri.scheme == "file") {
            rawUri.path?.let {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(it))
            } ?: rawUri
        } else {
            rawUri
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/x-wav"
            putExtra(Intent.EXTRA_STREAM, shareUri)
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
            settingsRepository: SettingsRepository,
            appContext: Context
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RecordingsViewModel(repository, settingsRepository, appContext) as T
            }
        }
    }
}
