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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.telecom.CallManager
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
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)
    val context = LocalContext.current
    val spacing = LocalSpacing.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showMoreMenu by remember { mutableStateOf(false) }
    var phonePickerAction by remember { mutableStateOf<String?>(null) } // "call" or "message"

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
            title = { Text(text = stringResource(id = R.string.contact_delete_dialog_title)) },
            text = { Text(text = stringResource(id = R.string.contact_delete_dialog_message, contactName)) },
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
                    CallManager.placeCall(context, selectedNumber)
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
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("btn_detail_back")) {
                        Icon(painter = painterResource(id = R.drawable.ic_arrow_back), contentDescription = stringResource(id = R.string.cd_back))
                    }
                },
                actions = {
                    if (uiState.contact != null) {
                        IconButton(onClick = onEditClick, modifier = Modifier.testTag("btn_detail_edit")) {
                            Icon(painter = painterResource(id = R.drawable.ic_edit), contentDescription = stringResource(id = R.string.contact_action_edit))
                        }
                        Box {
                            IconButton(onClick = { showMoreMenu = true }, modifier = Modifier.testTag("btn_detail_overflow")) {
                                Icon(painter = painterResource(id = R.drawable.ic_more_vert), contentDescription = stringResource(id = R.string.cd_more_options))
                            }
                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = R.string.action_change_photo)) },
                                    onClick = {
                                        showMoreMenu = false
                                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = R.string.contact_details_ringtone)) },
                                    onClick = {
                                        showMoreMenu = false
                                        launchRingtonePicker(uiState.contact.customRingtone)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = R.string.contact_action_delete), color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMoreMenu = false
                                        onDeleteRequest()
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val contact = uiState.contact
            if (contact != null) {
                val displayName = contact.displayName.ifBlank {
                    contact.phones.firstOrNull()?.number ?: stringResource(id = R.string.contact_unknown)
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .verticalScroll(rememberScrollState())
                        .padding(innerPadding)
                        .padding(horizontal = spacing.screenHorizontalPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(spacing.medium))

                    // Large Avatar with camera badge
                    Box(
                        modifier = Modifier
                            .size(112.dp)
                            .testTag("contact_detail_avatar"),
                        contentAlignment = Alignment.Center
                    ) {
                        ContactAvatar(
                            photoUri = contact.photoUri,
                            displayName = displayName,
                            size = 112.dp,
                            textStyle = MaterialTheme.typography.displayMedium,
                            modifier = Modifier
                                .clickable {
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
                                .size(36.dp)
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
                            shadowElevation = 2.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_camera),
                                    contentDescription = stringResource(id = R.string.cd_edit_photo),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
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

                    Spacer(modifier = Modifier.height(spacing.medium))

                    // Action Row (Call, Message, Favorite, More)
                    ContactActionRow(
                        onCallClick = {
                            if (phones.size == 1) {
                                CallManager.placeCall(context, phones.first().number)
                            } else if (phones.size > 1) {
                                phonePickerAction = "call"
                            }
                        },
                        onMessageClick = {
                            if (phones.size == 1) {
                                val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(phones.first().number)}")).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(smsIntent)
                            } else if (phones.size > 1) {
                                phonePickerAction = "message"
                            }
                        },
                        onFavoriteClick = onFavoriteToggle,
                        onMoreClick = { showMoreMenu = true },
                        isFavorite = uiState.isFavorite,
                        hasPhones = phones.isNotEmpty()
                    )

                    Spacer(modifier = Modifier.height(spacing.large))

                    // Phone Numbers Section
                    if (contact.phones.isNotEmpty()) {
                        DetailSectionCard(title = stringResource(id = R.string.contact_details_phones)) {
                            contact.phones.forEach { phone ->
                                PhoneRowItem(
                                    phone = phone,
                                    onCall = { CallManager.placeCall(context, phone.number) },
                                    onMessage = {
                                        val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(phone.number)}")).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(smsIntent)
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(spacing.medium))
                    }

                    // Email Addresses Section
                    if (contact.emails.isNotEmpty()) {
                        DetailSectionCard(title = stringResource(id = R.string.contact_details_emails)) {
                            contact.emails.forEach { email ->
                                EmailRowItem(
                                    email = email,
                                    onEmail = {
                                        val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${Uri.encode(email.address)}")).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(emailIntent)
                                    }
                                )
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

                    Spacer(modifier = Modifier.height(spacing.large))
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
                style = MaterialTheme.typography.labelMedium,
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
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(spacing.medium))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = phone.number,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = typeLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onCall, modifier = Modifier.size(36.dp)) {
            Icon(
                painter = painterResource(id = R.drawable.ic_phone),
                contentDescription = stringResource(id = R.string.action_call),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        IconButton(onClick = onMessage, modifier = Modifier.size(36.dp)) {
            Icon(
                painter = painterResource(id = R.drawable.ic_message),
                contentDescription = stringResource(id = R.string.action_message),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
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
            modifier = Modifier.size(20.dp)
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
        IconButton(onClick = onEmail, modifier = Modifier.size(36.dp)) {
            Icon(
                painter = painterResource(id = R.drawable.ic_email),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
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
            .clickable(onClick = onClick)
            .padding(vertical = spacing.small)
            .testTag("row_contact_ringtone"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_notifications),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
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
