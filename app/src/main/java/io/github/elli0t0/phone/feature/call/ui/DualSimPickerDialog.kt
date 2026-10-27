package io.github.elli0t0.phone.feature.call.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.common.bidiWrap
import io.github.elli0t0.phone.core.design.LiquidGlassPopupSurface
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.design.dialogEnter
import io.github.elli0t0.phone.core.design.pressScale
import io.github.elli0t0.phone.core.design.rememberLightHaptic
import io.github.elli0t0.phone.core.telecom.SimAccountInfo

@Composable
fun DualSimPickerDialog(
    simAccounts: List<SimAccountInfo>,
    targetNumber: String,
    onSimSelected: (SimAccountInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val spacing = LocalSpacing.current
    val lightHaptic = rememberLightHaptic()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        LiquidGlassPopupSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.large)
                .dialogEnter()
                .testTag("sim_picker_dialog")
        ) {
            Column(modifier = Modifier.padding(spacing.large)) {
                Text(
                    text = stringResource(id = R.string.call_sim_picker_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = spacing.medium)
                )
                Text(
                    text = bidiWrap(targetNumber),
                    style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Content),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = spacing.medium)
                )

                simAccounts.forEachIndexed { index, sim ->
                    val rowSource = remember { MutableInteractionSource() }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = PhoneDimens.simPickerRowMinHeight)
                            .clip(RoundedCornerShape(PhoneDimens.simPickerRowCornerRadius))
                            .pressScale(rowSource)
                            .clickable(
                                interactionSource = rowSource,
                                indication = ripple()
                            ) { lightHaptic(); onSimSelected(sim) }
                            .padding(horizontal = spacing.small, vertical = spacing.small)
                            .testTag("sim_picker_option_${sim.label}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_sim_card),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(PhoneDimens.simPickerIconSize)
                        )
                        Spacer(modifier = Modifier.width(spacing.medium))
                        Text(
                            text = sim.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (index < simAccounts.lastIndex) {
                        Spacer(modifier = Modifier.height(spacing.small))
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(id = R.string.action_cancel))
                    }
                }
            }
        }
    }
}
