package io.github.elli0t0.phone.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import io.github.elli0t0.phone.core.design.PhoneDimens

/**
 * Large collapsing page header (Recents, Contacts).
 *
 * Expanded: tall area (~30% of screen height) with the title centered
 * and bold, plus an optional subtitle under it. The action icons sit
 * right-aligned at the bottom of the header.
 *
 * As the list scrolls, the header shrinks to a small top bar with a
 * small centered title and the icons at the right. Background matches
 * the page, so no band is visible.
 *
 * NOTE: [actions] is the LAST parameter (after [modifier] with its
 * default), so trailing-lambda calls bind to [actions], not to
 * [modifier]. Do not reorder the parameters.
 */
@Composable
fun CollapsingPageHeader(
    title: String,
    subtitle: String?,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit
) {
    val density = LocalDensity.current
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val expandedHeight = screenHeight * PhoneDimens.headerExpandedFraction
    val collapsedHeight = PhoneDimens.headerCollapsedHeight
    val collapseRangePx = with(density) { (expandedHeight - collapsedHeight).toPx() }
    val fraction = if (collapseRangePx <= 0f) {
        1f
    } else if (listState.firstVisibleItemIndex > 0) {
        1f
    } else {
        (listState.firstVisibleItemScrollOffset / collapseRangePx).coerceIn(0f, 1f)
    }
    val headerHeight = lerp(expandedHeight, collapsedHeight, fraction)
    val titleSize = lerp(
        PhoneDimens.headerTitleExpandedSize,
        PhoneDimens.headerTitleCollapsedSize,
        fraction
    )
    val subtitleAlpha = (1f - fraction * 2f).coerceIn(0f, 1f)
    val actionsBottomPadding = lerp(
        PhoneDimens.headerActionsBottomPaddingExpanded,
        PhoneDimens.headerActionsBottomPaddingCollapsed,
        fraction
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(headerHeight)
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = titleSize,
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
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.alpha(subtitleAlpha)
                )
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = actionsBottomPadding),
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )
    }
}
