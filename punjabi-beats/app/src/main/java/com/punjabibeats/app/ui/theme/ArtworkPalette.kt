package com.punjabibeats.app.ui.theme

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Dynamic artwork-driven color palette derived directly from current track artwork.
 * Based on BitChord Section 7 UI/Theming architecture.
 */
@Immutable
data class ArtworkPalette(
    val background: Color,
    val wash: Color,
    val elevated: Color,
    val accent: Color,
    val onBackground: Color,
    val onBackgroundVariant: Color,
    val divider: Color,
)

/** Below this, boosting saturation makes quantisation noise visible as a tint. */
private const val CHROMATIC_SATURATION_THRESHOLD = 0.12f
private const val TINT_FADE_MS = 260
private const val SEED_CACHE_ENTRIES = 128
private const val PALETTE_SAMPLE_PX = 128
private const val BOTTOM_EDGE_BAND = 0.18f

@Immutable
data class ArtworkSeed(
    val dominant: Color,
    val vibrant: Color,
    val edge: Color,
    val topBandLuminance: Float,
)

private val seedCache = object : LinkedHashMap<String, ArtworkSeed>(0, 0.75f, true) {
    override fun removeEldestEntry(eldest: Map.Entry<String, ArtworkSeed>) = size > SEED_CACHE_ENTRIES
}

/**
 * Keeps neutral artwork neutral instead of inventing an artificial hue.
 */
fun adaptedArtworkSaturation(source: Float, minimum: Float, maximum: Float): Float {
    val saturation = source.coerceIn(0f, 1f)
    return if (saturation < CHROMATIC_SATURATION_THRESHOLD) {
        saturation
    } else {
        saturation.coerceIn(minimum, maximum)
    }
}

/** WCAG relative luminance calculation */
fun relativeLuminance(argb: Int): Float {
    fun linear(channel: Int): Float {
        val srgb = channel / 255f
        return if (srgb <= 0.04045f) srgb / 12.92f else ((srgb + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()
    }
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    return 0.2126f * linear(r) + 0.7152f * linear(g) + 0.0722f * linear(b)
}

private fun Color.withHsl(
    saturation: (Float) -> Float = { it },
    lightness: (Float) -> Float = { it },
): Color {
    val hsl = FloatArray(3).also { ColorUtils.colorToHSL(toArgb(), it) }
    hsl[1] = saturation(hsl[1]).coerceIn(0f, 1f)
    hsl[2] = lightness(hsl[2]).coerceIn(0f, 1f)
    return Color(ColorUtils.HSLToColor(hsl))
}

private fun ArtworkSeed.toPalette(dark: Boolean): ArtworkPalette = if (dark) {
    ArtworkPalette(
        background = dominant.withHsl(
            saturation = { adaptedArtworkSaturation(it, minimum = 0.20f, maximum = 0.62f) },
            lightness = { 0.13f },
        ),
        wash = edge.withHsl(
            saturation = { adaptedArtworkSaturation(it, minimum = 0.18f, maximum = 0.58f) },
            lightness = { it.coerceIn(0.14f, 0.24f) },
        ),
        elevated = dominant.withHsl(
            saturation = { adaptedArtworkSaturation(it, minimum = 0.20f, maximum = 0.62f) },
            lightness = { 0.22f },
        ),
        accent = vibrant.withHsl(
            saturation = { adaptedArtworkSaturation(it, minimum = 0.55f, maximum = 1f) },
            lightness = { it.coerceIn(0.62f, 0.78f) },
        ),
        onBackground = Color.White,
        onBackgroundVariant = dominant.withHsl(
            saturation = { adaptedArtworkSaturation(it, minimum = 0.22f, maximum = 0.50f) },
            lightness = { 0.80f },
        ),
        divider = Color.White.copy(alpha = 0.12f),
    )
} else {
    ArtworkPalette(
        background = dominant.withHsl(
            saturation = { adaptedArtworkSaturation(it, minimum = 0.14f, maximum = 0.50f) },
            lightness = { 0.91f },
        ),
        wash = edge.withHsl(
            saturation = { adaptedArtworkSaturation(it, minimum = 0.12f, maximum = 0.46f) },
            lightness = { it.coerceIn(0.78f, 0.90f) },
        ),
        elevated = dominant.withHsl(
            saturation = { adaptedArtworkSaturation(it, minimum = 0.14f, maximum = 0.50f) },
            lightness = { 0.83f },
        ),
        accent = vibrant.withHsl(
            saturation = { adaptedArtworkSaturation(it, minimum = 0.55f, maximum = 1f) },
            lightness = { it.coerceIn(0.30f, 0.44f) },
        ),
        onBackground = Color.Black,
        onBackgroundVariant = dominant.withHsl(
            saturation = { adaptedArtworkSaturation(it, minimum = 0.22f, maximum = 0.55f) },
            lightness = { 0.34f },
        ),
        divider = Color.Black.copy(alpha = 0.10f),
    )
}

private fun extractBottomEdgeColor(bitmap: Bitmap): Color {
    val height = bitmap.height
    val width = bitmap.width
    val band = (height * BOTTOM_EDGE_BAND).toInt().coerceIn(1, height)
    val pixels = IntArray(width * band)
    bitmap.getPixels(pixels, 0, width, 0, height - band, width, band)

    var red = 0L
    var green = 0L
    var blue = 0L
    for (pixel in pixels) {
        red += (pixel shr 16) and 0xFF
        green += (pixel shr 8) and 0xFF
        blue += pixel and 0xFF
    }
    val count = pixels.size.coerceAtLeast(1)
    return Color(
        red = (red / count).toInt(),
        green = (green / count).toInt(),
        blue = (blue / count).toInt(),
    )
}

private fun extractSeedFromBitmap(bitmap: Bitmap): ArtworkSeed {
    val palette = Palette.from(bitmap).maximumColorCount(24).generate()
    val swatches = palette.swatches
    val dominantSwatch = palette.dominantSwatch ?: swatches.maxByOrNull { it.population }
    val vibrantSwatch = palette.vibrantSwatch ?: swatches.maxByOrNull { swatch ->
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(swatch.rgb, hsl)
        hsl[1] * sqrt(swatch.population.toFloat())
    }

    val dominantColor = dominantSwatch?.rgb?.let { Color(it) } ?: Color(0xFF1B1322)
    val vibrantColor = vibrantSwatch?.rgb?.let { Color(it) } ?: Color(0xFFFF9900)
    val edgeColor = extractBottomEdgeColor(bitmap)
    val topBandPixel = bitmap.getPixel(bitmap.width / 2, (bitmap.height * 0.05f).toInt().coerceIn(0, bitmap.height - 1))
    val topLuminance = relativeLuminance(topBandPixel)

    return ArtworkSeed(
        dominant = dominantColor,
        vibrant = vibrantColor,
        edge = edgeColor,
        topBandLuminance = topLuminance,
    )
}

/**
 * Composable providing dynamic [ArtworkPalette] extracted from [imageUrl].
 */
@Composable
fun rememberArtworkPalette(
    imageUrl: String?,
    dark: Boolean = true,
    fallbackTheme: PBTheme = LocalPBTheme.current,
): ArtworkPalette {
    val context = LocalContext.current
    var seed by remember(imageUrl) { mutableStateOf(imageUrl?.let(seedCache::get)) }
    val knownUpFront = remember(imageUrl) { seed != null }

    LaunchedEffect(imageUrl) {
        if (imageUrl.isNullOrBlank() || seed != null) return@LaunchedEffect
        val found = withContext(Dispatchers.IO) {
            try {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .size(PALETTE_SAMPLE_PX)
                    .allowHardware(false)
                    .build()
                val result = (loader.execute(request) as? SuccessResult)?.drawable
                val bitmap = (result as? BitmapDrawable)?.bitmap
                bitmap?.let { extractSeedFromBitmap(it) }
            } catch (_: Exception) {
                null
            }
        }
        if (found != null) {
            seedCache[imageUrl] = found
            seed = found
        }
    }

    val target = seed?.toPalette(dark) ?: ArtworkPalette(
        background = fallbackTheme.background,
        wash = fallbackTheme.surface,
        elevated = fallbackTheme.card,
        accent = fallbackTheme.accent,
        onBackground = fallbackTheme.text,
        onBackgroundVariant = fallbackTheme.muted,
        divider = fallbackTheme.muted.copy(alpha = 0.2f),
    )

    val spec: AnimationSpec<Color> = if (knownUpFront) snap() else tween(TINT_FADE_MS)

    return ArtworkPalette(
        background = animateColorAsState(target.background, spec, label = "paletteBg").value,
        wash = animateColorAsState(target.wash, spec, label = "paletteWash").value,
        elevated = animateColorAsState(target.elevated, spec, label = "paletteElevated").value,
        accent = animateColorAsState(target.accent, spec, label = "paletteAccent").value,
        onBackground = animateColorAsState(target.onBackground, spec, label = "paletteOn").value,
        onBackgroundVariant = animateColorAsState(target.onBackgroundVariant, spec, label = "paletteOnVar").value,
        divider = animateColorAsState(target.divider, spec, label = "paletteDiv").value,
    )
}
