package io.github.elli0t0.phone.core.design

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Progressive edge fades for scrolling content: instead of a hard
 * edge where content slides under the top area (or above the
 * floating nav), the content dissolves into the theme background
 * over [fadeHeight] of scroll — alpha from 0 to 1 across the first
 * ~24dp of scroll at the top, and back to 0 across the last ~24dp
 * of content at the bottom.
 *
 * Scroll state is read inside the draw lambda, so scrolling redraws
 * without recomposing. The fade color is the theme background, the
 * same trick as the bottom-bar scrim, but scroll-driven: at rest
 * (top of the list) no fade is drawn at all.
 *
 * [bottom] is false for the tab lists: MainActivity already fades
 * the bottom behind the floating bar, and a second gradient there
 * double-dims the content (see the note in ContactListScreen).
 */
fun Modifier.scrollEdgeFade(
    listState: LazyListState,
    fadeHeight: Dp = 24.dp,
    bottom: Boolean = true
): Modifier = composed {
    val background = MaterialTheme.colorScheme.background
    val fadePx = with(LocalDensity.current) { fadeHeight.toPx() }
    this.drawWithContent {
        drawContent()
        val topHeight = minOf(fadePx, size.height)
        val topProgress = if (listState.firstVisibleItemIndex > 0) {
            1f
        } else {
            (listState.firstVisibleItemScrollOffset / fadePx).coerceIn(0f, 1f)
        }
        if (topProgress > 0f) {
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        background.copy(alpha = topProgress),
                        background.copy(alpha = 0f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(0f, topHeight)
                ),
                size = Size(size.width, topHeight)
            )
        }
        val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()
        val bottomProgress = if (!bottom || last == null) {
            0f
        } else {
            ((last.offset + last.size - listState.layoutInfo.viewportEndOffset) / fadePx)
                .coerceIn(0f, 1f)
        }
        if (bottomProgress > 0f) {
            val bottomHeight = minOf(fadePx, size.height)
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        background.copy(alpha = 0f),
                        background.copy(alpha = bottomProgress)
                    ),
                    start = Offset(0f, size.height - bottomHeight),
                    end = Offset(0f, size.height)
                ),
                topLeft = Offset(0f, size.height - bottomHeight),
                size = Size(size.width, bottomHeight)
            )
        }
    }
}

/** [ScrollState] variant of [scrollEdgeFade], for vertically scrolled columns. */
fun Modifier.scrollEdgeFade(
    scrollState: ScrollState,
    fadeHeight: Dp = 24.dp
): Modifier = composed {
    val background = MaterialTheme.colorScheme.background
    val fadePx = with(LocalDensity.current) { fadeHeight.toPx() }
    this.drawWithContent {
        drawContent()
        val topHeight = minOf(fadePx, size.height)
        val topProgress = (scrollState.value / fadePx).coerceIn(0f, 1f)
        if (topProgress > 0f) {
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        background.copy(alpha = topProgress),
                        background.copy(alpha = 0f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(0f, topHeight)
                ),
                size = Size(size.width, topHeight)
            )
        }
        val bottomProgress =
            ((scrollState.maxValue - scrollState.value) / fadePx).coerceIn(0f, 1f)
        if (bottomProgress > 0f) {
            val bottomHeight = minOf(fadePx, size.height)
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        background.copy(alpha = 0f),
                        background.copy(alpha = bottomProgress)
                    ),
                    start = Offset(0f, size.height - bottomHeight),
                    end = Offset(0f, size.height)
                ),
                topLeft = Offset(0f, size.height - bottomHeight),
                size = Size(size.width, bottomHeight)
            )
        }
    }
}
