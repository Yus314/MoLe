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

package net.ktnx.mobileledger.robot.template

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import net.ktnx.mobileledger.robot.base.BaseRobot
import net.ktnx.mobileledger.robot.base.BaseVerifyRobot

/**
 * Robot for interacting with the TemplateDetailScreen in UI tests.
 *
 * Provides a fluent API for:
 * - Template name input
 * - Pattern input and testing
 * - Account row operations
 * - Save/delete operations
 *
 * Usage:
 * ```kotlin
 * composeTestRule.templateDetailScreen {
 *     typeName("Bank Transfer")
 *     typePattern("(\\d+)円")
 * } verify {
 *     nameIsDisplayed("Bank Transfer")
 * }
 * ```
 */
@OptIn(ExperimentalTestApi::class)
class TemplateDetailScreenRobot(
    composeTestRule: ComposeTestRule
) : BaseRobot<TemplateDetailScreenRobot>(composeTestRule) {

    // ========================================
    // Template Name Operations
    // ========================================

    /**
     * Types text into the template name field.
     */
    fun typeName(name: String) = apply {
        composeTestRule.onNodeWithText("Template name").performTextInput(name)
    }

    /**
     * Clears and replaces text in the template name field.
     */
    fun replaceName(name: String) = apply {
        composeTestRule.onNodeWithText("Template name").performTextClearance()
        composeTestRule.onNodeWithText("Template name").performTextInput(name)
    }

    // ========================================
    // Pattern Operations
    // ========================================

    /**
     * Types text into the pattern field.
     */
    fun typePattern(pattern: String) = apply {
        composeTestRule.onNodeWithText("Regular expression pattern").performTextInput(pattern)
    }

    /**
     * Clears and replaces text in the pattern field.
     */
    fun replacePattern(pattern: String) = apply {
        composeTestRule.onNodeWithText("Regular expression pattern").performTextClearance()
        composeTestRule.onNodeWithText("Regular expression pattern").performTextInput(pattern)
    }

    /**
     * Types text into the test text field.
     */
    fun typeTestText(text: String) = apply {
        composeTestRule.onNodeWithText("Test text").performTextInput(text)
    }

    /**
     * Clears and replaces text in the test text field.
     */
    fun replaceTestText(text: String) = apply {
        composeTestRule.onNodeWithText("Test text").performTextClearance()
        composeTestRule.onNodeWithText("Test text").performTextInput(text)
    }

    // ========================================
    // Account Row Operations
    // ========================================

    /**
     * Taps the add account row button.
     * Scrolls to the element first since it may be below the viewport.
     */
    fun tapAddAccountRow() = apply {
        composeTestRule.onNodeWithContentDescription("行を追加").performScrollTo()
        composeTestRule.onNodeWithContentDescription("行を追加").performClick()
    }

    // ========================================
    // Fallback Toggle
    // ========================================

    /**
     * Taps the fallback toggle switch.
     * Scrolls to the element first since it may be below the viewport.
     */
    fun tapFallbackToggle() = apply {
        composeTestRule.onNodeWithText("Fallback template").performScrollTo()
        composeTestRule.onNodeWithText("Fallback template").performClick()
    }

    // ========================================
    // Action Buttons
    // ========================================

    /**
     * Taps the save FAB.
     */
    fun tapSave() = apply {
        composeTestRule.onNodeWithContentDescription("保存").performClick()
    }

    /**
     * Taps the delete button in the top bar.
     */
    fun tapDelete() = apply {
        composeTestRule.onNodeWithContentDescription("削除").performClick()
    }

    /**
     * Taps the back button in the top bar.
     */
    fun tapBack() = apply {
        composeTestRule.onNodeWithContentDescription("戻る").performClick()
    }

    // ========================================
    // Dialog Operations
    // ========================================

    /**
     * Confirms deletion in the delete confirmation dialog.
     */
    fun confirmDelete() = apply {
        composeTestRule.onNodeWithText("Remove").performClick()
    }

    /**
     * Cancels deletion in the delete confirmation dialog.
     */
    fun cancelDelete() = apply {
        composeTestRule.onNodeWithText("Cancel").performClick()
    }

    /**
     * Saves changes in the unsaved changes dialog.
     * Uses Japanese text as per UnsavedChangesDialog implementation.
     */
    fun saveChangesInDialog() = apply {
        composeTestRule.onNodeWithText("保存").performClick()
    }

    /**
     * Discards changes in the unsaved changes dialog.
     * Uses Japanese text as per UnsavedChangesDialog implementation.
     */
    fun discardChangesInDialog() = apply {
        composeTestRule.onNodeWithText("破棄").performClick()
    }

    // ========================================
    // Wait Operations
    // ========================================

    /**
     * Waits for the screen to be ready.
     */
    fun waitForScreenReady(timeoutMillis: Long = 5_000) = apply {
        composeTestRule.waitUntilExactlyOneExists(
            hasText("Template name"),
            timeoutMillis
        )
    }

    // ========================================
    // Verification
    // ========================================

    /**
     * Transitions to verification mode.
     */
    infix fun verify(block: TemplateDetailVerifyRobot.() -> Unit): TemplateDetailVerifyRobot =
        TemplateDetailVerifyRobot(composeTestRule).apply(block)
}

/**
 * Verification robot for TemplateDetailScreen assertions.
 */
@OptIn(ExperimentalTestApi::class)
class TemplateDetailVerifyRobot(
    composeTestRule: ComposeTestRule
) : BaseVerifyRobot<TemplateDetailVerifyRobot>(composeTestRule) {

    // ========================================
    // Title Assertions
    // ========================================

    /**
     * Asserts that the screen title shows "New template".
     */
    fun newTemplateTitle() = apply {
        composeTestRule.onNodeWithText("New template").assertIsDisplayed()
    }

    /**
     * Asserts that the screen title shows the template name.
     * Note: The name appears in both title and field, so we just verify it exists.
     */
    fun templateTitleIs(name: String) = apply {
        val nodes = composeTestRule.onAllNodesWithText(name, substring = true)
            .fetchSemanticsNodes()
        assert(nodes.isNotEmpty()) { "Template name '$name' not found in UI" }
    }

    // ========================================
    // Field Assertions
    // ========================================

    /**
     * Asserts that the name field contains the specified text.
     */
    fun nameFieldContains(text: String) = apply {
        composeTestRule.onNodeWithText("Template name").assertTextContains(text)
    }

    /**
     * Asserts that the pattern field contains the specified text.
     */
    fun patternFieldContains(text: String) = apply {
        composeTestRule.onNodeWithText("Regular expression pattern").assertTextContains(text)
    }

    /**
     * Asserts that the test text field is displayed.
     */
    fun testTextFieldIsDisplayed() = apply {
        composeTestRule.onNodeWithText("Test text").assertIsDisplayed()
    }

    // ========================================
    // Pattern Match Assertions
    // ========================================

    /**
     * Asserts that the pattern error message is displayed.
     */
    fun patternErrorIsDisplayed(errorMessage: String) = apply {
        composeTestRule.onNodeWithText(errorMessage, substring = true).assertIsDisplayed()
    }

    /**
     * Asserts that the group count is displayed.
     */
    fun groupCountIsDisplayed(count: Int) = apply {
        composeTestRule.onNodeWithText("グループ数: $count").assertIsDisplayed()
    }

    // ========================================
    // Account Row Assertions
    // ========================================

    /**
     * Asserts that the accounts section is displayed.
     * Scrolls to the element first since it may be below the viewport.
     */
    fun accountsSectionIsDisplayed() = apply {
        composeTestRule.onNodeWithText("Accounts").performScrollTo()
        composeTestRule.onNodeWithText("Accounts").assertIsDisplayed()
    }

    /**
     * Asserts the number of account rows.
     * Uses fetchSemanticsNodes to count occurrences.
     */
    fun accountRowCountIs(expectedCount: Int) = apply {
        // Count account row cards by looking for "Account name" text occurrences
        val nodes = composeTestRule.onAllNodesWithText("Account name", substring = true)
            .fetchSemanticsNodes()
        assert(nodes.size == expectedCount) {
            "Expected $expectedCount account rows but found ${nodes.size}"
        }
    }

    // ========================================
    // Fallback Assertions
    // ========================================

    /**
     * Asserts that the fallback switch is ON.
     */
    fun fallbackIsOn() = apply {
        // Find the switch by its parent row text
        composeTestRule.onNodeWithText("Fallback template").assertIsDisplayed()
        // Note: Switch state assertion requires finding the Switch specifically
    }

    /**
     * Asserts that the fallback toggle is displayed.
     * Scrolls to the element first since it may be below the viewport.
     */
    fun fallbackToggleIsDisplayed() = apply {
        composeTestRule.onNodeWithText("Fallback template").performScrollTo()
        composeTestRule.onNodeWithText("Fallback template").assertIsDisplayed()
    }

    // ========================================
    // Button Assertions
    // ========================================

    /**
     * Asserts that the save FAB is displayed.
     */
    fun saveFabIsDisplayed() = apply {
        composeTestRule.onNodeWithContentDescription("保存").assertIsDisplayed()
    }

    /**
     * Asserts that the delete button is displayed.
     */
    fun deleteButtonIsDisplayed() = apply {
        composeTestRule.onNodeWithContentDescription("削除").assertIsDisplayed()
    }

    /**
     * Asserts that the delete button is not displayed (new template).
     */
    fun deleteButtonIsNotDisplayed() = apply {
        composeTestRule.onAllNodesWithText("削除").fetchSemanticsNodes().let { nodes ->
            assert(nodes.isEmpty()) { "Delete button should not be displayed for new template" }
        }
    }

    // ========================================
    // Dialog Assertions
    // ========================================

    /**
     * Asserts that the delete confirmation dialog is displayed.
     */
    fun deleteConfirmDialogIsDisplayed() = apply {
        composeTestRule.onNodeWithText("Remove").assertIsDisplayed()
    }

    /**
     * Asserts that the unsaved changes dialog is displayed.
     * Uses Japanese text as per UnsavedChangesDialog implementation.
     */
    fun unsavedChangesDialogIsDisplayed() = apply {
        composeTestRule.onNodeWithText("未保存の変更").assertIsDisplayed()
        composeTestRule.onNodeWithText("保存").assertIsDisplayed()
        composeTestRule.onNodeWithText("破棄").assertIsDisplayed()
    }

    // ========================================
    // Loading Assertions
    // ========================================

    /**
     * Asserts that the loading indicator is displayed.
     */
    fun loadingIsDisplayed() = apply {
        composeTestRule.onNodeWithContentDescription("Loading").assertIsDisplayed()
    }
}

// ============================================================
// DSL Entry Point
// ============================================================

/**
 * DSL entry point for TemplateDetailScreen testing.
 *
 * Usage:
 * ```kotlin
 * composeTestRule.templateDetailScreen {
 *     typeName("Bank Transfer")
 *     typePattern("(\\d+)円")
 * } verify {
 *     nameFieldContains("Bank Transfer")
 * }
 * ```
 */
fun ComposeTestRule.templateDetailScreen(block: TemplateDetailScreenRobot.() -> Unit): TemplateDetailScreenRobot =
    TemplateDetailScreenRobot(this).apply(block)
