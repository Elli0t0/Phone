package io.github.elli0t0.phone.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.elli0t0.phone.core.design.GlassSurface
import io.github.elli0t0.phone.core.design.LiquidGlassPillShape
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.LocalHazeState
import io.github.elli0t0.phone.core.design.LocalMinimal
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.data.settings.model.HeaderSize

/**
 * Large page header (Recents, Contacts) as the first LazyColumn item.
 * Content-based height, scaled by the Header size setting only
 * (Compact 0.85x, Standard 1x, Large 1.2x): Row 1 is the split top
 * bar (Contacts) or the actions (Recents, end aligned), Row 2 the
 * left-aligned title (same as Recents), Row 3 the subtitle. Nothing
 * is absolutely positioned, so the rows cannot overlap each other
 * or the next list item.
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
    val scale = when (LocalAppSettings.current.headerSize) {
        HeaderSize.COMPACT -> 0.85f
        HeaderSize.STANDARD -> 1f
        HeaderSize.LARGE -> 1.2f
    }
    // Minimal (0.18.10): tall title area (38% of screen height) with
    // the title big, bold and centered, then a plain icon row
    // aligned right — no pill, no glass, no subtitle, no chips.
    // The header size setting is ignored here.
    if (LocalMinimal.current) {
        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
        Column(
            modifier = modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(screenHeight * 0.38f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.4).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
        }
        return
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp * scale, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp * scale)
    ) {
        // Row 1: the split top bar (Contacts: Groups pill start,
        // actions pill end) or the actions (Recents, end aligned).
        if (bottomContent != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                content = bottomContent
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = PhoneDimens.headerTitleExpandedSize * scale,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Start,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
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
    wrapInPill: Boolean = true,
    // Minimal (0.18.10): when set, the tall header collapses to a
    // small centered title once it scrolls off, instead of a pill.
    minimalTitle: String? = null
) {
    val pillVisible by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > revealAfterIndex
        }
    }
    if (!pillVisible) return
    if (LocalMinimal.current && minimalTitle != null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = minimalTitle,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        return
    }
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
 * Status-bar gradient scrim: a fade of the page background behind
 * the status bar only (no title, no hairline, no band), so the
 * status icons stay readable over scrolling rows. Shown only when
 * the floating actions pill is visible, i.e. once the large header
 * has scrolled off — the same reveal condition as the pill.
 */
@Composable
fun StatusBarGradientScrim(
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val visible by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }
    if (!visible) return
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp)
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
                        Color.Transparent
                    )
                )
            )
    )
}
