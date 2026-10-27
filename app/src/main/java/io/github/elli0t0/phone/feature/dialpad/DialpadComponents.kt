package io.github.elli0t0.phone.feature.dialpad

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.common.bidiWrap
import io.github.elli0t0.phone.core.design.LiquidGlassChip
import io.github.elli0t0.phone.core.design.LiquidGlassPill
import io.github.elli0t0.phone.core.design.KeypadKeyLightRim
import io.github.elli0t0.phone.core.design.LiquidGlassPillShape
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.design.LiquidGlassSurfaceLight
import io.github.elli0t0.phone.core.design.isDarkGlassTheme
import io.github.elli0t0.phone.core.design.liquidGlass
import io.github.elli0t0.phone.core.design.bouncyClick
import io.github.elli0t0.phone.core.design.glassPress
import io.github.elli0t0.phone.data.contacts.model.ContactSummary
import io.github.elli0t0.phone.feature.contacts.ui.ContactAvatar

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
        // Light keys: the glass fill alone is near-invisible on a
        // light background, so light keys get a slightly darker fill
        // and a 1dp 12% black rim to read as keys.
        val lightKey = !isDarkGlassTheme()
        Box(
            modifier = modifier
                .size(PhoneDimens.keypadTouchSize)
                .then(if (lightKey) Modifier.background(LiquidGlassSurfaceLight, CircleShape) else Modifier)
                .liquidGlass(shape = CircleShape)
                .then(if (lightKey) Modifier.border(1.dp, KeypadKeyLightRim, CircleShape) else Modifier)
                .glassPress(keySource)
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
 * T9 suggestions above the keypad: matching contacts as full-width
 * list rows (photo/initial avatar + name + number), up to three at a
 * time, with an "N more..." row that expands the rest in place. The
 * dialed digits inside the number are highlighted in the accent
 * color. Tapping a row selects the contact, filling its number into
 * the dialpad.
 */
@Composable
fun DialpadSuggestionChips(
    contacts: List<ContactSummary>,
    dialedDigits: String,
    onSelect: (ContactSummary) -> Unit,
    modifier: Modifier = Modifier,
    contactCounts: Map<Long, Int> = emptyMap()
) {
    val spacing = LocalSpacing.current
    var expanded by remember(dialedDigits) { mutableStateOf(false) }
    val capped = contacts.take(8)
    val visible = if (expanded) capped else capped.take(3)
    val suggestionRows: @Composable () -> Unit = {
        visible.forEach { contact ->
            DialpadSuggestionRow(
                contact = contact,
                dialedDigits = dialedDigits,
                count = contactCounts[contact.id] ?: 1,
                onClick = { onSelect(contact) }
            )
        }
    }
    Column(modifier = modifier.testTag("dialpad_suggestion_chips")) {
        if (expanded) {
            Column(
                modifier = Modifier
                    .heightIn(max = 232.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                suggestionRows()
            }
        } else {
            suggestionRows()
        }
        if (capped.size > 3) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = spacing.medium, vertical = spacing.small)
                    .testTag("dialpad_suggestions_more"),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (expanded) {
                        stringResource(id = R.string.dialpad_show_less)
                    } else {
                        stringResource(
                            id = R.string.dialpad_more_matches,
                            capped.size - 3
                        )
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/** One full-width suggestion row: avatar, name, highlighted number. */
@Composable
private fun DialpadSuggestionRow(
    contact: ContactSummary,
    dialedDigits: String,
    count: Int = 1,
    onClick: () -> Unit
) {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.medium, vertical = spacing.small)
            .testTag("dialpad_chip_${contact.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ContactAvatar(
            photoUri = contact.photoUri,
            displayName = contact.displayName,
            size = 40.dp,
            textStyle = MaterialTheme.typography.titleSmall
        )
        Spacer(modifier = Modifier.size(spacing.medium))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = bidiWrap(contact.displayName),
                    style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (count > 1) {
                    Spacer(modifier = Modifier.size(spacing.small))
                    Text(
                        text = stringResource(id = R.string.dialpad_contacts_count, count),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            val number = contact.primaryNumberPreview
            if (!number.isNullOrBlank()) {
                Text(
                    text = matchedNumberText(number, dialedDigits),
                    style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Content),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
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
 * contact prefilled with the number, add it to an existing contact,
 * or message the number. Three equal-width vertical chips (icon on
 * top, label below, up to two lines) so no label truncates at font
 * scale 1.3 on 360 dp or in Persian.
 */
@Composable
fun DialpadNoMatchActions(
    onCreateContact: () -> Unit,
    onAddToContact: () -> Unit,
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NoMatchGlassChip(
                    iconRes = R.drawable.ic_add,
                    labelRes = R.string.dialpad_new_contact,
                    chipTestTag = "dialpad_no_match_create",
                    onClick = onCreateContact,
                    modifier = Modifier.weight(1f)
                )
                NoMatchGlassChip(
                    iconRes = R.drawable.ic_person,
                    labelRes = R.string.dialpad_add_to_contact,
                    chipTestTag = "dialpad_no_match_add",
                    onClick = onAddToContact,
                    modifier = Modifier.weight(1f)
                )
                NoMatchGlassChip(
                    iconRes = R.drawable.ic_message,
                    labelRes = R.string.dialpad_message,
                    chipTestTag = "dialpad_no_match_message",
                    onClick = onMessage,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onCreateContact,
                    modifier = Modifier
                        .weight(1f)
                        .height(PhoneDimens.noMatchChipHeight)
                        .testTag("dialpad_no_match_create"),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    NoMatchChipContent(
                        iconRes = R.drawable.ic_add,
                        labelRes = R.string.dialpad_new_contact
                    )
                }
                FilledTonalButton(
                    onClick = onAddToContact,
                    modifier = Modifier
                        .weight(1f)
                        .height(PhoneDimens.noMatchChipHeight)
                        .testTag("dialpad_no_match_add"),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    NoMatchChipContent(
                        iconRes = R.drawable.ic_person,
                        labelRes = R.string.dialpad_add_to_contact
                    )
                }
                FilledTonalButton(
                    onClick = onMessage,
                    modifier = Modifier
                        .weight(1f)
                        .height(PhoneDimens.noMatchChipHeight)
                        .testTag("dialpad_no_match_message"),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    NoMatchChipContent(
                        iconRes = R.drawable.ic_message,
                        labelRes = R.string.dialpad_message
                    )
                }
            }
        }
    }
}

@Composable
private fun NoMatchGlassChip(
    iconRes: Int,
    labelRes: Int,
    chipTestTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val source = remember { MutableInteractionSource() }
    LiquidGlassPill(
        tint = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .height(PhoneDimens.noMatchChipHeight)
            .clip(LiquidGlassPillShape)
            .bouncyClick(source)
            .clickable(
                interactionSource = source,
                indication = ripple()
            ) { onClick() }
            .testTag(chipTestTag)
    ) {
        NoMatchChipContent(iconRes = iconRes, labelRes = labelRes)
    }
}

/**
 * Vertical no-match chip content: icon on top, label below, up to
 * two lines. Icon 20 dp + 4 dp gap + two 12 sp / 14 sp lines is
 * 60.4 dp at font scale 1.3, inside the 64 dp chip.
 */
@Composable
private fun NoMatchChipContent(
    iconRes: Int,
    labelRes: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(PhoneDimens.noMatchChipIconSize)
        )
        Spacer(modifier = Modifier.size(4.dp))
        Text(
            text = stringResource(id = labelRes),
            style = MaterialTheme.typography.labelMedium,
            fontSize = PhoneDimens.noMatchChipLabelSize,
            lineHeight = PhoneDimens.noMatchChipLabelLineHeight,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
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
                    text = bidiWrap(contact.displayName),
                    style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!contact.primaryNumberPreview.isNullOrBlank()) {
                    Text(
                        text = bidiWrap(contact.primaryNumberPreview),
                        style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Content),
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

/**
 * Empty-keypad quick redial: up to 3 chips with the last recent
 * numbers (name when known, else the formatted number). One tap
 * redials through the keypad's normal SIM-aware dial path.
 */
@Composable
fun QuickRedialChips(
    entries: List<QuickRedialEntry>,
    onRedial: (QuickRedialEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    Column(modifier = modifier) {
        Text(
            text = stringResource(id = R.string.quick_redial_recent),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = spacing.small)
        )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        entries.forEachIndexed { index, entry ->
            val chipModifier = Modifier.weight(1f).testTag("dialpad_quick_redial_$index")
            if (LocalAppSettings.current.glassDialpad) {
                val source = remember { MutableInteractionSource() }
                LiquidGlassPill(
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = chipModifier
                        .clip(LiquidGlassPillShape)
                        .bouncyClick(source)
                        .clickable(
                            interactionSource = source,
                            indication = ripple()
                        ) { onRedial(entry) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_phone),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            text = bidiWrap(entry.label),
                            style = MaterialTheme.typography.labelLarge.copy(textDirection = TextDirection.Content),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                FilledTonalButton(
                    onClick = { onRedial(entry) },
                    modifier = chipModifier
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_phone),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = bidiWrap(entry.label),
                        style = MaterialTheme.typography.labelLarge.copy(textDirection = TextDirection.Content),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
    }
}
