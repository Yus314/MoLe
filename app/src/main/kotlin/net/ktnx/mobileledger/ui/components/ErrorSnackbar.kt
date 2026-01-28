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

import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.ktnx.mobileledger.core.domain.model.AppError
import net.ktnx.mobileledger.core.ui.components.showAppError as coreShowAppError
import net.ktnx.mobileledger.core.ui.components.showError as coreShowError

// Re-export from core:ui for backward compatibility
// New code should import from net.ktnx.mobileledger.core.ui.components directly

@Composable
fun ErrorSnackbarHost(snackbarHostState: SnackbarHostState, modifier: Modifier = Modifier) =
    net.ktnx.mobileledger.core.ui.components.ErrorSnackbarHost(
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )

@Composable
fun ErrorSnackbar(snackbarData: SnackbarData, modifier: Modifier = Modifier) =
    net.ktnx.mobileledger.core.ui.components.ErrorSnackbar(
        snackbarData = snackbarData,
        modifier = modifier,
        dismissText = "閉じる"
    )

suspend fun SnackbarHostState.showError(
    message: String,
    actionLabel: String? = null,
    duration: SnackbarDuration = SnackbarDuration.Short
): SnackbarResult = coreShowError(message, actionLabel, duration)

suspend fun SnackbarHostState.showAppError(error: AppError, retryLabel: String = "再試行"): SnackbarResult =
    coreShowAppError(error, retryLabel)

@Composable
fun rememberErrorSnackbarState(): SnackbarHostState =
    net.ktnx.mobileledger.core.ui.components.rememberErrorSnackbarState()

@Composable
fun ErrorEffect(error: String?, snackbarHostState: SnackbarHostState, onErrorShown: () -> Unit = {}) =
    net.ktnx.mobileledger.core.ui.components.ErrorEffect(
        error = error,
        snackbarHostState = snackbarHostState,
        onErrorShown = onErrorShown
    )
