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
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import net.ktnx.mobileledger.core.common.utils.SimpleDate
import net.ktnx.mobileledger.core.domain.model.FutureDates
import net.ktnx.mobileledger.ui.theme.MoLeTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric tests for MoleDatePickerDialog.
 *
 * Note: Material3 DatePicker internal calendar selection cannot be fully tested
 * with Robolectric. These tests focus on dialog structure and callbacks.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DatePickerDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ========================================
    // Dialog structure tests
    // ========================================

    @Test
    fun `MoleDatePickerDialog displays OK and Cancel buttons`() {
        composeTestRule.setContent {
            MoLeTheme {
                MoleDatePickerDialog(
                    initialDate = SimpleDate(2026, 1, 21),
                    onDateSelected = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("OK").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun `Cancel button triggers onDismiss callback`() {
        var dismissCalled = false

        composeTestRule.setContent {
            MoLeTheme {
                MoleDatePickerDialog(
                    initialDate = SimpleDate(2026, 1, 21),
                    onDateSelected = {},
                    onDismiss = { dismissCalled = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Cancel").performClick()
        assertTrue("Dismiss callback should be triggered", dismissCalled)
    }

    @Test
    fun `OK button triggers onDateSelected with initial date`() {
        var selectedDate: SimpleDate? = null

        composeTestRule.setContent {
            MoLeTheme {
                MoleDatePickerDialog(
                    initialDate = SimpleDate(2026, 1, 21),
                    onDateSelected = { selectedDate = it },
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("OK").performClick()
        assertTrue("Date selection callback should be triggered", selectedDate != null)
    }

    // ========================================
    // FutureDates parameter tests
    // ========================================

    @Test
    fun `dialog accepts FutureDates None parameter`() {
        composeTestRule.setContent {
            MoLeTheme {
                MoleDatePickerDialog(
                    initialDate = SimpleDate(2026, 1, 21),
                    futureDates = FutureDates.None,
                    onDateSelected = {},
                    onDismiss = {}
                )
            }
        }

        // Dialog should display without error
        composeTestRule.onNodeWithText("OK").assertIsDisplayed()
    }

    @Test
    fun `dialog accepts FutureDates OneWeek parameter`() {
        composeTestRule.setContent {
            MoLeTheme {
                MoleDatePickerDialog(
                    initialDate = SimpleDate(2026, 1, 21),
                    futureDates = FutureDates.OneWeek,
                    onDateSelected = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("OK").assertIsDisplayed()
    }

    @Test
    fun `dialog accepts FutureDates All parameter`() {
        composeTestRule.setContent {
            MoLeTheme {
                MoleDatePickerDialog(
                    initialDate = SimpleDate(2026, 1, 21),
                    futureDates = FutureDates.All,
                    onDateSelected = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("OK").assertIsDisplayed()
    }

    // ========================================
    // Min/Max date parameter tests
    // ========================================

    @Test
    fun `dialog accepts minDate parameter`() {
        composeTestRule.setContent {
            MoLeTheme {
                MoleDatePickerDialog(
                    initialDate = SimpleDate(2026, 1, 21),
                    minDate = SimpleDate(2026, 1, 1),
                    onDateSelected = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("OK").assertIsDisplayed()
    }

    @Test
    fun `dialog accepts maxDate parameter`() {
        composeTestRule.setContent {
            MoLeTheme {
                MoleDatePickerDialog(
                    initialDate = SimpleDate(2026, 1, 21),
                    maxDate = SimpleDate(2026, 12, 31),
                    onDateSelected = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("OK").assertIsDisplayed()
    }

    @Test
    fun `dialog accepts both minDate and maxDate parameters`() {
        composeTestRule.setContent {
            MoLeTheme {
                MoleDatePickerDialog(
                    initialDate = SimpleDate(2026, 6, 15),
                    minDate = SimpleDate(2026, 1, 1),
                    maxDate = SimpleDate(2026, 12, 31),
                    onDateSelected = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("OK").assertIsDisplayed()
    }

    @Test
    fun `dialog combines maxDate with futureDates setting`() {
        composeTestRule.setContent {
            MoLeTheme {
                MoleDatePickerDialog(
                    initialDate = SimpleDate(2026, 1, 21),
                    maxDate = SimpleDate(2026, 12, 31),
                    futureDates = FutureDates.OneMonth,
                    onDateSelected = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("OK").assertIsDisplayed()
    }
}
