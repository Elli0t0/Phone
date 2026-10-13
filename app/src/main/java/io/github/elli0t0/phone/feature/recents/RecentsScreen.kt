package io.github.elli0t0.phone.feature.recents

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.elli0t0.phone.PhoneApplication
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.permissions.CallLogPermissionStatus
import io.github.elli0t0.phone.core.permissions.rememberCallLogPermissionState
import io.github.elli0t0.phone.core.telecom.CallManager
import io.github.elli0t0.phone.feature.contacts.ui.ContactSearchBar
import io.github.elli0t0.phone.feature.contacts.ui.PermissionCard
import io.github.elli0t0.phone.feature.setup.SetupWarningCard
import io.github.elli0t0.phone.feature.recents.ui.CallLogRow
import io.github.elli0t0.phone.feature.recents.ui.RecentsEmptyState
import io.github.elli0t0.phone.feature.recents.ui.RecentsFilterChips
import kotlinx.coroutines.launch

@Composable
fun RecentsScreen(
    onNavigateToContactDetail: (Long) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as PhoneApplication
    val viewModel: RecentsViewModel = viewModel(
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
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    var simPickerTargetNumber by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var searchVisible by remember { mutableStateOf(uiState.searchQuery.isNotEmpty()) }
    var menuExpanded by remember { mutableStateOf(false) }
    val simAccounts = remember(context) { io.github.elli0t0.phone.core.telecom.TelecomHelper.getCallCapablePhoneAccounts(context) }
    val recentsScope = androidx.compose.runtime.rememberCoroutineScope()

    fun initiateCall(number: String, lookupKey: String? = null) {
        if (simAccounts.size <= 1) {
            CallManager.placeCall(context, number)
            return
        }
        recentsScope.launch {
            when (val res = io.github.elli0t0.phone.core.telecom.SimCallHelper.resolveSim(
                context, number, lookupKey, simAccounts, app.container.contactSimRepository
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

    LaunchedEffect(uiState.userMessageRes) {
        uiState.userMessageRes?.let { resId ->
            snackbarHostState.showSnackbar(context.getString(resId))
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("recents_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = spacing.screenHorizontalPadding)
        ) {
            Spacer(modifier = Modifier.height(spacing.screenTopPadding))

            SetupWarningCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = spacing.small)
            )

            // Centered app title with search and menu actions
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = spacing.small)
            ) {
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.align(Alignment.Center)
                )

                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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

            when (val status = permissionState.status) {
                is CallLogPermissionStatus.Granted -> {
                    // Filter Chips
                    RecentsFilterChips(
                        selectedFilter = uiState.selectedFilter,
                        onFilterSelected = viewModel::onFilterSelected,
                        modifier = Modifier.padding(bottom = spacing.small)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("recents_call_list")
                    ) {
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
                                    isFiltered = uiState.selectedFilter != CallFilter.ALL || uiState.isSearching,
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
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = spacing.small, bottom = spacing.extraSmall)
                                            .testTag("day_header_${section.title}")
                                    )
                                }

                                items(section.calls, key = { it.id }) { group ->
                                    CallLogRow(
                                        group = group,
                                        isExpanded = uiState.expandedCallId == group.id,
                                        canWriteCallLog = permissionState.hasWritePermission,
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
                                            val intent = Intent(Intent.ACTION_INSERT).apply {
                                                type = ContactsContract.Contacts.CONTENT_TYPE
                                                putExtra(ContactsContract.Intents.Insert.PHONE, group.number)
                                            }
                                            context.startActivity(intent)
                                        },
                                        onCopyNumberClick = {
                                            clipboardManager.setText(AnnotatedString(group.number))
                                        },
                                        onDeleteClick = { viewModel.deleteCall(group.id) }
                                    )
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

