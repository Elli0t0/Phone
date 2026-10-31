package io.github.elli0t0.phone.feature.contacts.ui

import android.app.Activity
import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
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
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
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
import io.github.elli0t0.phone.feature.contacts.shareContacts


@Composable
fun ContactDetailBottomBar(
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
    modifier: Modifier = Modifier,
    tilt: State<Offset>? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(vertical = PhoneDimens.bottomBarBottomMargin),
        contentAlignment = Alignment.Center
    ) {
        // The bar content is shared by the glass and flat branches.
        val barContent: @Composable () -> Unit = {
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
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth(PhoneDimens.detailBottomBarWidthFraction)
                    .height(PhoneDimens.bottomBarHeight),
                shape = RoundedCornerShape(PhoneDimens.bottomBarCornerPercent),
                floating = true,
                premium = true,
                tilt = tilt
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
fun DetailBottomBarAction(
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

// Share the contact as a provider vCard: the full stored details,
// not the old hand-built text that dropped notes, birthday and
// address. Same path the bulk share uses.
fun shareContact(context: Context, contact: ContactDetail) {
    shareContacts(context, listOf(contact.lookupKey))
}

/**
 * A detail-screen card that renders glass when the contact-detail
 * glass switch is on, a flat surface Card when off.
 */
@Composable
fun lastCallSummary(entry: CallLogEntry): String {
    val context = LocalContext.current
    val typeLabel = stringResource(id = entry.type.titleRes)
    val showDuration = entry.type == CallType.Incoming || entry.type == CallType.Outgoing ||
        entry.type == CallType.Voicemail || entry.type == CallType.Unknown
    val durationText = when {
        !showDuration -> null
        entry.durationSeconds <= 0L -> stringResource(id = R.string.call_duration_seconds, 0)
        entry.durationSeconds < 3600 -> {
            val mins = (entry.durationSeconds / 60).toInt()
            val secs = (entry.durationSeconds % 60).toInt()
            if (mins > 0) {
                stringResource(id = R.string.call_duration_minutes, mins, secs)
            } else {
                stringResource(id = R.string.call_duration_seconds, secs)
            }
        }
        else -> {
            val hours = (entry.durationSeconds / 3600).toInt()
            val mins = ((entry.durationSeconds % 3600) / 60).toInt()
            stringResource(id = R.string.call_duration_hours, hours, mins)
        }
    }
    val timeOrDate = if (android.text.format.DateUtils.isToday(entry.date)) {
        android.text.format.DateFormat.getTimeFormat(context).format(java.util.Date(entry.date))
    } else {
        android.text.format.DateFormat.getDateFormat(context).format(java.util.Date(entry.date))
    }
    return if (durationText != null) {
        "$typeLabel · $durationText · $timeOrDate"
    } else {
        "$typeLabel · $timeOrDate"
    }
}

