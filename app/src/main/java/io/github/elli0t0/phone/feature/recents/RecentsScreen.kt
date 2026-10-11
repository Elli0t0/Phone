package io.github.elli0t0.phone.feature.recents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.elli0t0.phone.core.design.LocalSpacing

@Composable
fun RecentsScreen(
    modifier: Modifier = Modifier,
    viewModel: RecentsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = LocalSpacing.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = spacing.screenHorizontalPadding)
            .testTag("recents_screen"),
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(spacing.screenTopPadding))

        // One UI large screen title header
        Text(
            text = stringResource(id = uiState.titleRes),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = spacing.large)
        )

        // Rounded container card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("recents_content_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = MaterialTheme.shapes.large
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.cardContentPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = uiState.placeholderRes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
