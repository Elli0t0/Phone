package io.github.elli0t0.phone.feature.contacts.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.PhoneBottomSheet
import io.github.elli0t0.phone.data.contacts.ContactsRepository
import io.github.elli0t0.phone.data.contacts.model.ContactPhone
import io.github.elli0t0.phone.data.contacts.model.ContactSummary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Contact picker bottom sheet (0.19.3), shared by the keypad's
 * long-press assign flow and the Speed dial manager: search +
 * contacts list; tapping a contact picks its only number, or shows
 * its numbers to pick from when it has several. Only contacts with
 * a number to dial are listed.
 */
@Composable
fun ContactPickerSheet(
    contactsRepository: ContactsRepository,
    title: String,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    val spacing = LocalSpacing.current
    val scope = rememberCoroutineScope()
    val contacts by contactsRepository.getContactsFlow()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    var query by remember { mutableStateOf("") }
    // Contact whose numbers are being picked (null = contact list).
    var pickingContact by remember { mutableStateOf<ContactSummary?>(null) }
    var pickingNumbers by remember { mutableStateOf<List<ContactPhone>>(emptyList()) }

    val filtered = remember(contacts, query) {
        contacts.filter { contact ->
            !contact.primaryNumberPreview.isNullOrBlank() &&
                (query.isBlank() || contact.displayName.contains(query, ignoreCase = true))
        }
    }

    fun onContactTap(contact: ContactSummary) {
        scope.launch {
            val detail = contactsRepository
                .getContactDetail(contact.lookupKey, contact.id).first()
            val numbers = detail?.phones?.filter { it.number.isNotBlank() }
                ?: emptyList()
            when {
                numbers.size == 1 -> onPick(numbers.first().number)
                numbers.size > 1 -> {
                    pickingContact = contact
                    pickingNumbers = numbers
                }
                // No numbers in the detail (stale preview): fall back
                // to the listed primary number.
                !contact.primaryNumberPreview.isNullOrBlank() ->
                    onPick(contact.primaryNumberPreview)
            }
        }
    }

    PhoneBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.large)
                .padding(bottom = spacing.large)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                if (pickingContact != null) {
                    IconButton(onClick = {
                        pickingContact = null
                        pickingNumbers = emptyList()
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = null
                        )
                    }
                }
                Text(
                    text = if (pickingContact != null) {
                        stringResource(id = R.string.speed_dial_pick_number)
                    } else {
                        title
                    },
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(vertical = spacing.medium)
                )
            }
            if (pickingContact == null) {
                ContactSearchBar(
                    query = query,
                    onQueryChange = { query = it },
                    onClear = { query = "" },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = spacing.small)
                )
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(filtered, key = { it.id }) { contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onContactTap(contact) }
                                .padding(vertical = spacing.small),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(spacing.medium)
                        ) {
                            ContactAvatar(
                                photoUri = contact.photoUri,
                                displayName = contact.displayName,
                                size = 40.dp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = contact.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = contact.primaryNumberPreview ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(pickingNumbers, key = { it.id }) { phone ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(phone.number) }
                                .padding(vertical = spacing.small),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(spacing.medium)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = phone.number,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!phone.label.isNullOrBlank()) {
                                    Text(
                                        text = phone.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
