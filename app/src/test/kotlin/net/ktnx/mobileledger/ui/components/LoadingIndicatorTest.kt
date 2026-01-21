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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import net.ktnx.mobileledger.ui.theme.MoLeTheme
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric tests for LoadingIndicator and LoadingOverlay.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LoadingIndicatorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ========================================
    // LoadingIndicator tests
    // ========================================

    @Test
    fun `LoadingIndicator displays without message`() {
        composeTestRule.setContent {
            MoLeTheme {
                LoadingIndicator()
            }
        }

        // The indicator should render without errors
        // (CircularProgressIndicator doesn't have text to assert)
        composeTestRule.waitForIdle()
    }

    @Test
    fun `LoadingIndicator displays message when provided`() {
        composeTestRule.setContent {
            MoLeTheme {
                LoadingIndicator(message = "Loading...")
            }
        }

        composeTestRule.onNodeWithText("Loading...").assertIsDisplayed()
    }

    @Test
    fun `LoadingIndicator does not display message when null`() {
        composeTestRule.setContent {
            MoLeTheme {
                LoadingIndicator(message = null)
            }
        }

        // No text should be displayed
        assertFalse(
            "No message should be displayed",
            composeTestRule.onAllNodes(hasText("")).fetchSemanticsNodes().isNotEmpty()
        )
    }

    @Test
    fun `LoadingIndicator with Japanese message`() {
        composeTestRule.setContent {
            MoLeTheme {
                LoadingIndicator(message = "読み込み中...")
            }
        }

        composeTestRule.onNodeWithText("読み込み中...").assertIsDisplayed()
    }

    // ========================================
    // LoadingOverlay tests
    // ========================================

    @Test
    fun `LoadingOverlay shows content when not loading`() {
        composeTestRule.setContent {
            MoLeTheme {
                LoadingOverlay(
                    isLoading = false,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text("Content")
                }
            }
        }

        composeTestRule.onNodeWithText("Content").assertIsDisplayed()
    }

    @Test
    fun `LoadingOverlay shows loading indicator when loading`() {
        composeTestRule.setContent {
            MoLeTheme {
                LoadingOverlay(
                    isLoading = true,
                    message = "Please wait...",
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text("Content")
                }
            }
        }

        // Both content and loading message should be visible
        composeTestRule.onNodeWithText("Content").assertIsDisplayed()
        composeTestRule.onNodeWithText("Please wait...").assertIsDisplayed()
    }

    @Test
    fun `LoadingOverlay hides loading indicator when isLoading is false`() {
        composeTestRule.setContent {
            MoLeTheme {
                LoadingOverlay(
                    isLoading = false,
                    message = "Please wait...",
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text("Content")
                }
            }
        }

        // Content should be visible
        composeTestRule.onNodeWithText("Content").assertIsDisplayed()

        // Loading message should NOT be visible
        assertFalse(
            "Loading message should not be displayed when not loading",
            composeTestRule.onAllNodes(hasText("Please wait...")).fetchSemanticsNodes().isNotEmpty()
        )
    }

    @Test
    fun `LoadingOverlay without message shows just spinner`() {
        composeTestRule.setContent {
            MoLeTheme {
                LoadingOverlay(
                    isLoading = true,
                    message = null,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text("Content")
                }
            }
        }

        // Content should still be visible
        composeTestRule.onNodeWithText("Content").assertIsDisplayed()
    }
}
