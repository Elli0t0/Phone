package io.github.elli0t0.phone.navigation

import android.os.Build
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import io.github.elli0t0.phone.core.design.GlassBlurTintDark
import io.github.elli0t0.phone.core.design.GlassBlurTintLight
import io.github.elli0t0.phone.core.design.GlassSurface
import io.github.elli0t0.phone.core.design.LiquidGlassPillShape
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.LocalHazeState
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.design.isDarkGlassTheme
import io.github.elli0t0.phone.data.settings.model.GlassIntensity
import io.github.elli0t0.phone.data.settings.model.HeaderSize

/**
 * Large page header (Recents, Contacts) as the first LazyColumn item.
 * Content-based height, scaled by the Header size setting only
 * (Compact 0.85x, Standard 1x, Large 1.2x): Row 1 is the split top
 * bar (Contacts) or the actions (Recents, end aligned), Row 2 the
 * centered title, Row 3 the subtitle. Nothing is absolutely
 * positioned, so the rows cannot overlap each other or the next
 * list item.
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
    Box(modifier = modifier, contentAlignment = Alignment.CenterEnd) {
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
 * Collapsed header bar: once the large title has scrolled off (the
 * same reveal condition as the floating actions pill), the top area
 * becomes a full-width bar with a solid base behind the blur, so list
 * rows never show through, and a 0.5dp hairline at the bottom. The
 * title is start-aligned at a fixed 20sp SemiBold, capped at 45% of
 * the width so it never sits under the floating pill, in a 60dp row
 * the pill shares. It reads the shared haze state like the pill
 * (never the list's own state), so it cannot hit the haze/hazeChild
 * crash. Without blur (reduce transparency, API < 31, no state) the
 * base is near-opaque on its own.
 */
@Composable
fun CollapsedHeaderBar(
    listState: LazyListState,
    title: String,
    modifier: Modifier = Modifier,
    extraVisible: Boolean = true
) {
    val scrolled by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }
    if (!extraVisible || !scrolled) return
    val dark = isDarkGlassTheme()
    val settings = LocalAppSettings.current
    val hazeState = LocalHazeState.current
    val blurOn = hazeState != null &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        !settings.reduceTransparency
    val tint = (if (dark) GlassBlurTintDark else GlassBlurTintLight).copy(alpha = 0.35f)
    val radius = (when (settings.glassIntensity) {
        GlassIntensity.SUBTLE -> PhoneDimens.glassBlurRadiusSubtle
        GlassIntensity.STANDARD -> PhoneDimens.glassBlurRadiusStandard
        GlassIntensity.STRONG -> PhoneDimens.glassBlurRadiusStrong
    }) * settings.glassBlurAmount
    // Solid base first so list rows never show through, then the blur
    // on top of it (near-opaque when blur is off).
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.background.copy(alpha = if (blurOn) 0.82f else 0.96f)
            )
            .then(
                if (blurOn) {
                    Modifier.hazeEffect(
                        state = hazeState!!,
                        style = HazeStyle(
                            backgroundColor = Color.Transparent,
                            tint = HazeTint(tint),
                            blurRadius = radius
                        )
                    )
                } else {
                    Modifier
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            // One 60dp row shared with the floating pill: the title is
            // start-aligned and capped at 45% width so it can never sit
            // under the pill, whatever the header-size setting is (the
            // collapsed title is a fixed 20sp, not scaled).
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PhoneDimens.collapsedBarRowHeight)
            ) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 20.dp)
                        .fillMaxWidth(0.45f)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
    }
}
