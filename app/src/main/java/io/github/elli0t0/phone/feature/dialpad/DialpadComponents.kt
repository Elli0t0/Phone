package io.github.elli0t0.phone.feature.dialpad

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.Motion
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.design.pressScale
import io.github.elli0t0.phone.data.contacts.model.ContactSummary
import io.github.elli0t0.phone.data.settings.model.AnimationOption
import kotlinx.coroutines.launch

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

    // Shared press source: pressScale and combinedClickable observe the
    // same press state, so the key scales to 0.92 while pressed and ripples.
    val keySource = remember { MutableInteractionSource() }
    // One keypress = one animation, restarted via key(pressCount): the
    // digit dips 0.1 scale / 0.3 alpha and springs back (stiffness 1000,
    // damping 0.7). Reduced fades only; Off does nothing.
    var pressCount by remember { mutableStateOf(0) }
    val animationOption = LocalAppSettings.current.animationOption
    Box(
        modifier = modifier
            .size(PhoneDimens.keypadTouchSize)
            .pressScale(keySource)
            .combinedClickable(
                interactionSource = keySource,
                indication = ripple(),
                onClick = {
                    pressCount++
                    onClick()
                },
                onLongClick = {
                    pressCount++
                    if (onLongClick != null) onLongClick() else onClick()
                }
            )
            .testTag("dial_key_$keyTag"),
        contentAlignment = Alignment.Center
    ) {
        key(pressCount) {
            val digitScale = remember { Animatable(1f) }
            val digitAlpha = remember { Animatable(1f) }
            LaunchedEffect(pressCount) {
                if (pressCount == 0) return@LaunchedEffect
                when (animationOption) {
                    AnimationOption.ON -> {
                        digitScale.snapTo(0.9f)
                        digitAlpha.snapTo(0.7f)
                        val pressSpring = spring<Float>(
                            stiffness = 1000f,
                            dampingRatio = 0.7f
                        )
                        launch { digitScale.animateTo(1f, pressSpring) }
                        launch { digitAlpha.animateTo(1f, pressSpring) }
                    }
                    AnimationOption.REDUCED -> {
                        digitAlpha.snapTo(0.7f)
                        digitAlpha.animateTo(
                            1f,
                            Motion.tweenSpec(
                                Motion.Durations.Short,
                                Motion.Easings.Standard,
                                animationOption
                            )
                        )
                    }
                    AnimationOption.OFF -> Unit
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.graphicsLayer {
                    scaleX = digitScale.value
                    scaleY = digitScale.value
                    alpha = digitAlpha.value
                }
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
    }
}

/**
 * T9 suggestion chips above the keypad: matching contacts as a
 * horizontal chip row (initial avatar + name). Tapping a chip selects
 * the contact, filling its number into the dialpad.
 */
@Composable
fun DialpadSuggestionChips(
    contacts: List<ContactSummary>,
    onSelect: (ContactSummary) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    LazyRow(
        modifier = modifier.testTag("dialpad_suggestion_chips"),
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(contacts.take(8), key = { it.id }) { contact ->
            AssistChip(
                onClick = { onSelect(contact) },
                label = {
                    Text(
                        text = contact.displayName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = contact.displayName.trim().take(1).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                modifier = Modifier.testTag("dialpad_chip_${contact.id}")
            )
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
