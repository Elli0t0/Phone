package io.github.elli0t0.phone.feature.contacts.ui
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.telecom.SimAccountInfo
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.design.pressScale
import io.github.elli0t0.phone.core.design.rememberLightHaptic
import io.github.elli0t0.phone.data.contacts.model.ContactDetail
import io.github.elli0t0.phone.data.contacts.model.ContactPhone
import io.github.elli0t0.phone.feature.contacts.ContactDetailUiState

/**
 * The scrolling detail content: header card, Call/Message actions,
 * phone/email/ringtone/calling-SIM sections, and the History card.
 * Extracted from ContactDetailScreen to keep that file under 400 lines.
 */
@Composable
fun ContactDetailContent(
    contact: ContactDetail,
    displayName: String,
    phones: List<ContactPhone>,
    uiState: ContactDetailUiState,
    simAccounts: List<SimAccountInfo>,
    scrollState: androidx.compose.foundation.ScrollState,
    navBarBottom: androidx.compose.ui.unit.Dp,
    contentTopPadding: androidx.compose.ui.unit.Dp,
    simRowSource: MutableInteractionSource,
    onHeaderHeightMeasured: (Int) -> Unit,
    onCall: (String) -> Unit,
    onPhonePickerAction: (String) -> Unit,
    onShowCallingSimDialog: () -> Unit,
    onRingtonePicker: (String?) -> Unit,
    onShowAllHistoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val spacing = LocalSpacing.current
    val lightHaptic = rememberLightHaptic()
Column(
    modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .verticalScroll(scrollState)
        .padding(horizontal = spacing.screenHorizontalPadding)
        .padding(top = contentTopPadding)
        // Clearance above the floating bar so the last card
        // (History) is never covered: bar height + navigation
        // inset (both in the other terms) + a 24 dp gap + the
        // bottom scrim height.
        .padding(bottom = navBarBottom + PhoneDimens.bottomBarReservedHeight + PhoneDimens.detailHistoryGap + PhoneDimens.bottomScrimHeight),
    horizontalAlignment = Alignment.CenterHorizontally
) {
Spacer(modifier = Modifier.height(spacing.medium))

// Top card: name (left, up to 2 lines), number under it, round
// avatar at the right, and the Call / Message buttons below.
// No camera badge: photo editing lives in the Edit screen.
DetailGlassCard(
    modifier = Modifier.fillMaxWidth()
        .onGloballyPositioned { onHeaderHeightMeasured(it.size.height) }
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(spacing.medium)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = PhoneDimens.detailHeaderNameSize,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val primaryNumber = contact.phones.firstOrNull()?.number
                if (!primaryNumber.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(spacing.extraSmall))
                    Text(
                        text = primaryNumber,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!contact.organization.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(spacing.extraSmall))
                    Text(
                        text = contact.organization,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(spacing.medium))
            ContactAvatar(
                photoUri = contact.photoUri,
                displayName = displayName,
                size = PhoneDimens.detailHeaderAvatarSize,
                textStyle = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.testTag("contact_detail_avatar")
            )
        }

        Spacer(modifier = Modifier.height(spacing.medium))

        // Call / Message round buttons below the identity row.
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
                        onCall(phones.first().number)
                    } else if (phones.size > 1) {
                        onPhonePickerAction("call")
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
                        onPhonePickerAction("message")
                    }
                },
                enabled = phones.isNotEmpty(),
                testTag = "btn_action_message"
            )
        }
    }
}

Spacer(modifier = Modifier.height(spacing.large))
// Phone Numbers Section
if (contact.phones.isNotEmpty()) {
    DetailSectionCard(title = stringResource(id = R.string.contact_details_phones)) {
        contact.phones.forEachIndexed { index, phone ->
            PhoneRowItem(
                phone = phone,
                onCall = { onCall(phone.number) },
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
        onClick = { onRingtonePicker(contact.customRingtone) }
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
                ) { lightHaptic(); onShowCallingSimDialog() }
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

// History: title + last-call summary + chevron;
// tapping it opens the full contact history screen.
Spacer(modifier = Modifier.height(spacing.medium))
val historySource = remember { MutableInteractionSource() }
val lastCall = uiState.callHistory.firstOrNull()
DetailGlassCard(modifier = Modifier.fillMaxWidth()) {
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(id = R.string.contact_details_history),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (lastCall == null) {
                    stringResource(id = R.string.contact_details_history_empty)
                } else {
                    lastCallSummary(lastCall)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            painter = painterResource(id = R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(PhoneDimens.detailRowIconSize)
        )
    }
}

Spacer(modifier = Modifier.height(spacing.large))
}
}
