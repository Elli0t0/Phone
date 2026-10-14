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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.AccentBlue
import io.github.elli0t0.phone.core.design.CallAnswerGreen
import io.github.elli0t0.phone.core.design.CallRejectedOrange
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.OnAccentBlue
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
    showSim: Boolean = false,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    var showMenu by remember { mutableStateOf(false) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val formattedTime = remember(group.date) { timeFormat.format(Date(group.date)) }

    // Icon color by call type: incoming green, outgoing accent blue,
    // missed red, rejected/blocked orange.
    val typeColor = when (group.type) {
        CallType.Incoming -> CallAnswerGreen
        CallType.Outgoing -> AccentBlue
        CallType.Missed -> MaterialTheme.colorScheme.error
        CallType.Rejected, CallType.Blocked -> CallRejectedOrange
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    // Only a missed name is red; rejected/blocked names stay default.
    val nameColor = if (group.type == CallType.Missed) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    // Swipe to act: right = call, left = message. The row snaps back
    // after the action; it is never dismissed.
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
                SwipeToDismissBoxValue.Settled -> true
            }
        }
    )

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {
                val target = dismissState.targetValue
                if (target != SwipeToDismissBoxValue.Settled) {
                    val isCall = target == SwipeToDismissBoxValue.StartToEnd
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(if (isCall) CallAnswerGreen else AccentBlue),
                        contentAlignment = if (isCall) Alignment.CenterStart else Alignment.CenterEnd
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (isCall) R.drawable.ic_phone else R.drawable.ic_message
                            ),
                            contentDescription = null,
                            tint = OnAccentBlue,
                            modifier = Modifier
                                .padding(horizontal = spacing.medium)
                                .size(PhoneDimens.recentsSwipeRevealIconSize)
                        )
                    }
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = onRowClick,
                        onLongClick = { showMenu = true }
                    )
                    .testTag("call_row_${group.id}")
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.medium)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PhoneDimens.recentsRowHeight),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Call Type Icon
                Icon(
                    painter = painterResource(id = group.type.iconRes),
                    contentDescription = stringResource(id = group.type.titleRes),
                    tint = typeColor,
                    modifier = Modifier.size(PhoneDimens.recentsRowIconSize)
                )

                Spacer(modifier = Modifier.width(spacing.medium))

                // Title + Subtitle
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = group.displayTitle,
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

                    // Subtitle: number type and SIM (SIM only on dual-SIM phones).
                    // Type and duration live in the expanded card.
                    val subtitleParts = buildList {
                        add(stringResource(id = R.string.type_phone_mobile))
                        if (showSim) {
                            group.primaryCall.simInfo?.let { add(it) }
                        }
                    }
                    Text(
                        text = subtitleParts.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Time on the right, replacing the call-back button.
                // Call-back stays reachable via swipe right, long-press
                // menu, and the expanded details.
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
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
            }
        }
        if (showDivider) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = PhoneDimens.recentsDividerAlpha),
                modifier = Modifier.padding(
                    start = spacing.medium + PhoneDimens.recentsRowIconSize + spacing.medium,
                    end = spacing.medium
                )
            )
        }
    }
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
        shape = RoundedCornerShape(PhoneDimens.recentsDayCardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        calls.forEachIndexed { index, group ->
            rowContent(group)
            if (index < calls.lastIndex) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = PhoneDimens.recentsDividerAlpha),
                    modifier = Modifier.padding(
                        // Divider starts at the text, not at the row edge:
                        // row padding + icon + gap.
                        start = spacing.medium + PhoneDimens.recentsRowIconSize + spacing.medium,
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
        // Call type and duration, moved here from the collapsed row subtitle.
        val showDuration = call.type == CallType.Incoming || call.type == CallType.Outgoing ||
            call.type == CallType.Voicemail || call.type == CallType.Unknown
        val typeLabel = stringResource(id = call.type.titleRes)
        val typeAndDuration = if (showDuration && call.durationSeconds > 0) {
            "$typeLabel · ${formatDuration(call.durationSeconds, call.type)}"
        } else {
            typeLabel
        }
        Text(
            text = typeAndDuration,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = spacing.small)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ExpandedActionButton(
                iconRes = R.drawable.ic_phone,
                contentDescription = stringResource(id = R.string.action_call),
                tint = MaterialTheme.colorScheme.primary,
                onClick = onCallClick
            )
            ExpandedActionButton(
                iconRes = R.drawable.ic_message,
                contentDescription = stringResource(id = R.string.action_message),
                tint = MaterialTheme.colorScheme.primary,
                onClick = onMessageClick
            )
            if (call.contactId != null) {
                ExpandedActionButton(
                    iconRes = R.drawable.ic_person,
                    contentDescription = stringResource(id = R.string.action_open_contact),
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = onOpenContactClick
                )
            } else {
                ExpandedActionButton(
                    iconRes = R.drawable.ic_add,
                    contentDescription = stringResource(id = R.string.action_add_contact),
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = onAddContactClick
                )
            }
            if (canBlock) {
                ExpandedActionButton(
                    iconRes = R.drawable.ic_block,
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
                    },
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
                )
            }
        }
    }
}

// One circular action button in the expanded call details.
@Composable
private fun ExpandedActionButton(
    iconRes: Int,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
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
