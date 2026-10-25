package io.github.elli0t0.phone.feature.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.CallAnswerGreen
import io.github.elli0t0.phone.core.design.CallBackgroundLayer
import io.github.elli0t0.phone.core.design.CallDeclineRed
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.Motion
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.data.settings.model.AnswerStyle
import io.github.elli0t0.phone.feature.call.ui.AnswerControls
import io.github.elli0t0.phone.navigation.InnerTopBar
import io.github.elli0t0.phone.feature.contacts.ui.ContactAvatar
import kotlinx.coroutines.delay

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
        modifier = modifier.fillMaxSize().testTag("answer_style_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            InnerTopBar(
                onBack = onNavigateBack,
                title = stringResource(id = R.string.settings_answer_style),
                backTestTag = "btn_answer_style_back"
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = spacing.screenHorizontalPadding,
                    top = spacing.small,
                    end = spacing.screenHorizontalPadding,
                    bottom = spacing.small + innerPadding.calculateBottomPadding()
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
        Box {
            ScaledPreview(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CallBackgroundLayer(
                        background = settings.callBackground,
                        accentColor = settings.accentColor,
                        modifier = Modifier.fillMaxSize()
                    )
                    // The real controls at the bottom, as on a call.
                    AnswerControls(
                        style = style,
                        onAnswer = {},
                        onDecline = {},
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }
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

/**
 * Fixed-design preview frame: the content is always laid out at
 * 360 x 220 dp and scaled to the tile width, so previews are never
 * empty or cropped by the tile's actual size.
 */
@Composable
private fun ScaledPreview(
    modifier: Modifier = Modifier,
    designWidth: Dp = 360.dp,
    designHeight: Dp = 220.dp,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(designWidth / designHeight)
            .clip(RoundedCornerShape(20.dp))
    ) {
        val s = maxWidth / designWidth
        Box(
            modifier = Modifier
                .wrapContentSize(Alignment.TopStart, unbounded = true)
                .requiredSize(designWidth, designHeight)
                .graphicsLayer {
                    scaleX = s
                    scaleY = s
                    transformOrigin = TransformOrigin(0f, 0f)
                }
        ) {
            content()
        }
    }
}

private enum class TryItResult { ANSWERED, DECLINED }

@Composable
private fun AnswerStyleTryIt(style: AnswerStyle) {
    val spacing = LocalSpacing.current
    val settings = LocalAppSettings.current
    val sampleName = stringResource(id = R.string.answer_preview_sample_name)
    val sampleNumber = stringResource(id = R.string.answer_preview_sample_number)
    // The controls reset to idle 1200 ms after an answer/decline, so
    // the preview is never left stuck: keying on the attempt counter
    // rebuilds them in their initial state.
    var attempt by remember(style) { mutableIntStateOf(0) }
    var result by remember(style) { mutableStateOf<TryItResult?>(null) }
    LaunchedEffect(result) {
        if (result != null) {
            delay(1200)
            result = null
            attempt++
        }
    }
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
                // Sample caller, as on a real incoming call.
                ContactAvatar(
                    photoUri = null,
                    displayName = sampleName,
                    size = PhoneDimens.detailHeaderAvatarSize,
                    textStyle = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(spacing.small))
                Text(
                    text = sampleName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = sampleNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
                // Fixed-height feedback slot so the controls never jump.
                Box(
                    modifier = Modifier.height(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    result?.let { feedback ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (feedback == TryItResult.ANSWERED) CallAnswerGreen else CallDeclineRed
                                )
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = stringResource(
                                    id = if (feedback == TryItResult.ANSWERED) {
                                        R.string.answer_preview_answered
                                    } else {
                                        R.string.answer_preview_declined
                                    }
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                key(attempt) {
                    AnswerControls(
                        style = style,
                        onAnswer = { result = TryItResult.ANSWERED },
                        onDecline = { result = TryItResult.DECLINED },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
