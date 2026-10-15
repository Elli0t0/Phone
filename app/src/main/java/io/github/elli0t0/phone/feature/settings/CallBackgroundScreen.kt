package io.github.elli0t0.phone.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.elli0t0.phone.PhoneApplication
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.callBackgroundBrush
import io.github.elli0t0.phone.core.design.pressScale
import io.github.elli0t0.phone.core.design.rememberLightHaptic
import io.github.elli0t0.phone.data.call.CallState
import io.github.elli0t0.phone.data.call.OngoingCallModel
import io.github.elli0t0.phone.data.settings.model.CallBackground
import io.github.elli0t0.phone.feature.call.ui.IncomingCallScreen
import io.github.elli0t0.phone.feature.call.ui.OngoingCallScreen
import kotlin.math.roundToInt

/**
 * Call background studio as a full page: the top half is a live
 * full-screen preview of the real call screen (incoming or ongoing)
 * with a sample caller, and the bottom half holds every option:
 * background style, dim, and number visibility. Every change saves
 * immediately and shows in the preview. Back returns to Settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallBackgroundScreen(
    selected: CallBackground,
    accentColor: Long,
    onSelect: (CallBackground) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val lightHaptic = rememberLightHaptic()
    val settings = LocalAppSettings.current
    val context = LocalContext.current
    val app = context.applicationContext as PhoneApplication
    // Same instance SettingsScreen uses (same nav entry owner).
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.provideFactory(app.container.settingsRepository)
    )
    var previewOngoing by remember { mutableStateOf(false) }
    // Slider draft: the preview follows the drag live, and the value
    // is written to Settings once on release (not per frame).
    var dimDraft by remember(settings.callBackgroundDim) {
        mutableStateOf(settings.callBackgroundDim)
    }
    val mockCall = remember(previewOngoing) {
        OngoingCallModel(
            id = "preview",
            number = "+1 555 010 1234",
            displayName = "Alex Morgan",
            state = if (previewOngoing) CallState.ACTIVE else CallState.RINGING,
            durationSeconds = 125L
        )
    }
    val quickReplies = listOf(
        stringResource(id = R.string.quick_reply_cant_talk),
        stringResource(id = R.string.quick_reply_call_back),
        stringResource(id = R.string.quick_reply_in_meeting)
    )

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("call_background_screen"),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.settings_call_background)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("btn_call_background_back")) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = stringResource(id = R.string.cd_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Live preview: the real call screen with a sample caller.
            // Preview buttons are inert.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(
                        horizontal = spacing.screenHorizontalPadding,
                        vertical = spacing.small
                    )
                    .clip(RoundedCornerShape(24.dp))
            ) {
                if (previewOngoing) {
                    OngoingCallScreen(
                        callModel = mockCall,
                        onMuteToggle = {},
                        onAudioRouteSelect = {},
                        onHoldToggle = {},
                        onDtmfDigit = {},
                        onEndCall = {},
                        callBackground = selected,
                        accentColor = accentColor,
                        dim = dimDraft,
                        showNumber = settings.showNumberOnCallScreen
                    )
                } else {
                    IncomingCallScreen(
                        callModel = mockCall,
                        onAnswer = {},
                        onDecline = {},
                        onDeclineWithMessage = {},
                        quickDeclineMessages = quickReplies,
                        callBackground = selected,
                        accentColor = accentColor,
                        dim = dimDraft,
                        showNumber = settings.showNumberOnCallScreen
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = spacing.screenHorizontalPadding,
                        vertical = spacing.small
                    ),
                verticalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                // Preview switch: incoming / ongoing.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.small)
                ) {
                    FilterChip(
                        selected = !previewOngoing,
                        onClick = { previewOngoing = false },
                        label = { Text(text = stringResource(id = R.string.settings_call_preview_incoming)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("call_preview_incoming")
                    )
                    FilterChip(
                        selected = previewOngoing,
                        onClick = { previewOngoing = true },
                        label = { Text(text = stringResource(id = R.string.settings_call_preview_ongoing)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("call_preview_ongoing")
                    )
                }

                // Background style: swatch + label per option.
                Text(
                    text = stringResource(id = R.string.settings_call_background_style),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.medium)
                ) {
                    CallBackground.entries.forEach { option ->
                        val isSelected = option == selected
                        val optionSource = remember { MutableInteractionSource() }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .pressScale(optionSource)
                                .clickable(
                                    interactionSource = optionSource,
                                    indication = ripple()
                                ) { lightHaptic(); onSelect(option) }
                                .testTag("call_background_option_${option.name}")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(callBackgroundBrush(option, accentColor))
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(
                                                2.dp,
                                                MaterialTheme.colorScheme.primary,
                                                CircleShape
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                            )
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(top = spacing.extraSmall)
                            )
                        }
                    }
                }

                // Dim: black scrim over the background, 0-60%.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.settings_call_background_dim),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${(dimDraft * 100).roundToInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Slider(
                    value = dimDraft,
                    onValueChange = { dimDraft = it },
                    onValueChangeFinished = {
                        settingsViewModel.onCallBackgroundDimChanged(dimDraft)
                    },
                    valueRange = 0f..0.6f,
                    modifier = Modifier.fillMaxWidth().testTag("call_background_dim_slider")
                )
                Text(
                    text = stringResource(id = R.string.settings_call_background_dim_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Number under the caller name.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.settings_call_show_number),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(id = R.string.settings_call_show_number_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.showNumberOnCallScreen,
                        onCheckedChange = { settingsViewModel.onShowNumberOnCallScreenChanged(it) },
                        modifier = Modifier.testTag("call_show_number_switch")
                    )
                }
            }
        }
    }
}
