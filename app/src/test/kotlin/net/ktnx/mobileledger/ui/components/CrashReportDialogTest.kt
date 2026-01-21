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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import net.ktnx.mobileledger.ui.theme.MoLeTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric tests for CrashReportDialog.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CrashReportDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testCrashReport = """
        MoLe version: 1.0.0
        OS version: 14; API level 34

        java.lang.NullPointerException: Test exception
            at com.example.Test.method(Test.kt:42)
    """.trimIndent()

    // ========================================
    // Dialog structure tests
    // ========================================

    @Test
    fun `CrashReportDialog displays title`() {
        composeTestRule.setContent {
            MoLeTheme {
                CrashReportDialog(
                    crashReportText = testCrashReport,
                    onDismiss = {}
                )
            }
        }

        // Title from strings.xml: "MoLe crashed"
        composeTestRule.onNodeWithText("MoLe crashed").assertIsDisplayed()
    }

    @Test
    fun `CrashReportDialog displays question message`() {
        composeTestRule.setContent {
            MoLeTheme {
                CrashReportDialog(
                    crashReportText = testCrashReport,
                    onDismiss = {}
                )
            }
        }

        // Question message from strings.xml (partial match)
        composeTestRule.onNodeWithText("Would you like to send", substring = true).assertIsDisplayed()
    }

    @Test
    fun `CrashReportDialog displays show report button`() {
        composeTestRule.setContent {
            MoLeTheme {
                CrashReportDialog(
                    crashReportText = testCrashReport,
                    onDismiss = {}
                )
            }
        }

        // "Show report" button from strings.xml
        composeTestRule.onNodeWithText("Show report").assertIsDisplayed()
    }

    @Test
    fun `CrashReportDialog displays send button`() {
        composeTestRule.setContent {
            MoLeTheme {
                CrashReportDialog(
                    crashReportText = testCrashReport,
                    onDismiss = {}
                )
            }
        }

        // "Send…" button from strings.xml
        composeTestRule.onNodeWithText("Send…").assertIsDisplayed()
    }

    @Test
    fun `CrashReportDialog displays not now button`() {
        composeTestRule.setContent {
            MoLeTheme {
                CrashReportDialog(
                    crashReportText = testCrashReport,
                    onDismiss = {}
                )
            }
        }

        // "Not now" button from strings.xml
        composeTestRule.onNodeWithText("Not now").assertIsDisplayed()
    }

    // ========================================
    // Interaction tests
    // ========================================

    @Test
    fun `show report button reveals crash report text`() {
        composeTestRule.setContent {
            MoLeTheme {
                CrashReportDialog(
                    crashReportText = testCrashReport,
                    onDismiss = {}
                )
            }
        }

        // Initially, crash report should not be visible
        composeTestRule.onNodeWithText("NullPointerException", substring = true).assertIsNotDisplayed()

        // Click show report button
        composeTestRule.onNodeWithText("Show report").performClick()

        // Wait for animation
        composeTestRule.waitForIdle()

        // Now crash report should be visible
        composeTestRule.onNodeWithText("NullPointerException", substring = true).assertIsDisplayed()
    }

    @Test
    fun `show report button hides after clicking`() {
        composeTestRule.setContent {
            MoLeTheme {
                CrashReportDialog(
                    crashReportText = testCrashReport,
                    onDismiss = {}
                )
            }
        }

        // Click show report button
        composeTestRule.onNodeWithText("Show report").performClick()

        // Wait for state update
        composeTestRule.waitForIdle()

        // Show report button should no longer exist
        composeTestRule.onNodeWithText("Show report").assertDoesNotExist()
    }

    @Test
    fun `not now button triggers dismiss callback`() {
        var dismissCalled = false

        composeTestRule.setContent {
            MoLeTheme {
                CrashReportDialog(
                    crashReportText = testCrashReport,
                    onDismiss = { dismissCalled = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Not now").performClick()

        assertTrue("Dismiss callback should be called", dismissCalled)
    }

    // ========================================
    // Content display tests
    // ========================================

    @Test
    fun `crash report shows version information after expand`() {
        composeTestRule.setContent {
            MoLeTheme {
                CrashReportDialog(
                    crashReportText = testCrashReport,
                    onDismiss = {}
                )
            }
        }

        // Expand the report
        composeTestRule.onNodeWithText("Show report").performClick()
        composeTestRule.waitForIdle()

        // Check version info is displayed
        composeTestRule.onNodeWithText("MoLe version: 1.0.0", substring = true).assertIsDisplayed()
    }

    @Test
    fun `crash report shows stack trace after expand`() {
        composeTestRule.setContent {
            MoLeTheme {
                CrashReportDialog(
                    crashReportText = testCrashReport,
                    onDismiss = {}
                )
            }
        }

        // Expand the report
        composeTestRule.onNodeWithText("Show report").performClick()
        composeTestRule.waitForIdle()

        // Check stack trace is displayed
        composeTestRule.onNodeWithText("Test.method", substring = true).assertIsDisplayed()
    }
}
