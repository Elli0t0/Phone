package io.github.elli0t0.phone.feature.recents.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.data.calllog.model.CallGroup
import io.github.elli0t0.phone.data.calllog.model.CallLogEntry
import io.github.elli0t0.phone.data.calllog.model.CallType
import io.github.elli0t0.phone.feature.recents.CallFilter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecentsFilterChips(
    selectedFilter: CallFilter,
    onFilterSelected: (CallFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recents_filter_chips"),
        horizontalArrangement = Arrangement.spacedBy(spacing.small)
    ) {
        CallFilter.entries.forEach { filter ->
            val isSelected = filter == selectedFilter
            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(filter) },
                label = { Text(text = stringResource(id = filter.titleRes)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}")
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CallLogRow(
    group: CallGroup,
    isExpanded: Boolean,
    canWriteCallLog: Boolean,
    onRowClick: () -> Unit,
    onCallClick: () -> Unit,
    onMessageClick: () -> Unit,
    onOpenContactClick: () -> Unit,
    onAddContactClick: () -> Unit,
    onCopyNumberClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    var showMenu by remember { mutableStateOf(false) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val formattedTime = remember(group.date) { timeFormat.format(Date(group.date)) }

    val isMissed = group.type == CallType.Missed || group.type == CallType.Rejected || group.type == CallType.Blocked
    val typeColor = if (isMissed) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .combinedClickable(
                onClick = onRowClick,
                onLongClick = { showMenu = true }
            )
            .testTag("call_row_${group.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.medium, vertical = spacing.small)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Call Type Icon
                Icon(
                    painter = painterResource(id = group.type.iconRes),
                    contentDescription = stringResource(id = group.type.titleRes),
                    tint = typeColor,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(spacing.medium))

                // Title + Subtitle
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = group.displayTitle,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = if (isMissed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (group.count > 1) {
                            Text(
                                text = " (${group.count})",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isMissed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    val subtitleParts = buildList {
                        add(stringResource(id = group.type.titleRes))
                        if (!isMissed) {
                            add(formatDuration(group.primaryCall.durationSeconds, group.type))
                        }
                        add(formattedTime)
                        group.primaryCall.simInfo?.let { add(it) }
                    }
                    Text(
                        text = subtitleParts.joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Call back button
                IconButton(
                    onClick = onCallClick,
                    modifier = Modifier.testTag("btn_callback_${group.id}")
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_phone),
                        contentDescription = stringResource(id = R.string.cd_call_back, group.displayTitle),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Long press context menu
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.action_call)) },
                        onClick = { showMenu = false; onCallClick() }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.action_message)) },
                        onClick = { showMenu = false; onMessageClick() }
                    )
                    if (group.primaryCall.contactId != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.action_open_contact)) },
                            onClick = { showMenu = false; onOpenContactClick() }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.action_add_contact)) },
                            onClick = { showMenu = false; onAddContactClick() }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.action_copy_number)) },
                        onClick = { showMenu = false; onCopyNumberClick() }
                    )
                    if (canWriteCallLog) {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.action_delete_call), color = MaterialTheme.colorScheme.error) },
                            onClick = { showMenu = false; onDeleteClick() }
                        )
                    }
                }
            }

            // Expanded Call Details
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                ExpandedCallDetails(
                    call = group.primaryCall,
                    onCallClick = onCallClick,
                    onMessageClick = onMessageClick,
                    onOpenContactClick = onOpenContactClick,
                    onAddContactClick = onAddContactClick
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

@Composable
private fun ExpandedCallDetails(
    call: CallLogEntry,
    onCallClick: () -> Unit,
    onMessageClick: () -> Unit,
    onOpenContactClick: () -> Unit,
    onAddContactClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val durationText = formatDuration(call.durationSeconds, call.type)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = spacing.small)
            .testTag("expanded_call_details_${call.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(id = call.type.titleRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = durationText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(spacing.small))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = onCallClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_phone),
                    contentDescription = stringResource(id = R.string.action_call),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onMessageClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_message),
                    contentDescription = stringResource(id = R.string.action_message),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            if (call.contactId != null) {
                IconButton(onClick = onOpenContactClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_person),
                        contentDescription = stringResource(id = R.string.action_open_contact),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                IconButton(onClick = onAddContactClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add),
                        contentDescription = stringResource(id = R.string.action_add_contact),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

private fun formatDuration(durationSeconds: Long, type: CallType): String {
    if (type == CallType.Missed || type == CallType.Rejected || type == CallType.Blocked) {
        return "Missed"
    }
    if (durationSeconds <= 0L) return "0 sec"
    val mins = durationSeconds / 60
    val secs = durationSeconds % 60
    return if (mins > 0) {
        "${mins}m ${secs}s"
    } else {
        "${secs}s"
    }
}

@Composable
fun RecentsEmptyState(
    isFiltered: Boolean,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(spacing.large)
            .testTag("recents_empty_state"),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(id = R.drawable.ic_recents),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(spacing.medium))
            Text(
                text = stringResource(
                    id = if (isFiltered) R.string.recents_empty_filtered else R.string.recents_empty_title
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!isFiltered) {
                Spacer(modifier = Modifier.height(spacing.extraSmall))
                Text(
                    text = stringResource(id = R.string.recents_empty_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
