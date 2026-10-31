package io.github.elli0t0.phone.core.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.elli0t0.phone.R

/**
 * A call awaiting confirmation (0.18.6): the number to dial, the
 * contact lookup key for the SIM resolution (null when the caller
 * resolves it itself), and the label shown in the sheet — the
 * contact name when known, otherwise the number.
 */
data class PendingCall(
    val number: String,
    val lookupKey: String? = null,
    val label: String
)

/**
 * The "Call X?" sheet (0.18.6): shown before placing a call when
 * "Confirm before calling" is on. The number shows under the title
 * when the label is a contact name.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmCallSheet(
    pending: PendingCall,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    PhoneBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(id = R.string.confirm_call_title, pending.label),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (pending.label != pending.number) {
                Text(
                    text = pending.number,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(id = R.string.action_cancel))
                }
                TextButton(onClick = onConfirm) {
                    Text(text = stringResource(id = R.string.action_call))
                }
            }
        }
    }
}
