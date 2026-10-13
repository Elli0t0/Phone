package io.github.elli0t0.phone.feature.dialpad

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.PhoneDimens
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

    Box(
        modifier = modifier
            .size(PhoneDimens.keypadTouchSize)
            .pointerInput(dialKey) {
                detectTapGestures(
                    onPress = {
                        tryAwaitRelease()
                    },
                    onTap = { onClick() },
                    onLongPress = {
                        if (onLongClick != null) {
                            onLongClick()
                        } else {
                            onClick()
                        }
                    }
                )
            }
            .testTag("dial_key_$keyTag"),
        contentAlignment = Alignment.Center
    ) {
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
            if (dialKey.digit == '1') {
                Icon(
                    painter = painterResource(id = R.drawable.ic_voicemail),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(PhoneDimens.keypadVoicemailIconSize)
                )
            } else if (dialKey.letters.isNotEmpty()) {
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
