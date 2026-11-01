package io.github.elli0t0.phone.feature.dialpad

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.common.bidiWrap
import io.github.elli0t0.phone.core.design.GlassDropdownMenu
import io.github.elli0t0.phone.feature.contacts.ui.ContactAvatar

/**
 * Quick call strip (0.19.6 pills): one-tap calling for favorites
 * and recents, as a row of pills directly above the keys. Each pill
 * is 44dp tall with a 32dp avatar and the first name (max 96dp).
 * The row scrolls horizontally with a 12dp fade at the scrollable
 * edges. Tap calls; long-press opens a small glass menu (Message,
 * Open contact, Remove from strip). No label, no big circles, no
 * name under the avatar.
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
                // that fits entirely stays fully opaque. The fade is
                // a fixed 12dp, so a cut-off pill looks intentional.
                val fade = if (size.width > 0f) 12.dp.toPx() / size.width else 0f
                val fadeStart = if (listState.canScrollBackward) fade else 0f
                val fadeEnd = if (listState.canScrollForward) 1f - fade else 1f
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
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(entries, key = { it.contact.id }) { entry ->
            Box {
                QuickCallPill(
                    entry = entry,
                    onClick = { onCall(entry) },
                    onLongClick = { menuEntry = entry }
                )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickCallPill(
    entry: QuickCallEntry,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(22.dp)
    val displayName = entry.contact.displayName.ifBlank { entry.number }
    val firstName = displayName.trim().substringBefore(" ")
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 6.dp, end = 14.dp)
            .testTag("quick_call_${entry.contact.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ContactAvatar(
            photoUri = entry.contact.photoUri,
            displayName = displayName,
            size = 32.dp
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = bidiWrap(firstName),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 96.dp)
        )
    }
}
