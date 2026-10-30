package io.github.elli0t0.phone.feature.recording

import android.text.format.DateFormat
import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import io.github.elli0t0.phone.core.design.GlassListCard
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.telecom.CallManager
import io.github.elli0t0.phone.data.recordings.RecordingEntity
import io.github.elli0t0.phone.data.settings.model.CardStyle
import io.github.elli0t0.phone.feature.contacts.ui.ContactSearchBar
import io.github.elli0t0.phone.navigation.InnerTopBar
import java.util.Date

/**
 * Standalone recordings library: search, day filter, one card per
 * day, playback with seek, share, call back, delete. Separate from
 * Call recording settings, whose own list stays as it is.
 */
@Composable
fun RecordingsScreen(onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as PhoneApplication
    val vm: RecordingsViewModel = viewModel(factory = RecordingsViewModel.provideFactory(app.container.recordingsRepository, app))
    val days by vm.days.collectAsStateWithLifecycle()
    val query by vm.searchQuery.collectAsStateWithLifecycle()
    val filter by vm.dateFilter.collectAsStateWithLifecycle()
    val playingId by vm.playingId.collectAsStateWithLifecycle()
    val positionMs by vm.positionMs.collectAsStateWithLifecycle()
    val spacing = LocalSpacing.current
    var deleteTarget by remember { mutableStateOf<RecordingEntity?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("recordings_page"),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(bottom = inner.calculateBottomPadding())) {
            InnerTopBar(onBack = onNavigateBack, title = stringResource(R.string.recordings_title), backTestTag = "btn_recordings_back")
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
                if (days.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.recording_empty), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = spacing.large)
                        )
                    }
                }
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
                                    onCall = { CallManager.placeCall(context, e.number) }
                                )
                                if (index < day.items.lastIndex) {
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

@Composable
private fun RecordingListItem(
    entity: RecordingEntity, isPlaying: Boolean, positionMs: Long,
    onPlayToggle: () -> Unit, onSeek: (Int) -> Unit,
    onShare: () -> Unit, onDelete: () -> Unit, onCall: () -> Unit
) {
    val spacing = LocalSpacing.current
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    val time = remember(entity.startedAt) { DateFormat.getTimeFormat(context).format(Date(entity.startedAt)) }
    val size = remember(entity.sizeBytes) { Formatter.formatShortFileSize(context, entity.sizeBytes) }
    val title = entity.displayName?.takeIf { it.isNotBlank() } ?: entity.number.ifBlank { stringResource(R.string.recordings_unknown) }
    Column(Modifier.fillMaxWidth().padding(horizontal = spacing.medium, vertical = spacing.small)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
            Spacer(Modifier.width(spacing.medium))
            Column(Modifier.weight(1f)) {
                Text(
                    title, style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Content),
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
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
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.recording_delete), color = MaterialTheme.colorScheme.error) },
                        onClick = { menu = false; onDelete() }
                    )
                }
            }
        }
        AnimatedVisibility(visible = isPlaying) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = positionMs.toFloat(), onValueChange = { onSeek(it.toInt()) },
                    valueRange = 0f..maxOf(1f, entity.durationMs.toFloat()), modifier = Modifier.weight(1f)
                )
                Text(
                    "${formatPlayerTime(positionMs)} / ${formatPlayerTime(entity.durationMs)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/** Private copy of the player time format (the original is private in RecordingSettingsScreen). */
private fun formatPlayerTime(ms: Long): String {
    val totalSeconds = ms / 1000
    return "${totalSeconds / 60}:${(totalSeconds % 60).toString().padStart(2, '0')}"
}
