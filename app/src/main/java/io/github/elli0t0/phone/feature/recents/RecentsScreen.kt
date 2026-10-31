package io.github.elli0t0.phone.feature.recents

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import io.github.elli0t0.phone.core.design.GlassDropdownMenu
import io.github.elli0t0.phone.core.design.ConfirmCallSheet
import io.github.elli0t0.phone.core.design.PendingCall
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import io.github.elli0t0.phone.core.design.LocalMinimal
import io.github.elli0t0.phone.core.design.PhoneBottomSheet
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.elli0t0.phone.LocalAppSnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import io.github.elli0t0.phone.PhoneApplication
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.LiquidGlassPopupSurface
import io.github.elli0t0.phone.core.design.Motion
import io.github.elli0t0.phone.data.calllog.util.CallLogGroupingHelper
import io.github.elli0t0.phone.data.recordings.RecordingEntity
import io.github.elli0t0.phone.data.recordings.RecordingMatcher
import io.github.elli0t0.phone.data.recordings.RecordingQuickPlayer
import io.github.elli0t0.phone.data.settings.model.AnimationOption
import io.github.elli0t0.phone.data.settings.model.HeaderSize
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.design.dialogEnter
import io.github.elli0t0.phone.core.design.liquidGlass
import io.github.elli0t0.phone.core.permissions.CallLogPermissionStatus
import io.github.elli0t0.phone.core.permissions.rememberCallLogPermissionState
import io.github.elli0t0.phone.core.common.PhoneNumberFormatter
import io.github.elli0t0.phone.core.telecom.CallManager
import io.github.elli0t0.phone.feature.contacts.ui.ContactSearchBar
import io.github.elli0t0.phone.feature.contacts.ui.ContactsPillContainer
import io.github.elli0t0.phone.feature.contacts.ui.PermissionCard
import io.github.elli0t0.phone.feature.setup.SetupWarningCard
import io.github.elli0t0.phone.feature.recents.ui.CallLogRow
import io.github.elli0t0.phone.feature.recents.ui.DaySectionCard
import io.github.elli0t0.phone.feature.recents.ui.RecentsEmptyState

import io.github.elli0t0.phone.navigation.LocalSetSelectionBarState
import io.github.elli0t0.phone.navigation.SelectionBarState
import io.github.elli0t0.phone.navigation.StatusBarGradientScrim
import io.github.elli0t0.phone.navigation.FloatingActionsPill
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentsScreen(
    onNavigateToContactDetail: (Long) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToRecordings: () -> Unit = {},
    onCreateContact: (String) -> Unit = {},
    onAddToExistingContact: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as PhoneApplication
    // Activity-scoped: the list data survives tab switches, so returning
    // to the tab shows the previous list instantly (no skeleton, no empty
    // state, no re-query).
    val viewModel: RecentsViewModel = viewModel(
        viewModelStoreOwner = context as ComponentActivity,
        factory = RecentsViewModel.provideFactory(
            callLogRepository = app.container.callLogRepository,
            favoritesRepository = app.container.favoritesRepository,
            contactsRepository = app.container.contactsRepository
        )
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Recordings matched to call rows, for the per-row playback
    // icon. Matched once per uiState/recordings change, not per row.
    val recordings by app.container.recordingsRepository.recordings
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val recordingByCallId = remember(uiState.daySections, recordings) {
        if (recordings.isEmpty()) emptyMap<Long, RecordingEntity>()
        else RecordingMatcher.match(
            uiState.daySections.flatMap { s -> s.calls.flatMap { it.calls } },
            recordings
        )
    }
    // RECORDED narrows here, in the screen: the ViewModel has no
    // recordings, so its RECORDED branch passes everything through
    // and this keeps only groups with a matched recording.
    val displayedSections = if (uiState.selectedFilter == CallFilter.RECORDED) {
        uiState.daySections.mapNotNull { section ->
            val calls = section.calls.filter { group ->
                group.calls.any { it.id in recordingByCallId }
            }
            if (calls.isEmpty()) null else section.copy(calls = calls)
        }
    } else {
        uiState.daySections
    }
    // "N calls today", counted from the displayed (possibly
    // narrowed) sections, so it always matches the list below.
    val callsToday = displayedSections
        .flatMap { it.calls }
        .flatMap { it.calls }
        .count { CallLogGroupingHelper.isSameDay(it.date, System.currentTimeMillis()) }
    val quickPlayer = remember { RecordingQuickPlayer(context) }
    DisposableEffect(Unit) { onDispose { quickPlayer.stop() } }
    val permissionState = rememberCallLogPermissionState(
        onPermissionChanged = { isGranted ->
            if (isGranted) {
                viewModel.reload()
            }
        }
    )
    val spacing = LocalSpacing.current
    // Read here, not inside initiateCall: the handler is not a
    // @Composable context, so it cannot read a CompositionLocal.
    val defaultSim = LocalAppSettings.current.defaultSim
    val confirmBeforeCall = LocalAppSettings.current.confirmBeforeCall
    val clipboardManager = LocalClipboardManager.current
    // App-level snackbar host (MainActivity): one host above the
    // floating bar for every screen, no per-screen host under the bar.
    val snackbarHostState = LocalAppSnackbarHostState.current

    var simPickerTargetNumber by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var pendingConfirmCall by remember { androidx.compose.runtime.mutableStateOf<PendingCall?>(null) }
    // Unknown number chosen for in-app "Add to contacts": shows the
    // create-new / add-to-existing chooser dialog.
    var addNumber by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var searchVisible by remember { mutableStateOf(uiState.searchQuery.isNotEmpty()) }
    var filterMenuExpanded by remember { mutableStateOf(false) }
    // Hoisted: the LazyColumn items and the filter button's onClick
    // are not @Composable scopes, so they use this plain Boolean.
    val minimal = LocalMinimal.current
    // Minimal: the filter icon opens this sheet instead of the
    // dropdown menu, and instead of the filter chips row.
    var showFilterSheet by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val simAccounts = remember(context) { io.github.elli0t0.phone.core.telecom.TelecomHelper.getCallCapablePhoneAccounts(context) }
    val recentsScope = androidx.compose.runtime.rememberCoroutineScope()

    fun initiateCall(number: String, lookupKey: String? = null, confirmed: Boolean = false) {
        if (PhoneNumberUtils.isEmergencyNumber(PhoneNumberFormatter.normalizeDigits(number))) {
            // Emergency: skip the SIM picker and saved-SIM lookup
            // entirely; placeCall hands emergency numbers to the
            // system dialer.
            CallManager.placeCall(context, number)
            return
        }
        // Confirm-first (0.18.6): the sheet re-enters here confirmed,
        // so the SIM flow below runs exactly once per tap.
        if (confirmBeforeCall && !confirmed) {
            pendingConfirmCall = PendingCall(number = number, lookupKey = lookupKey, label = number)
            return
        }
        if (simAccounts.size <= 1) {
            CallManager.placeCall(context, number)
            return
        }
        recentsScope.launch {
            when (val res = io.github.elli0t0.phone.core.telecom.SimCallHelper.resolveSim(
                context, number, lookupKey, simAccounts, app.container.contactSimRepository,
                defaultSim
            )) {
                is io.github.elli0t0.phone.core.telecom.SimCallResolution.UseSim ->
                    CallManager.placeCall(context, number, res.sim.handle)
                is io.github.elli0t0.phone.core.telecom.SimCallResolution.ShowDialog ->
                    simPickerTargetNumber = number
            }
        }
    }

    // Bulk delete with Undo: the rows hide at once, the call-log
    // entries are removed only when the snackbar resolves without
    // Undo (or the screen goes away, so a pending delete never
    // lingers). One snackbar for the whole batch.
    fun deleteCallsWithUndo(callIds: Collection<Long>) {
        if (callIds.isEmpty()) return
        viewModel.clearSelection()
        viewModel.requestDelete(callIds)
        recentsScope.launch {
            var undone = false
            try {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(R.string.snackbar_calls_deleted, callIds.size),
                    actionLabel = context.getString(R.string.action_undo),
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.undoDelete(callIds)
                    undone = true
                }
            } finally {
                if (!undone) {
                    viewModel.confirmDelete(callIds)
                }
            }
        }
    }

    // Share the selection as plain text: one line per call, newest
    // first, grouped by day; grouped rows expand to their calls.
    fun shareSelectedCalls() {
        val selectedIds = uiState.selection.ids
        if (selectedIds.isEmpty()) return
        val dateFormat = java.text.DateFormat.getDateTimeInstance(
            java.text.DateFormat.MEDIUM,
            java.text.DateFormat.SHORT
        )
        val text = buildString {
            for (section in displayedSections) {
                val groups = section.calls.filter { it.id in selectedIds }
                if (groups.isEmpty()) continue
                appendLine(section.title)
                for (group in groups) {
                    for (call in group.calls) {
                        val duration = if (call.durationSeconds > 0) {
                            val mins = call.durationSeconds / 60
                            val secs = call.durationSeconds % 60
                            "%d:%02d".format(mins, secs)
                        } else {
                            "0:00"
                        }
                        appendLine(
                            "${call.displayTitle} · ${call.formattedNumber.ifBlank { call.number }} · " +
                                "${context.getString(call.type.titleRes)} · " +
                                "${dateFormat.format(java.util.Date(call.date))} · $duration"
                        )
                    }
                }
            }
        }
        if (text.isBlank()) return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(
            Intent.createChooser(intent, context.getString(R.string.action_share))
        )
    }

    fun copySelectedNumbers() {
        val selectedIds = uiState.selection.ids
        val numbers = displayedSections
            .flatMap { it.calls }
            .filter { it.id in selectedIds }
            .map { it.number }
            .filter { it.isNotBlank() }
            .distinct()
        if (numbers.isNotEmpty()) {
            clipboardManager.setText(AnnotatedString(numbers.joinToString("\n")))
        }
    }

    // Selection bar: published hoisted so MainActivity swaps the
    // bottom bar; Back exits selection mode.
    val selection = uiState.selection
    val setSelectionBar = LocalSetSelectionBarState.current
    val displayedGroupIds = displayedSections.flatMap { it.calls }.map { it.id }
    BackHandler(enabled = selection.isActive) { viewModel.clearSelection() }
    LaunchedEffect(selection, displayedSections) {
        if (selection.isActive) {
            val selectedGroups = displayedSections
                .flatMap { it.calls }
                .filter { it.id in selection.ids }
            val allSelected = displayedGroupIds.isNotEmpty() &&
                selectedGroups.size == displayedGroupIds.size
            setSelectionBar(
                SelectionBarState(
                    title = selection.ids.size.toString(),
                    allSelected = allSelected,
                    onClose = { viewModel.clearSelection() },
                    onToggleSelectAll = {
                        if (allSelected) {
                            viewModel.clearSelection()
                        } else {
                            viewModel.selectAllSelection(displayedGroupIds)
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { shareSelectedCalls() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_share),
                                contentDescription = stringResource(id = R.string.action_share),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (permissionState.hasWritePermission) {
                            IconButton(
                                onClick = {
                                    deleteCallsWithUndo(
                                        selectedGroups.flatMap { group ->
                                            group.calls.map { it.id }
                                        }
                                    )
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_delete),
                                    contentDescription = stringResource(id = R.string.action_delete_call),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Box {
                            var moreExpanded by remember { mutableStateOf(false) }
                            IconButton(
                                onClick = { moreExpanded = true },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_more_vert),
                                    contentDescription = stringResource(id = R.string.action_more),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            GlassDropdownMenu(
                                expanded = moreExpanded,
                                onDismissRequest = { moreExpanded = false }
                            ) {
                                if (selectedGroups.size == 1) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(id = R.string.action_copy_number)) },
                                        onClick = { copySelectedNumbers(); moreExpanded = false }
                                    )
                                    val single = selectedGroups.first()
                                    if (single.primaryCall.contactId != null) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(id = R.string.action_open_contact)) },
                                            onClick = {
                                                single.primaryCall.contactId?.let { id ->
                                                    onNavigateToContactDetail(id)
                                                }
                                                moreExpanded = false
                                            }
                                        )
                                    } else {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(id = R.string.action_add_contact)) },
                                            onClick = {
                                                addNumber = single.number
                                                moreExpanded = false
                                            }
                                        )
                                    }
                                } else {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(id = R.string.action_copy_numbers)) },
                                        onClick = { copySelectedNumbers(); moreExpanded = false }
                                    )
                                }
                            }
                        }
                    }
                )
            )
        } else {
            setSelectionBar(null)
        }
    }
    DisposableEffect(Unit) { onDispose { setSelectionBar(null) } }

    if (simPickerTargetNumber != null) {
        io.github.elli0t0.phone.feature.call.ui.DualSimPickerDialog(
            simAccounts = simAccounts,
            targetNumber = simPickerTargetNumber.orEmpty(),
            onSimSelected = { sim ->
                val num = simPickerTargetNumber.orEmpty()
                simPickerTargetNumber = null
                CallManager.placeCall(context, num, sim.handle)
            },
            onDismiss = { simPickerTargetNumber = null }
        )
    }

    pendingConfirmCall?.let { pending ->
        ConfirmCallSheet(
            pending = pending,
            onConfirm = {
                pendingConfirmCall = null
                initiateCall(pending.number, pending.lookupKey, confirmed = true)
            },
            onDismiss = { pendingConfirmCall = null }
        )
    }

    if (addNumber != null) {
        val numberToAdd = addNumber.orEmpty()
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { addNumber = null },
            title = { Text(stringResource(id = R.string.action_add_contact)) },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            addNumber = null
                            onCreateContact(numberToAdd)
                        }
                    ) {
                        Text(stringResource(id = R.string.dialpad_create_contact))
                    }
                    TextButton(
                        onClick = {
                            addNumber = null
                            onAddToExistingContact(numberToAdd)
                        }
                    ) {
                        Text(stringResource(id = R.string.add_to_existing_contact))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { addNumber = null }) {
                    Text(stringResource(id = R.string.action_cancel))
                }
            }
        )
    }

    LaunchedEffect(uiState.userMessageRes) {
        uiState.userMessageRes?.let { resId ->
            snackbarHostState.showSnackbar(context.getString(resId))
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("recents_screen"),
        // Transparent so the ambient backdrop shows through the
        // gaps; only the list cards stay opaque.
        containerColor = Color.Transparent
    ) { innerPadding ->
            // Header actions: filter, search, menu. Declared here (not
            // in the Column) so both the header's action row and the
            // sticky bar in the outer box share one lambda, one menu.
            val headerActions: @Composable RowScope.() -> Unit = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        androidx.compose.material3.IconButton(
                            onClick = {
                                if (minimal) {
                                    showFilterSheet = true
                                } else {
                                    filterMenuExpanded = true
                                }
                            },
                            modifier = Modifier.testTag("button_recents_filter")
                        ) {
                            androidx.compose.material3.Icon(
                                painter = painterResource(id = R.drawable.ic_filter),
                                contentDescription = stringResource(id = R.string.cd_filter),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        GlassDropdownMenu(
                            expanded = filterMenuExpanded,
                            onDismissRequest = { filterMenuExpanded = false },
                            modifier = Modifier.dialogEnter()
                        ) {
                            val animationOption = LocalAppSettings.current.animationOption
                            CallFilter.entries.forEachIndexed { index, filter ->
                                AnimatedVisibility(
                                    visible = true,
                                    enter = Motion.popupItemEnter(animationOption, index),
                                    exit = fadeOut()
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(text = stringResource(id = filter.titleRes)) },
                                        leadingIcon = if (filter == uiState.selectedFilter) {
                                            {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_check),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        } else {
                                            null
                                        },
                                        onClick = { filterMenuExpanded = false; viewModel.onFilterSelected(filter) },
                                        modifier = Modifier.testTag("filter_menu_${filter.name.lowercase()}")
                                    )
                                }
                            }
                            HorizontalDivider()
                            AnimatedVisibility(
                                visible = true,
                                enter = Motion.popupItemEnter(animationOption, CallFilter.entries.size),
                                exit = fadeOut()
                            ) {
                                DropdownMenuItem(
                                    text = { Text(text = stringResource(id = R.string.recents_pick_date)) },
                                    leadingIcon = if (uiState.selectedDate != null) {
                                        {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_check),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    } else {
                                        null
                                    },
                                    onClick = { filterMenuExpanded = false; showDatePicker = true },
                                    modifier = Modifier.testTag("filter_menu_date")
                                )
                            }
                        }
                    }
                    androidx.compose.material3.IconButton(
                        onClick = {
                            if (searchVisible) viewModel.onSearchQueryChanged("")
                            searchVisible = !searchVisible
                        },
                        modifier = Modifier.testTag("button_search_recents")
                    ) {
                        androidx.compose.material3.Icon(
                            painter = painterResource(id = R.drawable.ic_search),
                            contentDescription = stringResource(id = R.string.cd_search),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Overflow menu: a glass popup anchored under the
                    // three-dots button with a small gap, scaling in
                    // from the top-right with a spring. A Popup draws
                    // above the list, so it never overlaps the first
                    // row or the header.
                    //
                    // The menu state lives HERE, inside headerActions,
                    // not hoisted: the lambda renders twice (header row
                    // and floating pill), and a shared menuExpanded
                    // showed the popup from both instances at once —
                    // the stale one landing over the title. Each
                    // instance now owns its own menu and its own
                    // anchor height, so only the visible button's
                    // popup can show, directly under that button.
                    var menuExpanded by remember { mutableStateOf(false) }
                    var menuAnchorHeightPx by remember { mutableStateOf(0) }
                    val menuDensity = LocalDensity.current
                    Box(
                        modifier = Modifier.onGloballyPositioned { menuAnchorHeightPx = it.size.height }
                    ) {
                        androidx.compose.material3.IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.testTag("button_recents_menu")
                        ) {
                            androidx.compose.material3.Icon(
                                painter = painterResource(id = R.drawable.ic_more_vert),
                                contentDescription = stringResource(id = R.string.cd_more_options),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (menuExpanded) {
                            Popup(
                                onDismissRequest = { menuExpanded = false },
                                alignment = Alignment.TopEnd,
                                offset = IntOffset(
                                    0,
                                    menuAnchorHeightPx + with(menuDensity) { spacing.small.roundToPx() }
                                ),
                                properties = PopupProperties(focusable = true)
                            ) {
                                val animationOption = LocalAppSettings.current.animationOption
                                var entered by remember { mutableStateOf(false) }
                                LaunchedEffect(Unit) { entered = true }
                                val enterProgress by animateFloatAsState(
                                    targetValue = if (entered) 1f else 0f,
                                    animationSpec = when (animationOption) {
                                        AnimationOption.ON -> spring(dampingRatio = 0.5f, stiffness = 400f)
                                        AnimationOption.REDUCED -> tween(durationMillis = 150)
                                        AnimationOption.OFF -> tween(durationMillis = 0)
                                    },
                                    label = "recentsMenuEnter"
                                )
                                LiquidGlassPopupSurface(
                                    shape = RoundedCornerShape(PhoneDimens.menuCornerRadius),
                                    modifier = Modifier.graphicsLayer {
                                        val scale = if (animationOption == AnimationOption.ON) {
                                            0.85f + 0.15f * enterProgress
                                        } else {
                                            1f
                                        }
                                        scaleX = scale
                                        scaleY = scale
                                        transformOrigin = TransformOrigin(1f, 0f)
                                        alpha = if (animationOption == AnimationOption.OFF) {
                                            1f
                                        } else {
                                            enterProgress.coerceIn(0f, 1f)
                                        }
                                    }
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.recordings_title),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .clickable { menuExpanded = false; onNavigateToRecordings() }
                                            .padding(
                                                horizontal = spacing.medium,
                                                vertical = spacing.small
                                            )
                                            .testTag("recordings_button")
                                    )
                                    Text(
                                        text = stringResource(id = R.string.settings_title),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .clickable { menuExpanded = false; onNavigateToSettings() }
                                            .padding(
                                                horizontal = spacing.medium,
                                                vertical = spacing.small
                                            )
                                            .testTag("settings_button")
                                    )
                                }
                            }
                        }
                    }
                }
            }

        // Outer box at the very top of the screen: the sticky bar
        // lives here (full width, no Scaffold inset above it) while
        // the list keeps its inner padding below.
        Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                // No Scaffold top inset here: the sticky bar covers
                // the status area itself and the header self-insets
                // (RecentsHeaderItem), so the list starts at the top.
                .padding(
                    PaddingValues(
                        start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                        top = 0.dp,
                        end = innerPadding.calculateEndPadding(LocalLayoutDirection.current),
                        bottom = innerPadding.calculateBottomPadding()
                    )
                )
                .padding(horizontal = spacing.screenHorizontalPadding)
        ) {
            SetupWarningCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(bottom = spacing.small)
            )

            // Search Bar with "Search calls" placeholder, shown when the search icon is tapped.
            // Same top inset as the Contacts search bar: without it the
            // pill sits under the status bar whenever search is open
            // (the header, which self-insets, is hidden while searching).
            if (searchVisible) {
                ContactSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChanged,
                    onClear = {
                        viewModel.onSearchQueryChanged("")
                        searchVisible = false
                    },
                    placeholderText = stringResource(id = R.string.recents_search_hint),
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = spacing.small, bottom = spacing.small)
                )
            }

            // Active date filter: tap to pick another date, X to clear.
            if (uiState.selectedDate != null) {
                AssistChip(
                    onClick = { showDatePicker = true },
                    label = {
                        Text(
                            text = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.getDefault())
                                .format(uiState.selectedDate)
                        )
                    },
                    trailingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = stringResource(id = R.string.cd_clear_date_filter),
                            modifier = Modifier.clickable { viewModel.onDateSelected(null) }
                        )
                    },
                    modifier = Modifier
                        .padding(bottom = spacing.small)
                        .testTag("recents_date_filter_chip")
                )
            }

            // Date picker: filters the list to calls on the picked day.
            if (showDatePicker) {
                val pickerState = rememberDatePickerState(
                    initialSelectedDateMillis = uiState.selectedDate
                        ?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                )
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDatePicker = false
                                viewModel.onDateSelected(
                                    pickerState.selectedDateMillis?.let {
                                        LocalDate.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
                                    }
                                )
                            }
                        ) {
                            Text(text = stringResource(id = R.string.action_ok))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text(text = stringResource(id = R.string.action_cancel))
                        }
                    }
                ) {
                    DatePicker(state = pickerState)
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
            // No per-screen scrim here: MainActivity already fades the
            // bottom behind the bar, and a second gradient double-dimmed it.
            when (val status = permissionState.status) {
                is CallLogPermissionStatus.Granted -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("recents_call_list"),
                        // Clearance above the overlaid bottom bar:
                        // bar height + nav-bar inset + 16dp gap.
                        contentPadding = PaddingValues(
                            bottom = PhoneDimens.bottomBarReservedHeight +
                                WindowInsets.navigationBars.asPaddingValues()
                                    .calculateBottomPadding()
                        )
                    ) {
                        // Large header as the first list item: scrolls away naturally.
                        // Hidden while search is open: the search bar
                        // below replaces it, so the title does not eat
                        // a third of the screen above the results.
                        if (!searchVisible) {
                            item(key = "recents_header") {
                                RecentsHeaderItem(
                                    title = stringResource(id = R.string.app_name),
                                    callsToday = callsToday,
                                    modifier = Modifier.animateItem(),
                                    // Same pill as the Contacts actions toolbar;
                                    // the floating pill below keeps the bare
                                    // actions, so this does not double-pill.
                                    actions = headerActions
                                )
                            }
                        }
                        // Filter chips directly under the header, always
                        // visible instead of hidden behind the filter
                        // icon (which stays as the more-filters/date
                        // entry). Scrolls away with the header.
                        // Minimal: no chips — filtering opens from the
                        // filter icon in a bottom sheet instead.
                        if (!minimal) {
                        item(key = "recents_filter_chips") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(bottom = spacing.small)
                                    .animateItem(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    CallFilter.ALL,
                                    CallFilter.MISSED,
                                    CallFilter.OUTGOING,
                                    CallFilter.INCOMING,
                                    CallFilter.RECORDED
                                ).forEach { filter ->
                                    FilterChip(
                                        selected = uiState.selectedFilter == filter,
                                        onClick = { viewModel.onFilterSelected(filter) },
                                        label = { Text(text = stringResource(id = filter.titleRes)) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}")
                                    )
                                }
                            }
                        }
                        }
                        // Error State
                        if (uiState.errorMessage != null) {
                            item(key = "recents_error") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = spacing.large)
                                        .testTag("recents_error_state"),
                                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.recents_error_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(bottom = spacing.extraSmall)
                                    )
                                    Text(
                                        text = stringResource(id = R.string.recents_error_message, uiState.errorMessage.orEmpty()),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Loading skeleton or Empty calls state
                        if (uiState.isLoading && displayedSections.isEmpty()) {
                            items(count = 5, key = { "skeleton_$it" }) {
                                RecentsLoadingSkeletonItem()
                            }
                        } else if (uiState.errorMessage == null && displayedSections.isEmpty()) {
                            item(key = "empty_recents") {
                                RecentsEmptyState(
                                    isFiltered = uiState.selectedFilter != CallFilter.ALL || uiState.isSearching || uiState.selectedDate != null,
                                    modifier = Modifier.fillParentMaxHeight()
                                )
                            }
                        } else if (uiState.errorMessage == null) {

                            // Grouped Call List with Day Headers
                            displayedSections.forEach { section ->
                                item(key = "header_${section.title}") {
                                    Text(
                                        text = section.title,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier
                                            .animateItem()
                                            .fillMaxWidth()
                                            .padding(start = 20.dp, top = spacing.small, bottom = spacing.extraSmall)
                                            .testTag("day_header_${section.title}")
                                    )
                                }

                                item(key = "section_${section.title}") {
                                    DaySectionCard(
                                        calls = section.calls,
                                        modifier = Modifier
                                            .animateItem()
                                    ) { group ->
                                        CallLogRow(
                                            group = group,
                                            isExpanded = uiState.expandedCallId == group.id,
                                            isSelected = group.id in uiState.selection.ids,
                                            selectionActive = uiState.selection.isActive,
                                            onSelectionToggle = { viewModel.toggleSelection(group.id) },
                                            onRowClick = {
                                                viewModel.onToggleExpandCall(group.id)
                                            },
                                            onCallClick = { initiateCall(group.number) },
                                            onMessageClick = {
                                                val uri = Uri.parse("smsto:${Uri.encode(group.number)}")
                                                val intent = Intent(Intent.ACTION_SENDTO, uri)
                                                context.startActivity(intent)
                                            },
                                            onOpenContactClick = {
                                                group.primaryCall.contactId?.let { id ->
                                                    onNavigateToContactDetail(id)
                                                }
                                            },
                                            onAddContactClick = {
                                                addNumber = group.number
                                            },
                                            showSim = simAccounts.size > 1,
                                            showDivider = false,
                                            recordingByCallId = recordingByCallId,
                                            playingRecordingId = quickPlayer.playingId,
                                            onRecordingPlayClick = { rec ->
                                                val error = quickPlayer.toggle(rec)
                                                if (error != null) recentsScope.launch { snackbarHostState.showSnackbar(error) }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                is CallLogPermissionStatus.NotAsked,
                is CallLogPermissionStatus.Denied -> {
                    PermissionCard(
                        title = stringResource(id = R.string.permission_call_log_title),
                        message = stringResource(id = R.string.permission_call_log_message),
                        buttonText = stringResource(id = R.string.permission_grant_button),
                        onButtonClick = permissionState.requestPermission,
                        modifier = Modifier.padding(top = spacing.medium)
                    )
                }
                is CallLogPermissionStatus.PermanentlyDenied -> {
                    PermissionCard(
                        title = stringResource(id = R.string.permission_call_log_title),
                        message = stringResource(id = R.string.permission_settings_message),
                        buttonText = stringResource(id = R.string.permission_settings_button),
                        onButtonClick = permissionState.openSettings,
                        modifier = Modifier.padding(top = spacing.medium)
                    )
                }
            }
            }
        }
        // Status-bar gradient only (no title, no hairline, no band):
        // keeps the status icons readable over scrolling rows, shown
        // only while the floating pill is visible. The pill below
        // draws above it at the top-right.
        StatusBarGradientScrim(
            listState = listState,
            modifier = Modifier.align(Alignment.TopStart)
        )
        // Floating actions pill: the header's buttons, overlaid at
        // the top-right once the header has scrolled off. No title,
        // no full-width band — just the buttons. Hidden while the
        // search bar is open so it never overlaps it.
        if (!searchVisible) {
            FloatingActionsPill(
                listState = listState,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(end = 16.dp, top = 8.dp),
                actions = headerActions,
                minimalTitle = stringResource(id = R.string.app_name)
            )
        }
        // Minimal filter sheet: the same filter options the chips
        // and the dropdown menu offer, as plain rows.
        if (showFilterSheet) {
            PhoneBottomSheet(onDismissRequest = { showFilterSheet = false }) {
                CallFilter.entries.forEach { filter ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showFilterSheet = false
                                viewModel.onFilterSelected(filter)
                            }
                            .padding(
                                horizontal = spacing.medium,
                                vertical = spacing.small
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (filter == uiState.selectedFilter) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_check),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(spacing.small))
                        } else {
                            Spacer(modifier = Modifier.width(24.dp + spacing.small))
                        }
                        Text(
                            text = stringResource(id = filter.titleRes),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = spacing.extraSmall)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showFilterSheet = false
                            showDatePicker = true
                        }
                        .padding(
                            horizontal = spacing.medium,
                            vertical = spacing.small
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.selectedDate != null) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_check),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(spacing.small))
                    } else {
                        Spacer(modifier = Modifier.width(24.dp + spacing.small))
                    }
                    Text(
                        text = stringResource(id = R.string.recents_pick_date),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(spacing.medium))
            }
        }
        }
    }
}

/**
 * Recents-only header: the large title left-aligned with the
 * actions pill on the same row (no separate row above it, so no
 * empty band above the title) and a "N calls today" subtitle
 * under it. Contacts keeps the shared centered header.
 */
@Composable
private fun RecentsHeaderItem(
    title: String,
    callsToday: Int,
    actions: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = when (LocalAppSettings.current.headerSize) {
        HeaderSize.COMPACT -> 0.85f
        HeaderSize.STANDARD -> 1f
        HeaderSize.LARGE -> 1.2f
    }
    // Minimal (0.18.10): tall centered title, plain icon row below,
    // no pill, no subtitle, no filter chips (the filter icon opens
    // a bottom sheet instead).
    if (LocalMinimal.current) {
        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
        Column(
            modifier = modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(screenHeight * 0.38f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.4).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                actions()
            }
        }
        return
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp * scale, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp * scale)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = PhoneDimens.headerTitleExpandedSize * scale,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.4).sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            ContactsPillContainer {
                actions()
            }
        }
        Text(
            text = if (callsToday == 0) {
                stringResource(id = R.string.recents_no_calls_today)
            } else {
                pluralStringResource(
                    id = R.plurals.recents_calls_today,
                    count = callsToday,
                    callsToday
                )
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RecentsLoadingSkeletonItem() {
    val spacing = LocalSpacing.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = spacing.extraSmall)
            .testTag("recents_skeleton_item"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            )
            Spacer(modifier = Modifier.width(spacing.medium))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(18.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                )
                Spacer(modifier = Modifier.height(spacing.extraSmall))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.35f)
                        .height(14.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                )
            }
        }
    }
}

