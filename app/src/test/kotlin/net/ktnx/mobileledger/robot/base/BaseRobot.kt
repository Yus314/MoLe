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

import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput

/**
 * Base class for Robot Pattern implementation in Compose UI tests.
 *
 * Provides common functionality for interacting with Compose UI elements
 * while encapsulating implementation details from test logic.
 *
 * @param T The concrete Robot type for method chaining
 */
@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalTestApi::class)
abstract class BaseRobot<T : BaseRobot<T>>(
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
    // Node Finders (Priority: Text > ContentDescription > Tag)
    // ========================================

    /**
     * Finds a node by its displayed text.
     * Preferred finder - matches user-visible content.
     */
    protected fun nodeWithText(text: String): SemanticsNodeInteraction = composeTestRule.onNodeWithText(text)

    /**
     * Finds a node by its content description.
     * Use for icons and accessibility-labeled elements.
     */
    protected fun nodeWithContentDescription(description: String): SemanticsNodeInteraction =
        composeTestRule.onNodeWithContentDescription(description)

    /**
     * Finds a node by its test tag.
     * Use only for dynamic lists or structural containers.
     */
    protected fun nodeWithTag(tag: String): SemanticsNodeInteraction = composeTestRule.onNodeWithTag(tag)

    // ========================================
    // Common Actions
    // ========================================

    /**
     * Clicks on a node with the specified text.
     */
    protected fun clickOnText(text: String): T = apply {
        nodeWithText(text).performClick()
    } as T

    /**
     * Clicks on a node with the specified content description.
     */
    protected fun clickOnContentDescription(description: String): T = apply {
        nodeWithContentDescription(description).performClick()
    } as T

    /**
     * Clicks on a node with the specified test tag.
     */
    protected fun clickOnTag(tag: String): T = apply {
        nodeWithTag(tag).performClick()
    } as T

    /**
     * Types text into a field identified by its label text.
     */
    protected fun typeIntoFieldWithText(fieldLabel: String, text: String): T = apply {
        nodeWithText(fieldLabel).performTextInput(text)
    } as T

    /**
     * Clears text from a field identified by its label text.
     */
    protected fun clearFieldWithText(fieldLabel: String): T = apply {
        nodeWithText(fieldLabel).performTextClearance()
    } as T

    /**
     * Types text into a field identified by its test tag.
     */
    protected fun typeIntoFieldWithTag(tag: String, text: String): T = apply {
        nodeWithTag(tag).performTextInput(text)
    } as T

    /**
     * Clears and then types text into a field.
     */
    protected fun replaceTextInFieldWithText(fieldLabel: String, newText: String): T = apply {
        nodeWithText(fieldLabel).performTextClearance()
        nodeWithText(fieldLabel).performTextInput(newText)
    } as T

    /**
     * Scrolls to a node with the specified text.
     */
    protected fun scrollToText(text: String): T = apply {
        nodeWithText(text).performScrollTo()
    } as T

    /**
     * Scrolls to a node with the specified test tag.
     */
    protected fun scrollToTag(tag: String): T = apply {
        nodeWithTag(tag).performScrollTo()
    } as T

    // ========================================
    // Synchronization (No Thread.sleep!)
    // ========================================

    /**
     * Waits until a node matching the given matcher exists.
     * Use for async state changes.
     */
    protected fun waitForNode(matcher: SemanticsMatcher, timeoutMillis: Long = defaultTimeout): T = apply {
        composeTestRule.waitUntilExactlyOneExists(matcher, timeoutMillis)
    } as T

    /**
     * Waits until a node with the specified text exists.
     */
    protected fun waitForText(text: String, timeoutMillis: Long = defaultTimeout): T = apply {
        composeTestRule.waitUntilExactlyOneExists(hasText(text), timeoutMillis)
    } as T

    /**
     * Waits until a node with the specified content description exists.
     */
    protected fun waitForContentDescription(description: String, timeoutMillis: Long = defaultTimeout): T = apply {
        composeTestRule.waitUntilExactlyOneExists(hasContentDescription(description), timeoutMillis)
    } as T

    /**
     * Waits until a node matching the given matcher no longer exists.
     */
    protected fun waitUntilGone(matcher: SemanticsMatcher, timeoutMillis: Long = defaultTimeout): T = apply {
        composeTestRule.waitUntilDoesNotExist(matcher, timeoutMillis)
    } as T

    /**
     * Waits until a node with the specified text no longer exists.
     */
    protected fun waitUntilTextGone(text: String, timeoutMillis: Long = defaultTimeout): T = apply {
        composeTestRule.waitUntilDoesNotExist(hasText(text), timeoutMillis)
    } as T

    /**
     * Waits until a node with the specified content description no longer exists.
     */
    protected fun waitUntilContentDescriptionGone(description: String, timeoutMillis: Long = defaultTimeout): T =
        apply {
            composeTestRule.waitUntilDoesNotExist(hasContentDescription(description), timeoutMillis)
        } as T

    /**
     * Waits for the UI to become idle.
     */
    protected fun waitForIdle(): T = apply {
        composeTestRule.waitForIdle()
    } as T

    /**
     * Checks if a node with the specified text exists without throwing.
     * Useful for conditional logic in robots.
     */
    protected fun hasNodeWithText(text: String, timeoutMillis: Long = 1_000): Boolean = try {
        composeTestRule.waitUntilExactlyOneExists(hasText(text), timeoutMillis)
        true
    } catch (_: ComposeTimeoutException) {
        false
    }

    /**
     * Checks if a node with the specified content description exists without throwing.
     */
    protected fun hasNodeWithContentDescription(description: String, timeoutMillis: Long = 1_000): Boolean = try {
        composeTestRule.waitUntilExactlyOneExists(hasContentDescription(description), timeoutMillis)
        true
    } catch (_: ComposeTimeoutException) {
        false
    }
}
