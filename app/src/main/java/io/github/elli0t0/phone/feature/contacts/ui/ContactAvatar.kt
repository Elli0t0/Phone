package io.github.elli0t0.phone.feature.contacts.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.elli0t0.phone.R
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import io.github.elli0t0.phone.core.design.ContactAvatarGradients

/**
 * Deterministic avatar gradient for a name: the same pair
 * [ContactAvatar] draws. Call screens reuse it for the
 * avatar-colored background.
 */
fun avatarGradient(displayName: String): Pair<Color, Color> {
    val index = ((displayName.hashCode() % ContactAvatarGradients.size) +
        ContactAvatarGradients.size) % ContactAvatarGradients.size
    return ContactAvatarGradients[index]
}

@Composable
fun ContactAvatar(
    photoUri: String?,
    displayName: String,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium,
    // Unknown caller (no name, only a number): a person icon on the
    // gradient instead of the number's first character ("+").
    showPersonIcon: Boolean = false
) {
    val initial = displayName.trim().take(1).uppercase().ifBlank { "?" }
    // Soft two-tone pastel gradient, deterministic per name. The brush is
    // remembered: rebuilding it every composition re-created the shader.
    val gradient = avatarGradient(displayName)
    val fallbackBackground = remember(gradient) {
        Brush.linearGradient(colors = listOf(gradient.first, gradient.second))
    }
    // Sized request: decode at the displayed pixel size, not full resolution.
    // The request itself is remembered so it is not rebuilt per composition.
    val context = LocalContext.current
    val density = LocalDensity.current
    val imageRequest = remember(photoUri, size, context, density) {
        ImageRequest.Builder(context)
            .data(photoUri)
            .size(with(density) { size.roundToPx() })
            .crossfade(true)
            .build()
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(fallbackBackground),
        contentAlignment = Alignment.Center
    ) {
        if (!photoUri.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = imageRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
                error = {
                    Box(
                        modifier = Modifier
                            .size(size)
                            .background(fallbackBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        if (showPersonIcon) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_person),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(size * 0.6f)
                        )
                    } else {
                        Text(
                            text = initial,
                            style = textStyle,
                            color = Color.White
                        )
                    }
                    }
                },
                loading = {
                    Box(
                        modifier = Modifier
                            .size(size)
                            .background(fallbackBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        if (showPersonIcon) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_person),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(size * 0.6f)
                        )
                    } else {
                        Text(
                            text = initial,
                            style = textStyle,
                            color = Color.White
                        )
                    }
                    }
                }
            )
        } else {
            if (showPersonIcon) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_person),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.6f)
                )
            } else {
                Text(
                    text = initial,
                    style = textStyle,
                    color = Color.White
                )
            }
        }
    }
}
