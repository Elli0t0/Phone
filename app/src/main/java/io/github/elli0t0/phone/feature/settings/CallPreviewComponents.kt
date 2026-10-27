package io.github.elli0t0.phone.feature.settings

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.Motion
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.design.liquidGlass
import io.github.elli0t0.phone.data.call.OngoingCallModel
import io.github.elli0t0.phone.data.settings.model.AnimationOption
import io.github.elli0t0.phone.data.settings.model.AnswerStyle
import io.github.elli0t0.phone.data.settings.model.CallBackground
import io.github.elli0t0.phone.feature.call.ui.IncomingCallScreen
import io.github.elli0t0.phone.feature.call.ui.OngoingCallScreen
/**
 * Glass segmented control: a rounded glass track with one sliding
 * selection pill (filled accent, onPrimary label). The pill's slide
 * honours the Animations setting. No square edges anywhere.
 */
@Composable
internal fun GlassSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    optionTestTags: List<String>,
    modifier: Modifier = Modifier
) {
    val animationOption = LocalAppSettings.current.animationOption
    val trackShape = RoundedCornerShape(50)
    BoxWithConstraints(
        modifier = modifier
            .height(PhoneDimens.studioSegmentedControlHeight)
            .liquidGlass(shape = trackShape)
    ) {
        val segmentWidth = maxWidth / options.size
        val pillSpec: AnimationSpec<Dp> = when (animationOption) {
            AnimationOption.ON -> spring(dampingRatio = 0.8f, stiffness = 380f)
            AnimationOption.REDUCED -> tween(durationMillis = Motion.Durations.Short)
            AnimationOption.OFF -> snap()
        }
        val pillOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = pillSpec,
            label = "segmentedPill"
        )
        // Sliding selection pill under the labels.
        Box(
            modifier = Modifier
                .offset(x = pillOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .padding(PhoneDimens.studioSegmentedPillPadding)
                .clip(trackShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Row(modifier = Modifier.fillMaxSize()) {
            options.forEachIndexed { index, label ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onSelect(index) }
                        .testTag(optionTestTags.getOrElse(index) { "segmented_$index" }),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (index == selectedIndex) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

/**
 * The studio preview: the call screen is laid out at a reference
 * phone size, then scaled proportionally to fit the preview box,
 * so the caller header and call buttons are always visible.
 */
@Composable
internal fun ScaledCallPreview(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val scale = minOf(
            maxWidth / PhoneDimens.studioPreviewReferenceWidth,
            maxHeight / PhoneDimens.studioPreviewReferenceHeight
        )
        Box(
            modifier = Modifier
                .size(
                    PhoneDimens.studioPreviewReferenceWidth,
                    PhoneDimens.studioPreviewReferenceHeight
                )
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        ) {
            content()
        }
    }
}

/**
 * The live call preview: the real incoming or ongoing call screen
 * with a sample caller. Preview buttons are inert.
 */
@Composable
internal fun CallPreviewContent(
    previewOngoing: Boolean,
    mockCall: OngoingCallModel,
    selected: CallBackground,
    backgroundColor: Long,
    dimDraft: Float,
    showNumber: Boolean,
    answerStyle: AnswerStyle,
    quickReplies: List<String>
) {
    if (previewOngoing) {
        OngoingCallScreen(
            callModel = mockCall,
            onMuteToggle = {},
            onAudioRouteSelect = {},
            onHoldToggle = {},
            onDtmfDigit = {},
            onEndCall = {},
            callBackground = selected,
            accentColor = backgroundColor,
            dim = dimDraft,
            showNumber = showNumber
        )
    } else {
        IncomingCallScreen(
            callModel = mockCall,
            onAnswer = {},
            onDecline = {},
            onDeclineWithMessage = {},
            quickDeclineMessages = quickReplies,
            callBackground = selected,
            accentColor = backgroundColor,
            dim = dimDraft,
            showNumber = showNumber,
            answerStyle = answerStyle
        )
    }
}
