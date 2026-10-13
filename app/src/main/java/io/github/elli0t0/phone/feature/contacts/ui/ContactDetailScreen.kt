package io.github.elli0t0.phone.feature.contacts.ui

import android.provider.ContactsContract
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.data.contacts.model.ContactDetail
import io.github.elli0t0.phone.data.contacts.model.ContactEmail
import io.github.elli0t0.phone.data.contacts.model.ContactPhone
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
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)
    val spacing = LocalSpacing.current

    if (uiState.showDeleteDialog) {
        val contactName = uiState.contact?.displayName?.ifBlank {
            stringResource(id = R.string.contact_unknown)
        } ?: stringResource(id = R.string.contact_unknown)

        AlertDialog(
            onDismissRequest = onDismissDeleteDialog,
            title = { Text(text = stringResource(id = R.string.contact_delete_dialog_title)) },
            text = { Text(text = stringResource(id = R.string.contact_delete_dialog_message, contactName)) },
            confirmButton = {
                TextButton(
                    onClick = onConfirmDelete,
                    modifier = Modifier.testTag("button_confirm_delete")
                ) {
                    Text(
                        text = stringResource(id = R.string.contact_delete_confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDeleteDialog) {
                    Text(text = stringResource(id = R.string.action_cancel))
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("contact_detail_screen"),
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("btn_detail_back")) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = stringResource(id = R.string.cd_back)
                        )
                    }
                },
                actions = {
                    if (uiState.contact != null) {
                        IconButton(onClick = onFavoriteToggle, modifier = Modifier.testTag("btn_detail_favorite")) {
                            Icon(
                                painter = painterResource(
                                    id = if (uiState.isFavorite) R.drawable.ic_star else R.drawable.ic_star_outline
                                ),
                                contentDescription = stringResource(
                                    id = if (uiState.isFavorite) R.string.cd_favorite_remove else R.string.cd_favorite_add
                                ),
                                tint = if (uiState.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onEditClick, modifier = Modifier.testTag("btn_detail_edit")) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_edit),
                                contentDescription = stringResource(id = R.string.contact_action_edit)
                            )
                        }
                        IconButton(onClick = onDeleteRequest, modifier = Modifier.testTag("btn_detail_delete")) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_delete),
                                contentDescription = stringResource(id = R.string.contact_action_delete),
                                tint = MaterialTheme.colorScheme.error
                            )
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
                ContactDetailContent(contact = contact, modifier = Modifier.padding(innerPadding))
            }
        }
    }
}

@Composable
private fun ContactDetailContent(
    contact: ContactDetail,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val displayName = contact.displayName.ifBlank {
        contact.phones.firstOrNull()?.number ?: stringResource(id = R.string.contact_unknown)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.screenHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(spacing.medium))

        // Large Avatar
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayName.take(1).uppercase(),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.height(spacing.medium))

        // Large Display Name
        Text(
            text = displayName,
            style = MaterialTheme.typography.headlineMedium,
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

        Spacer(modifier = Modifier.height(spacing.large))

        // Phone Numbers Section
        if (contact.phones.isNotEmpty()) {
            DetailSectionCard(title = stringResource(id = R.string.contact_details_phones)) {
                contact.phones.forEach { phone ->
                    PhoneRowItem(phone = phone)
                }
            }
            Spacer(modifier = Modifier.height(spacing.medium))
        }

        // Email Addresses Section
        if (contact.emails.isNotEmpty()) {
            DetailSectionCard(title = stringResource(id = R.string.contact_details_emails)) {
                contact.emails.forEach { email ->
                    EmailRowItem(email = email)
                }
            }
            Spacer(modifier = Modifier.height(spacing.medium))
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
private fun PhoneRowItem(phone: ContactPhone) {
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
    }
}

@Composable
private fun EmailRowItem(email: ContactEmail) {
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
    }
}
