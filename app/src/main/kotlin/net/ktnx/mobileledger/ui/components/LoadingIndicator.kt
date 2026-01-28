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

package net.ktnx.mobileledger.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Re-export from core:ui for backward compatibility
// New code should import from net.ktnx.mobileledger.core.ui.components directly

@Composable
fun LoadingIndicator(modifier: Modifier = Modifier, message: String? = null) =
    net.ktnx.mobileledger.core.ui.components.LoadingIndicator(
        modifier = modifier,
        message = message
    )

@Composable
fun LoadingOverlay(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    message: String? = null,
    content: @Composable () -> Unit
) = net.ktnx.mobileledger.core.ui.components.LoadingOverlay(
    isLoading = isLoading,
    modifier = modifier,
    message = message,
    content = content
)
