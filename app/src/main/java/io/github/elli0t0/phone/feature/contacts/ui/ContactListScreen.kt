package io.github.elli0t0.phone.feature.contacts.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.permissions.ContactsPermissionState
import io.github.elli0t0.phone.core.permissions.ContactsPermissionStatus
import io.github.elli0t0.phone.data.contacts.model.ContactSummary
import io.github.elli0t0.phone.data.contacts.model.DuplicateGroup
import io.github.elli0t0.phone.feature.contacts.ContactsUiState
import io.github.elli0t0.phone.navigation.PageHeaderItem
import io.github.elli0t0.phone.navigation.PinnedPageBar

@Composable
fun ContactListScreen(
    uiState: ContactsUiState,
    permissionState: ContactsPermissionState,
    onSearchQueryChanged: (String) -> Unit,
    onClearSearch: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onContactClick: (ContactSummary) -> Unit,
    onAddContactClick: () -> Unit,
    onFindDuplicates: () -> Unit,
    onShowFavoritesHint: () -> Unit = {},
    onShowDuplicates: () -> Unit = {},
    onDismissDuplicatesCard: () -> Unit = {},
    onGroupsClick: () -> Unit = {},
    onBackFromDuplicates: () -> Unit,
    onMergeRequest: (DuplicateGroup) -> Unit,
    onDismissMerge: () -> Unit,
    onConfirmMerge: (Long) -> Unit,
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
            when (val status = permissionState.status) {
                is ContactsPermissionStatus.Granted -> {
                    ContactsContent(
                        uiState = uiState,
                        onSearchQueryChanged = onSearchQueryChanged,
                        onClearSearch = onClearSearch,
                        onNavigateToSettings = onNavigateToSettings,
                        onContactClick = onContactClick,
                        onAddContactClick = onAddContactClick,
                        onFindDuplicates = onFindDuplicates,
                        onShowFavoritesHint = onShowFavoritesHint,
                        onShowDuplicates = onShowDuplicates,
                        onDismissDuplicatesCard = onDismissDuplicatesCard,
                        onGroupsClick = onGroupsClick
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
    onSearchQueryChanged: (String) -> Unit,
    onClearSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onContactClick: (ContactSummary) -> Unit,
    onAddContactClick: () -> Unit,
    onFindDuplicates: () -> Unit,
    onShowFavoritesHint: () -> Unit,
    onShowDuplicates: () -> Unit,
    onDismissDuplicatesCard: () -> Unit,
    onGroupsClick: () -> Unit
) {
    val spacing = LocalSpacing.current
    var searchVisible by remember { mutableStateOf(uiState.searchQuery.isNotEmpty()) }
    var menuExpanded by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val contactCount = uiState.allContacts.size
    val countText = if (contactCount == 1) {
        stringResource(id = R.string.contacts_count_single, contactCount)
    } else {
        stringResource(id = R.string.contacts_count_multiple, contactCount)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Pinned top bar: background and title fade in as the list
        // scrolls; the add, search and menu actions stay visible.
        PinnedPageBar(
            title = stringResource(id = R.string.tab_contacts),
            listState = listState
        ) {
            Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.screenHorizontalPadding, vertical = spacing.small),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onAddContactClick,
                modifier = Modifier.testTag("button_add_contact")
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add),
                    contentDescription = stringResource(id = R.string.cd_add_contact),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(
                onClick = {
                    if (searchVisible) onClearSearch()
                    searchVisible = !searchVisible
                },
                modifier = Modifier.testTag("button_search_contacts")
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_search),
                    contentDescription = stringResource(id = R.string.cd_search),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.testTag("button_contacts_menu")
                ) {
                    Icon(
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
                        modifier = Modifier.testTag("button_settings")
                    )
                    DropdownMenuItem(
                        text = { Text(text = stringResource(id = R.string.duplicates_find)) },
                        onClick = { menuExpanded = false; onFindDuplicates() },
                        enabled = !uiState.isFindingDuplicates,
                        modifier = Modifier.testTag("button_find_duplicates")
                    )
                }
            }
        }
        }

        if (searchVisible) {
            ContactSearchBar(
                query = uiState.searchQuery,
                onQueryChange = onSearchQueryChanged,
                onClear = { onClearSearch(); searchVisible = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.screenHorizontalPadding, vertical = spacing.small)
            )
        }

        if (uiState.allContacts.isEmpty() && !uiState.isLoading) {
            EmptyContactsState(onAddContactClick = onAddContactClick)
        } else if (uiState.filteredContacts.isEmpty() && uiState.searchQuery.isNotEmpty()) {
            SearchEmptyState(query = uiState.searchQuery)
        } else {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = spacing.screenHorizontalPadding)
                .testTag("contacts_lazy_column"),
            // Clearance above the overlaid bottom bar:
            // bar height + nav-bar inset + 16dp gap.
            contentPadding = PaddingValues(
                bottom = PhoneDimens.bottomBarReservedHeight +
                    WindowInsets.navigationBars.asPaddingValues()
                        .calculateBottomPadding()
            )
        ) {
            // Large header as the first list item: scrolls away naturally.
            item(key = "contacts_header") {
                PageHeaderItem(
                    title = stringResource(id = R.string.tab_contacts),
                    subtitle = countText
                )
            }
            if (uiState.showDuplicatesCard && uiState.searchQuery.isEmpty()) {
                item(key = "duplicates_card") {
                    DuplicatesCard(
                        duplicateCount = uiState.duplicateGroups.sumOf { it.contacts.size },
                        onMergeClick = onShowDuplicates,
                        onDismiss = onDismissDuplicatesCard
                    )
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

            if (uiState.favorites.isEmpty() && uiState.searchQuery.isEmpty()) {
                item(key = "favorites_hint") {
                    FavoritesHintRow(onClick = onShowFavoritesHint)
                }
            }

            val profileContact = uiState.profileContact
            if (profileContact != null && uiState.searchQuery.isEmpty()) {
                item(key = "header_profile") {
                    Text(
                        text = stringResource(id = R.string.contacts_my_profile),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = spacing.small)
                    )
                }
                item(key = "profile_row") {
                    ProfileRow(
                        contact = profileContact,
                        onClick = { onContactClick(profileContact) }
                    )
                }
            }

            if (uiState.searchQuery.isEmpty()) {
                item(key = "groups_row") {
                    GroupsRow(onClick = onGroupsClick)
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

                item(key = "group_$letter") {
                    ContactGroupCard(
                        contacts = contactsInGroup,
                        onContactClick = onContactClick
                    )
                }
            }
        }
        }
    }
}
