package io.github.elli0t0.phone.navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateTopPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.elli0t0.phone.R

/**
 * The shared inner-screen top row: status-bar inset + 8dp, 48dp
 * high, back arrow and an optional title. A plain row above the
 * content — never an overlay, never a 64dp app bar — so detail,
 * history, settings sub-pages, setup and duplicates all line up
 * the same way. Pass null [onBack] for screens with nowhere to go
 * back to (setup); pass null [title] when the content below already
 * shows the name (detail at rest, where the title appears only once
 * the header card has scrolled away).
 */
@Composable
fun InnerTopBar(
    onBack: (() -> Unit)?,
    title: String?,
    modifier: Modifier = Modifier,
    backTestTag: String? = null
) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = statusBarTop + 8.dp)
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = if (backTestTag != null) Modifier.testTag(backTestTag) else Modifier
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = stringResource(id = R.string.cd_back)
                )
            }
        }
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
