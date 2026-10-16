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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import io.github.elli0t0.phone.core.design.GlassSurface
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.design.pressScale
import io.github.elli0t0.phone.core.design.rememberLightHaptic
import io.github.elli0t0.phone.data.contacts.model.ContactSummary

@Composable
fun ContactSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    placeholderText: String = stringResource(id = R.string.contacts_search_hint)
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
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier.testTag("contacts_search_input")
    )
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

    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = spacing.extraSmall)
            .pressScale(rowSource)
            .clickable(
                interactionSource = rowSource,
                indication = ripple()
            ) { lightHaptic(); onClick() }
            .testTag("contact_item_${contact.id}"),
        shape = MaterialTheme.shapes.extraLarge,
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
                    text = displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun ContactGroupCard(
    contacts: List<ContactSummary>,
    onContactClick: (ContactSummary) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = spacing.small),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        contacts.forEachIndexed { index, contact ->
            ContactGroupRow(
                contact = contact,
                onClick = { onContactClick(contact) }
            )
            if (index < contacts.lastIndex) {
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

@Composable
private fun ContactGroupRow(
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(rowSource)
            .clickable(
                interactionSource = rowSource,
                indication = ripple()
            ) { lightHaptic(); onClick() }
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
                text = displayName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun FavoritesHintRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val hintSource = remember { MutableInteractionSource() }
    val lightHaptic = rememberLightHaptic()
    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = spacing.small)
            .pressScale(hintSource)
            .clickable(
                interactionSource = hintSource,
                indication = ripple()
            ) { lightHaptic(); onClick() }
            .testTag("favorites_hint_row"),
        shape = RoundedCornerShape(PhoneDimens.contactsPillCornerPercent),
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
                color = MaterialTheme.colorScheme.onSurface
            )
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
    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = spacing.small)
            .pressScale(rowSource)
            .clickable(
                interactionSource = rowSource,
                indication = ripple()
            ) { lightHaptic(); onClick() }
            .testTag("profile_row"),
        shape = RoundedCornerShape(PhoneDimens.contactsPillCornerPercent),
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
                modifier = Modifier.testTag("avatar_profile")
            )
            Spacer(modifier = Modifier.width(spacing.medium))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
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
    // Compact centered card (~70% width): centered text, Merge pill
    // button below it, small circular dismiss at the top-right.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("duplicates_card"),
        contentAlignment = Alignment.Center
    ) {
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth(PhoneDimens.contactsDuplicatesCardWidthFraction),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.medium),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .size(PhoneDimens.contactsDuplicatesDismissSize)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable(onClick = onDismiss)
                            .testTag("button_duplicates_card_dismiss"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = stringResource(id = R.string.cd_dismiss),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(PhoneDimens.contactsDuplicatesDismissIconSize)
                        )
                    }
                }
                Text(
                    text = stringResource(id = R.string.duplicates_card_message, duplicateCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(spacing.small))
                Button(
                    onClick = onMergeClick,
                    shape = RoundedCornerShape(PhoneDimens.contactsPillCornerPercent),
                    modifier = Modifier.testTag("button_duplicates_card_merge")
                ) {
                    Text(text = stringResource(id = R.string.duplicates_merge))
                }
            }
        }
    }
}

@Composable
fun GroupsRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val groupsSource = remember { MutableInteractionSource() }
    val lightHaptic = rememberLightHaptic()
    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = spacing.small)
            .pressScale(groupsSource)
            .clickable(
                interactionSource = groupsSource,
                indication = ripple()
            ) { lightHaptic(); onClick() }
            .testTag("groups_row"),
        shape = RoundedCornerShape(PhoneDimens.contactsPillCornerPercent),
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
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_group),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(PhoneDimens.contactsGroupIconSize)
                )
            }
            Spacer(modifier = Modifier.width(spacing.medium))
            Text(
                text = stringResource(id = R.string.groups_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
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
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
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
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
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
            .pressScale(cardSource)
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
                text = displayName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
