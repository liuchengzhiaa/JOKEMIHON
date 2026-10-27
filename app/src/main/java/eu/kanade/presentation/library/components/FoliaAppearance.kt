package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.presentation.library.components.folia.FoliaFocusShape
import eu.kanade.presentation.library.components.folia.FoliaFocusSize
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import mihon.app.di.appGraph
import tachiyomi.core.common.preference.Preference
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.HeadingItem
import tachiyomi.presentation.core.components.SettingsChipRow
import tachiyomi.presentation.core.components.SliderItem
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.roundToInt

@Stable
internal class FoliaAppearance(private val preferences: LibraryPreferences) {
    var shape by mutableStateOf(
        FoliaFocusShape.entries.firstOrNull { it.name == preferences.foliaShape.get() }
            ?: FoliaFocusShape.FullCover,
    )
    var size by mutableStateOf(
        FoliaFocusSize.entries.firstOrNull { it.name == preferences.foliaSize.get() }
            ?: FoliaFocusSize.Medium,
    )
    var density by mutableIntStateOf(preferences.foliaDensity.get().coerceIn(70, 130))
    var zoom by mutableFloatStateOf((preferences.foliaZoom.get() / 100f).coerceIn(0.72f, 1.35f))
    var ambient by mutableIntStateOf(preferences.foliaAmbient.get().coerceIn(0, 150))
    var tint by mutableIntStateOf(preferences.foliaTint.get().coerceIn(0, 60))
    var motion by mutableIntStateOf(preferences.foliaMotion.get().coerceIn(0, 100))
    var lightsOut by mutableStateOf(preferences.foliaLightsOut.get())

    fun snapshot() = FoliaAppearanceSnapshot(
        shape.name,
        size.name,
        density,
        (zoom * 100).roundToInt(),
        ambient,
        tint,
        motion,
        lightsOut,
    )

    fun persist(value: FoliaAppearanceSnapshot = snapshot()) {
        fun <T> save(pref: Preference<T>, next: T) {
            if (pref.get() != next) pref.set(next)
        }
        save(preferences.foliaShape, value.shape)
        save(preferences.foliaSize, value.size)
        save(preferences.foliaDensity, value.density)
        save(preferences.foliaZoom, value.zoom)
        save(preferences.foliaAmbient, value.ambient)
        save(preferences.foliaTint, value.tint)
        save(preferences.foliaMotion, value.motion)
        save(preferences.foliaLightsOut, value.lightsOut)
    }
}

internal data class FoliaAppearanceSnapshot(
    val shape: String,
    val size: String,
    val density: Int,
    val zoom: Int,
    val ambient: Int,
    val tint: Int,
    val motion: Int,
    val lightsOut: Boolean,
)

@OptIn(FlowPreview::class)
@Composable
internal fun rememberFoliaAppearance(): FoliaAppearance {
    val context = LocalContext.current
    val appearance = remember { FoliaAppearance(context.appGraph.libraryPreferences) }
    LaunchedEffect(appearance) {
        snapshotFlow { appearance.snapshot() }.debounce(240).collect(appearance::persist)
    }
    DisposableEffect(appearance) { onDispose { appearance.persist() } }
    return appearance
}

@Composable
internal fun FoliaAppearanceSheet(
    appearance: FoliaAppearance,
    onRecenter: () -> Unit,
    onClearFocus: () -> Unit,
    onDismiss: () -> Unit,
) {
    AdaptiveSheet(onDismissRequest = onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(vertical = 12.dp)) {
            HeadingItem(MR.strings.jokemihon_folia_settings)
            SettingsChipRow(MR.strings.jokemihon_folia_shape) {
                FoliaFocusShape.entries.forEach { choice ->
                    val label = when (choice) {
                        FoliaFocusShape.FullCover -> MR.strings.jokemihon_folia_full_cover
                        FoliaFocusShape.Square -> MR.strings.jokemihon_folia_square
                        FoliaFocusShape.Portrait -> MR.strings.jokemihon_folia_portrait
                        FoliaFocusShape.Landscape -> MR.strings.jokemihon_folia_landscape
                        FoliaFocusShape.Heart -> MR.strings.jokemihon_folia_heart
                        FoliaFocusShape.Circle -> MR.strings.jokemihon_folia_circle
                    }
                    FilterChip(
                        selected = appearance.shape == choice,
                        onClick = { appearance.shape = choice },
                        label = { Text(stringResource(label)) },
                    )
                }
            }
            SettingsChipRow(MR.strings.jokemihon_folia_size) {
                FoliaFocusSize.entries.forEach { choice ->
                    val label = when (choice) {
                        FoliaFocusSize.Small -> MR.strings.jokemihon_folia_small
                        FoliaFocusSize.Medium -> MR.strings.jokemihon_folia_medium
                        FoliaFocusSize.Large -> MR.strings.jokemihon_folia_large
                    }
                    FilterChip(
                        selected = appearance.size == choice,
                        onClick = { appearance.size = choice },
                        label = { Text(stringResource(label)) },
                    )
                }
            }
            // Density rebuilds the scene: preview the number and commit once on release, as music does.
            var pendingDensity by remember(appearance.density) { mutableIntStateOf(appearance.density) }
            Column(Modifier.padding(horizontal = 24.dp)) {
                Text(stringResource(MR.strings.jokemihon_folia_density) + "  $pendingDensity%")
                Slider(
                    value = pendingDensity.toFloat(),
                    onValueChange = { pendingDensity = it.roundToInt() },
                    onValueChangeFinished = { appearance.density = pendingDensity },
                    valueRange = 70f..130f,
                    steps = 59,
                )
            }
            SliderItem(
                value = (appearance.zoom * 100).roundToInt(),
                valueRange = 72..135,
                label = stringResource(MR.strings.jokemihon_folia_zoom),
                valueString = "${(appearance.zoom * 100).roundToInt()}%",
                onChange = { appearance.zoom = it / 100f },
                pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            SliderItem(
                value = appearance.ambient,
                valueRange = 0..150,
                label = stringResource(MR.strings.jokemihon_folia_ambient),
                valueString = "${appearance.ambient}%",
                onChange = { appearance.ambient = it },
                pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            SliderItem(
                value = appearance.tint,
                valueRange = 0..60,
                label = stringResource(MR.strings.jokemihon_folia_tint),
                valueString = "${appearance.tint}%",
                onChange = { appearance.tint = it },
                pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            SliderItem(
                value = appearance.motion,
                valueRange = 0..100,
                label = stringResource(MR.strings.jokemihon_folia_motion),
                valueString = "${appearance.motion}%",
                onChange = { appearance.motion = it },
                pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            CheckboxItem(
                stringResource(MR.strings.jokemihon_folia_lights_out),
                appearance.lightsOut,
            ) { appearance.lightsOut = !appearance.lightsOut }
            Row(Modifier.padding(horizontal = 12.dp)) {
                TextButton(onClick = {
                    onRecenter()
                    onDismiss()
                }) { Text(stringResource(MR.strings.jokemihon_folia_reset)) }
                TextButton(onClick = {
                    onClearFocus()
                    onDismiss()
                }) { Text(stringResource(MR.strings.jokemihon_folia_clear_focus)) }
                TextButton(onClick = onDismiss) { Text(stringResource(MR.strings.action_close)) }
            }
        }
    }
}
