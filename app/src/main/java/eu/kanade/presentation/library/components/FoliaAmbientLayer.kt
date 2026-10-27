package eu.kanade.presentation.library.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import kotlin.math.max

@Composable
internal fun FoliaAmbientLayer(palette: List<Color>, appearance: FoliaAppearance, wash: Boolean, modifier: Modifier) {
    MusicAmbientLayer(
        palette,
        if (wash) AmbientLayerMode.PosterWash else AmbientLayerMode.Backdrop,
        appearance.ambient / 100f,
        appearance.motion,
        modifier,
    )
}

private fun foliaAmbientDuration(base: Int, percent: Int) = (base * percent.coerceIn(0, 100) / 100f).toInt()

internal enum class AmbientLayerMode { Backdrop, PosterWash }

@Composable
private fun MusicAmbientLayer(
    palette: List<Color>,
    mode: AmbientLayerMode,
    strengthMultiplier: Float,
    motionPercent: Int,
    modifier: Modifier = Modifier,
) {
    val primaryTarget = palette.getOrNull(0) ?: Color.Transparent
    val secondaryTarget = palette.getOrNull(1)
        ?: palette.getOrNull(0)?.let { lerp(it, Color.White, 0.18f) }
        ?: Color.Transparent
    val tertiaryTarget = palette.getOrNull(2)
        ?: palette.getOrNull(0)?.let { lerp(it, Color.Black, 0.24f) }
        ?: Color.Transparent
    val primary by animateColorAsState(
        targetValue = primaryTarget,
        animationSpec = tween(durationMillis = foliaAmbientDuration(900, motionPercent)),
        label = "wallAmbientPrimary",
    )
    val secondary by animateColorAsState(
        targetValue = secondaryTarget,
        animationSpec = tween(durationMillis = foliaAmbientDuration(900, motionPercent)),
        label = "wallAmbientSecondary",
    )
    val tertiary by animateColorAsState(
        targetValue = tertiaryTarget,
        animationSpec = tween(durationMillis = foliaAmbientDuration(900, motionPercent)),
        label = "wallAmbientTertiary",
    )
    val animatedStrength by animateFloatAsState(
        targetValue = strengthMultiplier,
        animationSpec = tween(durationMillis = foliaAmbientDuration(260, motionPercent)),
        label = "wallAmbientStrength${mode.name}",
    )
    val isDarkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val primaryStrength = when (mode) {
        AmbientLayerMode.Backdrop -> if (isDarkSurface) 0.32f else 0.20f
        AmbientLayerMode.PosterWash -> if (isDarkSurface) 0.13f else 0.085f
    }
    val secondaryStrength = when (mode) {
        AmbientLayerMode.Backdrop -> if (isDarkSurface) 0.28f else 0.17f
        AmbientLayerMode.PosterWash -> if (isDarkSurface) 0.105f else 0.065f
    }
    val tertiaryStrength = when (mode) {
        AmbientLayerMode.Backdrop -> if (isDarkSurface) 0.20f else 0.12f
        AmbientLayerMode.PosterWash -> if (isDarkSurface) 0.08f else 0.045f
    }
    val vignetteStrength = if (isDarkSurface) 0.18f else 0.055f
    val effectivePrimaryStrength = primaryStrength * animatedStrength
    val effectiveSecondaryStrength = secondaryStrength * animatedStrength
    val effectiveTertiaryStrength = tertiaryStrength * animatedStrength
    val effectiveVignetteStrength = vignetteStrength * animatedStrength

    Box(
        modifier = modifier.drawWithCache {
            val largestEdge = max(size.width, size.height)
            val primaryBrush = Brush.radialGradient(
                colors = listOf(
                    primary.multipliedAlpha(effectivePrimaryStrength),
                    primary.multipliedAlpha(effectivePrimaryStrength * 0.38f),
                    Color.Transparent,
                ),
                center = Offset(size.width * 0.14f, size.height * 0.16f),
                radius = largestEdge * 0.72f,
            )
            val secondaryBrush = Brush.radialGradient(
                colors = listOf(
                    secondary.multipliedAlpha(effectiveSecondaryStrength),
                    secondary.multipliedAlpha(effectiveSecondaryStrength * 0.32f),
                    Color.Transparent,
                ),
                center = Offset(size.width * 0.88f, size.height * 0.78f),
                radius = largestEdge * 0.67f,
            )
            val tertiaryBrush = Brush.radialGradient(
                colors = listOf(
                    tertiary.multipliedAlpha(effectiveTertiaryStrength),
                    Color.Transparent,
                ),
                center = Offset(size.width * 0.62f, size.height * 0.30f),
                radius = largestEdge * 0.48f,
            )
            val vignetteBrush = if (mode == AmbientLayerMode.Backdrop) {
                // A shallow edge falloff keeps the colours reading as light in a
                // space rather than three translucent circles. The poster wash omits
                // it so card artwork is coloured, not globally darkened twice.
                Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to Color.Transparent,
                        0.72f to Color.Transparent,
                        1f to Color.Black.copy(alpha = effectiveVignetteStrength),
                    ),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = largestEdge * 0.72f,
                )
            } else {
                null
            }
            onDrawBehind {
                drawRect(primaryBrush)
                drawRect(secondaryBrush)
                drawRect(tertiaryBrush)
                vignetteBrush?.let { brush -> drawRect(brush) }
            }
        },
    )
}

private fun fallbackAmbientPalette(accent: Color): List<Color> = listOf(
    accent,
    lerp(accent, Color.White, 0.18f),
    lerp(accent, Color.Black, 0.24f),
)

private fun Color.multipliedAlpha(factor: Float): Color =
    copy(alpha = alpha * factor.coerceIn(0f, 1f))

internal fun foliaBookPalette(
    facts: eu.kanade.presentation.library.components.folia.FoliaCoverFacts?,
    id: Long,
): List<Color> {
    val sampled = facts?.palette.orEmpty().map(::Color)
    if (sampled.isNotEmpty()) return sampled
    val fallback = listOf(0xFF315C72, 0xFF74586A, 0xFF476458, 0xFF73584B, 0xFF4E557C)
    return fallbackAmbientPalette(Color(fallback[(id.hashCode() - 1).mod(fallback.size)]))
}
