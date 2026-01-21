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
 */
class StyleConfigurerTest {

    // ========================================
    // DecimalPointChar tests
    // ========================================

    @Test
    fun `DecimalPointChar configures style with precision and decimal point`() {
        // Given
        val style = UnifiedParsedStyle()
        val precision = 4

        // When
        StyleConfigurer.DecimalPointChar.configureStyle(style, precision)

        // Then
        assertEquals(4, style.asprecision)
        assertEquals('.', style.asdecimalpoint)
    }

    @Test
    fun `DecimalPointChar ignores non-UnifiedParsedStyle objects`() {
        // Given
        val notAStyle = "not a style"

        // When/Then - should not throw
        StyleConfigurer.DecimalPointChar.configureStyle(notAStyle, 2)
    }

    // ========================================
    // DecimalPointCharWithParsedPrecision tests
    // ========================================

    @Test
    fun `DecimalPointCharWithParsedPrecision configures style correctly`() {
        // Given
        val style = UnifiedParsedStyle()
        val precision = 6

        // When
        StyleConfigurer.DecimalPointCharWithParsedPrecision.configureStyle(style, precision)

        // Then
        assertEquals(6, style.asprecision)
        assertEquals('.', style.asdecimalpoint)
    }

    @Test
    fun `DecimalPointCharWithParsedPrecision ignores non-UnifiedParsedStyle objects`() {
        // Given
        val notAStyle = 12345

        // When/Then - should not throw
        StyleConfigurer.DecimalPointCharWithParsedPrecision.configureStyle(notAStyle, 2)
    }

    // ========================================
    // DecimalPointCharIntPrecision tests
    // ========================================

    @Test
    fun `DecimalPointCharIntPrecision configures style correctly`() {
        // Given
        val style = UnifiedParsedStyle()
        val precision = 3

        // When
        StyleConfigurer.DecimalPointCharIntPrecision.configureStyle(style, precision)

        // Then
        assertEquals(3, style.asprecision)
        assertEquals('.', style.asdecimalpoint)
    }

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
        assertEquals('.', style.asdecimalpoint)
        assertEquals(".", style.asdecimalmark)
        assertNull(style.asrounding)
    }

    @Test
    fun `configureStyle can be called multiple times`() {
        // Given
        val style = UnifiedParsedStyle()

        // When - configure with different configurers
        StyleConfigurer.DecimalPointChar.configureStyle(style, 2)
        StyleConfigurer.DecimalMarkString.configureStyle(style, 4)

        // Then - last configuration wins
        assertEquals(4, style.asprecision)
        assertEquals(".", style.asdecimalmark)
        assertEquals("NoRounding", style.asrounding)
    }
}
