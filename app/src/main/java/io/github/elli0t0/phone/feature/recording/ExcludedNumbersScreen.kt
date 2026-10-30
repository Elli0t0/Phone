package io.github.elli0t0.phone.feature.recording

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.elli0t0.phone.PhoneApplication
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.bottomBarListBottomPadding
import io.github.elli0t0.phone.feature.contacts.ContactsViewModel
import io.github.elli0t0.phone.feature.contacts.ui.ContactPickerScreen
import io.github.elli0t0.phone.navigation.InnerTopBar

/**
 * Excluded-numbers list for the auto-record EXCEPT_LIST mode
 * (step 6): numbers stored as typed (trimmed) in
 * autoRecordExcluded. Mirrors BlockedNumbersScreen: list with
 * remove, a "+" dialog to type a number, and the in-app contact
 * picker for choosing one. Numbers compare through
 * PhoneNumberMatcher, so a stored "0912 345 6789" also excludes
 * "+989123456789".
 */
@Composable
fun ExcludedNumbersScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as PhoneApplication
    // Same instance as the recording page (same destination owner
    // and factory arguments), so the list stays in sync with it.
    val viewModel: RecordingSettingsViewModel = viewModel(
        factory = RecordingSettingsViewModel.provideFactory(
            app.container.recordingController,
            app.container.recordingsRepository,
            context.applicationContext,
            app.container.settingsRepository
        )
    )
    val excluded by viewModel.autoRecordExcluded.collectAsStateWithLifecycle()
    val spacing = LocalSpacing.current
    var showAddDialog by remember { mutableStateOf(false) }
    var numberInput by remember { mutableStateOf("") }
    var showPicker by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(text = stringResource(id = R.string.recording_excluded_add)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = numberInput,
                        onValueChange = { numberInput = it },
                        label = { Text(text = stringResource(id = R.string.recording_excluded_hint)) },
                        singleLine = true,
                        modifier = Modifier.testTag("excluded_number_input")
                    )
                    TextButton(
                        onClick = {
                            showAddDialog = false
                            showPicker = true
                        }
                    ) {
                        Text(text = stringResource(id = R.string.recording_excluded_pick))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (numberInput.isNotBlank()) {
                            viewModel.addExcludedNumber(numberInput)
                            numberInput = ""
                            showAddDialog = false
                        }
                    },
                    modifier = Modifier.testTag("excluded_number_confirm")
                ) {
                    Text(text = stringResource(id = R.string.recording_excluded_add))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(text = stringResource(id = R.string.action_cancel))
                }
            }
        )
    }

    if (showPicker) {
        // The picker paints no background of its own: cover the
        // list behind it with the theme background.
        val listViewModel: ContactsViewModel = viewModel(
            factory = ContactsViewModel.provideFactory(
                app.container.contactsRepository,
                app.container.favoritesRepository,
                app.container.settingsRepository
            )
        )
        val pickerState by listViewModel.uiState.collectAsStateWithLifecycle()
        Box(
            modifier = Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            ContactPickerScreen(
                uiState = pickerState,
                onSearchQueryChanged = listViewModel::onSearchQueryChanged,
                onClearSearch = listViewModel::clearSearchQuery,
                onContactClick = { contact ->
                    contact.primaryNumberPreview?.takeIf { it.isNotBlank() }?.let {
                        viewModel.addExcludedNumber(it)
                    }
                    listViewModel.clearSearchQuery()
                    showPicker = false
                },
                onBack = {
                    listViewModel.clearSearchQuery()
                    showPicker = false
                },
                titleRes = R.string.recording_excluded_pick
            )
        }
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize().testTag("excluded_numbers_screen"),
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.testTag("add_excluded_number_button")
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add),
                        contentDescription = stringResource(id = R.string.recording_excluded_add)
                    )
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
                    title = stringResource(id = R.string.recording_excluded_title)
                )
                Spacer(modifier = Modifier.height(spacing.small))
                if (excluded.isEmpty()) {
                    Text(
                        text = stringResource(id = R.string.recording_excluded_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(spacing.small),
                        contentPadding = PaddingValues(bottom = bottomBarListBottomPadding())
                    ) {
                        items(excluded.sorted(), key = { it }) { number ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = number,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.removeExcludedNumber(number) }
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_delete),
                                        contentDescription = stringResource(id = R.string.recording_delete)
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
