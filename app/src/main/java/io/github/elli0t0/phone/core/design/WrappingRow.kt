package io.github.elli0t0.phone.core.design

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp

/**
 * A row that wraps its children onto multiple lines, with fixed spacing
 * between items and between rows, and per-row horizontal alignment.
 * Built on the stable Layout API only: FlowRow's overload changed between
 * foundation-layout versions, and the app must not depend on the
 * version-sensitive overload.
 */
@Composable
fun WrappingRow(
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp,
    verticalSpacing: Dp,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable () -> Unit
) {
    Layout(content, modifier) { measurables, constraints ->
        val hSpacing = horizontalSpacing.roundToPx()
        val vSpacing = verticalSpacing.roundToPx()
        val maxWidth = if (constraints.maxWidth == Constraints.Infinity) Int.MAX_VALUE else constraints.maxWidth
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val rows = mutableListOf<MutableList<Placeable>>()
        var row = mutableListOf<Placeable>()
        var rowWidth = 0
        for (placeable in placeables) {
            val widthWithSpacing = if (row.isEmpty()) placeable.width else rowWidth + hSpacing + placeable.width
            if (row.isNotEmpty() && widthWithSpacing > maxWidth) {
                rows.add(row)
                row = mutableListOf()
                rowWidth = 0
            }
            if (row.isNotEmpty()) rowWidth += hSpacing
            row.add(placeable)
            rowWidth += placeable.width
        }
        if (row.isNotEmpty()) rows.add(row)
        val rowWidths = rows.map { r -> r.sumOf { it.width } + hSpacing * (r.size - 1).coerceAtLeast(0) }
        val rowHeights = rows.map { r -> r.maxOf { it.height } }
        val contentWidth = rowWidths.maxOrNull() ?: 0
        val contentHeight = rowHeights.sum() + vSpacing * (rows.size - 1).coerceAtLeast(0)
        val width = contentWidth.coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = contentHeight.coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(width, height) {
            var y = 0
            for ((index, r) in rows.withIndex()) {
                val rowHeight = rowHeights[index]
                val xStart = when (horizontalAlignment) {
                    Alignment.CenterHorizontally -> (width - rowWidths[index]) / 2
                    Alignment.End -> width - rowWidths[index]
                    else -> 0
                }
                var x = xStart
                for (placeable in r) {
                    placeable.placeRelative(x, y)
                    x += placeable.width + hSpacing
                }
                y += rowHeight + vSpacing
            }
        }
    }
}
