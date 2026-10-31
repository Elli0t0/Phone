package io.github.elli0t0.phone.feature.contacts.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ripple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.FavoriteCardGradients
import io.github.elli0t0.phone.core.design.FavoriteHintOrange
import io.github.elli0t0.phone.core.design.GlassListCard
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.SwipeActionRow
import io.github.elli0t0.phone.core.design.LiquidGlassCard
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.design.bouncyClick
import io.github.elli0t0.phone.core.design.insetRowPress
import io.github.elli0t0.phone.core.design.liquidGlass
import io.github.elli0t0.phone.core.design.rememberLightHaptic
import io.github.elli0t0.phone.data.settings.model.CardStyle
import io.github.elli0t0.phone.data.contacts.model.ContactSummary
import io.github.elli0t0.phone.core.common.bidiWrap
import io.github.elli0t0.phone.core.common.textAlignFor
import androidx.compose.ui.text.style.TextDirection

@Composable
fun ContactSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    placeholderText: String = stringResource(id = R.string.contacts_search_hint)
) {
    Box(
        modifier = modifier.liquidGlass(
            shape = MaterialTheme.shapes.large,
            floating = false
        )
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(text = placeholderText) },
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_search),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = stringResource(id = R.string.cd_clear_search),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth().testTag("contacts_search_input")
        )
    }
}

@Composable
fun ContactRow(
    contact: ContactSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val displayName = contact.displayName.ifBlank {
        contact.primaryNumberPreview ?: stringResource(id = R.string.contact_unknown)
    }
    val rowSource = remember { MutableInteractionSource() }
    val lightHaptic = rememberLightHaptic()
    val insetCard = LocalAppSettings.current.effectiveStyle.card == CardStyle.INSET

    GlassListCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = spacing.extraSmall)
            .bouncyClick(rowSource)
            .clickable(
                interactionSource = rowSource,
                indication = if (insetCard) null else ripple()
            ) { lightHaptic(); onClick() }
            .then(if (insetCard) Modifier.insetRowPress(rowSource) else Modifier)
            .testTag("contact_item_${contact.id}"),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = spacing.medium,
                    vertical = PhoneDimens.contactsRowVerticalPadding
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContactAvatar(
                photoUri = contact.photoUri,
                displayName = displayName,
                size = PhoneDimens.contactsRowAvatarSize,
                modifier = Modifier.testTag("avatar_${contact.id}")
            )


            Spacer(modifier = Modifier.width(spacing.medium))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bidiWrap(displayName),
                    style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Content),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = textAlignFor(displayName),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun ContactGroupCard(
    contacts: List<ContactSummary>,
    onContactClick: (ContactSummary) -> Unit,
    modifier: Modifier = Modifier,
    // Null (e.g. group members) means no swipe actions on these rows.
    onSwipeCall: ((ContactSummary) -> Unit)? = null,
    onSwipeMessage: ((ContactSummary) -> Unit)? = null
) {
    val spacing = LocalSpacing.current
    val insetCard = LocalAppSettings.current.effectiveStyle.card == CardStyle.INSET
    GlassListCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = spacing.small),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        contacts.forEachIndexed { index, contact ->
            ContactGroupRow(
                contact = contact,
                onClick = { onContactClick(contact) },
                onSwipeCall = onSwipeCall,
                onSwipeMessage = onSwipeMessage
            )
            if (index < contacts.lastIndex) {
                if (insetCard) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = Color.White.copy(alpha = 0.12f),
                        // Divider starts at the text: row padding +
                        // avatar + spacer (72dp).
                        modifier = Modifier.padding(
                            start = spacing.medium +
                                PhoneDimens.contactsRowAvatarSize + spacing.medium
                        )
                    )
                } else {
                    HorizontalDivider(
                        // Subtle divider like Recents and History.
                        color = MaterialTheme.colorScheme.outlineVariant.copy(
                            alpha = PhoneDimens.recentsDividerAlpha
                        ),
                        // Divider starts at the text: row padding + avatar + spacer.
                        modifier = Modifier.padding(
                            start = spacing.medium +
                                PhoneDimens.contactsRowAvatarSize + spacing.medium
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactGroupRow(
    contact: ContactSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSwipeCall: ((ContactSummary) -> Unit)? = null,
    onSwipeMessage: ((ContactSummary) -> Unit)? = null
) {
    val spacing = LocalSpacing.current
    val displayName = contact.displayName.ifBlank {
        contact.primaryNumberPreview ?: stringResource(id = R.string.contact_unknown)
    }
    val rowSource = remember { MutableInteractionSource() }
    val lightHaptic = rememberLightHaptic()
    val insetCard = LocalAppSettings.current.effectiveStyle.card == CardStyle.INSET
    // The first number is the swipe target; a row without one
    // cannot call or message, so it never swipes.
    val swipeNumber = contact.primaryNumberPreview
    SwipeActionRow(
        enabled = LocalAppSettings.current.swipeActionsEnabled &&
            onSwipeCall != null && onSwipeMessage != null &&
            !swipeNumber.isNullOrBlank(),
        onSwipeCall = { swipeNumber?.let { onSwipeCall?.invoke(contact) } },
        onSwipeMessage = { swipeNumber?.let { onSwipeMessage?.invoke(contact) } },
        modifier = modifier.fillMaxWidth()
    ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClick(rowSource)
            .clickable(
                interactionSource = rowSource,
                indication = if (insetCard) null else ripple()
            ) { lightHaptic(); onClick() }
            .then(if (insetCard) Modifier.insetRowPress(rowSource) else Modifier)
            .testTag("contact_item_${contact.id}")
            .padding(
                horizontal = spacing.medium,
                vertical = PhoneDimens.contactsRowVerticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ContactAvatar(
            photoUri = contact.photoUri,
            displayName = displayName,
            size = PhoneDimens.contactsRowAvatarSize,
            modifier = Modifier.testTag("avatar_${contact.id}")
        )
        Spacer(modifier = Modifier.width(spacing.medium))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = bidiWrap(displayName),
                style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Content),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
    }
}

@Composable
fun FavoritesHintRow(
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val hintSource = remember { MutableInteractionSource() }
    val lightHaptic = rememberLightHaptic()
    val insetCard = LocalAppSettings.current.effectiveStyle.card == CardStyle.INSET
    GlassListCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = spacing.small)
            .bouncyClick(hintSource)
            .clickable(
                interactionSource = hintSource,
                indication = if (insetCard) null else ripple()
            ) { lightHaptic(); onClick() }
            .then(if (insetCard) Modifier.insetRowPress(hintSource) else Modifier)
            .testTag("favorites_hint_row"),
        shape = RoundedCornerShape(PhoneDimens.contactsPillCornerPercent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = spacing.medium,
                    vertical = PhoneDimens.contactsRowVerticalPadding
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(PhoneDimens.contactsRowAvatarSize)
                    .clip(CircleShape)
                    .background(FavoriteHintOrange),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_star),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(PhoneDimens.contactsFavoritesHintIconSize)
                )
            }
            Spacer(modifier = Modifier.width(spacing.medium))
            Text(
                text = stringResource(id = R.string.contacts_add_favorites),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("favorites_hint_dismiss")
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = stringResource(id = R.string.cd_dismiss),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(PhoneDimens.contactsDuplicatesDismissIconSize)
                )
            }
        }
    }
}

@Composable
fun ProfileRow(
    contact: ContactSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val displayName = contact.displayName.ifBlank {
        contact.primaryNumberPreview ?: stringResource(id = R.string.contact_unknown)
    }
    val rowSource = remember { MutableInteractionSource() }
    val lightHaptic = rememberLightHaptic()
    GlassListCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = spacing.small)
            .bouncyClick(rowSource)
            .clickable(
                interactionSource = rowSource,
                indication = ripple()
            ) { lightHaptic(); onClick() }
            .testTag("profile_row"),
        shape = RoundedCornerShape(PhoneDimens.contactsPillCornerPercent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = spacing.medium,
                    vertical = PhoneDimens.contactsRowVerticalPadding
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // The profile row always shows the initial letter, never
            // the device profile's photo tile: that tile was rendering
            // instead of the letter (a lowercase "a"), while every
            // other avatar routes through contactInitial. Passing no
            // photo forces ContactAvatar's initial branch.
            ContactAvatar(
                photoUri = null,
                displayName = displayName,
                size = PhoneDimens.contactsRowAvatarSize,
                modifier = Modifier.testTag("avatar_profile")
            )
            Spacer(modifier = Modifier.width(spacing.medium))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bidiWrap(displayName),
                    style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Content),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = textAlignFor(displayName),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun DuplicatesCard(
    duplicateCount: Int,
    onMergeClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    // Slim glass row, one line, max ~56dp high: count text, Merge
    // text button, close icon. Dismiss behaviour and tags unchanged.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("duplicates_card"),
        contentAlignment = Alignment.Center
    ) {
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            floating = false
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 56.dp)
                    .padding(
                        start = spacing.medium,
                        end = spacing.small,
                        top = spacing.small,
                        bottom = spacing.small
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.duplicates_card_message, duplicateCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = onMergeClick,
                    modifier = Modifier.testTag("button_duplicates_card_merge")
                ) {
                    Text(text = stringResource(id = R.string.duplicates_merge))
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("button_duplicates_card_dismiss")
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = stringResource(id = R.string.cd_dismiss),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(PhoneDimens.contactsDuplicatesDismissIconSize)
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    message: String,
    buttonText: String,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(spacing.cardContentPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(spacing.small))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(spacing.medium))
            Button(
                onClick = onButtonClick,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.testTag("button_permission_action")
            ) {
                Text(text = buttonText)
            }
        }
    }
}

@Composable
fun EmptyContactsState(
    onAddContactClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    Box(
        modifier = modifier.fillMaxSize().padding(spacing.large),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(spacing.cardContentPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.contacts_empty_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(spacing.small))
                Text(
                    text = stringResource(id = R.string.contacts_empty_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(spacing.medium))
                Button(
                    onClick = onAddContactClick,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.testTag("button_empty_add_contact")
                ) {
                    Text(text = stringResource(id = R.string.contacts_empty_action))
                }
            }
        }
    }
}

@Composable
fun SearchEmptyState(
    query: String,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    Box(
        modifier = modifier.fillMaxSize().padding(spacing.large),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(id = R.string.contacts_search_empty_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(spacing.small))
            Text(
                text = stringResource(id = R.string.contacts_search_empty_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FavoriteContactCard(
    contact: ContactSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayName = contact.displayName.ifBlank {
        contact.primaryNumberPreview ?: stringResource(id = R.string.contact_unknown)
    }
    val gradientIndex =
        ((contact.id % FavoriteCardGradients.size) + FavoriteCardGradients.size) % FavoriteCardGradients.size
    val gradient = FavoriteCardGradients[gradientIndex.toInt()]
    val gradientBrush = remember(gradient) {
        Brush.linearGradient(colors = listOf(gradient.first, gradient.second))
    }
    val cardSource = remember { MutableInteractionSource() }
    val lightHaptic = rememberLightHaptic()

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.large)
            .background(gradientBrush)
            .bouncyClick(cardSource)
            .clickable(
                interactionSource = cardSource,
                indication = ripple()
            ) { lightHaptic(); onClick() }
            .testTag("favorite_card_${contact.id}")
            .padding(PhoneDimens.contactsFavoriteCardPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ContactAvatar(
                photoUri = contact.photoUri,
                displayName = displayName,
                size = PhoneDimens.contactsFavoriteAvatarSize
            )
            Spacer(modifier = Modifier.height(PhoneDimens.contactsFavoriteAvatarNameGap))
            Text(
                text = bidiWrap(displayName),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, textDirection = TextDirection.Content),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
