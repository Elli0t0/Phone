package io.github.elli0t0.phone.feature.settings

import android.content.Intent
import android.telecom.TelecomManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Checkbox
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.elli0t0.phone.PhoneApplication
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.telecom.rememberDefaultDialerState
import io.github.elli0t0.phone.navigation.InnerTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedNumbersScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as PhoneApplication
    val viewModel: BlockedNumbersViewModel = viewModel(
        factory = BlockedNumbersViewModel.provideFactory(app)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = LocalSpacing.current
    val dialerState = rememberDefaultDialerState()

    var showAddDialog by remember { mutableStateOf(false) }
    var numberInput by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf(false) }
    var prefixChecked by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(text = stringResource(id = R.string.blocked_numbers_add)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = numberInput,
                        onValueChange = {
                            numberInput = it
                            inputError = false
                        },
                        label = { Text(text = stringResource(id = R.string.blocked_numbers_hint)) },
                        singleLine = true,
                        isError = inputError,
                        modifier = Modifier.testTag("blocked_number_input")
                    )
                    if (inputError) {
                        Text(
                            text = stringResource(id = R.string.blocked_numbers_invalid),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = prefixChecked,
                            onCheckedChange = { prefixChecked = it },
                            modifier = Modifier.testTag("blocked_number_prefix")
                        )
                        Text(
                            text = stringResource(id = R.string.blocked_numbers_prefix_option),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (numberInput.isBlank()) {
                            inputError = true
                        } else {
                            viewModel.blockNumber(numberInput, prefixChecked)
                            numberInput = ""
                            prefixChecked = false
                            showAddDialog = false
                        }
                    },
                    modifier = Modifier.testTag("block_number_confirm")
                ) {
                    Text(text = stringResource(id = R.string.blocked_numbers_block))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(text = stringResource(id = R.string.action_cancel))
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("blocked_numbers_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState.canBlock) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.testTag("add_blocked_number_button")
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add),
                        contentDescription = stringResource(id = R.string.blocked_numbers_add)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = spacing.screenHorizontalPadding)
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            InnerTopBar(
                onBack = onNavigateBack,
                title = stringResource(id = R.string.blocked_numbers_title)
            )
            if (!uiState.canBlock) {
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(spacing.medium)) {
                        Text(
                            text = stringResource(id = R.string.blocked_numbers_not_available),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!dialerState.isDefault) {
                            Spacer(modifier = Modifier.height(spacing.small))
                            Button(onClick = dialerState.requestDefaultDialer) {
                                Text(text = stringResource(id = R.string.blocked_numbers_set_default))
                            }
                        }
                        Spacer(modifier = Modifier.height(spacing.small))
                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(TelecomManager.ACTION_SHOW_CALL_SETTINGS)
                                    )
                                }
                            }
                        ) {
                            Text(text = stringResource(id = R.string.blocked_numbers_open_system))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(spacing.medium))
            }

            if (!uiState.isLoading && uiState.entries.isNotEmpty()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(text = stringResource(id = R.string.blocked_numbers_search_hint)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = spacing.small)
                        .testTag("blocked_numbers_search")
                )
            }
            val filteredEntries = uiState.entries.filter {
                it.number.contains(searchQuery.trim(), ignoreCase = true)
            }
            if (!uiState.isLoading && uiState.entries.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.blocked_numbers_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = spacing.medium)
                )
            } else if (!uiState.isLoading && filteredEntries.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.blocked_numbers_no_results),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = spacing.medium)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(spacing.small)
                ) {
                    itemsIndexed(
                        filteredEntries,
                        key = { _, entry -> "${entry.isPrefix}:${entry.number}" }
                    ) { index, entry ->
                        Card(
                            shape = MaterialTheme.shapes.extraLarge,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(spacing.medium),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.number,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    if (entry.isPrefix) {
                                        Text(
                                            text = stringResource(id = R.string.blocked_numbers_prefix),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.unblockNumber(entry)
                                        snackbarScope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = context.getString(
                                                    R.string.blocked_numbers_unblocked
                                                ),
                                                actionLabel = context.getString(R.string.action_undo)
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.blockNumber(entry.number, entry.isPrefix)
                                            }
                                        }
                                    },
                                    modifier = Modifier.testTag("unblock_button_$index")
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_delete),
                                        contentDescription = stringResource(id = R.string.blocked_numbers_unblock)
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
