package io.github.elli0t0.phone.feature.contacts.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

@Composable
fun ContactAvatar(
    photoUri: String?,
    displayName: String,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium
) {
    val initial = displayName.trim().take(1).uppercase().ifBlank { "?" }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (!photoUri.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(photoUri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
                error = {
                    Box(
                        modifier = Modifier
                            .size(size)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            style = textStyle,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                loading = {
                    Box(
                        modifier = Modifier
                            .size(size)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            style = textStyle,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            )
        } else {
            Text(
                text = initial,
                style = textStyle,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
