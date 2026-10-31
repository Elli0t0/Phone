package io.github.elli0t0.phone.core.design

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.data.settings.model.AnimationOption

/**
 * The check circle a selected row's avatar swaps to: primary fill,
 * white check, popping in on selection (spring when Animations is
 * on, short fade when reduced, instant when off).
 */
@Composable
fun SelectionCheckCircle(size: Dp, modifier: Modifier = Modifier) {
    val animationOption = LocalAppSettings.current.animationOption
    // animateFloatAsState has no initialValue in this Compose
    // version, so the pop-in runs through an Animatable instead.
    val scale = remember { Animatable(0.6f) }
    LaunchedEffect(animationOption) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = when (animationOption) {
                AnimationOption.ON -> spring(dampingRatio = 0.6f, stiffness = 400f)
                AnimationOption.REDUCED -> tween(durationMillis = Motion.Durations.Short)
                AnimationOption.OFF -> snap()
            }
        )
    }
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .background(MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_check),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size * 0.6f)
        )
    }
}
