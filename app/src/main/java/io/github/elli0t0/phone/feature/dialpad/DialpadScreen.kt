package io.github.elli0t0.phone.feature.dialpad

import android.Manifest
import android.content.Context
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.elli0t0.phone.PhoneApplication
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.telecom.CallManager
import io.github.elli0t0.phone.feature.setup.SetupWarningCard
import kotlinx.coroutines.launch

@Composable
fun DialpadScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as PhoneApplication
    val repository = app.container.contactsRepository
    val viewModel: DialpadViewModel = viewModel(
        factory = DialpadViewModel.provideFactory(
            repository = repository,
            savedStateHandle = androidx.lifecycle.SavedStateHandle()
        )
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = LocalSpacing.current
    val view = LocalView.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val simAccounts = remember(context) { io.github.elli0t0.phone.core.telecom.TelecomHelper.getCallCapablePhoneAccounts(context) }
    var simPickerTargetNumber by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }

    fun dialNumber(number: String, lookupKey: String? = null) {
        if (simAccounts.size <= 1) {
            CallManager.placeCall(context, number)
            return
        }
        coroutineScope.launch {
            when (val res = io.github.elli0t0.phone.core.telecom.SimCallHelper.resolveSim(
                context, number, lookupKey, simAccounts, app.container.contactSimRepository
            )) {
                is io.github.elli0t0.phone.core.telecom.SimCallResolution.UseSim ->
                    CallManager.placeCall(context, number, res.sim.handle)
                is io.github.elli0t0.phone.core.telecom.SimCallResolution.ShowDialog -> {
                    simPickerTargetNumber = number
                }
            }
        }
    }

    if (simPickerTargetNumber != null) {
        io.github.elli0t0.phone.feature.call.ui.DualSimPickerDialog(
            simAccounts = simAccounts,
            targetNumber = simPickerTargetNumber.orEmpty(),
            onSimSelected = { sim ->
                val num = simPickerTargetNumber.orEmpty()
                simPickerTargetNumber = null
                CallManager.placeCall(context, num, sim.handle)
            },
            onDismiss = {
                simPickerTargetNumber = null
            }
        )
    }

    // Permission launcher for CALL_PHONE
    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // On either granted or denied, placeCall handles it (ACTION_CALL or ACTION_DIAL fallback)
        dialNumber(uiState.rawNumber)
    }

    fun performHaptic() {
        val hapticEnabled = try {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.HAPTIC_FEEDBACK_ENABLED,
                1
            ) != 0
        } catch (_: Exception) {
            true
        }
        if (hapticEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    fun performTickHaptic() {
        val hapticEnabled = try {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.HAPTIC_FEEDBACK_ENABLED,
                1
            ) != 0
        } catch (_: Exception) {
            true
        }
        if (hapticEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                viewModel.stopBackspaceRepeat()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            viewModel.stopBackspaceRepeat()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = spacing.screenHorizontalPadding)
            .testTag("dialpad_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SetupWarningCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.small)
        )
        // T9 Matching contacts list above the keypad
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = spacing.small),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (uiState.matchingContacts.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialpad_search_results")
                ) {
                    items(uiState.matchingContacts, key = { it.id }) { contact ->
                        DialpadSearchRow(
                            contact = contact,
                            onClick = {
                                performHaptic()
                                viewModel.onContactSelected(contact)
                            },
                            onCallClick = {
                                performHaptic()
                                val num = contact.primaryNumberPreview ?: ""
                                if (num.isNotBlank()) {
                                    dialNumber(num, contact.lookupKey)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Formatted Number Display (supports long click to paste)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            val clipText = clipboardManager.getText()?.text
                            if (!clipText.isNullOrBlank()) {
                                performHaptic()
                                viewModel.onNumberPasted(clipText)
                            }
                        }
                    )
                }
                .testTag("dialpad_number_display"),
            contentAlignment = Alignment.Center
        ) {
            val displayText = uiState.formattedNumber.ifBlank { uiState.rawNumber }
            Text(
                text = displayText,
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 36.sp),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.height(spacing.small))

        // Keypad Grid (Keys 1-9, *, 0, #)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = spacing.small),
            verticalArrangement = Arrangement.spacedBy(18.dp),
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
                                performHaptic()
                                viewModel.onKeyPressed(dialKey.digit)
                            },
                            onLongClick = if (dialKey.digit == '0') {
                                {
                                    performHaptic()
                                    viewModel.onLongPressZero()
                                }
                            } else null
                        )
                    }
                }
            }
        }

        // Action row: Left spacer, Call Button, Backspace Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = spacing.medium),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Placeholder empty box for balance (same size as backspace)
            Box(modifier = Modifier.size(56.dp))

            // Call button (disabled when empty input)
            FloatingActionButton(
                onClick = {
                    if (uiState.canCall) {
                        performHaptic()
                        callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                    }
                },
                modifier = Modifier
                    .size(76.dp)
                    .testTag("btn_call"),
                shape = CircleShape,
                containerColor = if (uiState.canCall) {
                    io.github.elli0t0.phone.core.design.CallAnswerGreen
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (uiState.canCall) {
                    io.github.elli0t0.phone.core.design.OnAccentBlue
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                },
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_phone),
                    contentDescription = stringResource(id = R.string.cd_call_button),
                    modifier = Modifier.size(32.dp)
                )
            }

            // Backspace Button with accelerated repeat deletion
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState.rawNumber.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    performHaptic()
                                    viewModel.onBackspacePressed()
                                    viewModel.startBackspaceRepeat(onTickHaptic = { performTickHaptic() })
                                    try {
                                        waitForUpOrCancellation()
                                    } finally {
                                        viewModel.stopBackspaceRepeat()
                                    }
                                }
                            }
                            .testTag("btn_backspace"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_backspace),
                            contentDescription = stringResource(id = R.string.cd_backspace_button),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
