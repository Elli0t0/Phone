package io.github.elli0t0.phone.feature.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.callBackgroundBrush
import io.github.elli0t0.phone.core.design.saveAppBackgroundPhoto
import io.github.elli0t0.phone.data.settings.model.AppBackground
import io.github.elli0t0.phone.data.settings.model.AppSettings
import io.github.elli0t0.phone.navigation.InnerTopBar
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * App background picker: the same page pattern as CallBackgroundScreen
 * (Scaffold + InnerTopBar + LazyColumn). AURORA is the default look;
 * gradients reuse the call-background brushes; PLAIN is the bare theme
 * background; PHOTO uses a user-chosen image with a darkness veil.
 * The Backdrop row and dialog stay as they are: intensity (Off /
 * Subtle / Vivid) is still set there.
 */
@Composable
fun AppBackgroundScreen(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val spacing = LocalSpacing.current
    val scope = rememberCoroutineScope()
    val photoExists = remember(settings.appBackgroundPhotoVersion) {
        File(context.filesDir, "app_background.jpg").exists()
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) { saveAppBackgroundPhoto(context, uri) }
                if (ok) {
                    viewModel.onAppBackgroundPhotoChanged(System.currentTimeMillis())
                    viewModel.onAppBackgroundChanged(AppBackground.PHOTO)
                } else {
                    Toast.makeText(context, R.string.app_background_photo_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    val pickPhoto = {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    // Slider draft: the preview follows the drag live, and the value
    // is written to Settings once on release (not per frame).
    var dimDraft by remember(settings.appBackgroundDim) {
        mutableStateOf(settings.appBackgroundDim)
    }

    Scaffold(modifier = modifier.fillMaxSize().testTag("app_background_screen")) { inner ->
        Column(Modifier.fillMaxSize().padding(bottom = inner.calculateBottomPadding())) {
            InnerTopBar(
                onBack = onNavigateBack,
                title = stringResource(R.string.settings_app_background),
                backTestTag = "btn_app_background_back"
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = spacing.screenHorizontalPadding,
                    end = spacing.screenHorizontalPadding,
                    top = spacing.small,
                    bottom = 24.dp
                )
            ) {
                items(AppBackground.entries.chunked(3)) { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = spacing.small),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { bg ->
                            AppBackgroundSwatch(
                                bg = bg,
                                selected = settings.appBackground == bg,
                                accentColor = settings.accentColor,
                                photoVersion = settings.appBackgroundPhotoVersion,
                                photoExists = photoExists,
                                onClick = {
                                    when {
                                        bg != AppBackground.PHOTO -> viewModel.onAppBackgroundChanged(bg)
                                        photoExists -> viewModel.onAppBackgroundChanged(AppBackground.PHOTO)
                                        else -> pickPhoto()
                                    }
                                }
                            )
                        }
                    }
                }
                if (settings.appBackground == AppBackground.PHOTO) {
                    item {
                        TextButton(onClick = pickPhoto) {
                            Text(stringResource(R.string.app_background_change_photo))
                        }
                    }
                    item {
                        Column(Modifier.fillMaxWidth().padding(top = spacing.small)) {
                            Text(
                                stringResource(R.string.app_background_darkness),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Slider(
                                value = dimDraft,
                                onValueChange = { dimDraft = it },
                                valueRange = 0f..0.9f,
                                onValueChangeFinished = { viewModel.onAppBackgroundDimChanged(dimDraft) }
                            )
                        }
                    }
                }
                item {
                    Text(
                        stringResource(R.string.app_background_intensity_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = spacing.small)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppBackgroundSwatch(
    bg: AppBackground,
    selected: Boolean,
    accentColor: Long,
    photoVersion: Long,
    photoExists: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val swatchShape = RoundedCornerShape(20.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(96.dp)
    ) {
        Box(
            modifier = Modifier
                .size(96.dp, 160.dp)
                .clip(swatchShape)
                .then(
                    if (selected) {
                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, swatchShape)
                    } else {
                        Modifier
                    }
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            when (bg) {
                AppBackground.AURORA -> {
                    val accent = Color(accentColor)
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.radialGradient(listOf(accent, accent.copy(alpha = 0.45f)))
                        )
                    )
                }
                AppBackground.PLAIN -> {
                    Box(
                        Modifier.fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, swatchShape)
                    )
                }
                AppBackground.PHOTO -> {
                    if (photoExists) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(File(context.filesDir, "app_background.jpg"))
                                .memoryCacheKey("app_bg_$photoVersion").build(),
                            contentDescription = null, contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            painterResource(R.drawable.ic_add), contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    Box(
                        Modifier.fillMaxSize()
                            .background(callBackgroundBrush(bg.gradient!!, accentColor))
                    )
                }
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painterResource(R.drawable.ic_check), contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        Text(
            bg.label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
