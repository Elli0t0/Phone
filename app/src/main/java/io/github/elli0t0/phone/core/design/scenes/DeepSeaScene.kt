package io.github.elli0t0.phone.core.design.scenes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Deep Sea: a plain very-dark-blue vertical gradient. */
@Composable
fun DeepSeaScene(intensity: Float, animate: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        drawRect(
            Brush.verticalGradient(listOf(Color(0xFF04122B), Color(0xFF0B3B66))),
            alpha = intensity
        )
    }
}
