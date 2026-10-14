package io.github.elli0t0.phone.feature.contacts.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.data.contacts.model.ContactPhone

@Composable
fun ContactActionRow(
    onCallClick: () -> Unit,
    onMessageClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onMoreClick: () -> Unit,
    isFavorite: Boolean,
    hasPhones: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = PhoneDimens.detailActionRowHorizontalPadding,
                vertical = PhoneDimens.detailActionRowVerticalPadding
            ),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ContactActionButton(
            icon = R.drawable.ic_phone,
            label = stringResource(id = R.string.action_call),
            onClick = onCallClick,
            enabled = hasPhones,
            testTag = "btn_action_call"
        )
        ContactActionButton(
            icon = R.drawable.ic_message,
            label = stringResource(id = R.string.action_message),
            onClick = onMessageClick,
            enabled = hasPhones,
            testTag = "btn_action_message"
        )
        ContactActionButton(
            icon = if (isFavorite) R.drawable.ic_star else R.drawable.ic_star_outline,
            label = stringResource(id = R.string.action_favorite),
            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            onClick = onFavoriteClick,
            testTag = "btn_action_favorite"
        )
        ContactActionButton(
            icon = R.drawable.ic_more_vert,
            label = stringResource(id = R.string.action_more),
            onClick = onMoreClick,
            testTag = "btn_action_more"
        )
    }
}

@Composable
fun ContactActionButton(
    icon: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.primary,
    testTag: String = ""
) {
    val spacing = LocalSpacing.current
    val contentAlpha = if (enabled) 1f else 0.38f

    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(PhoneDimens.detailActionButtonPadding)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(PhoneDimens.detailActionButtonSize),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = contentAlpha)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = label,
                    tint = tint.copy(alpha = contentAlpha),
                    modifier = Modifier.size(PhoneDimens.detailActionIconSize)
                )
            }
        }
        Spacer(modifier = Modifier.height(spacing.extraSmall))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
        )
    }
}

@Composable
fun PhotoCropPreviewDialog(
    photoBytes: ByteArray,
    onConfirmSave: () -> Unit,
    onDiscard: () -> Unit
) {
    val bitmap = remember(photoBytes) {
        BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.size)
    }

    AlertDialog(
        onDismissRequest = onDiscard,
        shape = RoundedCornerShape(PhoneDimens.dialogCornerRadius),
        title = {
            Text(
                text = stringResource(id = R.string.photo_crop_preview_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = stringResource(id = R.string.cd_contact_avatar),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(PhoneDimens.detailPhotoCropPreviewSize)
                            .clip(CircleShape)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirmSave, modifier = Modifier.testTag("btn_confirm_save_photo")) {
                Text(text = stringResource(id = R.string.action_save_photo))
            }
        },
        dismissButton = {
            TextButton(onClick = onDiscard) {
                Text(text = stringResource(id = R.string.action_discard))
            }
        }
    )
}

@Composable
fun PhotoFullPreviewDialog(
    photoUri: String?,
    displayName: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(PhoneDimens.dialogCornerRadius),
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (!photoUri.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(photoUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = displayName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(PhoneDimens.detailPhotoFullPreviewSize)
                            .clip(RoundedCornerShape(PhoneDimens.detailPhotoFullPreviewCornerRadius))
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.action_cancel))
            }
        }
    )
}

@Composable
fun ContactPhotoOptionsDialog(
    hasPhoto: Boolean,
    onPreviewPhoto: () -> Unit,
    onChangePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(PhoneDimens.dialogCornerRadius),
        title = {
            Text(
                text = stringResource(id = R.string.photo_preview_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (hasPhoto) {
                    TextButton(
                        onClick = onPreviewPhoto,
                        modifier = Modifier.fillMaxWidth().testTag("btn_photo_view")
                    ) {
                        Text(text = stringResource(id = R.string.action_preview_photo))
                    }
                }
                TextButton(
                    onClick = onChangePhoto,
                    modifier = Modifier.fillMaxWidth().testTag("btn_photo_change")
                ) {
                    Text(text = stringResource(id = R.string.action_change_photo))
                }
                if (hasPhoto) {
                    TextButton(
                        onClick = onRemovePhoto,
                        modifier = Modifier.fillMaxWidth().testTag("btn_photo_remove")
                    ) {
                        Text(
                            text = stringResource(id = R.string.action_remove_photo),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.action_cancel))
            }
        }
    )
}

@Composable
fun SelectPhoneDialog(
    title: String,
    phones: List<ContactPhone>,
    onSelectPhone: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(PhoneDimens.dialogCornerRadius),
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                phones.forEach { phone ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = spacing.extraSmall)
                            .clickable { onSelectPhone(phone.number) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(spacing.small)) {
                            Text(
                                text = phone.number,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.action_cancel))
            }
        }
    )
}
