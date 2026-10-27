package io.github.elli0t0.phone.feature.recents

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.elli0t0.phone.LocalAppSnackbarHostState
import io.github.elli0t0.phone.PhoneApplication
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.Motion
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.design.dialogEnter
import io.github.elli0t0.phone.core.design.liquidGlass
import io.github.elli0t0.phone.core.permissions.CallLogPermissionStatus
import io.github.elli0t0.phone.core.permissions.rememberCallLogPermissionState
import io.github.elli0t0.phone.core.common.PhoneNumberFormatter
import io.github.elli0t0.phone.core.telecom.CallManager
import io.github.elli0t0.phone.feature.contacts.ui.ContactSearchBar
import io.github.elli0t0.phone.feature.contacts.ui.PermissionCard
import io.github.elli0t0.phone.feature.setup.SetupWarningCard
import io.github.elli0t0.phone.feature.recents.ui.CallLogRow
import io.github.elli0t0.phone.feature.recents.ui.DaySectionCard
import io.github.elli0t0.phone.feature.recents.ui.RecentsEmptyState
import io.github.elli0t0.phone.navigation.FloatingActionsPill
import io.github.elli0t0.phone.navigation.BottomListScrim
import io.github.elli0t0.phone.navigation.PageHeaderItem
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
    val clipboardManager = LocalClipboardManager.current
    // App-level snackbar host (MainActivity): one host above the
    // floating bar for every screen, no per-screen host under the bar.
    val snackbarHostState = LocalAppSnackbarHostState.current

    var simPickerTargetNumber by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    // Unknown number chosen for in-app "Add to contacts": shows the
    // create-new / add-to-existing chooser dialog.
    var addNumber by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var searchVisible by remember { mutableStateOf(uiState.searchQuery.isNotEmpty()) }
    var menuExpanded by remember { mutableStateOf(false) }
    var filterMenuExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val simAccounts = remember(context) { io.github.elli0t0.phone.core.telecom.TelecomHelper.getCallCapablePhoneAccounts(context) }
    val recentsScope = androidx.compose.runtime.rememberCoroutineScope()

    fun initiateCall(number: String, lookupKey: String? = null) {
        if (PhoneNumberUtils.isEmergencyNumber(PhoneNumberFormatter.normalizeDigits(number))) {
            // Emergency: skip the SIM picker and saved-SIM lookup
            // entirely; placeCall hands emergency numbers to the
            // system dialer.
            CallManager.placeCall(context, number)
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
        modifier = modifier.fillMaxSize().testTag("recents_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = spacing.screenHorizontalPadding)
        ) {
            SetupWarningCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = spacing.small)
            )

            // Header actions: filter, search, menu. Shared by the header's
            // action row and the floating pill (same lambda, one menu).
            val headerActions: @Composable RowScope.() -> Unit = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        androidx.compose.material3.IconButton(
                            onClick = { filterMenuExpanded = true },
                            modifier = Modifier.testTag("button_recents_filter")
                        ) {
                            androidx.compose.material3.Icon(
                                painter = painterResource(id = R.drawable.ic_filter),
                                contentDescription = stringResource(id = R.string.cd_filter),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = filterMenuExpanded,
                            onDismissRequest = { filterMenuExpanded = false },
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(PhoneDimens.recentsFilterPopupCornerRadius)
                                )
                                .liquidGlass(
                                    shape = RoundedCornerShape(PhoneDimens.recentsFilterPopupCornerRadius),
                                    floating = true
                                )
                                .dialogEnter(),
                            shape = RoundedCornerShape(PhoneDimens.recentsFilterPopupCornerRadius),
                            containerColor = Color.Transparent,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp
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
                    Box {
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
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(text = stringResource(id = R.string.settings_title)) },
                                onClick = { menuExpanded = false; onNavigateToSettings() },
                                modifier = Modifier.testTag("settings_button")
                            )
                        }
                    }
                }
            }

            // Search Bar with "Search calls" placeholder, shown when the search icon is tapped
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
                        .padding(bottom = spacing.small)
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
            // Bottom scrim behind the list (never over the cards).
            BottomListScrim(modifier = Modifier.align(Alignment.BottomCenter))
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
                                PageHeaderItem(
                                    title = stringResource(id = R.string.app_name),
                                    subtitle = null,
                                    modifier = Modifier
                                        .animateItem(),
                                    actions = headerActions
                                )
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
                        if (uiState.isLoading && uiState.daySections.isEmpty()) {
                            items(count = 5, key = { "skeleton_$it" }) {
                                RecentsLoadingSkeletonItem()
                            }
                        } else if (uiState.errorMessage == null && uiState.daySections.isEmpty()) {
                            item(key = "empty_recents") {
                                RecentsEmptyState(
                                    isFiltered = uiState.selectedFilter != CallFilter.ALL || uiState.isSearching || uiState.selectedDate != null,
                                    modifier = Modifier.fillParentMaxHeight()
                                )
                            }
                        } else if (uiState.errorMessage == null) {

                            // Grouped Call List with Day Headers
                            uiState.daySections.forEach { section ->
                                item(key = "header_${section.title}") {
                                    Text(
                                        text = section.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier
                                            .animateItem()
                                            .fillMaxWidth()
                                            .padding(top = spacing.small, bottom = spacing.extraSmall)
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
                                            canWriteCallLog = permissionState.hasWritePermission,
                                            showSim = simAccounts.size > 1,
                                            onRowClick = { viewModel.onToggleExpandCall(group.id) },
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
                                            onCopyNumberClick = {
                                                clipboardManager.setText(AnnotatedString(group.number))
                                            },
                                            onDeleteClick = { viewModel.deleteCall(group.id) },
                                            showDivider = false
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
            // Floating glass pill with the header actions, overlaid at the
            // top-right of the list area once the header scrolls off.
            FloatingActionsPill(
                listState = listState,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp),
                actions = headerActions
            )
            }
        }
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

