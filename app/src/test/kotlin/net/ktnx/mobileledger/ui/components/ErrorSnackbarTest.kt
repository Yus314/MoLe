/*
 * Copyright © 2026 Damyan Ivanov.
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

package net.ktnx.mobileledger.ui.components

import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import net.ktnx.mobileledger.core.ui.components.ErrorEffect
import net.ktnx.mobileledger.core.ui.components.ErrorSnackbar
import net.ktnx.mobileledger.core.ui.components.ErrorSnackbarHost
import net.ktnx.mobileledger.core.ui.components.rememberErrorSnackbarState
import net.ktnx.mobileledger.ui.theme.MoLeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric tests for ErrorSnackbar components.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ErrorSnackbarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ========================================
    // ErrorSnackbar composable tests
    // ========================================

    @Test
    fun `ErrorSnackbar displays error message`() {
        val testMessage = "接続エラーが発生しました"

        composeTestRule.setContent {
            MoLeTheme {
                ErrorSnackbar(
                    snackbarData = createSnackbarData(
                        message = testMessage,
                        actionLabel = null,
                        withDismissAction = false
                    )
                )
            }
        }

        composeTestRule.onNodeWithText(testMessage).assertIsDisplayed()
    }

    @Test
    fun `ErrorSnackbar shows action button when actionLabel is provided`() {
        val actionLabel = "再試行"
        var actionPerformed = false

        composeTestRule.setContent {
            MoLeTheme {
                ErrorSnackbar(
                    snackbarData = createSnackbarData(
                        message = "エラー",
                        actionLabel = actionLabel,
                        withDismissAction = false,
                        onAction = { actionPerformed = true }
                    )
                )
            }
        }

        composeTestRule.onNodeWithText(actionLabel).assertIsDisplayed()
        composeTestRule.onNodeWithText(actionLabel).performClick()
        assertTrue("Action should be performed", actionPerformed)
    }

    @Test
    fun `ErrorSnackbar shows dismiss button when withDismissAction is true`() {
        var dismissed = false

        composeTestRule.setContent {
            MoLeTheme {
                ErrorSnackbar(
                    snackbarData = createSnackbarData(
                        message = "エラー",
                        actionLabel = null,
                        withDismissAction = true,
                        onDismiss = { dismissed = true }
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("閉じる").assertIsDisplayed()
        composeTestRule.onNodeWithText("閉じる").performClick()
        assertTrue("Snackbar should be dismissed", dismissed)
    }

    @Test
    fun `ErrorSnackbar shows both action and dismiss buttons`() {
        composeTestRule.setContent {
            MoLeTheme {
                ErrorSnackbar(
                    snackbarData = createSnackbarData(
                        message = "エラーメッセージ",
                        actionLabel = "再試行",
                        withDismissAction = true
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("エラーメッセージ").assertIsDisplayed()
        composeTestRule.onNodeWithText("再試行").assertIsDisplayed()
        composeTestRule.onNodeWithText("閉じる").assertIsDisplayed()
    }

    // ========================================
    // ErrorSnackbarHost tests
    // ========================================

    @Test
    fun `ErrorSnackbarHost renders without error`() {
        val snackbarHostState = SnackbarHostState()

        composeTestRule.setContent {
            MoLeTheme {
                ErrorSnackbarHost(snackbarHostState = snackbarHostState)
            }
        }

        // Just verify it renders without crashing
        composeTestRule.waitForIdle()
    }

    // ========================================
    // rememberErrorSnackbarState tests
    // ========================================

    @Test
    fun `rememberErrorSnackbarState returns SnackbarHostState`() {
        var state: SnackbarHostState? = null

        composeTestRule.setContent {
            MoLeTheme {
                state = rememberErrorSnackbarState()
            }
        }

        composeTestRule.waitForIdle()
        assertTrue("State should not be null", state != null)
    }

    // ========================================
    // ErrorEffect tests
    // ========================================

    @Test
    fun `ErrorEffect does not show snackbar when error is null`() {
        val snackbarHostState = SnackbarHostState()
        var errorShownCallbackCalled = false

        composeTestRule.setContent {
            MoLeTheme {
                ErrorEffect(
                    error = null,
                    snackbarHostState = snackbarHostState,
                    onErrorShown = { errorShownCallbackCalled = true }
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(100)
        composeTestRule.waitForIdle()

        assertTrue("onErrorShown callback should NOT be called", !errorShownCallbackCalled)
    }

    // ========================================
    // Helper functions
    // ========================================

    private fun createSnackbarData(
        message: String,
        actionLabel: String?,
        withDismissAction: Boolean,
        onAction: () -> Unit = {},
        onDismiss: () -> Unit = {}
    ): SnackbarData = object : SnackbarData {
        override val visuals: SnackbarVisuals = object : SnackbarVisuals {
            override val actionLabel: String? = actionLabel
            override val duration: SnackbarDuration = SnackbarDuration.Short
            override val message: String = message
            override val withDismissAction: Boolean = withDismissAction
        }

        override fun dismiss() {
            onDismiss()
        }

        override fun performAction() {
            onAction()
        }
    }
}
