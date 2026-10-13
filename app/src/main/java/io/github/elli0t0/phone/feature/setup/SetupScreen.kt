package io.github.elli0t0.phone.feature.setup

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.elli0t0.phone.PhoneApplication
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.permissions.SetupItemId
import io.github.elli0t0.phone.core.permissions.SetupItemState
import io.github.elli0t0.phone.core.permissions.SetupPermissions
import io.github.elli0t0.phone.core.telecom.DefaultDialerHelper
import kotlinx.coroutines.launch

@Composable
fun SetupScreen(
    onSetupFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as PhoneApplication
    val scope = rememberCoroutineScope()
    val spacing = LocalSpacing.current

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("setup_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = spacing.screenHorizontalPadding)
        ) {
            Spacer(modifier = Modifier.height(spacing.large))
            Text(
                text = stringResource(id = R.string.setup_title),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(spacing.small))
            Text(
                text = stringResource(id = R.string.setup_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(spacing.medium))
            SetupChecklist(
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.height(spacing.medium))
            Button(
                onClick = {
                    scope.launch {
                        app.container.setupRepository.setSetupCompleted(true)
                        onSetupFinished()
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("setup_continue_button")
            ) {
                Text(text = stringResource(id = R.string.setup_continue))
            }
            Spacer(modifier = Modifier.height(spacing.small))
            OutlinedButton(
                onClick = {
                    scope.launch {
                        app.container.setupRepository.setSetupCompleted(true)
                        onSetupFinished()
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("setup_skip_button")
            ) {
                Text(text = stringResource(id = R.string.setup_skip))
            }
            Spacer(modifier = Modifier.height(spacing.medium))
        }
    }
}

@Composable
fun SetupChecklist(
    modifier: Modifier = Modifier,
    scrollable: Boolean = true
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context as? Activity
    var askedItems by remember { mutableStateOf(setOf<SetupItemId>()) }
    var itemStates by remember {
        mutableStateOf(SetupPermissions.getItemStates(context))
    }

    fun computePermanentlyDenied(): Set<SetupItemId> {
        if (activity == null) return emptySet()
        val result = mutableSetOf<SetupItemId>()
        if (SetupItemId.CONTACTS in askedItems &&
            !SetupPermissions.isContactsGranted(context) &&
            SetupPermissions.isPermanentlyDenied(activity, SetupPermissions.CONTACTS_PERMISSIONS)
        ) {
            result.add(SetupItemId.CONTACTS)
        }
        if (SetupItemId.CALL_LOG in askedItems &&
            !SetupPermissions.isCallLogGranted(context) &&
            SetupPermissions.isPermanentlyDenied(activity, SetupPermissions.CALL_LOG_PERMISSIONS)
        ) {
            result.add(SetupItemId.CALL_LOG)
        }
        if (SetupItemId.CALL_PHONE in askedItems &&
            !SetupPermissions.isCallPhoneGranted(context) &&
            SetupPermissions.isPermanentlyDenied(activity, SetupPermissions.CALL_PHONE_PERMISSIONS)
        ) {
            result.add(SetupItemId.CALL_PHONE)
        }
        if (SetupItemId.PHONE_STATE in askedItems &&
            !SetupPermissions.isPhoneStateGranted(context) &&
            SetupPermissions.isPermanentlyDenied(activity, SetupPermissions.PHONE_STATE_PERMISSIONS)
        ) {
            result.add(SetupItemId.PHONE_STATE)
        }
        if (SetupItemId.NOTIFICATIONS in askedItems &&
            !SetupPermissions.isNotificationsGranted(context) &&
            SetupPermissions.isPermanentlyDenied(activity, SetupPermissions.NOTIFICATIONS_PERMISSIONS)
        ) {
            result.add(SetupItemId.NOTIFICATIONS)
        }
        if (SetupItemId.BLUETOOTH in askedItems &&
            !SetupPermissions.isBluetoothGranted(context) &&
            SetupPermissions.isPermanentlyDenied(activity, SetupPermissions.BLUETOOTH_PERMISSIONS)
        ) {
            result.add(SetupItemId.BLUETOOTH)
        }
        return result
    }

    fun refresh() {
        itemStates = SetupPermissions.getItemStates(context, computePermanentlyDenied())
    }

    var pendingItem by remember { mutableStateOf<SetupItemId?>(null) }

    val runtimeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        val requested = pendingItem
        if (requested != null) {
            askedItems = askedItems + requested
        }
        pendingItem = null
        refresh()
    }

    val specialLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        refresh()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val visibleItems = itemStates.filter { it.isApplicable }
    val scrollState = rememberScrollState()
    val checklistModifier = if (scrollable) {
        modifier.verticalScroll(scrollState).testTag("setup_checklist")
    } else {
        modifier.testTag("setup_checklist")
    }

    Column(
        modifier = checklistModifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        visibleItems.forEach { item ->
            SetupItemCard(
                item = item,
                onGrantClick = {
                    when (item.id) {
                        SetupItemId.CONTACTS -> {
                            pendingItem = SetupItemId.CONTACTS
                            runtimeLauncher.launch(SetupPermissions.CONTACTS_PERMISSIONS)
                        }
                        SetupItemId.CALL_LOG -> {
                            pendingItem = SetupItemId.CALL_LOG
                            runtimeLauncher.launch(SetupPermissions.CALL_LOG_PERMISSIONS)
                        }
                        SetupItemId.CALL_PHONE -> {
                            pendingItem = SetupItemId.CALL_PHONE
                            runtimeLauncher.launch(SetupPermissions.CALL_PHONE_PERMISSIONS)
                        }
                        SetupItemId.PHONE_STATE -> {
                            pendingItem = SetupItemId.PHONE_STATE
                            runtimeLauncher.launch(SetupPermissions.PHONE_STATE_PERMISSIONS)
                        }
                        SetupItemId.NOTIFICATIONS -> {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                pendingItem = SetupItemId.NOTIFICATIONS
                                runtimeLauncher.launch(SetupPermissions.NOTIFICATIONS_PERMISSIONS)
                            } else {
                                SetupPermissions.openNotificationSettings(context)
                            }
                        }
                        SetupItemId.BLUETOOTH -> {
                            pendingItem = SetupItemId.BLUETOOTH
                            runtimeLauncher.launch(SetupPermissions.BLUETOOTH_PERMISSIONS)
                        }
                        SetupItemId.FULL_SCREEN -> {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                                Uri.parse("package:" + context.packageName)
                            )
                            specialLauncher.launch(intent)
                        }
                        SetupItemId.DEFAULT_DIALER -> {
                            val intent = DefaultDialerHelper.createDefaultDialerIntent(context)
                            if (intent != null) {
                                specialLauncher.launch(intent)
                            }
                        }
                        SetupItemId.BATTERY -> {
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            specialLauncher.launch(intent)
                        }
                    }
                },
                onOpenSettingsClick = {
                    when (item.id) {
                        SetupItemId.NOTIFICATIONS -> SetupPermissions.openNotificationSettings(context)
                        else -> SetupPermissions.openAppSettings(context)
                    }
                }
            )
        }
    }
}

@Composable
private fun SetupItemCard(
    item: SetupItemState,
    onGrantClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val titleRes = when (item.id) {
        SetupItemId.CONTACTS -> R.string.setup_item_contacts_title
        SetupItemId.CALL_LOG -> R.string.setup_item_call_log_title
        SetupItemId.CALL_PHONE -> R.string.setup_item_call_phone_title
        SetupItemId.PHONE_STATE -> R.string.setup_item_phone_state_title
        SetupItemId.NOTIFICATIONS -> R.string.setup_item_notifications_title
        SetupItemId.BLUETOOTH -> R.string.setup_item_bluetooth_title
        SetupItemId.FULL_SCREEN -> R.string.setup_item_full_screen_title
        SetupItemId.DEFAULT_DIALER -> R.string.setup_item_default_dialer_title
        SetupItemId.BATTERY -> R.string.setup_item_battery_title
    }
    val descRes = when (item.id) {
        SetupItemId.CONTACTS -> R.string.setup_item_contacts_desc
        SetupItemId.CALL_LOG -> R.string.setup_item_call_log_desc
        SetupItemId.CALL_PHONE -> R.string.setup_item_call_phone_desc
        SetupItemId.PHONE_STATE -> R.string.setup_item_phone_state_desc
        SetupItemId.NOTIFICATIONS -> R.string.setup_item_notifications_desc
        SetupItemId.BLUETOOTH -> R.string.setup_item_bluetooth_desc
        SetupItemId.FULL_SCREEN -> R.string.setup_item_full_screen_desc
        SetupItemId.DEFAULT_DIALER -> R.string.setup_item_default_dialer_desc
        SetupItemId.BATTERY -> R.string.setup_item_battery_desc
    }
    val iconRes = when (item.id) {
        SetupItemId.CONTACTS -> R.drawable.ic_contacts
        SetupItemId.CALL_LOG -> R.drawable.ic_recents
        SetupItemId.CALL_PHONE -> R.drawable.ic_phone
        SetupItemId.PHONE_STATE -> R.drawable.ic_sim_card
        SetupItemId.NOTIFICATIONS -> R.drawable.ic_notifications
        SetupItemId.BLUETOOTH -> R.drawable.ic_bluetooth
        SetupItemId.FULL_SCREEN -> R.drawable.ic_notifications
        SetupItemId.DEFAULT_DIALER -> R.drawable.ic_phone
        SetupItemId.BATTERY -> R.drawable.ic_settings
    }
    val buttonTextRes = when {
        item.isGranted -> null
        item.isPermanentlyDenied -> R.string.setup_action_open_settings
        item.id == SetupItemId.DEFAULT_DIALER -> R.string.setup_action_set_as_default
        item.id == SetupItemId.FULL_SCREEN -> R.string.setup_action_open_full_screen
        item.id == SetupItemId.BATTERY -> R.string.setup_action_open_battery
        else -> R.string.setup_action_grant
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth().testTag("setup_item_${item.id.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Column(
                modifier = Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)
            ) {
                Text(
                    text = stringResource(id = titleRes),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(id = descRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (item.isGranted) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_check),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else if (buttonTextRes != null) {
                OutlinedButton(
                    onClick = {
                        if (item.isPermanentlyDenied) {
                            onOpenSettingsClick()
                        } else {
                            onGrantClick()
                        }
                    }
                ) {
                    Text(text = stringResource(id = buttonTextRes))
                }
            }
        }
    }
}

@Composable
fun SetupWarningCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isBlocked by remember {
        mutableStateOf(SetupPermissions.isNotificationsOrFullScreenBlocked(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isBlocked = SetupPermissions.isNotificationsOrFullScreenBlocked(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (!isBlocked) return
    val spacing = LocalSpacing.current
    val notificationsBlocked = !SetupPermissions.isNotificationsGranted(context)

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth().testTag("setup_warning_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.setup_warning_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(id = R.string.setup_warning_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = {
                    if (notificationsBlocked) {
                        SetupPermissions.openNotificationSettings(context)
                    } else {
                        SetupPermissions.openFullScreenSettings(context)
                    }
                }
            ) {
                Text(text = stringResource(id = R.string.setup_warning_button))
            }
        }
    }
}
