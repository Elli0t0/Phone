package io.github.elli0t0.phone.feature.recents.ui

import android.content.ContentValues
import android.provider.BlockedNumberContract
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.common.PhoneNumberFormatter
import io.github.elli0t0.phone.core.design.CallSwipeGreen
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.MessageSwipeBlue
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.data.calllog.model.CallGroup
import io.github.elli0t0.phone.data.calllog.model.CallLogEntry
import io.github.elli0t0.phone.data.calllog.model.CallType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    showDivider: Boolean = true,
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
    // Unknown numbers are formatted consistently (same formatter as the dialer)
    // instead of showing the raw number from the call log.
    val displayTitle = if (group.primaryCall.contactId == null) {
        PhoneNumberFormatter.format(group.displayTitle)
    } else {
        group.displayTitle
    }
    val nameColor = if (isMissed) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    // Swipe right = call, left = message. The action fires and the row snaps
    // back; the row is never dismissed.
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onCallClick()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onMessageClick()
                    false
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.targetValue
            val background = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> CallSwipeGreen
                SwipeToDismissBoxValue.EndToStart -> MessageSwipeBlue
                SwipeToDismissBoxValue.Settled -> androidx.compose.ui.graphics.Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(background)
                    .padding(horizontal = spacing.medium),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Alignment.CenterStart
                } else {
                    Alignment.CenterEnd
                }
            ) {
                if (direction != SwipeToDismissBoxValue.Settled) {
                    Icon(
                        painter = painterResource(
                            id = if (direction == SwipeToDismissBoxValue.StartToEnd) {
                                R.drawable.ic_phone
                            } else {
                                R.drawable.ic_message
                            }
                        ),
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        modifier = modifier.testTag("call_row_${group.id}"),
        content = {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .combinedClickable(
                onClick = onRowClick,
                onLongClick = { showMenu = true }
            )
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
                            text = displayTitle,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = nameColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (group.count > 1) {
                            Text(
                                text = " (${group.count})",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = nameColor
                            )
                        }
                    }

                    val subtitleParts = buildList {
                        add(stringResource(id = group.type.titleRes))
                        if (!isMissed) {
                            add(formatDuration(group.primaryCall.durationSeconds, group.type))
                        }
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

                // Time on the right, instead of the blue call icon.
                // Call-back stays reachable via swipe, tap-to-expand and long-press.
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.padding(start = spacing.small)
                )

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
        if (showDivider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
    }
        }
    )
}

/**
 * One card per day section: the day's call rows with dividers between them,
 * matching the reference Recents layout.
 */
@Composable
fun DaySectionCard(
    calls: List<CallGroup>,
    modifier: Modifier = Modifier,
    rowContent: @Composable (CallGroup) -> Unit
) {
    val spacing = LocalSpacing.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = spacing.small),
        shape = RoundedCornerShape(PhoneDimens.sectionCardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        calls.forEachIndexed { index, group ->
            rowContent(group)
            if (index < calls.lastIndex) {
                // Divider starts at the text, not at the row edge:
                // row padding + type icon + gap.
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 1.dp,
                    modifier = Modifier.padding(
                        start = spacing.medium + 20.dp + spacing.medium,
                        end = spacing.medium
                    )
                )
            }
        }
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val canBlock = remember {
        runCatching { BlockedNumberContract.canCurrentUserBlockNumbers(context) }
            .getOrDefault(false)
    }
    var isBlocked by remember(call.number) {
        mutableStateOf(
            runCatching { BlockedNumberContract.isBlocked(context, call.number) }
                .getOrDefault(false)
        )
    }

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
            if (canBlock) {
                IconButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            val succeeded = runCatching {
                                if (isBlocked) {
                                    BlockedNumberContract.unblock(context, call.number)
                                } else {
                                    val values = ContentValues().apply {
                                        put(
                                            BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER,
                                            call.number
                                        )
                                    }
                                    context.contentResolver.insert(
                                        BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                                        values
                                    )
                                }
                            }.isSuccess
                            if (succeeded) {
                                withContext(Dispatchers.Main) { isBlocked = !isBlocked }
                            }
                        }
                    },
                    modifier = Modifier.testTag("block_button_${call.id}")
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_block),
                        contentDescription = stringResource(
                            id = if (isBlocked) {
                                R.string.recents_unblock_number
                            } else {
                                R.string.recents_block_number
                            }
                        ),
                        tint = if (isBlocked) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
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
