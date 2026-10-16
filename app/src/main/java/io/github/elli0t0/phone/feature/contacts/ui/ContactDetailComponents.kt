package io.github.elli0t0.phone.feature.contacts.ui

import android.graphics.BitmapFactory
import android.text.format.DateFormat
import java.util.Date
import io.github.elli0t0.phone.data.calllog.model.CallLogEntry
import io.github.elli0t0.phone.data.calllog.model.CallType
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.ripple
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
import io.github.elli0t0.phone.core.design.AccentBlue
import io.github.elli0t0.phone.core.design.CallAnswerGreen
import io.github.elli0t0.phone.core.design.CallRejectedOrange
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.design.liquidGlass
import io.github.elli0t0.phone.core.design.pressScale
import io.github.elli0t0.phone.core.design.rememberLightHaptic
import io.github.elli0t0.phone.data.contacts.model.ContactPhone

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
    val actionSource = remember { MutableInteractionSource() }
    val lightHaptic = rememberLightHaptic()

    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .pressScale(actionSource)
            .clickable(
                interactionSource = actionSource,
                indication = ripple(),
                enabled = enabled
            ) { lightHaptic(); onClick() }
            .padding(PhoneDimens.detailActionButtonPadding)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val actionIcon: @Composable () -> Unit = {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = label,
                tint = tint.copy(alpha = contentAlpha),
                modifier = Modifier.size(PhoneDimens.detailActionIconSize)
            )
        }
        if (LocalAppSettings.current.glassContactDetail) {
            Box(
                modifier = Modifier
                    .size(PhoneDimens.detailActionButtonSize)
                    .liquidGlass(shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                actionIcon()
            }
        } else {
            Surface(
                modifier = Modifier.size(PhoneDimens.detailActionButtonSize),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = contentAlpha)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    actionIcon()
                }
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
    val spacing = LocalSpacing.current
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

// One call-history row, shared by the detail screen (first 5) and the
// full paged history screen.
@Composable
fun CallHistoryRowItem(
    entry: CallLogEntry,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val spacing = LocalSpacing.current
    val dateText = remember(entry.date) {
        val date = Date(entry.date)
        DateFormat.getMediumDateFormat(context).format(date) +
            " · " + DateFormat.getTimeFormat(context).format(date)
    }
    val subtitleParts = buildList {
        add(context.getString(entry.type.titleRes))
        if (!entry.isMissedOrRejected) {
            add(formatCallDuration(entry.durationSeconds))
        }
        entry.simInfo?.let { add(it) }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = entry.type.iconRes),
            contentDescription = stringResource(id = entry.type.titleRes),
            // Icon color by call type, same as Recents: incoming green,
            // outgoing accent blue, missed red, rejected/blocked orange.
            tint = when (entry.type) {
                CallType.Incoming -> CallAnswerGreen
                CallType.Outgoing -> AccentBlue
                CallType.Missed -> MaterialTheme.colorScheme.error
                CallType.Rejected, CallType.Blocked -> CallRejectedOrange
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(PhoneDimens.detailRowIconSize)
        )
        Spacer(modifier = Modifier.width(spacing.medium))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = dateText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitleParts.joinToString(" - "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun formatCallDuration(durationSeconds: Long): String {
    if (durationSeconds <= 0L) {
        return stringResource(id = R.string.call_duration_seconds, 0)
    }
    val hours = (durationSeconds / 3600).toInt()
    val mins = ((durationSeconds % 3600) / 60).toInt()
    val secs = (durationSeconds % 60).toInt()
    return when {
        hours > 0 -> stringResource(id = R.string.call_duration_hours, hours, mins)
        mins > 0 -> stringResource(id = R.string.call_duration_minutes, mins, secs)
        else -> stringResource(id = R.string.call_duration_seconds, secs)
    }
}
