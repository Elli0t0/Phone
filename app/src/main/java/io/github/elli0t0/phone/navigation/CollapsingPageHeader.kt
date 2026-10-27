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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
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
 * Shared by the header item and the floating pill (the pill fades in
 * across exactly this scroll distance).
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
 * once the header's icon row scrolls off. Fades and scales in from the
 * scroll offset via graphicsLayer (no recomposition while scrolling).
 *
 * The pill is only composed once scrolling starts: the same actions lambda
 * also renders in the header, and composing both at rest would show two
 * dropdown menus, since popups ignore the pill's alpha.
 *
 * [revealAfterIndex]: when > 0, the pill appears only once that item
 * has scrolled off. Contacts passes 1 (its in-list top-bar pills).
 */
@Composable
fun FloatingActionsPill(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit,
    revealAfterIndex: Int = 0
) {
    val pillVisible by remember {
        derivedStateOf {
            if (revealAfterIndex > 0) {
                listState.firstVisibleItemIndex > revealAfterIndex
            } else {
                listState.firstVisibleItemIndex > 0 ||
                    listState.firstVisibleItemScrollOffset > 0
            }
        }
    }
    if (!pillVisible) return
    val density = LocalDensity.current
    val headerHeightPx = with(density) { pageHeaderHeight().toPx() }
    Box(
        modifier = modifier.graphicsLayer {
            val progress = if (revealAfterIndex > 0) {
                // Instant full opacity.
                1f
            } else if (headerHeightPx <= 0f) {
                1f
            } else if (listState.firstVisibleItemIndex > 0) {
                1f
            } else {
                (listState.firstVisibleItemScrollOffset / headerHeightPx).coerceIn(0f, 1f)
            }
            alpha = progress
            val scale = 0.8f + 0.2f * progress
            scaleX = scale
            scaleY = scale
        }
    ) {
        // The pill content is shared by the glass and flat branches.
        val pillContent: @Composable () -> Unit = {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
        }
        if (LocalAppSettings.current.glassHeaders) {
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

/**
 * Bottom scrim behind the floating bottom bar: a 56dp gradient over
 * the list area fading to the page background at 55% alpha. It is
 * drawn BEHIND the list content (first child of the screen's root
 * box), so it tints only the background showing around the cards,
 * never the cards themselves. Real backdrop blur is not available
 * in this project (see the glass notes), so the gradient is the
 * honest fallback.
 */
@Composable
fun BottomListScrim(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(PhoneDimens.bottomScrimHeight)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        MaterialTheme.colorScheme.background.copy(
                            alpha = PhoneDimens.bottomScrimMaxAlpha
                        )
                    )
                )
            )
    )
}
