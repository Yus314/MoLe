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

package net.ktnx.mobileledger.robot.backups

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import net.ktnx.mobileledger.robot.base.BaseRobot
import net.ktnx.mobileledger.robot.base.BaseVerifyRobot

/**
 * Robot for interacting with the BackupsScreen in UI tests.
 *
 * Provides a fluent API for:
 * - Backup and restore button interactions
 * - Navigation operations
 *
 * Usage:
 * ```kotlin
 * composeTestRule.backupsScreen {
 *     tapBackupButton()
 * } verify {
 *     backupButtonIsEnabled()
 * }
 * ```
 */
@OptIn(ExperimentalTestApi::class)
class BackupsScreenRobot(
    composeTestRule: ComposeTestRule
) : BaseRobot<BackupsScreenRobot>(composeTestRule) {

    // ========================================
    // Action Methods
    // ========================================

    /**
     * Taps the backup button.
     * Uses hasClickAction to distinguish from header text.
     */
    fun tapBackupButton() = apply {
        composeTestRule.onNode(
            hasText("Backup") and hasClickAction()
        ).performClick()
    }

    /**
     * Taps the restore button.
     * Uses hasClickAction to distinguish from header text.
     */
    fun tapRestoreButton() = apply {
        composeTestRule.onNode(
            hasText("Restore") and hasClickAction()
        ).performClick()
    }

    /**
     * Taps the back navigation button.
     */
    fun tapBackButton() = apply {
        composeTestRule.onNodeWithContentDescription("Back").performClick()
    }

    // ========================================
    // Verification
    // ========================================

    /**
     * Transitions to verification mode.
     */
    infix fun verify(block: BackupsVerifyRobot.() -> Unit): BackupsVerifyRobot =
        BackupsVerifyRobot(composeTestRule).apply(block)
}

/**
 * Verification robot for BackupsScreen assertions.
 */
@OptIn(ExperimentalTestApi::class)
class BackupsVerifyRobot(
    composeTestRule: ComposeTestRule
) : BaseVerifyRobot<BackupsVerifyRobot>(composeTestRule) {

    // ========================================
    // Screen Structure
    // ========================================

    /**
     * Asserts that the screen title is displayed.
     */
    fun titleIsDisplayed() = apply {
        composeTestRule.onNodeWithText("Backup / Restore").assertIsDisplayed()
    }

    /**
     * Asserts that the back button is displayed.
     */
    fun backButtonIsDisplayed() = apply {
        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    // ========================================
    // Backup Section
    // ========================================

    /**
     * Asserts that the backup header is displayed.
     * Uses onAllNodesWithText since "Backup" appears in both header and button.
     */
    fun backupHeaderIsDisplayed() = apply {
        val nodes = composeTestRule.onAllNodesWithText("Backup").fetchSemanticsNodes()
        assert(nodes.isNotEmpty()) { "Backup header not found" }
    }

    /**
     * Asserts that the backup explanation text is displayed.
     */
    fun backupExplanationIsDisplayed() = apply {
        composeTestRule.onNodeWithText("Exports all profile configuration", substring = true)
            .assertIsDisplayed()
    }

    /**
     * Asserts that the backup button is enabled.
     */
    fun backupButtonIsEnabled() = apply {
        composeTestRule.onNode(
            hasText("Backup") and hasClickAction()
        ).assertIsEnabled()
    }

    /**
     * Asserts that the backup button is disabled.
     */
    fun backupButtonIsDisabled() = apply {
        composeTestRule.onNode(
            hasText("Backup") and hasClickAction()
        ).assertIsNotEnabled()
    }

    // ========================================
    // Restore Section
    // ========================================

    /**
     * Asserts that the restore header is displayed.
     * Uses onAllNodesWithText since "Restore" appears in both header and button.
     */
    fun restoreHeaderIsDisplayed() = apply {
        val nodes = composeTestRule.onAllNodesWithText("Restore").fetchSemanticsNodes()
        assert(nodes.isNotEmpty()) { "Restore header not found" }
    }

    /**
     * Asserts that the restore explanation text is displayed.
     */
    fun restoreExplanationIsDisplayed() = apply {
        composeTestRule.onNodeWithText("Restores all profiles and templates", substring = true)
            .assertIsDisplayed()
    }

    /**
     * Asserts that the restore button is enabled.
     */
    fun restoreButtonIsEnabled() = apply {
        composeTestRule.onNode(
            hasText("Restore") and hasClickAction()
        ).assertIsEnabled()
    }

    /**
     * Asserts that the restore button is disabled.
     */
    fun restoreButtonIsDisabled() = apply {
        composeTestRule.onNode(
            hasText("Restore") and hasClickAction()
        ).assertIsNotEnabled()
    }
}

// ============================================================
// DSL Entry Point
// ============================================================

/**
 * DSL entry point for BackupsScreen testing.
 *
 * Usage:
 * ```kotlin
 * composeTestRule.backupsScreen {
 *     tapBackupButton()
 * } verify {
 *     backupButtonIsEnabled()
 * }
 * ```
 */
fun ComposeTestRule.backupsScreen(block: BackupsScreenRobot.() -> Unit): BackupsScreenRobot =
    BackupsScreenRobot(this).apply(block)
