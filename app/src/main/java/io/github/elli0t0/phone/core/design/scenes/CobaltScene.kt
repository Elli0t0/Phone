package io.github.elli0t0.phone.core.design.scenes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Cobalt: a plain deep-blue vertical gradient. */
@Composable
fun CobaltScene(intensity: Float, animate: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        drawRect(
            Brush.verticalGradient(listOf(Color(0xFF0A2A7A), Color(0xFF2E7CD6))),
            alpha = intensity
        )
    }
}
