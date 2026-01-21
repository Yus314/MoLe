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

package net.ktnx.mobileledger.robot.util

import androidx.compose.ui.test.junit4.ComposeTestRule
import net.ktnx.mobileledger.robot.base.BaseRobot

/**
 * Extension functions for Compose UI testing with Robot Pattern.
 *
 * Provides DSL-style entry points for screen robots, enabling readable test syntax like:
 *
 * ```kotlin
 * composeTestRule.newTransactionScreen {
 *     typeDescription("Grocery shopping")
 *     typeAmount(0, "50.00")
 * } verify {
 *     submitButtonIsEnabled()
 * }
 * ```
 */

/**
 * Generic DSL entry point for any Robot.
 *
 * Usage:
 * ```kotlin
 * composeTestRule.robot(::MyScreenRobot) {
 *     doSomething()
 * }
 * ```
 *
 * @param factory Constructor reference for the Robot class
 * @param block Actions to perform on the Robot
 * @return The Robot instance for further chaining
 */
inline fun <T : BaseRobot<T>> ComposeTestRule.robot(factory: (ComposeTestRule) -> T, block: T.() -> Unit): T =
    factory(this).apply(block)

/**
 * Generic DSL entry point that only returns the Robot without executing actions.
 *
 * Useful when you need to reference the robot before calling actions:
 * ```kotlin
 * val robot = composeTestRule.robot(::MyScreenRobot)
 * // Setup...
 * robot.doSomething()
 * ```
 *
 * @param factory Constructor reference for the Robot class
 * @return The Robot instance
 */
fun <T : BaseRobot<T>> ComposeTestRule.robot(factory: (ComposeTestRule) -> T): T = factory(this)

// ============================================================
// Screen-specific DSL entry points will be added here
// as each screen robot is implemented.
//
// Example (to be added in Phase 2):
//
// fun ComposeTestRule.newTransactionScreen(
//     block: NewTransactionScreenRobot.() -> Unit
// ): NewTransactionScreenRobot =
//     NewTransactionScreenRobot(this).apply(block)
//
// Example (to be added in Phase 3):
//
// fun ComposeTestRule.mainScreen(
//     block: MainScreenRobot.() -> Unit
// ): MainScreenRobot =
//     MainScreenRobot(this).apply(block)
// ============================================================
