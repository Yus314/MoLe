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
    // fromString tests
    // ========================================

    @Test
    fun `fromString parses simple decimal`() {
        val quantity = UnifiedParsedQuantity.fromString("123.45")
        assertEquals(12345L, quantity.decimalMantissa)
        assertEquals(2, quantity.decimalPlaces)
    }

    @Test
    fun `fromString parses integer`() {
        val quantity = UnifiedParsedQuantity.fromString("1000")
        assertEquals(1000L, quantity.decimalMantissa)
        assertEquals(0, quantity.decimalPlaces)
    }

    @Test
    fun `fromString parses zero`() {
        val quantity = UnifiedParsedQuantity.fromString("0")
        assertEquals(0L, quantity.decimalMantissa)
        assertEquals(0, quantity.decimalPlaces)
    }

    @Test
    fun `fromString parses negative number`() {
        val quantity = UnifiedParsedQuantity.fromString("-50.25")
        assertEquals(-5025L, quantity.decimalMantissa)
        assertEquals(2, quantity.decimalPlaces)
    }

    @Test
    fun `fromString parses decimal with many places`() {
        val quantity = UnifiedParsedQuantity.fromString("1.12345")
        assertEquals(112345L, quantity.decimalMantissa)
        assertEquals(5, quantity.decimalPlaces)
    }

    @Test
    fun `fromString parses decimal with single decimal place`() {
        val quantity = UnifiedParsedQuantity.fromString("10.5")
        assertEquals(105L, quantity.decimalMantissa)
        assertEquals(1, quantity.decimalPlaces)
    }

    @Test
    fun `fromString parses decimal with leading zero`() {
        val quantity = UnifiedParsedQuantity.fromString("0.99")
        assertEquals(99L, quantity.decimalMantissa)
        assertEquals(2, quantity.decimalPlaces)
    }

    // ========================================
    // asFloat tests
    // ========================================

    @Test
    fun `asFloat converts simple decimal`() {
        val quantity = UnifiedParsedQuantity(
            decimalMantissa = 12345L,
            decimalPlaces = 2
        )
        assertEquals(123.45f, quantity.asFloat(), 0.001f)
    }

    @Test
    fun `asFloat converts integer`() {
        val quantity = UnifiedParsedQuantity(
            decimalMantissa = 1000L,
            decimalPlaces = 0
        )
        assertEquals(1000f, quantity.asFloat(), 0.001f)
    }

    @Test
    fun `asFloat converts zero`() {
        val quantity = UnifiedParsedQuantity(
            decimalMantissa = 0L,
            decimalPlaces = 0
        )
        assertEquals(0f, quantity.asFloat(), 0.001f)
    }

    @Test
    fun `asFloat converts negative number`() {
        val quantity = UnifiedParsedQuantity(
            decimalMantissa = -5025L,
            decimalPlaces = 2
        )
        assertEquals(-50.25f, quantity.asFloat(), 0.001f)
    }

    @Test
    fun `asFloat converts small decimal`() {
        val quantity = UnifiedParsedQuantity(
            decimalMantissa = 99L,
            decimalPlaces = 2
        )
        assertEquals(0.99f, quantity.asFloat(), 0.001f)
    }

    // ========================================
    // fromString and asFloat round-trip tests
    // ========================================

    @Test
    fun `fromString and asFloat round-trip for typical amount`() {
        val original = "1500.00"
        val quantity = UnifiedParsedQuantity.fromString(original)
        assertEquals(1500.00f, quantity.asFloat(), 0.001f)
    }

    @Test
    fun `fromString and asFloat round-trip for small amount`() {
        val original = "0.01"
        val quantity = UnifiedParsedQuantity.fromString(original)
        assertEquals(0.01f, quantity.asFloat(), 0.001f)
    }

    @Test
    fun `fromString and asFloat round-trip for negative decimal`() {
        val quantity = UnifiedParsedQuantity.fromString("-99.99")
        assertEquals(-99.99f, quantity.asFloat(), 0.001f)
    }

    // ========================================
    // Default values tests
    // ========================================

    @Test
    fun `default decimalMantissa is 0`() {
        val quantity = UnifiedParsedQuantity()
        assertEquals(0L, quantity.decimalMantissa)
    }

    @Test
    fun `default decimalPlaces is 0`() {
        val quantity = UnifiedParsedQuantity()
        assertEquals(0, quantity.decimalPlaces)
    }
}
