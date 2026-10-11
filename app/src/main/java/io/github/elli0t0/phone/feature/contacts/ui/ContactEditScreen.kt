package io.github.elli0t0.phone.feature.contacts.ui

import android.provider.ContactsContract
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.data.contacts.model.ContactEmail
import io.github.elli0t0.phone.data.contacts.model.ContactPhone
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
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onCancelClick)
    val spacing = LocalSpacing.current

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("contact_edit_screen"),
        topBar = {
            TopAppBar(
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
                    TextButton(onClick = onCancelClick, modifier = Modifier.testTag("btn_edit_cancel")) {
                        Text(text = stringResource(id = R.string.action_cancel))
                    }
                },
                actions = {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = spacing.medium))
                    } else {
                        Button(
                            onClick = onSaveClick,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.padding(end = spacing.small).testTag("btn_edit_save")
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
                .padding(horizontal = spacing.screenHorizontalPadding, vertical = spacing.medium)
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

            // Name & Organization Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(spacing.medium)) {
                    OutlinedTextField(
                        value = uiState.displayName,
                        onValueChange = onNameChanged,
                        label = { Text(stringResource(id = R.string.contact_field_name)) },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth().testTag("input_contact_name")
                    )
                    Spacer(modifier = Modifier.height(spacing.medium))
                    OutlinedTextField(
                        value = uiState.organization,
                        onValueChange = onOrganizationChanged,
                        label = { Text(stringResource(id = R.string.contact_field_organization)) },
                        shape = MaterialTheme.shapes.medium,
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

            Spacer(modifier = Modifier.height(40.dp))
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
        OutlinedTextField(
            value = phone.number,
            onValueChange = onNumberChange,
            label = { Text(stringResource(id = R.string.contact_field_phone)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.weight(1f).testTag("input_phone_$index")
        )

        Spacer(modifier = Modifier.width(8.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.width(110.dp)
        ) {
            OutlinedTextField(
                value = selectedLabel,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                types.forEach { (typeInt, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
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
        OutlinedTextField(
            value = email.address,
            onValueChange = onAddressChange,
            label = { Text(stringResource(id = R.string.contact_field_email)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.weight(1f).testTag("input_email_$index")
        )

        Spacer(modifier = Modifier.width(8.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.width(110.dp)
        ) {
            OutlinedTextField(
                value = selectedLabel,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                types.forEach { (typeInt, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
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
