package io.github.elli0t0.phone.feature.contacts.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.data.contacts.model.ContactGroup
import io.github.elli0t0.phone.data.contacts.model.ContactSummary
import io.github.elli0t0.phone.feature.contacts.GroupsViewModel

/**
 * Lists the device's contact groups. Tapping a group shows its members;
 * tapping a member opens that contact's detail screen.
 */
@Composable
fun GroupsScreen(
    viewModel: GroupsViewModel,
    onBack: () -> Unit,
    onMemberClick: (ContactSummary) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = LocalSpacing.current
    val selectedGroup = uiState.selectedGroup

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("groups_screen")
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
                    onClick = {
                        if (selectedGroup != null) viewModel.clearSelection() else onBack()
                    },
                    modifier = Modifier.testTag("groups_back")
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_back),
                        contentDescription = stringResource(id = R.string.cd_back)
                    )
                }
                Text(
                    text = selectedGroup?.title ?: stringResource(id = R.string.groups_title),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (selectedGroup == null) {
                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    uiState.groups.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(id = R.string.groups_empty_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(spacing.small))
                                Text(
                                    text = stringResource(id = R.string.groups_empty_message),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = spacing.small)
                        ) {
                            items(
                                items = uiState.groups,
                                key = { it.id }
                            ) { group ->
                                GroupRow(
                                    group = group,
                                    onClick = { viewModel.selectGroup(group) }
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }
                }
            } else {
                when {
                    uiState.isLoadingMembers -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    uiState.members.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.groups_no_members),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = spacing.small)
                        ) {
                            item(key = "members") {
                                ContactGroupCard(
                                    contacts = uiState.members,
                                    onContactClick = onMemberClick
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupRow(
    group: ContactGroup,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val countText = if (group.memberCount == 1) {
        stringResource(id = R.string.groups_member_count_single, group.memberCount)
    } else {
        stringResource(id = R.string.groups_member_count_multiple, group.memberCount)
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = spacing.small)
            .clickable(onClick = onClick)
            .testTag("group_item_${group.id}"),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = countText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
