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

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

private val LightColorScheme = lightColorScheme(
    primary = MoLePrimary,
    onPrimary = MoLeOnPrimary,
    primaryContainer = MoLePrimary,
    onPrimaryContainer = MoLeOnPrimary,
    secondary = MoLeSecondary,
    onSecondary = MoLeOnSecondary,
    secondaryContainer = MoLeSecondaryVariant,
    onSecondaryContainer = MoLeOnSecondary,
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

private val DarkColorScheme = darkColorScheme(
    primary = MoLePrimaryDark,
    onPrimary = MoLeOnPrimaryDark,
    primaryContainer = MoLePrimaryContainerDark,
    onPrimaryContainer = MoLeOnPrimaryContainerDark,
    secondary = MoLeSecondaryDark,
    onSecondary = MoLeOnSecondaryDark,
    secondaryContainer = MoLeSecondaryContainerDark,
    onSecondaryContainer = MoLeOnSecondaryContainerDark,
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

@Composable
fun MoLeTheme(darkTheme: Boolean = isSystemInDarkTheme(), profileHue: Float? = null, content: @Composable () -> Unit) {
    val colorScheme = remember(darkTheme, profileHue) {
        when {
            profileHue != null -> {
                if (darkTheme) {
                    darkColorSchemeFromHue(profileHue)
                } else {
                    lightColorSchemeFromHue(profileHue)
                }
            }

            darkTheme -> DarkColorScheme

            else -> LightColorScheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MoLeTypography,
        content = content
    )
}
