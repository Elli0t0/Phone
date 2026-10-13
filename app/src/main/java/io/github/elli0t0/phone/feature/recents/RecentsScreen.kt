package io.github.elli0t0.phone.feature.recents

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
import io.github.elli0t0.phone.feature.recents.ui.CallLogRow
import io.github.elli0t0.phone.feature.recents.ui.FavoritesHorizontalSection
import io.github.elli0t0.phone.feature.recents.ui.RecentsEmptyState
import io.github.elli0t0.phone.feature.recents.ui.RecentsFilterChips

@Composable
fun RecentsScreen(
    onNavigateToContactDetail: (Long) -> Unit = {},
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
    val permissionState = rememberCallLogPermissionState()
    val spacing = LocalSpacing.current
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }

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

            // One UI Large Header
            Text(
                text = stringResource(id = R.string.tab_recents),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = spacing.small)
            )

            // Search Bar
            ContactSearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChanged,
                onClear = { viewModel.onSearchQueryChanged("") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = spacing.small)
            )

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
                        // Favorites section if available
                        if (uiState.favorites.isNotEmpty() && !uiState.isSearching) {
                            item(key = "section_favorites") {
                                FavoritesHorizontalSection(
                                    favorites = uiState.favorites,
                                    onContactClick = { contact -> onNavigateToContactDetail(contact.id) },
                                    onCallClick = { contact ->
                                        contact.primaryNumberPreview?.let { num ->
                                            CallManager.placeCall(context, num)
                                        }
                                    },
                                    modifier = Modifier.padding(bottom = spacing.medium)
                                )
                            }
                        }

                        // Empty calls state
                        if (uiState.daySections.isEmpty()) {
                            item(key = "empty_recents") {
                                RecentsEmptyState(
                                    isFiltered = uiState.selectedFilter != CallFilter.ALL || uiState.isSearching
                                )
                            }
                        } else {
                            // Grouped Call List with Day Headers
                            uiState.daySections.forEach { section ->
                                item(key = "header_${section.title}") {
                                    Text(
                                        text = section.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
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
                                        onCallClick = { CallManager.placeCall(context, group.number) },
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
