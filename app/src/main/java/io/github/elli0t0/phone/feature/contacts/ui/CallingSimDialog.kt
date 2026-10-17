package io.github.elli0t0.phone.feature.contacts.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LiquidGlassPopupSurface
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.design.dialogEnter
import io.github.elli0t0.phone.core.telecom.SimAccountInfo

@Composable
fun CallingSimDialog(
    simAccounts: List<SimAccountInfo>,
    currentSimId: String?,
    onSimSelected: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val spacing = LocalSpacing.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        LiquidGlassPopupSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.large)
                .dialogEnter()
                .testTag("calling_sim_dialog")
        ) {
            Column(modifier = Modifier.padding(spacing.large)) {
                Text(
                    text = stringResource(id = R.string.contact_calling_sim_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = spacing.medium)
                )
                CallingSimRow(
                    label = stringResource(id = R.string.contact_calling_sim_ask),
                    isSelected = currentSimId == null,
                    iconRes = R.drawable.ic_sim_card,
                    onClick = { onSimSelected(null) },
                    testTag = "calling_sim_ask"
                )
                Spacer(modifier = Modifier.height(spacing.small))
                simAccounts.forEach { sim ->
                    CallingSimRow(
                        label = sim.label,
                        isSelected = currentSimId == sim.handle.id,
                        iconRes = R.drawable.ic_sim_card,
                        onClick = { onSimSelected(sim.handle.id) },
                        testTag = "calling_sim_${sim.label}"
                    )
                    Spacer(modifier = Modifier.height(spacing.small))
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

@Composable
private fun CallingSimRow(
    label: String,
    isSelected: Boolean,
    iconRes: Int,
    onClick: () -> Unit,
    testTag: String
) {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.small, vertical = spacing.small)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(spacing.medium))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(
                painter = painterResource(id = R.drawable.ic_check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
