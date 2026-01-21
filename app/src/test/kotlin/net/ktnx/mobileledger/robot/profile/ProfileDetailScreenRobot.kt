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

package net.ktnx.mobileledger.robot.profile

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
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
 * Robot for interacting with the ProfileDetailScreen in UI tests.
 *
 * Provides a fluent API for:
 * - Profile name and URL input
 * - Authentication settings
 * - Theme color selection
 * - Save/delete operations
 *
 * Usage:
 * ```kotlin
 * composeTestRule.profileDetailScreen {
 *     typeName("My Profile")
 *     typeUrl("https://example.com")
 * } verify {
 *     nameFieldContains("My Profile")
 * }
 * ```
 */
@OptIn(ExperimentalTestApi::class)
class ProfileDetailScreenRobot(
    composeTestRule: ComposeTestRule
) : BaseRobot<ProfileDetailScreenRobot>(composeTestRule) {

    // ========================================
    // Name Field Operations
    // ========================================

    /**
     * Types text into the profile name field.
     */
    fun typeName(name: String) = apply {
        composeTestRule.onNodeWithText("Profile name").performTextInput(name)
    }

    /**
     * Clears and replaces text in the profile name field.
     */
    fun replaceName(name: String) = apply {
        composeTestRule.onNodeWithText("Profile name").performTextClearance()
        composeTestRule.onNodeWithText("Profile name").performTextInput(name)
    }

    // ========================================
    // URL Field Operations
    // ========================================

    /**
     * Types text into the URL field.
     */
    fun typeUrl(url: String) = apply {
        composeTestRule.onNodeWithText("URL").performTextInput(url)
    }

    /**
     * Clears and replaces text in the URL field.
     */
    fun replaceUrl(url: String) = apply {
        composeTestRule.onNodeWithText("URL").performTextClearance()
        composeTestRule.onNodeWithText("URL").performTextInput(url)
    }

    // ========================================
    // Authentication Operations
    // ========================================

    /**
     * Scrolls to and toggles the authentication switch.
     */
    fun toggleAuthentication() = apply {
        composeTestRule.onNodeWithText("Use HTTP authentication").performScrollTo()
        composeTestRule.onNodeWithText("Use HTTP authentication").performClick()
    }

    /**
     * Types text into the username field.
     */
    fun typeUsername(username: String) = apply {
        composeTestRule.onNodeWithText("Username").performScrollTo()
        composeTestRule.onNodeWithText("Username").performTextInput(username)
    }

    /**
     * Types text into the password field.
     */
    fun typePassword(password: String) = apply {
        composeTestRule.onNodeWithText("Password").performScrollTo()
        composeTestRule.onNodeWithText("Password").performTextInput(password)
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

    /**
     * Taps the test connection button.
     */
    fun tapTestConnection() = apply {
        composeTestRule.onNodeWithText("Test connection").performScrollTo()
        composeTestRule.onNodeWithText("Test connection").performClick()
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
        // Japanese "キャンセル" (Cancel)
        composeTestRule.onNodeWithText("Cancel").performClick()
    }

    /**
     * Saves changes in the unsaved changes dialog.
     */
    fun saveChangesInDialog() = apply {
        composeTestRule.onNodeWithText("保存").performClick()
    }

    /**
     * Discards changes in the unsaved changes dialog.
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
            hasText("Profile name"),
            timeoutMillis
        )
    }

    // ========================================
    // Verification
    // ========================================

    /**
     * Transitions to verification mode.
     */
    infix fun verify(block: ProfileDetailVerifyRobot.() -> Unit): ProfileDetailVerifyRobot =
        ProfileDetailVerifyRobot(composeTestRule).apply(block)
}

/**
 * Verification robot for ProfileDetailScreen assertions.
 */
@OptIn(ExperimentalTestApi::class)
class ProfileDetailVerifyRobot(
    composeTestRule: ComposeTestRule
) : BaseVerifyRobot<ProfileDetailVerifyRobot>(composeTestRule) {

    // ========================================
    // Title Assertions
    // ========================================

    /**
     * Asserts that the screen title shows "New profile".
     */
    fun newProfileTitle() = apply {
        composeTestRule.onNodeWithText("New profile").assertIsDisplayed()
    }

    /**
     * Asserts that the screen title shows the profile name.
     */
    fun profileTitleIs(name: String) = apply {
        val nodes = composeTestRule.onAllNodesWithText(name, substring = true)
            .fetchSemanticsNodes()
        assert(nodes.isNotEmpty()) { "Profile name '$name' not found in UI" }
    }

    // ========================================
    // Field Assertions
    // ========================================

    /**
     * Asserts that the name field contains the specified text.
     */
    fun nameFieldContains(text: String) = apply {
        composeTestRule.onNodeWithText("Profile name").assertTextContains(text)
    }

    /**
     * Asserts that the URL field contains the specified text.
     */
    fun urlFieldContains(text: String) = apply {
        composeTestRule.onNodeWithText("URL").assertTextContains(text)
    }

    /**
     * Asserts that the name field is displayed.
     */
    fun nameFieldIsDisplayed() = apply {
        composeTestRule.onNodeWithText("Profile name").assertIsDisplayed()
    }

    /**
     * Asserts that the URL field is displayed.
     */
    fun urlFieldIsDisplayed() = apply {
        composeTestRule.onNodeWithText("URL").assertIsDisplayed()
    }

    // ========================================
    // Authentication Assertions
    // ========================================

    /**
     * Asserts that the authentication toggle is displayed.
     */
    fun authenticationToggleIsDisplayed() = apply {
        composeTestRule.onNodeWithText("Use HTTP authentication").performScrollTo()
        composeTestRule.onNodeWithText("Use HTTP authentication").assertIsDisplayed()
    }

    /**
     * Asserts that the username field is displayed.
     */
    fun usernameFieldIsDisplayed() = apply {
        composeTestRule.onNodeWithText("Username").performScrollTo()
        composeTestRule.onNodeWithText("Username").assertIsDisplayed()
    }

    /**
     * Asserts that the password field is displayed.
     */
    fun passwordFieldIsDisplayed() = apply {
        composeTestRule.onNodeWithText("Password").performScrollTo()
        composeTestRule.onNodeWithText("Password").assertIsDisplayed()
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
     * Asserts that the delete button is not displayed (new profile).
     */
    fun deleteButtonIsNotDisplayed() = apply {
        val nodes = composeTestRule.onAllNodesWithText("削除").fetchSemanticsNodes()
        assert(nodes.isEmpty()) { "Delete button should not be displayed for new profile" }
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
     */
    fun unsavedChangesDialogIsDisplayed() = apply {
        composeTestRule.onNodeWithText("未保存の変更").assertIsDisplayed()
    }

    // ========================================
    // Validation Assertions
    // ========================================

    /**
     * Asserts that an error message is displayed.
     */
    fun errorIsDisplayed(message: String) = apply {
        composeTestRule.onNodeWithText(message, substring = true).assertIsDisplayed()
    }
}

// ============================================================
// DSL Entry Point
// ============================================================

/**
 * DSL entry point for ProfileDetailScreen testing.
 *
 * Usage:
 * ```kotlin
 * composeTestRule.profileDetailScreen {
 *     typeName("My Profile")
 *     typeUrl("https://example.com")
 * } verify {
 *     nameFieldContains("My Profile")
 * }
 * ```
 */
fun ComposeTestRule.profileDetailScreen(block: ProfileDetailScreenRobot.() -> Unit): ProfileDetailScreenRobot =
    ProfileDetailScreenRobot(this).apply(block)
