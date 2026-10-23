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
import androidx.compose.foundation.layout.ColumnScope
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
import io.github.elli0t0.phone.LocalAppSnackbarHostState
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.common.PhoneNumberFormatter
import io.github.elli0t0.phone.core.design.LiquidGlassCard
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
import io.github.elli0t0.phone.data.calllog.model.CallType
import io.github.elli0t0.phone.data.favorites.ContactSimRepository
import io.github.elli0t0.phone.feature.call.ui.DualSimPickerDialog
import java.util.Date
import kotlinx.coroutines.launch
import io.github.elli0t0.phone.data.contacts.model.ContactDetail
import io.github.elli0t0.phone.data.contacts.model.ContactEmail
import io.github.elli0t0.phone.data.contacts.model.ContactPhone
import io.github.elli0t0.phone.data.contacts.util.ContactPhotoUtil
import io.github.elli0t0.phone.feature.contacts.ContactDetailUiState
import io.github.elli0t0.phone.core.common.bidiWrap
import androidx.compose.ui.text.style.TextDirection


@Composable
fun DetailSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val spacing = LocalSpacing.current
    DetailGlassCard(
        modifier = modifier.fillMaxWidth()
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
fun PhoneRowItem(
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
                text = bidiWrap(PhoneNumberFormatter.format(phone.number)),
                style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content),
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
fun EmailRowItem(
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
fun RingtoneRowItem(
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
fun DetailGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    if (LocalAppSettings.current.glassContactDetail) {
        LiquidGlassCard(
            modifier = modifier,
            shape = MaterialTheme.shapes.large
        ) {
            content()
        }
    } else {
        Card(
            modifier = modifier,
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            content()
        }
    }
}

/**
 * One-line summary of the last call with a contact:
 * "Incoming · 36s · 2:05 PM" (time for today, date otherwise).
 * Missed/rejected/blocked calls show no duration.
 */
