package io.github.elli0t0.phone.feature.contacts.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import io.github.elli0t0.phone.data.contacts.model.DuplicateGroup
import io.github.elli0t0.phone.data.contacts.model.DuplicateReason
import io.github.elli0t0.phone.feature.contacts.ContactsUiState

/**
 * Lists duplicate contacts (same normalized number or same name). Merging a
 * group keeps the first contact, moves the others numbers and emails into it,
 * and deletes the merged contacts — only after the user confirms in the dialog.
 * Nothing is deleted without confirmation.
 */
@Composable
fun DuplicatesScreen(
    uiState: ContactsUiState,
    onBack: () -> Unit,
    onMergeRequest: (DuplicateGroup) -> Unit,
    onDismissMerge: () -> Unit,
    onConfirmMerge: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("duplicates_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = spacing.screenHorizontalPadding)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = spacing.small)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("duplicates_back")
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_back),
                        contentDescription = stringResource(id = R.string.cd_back)
                    )
                }
                Text(
                    text = stringResource(id = R.string.duplicates_title),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = stringResource(id = R.string.duplicates_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = spacing.small, bottom = spacing.medium)
            )

            when {
                uiState.isFindingDuplicates -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.duplicateGroups.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(id = R.string.duplicates_none),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(spacing.medium),
                        // Clearance above the overlaid bottom bar:
                        // bar height + nav-bar inset + 16dp gap.
                        contentPadding = PaddingValues(
                            bottom = PhoneDimens.bottomBarReservedHeight +
                                WindowInsets.navigationBars.asPaddingValues()
                                    .calculateBottomPadding()
                        )
                    ) {
                        items(
                            items = uiState.duplicateGroups,
                            key = { group -> group.contacts.joinToString(",") { it.id.toString() } }
                        ) { group ->
                            DuplicateGroupItem(
                                group = group,
                                onMergeClick = { onMergeRequest(group) }
                            )
                        }
                    }
                }
            }
        }
    }

    val pendingGroup = uiState.pendingMergeGroup
    if (pendingGroup != null) {
        var keepId by remember(pendingGroup) {
            mutableStateOf(pendingGroup.contacts.first().id)
        }
        val kept = pendingGroup.contacts.firstOrNull { it.id == keepId }
            ?: pendingGroup.contacts.first()
        val removedNames = pendingGroup.contacts
            .filter { it.id != kept.id }
            .joinToString(", ") { it.displayName }
        AlertDialog(
            onDismissRequest = { if (!uiState.isMerging) onDismissMerge() },
            title = {
                Text(
                    text = stringResource(id = R.string.duplicates_merge_title),
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(id = R.string.duplicates_choose_keep),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(spacing.small))
                    pendingGroup.contacts.forEach { contact ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { keepId = contact.id }
                                .padding(vertical = spacing.extraSmall)
                        ) {
                            RadioButton(
                                selected = contact.id == keepId,
                                onClick = { keepId = contact.id }
                            )
                            Column(modifier = Modifier.padding(start = spacing.small)) {
                                Text(
                                    text = contact.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                contact.primaryNumberPreview?.takeIf { it.isNotBlank() }?.let { number ->
                                    Text(
                                        text = number,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(spacing.small))
                    Text(
                        text = stringResource(
                            id = R.string.duplicates_merge_message,
                            kept.displayName,
                            removedNames
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onConfirmMerge(keepId) },
                    enabled = !uiState.isMerging
                ) {
                    Text(text = stringResource(id = R.string.duplicates_merge_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissMerge,
                    enabled = !uiState.isMerging
                ) {
                    Text(text = stringResource(id = R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun DuplicateGroupItem(
    group: DuplicateGroup,
    onMergeClick: () -> Unit
) {
    val spacing = LocalSpacing.current

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = when (group.reason) {
                DuplicateReason.SAME_NUMBER -> stringResource(id = R.string.duplicates_reason_number)
                DuplicateReason.SAME_NAME -> stringResource(id = R.string.duplicates_reason_name)
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(spacing.small))
        group.contacts.forEach { contact ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.small)
            ) {
                Text(
                    text = contact.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                contact.primaryNumberPreview?.takeIf { it.isNotBlank() }?.let { number ->
                    Text(
                        text = number,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        OutlinedButton(
            onClick = onMergeClick,
            modifier = Modifier.testTag("duplicates_merge_${group.contacts.firstOrNull()?.id}")
        ) {
            Text(text = stringResource(id = R.string.duplicates_merge))
        }
    }
}
