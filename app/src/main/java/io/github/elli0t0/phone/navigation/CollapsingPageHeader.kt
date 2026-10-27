package io.github.elli0t0.phone.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.elli0t0.phone.core.design.GlassSurface
import io.github.elli0t0.phone.core.design.LiquidGlassPillShape
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.data.settings.model.HeaderSize

/**
 * Expanded header height: the Header size setting's fraction of the
 * screen (Compact 22%, Standard 30%, Large 38%), at least 220dp.
 */
@Composable
fun pageHeaderHeight(): Dp {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val fraction = when (LocalAppSettings.current.headerSize) {
        HeaderSize.COMPACT -> PhoneDimens.headerHeightFractionCompact
        HeaderSize.STANDARD -> PhoneDimens.headerHeightFractionStandard
        HeaderSize.LARGE -> PhoneDimens.headerHeightFraction
    }
    return maxOf(
        screenHeight * fraction,
        PhoneDimens.headerMinHeight
    )
}

/**
 * Large page header (Recents, Contacts) as the first LazyColumn item.
 * Title centered at ~40% of the header height, subtitle under it, action
 * row right-aligned at the bottom, 12dp above the first list item.
 * [bottomContent] is an optional full-width row pinned to the header
 * bottom (e.g. Contacts' split top bar): it lives inside the header
 * block, so the header still caps at its height fraction. Pass either
 * [actions] or [bottomContent], not both — they would overlap.
 * Scrolls away naturally: no snapping, no height animation.
 */
@Composable
fun PageHeaderItem(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    bottomContent: (@Composable RowScope.() -> Unit)? = null
) {
    val spacing = LocalSpacing.current
    val headerHeight = pageHeaderHeight()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(headerHeight)
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // Title block centered at ~40% of the header height: centered in
        // the box, then shifted up by the 50% - 40% difference.
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = headerHeight * (PhoneDimens.headerTitleCenterFraction - 0.5f))
                .padding(horizontal = spacing.screenHorizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = PhoneDimens.headerTitleExpandedSize,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        // Action row at the bottom, right-aligned.
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = PhoneDimens.headerActionsBottomPaddingExpanded),
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )
        // Full-width bottom row (Contacts' split top bar): pinned to
        // the header bottom so the block stays one fraction tall.
        if (bottomContent != null) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = spacing.small),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                content = bottomContent
            )
        }
    }
}

/**
 * Floating glass pill with the header actions, overlaid at the top-right
 * once the header item has scrolled off completely. It used to fade in
 * from any scroll offset, so it overlapped the still-visible header
 * and its section header; the offset term is gone, and the pill
 * appears only once the header is fully off-screen.
 *
 * The pill is only composed once scrolling starts: the same actions lambda
 * also renders in the header, and composing both at rest would show two
 * dropdown menus, since popups ignore the pill's alpha.
 *
 * [revealAfterIndex]: the pill appears once the item at this index has
 * scrolled off (0 = the header item). [wrapInPill]: false when the
 * actions already render their own pill containers (Contacts' top-bar
 * pills) — wrapping them again would put pills inside a pill.
 */
@Composable
fun FloatingActionsPill(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit,
    revealAfterIndex: Int = 0,
    wrapInPill: Boolean = true
) {
    val pillVisible by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > revealAfterIndex
        }
    }
    if (!pillVisible) return
    Box(modifier = modifier) {
        // The pill content is shared by the glass and flat branches.
        val pillContent: @Composable () -> Unit = {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
        }
        if (!wrapInPill) {
            pillContent()
        } else if (LocalAppSettings.current.glassHeaders) {
            GlassSurface(shape = LiquidGlassPillShape) {
                pillContent()
            }
        } else {
            // Flat pill: solid surface with a soft shadow.
            Box(
                modifier = Modifier
                    .shadow(
                        PhoneDimens.bottomBarElevation,
                        LiquidGlassPillShape,
                        spotColor = Color.Black.copy(alpha = PhoneDimens.bottomBarShadowAlpha),
                        ambientColor = Color.Black.copy(alpha = PhoneDimens.bottomBarShadowAlpha)
                    )
                    .clip(LiquidGlassPillShape)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                pillContent()
            }
        }
    }
}

