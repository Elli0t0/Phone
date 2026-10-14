package io.github.elli0t0.phone.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.PhoneDimens

/**
 * Large page header (Recents, Contacts) as the first LazyColumn item.
 * Fixed compact height, title + subtitle start-aligned at the bottom,
 * scrolls away naturally. No size animation, no snap.
 */
@Composable
fun PageHeaderItem(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(PhoneDimens.headerExpandedHeight)
            .background(MaterialTheme.colorScheme.background)
            .padding(
                start = spacing.screenHorizontalPadding,
                end = spacing.screenHorizontalPadding,
                bottom = spacing.medium
            ),
        contentAlignment = Alignment.BottomStart
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = PhoneDimens.headerTitleExpandedSize,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Small pinned top bar. Its background and title fade in with graphicsLayer
 * alpha from the list scroll offset; the actions stay always visible.
 * The title is start-aligned with end padding reserved for the actions,
 * so it can never run under them. No size animation, no snap.
 */
@Composable
fun PinnedPageBar(
    title: String,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit
) {
    val density = LocalDensity.current
    val spacing = LocalSpacing.current
    val headerHeightPx = with(density) { PhoneDimens.headerExpandedHeight.toPx() }
    val barAlpha = if (listState.firstVisibleItemIndex > 0) {
        1f
    } else if (headerHeightPx <= 0f) {
        1f
    } else {
        (listState.firstVisibleItemScrollOffset / headerHeightPx).coerceIn(0f, 1f)
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(PhoneDimens.headerCollapsedHeight)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = barAlpha }
                .background(MaterialTheme.colorScheme.background)
                .padding(
                    start = spacing.screenHorizontalPadding,
                    end = PhoneDimens.headerBarActionsReservedWidth
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = PhoneDimens.headerTitleCollapsedSize,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )
    }
}
