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

package net.ktnx.mobileledger.json.unified

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [UnifiedParsedQuantity].
 *
 * Tests verify:
 * - Parsing decimal strings to mantissa/places representation
 * - Conversion back to float values
 * - Edge cases like zero, negative numbers, integers
 */
class UnifiedParsedQuantityTest {

    // ========================================
    // parseString tests
    // ========================================

    @Test
    fun `parseString parses simple decimal`() {
        val quantity = UnifiedParsedQuantity("123.45")
        assertEquals(12345L, quantity.decimalMantissa)
        assertEquals(2, quantity.decimalPlaces)
    }

    @Test
    fun `parseString parses integer`() {
        val quantity = UnifiedParsedQuantity("1000")
        assertEquals(1000L, quantity.decimalMantissa)
        assertEquals(0, quantity.decimalPlaces)
    }

    @Test
    fun `parseString parses zero`() {
        val quantity = UnifiedParsedQuantity("0")
        assertEquals(0L, quantity.decimalMantissa)
        assertEquals(0, quantity.decimalPlaces)
    }

    @Test
    fun `parseString parses negative number`() {
        val quantity = UnifiedParsedQuantity("-50.25")
        assertEquals(-5025L, quantity.decimalMantissa)
        assertEquals(2, quantity.decimalPlaces)
    }

    @Test
    fun `parseString parses decimal with many places`() {
        val quantity = UnifiedParsedQuantity("1.12345")
        assertEquals(112345L, quantity.decimalMantissa)
        assertEquals(5, quantity.decimalPlaces)
    }

    @Test
    fun `parseString parses decimal with single decimal place`() {
        val quantity = UnifiedParsedQuantity("10.5")
        assertEquals(105L, quantity.decimalMantissa)
        assertEquals(1, quantity.decimalPlaces)
    }

    @Test
    fun `parseString parses decimal with leading zero`() {
        val quantity = UnifiedParsedQuantity("0.99")
        assertEquals(99L, quantity.decimalMantissa)
        assertEquals(2, quantity.decimalPlaces)
    }

    // ========================================
    // asFloat tests
    // ========================================

    @Test
    fun `asFloat converts simple decimal`() {
        val quantity = UnifiedParsedQuantity().apply {
            decimalMantissa = 12345L
            decimalPlaces = 2
        }
        assertEquals(123.45f, quantity.asFloat(), 0.001f)
    }

    @Test
    fun `asFloat converts integer`() {
        val quantity = UnifiedParsedQuantity().apply {
            decimalMantissa = 1000L
            decimalPlaces = 0
        }
        assertEquals(1000f, quantity.asFloat(), 0.001f)
    }

    @Test
    fun `asFloat converts zero`() {
        val quantity = UnifiedParsedQuantity().apply {
            decimalMantissa = 0L
            decimalPlaces = 0
        }
        assertEquals(0f, quantity.asFloat(), 0.001f)
    }

    @Test
    fun `asFloat converts negative number`() {
        val quantity = UnifiedParsedQuantity().apply {
            decimalMantissa = -5025L
            decimalPlaces = 2
        }
        assertEquals(-50.25f, quantity.asFloat(), 0.001f)
    }

    @Test
    fun `asFloat converts small decimal`() {
        val quantity = UnifiedParsedQuantity().apply {
            decimalMantissa = 99L
            decimalPlaces = 2
        }
        assertEquals(0.99f, quantity.asFloat(), 0.001f)
    }

    // ========================================
    // String constructor tests
    // ========================================

    @Test
    fun `constructor with string parses and converts correctly`() {
        val quantity = UnifiedParsedQuantity("1234.56")
        assertEquals(1234.56f, quantity.asFloat(), 0.001f)
    }

    @Test
    fun `constructor with integer string`() {
        val quantity = UnifiedParsedQuantity("500")
        assertEquals(500f, quantity.asFloat(), 0.001f)
    }

    @Test
    fun `constructor with negative decimal string`() {
        val quantity = UnifiedParsedQuantity("-99.99")
        assertEquals(-99.99f, quantity.asFloat(), 0.001f)
    }

    // ========================================
    // Round-trip tests
    // ========================================

    @Test
    fun `parseString and asFloat round-trip for typical amount`() {
        val original = "1500.00"
        val quantity = UnifiedParsedQuantity(original)
        assertEquals(1500.00f, quantity.asFloat(), 0.001f)
    }

    @Test
    fun `parseString and asFloat round-trip for small amount`() {
        val original = "0.01"
        val quantity = UnifiedParsedQuantity(original)
        assertEquals(0.01f, quantity.asFloat(), 0.001f)
    }
}
