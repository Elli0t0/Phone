package io.github.elli0t0.phone.feature.dialpad

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LiquidGlassChip
import io.github.elli0t0.phone.core.design.LiquidGlassPill
import io.github.elli0t0.phone.core.design.LiquidGlassPillShape
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.design.liquidGlass
import io.github.elli0t0.phone.core.design.bouncyClick
import io.github.elli0t0.phone.data.contacts.model.ContactSummary

data class DialKey(
    val digit: Char,
    val letters: String
)

val DIAL_KEYS_GRID = listOf(
    listOf(DialKey('1', ""), DialKey('2', "ABC"), DialKey('3', "DEF")),
    listOf(DialKey('4', "GHI"), DialKey('5', "JKL"), DialKey('6', "MNO")),
    listOf(DialKey('7', "PQRS"), DialKey('8', "TUV"), DialKey('9', "WXYZ")),
    listOf(DialKey('*', ""), DialKey('0', "+"), DialKey('#', ""))
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialKeyView(
    dialKey: DialKey,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val keyTag = when (dialKey.digit) {
        '*' -> "star"
        '#' -> "hash"
        else -> dialKey.digit.toString()
    }

    // Shared press source: bouncyClick and combinedClickable observe
    // the same press state, so the key bounces (0.90) while pressed.
    // No ripple: the glass surface is the feedback.
    val keySource = remember { MutableInteractionSource() }
    val glassDialpad = LocalAppSettings.current.glassDialpad
    // Key content shared by the glass and flat branches.
    val keyContent: @Composable () -> Unit = {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (dialKey.digit == '*') {
                Icon(
                    painter = painterResource(id = R.drawable.ic_asterisk),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(PhoneDimens.keypadAsteriskSize)
                )
            } else {
                Text(
                    text = dialKey.digit.toString(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = PhoneDimens.keypadDigitSize,
                        fontWeight = FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            // Always emit the sublabel, even when empty: an empty Text
            // still occupies one line height, so every key has the same
            // height and the digits stay aligned.
            Text(
                text = dialKey.letters,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = PhoneDimens.keypadLetterSize,
                    fontWeight = FontWeight.Normal
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    if (glassDialpad) {
        Box(
            modifier = modifier
                .size(PhoneDimens.keypadTouchSize)
                .bouncyClick(keySource)
                .liquidGlass(shape = CircleShape, elevation = 0.dp)
                .combinedClickable(
                    interactionSource = keySource,
                    indication = null,
                    onClick = onClick,
                    onLongClick = {
                        if (onLongClick != null) onLongClick() else onClick()
                    }
                )
                .testTag("dial_key_$keyTag"),
            contentAlignment = Alignment.Center
        ) {
            keyContent()
        }
    } else {
        // Flat key: bare digits with press-scale and a circle-clipped ripple.
        Box(
            modifier = modifier
                .size(PhoneDimens.keypadTouchSize)
                .clip(CircleShape)
                .bouncyClick(keySource)
                .combinedClickable(
                    interactionSource = keySource,
                    indication = ripple(),
                    onClick = onClick,
                    onLongClick = {
                        if (onLongClick != null) onLongClick() else onClick()
                    }
                )
                .testTag("dial_key_$keyTag"),
            contentAlignment = Alignment.Center
        ) {
            keyContent()
        }
    }
}

/**
 * T9 suggestion chips above the keypad: matching contacts as a
 * horizontal glass chip row (initial avatar + name + matched number).
 * The dialed digits inside the number are highlighted in the accent
 * color. Tapping a chip selects the contact, filling its number into
 * the dialpad.
 */
@Composable
fun DialpadSuggestionChips(
    contacts: List<ContactSummary>,
    dialedDigits: String,
    onSelect: (ContactSummary) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val glassDialpad = LocalAppSettings.current.glassDialpad
    LazyRow(
        modifier = modifier.testTag("dialpad_suggestion_chips"),
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        // End padding so the last chip is never clipped abruptly.
        contentPadding = PaddingValues(horizontal = spacing.small)
    ) {
        items(contacts.take(8), key = { it.id }) { contact ->
            if (glassDialpad) {
                LiquidGlassChip(
                    selected = false,
                    onClick = { onSelect(contact) },
                    modifier = Modifier.testTag("dialpad_chip_${contact.id}")
                ) {
                    DialpadChipAvatar(contact.displayName)
                    Spacer(modifier = Modifier.size(6.dp))
                    Column {
                        Text(
                            text = contact.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val number = contact.primaryNumberPreview
                        if (!number.isNullOrBlank()) {
                            Text(
                                text = matchedNumberText(number, dialedDigits),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else {
                AssistChip(
                    onClick = { onSelect(contact) },
                    label = {
                        Text(
                            text = contact.displayName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = { DialpadChipAvatar(contact.displayName) },
                    modifier = Modifier.testTag("dialpad_chip_${contact.id}")
                )
            }
        }
    }
}

/** Initial avatar used by both the glass and the flat T9 chips. */
@Composable
private fun DialpadChipAvatar(name: String) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.trim().take(1).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

/**
 * The contact's number with the dialed digits highlighted in the accent
 * color. Matching is digits-only, mapped back onto the displayed text;
 * when the dialed digits do not appear in the number (e.g. a T9 name
 * match), the number is returned unstyled.
 */
@Composable
private fun matchedNumberText(number: String, dialed: String): AnnotatedString {
    val accent = MaterialTheme.colorScheme.primary
    val queryDigits = dialed.filter { it.isDigit() }
    if (queryDigits.isEmpty()) return AnnotatedString(number)
    val digitIndexOfChar = IntArray(number.length)
    val numberDigits = StringBuilder()
    number.forEachIndexed { i, ch ->
        if (ch.isDigit()) {
            digitIndexOfChar[i] = numberDigits.length
            numberDigits.append(ch)
        } else {
            digitIndexOfChar[i] = -1
        }
    }
    val matchStart = numberDigits.toString().indexOf(queryDigits)
    if (matchStart < 0) return AnnotatedString(number)
    val matchEnd = matchStart + queryDigits.length
    return buildAnnotatedString {
        number.forEachIndexed { i, ch ->
            val d = digitIndexOfChar[i]
            if (d in matchStart until matchEnd) {
                withStyle(SpanStyle(color = accent, fontWeight = FontWeight.SemiBold)) {
                    append(ch)
                }
            } else {
                append(ch)
            }
        }
    }
}

/**
 * Actions for a dialed number that matches no contact: create a new
 * contact prefilled with the number, or message the number.
 */
@Composable
fun DialpadNoMatchActions(
    onCreateContact: () -> Unit,
    onMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.dialpad_empty_search),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (LocalAppSettings.current.glassDialpad) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val createSource = remember { MutableInteractionSource() }
            LiquidGlassPill(
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(LiquidGlassPillShape)
                    .bouncyClick(createSource)
                    .clickable(
                        interactionSource = createSource,
                        indication = ripple()
                    ) { onCreateContact() }
                    .testTag("dialpad_no_match_create")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = stringResource(id = R.string.dialpad_create_contact),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            val messageSource = remember { MutableInteractionSource() }
            LiquidGlassPill(
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(LiquidGlassPillShape)
                    .bouncyClick(messageSource)
                    .clickable(
                        interactionSource = messageSource,
                        indication = ripple()
                    ) { onMessage() }
                    .testTag("dialpad_no_match_message")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_message),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = stringResource(id = R.string.dialpad_message),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onCreateContact,
                    modifier = Modifier.testTag("dialpad_no_match_create")
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(text = stringResource(id = R.string.dialpad_create_contact))
                }
                FilledTonalButton(
                    onClick = onMessage,
                    modifier = Modifier.testTag("dialpad_no_match_message")
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_message),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(text = stringResource(id = R.string.dialpad_message))
                }
            }
        }
    }
}

@Composable
fun DialpadSearchRow(
    contact: ContactSummary,
    onClick: () -> Unit,
    onCallClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = spacing.extraSmall)
            .clickable(onClick = onClick)
            .testTag("search_contact_${contact.id}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.medium, vertical = spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_person),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.size(spacing.medium))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!contact.primaryNumberPreview.isNullOrBlank()) {
                    Text(
                        text = contact.primaryNumberPreview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (onCallClick != null && !contact.primaryNumberPreview.isNullOrBlank()) {
                androidx.compose.material3.IconButton(
                    onClick = onCallClick,
                    modifier = Modifier.testTag("search_contact_call_${contact.id}")
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_phone),
                        contentDescription = stringResource(id = R.string.cd_call_button),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
