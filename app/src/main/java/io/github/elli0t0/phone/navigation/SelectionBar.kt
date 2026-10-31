package io.github.elli0t0.phone.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.design.bottomBarSizeSpec
import io.github.elli0t0.phone.core.design.isDarkGlassTheme
import io.github.elli0t0.phone.core.design.liquidGlass

/**
 * The bottom bar's selection-mode replacement: an X to exit, the
 * selection count only (one line, never wrapped), a select-all
 * toggle, and the screen's actions. Same pill geometry and bottom
 * clearance as the navigation bar, so lists keep their existing
 * bottom padding above it. Glass like the bottom bar, with a
 * near-opaque surface under it so list text never shows through.
 */
@Composable
fun SelectionBar(state: SelectionBarState, modifier: Modifier = Modifier) {
    val sizeSpec = bottomBarSizeSpec(LocalAppSettings.current.bottomBarSize)
    Box(
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = PhoneDimens.bottomBarBottomMargin)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(sizeSpec.widthFraction)
                .height(sizeSpec.height)
                .liquidGlass(
                    shape = RoundedCornerShape(PhoneDimens.bottomBarCornerPercent)
                )
                .background(
                    MaterialTheme.colorScheme.surface.copy(
                        alpha = if (isDarkGlassTheme()) 0.80f else 0.85f
                    )
                )
                .padding(horizontal = LocalSpacing.current.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = state.onClose,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = stringResource(id = R.string.action_close),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = state.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
            )
            Checkbox(
                checked = state.allSelected,
                onCheckedChange = { state.onToggleSelectAll() },
                modifier = Modifier.size(40.dp)
            )
            // Qualified call to an extension-typed property: the
            // RowScope receiver must be passed explicitly.
            state.actions(this)
        }
    }
}
