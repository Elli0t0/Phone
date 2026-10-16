package io.github.elli0t0.phone.feature.contacts.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LiquidGlassChip
import io.github.elli0t0.phone.core.design.LiquidGlassPill
import io.github.elli0t0.phone.core.design.LiquidGlassPillShape
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.Motion
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.design.firstLoadReveal
import io.github.elli0t0.phone.core.design.liquidGlass
import io.github.elli0t0.phone.core.design.rememberLightHaptic
import io.github.elli0t0.phone.core.permissions.ContactsPermissionState
import io.github.elli0t0.phone.core.permissions.ContactsPermissionStatus
import io.github.elli0t0.phone.data.contacts.model.ContactSummary
import io.github.elli0t0.phone.data.contacts.model.DuplicateGroup
import io.github.elli0t0.phone.data.settings.model.AnimationOption
import io.github.elli0t0.phone.feature.contacts.ContactsUiState
import io.github.elli0t0.phone.navigation.FloatingActionsPill
import io.github.elli0t0.phone.navigation.PageHeaderItem
import kotlinx.coroutines.launch

@Composable
fun ContactListScreen(
    uiState: ContactsUiState,
    permissionState: ContactsPermissionState,
    onSearchQueryChanged: (String) -> Unit,
    onClearSearch: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onContactClick: (ContactSummary) -> Unit,
    onAddContactClick: () -> Unit,
    onFindDuplicates: () -> Unit,
    onShowFavoritesHint: () -> Unit = {},
    onShowDuplicates: () -> Unit = {},
    onDismissDuplicatesCard: () -> Unit = {},
    onGroupsClick: () -> Unit = {},
    onBackFromDuplicates: () -> Unit,
    onMergeRequest: (DuplicateGroup) -> Unit,
    onDismissMerge: () -> Unit,
    onConfirmMerge: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("contact_list_screen")
    ) { innerPadding ->
        if (uiState.showDuplicates && permissionState.status is ContactsPermissionStatus.Granted) {
            DuplicatesScreen(
                uiState = uiState,
                onBack = onBackFromDuplicates,
                onMergeRequest = onMergeRequest,
                onDismissMerge = onDismissMerge,
                onConfirmMerge = onConfirmMerge,
                modifier = Modifier.padding(innerPadding)
            )
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            when (val status = permissionState.status) {
                is ContactsPermissionStatus.Granted -> {
                    ContactsContent(
                        uiState = uiState,
                        onSearchQueryChanged = onSearchQueryChanged,
                        onClearSearch = onClearSearch,
                        onNavigateToSettings = onNavigateToSettings,
                        onContactClick = onContactClick,
                        onAddContactClick = onAddContactClick,
                        onFindDuplicates = onFindDuplicates,
                        onShowFavoritesHint = onShowFavoritesHint,
                        onShowDuplicates = onShowDuplicates,
                        onDismissDuplicatesCard = onDismissDuplicatesCard,
                        onGroupsClick = onGroupsClick
                    )
                }
                is ContactsPermissionStatus.NotAsked,
                is ContactsPermissionStatus.Denied -> {
                    PermissionCard(
                        title = stringResource(id = R.string.permission_contacts_title),
                        message = stringResource(id = R.string.permission_contacts_message),
                        buttonText = stringResource(id = R.string.permission_grant_button),
                        onButtonClick = permissionState.requestPermissions,
                        modifier = Modifier.padding(spacing.screenHorizontalPadding)
                    )
                }
                is ContactsPermissionStatus.PermanentlyDenied -> {
                    PermissionCard(
                        title = stringResource(id = R.string.permission_contacts_title),
                        message = stringResource(id = R.string.permission_settings_message),
                        buttonText = stringResource(id = R.string.permission_settings_button),
                        onButtonClick = permissionState.openSettings,
                        modifier = Modifier.padding(spacing.screenHorizontalPadding)
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactsContent(
    uiState: ContactsUiState,
    onSearchQueryChanged: (String) -> Unit,
    onClearSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onContactClick: (ContactSummary) -> Unit,
    onAddContactClick: () -> Unit,
    onFindDuplicates: () -> Unit,
    onShowFavoritesHint: () -> Unit,
    onShowDuplicates: () -> Unit,
    onDismissDuplicatesCard: () -> Unit,
    onGroupsClick: () -> Unit
) {
    val spacing = LocalSpacing.current
    var searchVisible by remember { mutableStateOf(uiState.searchQuery.isNotEmpty()) }
    var menuExpanded by remember { mutableStateOf(false) }
    // First-load reveal: flips true when the first contacts arrive.
    var listRevealed by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.allContacts.isNotEmpty()) {
        if (uiState.allContacts.isNotEmpty()) listRevealed = true
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val contactCount = uiState.allContacts.size
    val countText = if (contactCount == 1) {
        stringResource(id = R.string.contacts_count_single, contactCount)
    } else {
        stringResource(id = R.string.contacts_count_multiple, contactCount)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top-bar pills: Groups on the left, actions on the right.
        // Shared by the top-bar list item and the floating pill
        // (same lambda, one menu).
        val topBarPills: @Composable RowScope.() -> Unit = {
            ContactsPillButton(
                onClick = onGroupsClick,
                testTag = "button_groups_pill"
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_group),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(PhoneDimens.contactsGroupIconSize)
                )
                Spacer(modifier = Modifier.width(spacing.small))
                Text(
                    text = stringResource(id = R.string.groups_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(PhoneDimens.contactsTopBarPillGap))
            ContactsPillContainer {
                IconButton(
                    onClick = onAddContactClick,
                    modifier = Modifier.testTag("button_add_contact")
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add),
                        contentDescription = stringResource(id = R.string.cd_add_contact),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = {
                        if (searchVisible) onClearSearch()
                        searchVisible = !searchVisible
                    },
                    modifier = Modifier.testTag("button_search_contacts")
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_search),
                        contentDescription = stringResource(id = R.string.cd_search),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.testTag("button_contacts_menu")
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_more_vert),
                            contentDescription = stringResource(id = R.string.cd_more_options),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(text = stringResource(id = R.string.settings_title)) },
                            onClick = { menuExpanded = false; onNavigateToSettings() },
                            modifier = Modifier.testTag("button_settings")
                        )
                        DropdownMenuItem(
                            text = { Text(text = stringResource(id = R.string.duplicates_find)) },
                            onClick = { menuExpanded = false; onFindDuplicates() },
                            enabled = !uiState.isFindingDuplicates,
                            modifier = Modifier.testTag("button_find_duplicates")
                        )
                    }
                }
            }
        }

        if (searchVisible) {
            ContactSearchBar(
                query = uiState.searchQuery,
                onQueryChange = onSearchQueryChanged,
                onClear = { onClearSearch(); searchVisible = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.screenHorizontalPadding, vertical = spacing.small)
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.allContacts.isEmpty() && !uiState.isLoading) {
            EmptyContactsState(onAddContactClick = onAddContactClick)
        } else if (uiState.filteredContacts.isEmpty() && uiState.searchQuery.isNotEmpty()) {
            SearchEmptyState(query = uiState.searchQuery)
        } else {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = spacing.screenHorizontalPadding)
                .testTag("contacts_lazy_column"),
            // Clearance above the overlaid bottom bar:
            // bar height + nav-bar inset + 16dp gap.
            contentPadding = PaddingValues(
                bottom = PhoneDimens.bottomBarReservedHeight +
                    WindowInsets.navigationBars.asPaddingValues()
                        .calculateBottomPadding()
            )
        ) {
            // Reveal index: assigned in list order, so the
            // first 8 rows stagger in on first load.
            var revealIndex = 0
            // Large header as the first list item: scrolls away naturally.
            item(key = "contacts_header") {
                PageHeaderItem(
                    title = stringResource(id = R.string.tab_contacts),
                    subtitle = countText,
                    modifier = Modifier
                        .animateItem()
                        .firstLoadReveal(revealIndex++, listRevealed)
                )
            }
            // Split glass top bar: Groups pill left, actions pill right.
            item(key = "contacts_top_bar") {
                Row(
                    modifier = Modifier
                        .animateItem()
                        .firstLoadReveal(revealIndex++, listRevealed)
                        .fillMaxWidth()
                        .padding(bottom = spacing.small),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    topBarPills()
                }
            }
            if (uiState.showDuplicatesCard && uiState.searchQuery.isEmpty()) {
                item(key = "duplicates_card") {
                    DuplicatesCard(
                        duplicateCount = uiState.duplicateGroups.sumOf { it.contacts.size },
                        onMergeClick = onShowDuplicates,
                        onDismiss = onDismissDuplicatesCard,
                        modifier = Modifier
                            .animateItem()
                            .firstLoadReveal(revealIndex++, listRevealed)
                    )
                }
            }

            if (uiState.favorites.isNotEmpty() && uiState.searchQuery.isEmpty()) {
                item(key = "header_favorites") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .animateItem()
                            .firstLoadReveal(revealIndex++, listRevealed)
                            .padding(vertical = spacing.small)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_star),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(spacing.extraSmall))
                        Text(
                            text = stringResource(id = R.string.section_favorites),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                uiState.favorites.chunked(3).forEachIndexed { rowIndex, rowContacts ->
                    item(key = "favorites_row_$rowIndex") {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(spacing.small),
                            modifier = Modifier
                                .animateItem()
                                .firstLoadReveal(revealIndex++, listRevealed)
                                .fillMaxWidth()
                                .padding(bottom = spacing.small)
                        ) {
                            rowContacts.forEach { contact ->
                                FavoriteContactCard(
                                    contact = contact,
                                    onClick = { onContactClick(contact) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - rowContacts.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            if (uiState.favorites.isEmpty() && uiState.searchQuery.isEmpty()) {
                item(key = "favorites_hint") {
                    FavoritesHintRow(
                        onClick = onShowFavoritesHint,
                        modifier = Modifier
                            .animateItem()
                            .firstLoadReveal(revealIndex++, listRevealed)
                    )
                }
            }

            val profileContact = uiState.profileContact
            if (profileContact != null && uiState.searchQuery.isEmpty()) {
                item(key = "header_profile") {
                    Text(
                        text = stringResource(id = R.string.contacts_my_profile),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .animateItem()
                            .firstLoadReveal(revealIndex++, listRevealed)
                            .padding(vertical = spacing.small)
                    )
                }
                item(key = "profile_row") {
                    ProfileRow(
                        contact = profileContact,
                        onClick = { onContactClick(profileContact) },
                        modifier = Modifier
                            .animateItem()
                            .firstLoadReveal(revealIndex++, listRevealed)
                    )
                }
            }

            uiState.groupedContacts.forEach { (letter, contactsInGroup) ->
                item(key = "header_$letter") {
                    Text(
                        text = letter,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .animateItem()
                            .firstLoadReveal(revealIndex++, listRevealed)
                            .padding(vertical = spacing.small)
                    )
                }

                item(key = "group_$letter") {
                    ContactGroupCard(
                        contacts = contactsInGroup,
                        onContactClick = onContactClick,
                        modifier = Modifier
                            .animateItem()
                            .firstLoadReveal(revealIndex++, listRevealed)
                    )
                }
            }
        }
        }
        // Floating glass pill with the top-bar pills, overlaid at the
        // top-right of the list area once the header scrolls off.
        FloatingActionsPill(
            listState = listState,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp),
            actions = topBarPills
        )
        // A–Z fast scroller along the right edge.
        if (uiState.groupedContacts.isNotEmpty()) {
            ContactsFastScroller(
                availableLetters = uiState.groupedContacts.keys,
                onLetterSelected = { letter ->
                    scope.launch {
                        listState.scrollToItem(headerItemIndex(letter, uiState))
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(
                        top = PhoneDimens.contactsScrubberTopPadding,
                        end = PhoneDimens.contactsScrubberEndPadding
                    )
            )
        }
        }
    }
}

/**
 * A pill button (e.g. Groups) that renders glass when the headers
 * glass switch is on, flat surface + soft shadow when off.
 */
@Composable
private fun ContactsPillButton(
    onClick: () -> Unit,
    testTag: String,
    content: @Composable RowScope.() -> Unit
) {
    val pillContent: @Composable RowScope.() -> Unit = {
        Row(
            modifier = Modifier.padding(
                horizontal = PhoneDimens.contactsTopBarPillHorizontalPadding,
                vertical = PhoneDimens.contactsTopBarPillVerticalPadding
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
        }
    }
    if (LocalAppSettings.current.glassHeaders) {
        LiquidGlassChip(
            selected = false,
            onClick = onClick,
            modifier = Modifier.testTag(testTag)
        ) {
            pillContent()
        }
    } else {
        Row(
            modifier = Modifier
                .shadow(
                    PhoneDimens.bottomBarElevation,
                    LiquidGlassPillShape,
                    spotColor = Color.Black.copy(alpha = PhoneDimens.bottomBarShadowAlpha),
                    ambientColor = Color.Black.copy(alpha = PhoneDimens.bottomBarShadowAlpha)
                )
                .clip(LiquidGlassPillShape)
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onClick)
                .testTag(testTag),
            verticalAlignment = Alignment.CenterVertically
        ) {
            pillContent()
        }
    }
}

/**
 * A pill container (e.g. the actions toolbar) that renders glass when
 * the headers glass switch is on, flat surface + soft shadow when off.
 */
@Composable
private fun ContactsPillContainer(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    if (LocalAppSettings.current.glassHeaders) {
        LiquidGlassPill(modifier = modifier) {
            content()
        }
    } else {
        Row(
            modifier = modifier
                .shadow(
                    PhoneDimens.bottomBarElevation,
                    LiquidGlassPillShape,
                    spotColor = Color.Black.copy(alpha = PhoneDimens.bottomBarShadowAlpha),
                    ambientColor = Color.Black.copy(alpha = PhoneDimens.bottomBarShadowAlpha)
                )
                .clip(LiquidGlassPillShape)
                .background(MaterialTheme.colorScheme.surface),
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
        }
    }
}

/**
 * Interactive vertical A–Z fast scroller along the right edge of the
 * contacts list. Dragging or tapping jumps to the nearest available
 * section; a glass preview bubble shows the current letter while
 * dragging.
 */
@Composable
private fun ContactsFastScroller(
    availableLetters: Set<String>,
    onLetterSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val alphabet = remember { ('A'..'Z').map { it.toString() } }
    val available = remember(availableLetters) { availableLetters.toSet() }
    val latestOnLetterSelected by rememberUpdatedState(onLetterSelected)
    val lightHaptic = rememberLightHaptic()
    val animationOption = LocalAppSettings.current.animationOption
    var dragging by remember { mutableStateOf(false) }
    var previewLetter by remember { mutableStateOf<String?>(null) }
    var selectedLetter by remember { mutableStateOf<String?>(null) }

    fun resolve(letter: String): String? {
        if (available.contains(letter)) return letter
        val index = alphabet.indexOf(letter)
        if (index < 0) return null
        return alphabet.subList(index, alphabet.size).firstOrNull { available.contains(it) }
            ?: alphabet.subList(0, index).lastOrNull { available.contains(it) }
    }

    fun handleY(yPx: Float, heightPx: Float) {
        if (heightPx <= 0f) return
        val index = ((yPx / heightPx) * alphabet.size).toInt().coerceIn(0, alphabet.lastIndex)
        val resolved = resolve(alphabet[index]) ?: return
        if (resolved != selectedLetter) {
            selectedLetter = resolved
            previewLetter = resolved
            lightHaptic()
            latestOnLetterSelected(resolved)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(PhoneDimens.contactsScrubberWidth)
    ) {
        val heightPx = with(LocalDensity.current) { maxHeight.toPx() }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(heightPx, alphabet, available) {
                    detectVerticalDragGestures(
                        onDragStart = { dragging = true },
                        onDragEnd = { dragging = false; previewLetter = null },
                        onDragCancel = { dragging = false; previewLetter = null },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            handleY(change.position.y, heightPx)
                        }
                    )
                }
                .pointerInput(heightPx, alphabet, available) {
                    detectTapGestures(onTap = { handleY(it.y, heightPx) })
                }
                .testTag("contacts_fast_scroller")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .liquidGlass(shape = RoundedCornerShape(percent = 50), elevation = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                alphabet.forEach { letter ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = letter,
                            fontSize = PhoneDimens.contactsScrubberLetterSize,
                            color = if (available.contains(letter)) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                            }
                        )
                    }
                }
            }
        }
        // Floating glass letter preview while dragging.
        val previewEnter: EnterTransition = when (animationOption) {
            AnimationOption.OFF -> EnterTransition.None
            AnimationOption.REDUCED -> fadeIn(
                animationSpec = Motion.tweenSpec(150, Motion.Easings.Standard, animationOption)
            )
            AnimationOption.ON -> fadeIn(
                animationSpec = Motion.tweenSpec(150, Motion.Easings.Standard, animationOption)
            ) + scaleIn(
                animationSpec = Motion.tweenSpec(150, Motion.Easings.Emphasized, animationOption),
                initialScale = 0.8f
            )
        }
        val previewExit: ExitTransition = when (animationOption) {
            AnimationOption.OFF -> ExitTransition.None
            AnimationOption.REDUCED -> fadeOut(
                animationSpec = Motion.tweenSpec(150, Motion.Easings.Standard, animationOption)
            )
            AnimationOption.ON -> fadeOut(
                animationSpec = Motion.tweenSpec(150, Motion.Easings.Standard, animationOption)
            ) + scaleOut(
                animationSpec = Motion.tweenSpec(150, Motion.Easings.Standard, animationOption),
                targetScale = 0.8f
            )
        }
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = dragging && previewLetter != null,
                enter = previewEnter,
                exit = previewExit,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(
                        x = -(PhoneDimens.contactsScrubberWidth / 2 +
                            PhoneDimens.contactsScrubberGap +
                            PhoneDimens.contactsScrubberPreviewSize / 2)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .size(PhoneDimens.contactsScrubberPreviewSize)
                        .liquidGlass(shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = previewLetter ?: "",
                        fontSize = PhoneDimens.contactsScrubberPreviewLetterSize,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * LazyColumn item index of the `header_$letter` section header,
 * mirroring the item order in ContactsContent so the fast scroller
 * can jump to it.
 */
private fun headerItemIndex(letter: String, uiState: ContactsUiState): Int {
    var index = 2 // contacts_header + contacts_top_bar
    if (uiState.searchQuery.isEmpty()) {
        if (uiState.showDuplicatesCard) index += 1
        if (uiState.favorites.isNotEmpty()) {
            index += 1 + (uiState.favorites.size + 2) / 3
        } else {
            index += 1 // favorites_hint
        }
        if (uiState.profileContact != null) index += 2
    }
    val sorted = uiState.groupedContacts.keys.sorted()
    val letterIndex = sorted.indexOf(letter)
    if (letterIndex < 0) return 0
    return index + letterIndex * 2
}
