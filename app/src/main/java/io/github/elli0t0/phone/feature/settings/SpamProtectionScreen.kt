package io.github.elli0t0.phone.feature.settings

import android.app.role.RoleManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.elli0t0.phone.PhoneApplication
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.bottomBarListBottomPadding
import io.github.elli0t0.phone.data.settings.model.AppSettings
import io.github.elli0t0.phone.navigation.InnerTopBar
import kotlinx.coroutines.launch

/**
 * Settings > Spam protection (offline only): three toggles, the
 * lifetime blocked-call counter, the call-screening role request,
 * and the user-reported numbers with remove + undo. Nothing here
 * touches the network and no number ever leaves the phone.
 */
@Composable
fun SpamProtectionScreen(
    settings: AppSettings,
    settingsViewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as PhoneApplication
    val viewModel: SpamProtectionViewModel = viewModel(
        factory = SpamProtectionViewModel.provideFactory(app)
    )
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val spacing = LocalSpacing.current
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()

    // Call screening role (API 29+): the system binds the screening
    // service when the app is the default dialer or holds this role.
    val lifecycleOwner = LocalLifecycleOwner.current
    val roleManager = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getSystemService(RoleManager::class.java)
        } else {
            null
        }
    }
    var holdsRole by remember {
        mutableStateOf(
            roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) ?: false
        )
    }
    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        holdsRole = roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) ?: false
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                holdsRole = roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) ?: false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("spam_protection_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = spacing.screenHorizontalPadding)
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            InnerTopBar(
                onBack = onNavigateBack,
                title = stringResource(id = R.string.spam_title)
            )
            Text(
                text = stringResource(id = R.string.spam_offline_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = spacing.small)
            )
            Spacer(modifier = Modifier.height(spacing.medium))
            Card(
                shape = settingsListCardShape(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(spacing.medium)) {
                    SettingsSwitchRow(
                        title = stringResource(id = R.string.spam_block_known),
                        subtitle = stringResource(id = R.string.spam_block_known_desc),
                        checked = settings.spamBlockKnown,
                        onCheckedChange = { settingsViewModel.onSpamBlockKnownChanged(it) }
                    )
                    SettingsSwitchRow(
                        title = stringResource(id = R.string.spam_block_hidden),
                        subtitle = stringResource(id = R.string.spam_block_hidden_desc),
                        checked = settings.spamBlockHidden,
                        onCheckedChange = { settingsViewModel.onSpamBlockHiddenChanged(it) }
                    )
                    SettingsSwitchRow(
                        title = stringResource(id = R.string.spam_silence_unknown),
                        subtitle = stringResource(id = R.string.spam_silence_unknown_desc),
                        checked = settings.spamSilenceUnknown,
                        onCheckedChange = { settingsViewModel.onSpamSilenceUnknownChanged(it) }
                    )
                    Text(
                        text = stringResource(id = R.string.spam_blocked_count, settings.spamBlockedCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = spacing.small)
                    )
                }
            }
            if (roleManager != null) {
                Spacer(modifier = Modifier.height(spacing.medium))
                Card(
                    shape = settingsListCardShape(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(spacing.medium)) {
                        Text(
                            text = stringResource(id = R.string.spam_role_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(id = R.string.spam_role_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(spacing.small))
                        if (holdsRole) {
                            Text(
                                text = stringResource(id = R.string.spam_role_held),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        roleLauncher.launch(
                                            roleManager.createRequestRoleIntent(
                                                RoleManager.ROLE_CALL_SCREENING
                                            )
                                        )
                                    }
                                },
                                modifier = Modifier.testTag("spam_role_request_button")
                            ) {
                                Text(text = stringResource(id = R.string.spam_role_request))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(spacing.medium))
            Text(
                text = stringResource(id = R.string.spam_reported_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (entries.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.spam_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = spacing.small)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(spacing.small),
                    contentPadding = PaddingValues(bottom = bottomBarListBottomPadding()),
                    modifier = Modifier.padding(top = spacing.small)
                ) {
                    items(entries, key = { it.numberNormalized }) { entry ->
                        Card(
                            shape = settingsListCardShape(),
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
                                        text = entry.numberNormalized,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    if (entry.reason.isNotBlank()) {
                                        Text(
                                            text = entry.reason,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.removeSpam(entry)
                                        snackbarScope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = context.getString(R.string.spam_removed),
                                                actionLabel = context.getString(R.string.action_undo),
                                                duration = SnackbarDuration.Long
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.reportSpam(entry.numberNormalized, entry.reason)
                                            }
                                        }
                                    },
                                    modifier = Modifier.testTag("spam_remove_${entry.numberNormalized}")
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_delete),
                                        contentDescription = stringResource(id = R.string.spam_remove)
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
