package io.github.elli0t0.phone.feature.recents.ui

import android.content.ContentValues
import android.provider.BlockedNumberContract
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.ripple
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.AccentBlue
import io.github.elli0t0.phone.core.design.SwipeCallGreen
import io.github.elli0t0.phone.core.design.SwipeDeleteRed
import io.github.elli0t0.phone.core.design.GlassListCard
import io.github.elli0t0.phone.core.design.CallAnswerGreen
import io.github.elli0t0.phone.core.design.CallRejectedOrange
import io.github.elli0t0.phone.core.design.LiquidGlassIconButton
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.Motion
import io.github.elli0t0.phone.core.design.OnAccentBlue
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.design.bouncyClick
import io.github.elli0t0.phone.core.design.insetRowPress
import io.github.elli0t0.phone.core.design.rememberLightHaptic
import io.github.elli0t0.phone.data.settings.model.CardStyle
import io.github.elli0t0.phone.data.calllog.model.CallGroup
import io.github.elli0t0.phone.data.calllog.model.CallLogEntry
import io.github.elli0t0.phone.data.calllog.model.CallType
import android.text.format.DateFormat
import io.github.elli0t0.phone.data.settings.model.AnimationOption
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.github.elli0t0.phone.core.common.bidiWrap
import androidx.compose.ui.text.style.TextDirection

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CallLogRow(
    group: CallGroup,
    isExpanded: Boolean,
    showMenu: Boolean,
    canWriteCallLog: Boolean,
    onRowClick: () -> Unit,
    onCollapse: () -> Unit,
    onShowMenu: () -> Unit,
    onDismissMenu: () -> Unit,
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
    val timeFormat = rememberTimeFormat()
    val formattedTime = remember(group.date, timeFormat) { timeFormat.format(Date(group.date)) }
    // Expanded details respect the Animations setting: ON expands +
    // fades, REDUCED fades only at ~60% duration, OFF is instant.
    val animationOption = LocalAppSettings.current.animationOption
    val detailsEnter: EnterTransition = when (animationOption) {
        AnimationOption.ON -> expandVertically(
            animationSpec = tween<IntSize>(
                durationMillis = Motion.scaledDurationMs(Motion.Durations.Medium, animationOption),
                easing = Motion.Easings.Standard
            )
        ) + fadeIn(
            animationSpec = tween<Float>(
                durationMillis = Motion.scaledDurationMs(Motion.Durations.Medium, animationOption),
                easing = Motion.Easings.Standard
            )
        )
        AnimationOption.REDUCED -> fadeIn(
            animationSpec = tween<Float>(
                durationMillis = Motion.scaledDurationMs(Motion.Durations.Medium, animationOption),
                easing = Motion.Easings.Standard
            )
        )
        AnimationOption.OFF -> EnterTransition.None
    }
    val detailsExit: ExitTransition = when (animationOption) {
        AnimationOption.ON -> shrinkVertically(
            animationSpec = tween<IntSize>(
                durationMillis = Motion.scaledDurationMs(Motion.Durations.Medium, animationOption),
                easing = Motion.Easings.Standard
            )
        ) + fadeOut(
            animationSpec = tween<Float>(
                durationMillis = Motion.scaledDurationMs(Motion.Durations.Medium, animationOption),
                easing = Motion.Easings.Standard
            )
        )
        AnimationOption.REDUCED -> fadeOut(
            animationSpec = tween<Float>(
                durationMillis = Motion.scaledDurationMs(Motion.Durations.Medium, animationOption),
                easing = Motion.Easings.Standard
            )
        )
        AnimationOption.OFF -> ExitTransition.None
    }
    // Shared press source: bouncyClick and combinedClickable observe the
    // same press state, so the row bounces (0.90) while pressed and ripples.
    val rowSource = remember { MutableInteractionSource() }
    val lightHaptic = rememberLightHaptic()
    val insetCard = LocalAppSettings.current.effectiveStyle.card == CardStyle.INSET
    val swipeEnabled = LocalAppSettings.current.effectiveStyle.swipeActions

    // Icon color by call type.
    val typeColor = callTypeColor(group.type)
    // Only a missed name is red; rejected/blocked names stay default.
    val nameColor = if (group.type == CallType.Missed) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    // Swipe to act: start->end = call back (snaps back), end->start
    // = delete (dismisses; the screen shows an Undo snackbar).
    // Start/end are layout directions, so RTL swaps the physical
    // swipe directions automatically.
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onCallClick()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDeleteClick()
                    true
                }
                SwipeToDismissBoxValue.Settled -> true
            }
        }
    )
    // Light haptic the moment the swipe crosses the dismiss
    // threshold (target leaves Settled), not on release.
    LaunchedEffect(dismissState.targetValue) {
        if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
            lightHaptic()
        }
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        val rowContent: @Composable () -> Unit = {
            // No clickable on this column anymore: the header row
            // below owns tap/long-press, so the ripple stays bounded
            // to it instead of covering the whole expanded card.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("call_row_${group.id}")
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.medium)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PhoneDimens.recentsRowHeight)
                    // Only the header row is clickable: the bounded
                    // ripple stays clipped to this row's shape, and
                    // the expanded details and action buttons no
                    // longer share its clickable/indication.
                    .bouncyClick(rowSource)
                    .combinedClickable(
                        interactionSource = rowSource,
                        indication = if (insetCard) null else ripple(),
                        onClick = onRowClick,
                        // Collapse first (this row or any other),
                        // then open the menu: expand and menu never
                        // coexist, so the menu can never cover
                        // expanded details text.
                        onLongClick = { onCollapse(); onShowMenu() }
                    )
                    .then(if (insetCard) Modifier.insetRowPress(rowSource) else Modifier),
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

                // Title + Subtitle. Name and subtitle share one
                // column; both start-align with content direction, so
                // an RTL name (e.g. Persian) starts at the row's
                // leading edge instead of sliding toward the time.
                // Row height and padding are unchanged from LTR rows.
                Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = bidiWrap(group.displayTitle),
                            style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Content),
                            color = nameColor,
                            textAlign = TextAlign.Start,
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
                        text = bidiWrap(subtitleParts.joinToString(" · ")),
                        style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Content),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f),
                        textAlign = TextAlign.Start,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Time in its own fixed-width slot at the row's
                // trailing edge, separated from the name column so a
                // long RTL name can never collide with it. Call-back
                // stays reachable via swipe right, long-press menu,
                // and the expanded details.
                Spacer(modifier = Modifier.width(spacing.medium))
                Box(
                    modifier = Modifier.width(PhoneDimens.recentsTimeSlotWidth),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Long press context menu, anchored to the row's
                    // end side (the time slot) with a small offset,
                    // so it opens over the row's right half. Expanded
                    // details are collapsed whenever it opens, so it
                    // can never cover detail lines; the stock menu
                    // still flips above when there is no room below.
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = onDismissMenu,
                        offset = DpOffset(0.dp, 4.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.action_call)) },
                            onClick = { onDismissMenu(); onCallClick() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.action_message)) },
                            onClick = { onDismissMenu(); onMessageClick() }
                        )
                        if (group.primaryCall.contactId != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.action_open_contact)) },
                                onClick = { onDismissMenu(); onOpenContactClick() }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.action_add_contact)) },
                                onClick = { onDismissMenu(); onAddContactClick() }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.action_copy_number)) },
                            onClick = { onDismissMenu(); onCopyNumberClick() }
                        )
                        if (canWriteCallLog) {
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.action_delete_call), color = MaterialTheme.colorScheme.error) },
                                onClick = { onDismissMenu(); onDeleteClick() }
                            )
                        }
                    }
                }
            }

            // Expanded Call Details
            AnimatedVisibility(
                visible = isExpanded,
                enter = detailsEnter,
                exit = detailsExit
            ) {
                ExpandedCallDetails(
                    group = group,
                    showSim = showSim,
                    onCallClick = onCallClick,
                    onMessageClick = onMessageClick,
                    onOpenContactClick = onOpenContactClick,
                    onAddContactClick = onAddContactClick
                )
            }
                }
            }
        }
        if (swipeEnabled) {
            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {
                    val target = dismissState.targetValue
                    if (target != SwipeToDismissBoxValue.Settled) {
                        val isCall = target == SwipeToDismissBoxValue.StartToEnd
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(if (isCall) SwipeCallGreen else SwipeDeleteRed),
                            contentAlignment = if (isCall) Alignment.CenterStart else Alignment.CenterEnd
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = if (isCall) R.drawable.ic_phone else R.drawable.ic_delete
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
                rowContent()
            }
        } else {
            rowContent()
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
    val insetCard = LocalAppSettings.current.effectiveStyle.card == CardStyle.INSET
    GlassListCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = spacing.small),
        shape = RoundedCornerShape(PhoneDimens.recentsDayCardCornerRadius)
    ) {
        calls.forEachIndexed { index, group ->
            rowContent(group)
            if (index < calls.lastIndex) {
                if (insetCard) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = Color.White.copy(alpha = 0.12f),
                        modifier = Modifier.padding(
                            // Divider starts at the text, not at the row edge:
                            // row padding + icon + gap.
                            start = spacing.medium + PhoneDimens.recentsRowIconSize + spacing.medium
                        )
                    )
                } else {
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
}

@Composable
private fun ExpandedCallDetails(
    group: CallGroup,
    showSim: Boolean,
    onCallClick: () -> Unit,
    onMessageClick: () -> Unit,
    onOpenContactClick: () -> Unit,
    onAddContactClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val call = group.primaryCall
    val spacing = LocalSpacing.current
    val context = LocalContext.current
    val timeFormat = rememberTimeFormat()
    val scope = rememberCoroutineScope()
    val animationOption = LocalAppSettings.current.animationOption
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
        // Single call: one line. Grouped calls: breakdown rows below.
        if (group.calls.size <= 1) {
            val showDuration = call.type == CallType.Incoming || call.type == CallType.Outgoing ||
                call.type == CallType.Voicemail || call.type == CallType.Unknown
            val typeLabel = stringResource(id = call.type.titleRes)
            val typeAndDuration = if (showDuration && call.durationSeconds > 0) {
                "$typeLabel · ${formatDuration(call.durationSeconds, call.type)}"
            } else {
                typeLabel
            }
            Text(
                text = "$typeAndDuration · ${timeFormat.format(Date(call.date))}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = spacing.small)
            )
        } else {
            // Breakdown list inside a recessed container.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(vertical = spacing.extraSmall)
            ) {
                group.calls.forEachIndexed { index, entry ->
                    CallBreakdownRow(entry = entry, showSim = showSim)
                    if (index < group.calls.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = PhoneDimens.recentsDividerAlpha),
                            modifier = Modifier.padding(horizontal = spacing.medium)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(spacing.small))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StaggeredAction(animationOption, 0) {
                ExpandedActionButton(
                    iconRes = R.drawable.ic_phone,
                    contentDescription = stringResource(id = R.string.action_call),
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = onCallClick
                )
            }
            StaggeredAction(animationOption, 1) {
                ExpandedActionButton(
                    iconRes = R.drawable.ic_message,
                    contentDescription = stringResource(id = R.string.action_message),
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = onMessageClick
                )
            }
            StaggeredAction(animationOption, 2) {
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
            }
            if (canBlock) {
                StaggeredAction(animationOption, 3) {
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
}

// One expanded action button, entering with the staggered scale-in.
@Composable
private fun StaggeredAction(
    animationOption: AnimationOption,
    index: Int,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = true,
        enter = expandedActionEnter(animationOption, index),
        exit = fadeOut()
    ) {
        content()
    }
}

// One glass action button.
@Composable
private fun ExpandedActionButton(
    iconRes: Int,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidGlassIconButton(
        onClick = onClick,
        modifier = modifier.size(PhoneDimens.detailActionButtonSize)
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(PhoneDimens.detailActionIconSize)
        )
    }
}

/**
 * The recents time format: honors the user's 12/24-hour setting and
 * locale (DateFormat.getTimeFormat), rebuilt when the 24-hour
 * setting or configuration changes. DateFormat is not thread-safe,
 * but all composition runs on the main thread, so one instance per
 * composition is safe.
 */
@Composable
private fun rememberTimeFormat(): java.text.DateFormat {
    val context = LocalContext.current
    return remember(context, DateFormat.is24HourFormat(context)) {
        DateFormat.getTimeFormat(context)
    }
}

@Composable
private fun formatDuration(durationSeconds: Long, type: CallType): String {
    if (type == CallType.Missed || type == CallType.Rejected || type == CallType.Blocked) {
        return stringResource(id = R.string.recents_missed)
    }
    if (durationSeconds <= 0L) return stringResource(id = R.string.recents_duration_zero)
    val mins = durationSeconds / 60
    val secs = durationSeconds % 60
    return if (mins > 0) {
        stringResource(id = R.string.recents_duration_min_sec, mins, secs)
    } else {
        stringResource(id = R.string.recents_duration_sec, secs)
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

// Semantic color for a call type.
@Composable
private fun callTypeColor(type: CallType): Color = when (type) {
    CallType.Incoming -> CallAnswerGreen
    CallType.Outgoing -> AccentBlue
    CallType.Missed -> MaterialTheme.colorScheme.error
    CallType.Rejected, CallType.Blocked -> CallRejectedOrange
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

// Staggered scale-in: ON fades+scales per button, REDUCED fades only, OFF instant.
private fun expandedActionEnter(option: AnimationOption, index: Int): EnterTransition {
    if (option == AnimationOption.OFF) return EnterTransition.None
    val duration = Motion.scaledDurationMs(Motion.Durations.Short, option)
    val fade = fadeIn(
        animationSpec = tween<Float>(
            durationMillis = duration,
            delayMillis = if (option == AnimationOption.ON) Motion.staggerDelayMs(index) else 0
        )
    )
    if (option == AnimationOption.REDUCED) return fade
    return fade + scaleIn(
        animationSpec = tween<Float>(
            durationMillis = duration,
            delayMillis = Motion.staggerDelayMs(index),
            easing = Motion.Easings.Emphasized
        ),
        initialScale = 0.8f
    )
}

// One breakdown row: type icon+label, exact time, duration, SIM badge.
@Composable
private fun CallBreakdownRow(
    entry: CallLogEntry,
    showSim: Boolean,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val timeFormat = rememberTimeFormat()
    val typeColor = callTypeColor(entry.type)
    val showDuration = entry.type == CallType.Incoming || entry.type == CallType.Outgoing ||
        entry.type == CallType.Voicemail || entry.type == CallType.Unknown
    val detailParts = buildList {
        add(timeFormat.format(Date(entry.date)))
        if (showDuration) {
            add(formatDuration(entry.durationSeconds, entry.type))
        }
        if (showSim) {
            entry.simInfo?.let { add(it) }
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.medium, vertical = spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = entry.type.iconRes),
            contentDescription = null,
            tint = typeColor,
            modifier = Modifier.size(PhoneDimens.recentsRowIconSize)
        )
        Spacer(modifier = Modifier.width(spacing.small))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(id = entry.type.titleRes),
                style = MaterialTheme.typography.bodyMedium,
                color = typeColor
            )
            Text(
                text = detailParts.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
