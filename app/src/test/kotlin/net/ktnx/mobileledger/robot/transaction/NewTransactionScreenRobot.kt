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

package net.ktnx.mobileledger.robot.transaction

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import net.ktnx.mobileledger.robot.base.BaseRobot
import net.ktnx.mobileledger.robot.base.BaseVerifyRobot

/**
 * Robot for interacting with the NewTransactionScreen in UI tests.
 *
 * Provides a fluent API for:
 * - Form field interactions (date, description, comment)
 * - Account row operations (add, remove, update)
 * - Currency selection
 * - Navigation and submission
 *
 * Usage:
 * ```kotlin
 * composeTestRule.newTransactionScreen {
 *     typeDescription("Grocery shopping")
 *     typeAccountName(0, "Expenses:Food")
 *     typeAmount(0, "50.00")
 *     typeAccountName(1, "Assets:Cash")
 *     tapSave()
 * } verify {
 *     transactionSavedMessageIsDisplayed()
 * }
 * ```
 */
@OptIn(ExperimentalTestApi::class)
class NewTransactionScreenRobot(
    composeTestRule: ComposeTestRule
) : BaseRobot<NewTransactionScreenRobot>(composeTestRule) {

    // ========================================
    // Form Field Interactions
    // ========================================

    /**
     * Types text into the description field.
     */
    fun typeDescription(text: String) = apply {
        composeTestRule.onNodeWithText("Description").performTextInput(text)
    }

    /**
     * Clears and replaces the description field text.
     */
    fun replaceDescription(text: String) = apply {
        composeTestRule.onNodeWithText("Description").performTextClearance()
        composeTestRule.onNodeWithText("Description").performTextInput(text)
    }

    /**
     * Clears the description field.
     */
    fun clearDescription() = apply {
        composeTestRule.onNodeWithText("Description").performTextClearance()
    }

    /**
     * Taps on the date field to open the date picker.
     */
    fun tapDateField() = apply {
        // The date is displayed as a clickable text, format: yyyy/MM/dd
        // We look for a pattern that matches date format
        composeTestRule.onAllNodesWithText(
            substring = true,
            text = "/"
        )[0].performClick()
    }

    // ========================================
    // Account Row Interactions
    // ========================================

    /**
     * Types an account name in the specified row (0-indexed).
     */
    fun typeAccountName(rowIndex: Int, accountName: String) = apply {
        val accountFields = composeTestRule.onAllNodesWithText("Account")
        accountFields[rowIndex].performTextInput(accountName)
    }

    /**
     * Clears and replaces the account name in the specified row.
     */
    fun replaceAccountName(rowIndex: Int, accountName: String) = apply {
        val accountFields = composeTestRule.onAllNodesWithText("Account")
        accountFields[rowIndex].performTextClearance()
        accountFields[rowIndex].performTextInput(accountName)
    }

    /**
     * Types an amount in the specified row (0-indexed).
     */
    fun typeAmount(rowIndex: Int, amount: String) = apply {
        val amountFields = composeTestRule.onAllNodesWithText("Amount")
        amountFields[rowIndex].performTextInput(amount)
    }

    /**
     * Clears and replaces the amount in the specified row.
     */
    fun replaceAmount(rowIndex: Int, amount: String) = apply {
        val amountFields = composeTestRule.onAllNodesWithText("Amount")
        amountFields[rowIndex].performTextClearance()
        amountFields[rowIndex].performTextInput(amount)
    }

    /**
     * Clears the amount in the specified row.
     */
    fun clearAmount(rowIndex: Int) = apply {
        val amountFields = composeTestRule.onAllNodesWithText("Amount")
        amountFields[rowIndex].performTextClearance()
    }

    /**
     * Taps the "Add account row" button.
     */
    fun tapAddAccountRow() = apply {
        composeTestRule.onNodeWithText("Add account row").performClick()
    }

    // ========================================
    // Navigation
    // ========================================

    /**
     * Taps the back button.
     */
    fun tapBack() = apply {
        composeTestRule.onNodeWithContentDescription("Back").performClick()
    }

    /**
     * Taps the save (FAB) button to submit the transaction.
     */
    fun tapSave() = apply {
        composeTestRule.onNodeWithContentDescription("Save transaction").performClick()
    }

    /**
     * Taps save and waits for the transaction saved message.
     */
    fun tapSaveAndWait(timeoutMillis: Long = 5_000) = apply {
        tapSave()
        composeTestRule.waitUntilExactlyOneExists(
            hasText("Transaction saved"),
            timeoutMillis
        )
    }

    // ========================================
    // Menu Interactions
    // ========================================

    /**
     * Opens the overflow menu.
     */
    fun openMenu() = apply {
        composeTestRule.onNodeWithContentDescription("More options").performClick()
    }

    /**
     * Selects "Show currency" or "Hide currency" from the menu.
     */
    fun toggleCurrencyFromMenu() = apply {
        openMenu()
        // Try both possible states
        try {
            composeTestRule.onNodeWithText("Show currency").performClick()
        } catch (_: AssertionError) {
            composeTestRule.onNodeWithText("Hide currency").performClick()
        }
    }

    /**
     * Selects "Use template" from the menu.
     */
    fun openTemplateSelector() = apply {
        openMenu()
        composeTestRule.onNodeWithText("Use template").performClick()
    }

    /**
     * Selects "Reset" from the menu.
     */
    fun resetFromMenu() = apply {
        openMenu()
        composeTestRule.onNodeWithText("Reset").performClick()
    }

    /**
     * Toggles "Simulate save" from the menu.
     */
    fun toggleSimulateSaveFromMenu() = apply {
        openMenu()
        try {
            composeTestRule.onNodeWithText("Enable simulate save").performClick()
        } catch (_: AssertionError) {
            composeTestRule.onNodeWithText("Disable simulate save").performClick()
        }
    }

    // ========================================
    // Dialog Interactions
    // ========================================

    /**
     * Confirms discarding changes in the discard dialog.
     */
    fun confirmDiscard() = apply {
        composeTestRule.onNodeWithText("Discard").performClick()
    }

    /**
     * Cancels discarding changes (keeps editing).
     */
    fun cancelDiscard() = apply {
        composeTestRule.onNodeWithText("Keep editing").performClick()
    }

    // ========================================
    // Wait Operations
    // ========================================

    /**
     * Waits for the screen to be ready (title visible).
     */
    fun waitForScreenReady(timeoutMillis: Long = 5_000) = apply {
        composeTestRule.waitUntilExactlyOneExists(
            hasText("New Transaction"),
            timeoutMillis
        )
    }

    /**
     * Waits for the loading indicator to disappear.
     */
    fun waitForLoadingComplete(timeoutMillis: Long = 5_000) = apply {
        composeTestRule.waitUntilDoesNotExist(
            hasContentDescription("Loading"),
            timeoutMillis
        )
    }

    // ========================================
    // Verification
    // ========================================

    /**
     * Transitions to verification mode.
     */
    infix fun verify(block: NewTransactionVerifyRobot.() -> Unit): NewTransactionVerifyRobot =
        NewTransactionVerifyRobot(composeTestRule).apply(block)
}

/**
 * Verification robot for NewTransactionScreen assertions.
 */
@OptIn(ExperimentalTestApi::class)
class NewTransactionVerifyRobot(
    composeTestRule: ComposeTestRule
) : BaseVerifyRobot<NewTransactionVerifyRobot>(composeTestRule) {

    // ========================================
    // Screen State Assertions
    // ========================================

    /**
     * Asserts that the New Transaction screen title is displayed.
     */
    fun screenTitleIsDisplayed() = apply {
        composeTestRule.onNodeWithText("New Transaction").assertIsDisplayed()
    }

    /**
     * Asserts that the save button (FAB) is displayed.
     */
    fun saveButtonIsDisplayed() = apply {
        composeTestRule.onNodeWithContentDescription("Save transaction").assertIsDisplayed()
    }

    /**
     * Asserts that the save button is enabled (full opacity).
     * Note: The FAB uses alpha animation, so we check if it's interactable.
     */
    fun saveButtonIsEnabled() = apply {
        composeTestRule.onNodeWithContentDescription("Save transaction").assertIsEnabled()
    }

    /**
     * Asserts that the save button is disabled (reduced opacity).
     */
    fun saveButtonIsDisabled() = apply {
        composeTestRule.onNodeWithContentDescription("Save transaction").assertIsNotEnabled()
    }

    // ========================================
    // Form Field Assertions
    // ========================================

    /**
     * Asserts that the description field is displayed.
     */
    fun descriptionFieldIsDisplayed() = apply {
        composeTestRule.onNodeWithText("Description").assertIsDisplayed()
    }

    /**
     * Asserts that the description field contains specific text.
     */
    fun descriptionContains(text: String) = apply {
        composeTestRule.onNodeWithText(text, substring = true).assertIsDisplayed()
    }

    // ========================================
    // Account Row Assertions
    // ========================================

    /**
     * Asserts the number of account rows displayed.
     */
    fun accountRowCountIs(expectedCount: Int) = apply {
        val accountFields = composeTestRule.onAllNodesWithText("Account").fetchSemanticsNodes()
        assert(accountFields.size == expectedCount) {
            "Expected $expectedCount account rows, but found ${accountFields.size}"
        }
    }

    /**
     * Asserts that the "Add account row" button is displayed.
     */
    fun addAccountRowButtonIsDisplayed() = apply {
        composeTestRule.onNodeWithText("Add account row").assertIsDisplayed()
    }

    // ========================================
    // Message Assertions
    // ========================================

    /**
     * Asserts that the "Transaction saved" snackbar is displayed.
     */
    fun transactionSavedMessageIsDisplayed() = apply {
        composeTestRule.onNodeWithText("Transaction saved").assertIsDisplayed()
    }

    /**
     * Asserts that an error message is displayed.
     */
    fun errorMessageIsDisplayed(message: String) = apply {
        composeTestRule.onNodeWithText(message).assertIsDisplayed()
    }

    // ========================================
    // Dialog Assertions
    // ========================================

    /**
     * Asserts that the discard changes dialog is displayed.
     */
    fun discardDialogIsDisplayed() = apply {
        composeTestRule.onNodeWithText("Discard changes?").assertIsDisplayed()
    }

    /**
     * Asserts that the discard changes dialog is not displayed.
     */
    fun discardDialogIsNotDisplayed() = apply {
        composeTestRule.onNodeWithText("Discard changes?").assertDoesNotExist()
    }

    /**
     * Asserts that the date picker dialog is displayed.
     */
    fun datePickerIsDisplayed() = apply {
        // Material3 DatePicker typically has "Select date" or similar text
        composeTestRule.onNodeWithText("Select date").assertIsDisplayed()
    }

    // ========================================
    // Wait-based Assertions
    // ========================================

    /**
     * Waits for and asserts the transaction saved message.
     */
    fun waitAndAssertTransactionSaved(timeoutMillis: Long = 5_000) = apply {
        composeTestRule.waitUntilExactlyOneExists(
            hasText("Transaction saved"),
            timeoutMillis
        )
    }

    /**
     * Waits for and asserts that the screen has been reset.
     */
    fun waitAndAssertFormReset(timeoutMillis: Long = 5_000) = apply {
        // After reset, description should be empty
        composeTestRule.waitUntilExactlyOneExists(
            hasText("Description"),
            timeoutMillis
        )
    }
}

// ============================================================
// DSL Entry Point
// ============================================================

/**
 * DSL entry point for NewTransactionScreen testing.
 *
 * Usage:
 * ```kotlin
 * composeTestRule.newTransactionScreen {
 *     typeDescription("Grocery")
 *     tapSave()
 * } verify {
 *     transactionSavedMessageIsDisplayed()
 * }
 * ```
 */
fun ComposeTestRule.newTransactionScreen(block: NewTransactionScreenRobot.() -> Unit): NewTransactionScreenRobot =
    NewTransactionScreenRobot(this).apply(block)
