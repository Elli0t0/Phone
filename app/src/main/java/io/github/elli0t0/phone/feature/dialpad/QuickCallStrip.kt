package io.github.elli0t0.phone.feature.dialpad

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.common.bidiWrap
import io.github.elli0t0.phone.core.design.GlassDropdownMenu
import io.github.elli0t0.phone.feature.contacts.ui.ContactAvatar

/**
 * Quick call strip (0.19.3): one-tap calling for favorites and
 * recents, placed just above the keys under the number display.
 * Cards are 68dp wide: a 52dp avatar circle plus the first name
 * below (one line, 12sp). The row scrolls horizontally with a fade
 * at the scrollable edges. Tap calls; long-press opens a small
 * glass menu (Message, Open contact, Remove from strip).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuickCallStrip(
    entries: List<QuickCallEntry>,
    onCall: (QuickCallEntry) -> Unit,
    onMessage: (QuickCallEntry) -> Unit,
    onOpenContact: (QuickCallEntry) -> Unit,
    onRemove: (QuickCallEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var menuEntry by remember { mutableStateOf<QuickCallEntry?>(null) }
    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            // Offscreen layer so the DstIn edge fade below applies
            // only to this row's content, not to what sits behind it.
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                // Fade only the edges that can still scroll: a row
                // that fits entirely stays fully opaque.
                val fadeStart = if (listState.canScrollBackward) 0.08f else 0f
                val fadeEnd = if (listState.canScrollForward) 0.92f else 1f
                if (fadeStart > 0f || fadeEnd < 1f) {
                    val stops = buildList {
                        if (fadeStart > 0f) {
                            add(0f to Color.Transparent)
                            add(fadeStart to Color.Black)
                        } else {
                            add(0f to Color.Black)
                        }
                        if (fadeEnd < 1f) {
                            add(fadeEnd to Color.Black)
                            add(1f to Color.Transparent)
                        } else {
                            add(1f to Color.Black)
                        }
                    }
                    drawRect(
                        brush = Brush.horizontalGradient(*stops.toTypedArray()),
                        blendMode = BlendMode.DstIn
                    )
                }
            },
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(entries, key = { it.contact.id }) { entry ->
            val displayName = entry.contact.displayName.ifBlank { entry.number }
            val firstName = displayName.trim().substringBefore(" ")
            Box {
                Column(
                    modifier = Modifier
                        .width(68.dp)
                        .combinedClickable(
                            onClick = { onCall(entry) },
                            onLongClick = { menuEntry = entry }
                        )
                        .testTag("quick_call_${entry.contact.id}"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ContactAvatar(
                        photoUri = entry.contact.photoUri,
                        displayName = displayName,
                        size = 52.dp
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = bidiWrap(firstName),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 12.sp,
                            textDirection = TextDirection.Content
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
                GlassDropdownMenu(
                    expanded = menuEntry == entry,
                    onDismissRequest = { menuEntry = null }
                ) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(id = R.string.action_message)) },
                        onClick = {
                            menuEntry = null
                            onMessage(entry)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(text = stringResource(id = R.string.quick_call_open_contact)) },
                        onClick = {
                            menuEntry = null
                            onOpenContact(entry)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(text = stringResource(id = R.string.quick_call_remove)) },
                        onClick = {
                            menuEntry = null
                            onRemove(entry)
                        }
                    )
                }
            }
        }
    }
}
