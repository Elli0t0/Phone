package io.github.elli0t0.phone.feature.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.CallBackgroundLayer
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.Motion
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.data.settings.model.AnswerStyle
import io.github.elli0t0.phone.feature.call.ui.AnswerControls

/**
 * Answer style page: four live mini-previews of the real answer
 * controls (scaled down, inert; tapping a tile selects the style),
 * the slider-only-when-locked option, and a full-size Try-it area
 * that answers/declines a fake call.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnswerStyleScreen(
    selected: AnswerStyle,
    sliderOnlyWhenLocked: Boolean,
    onSelect: (AnswerStyle) -> Unit,
    onSliderOnlyWhenLockedChange: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    Scaffold(
        modifier = modifier.fillMaxSize().testTag("answer_style_screen"),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.settings_answer_style)) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_answer_style_back")
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = stringResource(id = R.string.cd_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(
                horizontal = spacing.screenHorizontalPadding,
                vertical = spacing.small
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.medium)) {
                    Box(modifier = Modifier.weight(1f)) {
                        AnswerStyleTile(
                            style = AnswerStyle.CLASSIC,
                            selected = selected == AnswerStyle.CLASSIC,
                            onClick = { onSelect(AnswerStyle.CLASSIC) }
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        AnswerStyleTile(
                            style = AnswerStyle.SLIDER,
                            selected = selected == AnswerStyle.SLIDER,
                            onClick = { onSelect(AnswerStyle.SLIDER) }
                        )
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.medium)) {
                    Box(modifier = Modifier.weight(1f)) {
                        AnswerStyleTile(
                            style = AnswerStyle.SWIPE_BUTTONS,
                            selected = selected == AnswerStyle.SWIPE_BUTTONS,
                            onClick = { onSelect(AnswerStyle.SWIPE_BUTTONS) }
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        AnswerStyleTile(
                            style = AnswerStyle.LIQUID,
                            selected = selected == AnswerStyle.LIQUID,
                            onClick = { onSelect(AnswerStyle.LIQUID) }
                        )
                    }
                }
            }
            if (selected == AnswerStyle.SLIDER) {
                item {
                    SettingsSwitchRow(
                        title = stringResource(id = R.string.settings_slider_only_locked),
                        subtitle = stringResource(id = R.string.settings_slider_only_locked_desc),
                        checked = sliderOnlyWhenLocked,
                        onCheckedChange = onSliderOnlyWhenLockedChange
                    )
                }
            }
            item {
                AnswerStyleTryIt(style = selected)
            }
        }
    }
}

@Composable
private fun AnswerStyleTile(
    style: AnswerStyle,
    selected: Boolean,
    onClick: () -> Unit
) {
    val spacing = LocalSpacing.current
    val settings = LocalAppSettings.current
    val borderColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant
        },
        animationSpec = tween(durationMillis = Motion.Durations.Medium),
        label = "tileBorderColor"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 2.dp else 1.dp,
        animationSpec = tween(durationMillis = Motion.Durations.Medium),
        label = "tileBorderWidth"
    )
    val tileShape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .clip(tileShape)
            .border(borderWidth, borderColor, tileShape)
            .testTag("answer_style_tile_${style.name}")
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(PhoneDimens.answerStylePreviewHeight)
        ) {
            CallBackgroundLayer(
                background = settings.callBackground,
                accentColor = settings.accentColor,
                modifier = Modifier.fillMaxSize()
            )
            // The real controls at half scale, anchored top-left.
            Box(
                modifier = Modifier
                    .requiredSize(
                        width = maxWidth * 2,
                        height = PhoneDimens.answerStylePreviewHeight * 2
                    )
                    .graphicsLayer {
                        scaleX = 0.5f
                        scaleY = 0.5f
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
            ) {
                AnswerControls(
                    style = style,
                    onAnswer = {},
                    onDecline = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
            // Inert overlay: the tile click selects the style.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { onClick() }
            )
        }
        Text(
            text = style.label,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = spacing.small)
        )
    }
}

@Composable
private fun AnswerStyleTryIt(style: AnswerStyle) {
    val spacing = LocalSpacing.current
    val settings = LocalAppSettings.current
    val answeredText = stringResource(id = R.string.answer_preview_answered)
    val declinedText = stringResource(id = R.string.answer_preview_declined)
    var result by remember(style) { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        Text(
            text = stringResource(id = R.string.settings_answer_style_try_it),
            style = MaterialTheme.typography.titleMedium
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(PhoneDimens.answerStyleTryItHeight)
                .clip(RoundedCornerShape(16.dp))
                .testTag("answer_style_try_it")
        ) {
            CallBackgroundLayer(
                background = settings.callBackground,
                accentColor = settings.accentColor,
                modifier = Modifier.fillMaxSize()
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(spacing.medium),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Fixed-height result slot so the controls never jump.
                Box(
                    modifier = Modifier.height(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    result?.let { text ->
                        Text(
                            text = text,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                AnswerControls(
                    style = style,
                    onAnswer = { result = answeredText },
                    onDecline = { result = declinedText },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
