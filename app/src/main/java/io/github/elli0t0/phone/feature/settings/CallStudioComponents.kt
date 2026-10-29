package io.github.elli0t0.phone.feature.settings

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.Motion
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.design.SwitchGreenTrack
import io.github.elli0t0.phone.core.design.SwitchGreenUncheckedTrack
import io.github.elli0t0.phone.data.settings.model.SwitchStyle
import io.github.elli0t0.phone.data.settings.model.AnimationOption
/**
 * One settings group as a solid card: an optional title plus the
 * group's controls. Groups in the Call Studio are card items in
 * the page. Shared with CallStudioSections (same package).
 */
@Composable
fun StudioGroupCard(
    title: String?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(PhoneDimens.studioGroupCardCornerRadius),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(LocalSpacing.current.medium),
            verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.small)
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            content()
        }
    }
}

/**
 * Custom switch: wide 52x28dp track with a large white round thumb,
 * matching the main Settings switch (white thumb on accent track).
 * Shared with SettingsScreen's SettingsSwitch; moves to core/design
 * when SettingsScreen is split.
 */
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Thumb slide respects the Animations setting: ON keeps the
    // default spring, REDUCED shortens to a fade-speed tween,
    // OFF snaps instantly.
    val animationOption = LocalAppSettings.current.animationOption
    val thumbSpec: AnimationSpec<Dp> = when (animationOption) {
        AnimationOption.ON -> spring(stiffness = Spring.StiffnessMediumLow)
        AnimationOption.REDUCED -> tween(
            durationMillis = Motion.scaledDurationMs(200, animationOption),
            easing = Motion.Easings.Standard
        )
        AnimationOption.OFF -> snap()
    }
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) {
            PhoneDimens.settingsSwitchWidth - PhoneDimens.settingsSwitchThumbSize -
                PhoneDimens.settingsSwitchThumbPadding * 2
        } else {
            PhoneDimens.settingsSwitchThumbPadding
        },
        animationSpec = thumbSpec
    )
    Box(
        modifier = modifier
            .size(
                width = PhoneDimens.settingsSwitchWidth,
                height = PhoneDimens.settingsSwitchHeight
            )
            .clip(RoundedCornerShape(PhoneDimens.settingsSwitchCornerPercent))
            .background(
                if (LocalAppSettings.current.effectiveStyle.switch == SwitchStyle.GREEN) {
                    if (checked) SwitchGreenTrack else SwitchGreenUncheckedTrack
                } else if (checked) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(PhoneDimens.settingsSwitchThumbSize)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}
