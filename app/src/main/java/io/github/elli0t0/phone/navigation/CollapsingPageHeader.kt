package io.github.elli0t0.phone.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import io.github.elli0t0.phone.core.design.glassBlurModifier
import io.github.elli0t0.phone.core.design.glassSolidBaseModifier
import io.github.elli0t0.phone.core.design.isDarkGlassTheme
import io.github.elli0t0.phone.data.settings.model.HeaderSize

/**
 * Expanded header height: the Header size setting's fraction of the
 * screen (Compact 16%, Standard 30%, Large 38%), at least 160dp.
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
 * row on the same row as the title, right-aligned.
 * [bottomContent] is an optional full-width row on the title row too
 * (e.g. Contacts' split top bar): it lives inside the header block,
 * so the header still caps at its height fraction. Pass either
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
            .background(Color.Transparent),
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
        // Action row on the title row, right-aligned: the same -10%
        // offset as the title block, so the icons sit on the title's
        // row instead of far below it.
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(y = headerHeight * (PhoneDimens.headerTitleCenterFraction - 0.5f)),
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )
        // Full-width row (Contacts' split top bar): on the title row
        // too, so the block stays one fraction tall.
        if (bottomContent != null) {
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .offset(y = headerHeight * (PhoneDimens.headerTitleCenterFraction - 0.5f)),
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

/**
 * Sticky glass top bar for a tab list (Recents, Contacts): an overlay
 * drawn after the list (a sibling of it, never a list item), covering
 * the status-bar inset plus a compact 56dp title row, with the
 * collapsed title centered and the header's action icons reachable.
 * Invisible while the large header is fully visible; fades in as the
 * header scrolls away, driven by the list state against the header
 * height. The bar blurs the scrolling content under it with the
 * existing glass style, plus a 16dp fade band below it (blur
 * fading into the background) so content dissolves instead of clipping. The
 * content is only composed once scrolling starts: popups in the
 * actions ignore alpha, so composing them at alpha 0 would open
 * menus from an invisible bar (the FloatingActionsPill lesson).
 * Falls back to the solid treatment when blur is unavailable
 * (Reduce transparency / API < 31) via the shared glass helpers.
 * Other tab lists can reuse it by passing their title and actions.
 */
@Composable
fun StickyGlassTopBar(
    title: String,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    leftContent: @Composable RowScope.() -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    val contentVisible by remember(listState) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 ||
                listState.firstVisibleItemScrollOffset > 0
        }
    }
    if (!contentVisible) return
    val headerHeightPx = with(LocalDensity.current) { pageHeaderHeight().toPx() }
    val barAlpha by remember(listState, headerHeightPx) {
        derivedStateOf {
            when {
                listState.firstVisibleItemIndex > 0 -> 1f
                listState.firstVisibleItemScrollOffset <= 0 -> 0f
                else -> (listState.firstVisibleItemScrollOffset / headerHeightPx)
                    .coerceIn(0f, 1f)
            }
        }
    }
    val dark = isDarkGlassTheme()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = barAlpha }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(glassBlurModifier(dark))
                .then(glassSolidBaseModifier(dark))
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    content = leftContent
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions
                )
            }
        }
        // Fade band below the bar: uniform blur plus a background
        // gradient (transparent at the bar's edge, opaque at the
        // bottom) so scrolling content dissolves over 16dp instead
        // of clipping under a hard edge.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .then(glassBlurModifier(dark))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )
        }
    }
}

