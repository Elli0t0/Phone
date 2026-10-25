package io.github.elli0t0.phone.feature.contacts.ui

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.design.firstLoadReveal
import io.github.elli0t0.phone.data.calllog.model.CallLogEntry
import io.github.elli0t0.phone.data.calllog.model.CallType
import io.github.elli0t0.phone.feature.contacts.ContactHistoryViewModel
import io.github.elli0t0.phone.navigation.InnerTopBar
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

private enum class HistoryFilter(val labelRes: Int) {
    ALL(R.string.filter_all),
    MISSED(R.string.filter_missed),
    INCOMING(R.string.filter_incoming),
    OUTGOING(R.string.filter_outgoing);

    fun matches(entry: CallLogEntry): Boolean = when (this) {
        ALL -> true
        MISSED -> entry.type == CallType.Missed
        INCOMING -> entry.type == CallType.Incoming
        OUTGOING -> entry.type == CallType.Outgoing
    }
}

private data class DayGroup(val label: String, val calls: List<CallLogEntry>)

// Full call history for one contact, paged 50 at a time in a LazyColumn:
// never rendered in a non-lazy Column, so long histories stay smooth.
@Composable
fun ContactHistoryScreen(
    viewModel: ContactHistoryViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val spacing = LocalSpacing.current
    val listState = rememberLazyListState()
    // First-load reveal, like Recents and Contacts: flips true when the
    // first entries arrive, so the first 8 groups stagger in; later
    // (paged) groups appear immediately.
    var listRevealed by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.entries.isNotEmpty()) {
        if (uiState.entries.isNotEmpty()) listRevealed = true
    }
    var filter by remember { mutableStateOf(HistoryFilter.ALL) }
    val filteredEntries = remember(filter, uiState.entries) {
        uiState.entries.filter { filter.matches(it) }
    }
    // One group per day, newest day first, calls newest-first within
    // each day (entries arrive sorted, and groupBy keeps order).
    val dayGroups = remember(filteredEntries) {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        filteredEntries
            .groupBy {
                Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate()
            }
            .map { (day, calls) ->
                val label = when (day) {
                    today -> context.getString(R.string.history_day_today)
                    yesterday -> context.getString(R.string.history_day_yesterday)
                    else -> DateFormat.getMediumDateFormat(context).format(
                        Date.from(day.atStartOfDay(ZoneId.systemDefault()).toInstant())
                    )
                }
                DayGroup(label, calls)
            }
    }

    // Paging is end-based: the grouped list has one item per day, so
    // a raw entry index would be meaningless. Re-collect when the
    // group count changes so the end test always uses fresh groups.
    LaunchedEffect(listState, dayGroups.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { index ->
                viewModel.loadMoreIfAtEnd(index != null && index >= dayGroups.size - 1)
            }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("contact_history_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = spacing.screenHorizontalPadding)
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            InnerTopBar(
                onBack = onBack,
                title = uiState.contactName.ifBlank { stringResource(id = R.string.contact_details_history) },
                backTestTag = "btn_history_back"
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
                modifier = Modifier.padding(bottom = spacing.small)
            ) {
                HistoryFilter.entries.forEach { option ->
                    FilterChip(
                        selected = filter == option,
                        onClick = { filter = option },
                        label = { Text(text = stringResource(id = option.labelRes)) }
                    )
                }
            }

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                filteredEntries.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(id = R.string.contact_details_history_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().testTag("contact_history_list"),
                        verticalArrangement = Arrangement.spacedBy(spacing.small)
                    ) {
                        itemsIndexed(
                            items = dayGroups,
                            key = { _, group -> group.label }
                        ) { dayIndex, group ->
                            Column(
                                modifier = Modifier
                                    .animateItem()
                                    .firstLoadReveal(dayIndex, listRevealed)
                            ) {
                                Text(
                                    text = group.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(
                                        top = if (dayIndex == 0) 0.dp else spacing.small,
                                        bottom = spacing.extraSmall
                                    )
                                )
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = spacing.medium)
                                    ) {
                                        group.calls.forEachIndexed { callIndex, entry ->
                                            CallHistoryRowItem(entry = entry)
                                            if (callIndex < group.calls.lastIndex) {
                                                HorizontalDivider(
                                                    color = MaterialTheme.colorScheme.outlineVariant.copy(
                                                        alpha = PhoneDimens.recentsDividerAlpha
                                                    ),
                                                    modifier = Modifier.padding(
                                                        start = spacing.medium +
                                                            PhoneDimens.detailRowIconSize + spacing.medium
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (uiState.isLoadingMore) {
                            item(key = "loading_more") {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = spacing.medium),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
