package io.github.elli0t0.phone.feature.contacts.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.permissions.ContactsPermissionState
import io.github.elli0t0.phone.core.permissions.ContactsPermissionStatus
import io.github.elli0t0.phone.data.contacts.model.ContactSummary
import io.github.elli0t0.phone.data.contacts.model.DuplicateGroup
import io.github.elli0t0.phone.feature.contacts.ContactsUiState

@Composable
fun ContactListScreen(
    uiState: ContactsUiState,
    permissionState: ContactsPermissionState,
    onSearchQueryChanged: (String) -> Unit,
    onClearSearch: () -> Unit,
    onContactClick: (ContactSummary) -> Unit,
    onAddContactClick: () -> Unit,
    onFindDuplicates: () -> Unit,
    onBackFromDuplicates: () -> Unit,
    onMergeRequest: (DuplicateGroup) -> Unit,
    onDismissMerge: () -> Unit,
    onConfirmMerge: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("contact_list_screen")
    ) { innerPadding ->
        if (uiState.showDuplicates && permissionState.status is ContactsPermissionStatus.Granted) {
            DuplicatesScreen(
                uiState = uiState,
                onBack = onBackFromDuplicates,
                onMergeRequest = onMergeRequest,
                onDismissMerge = onDismissMerge,
                onConfirmMerge = onConfirmMerge,
                modifier = Modifier.padding(innerPadding)
            )
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            ContactSearchBar(
                query = uiState.searchQuery,
                onQueryChange = onSearchQueryChanged,
                onClear = onClearSearch,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.screenHorizontalPadding, vertical = spacing.small)
            )

            when (val status = permissionState.status) {
                is ContactsPermissionStatus.Granted -> {
                    ContactsContent(
                        uiState = uiState,
                        onContactClick = onContactClick,
                        onAddContactClick = onAddContactClick,
                        onFindDuplicates = onFindDuplicates
                    )
                }
                is ContactsPermissionStatus.NotAsked,
                is ContactsPermissionStatus.Denied -> {
                    PermissionCard(
                        title = stringResource(id = R.string.permission_contacts_title),
                        message = stringResource(id = R.string.permission_contacts_message),
                        buttonText = stringResource(id = R.string.permission_grant_button),
                        onButtonClick = permissionState.requestPermissions,
                        modifier = Modifier.padding(spacing.screenHorizontalPadding)
                    )
                }
                is ContactsPermissionStatus.PermanentlyDenied -> {
                    PermissionCard(
                        title = stringResource(id = R.string.permission_contacts_title),
                        message = stringResource(id = R.string.permission_settings_message),
                        buttonText = stringResource(id = R.string.permission_settings_button),
                        onButtonClick = permissionState.openSettings,
                        modifier = Modifier.padding(spacing.screenHorizontalPadding)
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactsContent(
    uiState: ContactsUiState,
    onContactClick: (ContactSummary) -> Unit,
    onAddContactClick: () -> Unit,
    onFindDuplicates: () -> Unit
) {
    val spacing = LocalSpacing.current

    if (uiState.allContacts.isEmpty() && !uiState.isLoading) {
        EmptyContactsState(onAddContactClick = onAddContactClick)
    } else if (uiState.filteredContacts.isEmpty() && uiState.searchQuery.isNotEmpty()) {
        SearchEmptyState(query = uiState.searchQuery)
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = spacing.screenHorizontalPadding)
                .testTag("contacts_lazy_column")
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = spacing.small, bottom = spacing.medium)
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(id = R.string.app_name),
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        val count = uiState.allContacts.size
                        val countText = if (count == 1) {
                            stringResource(id = R.string.contacts_count_single, count)
                        } else {
                            stringResource(id = R.string.contacts_count_multiple, count)
                        }
                        Text(
                            text = countText,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (uiState.searchQuery.isEmpty()) {
                            TextButton(
                                onClick = onFindDuplicates,
                                enabled = !uiState.isFindingDuplicates,
                                modifier = Modifier.testTag("button_find_duplicates")
                            ) {
                                Text(text = stringResource(id = R.string.duplicates_find))
                            }
                        }
                    }
                    IconButton(
                        onClick = onAddContactClick,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .testTag("button_add_contact")
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_add),
                            contentDescription = stringResource(id = R.string.cd_add_contact),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (uiState.favorites.isNotEmpty() && uiState.searchQuery.isEmpty()) {
                item(key = "header_favorites") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = spacing.small)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_star),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(spacing.extraSmall))
                        Text(
                            text = stringResource(id = R.string.section_favorites),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                uiState.favorites.chunked(3).forEachIndexed { rowIndex, rowContacts ->
                    item(key = "favorites_row_$rowIndex") {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(spacing.small),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = spacing.small)
                        ) {
                            rowContacts.forEach { contact ->
                                FavoriteContactCard(
                                    contact = contact,
                                    onClick = { onContactClick(contact) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - rowContacts.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            uiState.groupedContacts.forEach { (letter, contactsInGroup) ->
                item(key = "header_$letter") {
                    Text(
                        text = letter,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = spacing.small)
                    )
                }

                items(
                    items = contactsInGroup,
                    key = { it.id }
                ) { contact ->
                    ContactRow(
                        contact = contact,
                        onClick = { onContactClick(contact) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
