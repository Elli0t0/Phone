package io.github.elli0t0.phone.feature.settings
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.font.FontWeight
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.pressScale
import io.github.elli0t0.phone.core.design.rememberLightHaptic
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.telecom.UssdHelper
import io.github.elli0t0.phone.core.telecom.UssdSim

/**
 * Holds every dialog visibility flag on the Settings screen, plus the
 * custom-accent hex input state. One object is passed to the sections
 * (which open dialogs) and the dialogs (which dismiss them), so the
 * main SettingsScreen stays under 400 lines.
 */
class SettingsDialogState {
    var showResetDialog by mutableStateOf(false)
    var showCustomColorDialog by mutableStateOf(false)
    var showThemeDialog by mutableStateOf(false)
    var showAccentDialog by mutableStateOf(false)
    var showDensityDialog by mutableStateOf(false)
    var showCardStyleDialog by mutableStateOf(false)
    var showTabDialog by mutableStateOf(false)
    var showAnimDialog by mutableStateOf(false)
    var showBottomBarStyleDialog by mutableStateOf(false)
    var showBottomBarSizeDialog by mutableStateOf(false)
    var showHeaderSizeDialog by mutableStateOf(false)
    var showGlassIntensityDialog by mutableStateOf(false)
    var showBackdropDialog by mutableStateOf(false)
    var showSortDialog by mutableStateOf(false)
    var showFormatDialog by mutableStateOf(false)
    var showCallDisplayDialog by mutableStateOf(false)
    var showCallBackgroundPage by mutableStateOf(false)
    var showAnswerStylePage by mutableStateOf(false)
    var showGlassStudioPage by mutableStateOf(false)
    var showDefaultSimDialog by mutableStateOf(false)
    var showQuickDeclineDialog by mutableStateOf(false)
    var showBlockedNumbers by mutableStateOf(false)
    var showAboutDialog by mutableStateOf(false)
    var showDebugLogDialog by mutableStateOf(false)
    var showCallWaitingDialog by mutableStateOf(false)
    var showUssdSimPicker by mutableStateOf(false)
    var customColorHexInput by mutableStateOf("")
    var customColorError by mutableStateOf(false)
    // USSD flow: picked code -> SIM picker (dual SIM) -> sending -> result.
    // ussdRequestId invalidates late carrier callbacks after dismissal.
    var pendingUssdCode by mutableStateOf<String?>(null)
    var ussdSims by mutableStateOf<List<UssdSim>>(emptyList())
    var ussdState by mutableStateOf<UssdState?>(null)
    var ussdRequestId by mutableIntStateOf(0)

    fun dialUssdFallback(context: android.content.Context, code: String) {
        runCatching {
            context.startActivity(
                Intent(
                    Intent.ACTION_DIAL,
                    Uri.parse("tel:${Uri.encode(code)}")
                )
            )
        }
    }

    fun runUssd(context: android.content.Context, code: String, sim: UssdSim) {
        val requestId = ussdRequestId + 1
        ussdRequestId = requestId
        ussdState = UssdState.Sending(code, sim.label)
        val started = UssdHelper.sendUssdRequest(
            context,
            sim.subscriptionId,
            code,
            onResponse = { response ->
                if (requestId == ussdRequestId) {
                    ussdState = UssdState.Result(response)
                }
            },
            onCarrierError = { reason ->
                if (requestId == ussdRequestId) {
                    ussdState = UssdState.Result(
                        if (reason == TelephonyManager.USSD_ERROR_SERVICE_UNAVAIL) {
                            context.getString(R.string.ussd_error_unavailable)
                        } else {
                            context.getString(R.string.ussd_error_failed)
                        }
                    )
                }
            }
        )
        if (!started) {
            ussdState = null
            dialUssdFallback(context, code)
        }
    }

    fun pickSimForUssd(context: android.content.Context, code: String) {
        val sims = UssdHelper.getActiveSims(context)
        when {
            sims.size > 1 -> {
                pendingUssdCode = code
                ussdSims = sims
                showUssdSimPicker = true
            }
            sims.size == 1 -> runUssd(context, code, sims[0])
            else -> dialUssdFallback(context, code)
        }
    }
}

