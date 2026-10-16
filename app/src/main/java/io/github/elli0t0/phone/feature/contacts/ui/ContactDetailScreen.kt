package io.github.elli0t0.phone.feature.contacts.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.common.PhoneNumberFormatter
import io.github.elli0t0.phone.core.design.GlassSurface
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.design.pressScale
import io.github.elli0t0.phone.core.design.rememberLightHaptic
import io.github.elli0t0.phone.core.telecom.CallManager
import io.github.elli0t0.phone.core.telecom.SimCallHelper
import io.github.elli0t0.phone.core.telecom.SimCallResolution
import io.github.elli0t0.phone.core.telecom.TelecomHelper
import android.text.format.DateFormat
import io.github.elli0t0.phone.data.calllog.model.CallLogEntry
import io.github.elli0t0.phone.data.favorites.ContactSimRepository
import io.github.elli0t0.phone.feature.call.ui.DualSimPickerDialog
import java.util.Date
import kotlinx.coroutines.launch
import io.github.elli0t0.phone.data.contacts.model.ContactDetail
import io.github.elli0t0.phone.data.contacts.model.ContactEmail
import io.github.elli0t0.phone.data.contacts.model.ContactPhone
import io.github.elli0t0.phone.data.contacts.util.ContactPhotoUtil
import io.github.elli0t0.phone.feature.contacts.ContactDetailUiState

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
    // Read here, not inside placeCallWithSim: the handler is not a
    // @Composable context, so it cannot read a CompositionLocal.
    val defaultSim = LocalAppSettings.current.defaultSim
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var showMoreMenu by remember { mutableStateOf(false) }
    var phonePickerAction by remember { mutableStateOf<String?>(null) } // "call" or "message"
    var simPickerNumber by remember { mutableStateOf<String?>(null) }
    var showCallingSimDialog by remember { mutableStateOf(false) }
    val simAccounts = remember(context) { TelecomHelper.getCallCapablePhoneAccounts(context) }
    val avatarSource = remember { MutableInteractionSource() }
    val simRowSource = remember { MutableInteractionSource() }
    val lightHaptic = rememberLightHaptic()

    fun placeCallWithSim(number: String) {
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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
                    }
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
                val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                val topBarHeight = statusBarTop + PhoneDimens.detailTopBarHeight
                val collapseThresholdPx = headerHeightPx + with(density) { spacing.medium.toPx() }
                val showSolidBar = scrollState.value >= collapseThresholdPx
                val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .verticalScroll(scrollState)
                            .padding(horizontal = spacing.screenHorizontalPadding)
                            .padding(top = topBarHeight)
                            .padding(bottom = navBarBottom + PhoneDimens.bottomBarReservedHeight),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                    Spacer(modifier = Modifier.height(spacing.medium))

                    // Top card: avatar, name, organization, primary number
                    Card(
                        modifier = Modifier.fillMaxWidth()
                            .onGloballyPositioned { headerHeightPx = it.size.height },
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(spacing.medium),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Large Avatar with camera badge
                            Box(
                                modifier = Modifier
                                    .size(PhoneDimens.detailAvatarSize)
                                    .testTag("contact_detail_avatar"),
                                contentAlignment = Alignment.Center
                            ) {
                                ContactAvatar(
                                    photoUri = contact.photoUri,
                                    displayName = displayName,
                                    size = PhoneDimens.detailAvatarSize,
                                    textStyle = MaterialTheme.typography.displayMedium,
                                    modifier = Modifier
                                        .pressScale(avatarSource)
                                        .clickable(
                                            interactionSource = avatarSource,
                                            indication = ripple()
                                        ) {
                                            lightHaptic()
                                            if (!contact.photoUri.isNullOrBlank()) {
                                                onPhotoOptionsRequest()
                                            } else {
                                                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                            }
                                        }
                                )

                                // Camera/Edit photo badge
                                Surface(
                                    modifier = Modifier
                                        .size(PhoneDimens.detailPhotoBadgeSize)
                                        .align(Alignment.BottomEnd)
                                        .clip(CircleShape)
                                        .clickable {
                                            if (!contact.photoUri.isNullOrBlank()) {
                                                onPhotoOptionsRequest()
                                            } else {
                                                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                            }
                                        },
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shadowElevation = PhoneDimens.detailPhotoBadgeElevation
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_camera),
                                            contentDescription = stringResource(id = R.string.cd_edit_photo),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(PhoneDimens.detailPhotoBadgeIconSize)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(spacing.medium))

                            // Large Contact Display Name
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            // Organization
                            if (!contact.organization.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(spacing.extraSmall))
                                Text(
                                    text = contact.organization,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Primary number
                            val primaryNumber = contact.phones.firstOrNull()?.number
                            if (!primaryNumber.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(spacing.extraSmall))
                                Text(
                                    text = primaryNumber,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(spacing.medium))

                    // Call / Message row; the other actions are in the bottom bar.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ContactActionButton(
                            icon = R.drawable.ic_phone,
                            label = stringResource(id = R.string.action_call),
                            onClick = {
                                if (phones.size == 1) {
                                    placeCallWithSim(phones.first().number)
                                } else if (phones.size > 1) {
                                    phonePickerAction = "call"
                                }
                            },
                            enabled = phones.isNotEmpty(),
                            testTag = "btn_action_call"
                        )
                        ContactActionButton(
                            icon = R.drawable.ic_message,
                            label = stringResource(id = R.string.action_message),
                            onClick = {
                                if (phones.size == 1) {
                                    val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(phones.first().number)}")).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(smsIntent)
                                } else if (phones.size > 1) {
                                    phonePickerAction = "message"
                                }
                            },
                            enabled = phones.isNotEmpty(),
                            testTag = "btn_action_message"
                        )
                    }

                    Spacer(modifier = Modifier.height(spacing.large))

                    // Phone Numbers Section
                    if (contact.phones.isNotEmpty()) {
                        DetailSectionCard(title = stringResource(id = R.string.contact_details_phones)) {
                            contact.phones.forEachIndexed { index, phone ->
                                PhoneRowItem(
                                    phone = phone,
                                    onCall = { placeCallWithSim(phone.number) },
                                    onMessage = {
                                        val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(phone.number)}")).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(smsIntent)
                                    }
                                )
                                if (index < contact.phones.lastIndex) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(
                                            alpha = PhoneDimens.recentsDividerAlpha
                                        ),
                                        thickness = 1.dp,
                                        // Divider starts at the text: card padding + icon + spacer.
                                        modifier = Modifier.padding(
                                            start = spacing.medium +
                                                PhoneDimens.detailRowIconSize + spacing.medium
                                        )
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(spacing.medium))
                    }

                    // Email Addresses Section
                    if (contact.emails.isNotEmpty()) {
                        DetailSectionCard(title = stringResource(id = R.string.contact_details_emails)) {
                            contact.emails.forEachIndexed { index, email ->
                                EmailRowItem(
                                    email = email,
                                    onEmail = {
                                        val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${Uri.encode(email.address)}")).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(emailIntent)
                                    }
                                )
                                if (index < contact.emails.lastIndex) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(
                                            alpha = PhoneDimens.recentsDividerAlpha
                                        ),
                                        thickness = 1.dp,
                                        // Divider starts at the text: card padding + icon + spacer.
                                        modifier = Modifier.padding(
                                            start = spacing.medium +
                                                PhoneDimens.detailRowIconSize + spacing.medium
                                        )
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(spacing.medium))
                    }

                    // Ringtone Section
                    DetailSectionCard(title = stringResource(id = R.string.contact_details_ringtone)) {
                        RingtoneRowItem(
                            ringtoneUriString = contact.customRingtone,
                            onClick = { launchRingtonePicker(contact.customRingtone) }
                        )
                    }

                    // Calling SIM Section (dual-SIM only)
                    if (simAccounts.size > 1) {
                        Spacer(modifier = Modifier.height(spacing.medium))
                        DetailSectionCard(title = stringResource(id = R.string.contact_calling_sim_title)) {
                            val currentLabel = simAccounts.firstOrNull { it.handle.id == uiState.callingSimId }?.label
                                ?: stringResource(id = R.string.contact_calling_sim_ask)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.small)
                                    .pressScale(simRowSource)
                                    .clickable(
                                        interactionSource = simRowSource,
                                        indication = ripple()
                                    ) { lightHaptic(); showCallingSimDialog = true }
                                    .padding(vertical = spacing.small)
                                    .testTag("row_contact_calling_sim"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_sim_card),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(PhoneDimens.detailRowIconSize)
                                )
                                Spacer(modifier = Modifier.width(spacing.medium))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentLabel,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(id = R.string.contact_calling_sim_desc),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // History: just a row with the title; tapping it opens
                    // the full contact history screen.
                    Spacer(modifier = Modifier.height(spacing.medium))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        val historySource = remember { MutableInteractionSource() }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.large)
                                .pressScale(historySource)
                                .clickable(
                                    interactionSource = historySource,
                                    indication = ripple()
                                ) { lightHaptic(); onShowAllHistoryClick() }
                                .padding(spacing.medium)
                                .testTag("btn_show_all_history"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.contact_details_history),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(spacing.large))
                    }

                    // Overlay top bar: status-bar inset + 56dp, transparent at
                    // the top, solid page color with the name once the header
                    // card has scrolled away. Content scrolls underneath it.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(topBarHeight)
                            .background(
                                if (showSolidBar) MaterialTheme.colorScheme.background
                                else Color.Transparent
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = statusBarTop),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onBackClick, modifier = Modifier.testTag("btn_detail_back")) {
                                Icon(painter = painterResource(id = R.drawable.ic_arrow_back), contentDescription = stringResource(id = R.string.cd_back))
                            }
                            if (showSolidBar) {
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val spacing = LocalSpacing.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(spacing.medium)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = spacing.small)
            )
            content()
        }
    }
}

@Composable
private fun PhoneRowItem(
    phone: ContactPhone,
    onCall: () -> Unit,
    onMessage: () -> Unit
) {
    val spacing = LocalSpacing.current
    val typeLabel = when (phone.type) {
        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> stringResource(id = R.string.type_phone_mobile)
        ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> stringResource(id = R.string.type_phone_home)
        ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> stringResource(id = R.string.type_phone_work)
        else -> stringResource(id = R.string.type_phone_other)
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_phone),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(PhoneDimens.detailRowIconSize)
        )
        Spacer(modifier = Modifier.width(spacing.medium))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = PhoneNumberFormatter.format(phone.number),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = typeLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onCall, modifier = Modifier.size(PhoneDimens.detailRowActionButtonSize)) {
            Icon(
                painter = painterResource(id = R.drawable.ic_phone),
                contentDescription = stringResource(id = R.string.action_call),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(PhoneDimens.detailRowIconSize)
            )
        }
        IconButton(onClick = onMessage, modifier = Modifier.size(PhoneDimens.detailRowActionButtonSize)) {
            Icon(
                painter = painterResource(id = R.drawable.ic_message),
                contentDescription = stringResource(id = R.string.action_message),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(PhoneDimens.detailRowIconSize)
            )
        }
    }
}

@Composable
private fun EmailRowItem(
    email: ContactEmail,
    onEmail: () -> Unit
) {
    val spacing = LocalSpacing.current
    val typeLabel = when (email.type) {
        ContactsContract.CommonDataKinds.Email.TYPE_HOME -> stringResource(id = R.string.type_email_home)
        ContactsContract.CommonDataKinds.Email.TYPE_WORK -> stringResource(id = R.string.type_email_work)
        else -> stringResource(id = R.string.type_email_other)
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_email),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(PhoneDimens.detailRowIconSize)
        )
        Spacer(modifier = Modifier.width(spacing.medium))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = email.address,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = typeLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onEmail, modifier = Modifier.size(PhoneDimens.detailRowActionButtonSize)) {
            Icon(
                painter = painterResource(id = R.drawable.ic_email),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(PhoneDimens.detailRowIconSize)
            )
        }
    }
}

@Composable
private fun RingtoneRowItem(
    ringtoneUriString: String?,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val spacing = LocalSpacing.current
    val ringtoneSource = remember { MutableInteractionSource() }
    val lightHaptic = rememberLightHaptic()

    val label = remember(ringtoneUriString) {
        when {
            ringtoneUriString == null -> context.getString(R.string.ringtone_default)
            ringtoneUriString.isBlank() || ringtoneUriString == "silent" -> context.getString(R.string.ringtone_silent)
            else -> {
                try {
                    val ringtone = RingtoneManager.getRingtone(context, Uri.parse(ringtoneUriString))
                    ringtone?.getTitle(context) ?: context.getString(R.string.ringtone_custom)
                } catch (_: Exception) {
                    context.getString(R.string.ringtone_custom)
                }
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .pressScale(ringtoneSource)
            .clickable(
                interactionSource = ringtoneSource,
                indication = ripple()
            ) { lightHaptic(); onClick() }
            .padding(vertical = spacing.small)
            .testTag("row_contact_ringtone"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_notifications),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(PhoneDimens.detailRowIconSize)
        )
        Spacer(modifier = Modifier.width(spacing.medium))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(id = R.string.contact_details_ringtone),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ContactDetailBottomBar(
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onEditClick: () -> Unit,
    onShareClick: () -> Unit,
    showMoreMenu: Boolean,
    onMoreClick: () -> Unit,
    onDismissMoreMenu: () -> Unit,
    onChangePhoto: () -> Unit,
    onRingtoneClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(vertical = PhoneDimens.bottomBarBottomMargin),
        contentAlignment = Alignment.Center
    ) {
        // The bar content is shared by the glass and flat branches.
        val barContent: @Composable BoxScope.() -> Unit = {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DetailBottomBarAction(
                    icon = if (isFavorite) R.drawable.ic_star else R.drawable.ic_star_outline,
                    label = stringResource(id = R.string.action_favorite),
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    onClick = onFavoriteClick,
                    testTag = "btn_detail_favorite",
                    modifier = Modifier.weight(1f)
                )
                DetailBottomBarAction(
                    icon = R.drawable.ic_edit,
                    label = stringResource(id = R.string.contact_action_edit),
                    onClick = onEditClick,
                    testTag = "btn_detail_edit",
                    modifier = Modifier.weight(1f)
                )
                DetailBottomBarAction(
                    icon = R.drawable.ic_share,
                    label = stringResource(id = R.string.action_share),
                    onClick = onShareClick,
                    testTag = "btn_detail_share",
                    modifier = Modifier.weight(1f)
                )
                Box(modifier = Modifier.weight(1f)) {
                    DetailBottomBarAction(
                        icon = R.drawable.ic_more_vert,
                        label = stringResource(id = R.string.action_more),
                        onClick = onMoreClick,
                        testTag = "btn_detail_overflow",
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = onDismissMoreMenu
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.action_change_photo)) },
                            onClick = onChangePhoto
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.contact_details_ringtone)) },
                            onClick = onRingtoneClick
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.contact_action_delete), color = MaterialTheme.colorScheme.error) },
                            onClick = onDeleteClick
                        )
                    }
                }
            }
        }
        if (LocalAppSettings.current.glassContactDetail) {
            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth(PhoneDimens.detailBottomBarWidthFraction)
                    .height(PhoneDimens.bottomBarHeight),
                shape = RoundedCornerShape(PhoneDimens.bottomBarCornerPercent)
            ) {
                barContent()
            }
        } else {
            // Flat bar: solid surface with a soft shadow.
            Box(
                modifier = Modifier
                    .fillMaxWidth(PhoneDimens.detailBottomBarWidthFraction)
                    .height(PhoneDimens.bottomBarHeight)
                    .shadow(
                        PhoneDimens.bottomBarElevation,
                        RoundedCornerShape(PhoneDimens.bottomBarCornerPercent),
                        spotColor = Color.Black.copy(alpha = PhoneDimens.bottomBarShadowAlpha),
                        ambientColor = Color.Black.copy(alpha = PhoneDimens.bottomBarShadowAlpha)
                    )
                    .clip(RoundedCornerShape(PhoneDimens.bottomBarCornerPercent))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                barContent()
            }
        }
    }
}

@Composable
private fun DetailBottomBarAction(
    icon: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    testTag: String = ""
) {
    val barSource = remember { MutableInteractionSource() }
    val lightHaptic = rememberLightHaptic()
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .pressScale(barSource)
            .clickable(
                interactionSource = barSource,
                indication = ripple()
            ) { lightHaptic(); onClick() }
            .padding(vertical = PhoneDimens.detailActionRowVerticalPadding)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(PhoneDimens.bottomBarIconSize)
        )
        Spacer(modifier = Modifier.height(PhoneDimens.bottomBarLabelIconGap))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// Share the contact as a vCard.
private fun shareContact(context: Context, contact: ContactDetail) {
    val vCard = buildString {
        appendLine("BEGIN:VCARD")
        appendLine("VERSION:3.0")
        appendLine("FN:${contact.displayName}")
        if (!contact.organization.isNullOrBlank()) {
            appendLine("ORG:${contact.organization}")
        }
        contact.phones.forEach { phone ->
            val type = when (phone.type) {
                ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "CELL"
                ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "HOME"
                ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "WORK"
                else -> "VOICE"
            }
            appendLine("TEL;TYPE=$type:${phone.number}")
        }
        contact.emails.forEach { email ->
            appendLine("EMAIL:${email.address}")
        }
        appendLine("END:VCARD")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/x-vcard"
        putExtra(Intent.EXTRA_TEXT, vCard)
        putExtra(Intent.EXTRA_SUBJECT, contact.displayName)
    }
    context.startActivity(
        Intent.createChooser(intent, context.getString(R.string.action_share))
    )
}
