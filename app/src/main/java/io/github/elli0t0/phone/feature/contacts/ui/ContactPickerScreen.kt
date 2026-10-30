package io.github.elli0t0.phone.feature.contacts.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.data.contacts.model.ContactSummary
import io.github.elli0t0.phone.feature.contacts.ContactsUiState

/**
 * In-app contact picker for "add number to an existing contact": the
 * same search state and grouping as the contacts list (same
 * normalizer), so picking feels identical to finding a contact.
 */
@Composable
fun ContactPickerScreen(
    uiState: ContactsUiState,
    onSearchQueryChanged: (String) -> Unit,
    onClearSearch: () -> Unit,
    onContactClick: (ContactSummary) -> Unit,
    onBack: () -> Unit,
    titleRes: Int = R.string.add_to_existing_contact,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = spacing.screenHorizontalPadding)
            .testTag("contact_picker_screen")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = spacing.small)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("contact_picker_back")
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = stringResource(id = R.string.cd_back)
                )
            }
            Text(
                text = stringResource(id = titleRes),
                style = MaterialTheme.typography.titleLarge
            )
        }
        Spacer(modifier = Modifier.height(spacing.small))
        ContactSearchBar(
            query = uiState.searchQuery,
            onQueryChange = onSearchQueryChanged,
            onClear = onClearSearch
        )
        Spacer(modifier = Modifier.height(spacing.small))
        val searching = uiState.searchQuery.isNotBlank()
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (searching) {
                items(uiState.filteredContacts, key = { it.id }) { contact ->
                    ContactRow(
                        contact = contact,
                        onClick = { onContactClick(contact) }
                    )
                }
            } else {
                uiState.groupedContacts.forEach { (letter, contacts) ->
                    item(key = "picker_header_$letter") {
                        Text(
                            text = letter,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = spacing.extraSmall)
                        )
                    }
                    items(contacts, key = { it.id }) { contact ->
                        ContactRow(
                            contact = contact,
                            onClick = { onContactClick(contact) }
                        )
                    }
                }
            }
        }
    }
}
