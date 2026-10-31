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
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.BackdropContent
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.saveAppBackgroundPhoto
import io.github.elli0t0.phone.core.design.scenes.AnimeSceneBackgrounds
import io.github.elli0t0.phone.core.design.scenes.AnimeScenes
import io.github.elli0t0.phone.core.design.scenes.SceneSpec
import io.github.elli0t0.phone.core.design.scenes.scenePreviewIntensity
import io.github.elli0t0.phone.data.settings.model.AppBackground
import io.github.elli0t0.phone.data.settings.model.AppSettings
import io.github.elli0t0.phone.data.settings.model.PaletteBackgrounds
import io.github.elli0t0.phone.data.settings.model.PaletteBg
import io.github.elli0t0.phone.navigation.InnerTopBar
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * App background picker: the same page pattern as CallBackgroundScreen
 * (Scaffold + InnerTopBar + LazyColumn). Intensity (0.18.7) is a
 * slider at the top of this page, replacing the old Backdrop
 * Off/Subtle/Vivid gate. Sections: Colors (data-driven palettes,
 * 3 per row), Scenes (every other background, each swatch
 * rendering its real scene live, so animated backgrounds drift,
 * twinkle, and pulse in the picker too), Photo (user image with
 * its darkness slider). PLAIN is the bare theme background.
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
    // Slider drafts: the preview follows the drag live, and the
    // value is written to Settings once on release (not per frame).
    var intensityDraft by remember(settings.appBackgroundIntensity) {
        mutableStateOf(settings.appBackgroundIntensity)
    }
    var dimDraft by remember(settings.appBackgroundDim) {
        mutableStateOf(settings.appBackgroundDim)
    }
    val scenes = remember {
        AppBackground.entries.filter {
            it != AppBackground.PALETTE && it != AppBackground.PHOTO && it !in AnimeSceneBackgrounds
        }
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
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(top = spacing.small)
                            .searchHighlight("background_intensity")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                stringResource(R.string.settings_app_background_intensity),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "${(intensityDraft * 100).roundToInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Slider(
                            value = intensityDraft,
                            onValueChange = { intensityDraft = it },
                            valueRange = 0.1f..1.0f,
                            steps = 8,
                            onValueChangeFinished = {
                                viewModel.onAppBackgroundIntensityChanged(intensityDraft)
                            }
                        )
                    }
                }
                if (settings.pureBlackDarkMode) {
                    item {
                        Text(
                            stringResource(R.string.settings_app_background_pure_black_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = spacing.small)
                        )
                    }
                }
                item {
                    SettingsSectionHeader(stringResource(R.string.settings_app_background_colors))
                }
                items(PaletteBackgrounds.chunked(3)) { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = spacing.small),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { palette ->
                            PaletteSwatch(
                                palette = palette,
                                selected = settings.appBackground == AppBackground.PALETTE &&
                                    settings.appBackgroundPaletteId == palette.id,
                                onClick = {
                                    // Id first, then the background: the
                                    // intermediate frame still shows the
                                    // old background, never a wrong palette.
                                    viewModel.onAppBackgroundPaletteIdChanged(palette.id)
                                    viewModel.onAppBackgroundChanged(AppBackground.PALETTE)
                                }
                            )
                        }
                    }
                }
                item {
                    SettingsSectionHeader(stringResource(R.string.settings_app_background_scenes))
                }
                items(scenes.chunked(3)) { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = spacing.small),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { bg ->
                            AppBackgroundSwatch(
                                bg = bg,
                                selected = settings.appBackground == bg,
                                photoExists = photoExists,
                                onClick = { viewModel.onAppBackgroundChanged(bg) }
                            )
                        }
                    }
                }
                items(AnimeScenes.chunked(2)) { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = spacing.small),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { spec ->
                            AnimeScenePreview(
                                spec = spec,
                                selected = settings.appBackground == spec.background,
                                onClick = { viewModel.onAppBackgroundChanged(spec.background) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                item {
                    SettingsSectionHeader(stringResource(R.string.settings_app_background_photo))
                }
                item {
                    AppBackgroundSwatch(
                        bg = AppBackground.PHOTO,
                        selected = settings.appBackground == AppBackground.PHOTO,
                        photoExists = photoExists,
                        onClick = {
                            if (photoExists) {
                                viewModel.onAppBackgroundChanged(AppBackground.PHOTO)
                            } else {
                                pickPhoto()
                            }
                        }
                    )
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
            }
        }
    }
}

@Composable
private fun PaletteSwatch(
    palette: PaletteBg,
    selected: Boolean,
    onClick: () -> Unit
) {
    val swatchShape = RoundedCornerShape(20.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(96.dp)
    ) {
        Box(
            modifier = Modifier
                .size(96.dp, 72.dp)
                .clip(swatchShape)
                .background(Brush.horizontalGradient(palette.colors.map { Color(it) }))
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
            palette.label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun AnimeScenePreview(
    spec: SceneSpec,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val previewShape = RoundedCornerShape(20.dp)
    val intensity = scenePreviewIntensity()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(previewShape)
                .then(
                    if (selected) {
                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, previewShape)
                    } else {
                        Modifier
                    }
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            // Static frame: the real scene, frozen (animate = false).
            spec.content(intensity, false, Modifier.fillMaxSize())
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
            spec.background.label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun AppBackgroundSwatch(
    bg: AppBackground,
    selected: Boolean,
    photoExists: Boolean,
    onClick: () -> Unit
) {
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
                AppBackground.PLAIN -> {
                    Box(
                        Modifier.fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, swatchShape)
                    )
                }
                AppBackground.PHOTO -> {
                    if (photoExists) {
                        BackdropContent(bg, Modifier.fillMaxSize())
                    } else {
                        Icon(
                            painterResource(R.drawable.ic_add), contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // Every other entry renders its real scene live:
                // gradients, blobs, mesh, sky, waves, neon, and
                // grain all preview exactly as they paint.
                else -> BackdropContent(bg, Modifier.fillMaxSize())
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
