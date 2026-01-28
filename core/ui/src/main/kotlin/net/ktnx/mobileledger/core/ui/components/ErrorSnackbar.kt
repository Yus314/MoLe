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

package net.ktnx.mobileledger.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import net.ktnx.mobileledger.core.domain.model.AppError
import net.ktnx.mobileledger.core.ui.theme.MoLeTheme

@Composable
fun ErrorSnackbarHost(snackbarHostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = modifier
    ) { snackbarData ->
        ErrorSnackbar(snackbarData = snackbarData)
    }
}

@Composable
fun ErrorSnackbar(snackbarData: SnackbarData, modifier: Modifier = Modifier, dismissText: String = "閉じる") {
    Snackbar(
        modifier = modifier,
        action = snackbarData.visuals.actionLabel?.let { actionLabel ->
            {
                TextButton(onClick = { snackbarData.performAction() }) {
                    Text(
                        text = actionLabel,
                        color = MaterialTheme.colorScheme.inversePrimary
                    )
                }
            }
        },
        dismissAction = if (snackbarData.visuals.withDismissAction) {
            {
                TextButton(onClick = { snackbarData.dismiss() }) {
                    Text(
                        text = dismissText,
                        color = MaterialTheme.colorScheme.inversePrimary
                    )
                }
            }
        } else {
            null
        },
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    ) {
        Text(text = snackbarData.visuals.message)
    }
}

suspend fun SnackbarHostState.showError(
    message: String,
    actionLabel: String? = null,
    duration: SnackbarDuration = SnackbarDuration.Short
): SnackbarResult = showSnackbar(
    message = message,
    actionLabel = actionLabel,
    duration = duration,
    withDismissAction = true
)

/**
 * Extension function to display an AppError.
 *
 * Automatically shows retry button based on isRetryable flag.
 *
 * @param error The AppError to display
 * @param retryLabel Label for the retry button (default: "Retry")
 * @return SnackbarResult (ActionPerformed if retry was pressed)
 */
suspend fun SnackbarHostState.showAppError(error: AppError, retryLabel: String = "再試行"): SnackbarResult = showSnackbar(
    message = error.message,
    actionLabel = if (error.isRetryable) retryLabel else null,
    duration = SnackbarDuration.Long,
    withDismissAction = true
)

@Composable
fun rememberErrorSnackbarState(): SnackbarHostState = remember { SnackbarHostState() }

@Composable
fun ErrorEffect(error: String?, snackbarHostState: SnackbarHostState, onErrorShown: () -> Unit = {}) {
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showError(it)
            onErrorShown()
        }
    }
}

@Preview
@Composable
private fun ErrorSnackbarPreview() {
    MoLeTheme {
        ErrorSnackbar(
            snackbarData = object : SnackbarData {
                override val visuals: SnackbarVisuals = object : SnackbarVisuals {
                    override val actionLabel: String = "Retry"
                    override val duration: SnackbarDuration = SnackbarDuration.Short
                    override val message: String = "Connection error occurred"
                    override val withDismissAction: Boolean = true
                }
                override fun dismiss() {
                    // No-op: Preview mock
                }
                override fun performAction() {
                    // No-op: Preview mock
                }
            }
        )
    }
}
