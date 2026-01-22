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

package net.ktnx.mobileledger.json.common

import net.ktnx.mobileledger.json.unified.UnifiedParsedStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [StyleConfigurer] implementations.
 *
 * Tests the DecimalMarkString configurer for hledger API v1_32+.
 */
class StyleConfigurerTest {

    // ========================================
    // DecimalMarkString tests
    // ========================================

    @Test
    fun `DecimalMarkString configures style with mark and rounding`() {
        // Given
        val style = UnifiedParsedStyle()
        val precision = 8

        // When
        StyleConfigurer.DecimalMarkString.configureStyle(style, precision)

        // Then
        assertEquals(8, style.asprecision)
        assertEquals(".", style.asdecimalmark)
        assertEquals("NoRounding", style.asrounding)
    }

    @Test
    fun `DecimalMarkString ignores non-UnifiedParsedStyle objects`() {
        // Given
        val notAStyle = listOf(1, 2, 3)

        // When/Then - should not throw
        StyleConfigurer.DecimalMarkString.configureStyle(notAStyle, 2)
    }

    // ========================================
    // Default values tests
    // ========================================

    @Test
    fun `UnifiedParsedStyle starts with default values`() {
        // Given
        val style = UnifiedParsedStyle()

        // Then
        assertEquals(0, style.asprecision)
        assertEquals(".", style.asdecimalmark)
        assertNull(style.asrounding)
    }

    @Test
    fun `configureStyle overwrites previous values`() {
        // Given
        val style = UnifiedParsedStyle()
        style.asprecision = 2

        // When
        StyleConfigurer.DecimalMarkString.configureStyle(style, 4)

        // Then
        assertEquals(4, style.asprecision)
        assertEquals(".", style.asdecimalmark)
        assertEquals("NoRounding", style.asrounding)
    }
}
