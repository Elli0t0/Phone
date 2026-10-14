package io.github.elli0t0.phone.feature.contacts.ui

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.data.contacts.model.ContactEmail
import io.github.elli0t0.phone.data.contacts.model.ContactPhone
import io.github.elli0t0.phone.data.contacts.util.ContactPhotoUtil
import io.github.elli0t0.phone.feature.contacts.ContactEditUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactEditScreen(
    uiState: ContactEditUiState,
    onNameChanged: (String) -> Unit,
    onOrganizationChanged: (String) -> Unit,
    onPhoneNumberChanged: (Int, String) -> Unit,
    onPhoneTypeChanged: (Int, Int) -> Unit,
    onAddPhone: () -> Unit,
    onRemovePhone: (Int) -> Unit,
    onEmailAddressChanged: (Int, String) -> Unit,
    onEmailTypeChanged: (Int, Int) -> Unit,
    onAddEmail: () -> Unit,
    onRemoveEmail: (Int) -> Unit,
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit,
    onPhotoPicked: (ByteArray) -> Unit = {},
    onConfirmSaveCroppedPhoto: () -> Unit = {},
    onDiscardPendingCropPhoto: () -> Unit = {},
    onRemovePhoto: () -> Unit = {},
    onPhotoPreviewRequest: () -> Unit = {},
    onDismissPhotoPreview: () -> Unit = {},
    onPhotoOptionsRequest: () -> Unit = {},
    onDismissPhotoOptions: () -> Unit = {},
    onRingtoneSelected: (Uri?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onCancelClick)
    val context = LocalContext.current
    val spacing = LocalSpacing.current

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
        if (result.resultCode == android.app.Activity.RESULT_OK) {
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

    // Photo crop / save confirmation dialog
    if (uiState.pendingCropPhotoBytes != null) {
        PhotoCropPreviewDialog(
            photoBytes = uiState.pendingCropPhotoBytes,
            onConfirmSave = onConfirmSaveCroppedPhoto,
            onDiscard = onDiscardPendingCropPhoto
        )
    }

    val hasEffectivePhoto = (uiState.stagedPhotoBytes != null) ||
        (!uiState.isPhotoRemoved && !uiState.initialPhotoUri.isNullOrBlank())

    // Photo options dialog
    if (uiState.showPhotoOptionsDialog) {
        ContactPhotoOptionsDialog(
            hasPhoto = hasEffectivePhoto,
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

    // Photo full preview dialog
    if (uiState.showPhotoPreview) {
        PhotoFullPreviewDialog(
            photoUri = if (uiState.stagedPhotoBytes != null) null else uiState.initialPhotoUri,
            displayName = uiState.displayName.ifBlank { stringResource(id = R.string.contact_unknown) },
            onDismiss = onDismissPhotoPreview
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("contact_edit_screen"),
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(top = spacing.small),
                title = {
                    Text(
                        text = if (uiState.isNewContact) {
                            stringResource(id = R.string.contact_create_title)
                        } else {
                            stringResource(id = R.string.contact_edit_title)
                        }
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onCancelClick, modifier = Modifier.padding(start = spacing.screenHorizontalPadding).testTag("btn_edit_cancel")) {
                        Text(text = stringResource(id = R.string.action_cancel))
                    }
                },
                actions = {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(PhoneDimens.editLoadingSize).padding(end = spacing.medium))
                    } else {
                        Button(
                            onClick = onSaveClick,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.padding(end = spacing.screenHorizontalPadding).testTag("btn_edit_save")
                        ) {
                            Text(text = stringResource(id = R.string.action_save))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = spacing.screenHorizontalPadding, vertical = spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Validation Error Alert
            if (uiState.validationErrorRes != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(bottom = spacing.medium)
                ) {
                    Text(
                        text = stringResource(id = uiState.validationErrorRes),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(spacing.medium)
                    )
                }
            }

            // Top Avatar with camera edit badge
            Box(
                modifier = Modifier
                    .size(PhoneDimens.detailAvatarSize)
                    .testTag("edit_contact_avatar"),
                contentAlignment = Alignment.Center
            ) {
                val effectivePhotoUri = if (uiState.isPhotoRemoved) null else uiState.initialPhotoUri

                ContactAvatar(
                    photoUri = effectivePhotoUri,
                    displayName = uiState.displayName.ifBlank { "?" },
                    size = PhoneDimens.detailAvatarSize,
                    textStyle = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.clickable {
                        if (hasEffectivePhoto) {
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
                            if (hasEffectivePhoto) {
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
                            modifier = Modifier.size(PhoneDimens.detailPhotoBadgeIconSize)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.medium))

            // Name & Organization Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(spacing.medium)) {
                    TextField(
                        value = uiState.displayName,
                        onValueChange = onNameChanged,
                        label = { Text(stringResource(id = R.string.contact_field_name)) },
                        shape = MaterialTheme.shapes.medium,
                        colors = EditFieldColors(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_contact_name")
                    )
                    Spacer(modifier = Modifier.height(spacing.medium))
                    TextField(
                        value = uiState.organization,
                        onValueChange = onOrganizationChanged,
                        label = { Text(stringResource(id = R.string.contact_field_organization)) },
                        shape = MaterialTheme.shapes.medium,
                        colors = EditFieldColors(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_contact_org")
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.medium))

            // Phone Numbers Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(spacing.medium)) {
                    Text(
                        text = stringResource(id = R.string.contact_details_phones),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = spacing.small)
                    )

                    uiState.phones.forEachIndexed { index, phone ->
                        PhoneInputField(
                            phone = phone,
                            onNumberChange = { onPhoneNumberChanged(index, it) },
                            onTypeChange = { onPhoneTypeChanged(index, it) },
                            onRemove = { onRemovePhone(index) },
                            canRemove = uiState.phones.size > 1 || phone.number.isNotEmpty(),
                            index = index
                        )
                        Spacer(modifier = Modifier.height(spacing.small))
                    }

                    TextButton(onClick = onAddPhone, modifier = Modifier.testTag("btn_add_phone")) {
                        Text(text = stringResource(id = R.string.contact_add_phone))
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.medium))

            // Emails Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(spacing.medium)) {
                    Text(
                        text = stringResource(id = R.string.contact_details_emails),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = spacing.small)
                    )

                    uiState.emails.forEachIndexed { index, email ->
                        EmailInputField(
                            email = email,
                            onAddressChange = { onEmailAddressChanged(index, it) },
                            onTypeChange = { onEmailTypeChanged(index, it) },
                            onRemove = { onRemoveEmail(index) },
                            index = index
                        )
                        Spacer(modifier = Modifier.height(spacing.small))
                    }

                    TextButton(onClick = onAddEmail, modifier = Modifier.testTag("btn_add_email")) {
                        Text(text = stringResource(id = R.string.contact_add_email))
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.medium))

            // Ringtone Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(spacing.medium)) {
                    Text(
                        text = stringResource(id = R.string.contact_details_ringtone),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = spacing.small)
                    )

                    val ringtoneLabel = remember(uiState.customRingtone) {
                        when {
                            uiState.customRingtone == null -> context.getString(R.string.ringtone_default)
                            uiState.customRingtone.isBlank() || uiState.customRingtone == "silent" -> context.getString(R.string.ringtone_silent)
                            else -> {
                                try {
                                    val ringtone = RingtoneManager.getRingtone(context, Uri.parse(uiState.customRingtone))
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
                            .clickable { launchRingtonePicker(uiState.customRingtone) }
                            .padding(vertical = spacing.small)
                            .testTag("row_edit_ringtone"),
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
                                text = ringtoneLabel,
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
            }

            Spacer(modifier = Modifier.height(PhoneDimens.editBottomSpacer))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhoneInputField(
    phone: ContactPhone,
    onNumberChange: (String) -> Unit,
    onTypeChange: (Int) -> Unit,
    onRemove: () -> Unit,
    canRemove: Boolean,
    index: Int
) {
    var expanded by remember { mutableStateOf(false) }
    val types = listOf(
        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE to stringResource(id = R.string.type_phone_mobile),
        ContactsContract.CommonDataKinds.Phone.TYPE_HOME to stringResource(id = R.string.type_phone_home),
        ContactsContract.CommonDataKinds.Phone.TYPE_WORK to stringResource(id = R.string.type_phone_work),
        ContactsContract.CommonDataKinds.Phone.TYPE_OTHER to stringResource(id = R.string.type_phone_other)
    )
    val selectedLabel = types.firstOrNull { it.first == phone.type }?.second ?: stringResource(id = R.string.type_phone_mobile)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = phone.number,
            onValueChange = onNumberChange,
            label = { Text(stringResource(id = R.string.contact_field_phone)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            shape = MaterialTheme.shapes.medium,
            colors = EditFieldColors(),
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("input_phone_$index")
        )

        Spacer(modifier = Modifier.width(PhoneDimens.editFieldGap))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.width(PhoneDimens.editTypeDropdownWidth)
        ) {
            TextField(
                value = selectedLabel,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                shape = MaterialTheme.shapes.medium,
                colors = EditFieldColors(),
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                types.forEach { (typeInt, label) ->
                    DropdownMenuItem(
                        text = { Text(label, maxLines = 1) },
                        onClick = {
                            onTypeChange(typeInt)
                            expanded = false
                        }
                    )
                }
            }
        }

        if (canRemove) {
            IconButton(onClick = onRemove, modifier = Modifier.testTag("btn_remove_phone_$index")) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = stringResource(id = R.string.contact_remove_field),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmailInputField(
    email: ContactEmail,
    onAddressChange: (String) -> Unit,
    onTypeChange: (Int) -> Unit,
    onRemove: () -> Unit,
    index: Int
) {
    var expanded by remember { mutableStateOf(false) }
    val types = listOf(
        ContactsContract.CommonDataKinds.Email.TYPE_HOME to stringResource(id = R.string.type_email_home),
        ContactsContract.CommonDataKinds.Email.TYPE_WORK to stringResource(id = R.string.type_email_work),
        ContactsContract.CommonDataKinds.Email.TYPE_OTHER to stringResource(id = R.string.type_email_other)
    )
    val selectedLabel = types.firstOrNull { it.first == email.type }?.second ?: stringResource(id = R.string.type_email_home)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = email.address,
            onValueChange = onAddressChange,
            label = { Text(stringResource(id = R.string.contact_field_email)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = MaterialTheme.shapes.medium,
            colors = EditFieldColors(),
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("input_email_$index")
        )

        Spacer(modifier = Modifier.width(PhoneDimens.editFieldGap))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.width(PhoneDimens.editTypeDropdownWidth)
        ) {
            TextField(
                value = selectedLabel,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                shape = MaterialTheme.shapes.medium,
                colors = EditFieldColors(),
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                types.forEach { (typeInt, label) ->
                    DropdownMenuItem(
                        text = { Text(label, maxLines = 1) },
                        onClick = {
                            onTypeChange(typeInt)
                            expanded = false
                        }
                    )
                }
            }
        }

        IconButton(onClick = onRemove, modifier = Modifier.testTag("btn_remove_email_$index")) {
            Icon(
                painter = painterResource(id = R.drawable.ic_close),
                contentDescription = stringResource(id = R.string.contact_remove_field),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EditFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent
)
