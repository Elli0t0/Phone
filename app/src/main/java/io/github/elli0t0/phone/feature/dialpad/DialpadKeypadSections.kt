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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.KeypadCallButtonGreenDark
import io.github.elli0t0.phone.core.design.KeypadCallButtonGreenLight
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.OnAccentBlue
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.design.liquidGlass
import io.github.elli0t0.phone.core.design.bouncyClick
import io.github.elli0t0.phone.data.settings.model.KeyStyle
import io.github.elli0t0.phone.data.settings.model.KeypadStyle

@Composable
fun DialpadKeypadGrid(
    onKeyPressed: (Char) -> Unit,
    onLongPressDigit: (Char) -> Unit,
    keySize: Dp = 72.dp,
    verticalSpacing: Dp = 12.dp,
    keypadStyle: KeypadStyle = KeypadStyle.GLASS
) {
    val spacing = LocalSpacing.current
    // Keypad Grid (Keys 1-9, *, 0, #): centered rows with 20dp
    // gaps, so the middle column sits dead center under the call
    // button below. Key look comes from the keypad style.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = spacing.small),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DIAL_KEYS_GRID.forEach { rowKeys ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                rowKeys.forEach { dialKey ->
                    KeypadKey(
                        dialKey = dialKey,
                        style = keypadStyle,
                        size = keySize,
                        onClick = {
                            onKeyPressed(dialKey.digit)
                        },
                        onLongClick = {
                            onLongPressDigit(dialKey.digit)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DialpadCallButtonRow(
    keyStyle: KeyStyle,
    darkTheme: Boolean,
    onCallClick: () -> Unit
) {
    // One centered green call button, 72dp like the keys. Left of
    // it an empty 72dp box, right of it nothing: the 72dp end
    // padding balances the left box, so the button stays centered
    // (aligned under the middle key) with no second button.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                bottom = PhoneDimens.keypadCallButtonBottomPadding,
                end = 72.dp
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Placeholder empty box for balance.
        Box(modifier = Modifier.size(72.dp))

        // Call button: glossy liquid-glass emerald, a plain green
        // circle for the flat keys, or the flat green FAB when the
        // dialpad's liquid glass is off. With no digits it does nothing.
        val callButtonSource = remember { MutableInteractionSource() }
        if (keyStyle == KeyStyle.FLAT) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = callButtonSource,
                    indication = null
                ) {
                    onCallClick()
                }
                .background(
                    color = Color(0xFF34C759),
                    shape = CircleShape
                )
                .testTag("btn_call"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_phone),
                contentDescription = stringResource(id = R.string.cd_call_button),
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }
        } else if (keyStyle == KeyStyle.GLASS) {
        Box(
            modifier = Modifier
                .size(72.dp)
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
                modifier = Modifier.size(30.dp)
            )
        }
        } else {
            FloatingActionButton(
                onClick = {
                    onCallClick()
                },
                modifier = Modifier
                    .size(72.dp)
                    .testTag("btn_call"),
                shape = CircleShape,
                containerColor = if (darkTheme) KeypadCallButtonGreenDark else KeypadCallButtonGreenLight,
                contentColor = OnAccentBlue,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_phone),
                    contentDescription = stringResource(id = R.string.cd_call_button),
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}
