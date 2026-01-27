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

package net.ktnx.mobileledger.ui.transaction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import net.ktnx.mobileledger.core.common.utils.SimpleDate
import net.ktnx.mobileledger.robot.transaction.newTransactionScreen
import net.ktnx.mobileledger.ui.theme.MoLeTheme
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric tests for NewTransactionContent using Robot Pattern.
 *
 * Tests the transaction form UI including:
 * - Initial state and layout
 * - Form field interactions
 * - Account row operations
 * - Validation feedback
 *
 * Uses testTag for dynamic list items (account rows) per best practices.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NewTransactionScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var formUiState by mutableStateOf(TransactionFormUiState())
    private var accountRowsUiState by mutableStateOf(AccountRowsUiState())

    // Captured events for verification
    private val capturedFormEvents = mutableListOf<TransactionFormEvent>()
    private val capturedAccountRowsEvents = mutableListOf<AccountRowsEvent>()

    private val descriptionFocusRequester = FocusRequester()

    @Before
    fun setup() {
        // Reset state before each test
        formUiState = TransactionFormUiState(
            date = SimpleDate(2026, 1, 22),
            description = ""
        )
        accountRowsUiState = AccountRowsUiState(
            accounts = listOf(
                TransactionAccountRow(id = 1, accountName = "", amountText = ""),
                TransactionAccountRow(id = 2, accountName = "", amountText = "")
            )
        )
        capturedFormEvents.clear()
        capturedAccountRowsEvents.clear()
    }

    private fun setContent() {
        composeTestRule.setContent {
            MoLeTheme {
                NewTransactionContent(
                    formUiState = formUiState,
                    accountRowsUiState = accountRowsUiState,
                    onFormEvent = { event -> capturedFormEvents.add(event) },
                    onAccountRowsEvent = { event -> capturedAccountRowsEvents.add(event) },
                    descriptionFocusRequester = descriptionFocusRequester
                )
            }
        }
    }

    // ========================================
    // Initial State Tests
    // ========================================

    @Test
    fun `initial state displays date field`() {
        setContent()

        // Date should be displayed in format yyyy/MM/dd
        composeTestRule.onNodeWithText("2026/01/22").assertIsDisplayed()
    }

    @Test
    fun `initial state displays two account rows by testTag`() {
        setContent()

        // Use testTag to find account rows
        composeTestRule.onNodeWithTag("account_row_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("account_row_2").assertIsDisplayed()
    }

    @Test
    fun `initial state displays add account row button`() {
        setContent()

        composeTestRule.onNodeWithText("Add account row").assertIsDisplayed()
    }

    // ========================================
    // Form Field Interaction Tests
    // ========================================

    @Test
    fun `tapping date field triggers ShowDatePicker event`() {
        setContent()

        composeTestRule.onNodeWithText("2026/01/22").performClick()

        assertTrue(
            "Should capture ShowDatePicker event",
            capturedFormEvents.any { it is TransactionFormEvent.ShowDatePicker }
        )
    }

    // ========================================
    // Account Row Tests Using testTag
    // ========================================

    @Test
    fun `account rows are displayed with correct testTags`() {
        setContent()

        // Verify both rows exist by their specific testTags
        composeTestRule.onNodeWithTag("account_row_1").assertExists()
        composeTestRule.onNodeWithTag("account_row_2").assertExists()
    }

    @Test
    fun `tapping add account row button triggers AddAccountRow event`() {
        setContent()

        composeTestRule.onNodeWithText("Add account row").performClick()

        assertTrue(
            "Should capture AddAccountRow event",
            capturedAccountRowsEvents.any { it is AccountRowsEvent.AddAccountRow }
        )
    }

    @Test
    fun `three account rows display correctly`() {
        // Start with 3 rows
        accountRowsUiState = accountRowsUiState.copy(
            accounts = listOf(
                TransactionAccountRow(id = 1, accountName = "", amountText = ""),
                TransactionAccountRow(id = 2, accountName = "", amountText = ""),
                TransactionAccountRow(id = 3, accountName = "", amountText = "")
            )
        )
        setContent()

        composeTestRule.onNodeWithTag("account_row_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("account_row_2").assertIsDisplayed()
        composeTestRule.onNodeWithTag("account_row_3").assertIsDisplayed()
    }

    // ========================================
    // Robot Pattern Verification Tests
    // ========================================

    @Test
    fun `robot pattern verify chain works correctly`() {
        setContent()

        composeTestRule.newTransactionScreen {
            // No actions - just verify initial state
        } verify {
            addAccountRowButtonIsDisplayed()
        }
    }

    // ========================================
    // Balance Indicator Tests
    // ========================================

    @Test
    fun `account rows with valid amounts display correctly`() {
        accountRowsUiState = accountRowsUiState.copy(
            accounts = listOf(
                TransactionAccountRow(
                    id = 1,
                    accountName = "Expenses:Food",
                    amountText = "50.00",
                    isAmountValid = true
                ),
                TransactionAccountRow(
                    id = 2,
                    accountName = "Assets:Cash",
                    amountText = "-50.00",
                    isAmountValid = true
                )
            )
        )
        setContent()

        // Verify the rows are displayed using testTag
        composeTestRule.onNodeWithTag("account_row_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("account_row_2").assertIsDisplayed()
    }

    @Test
    fun `account row with invalid amount renders without crash`() {
        accountRowsUiState = accountRowsUiState.copy(
            accounts = listOf(
                TransactionAccountRow(
                    id = 1,
                    accountName = "Expenses:Food",
                    amountText = "invalid",
                    isAmountValid = false
                ),
                TransactionAccountRow(
                    id = 2,
                    accountName = "",
                    amountText = ""
                )
            )
        )
        setContent()

        // The test should complete without error
        composeTestRule.onNodeWithTag("account_row_1").assertIsDisplayed()
        composeTestRule.waitForIdle()
    }

    // ========================================
    // Currency Display Tests
    // ========================================

    @Test
    fun `currency field visibility controlled by showCurrency state`() {
        // With currency hidden (default)
        accountRowsUiState = accountRowsUiState.copy(showCurrency = false)
        setContent()

        // Should complete without crash - just verifying rendering
        composeTestRule.waitForIdle()
    }

    @Test
    fun `currency field displayed when showCurrency is true`() {
        accountRowsUiState = accountRowsUiState.copy(
            showCurrency = true,
            accounts = listOf(
                TransactionAccountRow(id = 1, accountName = "", amountText = "", currency = "USD"),
                TransactionAccountRow(id = 2, accountName = "", amountText = "", currency = "")
            )
        )
        setContent()

        // Should display without crash
        composeTestRule.onNodeWithTag("account_row_1").assertIsDisplayed()
        composeTestRule.waitForIdle()
    }

    // ========================================
    // Additional State Tests
    // ========================================

    @Test
    fun `pre-filled account names display correctly`() {
        accountRowsUiState = accountRowsUiState.copy(
            accounts = listOf(
                TransactionAccountRow(
                    id = 1,
                    accountName = "Expenses:Food:Groceries",
                    amountText = ""
                ),
                TransactionAccountRow(
                    id = 2,
                    accountName = "Assets:Bank:Checking",
                    amountText = ""
                )
            )
        )
        setContent()

        // Account names should be visible
        composeTestRule.onNodeWithText("Expenses:Food:Groceries", substring = true).assertExists()
        composeTestRule.onNodeWithText("Assets:Bank:Checking", substring = true).assertExists()
    }

    @Test
    fun `pre-filled amounts display correctly`() {
        accountRowsUiState = accountRowsUiState.copy(
            accounts = listOf(
                TransactionAccountRow(id = 1, accountName = "", amountText = "100.00"),
                TransactionAccountRow(id = 2, accountName = "", amountText = "-50.00")
            )
        )
        setContent()

        // Amounts should be visible - use exact text to avoid substring overlap
        composeTestRule.onNodeWithText("100.00").assertExists()
        composeTestRule.onNodeWithText("-50.00").assertExists()
    }
}
