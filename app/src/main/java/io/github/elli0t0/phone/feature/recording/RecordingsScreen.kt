package io.github.elli0t0.phone.feature.recording

import android.text.format.DateFormat
import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.elli0t0.phone.PhoneApplication
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.common.PhoneNumberFormatter
import io.github.elli0t0.phone.core.design.GlassListCard
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.telecom.CallManager
import io.github.elli0t0.phone.data.recordings.RecordingEntity
import io.github.elli0t0.phone.data.settings.model.CardStyle
import io.github.elli0t0.phone.data.settings.model.RecordingSort
import io.github.elli0t0.phone.feature.contacts.ui.ContactSearchBar
import io.github.elli0t0.phone.navigation.InnerTopBar
import java.util.Date

/**
 * Standalone recordings library: search, day filter, sort, one
 * card per day (or one flat card for the non-day sorts), selection
 * with bulk delete, star, rename/note, playback with seek, skip
 * and speed, share, call back, delete.
 */
@Composable
fun RecordingsScreen(onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as PhoneApplication
    val vm: RecordingsViewModel = viewModel(
        factory = RecordingsViewModel.provideFactory(
            app.container.recordingsRepository, app.container.settingsRepository, app
        )
    )
    val days by vm.days.collectAsStateWithLifecycle()
    val flatItems by vm.flatItems.collectAsStateWithLifecycle()
    val query by vm.searchQuery.collectAsStateWithLifecycle()
    val filter by vm.dateFilter.collectAsStateWithLifecycle()
    val sort by vm.sort.collectAsStateWithLifecycle()
    val playingId by vm.playingId.collectAsStateWithLifecycle()
    val positionMs by vm.positionMs.collectAsStateWithLifecycle()
    val speed by vm.speed.collectAsStateWithLifecycle()
    val selectedIds by vm.selectedIds.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val spacing = LocalSpacing.current
    var deleteTarget by remember { mutableStateOf<RecordingEntity?>(null) }
    var editTarget by remember { mutableStateOf<RecordingEntity?>(null) }
    var showBulkDelete by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    val selecting = selectedIds.isNotEmpty()
    val dayMode = sort == RecordingSort.NEWEST || sort == RecordingSort.OLDEST
    val visibleIds: List<Long> = if (dayMode) {
        days.flatMap { it.items }.map { it.id }
    } else {
        flatItems.map { it.id }
    }

    BackHandler(enabled = selecting) { vm.clearSelection() }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("recordings_page"),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(bottom = inner.calculateBottomPadding())) {
            InnerTopBar(
                onBack = onNavigateBack,
                title = if (selecting) {
                    stringResource(R.string.recordings_selected_count, selectedIds.size)
                } else {
                    stringResource(R.string.recordings_title)
                },
                backTestTag = "btn_recordings_back",
                actions = {
                    if (selecting) {
                        IconButton(onClick = { vm.selectAll(visibleIds) }) {
                            Icon(
                                painterResource(R.drawable.ic_check),
                                contentDescription = stringResource(R.string.recordings_select_all),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        IconButton(onClick = { showBulkDelete = true }) {
                            Icon(
                                painterResource(R.drawable.ic_delete),
                                contentDescription = stringResource(R.string.recording_delete),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    } else {
                        Box {
                            IconButton(onClick = { sortMenu = true }) {
                                Icon(
                                    painterResource(R.drawable.ic_sort),
                                    contentDescription = stringResource(R.string.recordings_sort),
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                                RecordingSort.entries.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(s.labelRes())) },
                                        onClick = { vm.setSort(s); sortMenu = false },
                                        trailingIcon = if (sort == s) {
                                            { Icon(painterResource(R.drawable.ic_check), contentDescription = null) }
                                        } else {
                                            null
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            )
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = spacing.screenHorizontalPadding),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    ContactSearchBar(
                        query = query, onQueryChange = vm::onSearchChanged, onClear = { vm.onSearchChanged("") },
                        placeholderText = stringResource(R.string.recordings_search_hint),
                        modifier = Modifier.fillMaxWidth().padding(vertical = spacing.small)
                    )
                }
                item {
                    Text(
                        stringResource(
                            R.string.recordings_open_summary, stats.count,
                            Formatter.formatShortFileSize(context, stats.totalBytes)
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = spacing.small)
                    )
                }
                item {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RecordingDateFilter.entries.forEach { f ->
                            FilterChip(
                                selected = filter == f, onClick = { vm.onFilterChanged(f) },
                                label = { Text(stringResource(f.labelRes())) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }
                if (days.isEmpty() && flatItems.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.recording_empty), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = spacing.large)
                        )
                    }
                }
                if (dayMode) {
                    days.forEach { day ->
                        item(key = "h_${day.key}") {
                            Text(
                                day.title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(top = spacing.small, bottom = spacing.extraSmall)
                            )
                        }
                        item(key = "c_${day.key}") {
                            RecordingsDayCard {
                                day.items.forEachIndexed { index, e ->
                                    RecordingListItem(
                                        entity = e, isPlaying = playingId == e.id,
                                        positionMs = if (playingId == e.id) positionMs else 0L,
                                        onPlayToggle = { vm.togglePlay(e) }, onSeek = vm::seekTo,
                                        onShare = { vm.share(e, context) }, onDelete = { deleteTarget = e },
                                        onCall = { CallManager.placeCall(context, e.number) },
                                        selecting = selecting, selected = e.id in selectedIds,
                                        onToggleSelect = { vm.toggleSelect(e.id) },
                                        onStar = { vm.toggleStarred(e) }, onEdit = { editTarget = e },
                                        speed = speed, onSkip = vm::skip, onCycleSpeed = vm::cycleSpeed
                                    )
                                    if (index < day.items.lastIndex) {
                                        RecordingDivider()
                                    }
                                }
                            }
                        }
                    }
                } else {
                    item(key = "c_flat") {
                        RecordingsDayCard {
                            flatItems.forEachIndexed { index, e ->
                                RecordingListItem(
                                    entity = e, isPlaying = playingId == e.id,
                                    positionMs = if (playingId == e.id) positionMs else 0L,
                                    onPlayToggle = { vm.togglePlay(e) }, onSeek = vm::seekTo,
                                    onShare = { vm.share(e, context) }, onDelete = { deleteTarget = e },
                                    onCall = { CallManager.placeCall(context, e.number) },
                                    selecting = selecting, selected = e.id in selectedIds,
                                    onToggleSelect = { vm.toggleSelect(e.id) },
                                    onStar = { vm.toggleStarred(e) }, onEdit = { editTarget = e },
                                    speed = speed, onSkip = vm::skip, onCycleSpeed = vm::cycleSpeed
                                )
                                if (index < flatItems.lastIndex) {
                                    RecordingDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    deleteTarget?.let { t ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.recordings_delete_title)) },
            text = { Text(stringResource(R.string.recordings_delete_message)) },
            confirmButton = { TextButton(onClick = { vm.delete(t); deleteTarget = null }) { Text(stringResource(R.string.recording_delete)) } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }
    if (showBulkDelete) {
        AlertDialog(
            onDismissRequest = { showBulkDelete = false },
            title = { Text(stringResource(R.string.recordings_delete_selected_title, selectedIds.size)) },
            text = { Text(stringResource(R.string.recordings_delete_selected_message, selectedIds.size)) },
            confirmButton = { TextButton(onClick = { vm.deleteSelected(); showBulkDelete = false }) { Text(stringResource(R.string.recording_delete)) } },
            dismissButton = { TextButton(onClick = { showBulkDelete = false }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }
    editTarget?.let { t ->
        var titleText by remember(t) { mutableStateOf(t.title ?: "") }
        var noteText by remember(t) { mutableStateOf(t.note ?: "") }
        AlertDialog(
            onDismissRequest = { editTarget = null },
            title = { Text(stringResource(R.string.recordings_edit_title)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = titleText, onValueChange = { titleText = it },
                        label = { Text(stringResource(R.string.recordings_title_hint)) },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(spacing.small))
                    OutlinedTextField(
                        value = noteText, onValueChange = { noteText = it },
                        label = { Text(stringResource(R.string.recordings_note_hint)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = { TextButton(onClick = { vm.saveTitleNote(t, titleText, noteText); editTarget = null }) { Text(stringResource(R.string.action_save)) } },
            dismissButton = { TextButton(onClick = { editTarget = null }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }
}

/** Same container styling as DaySectionCard in Recents. */
@Composable
private fun RecordingsDayCard(content: @Composable () -> Unit) {
    val spacing = LocalSpacing.current
    GlassListCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = spacing.small),
        shape = RoundedCornerShape(PhoneDimens.recentsDayCardCornerRadius)
    ) {
        content()
    }
}

/** Divider between items in a day card, same style as the Recents rows. */
@Composable
private fun RecordingDivider() {
    val spacing = LocalSpacing.current
    val insetCard = LocalAppSettings.current.effectiveStyle.card == CardStyle.INSET
    if (insetCard) {
        HorizontalDivider(
            thickness = 0.5.dp,
            color = Color.White.copy(alpha = 0.12f),
            modifier = Modifier.padding(start = spacing.medium + 44.dp + spacing.medium)
        )
    } else {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = PhoneDimens.recentsDividerAlpha),
            modifier = Modifier.padding(
                start = spacing.medium + 44.dp + spacing.medium,
                end = spacing.medium
            )
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecordingListItem(
    entity: RecordingEntity, isPlaying: Boolean, positionMs: Long,
    onPlayToggle: () -> Unit, onSeek: (Int) -> Unit,
    onShare: () -> Unit, onDelete: () -> Unit, onCall: () -> Unit,
    selecting: Boolean, selected: Boolean, onToggleSelect: () -> Unit,
    onStar: () -> Unit, onEdit: () -> Unit,
    speed: Float, onSkip: (Int) -> Unit, onCycleSpeed: () -> Unit
) {
    val spacing = LocalSpacing.current
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    val time = remember(entity.startedAt) { DateFormat.getTimeFormat(context).format(Date(entity.startedAt)) }
    val size = remember(entity.sizeBytes) { Formatter.formatShortFileSize(context, entity.sizeBytes) }
    val title = entity.title?.takeIf { it.isNotBlank() }
        ?: entity.displayName?.takeIf { it.isNotBlank() }
        // Same formatter as Recents and contact details, so a
        // recording never shows its raw number (+98...) while the
        // same call shows it spaced (+98 13 ...).
        ?: PhoneNumberFormatter.format(entity.number)
            .ifBlank { stringResource(R.string.recordings_unknown) }
    val speedText = if (speed == speed.toInt().toFloat()) "${speed.toInt()}x" else "${speed}x"
    Column(
        Modifier.fillMaxWidth()
            .combinedClickable(
                onClick = { if (selecting) onToggleSelect() },
                onLongClick = { onToggleSelect() }
            )
            .padding(horizontal = spacing.medium, vertical = spacing.small)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selecting) {
                Checkbox(checked = selected, onCheckedChange = { onToggleSelect() })
            } else {
                Box(
                    Modifier.size(44.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                        .clickable(onClick = onPlayToggle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painterResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play), null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(Modifier.width(spacing.medium))
            Column(Modifier.weight(1f)) {
                Text(
                    title, style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Content),
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                if (!entity.note.isNullOrBlank()) {
                    Text(
                        entity.note, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    "$time · ${formatPlayerTime(entity.durationMs)} · $size", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f)
                )
                if (entity.isSilent) {
                    Text(
                        stringResource(R.string.recording_silent_badge),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error
                    )
                }
            }
            IconButton(onClick = onStar) {
                Icon(
                    painterResource(if (entity.starred) R.drawable.ic_star else R.drawable.ic_star_outline),
                    contentDescription = stringResource(R.string.cd_favorite_star),
                    tint = if (entity.starred) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(
                        painterResource(R.drawable.ic_more_vert), stringResource(R.string.cd_more_options),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.action_call)) }, onClick = { menu = false; onCall() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.recording_share)) }, onClick = { menu = false; onShare() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.recordings_edit)) }, onClick = { menu = false; onEdit() })
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.recording_delete), color = MaterialTheme.colorScheme.error) },
                        onClick = { menu = false; onDelete() }
                    )
                }
            }
        }
        AnimatedVisibility(visible = isPlaying) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onSkip(-10_000) }) { Text("-10s") }
                Slider(
                    value = positionMs.toFloat(), onValueChange = { onSeek(it.toInt()) },
                    valueRange = 0f..maxOf(1f, entity.durationMs.toFloat()), modifier = Modifier.weight(1f)
                )
                Text(
                    "${formatPlayerTime(positionMs)} / ${formatPlayerTime(entity.durationMs)}",
                    style = MaterialTheme.typography.bodySmall
                )
                TextButton(onClick = { onSkip(10_000) }) { Text("+10s") }
                TextButton(onClick = onCycleSpeed) { Text(speedText) }
            }
        }
    }
}

/** Private copy of the player time format (the original is private in RecordingSettingsScreen). */
private fun formatPlayerTime(ms: Long): String {
    val totalSeconds = ms / 1000
    return "${totalSeconds / 60}:${(totalSeconds % 60).toString().padStart(2, '0')}"
}

/** Sort menu labels; the enum's own label is English-only. */
private fun RecordingSort.labelRes(): Int = when (this) {
    RecordingSort.NEWEST -> R.string.recordings_sort_newest
    RecordingSort.OLDEST -> R.string.recordings_sort_oldest
    RecordingSort.LONGEST -> R.string.recordings_sort_longest
    RecordingSort.LARGEST -> R.string.recordings_sort_largest
    RecordingSort.STARRED_FIRST -> R.string.recordings_sort_starred_first
}
