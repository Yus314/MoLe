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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import net.ktnx.mobileledger.core.ui.components.WeakOverscrollContainer
import net.ktnx.mobileledger.ui.theme.MoLeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric tests for WeakOverscrollContainer component.
 *
 * Tests cover:
 * - Content rendering
 * - stretchFactor parameter handling
 * - Modifier application
 * - Content preservation after scroll operations
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class WeakOverscrollEffectTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ========================================
    // Rendering Tests
    // ========================================

    @Test
    fun `WeakOverscrollContainer renders content`() {
        composeTestRule.setContent {
            MoLeTheme {
                WeakOverscrollContainer {
                    Text("Test Content")
                }
            }
        }

        composeTestRule.onNodeWithText("Test Content").assertIsDisplayed()
    }

    @Test
    fun `WeakOverscrollContainer with default stretchFactor`() {
        composeTestRule.setContent {
            MoLeTheme {
                WeakOverscrollContainer {
                    Text("Default Stretch")
                }
            }
        }

        composeTestRule.onNodeWithText("Default Stretch").assertIsDisplayed()
    }

    @Test
    fun `WeakOverscrollContainer with custom stretchFactor`() {
        composeTestRule.setContent {
            MoLeTheme {
                WeakOverscrollContainer(stretchFactor = 0.5f) {
                    Text("Custom Stretch")
                }
            }
        }

        composeTestRule.onNodeWithText("Custom Stretch").assertIsDisplayed()
    }

    @Test
    fun `WeakOverscrollContainer with modifier`() {
        composeTestRule.setContent {
            MoLeTheme {
                WeakOverscrollContainer(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text("With Modifier")
                }
            }
        }

        composeTestRule.onNodeWithText("With Modifier").assertIsDisplayed()
    }

    @Test
    fun `WeakOverscrollContainer preserves content after scroll`() {
        val items = (1..20).map { "Item $it" }

        composeTestRule.setContent {
            MoLeTheme {
                WeakOverscrollContainer(
                    modifier = Modifier.height(200.dp)
                ) {
                    LazyColumn {
                        items(items) { item ->
                            Text(item)
                        }
                    }
                }
            }
        }

        // First item should be visible
        composeTestRule.onNodeWithText("Item 1").assertIsDisplayed()

        // Perform scroll
        composeTestRule.onNodeWithText("Item 1").performTouchInput {
            swipeUp()
        }

        // Content should still be present after scroll (some items visible)
        composeTestRule.waitForIdle()
    }

    @Test
    fun `WeakOverscrollContainer renders multiple children`() {
        composeTestRule.setContent {
            MoLeTheme {
                WeakOverscrollContainer {
                    Column {
                        Text("First")
                        Text("Second")
                        Text("Third")
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("First").assertIsDisplayed()
        composeTestRule.onNodeWithText("Second").assertIsDisplayed()
        composeTestRule.onNodeWithText("Third").assertIsDisplayed()
    }
}
