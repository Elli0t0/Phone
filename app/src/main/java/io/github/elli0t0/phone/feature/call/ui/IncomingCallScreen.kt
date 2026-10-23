package io.github.elli0t0.phone.feature.call.ui

import android.app.KeyguardManager
import android.content.Context
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.common.PhoneNumberFormatter
import io.github.elli0t0.phone.data.settings.model.AccentPreset
import io.github.elli0t0.phone.data.settings.model.AnswerButtonSize
import io.github.elli0t0.phone.data.settings.model.AnswerGesture
import io.github.elli0t0.phone.data.settings.model.AnswerStyle
import io.github.elli0t0.phone.data.settings.model.AnswerSide
import io.github.elli0t0.phone.data.settings.model.CallerInfoPosition
import io.github.elli0t0.phone.data.settings.model.IncomingAvatarShape
import io.github.elli0t0.phone.data.settings.model.IncomingAvatarSize
import io.github.elli0t0.phone.core.design.CallAnswerGreen
import io.github.elli0t0.phone.core.design.CallBackgroundLayer
import io.github.elli0t0.phone.core.design.CallDeclineRed
import io.github.elli0t0.phone.core.design.InCallAvatarRing
import io.github.elli0t0.phone.core.design.InCallControlBackground
import io.github.elli0t0.phone.core.design.InCallControlBackgroundFlat
import io.github.elli0t0.phone.core.design.InCallSwipeTrackBackground
import io.github.elli0t0.phone.core.design.LiquidGlassChip
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.design.PulseRings
import io.github.elli0t0.phone.core.design.pressScale
import io.github.elli0t0.phone.core.design.rememberLightHaptic
import io.github.elli0t0.phone.data.call.OngoingCallModel
import io.github.elli0t0.phone.data.settings.model.AnimationOption
import io.github.elli0t0.phone.data.settings.model.CallBackground
import kotlin.math.roundToInt
import io.github.elli0t0.phone.core.common.bidiWrap
import androidx.compose.ui.text.style.TextDirection

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IncomingCallScreen(
    callModel: OngoingCallModel,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    onDeclineWithMessage: (String) -> Unit = {},
    quickDeclineMessages: List<String>,
    callBackground: CallBackground = CallBackground.DEFAULT,
    accentColor: Long = AccentPreset.ONE_UI_BLUE.colorValue,
    // Black scrim over the background (0 = none). Number under the name.
    dim: Float = 0f,
    showNumber: Boolean = true,
    answerStyle: AnswerStyle = AnswerStyle.CLASSIC,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val lightHaptic = rememberLightHaptic()
    val context = LocalContext.current
    // The slider only takes over when the phone is locked (its
    // setting); unlocked, the classic buttons show instead.
    val keyguardLocked = remember {
        (context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)
            ?.isKeyguardLocked ?: false
    }
    val effectiveStyle = if (
        answerStyle == AnswerStyle.SLIDER &&
        LocalAppSettings.current.sliderOnlyWhenLocked &&
        !keyguardLocked
    ) {
        AnswerStyle.CLASSIC
    } else {
        answerStyle
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("incoming_call_screen")
    ) {
        CallBackgroundLayer(
            background = callBackground,
            accentColor = accentColor,
            modifier = Modifier.fillMaxSize()
        )
        if (dim > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = dim.coerceIn(0f, 1f)))
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = spacing.screenHorizontalPadding, vertical = spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Caller info position: top (default), or centered in the
            // space above the quick responses via spacers around it.
            val callerInfoCentered =
                LocalAppSettings.current.callerInfoPosition == CallerInfoPosition.CENTER
            if (callerInfoCentered) {
                Spacer(modifier = Modifier.weight(1f))
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = PhoneDimens.incomingHeaderTopPadding)
            ) {
            Text(
                text = stringResource(id = R.string.call_incoming),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = spacing.medium)
            )

            val hasName = !callModel.displayName.isNullOrBlank() && callModel.displayName != callModel.number
            val formattedNumber = PhoneNumberFormatter.format(callModel.number)
            val titleText = if (hasName) callModel.displayName!! else formattedNumber
            val initial = titleText.trim().take(1).uppercase().ifBlank { "?" }

            // Avatar size and shape from the settings.
            val glassCallScreens = LocalAppSettings.current.glassCallScreens
            val avatarScale = LocalAppSettings.current.incomingAvatarSize.scale
            val avatarShape = if (LocalAppSettings.current.incomingAvatarShape == IncomingAvatarShape.ROUNDED) RoundedCornerShape(24) else CircleShape

            // Pulsing glass rings around the caller avatar.
            PulseRings(
                color = InCallAvatarRing,
                modifier = Modifier.size(PhoneDimens.incomingAvatarSize * avatarScale)
            ) {
                Box(
                    modifier = Modifier
                        .size(PhoneDimens.incomingAvatarSize * avatarScale)
                        .clip(avatarShape)
                        .background(if (glassCallScreens) InCallControlBackground else InCallControlBackgroundFlat),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = MaterialTheme.typography.displayLarge.fontSize * avatarScale
                        ),
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.medium))

            Text(
                text = bidiWrap(titleText),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = PhoneDimens.incomingTitleSize,
                    textDirection = TextDirection.Content
                ),
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = spacing.extraSmall)
            )

            if (hasName && showNumber && callModel.number.isNotBlank()) {
                Text(
                    text = bidiWrap(formattedNumber),
                    style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content),
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }

        if (callerInfoCentered) {
            Spacer(modifier = Modifier.weight(1f))
        }

        // Quick replies position: above (default) or below the answer buttons.
        val quickRepliesBelow = LocalAppSettings.current.quickRepliesBelowButtons
        if (!quickRepliesBelow) QuickRepliesSection(quickDeclineMessages = quickDeclineMessages, onDeclineWithMessage = onDeclineWithMessage)
        AnswerControls(
            style = effectiveStyle,
            onAnswer = onAnswer,
            onDecline = onDecline,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = PhoneDimens.incomingActionsBottomPadding)
        )
        if (quickRepliesBelow) QuickRepliesSection(quickDeclineMessages = quickDeclineMessages, onDeclineWithMessage = onDeclineWithMessage)
        }
    }
}

/**
 * Quick replies: decline the call and send the message as a reply SMS.
 * Hidden when the quick-replies setting is off.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickRepliesSection(
    quickDeclineMessages: List<String>,
    onDeclineWithMessage: (String) -> Unit
) {
    if (!LocalAppSettings.current.showQuickReplies) return
    val glassCallScreens = LocalAppSettings.current.glassCallScreens
    val spacing = LocalSpacing.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = spacing.medium)
    ) {
        Text(
            text = stringResource(id = R.string.quick_reply_title),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.padding(bottom = spacing.small)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(PhoneDimens.incomingChipSpacing, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(PhoneDimens.incomingChipSpacing),
            modifier = Modifier.fillMaxWidth()
        ) {
            quickDeclineMessages.forEachIndexed { index, message ->
                if (glassCallScreens) {
                    LiquidGlassChip(
                        selected = false,
                        onClick = { onDeclineWithMessage(message) },
                        modifier = Modifier.testTag("quick_reply_$index"),
                        forceDarkGlass = true
                    ) {
                        Text(
                            text = message,
                            color = Color.White,
                            modifier = Modifier.padding(
                                horizontal = PhoneDimens.incomingChipHorizontalPadding,
                                vertical = PhoneDimens.incomingChipVerticalPadding
                            )
                        )
                    }
                } else {
                    val chipSource = remember { MutableInteractionSource() }
                    AssistChip(
                        onClick = { onDeclineWithMessage(message) },
                        label = { Text(text = message) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = InCallControlBackgroundFlat,
                            labelColor = Color.White
                        ),
                        interactionSource = chipSource,
                        modifier = Modifier
                            .pressScale(chipSource)
                            .testTag("quick_reply_$index")
                    )
                }
            }
        }
    }
}
