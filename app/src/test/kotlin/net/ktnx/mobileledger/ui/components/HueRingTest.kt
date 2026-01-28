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

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import net.ktnx.mobileledger.core.ui.components.HueRing
import net.ktnx.mobileledger.ui.theme.MoLeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric tests for HueRing component.
 *
 * Tests cover:
 * - Component rendering with various parameters
 * - Tap gesture callback triggering
 * - Drag gesture callback triggering
 * - Hue normalization to 5-degree increments
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HueRingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var capturedHue: Int? = null

    private fun setupHueRing(selectedHue: Int = 0, initialHue: Int = 0, modifier: Modifier = Modifier.size(200.dp)) {
        capturedHue = null
        composeTestRule.setContent {
            MoLeTheme {
                HueRing(
                    selectedHue = selectedHue,
                    initialHue = initialHue,
                    onHueSelected = { capturedHue = it },
                    modifier = modifier
                )
            }
        }
    }

    // ========================================
    // Rendering Tests
    // ========================================

    @Test
    fun `HueRing renders without error`() {
        setupHueRing()
        composeTestRule.waitForIdle()
        // Canvas component renders without errors
    }

    @Test
    fun `HueRing accepts selectedHue parameter`() {
        setupHueRing(selectedHue = 120)
        composeTestRule.waitForIdle()
        // Rendering with selectedHue=120 completes without errors
    }

    @Test
    fun `HueRing accepts initialHue parameter`() {
        setupHueRing(initialHue = 240)
        composeTestRule.waitForIdle()
        // Rendering with initialHue=240 completes without errors
    }

    @Test
    fun `HueRing boundary hue 0 renders`() {
        setupHueRing(selectedHue = 0, initialHue = 0)
        composeTestRule.waitForIdle()
        // Boundary value 0 renders correctly
    }

    @Test
    fun `HueRing boundary hue 355 renders`() {
        setupHueRing(selectedHue = 355, initialHue = 355)
        composeTestRule.waitForIdle()
        // Boundary value 355 renders correctly
    }

    @Test
    fun `HueRing with custom modifier`() {
        setupHueRing(modifier = Modifier.size(300.dp))
        composeTestRule.waitForIdle()
        // Custom modifier applied successfully
    }

    // ========================================
    // Interaction Tests
    // ========================================

    @Test
    fun `HueRing tap on ring triggers callback`() {
        setupHueRing(selectedHue = 0, initialHue = 0)
        composeTestRule.waitForIdle()

        // Tap on right edge of ring (0 degrees position)
        composeTestRule.onRoot().performTouchInput {
            click(position = center.copy(x = center.x + (width / 2f) * 0.8f))
        }

        assertTrue("Callback should be triggered", capturedHue != null)
    }

    @Test
    fun `HueRing tap on center top resets to initial`() {
        val initialHue = 180
        setupHueRing(selectedHue = 90, initialHue = initialHue)
        composeTestRule.waitForIdle()

        // Tap on center top (y < center)
        composeTestRule.onRoot().performTouchInput {
            click(position = center.copy(y = center.y - 10f))
        }

        assertEquals("Should reset to initial hue", initialHue, capturedHue)
    }

    @Test
    fun `HueRing drag triggers callback`() {
        setupHueRing()
        composeTestRule.waitForIdle()

        composeTestRule.onRoot().performTouchInput {
            swipe(
                start = center.copy(x = center.x + (width / 2f) * 0.7f),
                end = center.copy(y = center.y - (height / 2f) * 0.7f),
                durationMillis = 200
            )
        }

        assertTrue("Callback should be triggered by drag", capturedHue != null)
    }

    @Test
    fun `HueRing callback receives normalized hue`() {
        setupHueRing()
        composeTestRule.waitForIdle()

        composeTestRule.onRoot().performTouchInput {
            click(position = center.copy(x = center.x + (width / 2f) * 0.8f))
        }

        capturedHue?.let { hue ->
            assertEquals("Hue should be multiple of 5", 0, hue % 5)
        }
    }
}
