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

@file:Suppress("UNUSED", "MatchingDeclarationName")

package net.ktnx.mobileledger.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import net.ktnx.mobileledger.core.ui.theme.toHsl as coreToHsl

// Re-export profile theme functions from core:ui for backward compatibility
// New code should import from net.ktnx.mobileledger.core.ui.theme directly

fun lightColorSchemeFromHue(hue: Float): ColorScheme = net.ktnx.mobileledger.core.ui.theme.lightColorSchemeFromHue(hue)

fun darkColorSchemeFromHue(hue: Float): ColorScheme = net.ktnx.mobileledger.core.ui.theme.darkColorSchemeFromHue(hue)

fun hslToColor(hue: Float, saturation: Float, lightness: Float): Color =
    net.ktnx.mobileledger.core.ui.theme.hslToColor(hue, saturation, lightness)

fun Color.toHsl(): Triple<Float, Float, Float> = coreToHsl()
