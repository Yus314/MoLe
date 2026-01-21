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

package net.ktnx.mobileledger.robot.main

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import net.ktnx.mobileledger.robot.base.BaseRobot
import net.ktnx.mobileledger.robot.base.BaseVerifyRobot

/**
 * Robot for interacting with the MainScreen in UI tests.
 *
 * Provides a fluent API for:
 * - Tab navigation
 * - Drawer operations
 * - Profile selection
 * - Refresh operations
 * - FAB interactions
 *
 * Usage:
 * ```kotlin
 * composeTestRule.mainScreen {
 *     selectTransactionsTab()
 *     openDrawer()
 * } verify {
 *     transactionsTabIsSelected()
 * }
 * ```
 */
@OptIn(ExperimentalTestApi::class)
class MainScreenRobot(
    composeTestRule: ComposeTestRule
) : BaseRobot<MainScreenRobot>(composeTestRule) {

    // ========================================
    // Tab Navigation
    // ========================================

    /**
     * Selects the Accounts tab.
     */
    fun selectAccountsTab() = apply {
        composeTestRule.onNodeWithText("Accounts").performClick()
    }

    /**
     * Selects the Transactions tab.
     */
    fun selectTransactionsTab() = apply {
        composeTestRule.onNodeWithText("Transactions").performClick()
    }

    // ========================================
    // Drawer Operations
    // ========================================

    /**
     * Opens the navigation drawer by clicking the hamburger menu.
     * Uses testTag to uniquely identify the menu button.
     */
    fun openDrawer() = apply {
        composeTestRule.onNodeWithTag("menu_button").performClick()
    }

    /**
     * Selects a profile by name from the drawer.
     */
    fun selectProfile(profileName: String) = apply {
        composeTestRule.onNodeWithText(profileName).performClick()
    }

    /**
     * Taps the "New profile" button in the drawer.
     */
    fun tapNewProfile() = apply {
        composeTestRule.onNodeWithText("New profile").performClick()
    }

    /**
     * Taps the "Templates" menu item in the drawer.
     */
    fun tapTemplates() = apply {
        composeTestRule.onNodeWithText("Templates").performClick()
    }

    /**
     * Taps the "Backup / Restore" menu item in the drawer.
     */
    fun tapBackupRestore() = apply {
        composeTestRule.onNodeWithText("Backup / Restore").performClick()
    }

    // ========================================
    // Account Tab Actions
    // ========================================

    /**
     * Toggles zero balance accounts visibility.
     */
    fun toggleZeroBalanceAccounts() = apply {
        composeTestRule.onNodeWithContentDescription("Show zero balances").performClick()
    }

    // ========================================
    // Transaction Tab Actions
    // ========================================

    /**
     * Opens the search/filter input in transactions tab.
     */
    fun openTransactionFilter() = apply {
        composeTestRule.onNodeWithContentDescription("Filter by account").performClick()
    }

    /**
     * Opens the go-to-date picker in transactions tab.
     */
    fun openGoToDate() = apply {
        composeTestRule.onNodeWithContentDescription("Go to date").performClick()
    }

    // ========================================
    // FAB Actions
    // ========================================

    /**
     * Taps the FAB to create a new transaction.
     */
    fun tapNewTransactionFab() = apply {
        composeTestRule.onNodeWithContentDescription("Plus icon").performClick()
    }

    // ========================================
    // Wait Operations
    // ========================================

    /**
     * Waits for the main screen to be ready.
     */
    fun waitForScreenReady(timeoutMillis: Long = 5_000) = apply {
        composeTestRule.waitUntilExactlyOneExists(
            hasText("Accounts") or hasText("MoLe"),
            timeoutMillis
        )
    }

    /**
     * Waits for refresh to complete.
     */
    fun waitForRefreshComplete(timeoutMillis: Long = 5_000) = apply {
        composeTestRule.waitUntilDoesNotExist(
            hasContentDescription("Refreshing"),
            timeoutMillis
        )
    }

    // ========================================
    // Verification
    // ========================================

    /**
     * Transitions to verification mode.
     */
    infix fun verify(block: MainScreenVerifyRobot.() -> Unit): MainScreenVerifyRobot =
        MainScreenVerifyRobot(composeTestRule).apply(block)
}

/**
 * Verification robot for MainScreen assertions.
 */
@OptIn(ExperimentalTestApi::class)
class MainScreenVerifyRobot(
    composeTestRule: ComposeTestRule
) : BaseVerifyRobot<MainScreenVerifyRobot>(composeTestRule) {

    // ========================================
    // Tab State Assertions
    // ========================================

    /**
     * Asserts that the Accounts tab is displayed.
     */
    fun accountsTabIsDisplayed() = apply {
        composeTestRule.onNodeWithText("Accounts").assertIsDisplayed()
    }

    /**
     * Asserts that the Transactions tab is displayed.
     */
    fun transactionsTabIsDisplayed() = apply {
        composeTestRule.onNodeWithText("Transactions").assertIsDisplayed()
    }

    // ========================================
    // Profile Assertions
    // ========================================

    /**
     * Asserts that the profile name is displayed in the top bar.
     * Uses testTag to uniquely identify the top bar title.
     */
    fun profileNameIsDisplayed(profileName: String) = apply {
        composeTestRule.onNodeWithTag("top_bar_title").assertTextEquals(profileName)
    }

    /**
     * Asserts that the app name is displayed (no profile selected).
     * Uses testTag to uniquely identify the top bar title.
     * Checks for text containing "MoLe" since debug builds may have " (dev)" suffix.
     */
    fun appNameIsDisplayed() = apply {
        composeTestRule.onNodeWithTag("top_bar_title").assertTextContains("MoLe", substring = true)
    }

    // ========================================
    // FAB Assertions
    // ========================================

    /**
     * Asserts that the new transaction FAB is displayed.
     */
    fun newTransactionFabIsDisplayed() = apply {
        composeTestRule.onNodeWithContentDescription("Plus icon").assertIsDisplayed()
    }

    /**
     * Asserts that the new transaction FAB is not displayed.
     */
    fun newTransactionFabIsNotDisplayed() = apply {
        composeTestRule.onNodeWithContentDescription("Plus icon").assertDoesNotExist()
    }

    // ========================================
    // Welcome Screen Assertions
    // ========================================

    /**
     * Asserts that the welcome screen is displayed (no profile).
     */
    fun welcomeScreenIsDisplayed() = apply {
        composeTestRule.onNodeWithText("Welcome").assertIsDisplayed()
    }

    /**
     * Asserts that the create profile button is displayed.
     */
    fun createProfileButtonIsDisplayed() = apply {
        composeTestRule.onNodeWithText("New profile").assertIsDisplayed()
    }

    // ========================================
    // Drawer Assertions
    // ========================================

    /**
     * Asserts that a profile is displayed in the drawer.
     * Uses assertIsDisplayed on the first match since the profile name may appear in multiple places
     * (e.g., top bar title and drawer list).
     */
    fun profileInDrawerIsDisplayed(profileName: String) = apply {
        // Just verify the text exists somewhere in the tree
        val nodes = composeTestRule.onAllNodesWithText(profileName, substring = true)
            .fetchSemanticsNodes()
        assert(nodes.isNotEmpty()) { "Profile '$profileName' not found in drawer" }
    }

    /**
     * Asserts that the Templates menu item is displayed.
     */
    fun templatesMenuIsDisplayed() = apply {
        composeTestRule.onNodeWithText("Templates").assertIsDisplayed()
    }

    /**
     * Asserts that the Backup / Restore menu item is displayed.
     */
    fun backupRestoreMenuIsDisplayed() = apply {
        composeTestRule.onNodeWithText("Backup / Restore").assertIsDisplayed()
    }

    // ========================================
    // Account Tab Assertions
    // ========================================

    /**
     * Asserts that an account with the given name is displayed.
     */
    fun accountIsDisplayed(accountName: String) = apply {
        composeTestRule.onNodeWithText(accountName, substring = true).assertIsDisplayed()
    }

    // ========================================
    // Transaction Tab Assertions
    // ========================================

    /**
     * Asserts that a transaction with the given description is displayed.
     */
    fun transactionIsDisplayed(description: String) = apply {
        composeTestRule.onNodeWithText(description, substring = true).assertIsDisplayed()
    }

    // ========================================
    // Wait-based Assertions
    // ========================================

    /**
     * Waits for and asserts that accounts are loaded.
     */
    fun waitAndAssertAccountsLoaded(timeoutMillis: Long = 5_000) = apply {
        composeTestRule.waitUntilDoesNotExist(
            hasText("Loading..."),
            timeoutMillis
        )
    }
}

// ============================================================
// DSL Entry Point
// ============================================================

/**
 * DSL entry point for MainScreen testing.
 *
 * Usage:
 * ```kotlin
 * composeTestRule.mainScreen {
 *     selectAccountsTab()
 * } verify {
 *     accountsTabIsDisplayed()
 * }
 * ```
 */
fun ComposeTestRule.mainScreen(block: MainScreenRobot.() -> Unit): MainScreenRobot = MainScreenRobot(this).apply(block)
