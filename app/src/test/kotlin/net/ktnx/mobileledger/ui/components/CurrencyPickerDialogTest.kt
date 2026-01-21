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
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isNotSelected
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import net.ktnx.mobileledger.domain.model.CurrencyPosition
import net.ktnx.mobileledger.ui.theme.MoLeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric tests for CurrencyPickerDialog.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CurrencyPickerDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ========================================
    // Currency grid tests
    // ========================================

    @Test
    fun `CurrencyPickerDialog displays all currencies in grid`() {
        composeTestRule.setContent {
            MoLeTheme {
                CurrencyPickerDialog(
                    currencies = listOf("USD", "EUR", "JPY"),
                    onCurrencySelected = {},
                    onCurrencyAdded = { _, _, _ -> },
                    onCurrencyDeleted = {},
                    onNoCurrencySelected = {},
                    onPositionChanged = {},
                    onGapChanged = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("USD").assertIsDisplayed()
        composeTestRule.onNodeWithText("EUR").assertIsDisplayed()
        composeTestRule.onNodeWithText("JPY").assertIsDisplayed()
    }

    @Test
    fun `CurrencyPickerDialog shows empty message when no currencies`() {
        composeTestRule.setContent {
            MoLeTheme {
                CurrencyPickerDialog(
                    currencies = emptyList(),
                    onCurrencySelected = {},
                    onCurrencyAdded = { _, _, _ -> },
                    onCurrencyDeleted = {},
                    onNoCurrencySelected = {},
                    onPositionChanged = {},
                    onGapChanged = {},
                    onDismiss = {}
                )
            }
        }

        // The "no currencies" message should be displayed
        composeTestRule.onNodeWithText("No currencies defined").assertIsDisplayed()
    }

    @Test
    fun `currency selection triggers callback with selected name`() {
        var selectedCurrency: String? = null

        composeTestRule.setContent {
            MoLeTheme {
                CurrencyPickerDialog(
                    currencies = listOf("USD", "EUR", "JPY"),
                    onCurrencySelected = { selectedCurrency = it },
                    onCurrencyAdded = { _, _, _ -> },
                    onCurrencyDeleted = {},
                    onNoCurrencySelected = {},
                    onPositionChanged = {},
                    onGapChanged = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("EUR").performClick()
        assertEquals("EUR", selectedCurrency)
    }

    // ========================================
    // Add currency section tests
    // ========================================

    @Test
    fun `Add button shows add currency section`() {
        composeTestRule.setContent {
            MoLeTheme {
                CurrencyPickerDialog(
                    currencies = listOf("USD"),
                    onCurrencySelected = {},
                    onCurrencyAdded = { _, _, _ -> },
                    onCurrencyDeleted = {},
                    onNoCurrencySelected = {},
                    onPositionChanged = {},
                    onGapChanged = {},
                    onDismiss = {}
                )
            }
        }

        // Click add button
        composeTestRule.onNodeWithText("Add…").performClick()

        // Input field should be visible (label text)
        composeTestRule.onNodeWithText("currency/commodity").assertIsDisplayed()
    }

    @Test
    fun `add currency input accepts text and triggers callback`() {
        var addedCurrency: String? = null
        var addedPosition: CurrencyPosition? = null
        var addedGap: Boolean? = null

        composeTestRule.setContent {
            MoLeTheme {
                CurrencyPickerDialog(
                    currencies = listOf("USD"),
                    initialPosition = CurrencyPosition.BEFORE,
                    initialGap = true,
                    onCurrencySelected = {},
                    onCurrencyAdded = { name, position, gap ->
                        addedCurrency = name
                        addedPosition = position
                        addedGap = gap
                    },
                    onCurrencyDeleted = {},
                    onNoCurrencySelected = {},
                    onPositionChanged = {},
                    onGapChanged = {},
                    onDismiss = {}
                )
            }
        }

        // Click add button
        composeTestRule.onNodeWithText("Add…").performClick()

        // Enter new currency name (use the text field label)
        composeTestRule.onNodeWithText("currency/commodity").performTextInput("GBP")

        // Confirm add
        composeTestRule.onNodeWithText("OK").performClick()

        assertEquals("GBP", addedCurrency)
        assertEquals(CurrencyPosition.BEFORE, addedPosition)
        assertEquals(true, addedGap)
    }

    @Test
    fun `confirm add with blank name is disabled`() {
        composeTestRule.setContent {
            MoLeTheme {
                CurrencyPickerDialog(
                    currencies = listOf("USD"),
                    onCurrencySelected = {},
                    onCurrencyAdded = { _, _, _ -> },
                    onCurrencyDeleted = {},
                    onNoCurrencySelected = {},
                    onPositionChanged = {},
                    onGapChanged = {},
                    onDismiss = {}
                )
            }
        }

        // Click add button
        composeTestRule.onNodeWithText("Add…").performClick()

        // OK button should be disabled when input is empty
        composeTestRule.onNodeWithText("OK").assertIsNotEnabled()
    }

    @Test
    fun `cancel add returns to main view`() {
        composeTestRule.setContent {
            MoLeTheme {
                CurrencyPickerDialog(
                    currencies = listOf("USD", "EUR"),
                    onCurrencySelected = {},
                    onCurrencyAdded = { _, _, _ -> },
                    onCurrencyDeleted = {},
                    onNoCurrencySelected = {},
                    onPositionChanged = {},
                    onGapChanged = {},
                    onDismiss = {}
                )
            }
        }

        // Click add button
        composeTestRule.onNodeWithText("Add…").performClick()

        // There are 2 Cancel buttons - click the first one (in AddCurrencySection)
        composeTestRule.onAllNodesWithText("Cancel")[0].performClick()

        // Add button should be visible again
        composeTestRule.onNodeWithText("Add…").assertIsDisplayed()
    }

    // ========================================
    // Position settings tests
    // ========================================

    @Test
    fun `position radio buttons are displayed when showPositionSettings is true`() {
        composeTestRule.setContent {
            MoLeTheme {
                CurrencyPickerDialog(
                    currencies = listOf("USD"),
                    showPositionSettings = true,
                    onCurrencySelected = {},
                    onCurrencyAdded = { _, _, _ -> },
                    onCurrencyDeleted = {},
                    onNoCurrencySelected = {},
                    onPositionChanged = {},
                    onGapChanged = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Left").assertIsDisplayed()
        composeTestRule.onNodeWithText("Right").assertIsDisplayed()
    }

    @Test
    fun `position settings hidden when showPositionSettings is false`() {
        composeTestRule.setContent {
            MoLeTheme {
                CurrencyPickerDialog(
                    currencies = listOf("USD"),
                    showPositionSettings = false,
                    onCurrencySelected = {},
                    onCurrencyAdded = { _, _, _ -> },
                    onCurrencyDeleted = {},
                    onNoCurrencySelected = {},
                    onPositionChanged = {},
                    onGapChanged = {},
                    onDismiss = {}
                )
            }
        }

        // Position settings should not be visible (check that they don't exist)
        assertFalse(
            "Left should not exist",
            composeTestRule.onAllNodes(
                androidx.compose.ui.test.hasText("Left")
            ).fetchSemanticsNodes().isNotEmpty()
        )
        assertFalse(
            "Right should not exist",
            composeTestRule.onAllNodes(
                androidx.compose.ui.test.hasText("Right")
            ).fetchSemanticsNodes().isNotEmpty()
        )
    }

    @Test
    fun `position change triggers callback`() {
        var newPosition: CurrencyPosition? = null

        composeTestRule.setContent {
            MoLeTheme {
                CurrencyPickerDialog(
                    currencies = listOf("USD"),
                    initialPosition = CurrencyPosition.BEFORE,
                    showPositionSettings = true,
                    onCurrencySelected = {},
                    onCurrencyAdded = { _, _, _ -> },
                    onCurrencyDeleted = {},
                    onNoCurrencySelected = {},
                    onPositionChanged = { newPosition = it },
                    onGapChanged = {},
                    onDismiss = {}
                )
            }
        }

        // Click the unselected RadioButton (for "Right" position)
        // There are 2 RadioButtons - the first is selected (BEFORE), click the second
        composeTestRule.onAllNodes(isSelectable() and isNotSelected())[0].performClick()
        assertEquals(CurrencyPosition.AFTER, newPosition)
    }

    @Test
    fun `gap switch change triggers callback`() {
        var gapValue: Boolean? = null

        composeTestRule.setContent {
            MoLeTheme {
                CurrencyPickerDialog(
                    currencies = listOf("USD"),
                    initialGap = true,
                    showPositionSettings = true,
                    onCurrencySelected = {},
                    onCurrencyAdded = { _, _, _ -> },
                    onCurrencyDeleted = {},
                    onNoCurrencySelected = {},
                    onPositionChanged = {},
                    onGapChanged = { gapValue = it },
                    onDismiss = {}
                )
            }
        }

        // Click the Switch directly (isToggleable finds Switch components)
        composeTestRule.onAllNodes(isToggleable())[0].performClick()
        assertEquals("Gap should change to false when toggled", false, gapValue)
    }

    // ========================================
    // No currency button tests
    // ========================================

    @Test
    fun `no currency button triggers callback`() {
        var noCurrencyClicked = false

        composeTestRule.setContent {
            MoLeTheme {
                CurrencyPickerDialog(
                    currencies = listOf("USD"),
                    onCurrencySelected = {},
                    onCurrencyAdded = { _, _, _ -> },
                    onCurrencyDeleted = {},
                    onNoCurrencySelected = { noCurrencyClicked = true },
                    onPositionChanged = {},
                    onGapChanged = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("none").performClick()
        assertTrue("No currency callback should be triggered", noCurrencyClicked)
    }

    // ========================================
    // Dialog title tests
    // ========================================

    @Test
    fun `dialog displays currency selection title`() {
        composeTestRule.setContent {
            MoLeTheme {
                CurrencyPickerDialog(
                    currencies = listOf("USD"),
                    showPositionSettings = false, // Hide position settings to avoid duplicate "Currency" text
                    onCurrencySelected = {},
                    onCurrencyAdded = { _, _, _ -> },
                    onCurrencyDeleted = {},
                    onNoCurrencySelected = {},
                    onPositionChanged = {},
                    onGapChanged = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Currency").assertIsDisplayed()
    }
}
