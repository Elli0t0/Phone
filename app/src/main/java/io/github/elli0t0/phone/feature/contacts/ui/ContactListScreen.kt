package io.github.elli0t0.phone.feature.contacts.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.permissions.ContactsPermissionState
import io.github.elli0t0.phone.core.permissions.ContactsPermissionStatus
import io.github.elli0t0.phone.data.contacts.model.ContactSummary
import io.github.elli0t0.phone.feature.contacts.ContactsUiState

@Composable
fun ContactListScreen(
    uiState: ContactsUiState,
    permissionState: ContactsPermissionState,
    onSearchQueryChanged: (String) -> Unit,
    onClearSearch: () -> Unit,
    onContactClick: (ContactSummary) -> Unit,
    onAddContactClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("contact_list_screen"),
        floatingActionButton = {
            if (permissionState.status is ContactsPermissionStatus.Granted) {
                FloatingActionButton(
                    onClick = onAddContactClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_add_contact")
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add),
                        contentDescription = stringResource(id = R.string.cd_add_contact)
                    )
                }
            }
        }
    ) { innerPadding ->
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
                        onAddContactClick = onAddContactClick
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
    onAddContactClick: () -> Unit
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
                Column(modifier = Modifier.padding(top = spacing.small, bottom = spacing.medium)) {
                    Text(
                        text = stringResource(id = R.string.tab_contacts),
                        style = MaterialTheme.typography.displayLarge,
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
                }
            }

            if (uiState.favorites.isNotEmpty() && uiState.searchQuery.isEmpty()) {
                item(key = "header_favorites") {
                    Text(
                        text = stringResource(id = R.string.section_favorites),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = spacing.small)
                    )
                }

                items(
                    items = uiState.favorites,
                    key = { "fav_${it.id}" }
                ) { contact ->
                    ContactRow(
                        contact = contact,
                        onClick = { onContactClick(contact) }
                    )
                }
            }

            uiState.groupedContacts.forEach { (letter, contactsInGroup) ->
                item(key = "header_$letter") {
                    Text(
                        text = letter,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
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
