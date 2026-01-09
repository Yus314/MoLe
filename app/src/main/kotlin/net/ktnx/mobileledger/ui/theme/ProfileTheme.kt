/*
 * Copyright © 2024 Damyan Ivanov.
 * This file is part of MoLe.
 * MoLe is free software: you can distribute it and/or modify it
 * under the term of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your opinion), any later version.
 *
 * MoLe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License terms for details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MoLe. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ktnx.mobileledger.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.math.abs

/**
 * Creates a light color scheme based on a hue value (0-360).
 * Uses HSL color model to generate consistent color variations.
 */
fun lightColorSchemeFromHue(hue: Float): ColorScheme {
    val normalizedHue = normalizeHue(hue)

    val primary = hslToColor(normalizedHue, 0.6f, 0.35f)
    val primaryContainer = hslToColor(normalizedHue, 0.5f, 0.85f)
    val onPrimary = Color.White
    val onPrimaryContainer = hslToColor(normalizedHue, 0.7f, 0.15f)

    val secondary = hslToColor(normalizedHue + 30f, 0.5f, 0.45f)
    val secondaryContainer = hslToColor(normalizedHue + 30f, 0.4f, 0.85f)
    val onSecondary = Color.White
    val onSecondaryContainer = hslToColor(normalizedHue + 30f, 0.6f, 0.15f)

    val tertiary = hslToColor(normalizedHue + 60f, 0.4f, 0.45f)
    val tertiaryContainer = hslToColor(normalizedHue + 60f, 0.3f, 0.85f)
    val onTertiary = Color.White
    val onTertiaryContainer = hslToColor(normalizedHue + 60f, 0.5f, 0.15f)

    return lightColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = MoLeBackground,
        onBackground = MoLeOnBackground,
        surface = MoLeSurface,
        onSurface = MoLeOnSurface,
        surfaceVariant = MoLeSurfaceVariant,
        onSurfaceVariant = MoLeOnSurfaceVariant,
        error = MoLeError,
        onError = MoLeOnError,
        errorContainer = MoLeErrorContainer,
        onErrorContainer = MoLeOnErrorContainer,
        outline = MoLeOutline,
        outlineVariant = MoLeOutlineVariant
    )
}

/**
 * Creates a dark color scheme based on a hue value (0-360).
 * Uses HSL color model to generate consistent color variations.
 */
fun darkColorSchemeFromHue(hue: Float): ColorScheme {
    val normalizedHue = normalizeHue(hue)

    val primary = hslToColor(normalizedHue, 0.5f, 0.65f)
    val primaryContainer = hslToColor(normalizedHue, 0.6f, 0.25f)
    val onPrimary = hslToColor(normalizedHue, 0.7f, 0.15f)
    val onPrimaryContainer = hslToColor(normalizedHue, 0.4f, 0.85f)

    val secondary = hslToColor(normalizedHue + 30f, 0.4f, 0.65f)
    val secondaryContainer = hslToColor(normalizedHue + 30f, 0.5f, 0.25f)
    val onSecondary = hslToColor(normalizedHue + 30f, 0.6f, 0.15f)
    val onSecondaryContainer = hslToColor(normalizedHue + 30f, 0.3f, 0.85f)

    val tertiary = hslToColor(normalizedHue + 60f, 0.35f, 0.65f)
    val tertiaryContainer = hslToColor(normalizedHue + 60f, 0.45f, 0.25f)
    val onTertiary = hslToColor(normalizedHue + 60f, 0.55f, 0.15f)
    val onTertiaryContainer = hslToColor(normalizedHue + 60f, 0.25f, 0.85f)

    return darkColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = MoLeBackgroundDark,
        onBackground = MoLeOnBackgroundDark,
        surface = MoLeSurfaceDark,
        onSurface = MoLeOnSurfaceDark,
        surfaceVariant = MoLeSurfaceVariantDark,
        onSurfaceVariant = MoLeOnSurfaceVariantDark,
        error = MoLeErrorDark,
        onError = MoLeOnErrorDark,
        errorContainer = MoLeErrorContainerDark,
        onErrorContainer = MoLeOnErrorContainerDark,
        outline = MoLeOutlineDark,
        outlineVariant = MoLeOutlineVariantDark
    )
}

/**
 * Normalizes hue to 0-360 range.
 */
private fun normalizeHue(hue: Float): Float {
    var normalized = hue % 360f
    if (normalized < 0) normalized += 360f
    return normalized
}

/**
 * Converts HSL color values to a Compose Color.
 *
 * @param hue Hue in degrees (0-360)
 * @param saturation Saturation (0-1)
 * @param lightness Lightness (0-1)
 * @return Compose Color
 */
fun hslToColor(hue: Float, saturation: Float, lightness: Float): Color {
    val normalizedHue = normalizeHue(hue)
    val c = (1f - abs(2f * lightness - 1f)) * saturation
    val x = c * (1f - abs((normalizedHue / 60f) % 2f - 1f))
    val m = lightness - c / 2f

    val (r, g, b) = when {
        normalizedHue < 60f -> Triple(c, x, 0f)
        normalizedHue < 120f -> Triple(x, c, 0f)
        normalizedHue < 180f -> Triple(0f, c, x)
        normalizedHue < 240f -> Triple(0f, x, c)
        normalizedHue < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    return Color(
        red = (r + m).coerceIn(0f, 1f),
        green = (g + m).coerceIn(0f, 1f),
        blue = (b + m).coerceIn(0f, 1f)
    )
}

/**
 * Converts a Color to HSL values.
 *
 * @return Triple of (hue: 0-360, saturation: 0-1, lightness: 0-1)
 */
fun Color.toHsl(): Triple<Float, Float, Float> {
    val r = red
    val g = green
    val b = blue

    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val l = (max + min) / 2f

    if (max == min) {
        return Triple(0f, 0f, l)
    }

    val d = max - min
    val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)

    val h = when (max) {
        r -> ((g - b) / d + (if (g < b) 6f else 0f)) * 60f
        g -> ((b - r) / d + 2f) * 60f
        else -> ((r - g) / d + 4f) * 60f
    }

    return Triple(h, s, l)
}
