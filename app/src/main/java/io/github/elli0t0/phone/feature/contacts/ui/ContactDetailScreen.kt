package io.github.elli0t0.phone.feature.contacts.ui

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.telephony.PhoneNumberUtils
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import io.github.elli0t0.phone.LocalAppSnackbarHostState
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.common.PhoneNumberFormatter
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.design.rememberLightHaptic
import io.github.elli0t0.phone.core.design.rememberTilt
import io.github.elli0t0.phone.core.telecom.CallManager
import io.github.elli0t0.phone.core.telecom.SimCallHelper
import io.github.elli0t0.phone.core.telecom.SimCallResolution
import io.github.elli0t0.phone.core.telecom.TelecomHelper
import io.github.elli0t0.phone.data.favorites.ContactSimRepository
import io.github.elli0t0.phone.feature.call.ui.DualSimPickerDialog
import kotlinx.coroutines.launch
import io.github.elli0t0.phone.data.contacts.util.ContactPhotoUtil
import io.github.elli0t0.phone.feature.contacts.ContactDetailUiState
import io.github.elli0t0.phone.navigation.InnerTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(
    uiState: ContactDetailUiState,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    onFavoriteToggle: () -> Unit = {},
    onDeleteRequest: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDeleteDialog: () -> Unit,
    onPhotoPicked: (ByteArray) -> Unit = {},
    onConfirmSavePhoto: () -> Unit = {},
    onDiscardPendingPhoto: () -> Unit = {},
    onRemovePhoto: () -> Unit = {},
    onPhotoPreviewRequest: () -> Unit = {},
    onDismissPhotoPreview: () -> Unit = {},
    onPhotoOptionsRequest: () -> Unit = {},
    onDismissPhotoOptions: () -> Unit = {},
    onRingtoneSelected: (Uri?) -> Unit = {},
    contactSimRepository: ContactSimRepository? = null,
    onCallingSimSelected: (String?) -> Unit = {},
    onShowAllHistoryClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)
    val context = LocalContext.current
    val spacing = LocalSpacing.current
    // One tilt listener for the detail bar (Dynamic highlights).
    val tilt = rememberTilt()
    // Read here, not inside placeCallWithSim: the handler is not a
    // @Composable context, so it cannot read a CompositionLocal.
    val defaultSim = LocalAppSettings.current.defaultSim
    // App-level snackbar host (MainActivity): one host above the
    // floating bar for every screen, no per-screen host under the bar.
    val snackbarHostState = LocalAppSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()

    var showMoreMenu by remember { mutableStateOf(false) }
    var phonePickerAction by remember { mutableStateOf<String?>(null) } // "call" or "message"
    var simPickerNumber by remember { mutableStateOf<String?>(null) }
    var showCallingSimDialog by remember { mutableStateOf(false) }
    val simAccounts = remember(context) { TelecomHelper.getCallCapablePhoneAccounts(context) }
    val simRowSource = remember { MutableInteractionSource() }
    val lightHaptic = rememberLightHaptic()

    fun placeCallWithSim(number: String) {
        if (PhoneNumberUtils.isEmergencyNumber(PhoneNumberFormatter.normalizeDigits(number))) {
            // Emergency: skip the SIM picker and saved-SIM lookup
            // entirely; placeCall hands emergency numbers to the
            // system dialer.
            CallManager.placeCall(context, number)
            return
        }
        if (simAccounts.size <= 1) {
            CallManager.placeCall(context, number)
            return
        }
        val repo = contactSimRepository
        if (repo == null) {
            simPickerNumber = number
            return
        }
        coroutineScope.launch {
            when (val res = SimCallHelper.resolveSim(context, number, uiState.contact?.lookupKey, simAccounts, repo, defaultSim)) {
                is SimCallResolution.UseSim -> CallManager.placeCall(context, number, res.sim.handle)
                is SimCallResolution.ShowDialog -> simPickerNumber = number
            }
        }
    }

    if (simPickerNumber != null && simAccounts.size > 1) {
        DualSimPickerDialog(
            simAccounts = simAccounts,
            targetNumber = simPickerNumber.orEmpty(),
            onSimSelected = { sim ->
                val num = simPickerNumber.orEmpty()
                simPickerNumber = null
                CallManager.placeCall(context, num, sim.handle)
            },
            onDismiss = { simPickerNumber = null }
        )
    }

    if (showCallingSimDialog && simAccounts.size > 1) {
        CallingSimDialog(
            simAccounts = simAccounts,
            currentSimId = uiState.callingSimId,
            onSimSelected = { simId ->
                showCallingSimDialog = false
                onCallingSimSelected(simId)
            },
            onDismiss = { showCallingSimDialog = false }
        )
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val bytes = ContactPhotoUtil.processPickedPhoto(context, uri)
            if (bytes != null) {
                onPhotoPicked(bytes)
            }
        }
    }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val pickedUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            onRingtoneSelected(pickedUri)
        }
    }

    fun launchRingtonePicker(currentUriString: String?) {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_RINGTONE_URI)
            if (currentUriString == "silent" || currentUriString == "") {
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, null as Uri?)
            } else if (!currentUriString.isNullOrBlank()) {
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(currentUriString))
            } else {
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Settings.System.DEFAULT_RINGTONE_URI)
            }
        }
        ringtonePickerLauncher.launch(intent)
    }

    // Snackbar triggers
    LaunchedEffect(uiState.infoMessageRes, uiState.errorMessageRes) {
        uiState.infoMessageRes?.let { snackbarHostState.showSnackbar(context.getString(it)) }
        uiState.errorMessageRes?.let { snackbarHostState.showSnackbar(context.getString(it)) }
    }

    // Delete dialog
    if (uiState.showDeleteDialog) {
        val contactName = uiState.contact?.displayName?.ifBlank {
            stringResource(id = R.string.contact_unknown)
        } ?: stringResource(id = R.string.contact_unknown)

        AlertDialog(
            onDismissRequest = onDismissDeleteDialog,
            shape = RoundedCornerShape(PhoneDimens.dialogCornerRadius),
            title = {
                Text(
                    text = stringResource(id = R.string.contact_delete_dialog_title),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.contact_delete_dialog_message, contactName),
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmDelete, modifier = Modifier.testTag("button_confirm_delete")) {
                    Text(text = stringResource(id = R.string.contact_delete_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDeleteDialog) {
                    Text(text = stringResource(id = R.string.action_cancel))
                }
            }
        )
    }

    // Photo crop / save confirmation dialog
    if (uiState.pendingPhotoBytes != null) {
        PhotoCropPreviewDialog(
            photoBytes = uiState.pendingPhotoBytes,
            onConfirmSave = onConfirmSavePhoto,
            onDiscard = onDiscardPendingPhoto
        )
    }

    // Photo options dialog
    if (uiState.showPhotoOptionsDialog) {
        ContactPhotoOptionsDialog(
            hasPhoto = !uiState.contact?.photoUri.isNullOrBlank(),
            onPreviewPhoto = {
                onDismissPhotoOptions()
                onPhotoPreviewRequest()
            },
            onChangePhoto = {
                onDismissPhotoOptions()
                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onRemovePhoto = onRemovePhoto,
            onDismiss = onDismissPhotoOptions
        )
    }

    // Full photo preview dialog
    if (uiState.showPhotoPreview) {
        PhotoFullPreviewDialog(
            photoUri = uiState.contact?.photoUri,
            displayName = uiState.contact?.displayName.orEmpty(),
            onDismiss = onDismissPhotoPreview
        )
    }

    // Multiple phone selection dialog
    val phones = uiState.contact?.phones ?: emptyList()
    if (phonePickerAction != null && phones.size > 1) {
        val action = phonePickerAction
        SelectPhoneDialog(
            title = if (action == "call") stringResource(id = R.string.action_call) else stringResource(id = R.string.action_message),
            phones = phones,
            onSelectPhone = { selectedNumber ->
                phonePickerAction = null
                if (action == "call") {
                    placeCallWithSim(selectedNumber)
                } else {
                    val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(selectedNumber)}")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(smsIntent)
                }
            },
            onDismiss = { phonePickerAction = null }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("contact_detail_screen"),
        bottomBar = {
            if (uiState.contact != null) {
                ContactDetailBottomBar(
                    isFavorite = uiState.isFavorite,
                    onFavoriteClick = onFavoriteToggle,
                    onEditClick = onEditClick,
                    onShareClick = {
                        uiState.contact?.let { shareContact(context, it) }
                    },
                    showMoreMenu = showMoreMenu,
                    onMoreClick = { showMoreMenu = true },
                    onDismissMoreMenu = { showMoreMenu = false },
                    onChangePhoto = {
                        showMoreMenu = false
                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onRingtoneClick = {
                        showMoreMenu = false
                        launchRingtonePicker(uiState.contact?.customRingtone)
                    },
                    onDeleteClick = {
                        showMoreMenu = false
                        onDeleteRequest()
                    },
                    tilt = tilt
                )
            }
        },
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val contact = uiState.contact
            if (contact != null) {
                val displayName = contact.displayName.ifBlank {
                    contact.phones.firstOrNull()?.number?.let { PhoneNumberFormatter.format(it) }
                        ?: stringResource(id = R.string.contact_unknown)
                }

                val scrollState = rememberScrollState()
                var headerHeightPx by remember { mutableStateOf(0) }
                val density = LocalDensity.current
                val collapseThresholdPx = headerHeightPx + with(density) { spacing.medium.toPx() }
                // The name appears in the top row once the header card
                // has scrolled away; at rest the card itself shows it.
                val showTitle = scrollState.value >= collapseThresholdPx
                val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                // Normal top row above the card: back arrow in its own
                // row, then the scrolling content below it.
                Column(modifier = Modifier.fillMaxSize()) {
                    InnerTopBar(
                        onBack = onBackClick,
                        title = if (showTitle) displayName else null,
                        backTestTag = "btn_detail_back"
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        ContactDetailContent(
                            contact = contact,
                            displayName = displayName,
                            phones = phones,
                            uiState = uiState,
                            simAccounts = simAccounts,
                            scrollState = scrollState,
                            navBarBottom = navBarBottom,
                            contentTopPadding = 0.dp,
                            simRowSource = simRowSource,
                            onHeaderHeightMeasured = { headerHeightPx = it },
                            onCall = ::placeCallWithSim,
                            onPhonePickerAction = { phonePickerAction = it },
                            onShowCallingSimDialog = { showCallingSimDialog = true },
                            onRingtonePicker = ::launchRingtonePicker,
                            onShowAllHistoryClick = onShowAllHistoryClick
                        )
                    }
                }
            }
        }
    }
}
