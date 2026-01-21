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

package net.ktnx.mobileledger.robot.base

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText

/**
 * Base class for verification/assertion robots in Compose UI tests.
 *
 * Provides common assertion functionality while separating verification
 * logic from interaction logic (in BaseRobot).
 *
 * @param T The concrete VerifyRobot type for method chaining
 */
@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalTestApi::class)
abstract class BaseVerifyRobot<T : BaseVerifyRobot<T>>(
    protected val composeTestRule: ComposeTestRule
) {
    /**
     * Default timeout for wait operations in milliseconds.
     */
    protected open val defaultTimeout: Long = 5_000

    /**
     * Returns this instance as the concrete type for method chaining.
     */
    protected fun self(): T = this as T

    // ========================================
    // Node Finders
    // ========================================

    /**
     * Finds a node by its displayed text.
     */
    protected fun nodeWithText(text: String): SemanticsNodeInteraction = composeTestRule.onNodeWithText(text)

    /**
     * Finds a node by its content description.
     */
    protected fun nodeWithContentDescription(description: String): SemanticsNodeInteraction =
        composeTestRule.onNodeWithContentDescription(description)

    /**
     * Finds a node by its test tag.
     */
    protected fun nodeWithTag(tag: String): SemanticsNodeInteraction = composeTestRule.onNodeWithTag(tag)

    // ========================================
    // Visibility Assertions
    // ========================================

    /**
     * Asserts that a node with the specified text is displayed.
     * Preferred assertion - verifies what users see.
     */
    fun textIsDisplayed(text: String): T = apply {
        nodeWithText(text).assertIsDisplayed()
    } as T

    /**
     * Asserts that a node with the specified text is NOT displayed.
     */
    fun textIsNotDisplayed(text: String): T = apply {
        nodeWithText(text).assertIsNotDisplayed()
    } as T

    /**
     * Asserts that a node with the specified text does not exist in the tree.
     */
    fun textDoesNotExist(text: String): T = apply {
        nodeWithText(text).assertDoesNotExist()
    } as T

    /**
     * Asserts that a node with the specified content description is displayed.
     */
    fun contentDescriptionIsDisplayed(description: String): T = apply {
        nodeWithContentDescription(description).assertIsDisplayed()
    } as T

    /**
     * Asserts that a node with the specified content description does not exist.
     */
    fun contentDescriptionDoesNotExist(description: String): T = apply {
        nodeWithContentDescription(description).assertDoesNotExist()
    } as T

    /**
     * Asserts that a node with the specified tag is displayed.
     */
    fun tagIsDisplayed(tag: String): T = apply {
        nodeWithTag(tag).assertIsDisplayed()
    } as T

    /**
     * Asserts that a node with the specified tag does not exist.
     */
    fun tagDoesNotExist(tag: String): T = apply {
        nodeWithTag(tag).assertDoesNotExist()
    } as T

    // ========================================
    // Enabled State Assertions
    // ========================================

    /**
     * Asserts that a node with the specified text is enabled.
     */
    fun textIsEnabled(text: String): T = apply {
        nodeWithText(text).assertIsEnabled()
    } as T

    /**
     * Asserts that a node with the specified text is disabled.
     */
    fun textIsDisabled(text: String): T = apply {
        nodeWithText(text).assertIsNotEnabled()
    } as T

    /**
     * Asserts that a node with the specified content description is enabled.
     */
    fun contentDescriptionIsEnabled(description: String): T = apply {
        nodeWithContentDescription(description).assertIsEnabled()
    } as T

    /**
     * Asserts that a node with the specified content description is disabled.
     */
    fun contentDescriptionIsDisabled(description: String): T = apply {
        nodeWithContentDescription(description).assertIsNotEnabled()
    } as T

    // ========================================
    // Text Content Assertions
    // ========================================

    /**
     * Asserts that a node with the specified tag has the exact text.
     */
    fun tagHasText(tag: String, expectedText: String): T = apply {
        nodeWithTag(tag).assertTextEquals(expectedText)
    } as T

    /**
     * Asserts that a node with the specified tag contains the text.
     */
    fun tagContainsText(tag: String, expectedSubstring: String): T = apply {
        nodeWithTag(tag).assertTextContains(expectedSubstring)
    } as T

    // ========================================
    // Count Assertions
    // ========================================

    /**
     * Asserts that the number of nodes with the specified tag equals the expected count.
     */
    fun tagCountEquals(tag: String, expectedCount: Int): T = apply {
        val actualCount = composeTestRule.onAllNodesWithTag(tag).fetchSemanticsNodes().size
        assert(actualCount == expectedCount) {
            "Expected $expectedCount nodes with tag '$tag', but found $actualCount"
        }
    } as T

    /**
     * Asserts that at least one node with the specified tag exists.
     */
    fun tagExists(tag: String): T = apply {
        val count = composeTestRule.onAllNodesWithTag(tag).fetchSemanticsNodes().size
        assert(count > 0) {
            "Expected at least one node with tag '$tag', but found none"
        }
    } as T

    // ========================================
    // Wait-based Assertions
    // ========================================

    /**
     * Waits for and asserts that a node with the specified text is displayed.
     */
    fun waitAndAssertTextIsDisplayed(text: String, timeoutMillis: Long = defaultTimeout): T = apply {
        composeTestRule.waitUntilExactlyOneExists(hasText(text), timeoutMillis)
        nodeWithText(text).assertIsDisplayed()
    } as T

    /**
     * Waits for and asserts that a node with the specified text no longer exists.
     */
    fun waitAndAssertTextDoesNotExist(text: String, timeoutMillis: Long = defaultTimeout): T = apply {
        composeTestRule.waitUntilDoesNotExist(hasText(text), timeoutMillis)
    } as T

    /**
     * Waits for and asserts that a node with the specified content description is displayed.
     */
    fun waitAndAssertContentDescriptionIsDisplayed(description: String, timeoutMillis: Long = defaultTimeout): T =
        apply {
            composeTestRule.waitUntilExactlyOneExists(hasContentDescription(description), timeoutMillis)
            nodeWithContentDescription(description).assertIsDisplayed()
        } as T

    /**
     * Waits for and asserts that a node with the specified content description no longer exists.
     */
    fun waitAndAssertContentDescriptionDoesNotExist(description: String, timeoutMillis: Long = defaultTimeout): T =
        apply {
            composeTestRule.waitUntilDoesNotExist(hasContentDescription(description), timeoutMillis)
        } as T

    // ========================================
    // Custom Assertions
    // ========================================

    /**
     * Asserts using a custom semantic matcher.
     */
    fun assertNodeExists(matcher: SemanticsMatcher): T = apply {
        composeTestRule.onNode(matcher).assertExists()
    } as T

    /**
     * Asserts that a node matching the matcher is displayed.
     */
    fun assertNodeIsDisplayed(matcher: SemanticsMatcher): T = apply {
        composeTestRule.onNode(matcher).assertIsDisplayed()
    } as T
}
