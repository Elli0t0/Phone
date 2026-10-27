package io.github.elli0t0.phone.feature.dialpad

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.KeypadCallButtonGreenDark
import io.github.elli0t0.phone.core.design.KeypadCallButtonGreenLight
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.OnAccentBlue
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.design.liquidGlass
import io.github.elli0t0.phone.core.design.bouncyClick

@Composable
fun DialpadKeypadGrid(
    onKeyPressed: (Char) -> Unit,
    onLongPressZero: () -> Unit
) {
    val spacing = LocalSpacing.current
    // Keypad Grid (Keys 1-9, *, 0, #)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = spacing.small),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DIAL_KEYS_GRID.forEach { rowKeys ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                rowKeys.forEach { dialKey ->
                    DialKeyView(
                        dialKey = dialKey,
                        onClick = {
                            onKeyPressed(dialKey.digit)
                        },
                        onLongClick = if (dialKey.digit == '0') {
                            {
                                onLongPressZero()
                            }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
fun DialpadCallButtonRow(
    glassDialpad: Boolean,
    darkTheme: Boolean,
    onCallClick: () -> Unit
) {
    // Action row: Left spacer, Call Button, Backspace Button
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = PhoneDimens.keypadCallButtonBottomPadding),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Placeholder empty box for balance (same size as backspace)
        Box(modifier = Modifier.size(56.dp))

        // Call button: glossy liquid-glass emerald, or the flat green
        // FAB when the dialpad's liquid glass is off. With no digits
        // it does nothing.
        val callButtonSource = remember { MutableInteractionSource() }
        if (glassDialpad) {
        Box(
            modifier = Modifier
                .size(PhoneDimens.callButtonSize)
                .clip(CircleShape)
                .bouncyClick(callButtonSource)
                .clickable(
                    interactionSource = callButtonSource,
                    indication = null
                ) {
                    onCallClick()
                }
                // Solid green under the glass: the glass fill alone
                // (22% alpha) washes out to near-white in light
                // theme, so the button was nearly invisible. The
                // haze tint is skipped too: a white blur tint over
                // the green washed it out in light theme.
                .background(
                    color = if (darkTheme) KeypadCallButtonGreenDark else KeypadCallButtonGreenLight,
                    shape = CircleShape
                )
                .liquidGlass(
                    shape = CircleShape,
                    tint = if (darkTheme) KeypadCallButtonGreenDark else KeypadCallButtonGreenLight,
                    elevation = 10.dp,
                    isSelected = true,
                    blur = false
                )
                .testTag("btn_call"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_phone),
                contentDescription = stringResource(id = R.string.cd_call_button),
                tint = OnAccentBlue,
                modifier = Modifier.size(32.dp)
            )
        }
        } else {
            FloatingActionButton(
                onClick = {
                    onCallClick()
                },
                modifier = Modifier
                    .size(PhoneDimens.callButtonSize)
                    .testTag("btn_call"),
                shape = CircleShape,
                containerColor = if (darkTheme) KeypadCallButtonGreenDark else KeypadCallButtonGreenLight,
                contentColor = OnAccentBlue,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_phone),
                    contentDescription = stringResource(id = R.string.cd_call_button),
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Right spacer for balance (backspace now sits by the number display).
        Box(modifier = Modifier.size(56.dp))
    }
}
