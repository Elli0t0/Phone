package io.github.elli0t0.phone.navigation

import androidx.compose.foundation.background
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
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.PhoneDimens

/**
 * Expanded header height: ~38% of the screen, at least 260dp.
 * Shared by the header item and the floating pill (the pill fades in
 * across exactly this scroll distance).
 */
@Composable
fun pageHeaderHeight(): Dp {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    return maxOf(
        screenHeight * PhoneDimens.headerHeightFraction,
        PhoneDimens.headerMinHeight
    )
}

/**
 * Large page header (Recents, Contacts) as the first LazyColumn item.
 * Title centered at ~42% of the header height, subtitle under it, action
 * row right-aligned at the bottom, 12dp above the first list item.
 * Scrolls away naturally: no snapping, no height animation.
 */
@Composable
fun PageHeaderItem(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
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
        // Title block centered at ~42% of the header height: centered in
        // the box, then shifted up by the 50% - 42% difference.
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
 */
@Composable
fun FloatingActionsPill(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit
) {
    val pillVisible by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 ||
                listState.firstVisibleItemScrollOffset > 0
        }
    }
    if (!pillVisible) return
    val density = LocalDensity.current
    val headerHeightPx = with(density) { pageHeaderHeight().toPx() }
    Box(
        modifier = modifier.graphicsLayer {
            val progress = if (headerHeightPx <= 0f) {
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
        GlassSurface(shape = LiquidGlassPillShape) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
        }
    }
}
