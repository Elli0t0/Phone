package io.github.elli0t0.phone.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.elli0t0.phone.core.design.GlassSurface
import io.github.elli0t0.phone.core.design.LiquidGlassPillShape
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.design.glassBlurModifier
import io.github.elli0t0.phone.core.design.glassSolidBaseModifier
import io.github.elli0t0.phone.core.design.isDarkGlassTheme
import io.github.elli0t0.phone.data.settings.model.HeaderSize

/**
 * Measured height of the content-based header block, written by
 * PageHeaderItem's onSizeChanged and read by pageHeaderHeight()
 * (the sticky bar fades in from it). Starts at the old minimum;
 * the first layout pass corrects it before the bar can appear
 * (the bar only composes once scrolling starts).
 */
private val measuredHeaderHeightState = mutableStateOf(PhoneDimens.headerMinHeight)

/**
 * Current page-header height: the measured height of the
 * PageHeaderItem block (content-based, scaled by the Header size
 * setting), not a screen fraction.
 */
@Composable
fun pageHeaderHeight(): Dp = measuredHeaderHeightState.value

/**
 * Large page header (Recents, Contacts) as the first LazyColumn item.
 * Content-based height, scaled by the Header size setting only
 * (Compact 0.85x, Standard 1x, Large 1.2x): Row 1 is the split top
 * bar (Contacts) or the actions (Recents, end aligned), Row 2 the
 * centered title, Row 3 the subtitle. Nothing is absolutely
 * positioned, so the rows cannot overlap each other or the next
 * list item. The block reports its measured height for the sticky
 * bar's fade.
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
    val density = LocalDensity.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp * scale, bottom = 12.dp)
            .onSizeChanged { size ->
                measuredHeaderHeightState.value = with(density) { size.height.toDp() }
            },
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
 * drawn after the list (a sibling of it, never a list item), placed
 * at the very top of the screen at full width (no Scaffold top inset
 * above it), with the collapsed title centered and the header's
 * action icons reachable. Total height is the status-bar inset plus
 * a compact 56dp title row: the inner Row carries the status inset
 * itself, so the blur covers the status-bar area too.
 * Invisible while the large header is fully visible; fades in as the
 * header scrolls away, driven by the list state against the measured
 * header height. The bar blurs the scrolling content under it with
 * the existing glass style, plus a 16dp fade band below it (theme
 * background at 0.6 fading to transparent, no blur on the band
 * itself) so content dissolves downward instead of clipping. The
 * content is only composed once scrolling starts: popups in the
 * actions ignore alpha, so composing them at alpha 0 would open
 * menus from an invisible bar (the FloatingActionsPill lesson).
 * Falls back to the solid treatment when blur is unavailable
 * (Reduce transparency / API < 31) via the shared glass helpers.
 * The compact bar renders plain icon buttons, never the
 * large-header pills. Other tab lists can reuse it by passing
 * their title and actions.
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
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp)
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
        // Fade band below the bar: theme background at 0.6 fading to
        // transparent over 16dp, so scrolling content dissolves
        // downward instead of clipping under a hard edge. No blur
        // on the band itself — the gradient does the fading.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .background(
                    Brush.verticalGradient(
                        0f to MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                        1f to Color.Transparent
                    )
                )
        )
    }
}

