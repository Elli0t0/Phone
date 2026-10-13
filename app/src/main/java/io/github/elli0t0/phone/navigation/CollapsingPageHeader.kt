package io.github.elli0t0.phone.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import io.github.elli0t0.phone.core.design.PhoneDimens

/**
 * Large collapsing page header: a tall expanded area (~30% of the screen)
 * with a big centered bold title, collapsing to a small top bar with a small
 * centered title as the list scrolls. The action icons sit right-aligned at
 * the bottom of the expanded header, at the right of the collapsed bar.
 * The header background is the page background, so there is no visible band.
 */
@Composable
fun CollapsingPageHeader(
    title: String,
    subtitle: String?,
    listState: LazyListState,
    actions: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val collapseRangePx = with(density) { 180.dp.toPx() }
    val fraction = if (listState.firstVisibleItemIndex > 0) {
        1f
    } else {
        (listState.firstVisibleItemScrollOffset / collapseRangePx).coerceIn(0f, 1f)
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val expandedHeight = maxHeight * 0.30f
        val headerHeight = lerp(expandedHeight, PhoneDimens.headerCollapsedHeight, fraction)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = lerp(34.sp, 20.sp, fraction),
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.alpha(1f - fraction)
                    )
                }
            }
            Row(
                modifier = Modifier
                    .align(
                        if (fraction > 0.5f) Alignment.CenterEnd else Alignment.BottomEnd
                    )
                    .padding(end = 4.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
        }
    }
}
