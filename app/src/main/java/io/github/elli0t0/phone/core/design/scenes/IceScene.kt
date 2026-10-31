package io.github.elli0t0.phone.core.design.scenes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Ice: a plain pale icy-blue vertical gradient. */
@Composable
fun IceScene(intensity: Float, animate: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        drawRect(
            Brush.verticalGradient(listOf(Color(0xFFD6ECFF), Color(0xFF8EC9FF))),
            alpha = intensity
        )
    }
}
