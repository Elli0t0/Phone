package io.github.elli0t0.phone.feature.call.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.CallAnswerGreen
import io.github.elli0t0.phone.core.design.CallDeclineRed
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.design.pressScale
import io.github.elli0t0.phone.core.design.rememberLightHaptic
import io.github.elli0t0.phone.data.settings.model.AnimationOption
import io.github.elli0t0.phone.data.settings.model.AnswerSide
@Composable
internal fun ClassicAnswerControls(
    onAnswer: () -> Unit,
    onDecline: () -> Unit
) {
    val answerOnLeft = LocalAppSettings.current.answerSide == AnswerSide.LEFT
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        if (answerOnLeft) {
            ClassicAnswerButton(isAnswer = true, onClick = onAnswer, haloDelayMillis = 400)
            ClassicAnswerButton(isAnswer = false, onClick = onDecline, haloDelayMillis = 0)
        } else {
            ClassicAnswerButton(isAnswer = false, onClick = onDecline, haloDelayMillis = 0)
            ClassicAnswerButton(isAnswer = true, onClick = onAnswer, haloDelayMillis = 400)
        }
    }
}

@Composable
internal fun ClassicAnswerButton(
    isAnswer: Boolean,
    onClick: () -> Unit,
    haloDelayMillis: Int
) {
    val option = LocalAppSettings.current.animationOption
    val source = remember { MutableInteractionSource() }
    val lightHaptic = rememberLightHaptic()
    val color = if (isAnswer) CallAnswerGreen else CallDeclineRed
    val size = PhoneDimens.incomingActionButtonSize * LocalAppSettings.current.answerButtonSize.scale
    val haloTransition = rememberInfiniteTransition(label = "halo")
    val haloP by haloTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, delayMillis = haloDelayMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "haloP"
    )
    val wiggleTransition = rememberInfiniteTransition(label = "wiggle")
    val wiggle by wiggleTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3000
                0f at 0
                -14f at 167
                14f at 333
                0f at 500
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "wiggleR"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            // Halo behind the button only: never over the label or the
            // other button.
            if (option == AnimationOption.ON) {
                Box(
                    modifier = Modifier
                        .size(size)
                        .graphicsLayer {
                            scaleX = 1f + 0.25f * haloP
                            scaleY = 1f + 0.25f * haloP
                            alpha = 0.35f * (1f - haloP)
                        }
                        .background(color, CircleShape)
                )
            } else if (option == AnimationOption.REDUCED) {
                Box(
                    modifier = Modifier
                        .size(size)
                        .graphicsLayer {
                            scaleX = 1.12f
                            scaleY = 1.12f
                            alpha = 0.18f
                        }
                        .background(color, CircleShape)
                )
            }
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(color)
                    .pressScale(source)
                    .clickable(
                        interactionSource = source,
                        indication = ripple()
                    ) { lightHaptic(); onClick() }
                    .testTag(if (isAnswer) "answer_call_button" else "decline_call_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        id = if (isAnswer) R.drawable.ic_phone else R.drawable.ic_call_end
                    ),
                    contentDescription = stringResource(
                        id = if (isAnswer) R.string.call_action_answer else R.string.call_action_decline
                    ),
                    tint = Color.White,
                    modifier = Modifier
                        .size(PhoneDimens.incomingActionIconSize)
                        .graphicsLayer { rotationZ = if (isAnswer && option == AnimationOption.ON) wiggle else 0f }
                )
            }
        }
        Spacer(modifier = Modifier.height(PhoneDimens.answerLabelGap))
        Text(
            text = stringResource(
                id = if (isAnswer) R.string.call_action_answer else R.string.call_action_decline
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}
