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

package net.ktnx.mobileledger.json.mapper

import net.ktnx.mobileledger.domain.model.AmountStyle
import net.ktnx.mobileledger.json.unified.UnifiedParsedStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [AmountStyleMapper].
 *
 * Tests verify:
 * - Conversion from UnifiedParsedStyle to AmountStyle
 * - Currency position determination
 * - Null handling
 */
class AmountStyleMapperTest {

    // ========================================
    // toDomain null handling tests
    // ========================================

    @Test
    fun `toDomain returns null for null input`() {
        val result = AmountStyleMapper.toDomain(null, "USD")
        assertNull(result)
    }

    // ========================================
    // Position determination tests
    // ========================================

    @Test
    fun `toDomain with side L returns BEFORE position`() {
        val parsedStyle = UnifiedParsedStyle().apply {
            ascommodityside = 'L'
            isAscommodityspaced = false
            asprecision = 2
            asdecimalmark = "."
        }

        val result = AmountStyleMapper.toDomain(parsedStyle, "USD")

        assertEquals(AmountStyle.Position.BEFORE, result!!.commodityPosition)
    }

    @Test
    fun `toDomain with side R returns AFTER position`() {
        val parsedStyle = UnifiedParsedStyle().apply {
            ascommodityside = 'R'
            isAscommodityspaced = true
            asprecision = 2
            asdecimalmark = "."
        }

        val result = AmountStyleMapper.toDomain(parsedStyle, "EUR")

        assertEquals(AmountStyle.Position.AFTER, result!!.commodityPosition)
    }

    @Test
    fun `toDomain with null currency returns NONE position`() {
        val parsedStyle = UnifiedParsedStyle().apply {
            ascommodityside = 'L'
            asprecision = 2
        }

        val result = AmountStyleMapper.toDomain(parsedStyle, null)

        assertEquals(AmountStyle.Position.NONE, result!!.commodityPosition)
    }

    @Test
    fun `toDomain with empty currency returns NONE position`() {
        val parsedStyle = UnifiedParsedStyle().apply {
            ascommodityside = 'L'
            asprecision = 2
        }

        val result = AmountStyleMapper.toDomain(parsedStyle, "")

        assertEquals(AmountStyle.Position.NONE, result!!.commodityPosition)
    }

    @Test
    fun `toDomain with unknown side returns NONE position`() {
        val parsedStyle = UnifiedParsedStyle().apply {
            ascommodityside = 'X'
            asprecision = 2
        }

        val result = AmountStyleMapper.toDomain(parsedStyle, "USD")

        assertEquals(AmountStyle.Position.NONE, result!!.commodityPosition)
    }

    // ========================================
    // Spaced property tests
    // ========================================

    @Test
    fun `toDomain preserves spaced true`() {
        val parsedStyle = UnifiedParsedStyle().apply {
            ascommodityside = 'L'
            isAscommodityspaced = true
            asprecision = 2
        }

        val result = AmountStyleMapper.toDomain(parsedStyle, "USD")

        assertEquals(true, result!!.isCommoditySpaced)
    }

    @Test
    fun `toDomain preserves spaced false`() {
        val parsedStyle = UnifiedParsedStyle().apply {
            ascommodityside = 'R'
            isAscommodityspaced = false
            asprecision = 2
        }

        val result = AmountStyleMapper.toDomain(parsedStyle, "USD")

        assertEquals(false, result!!.isCommoditySpaced)
    }

    // ========================================
    // Precision property tests
    // ========================================

    @Test
    fun `toDomain preserves precision 0`() {
        val parsedStyle = UnifiedParsedStyle().apply {
            ascommodityside = 'L'
            asprecision = 0
        }

        val result = AmountStyleMapper.toDomain(parsedStyle, "JPY")

        assertEquals(0, result!!.precision)
    }

    @Test
    fun `toDomain preserves precision 2`() {
        val parsedStyle = UnifiedParsedStyle().apply {
            ascommodityside = 'L'
            asprecision = 2
        }

        val result = AmountStyleMapper.toDomain(parsedStyle, "USD")

        assertEquals(2, result!!.precision)
    }

    @Test
    fun `toDomain preserves precision 8`() {
        val parsedStyle = UnifiedParsedStyle().apply {
            ascommodityside = 'L'
            asprecision = 8
        }

        val result = AmountStyleMapper.toDomain(parsedStyle, "BTC")

        assertEquals(8, result!!.precision)
    }

    // ========================================
    // Decimal mark property tests
    // ========================================

    @Test
    fun `toDomain preserves decimal mark dot`() {
        val parsedStyle = UnifiedParsedStyle().apply {
            ascommodityside = 'L'
            asprecision = 2
            asdecimalmark = "."
        }

        val result = AmountStyleMapper.toDomain(parsedStyle, "USD")

        assertEquals(".", result!!.decimalMark)
    }

    @Test
    fun `toDomain preserves decimal mark comma`() {
        val parsedStyle = UnifiedParsedStyle().apply {
            ascommodityside = 'R'
            asprecision = 2
            asdecimalmark = ","
        }

        val result = AmountStyleMapper.toDomain(parsedStyle, "EUR")

        assertEquals(",", result!!.decimalMark)
    }

    // ========================================
    // Full conversion tests
    // ========================================

    @Test
    fun `toDomain converts all properties`() {
        val parsedStyle = UnifiedParsedStyle().apply {
            ascommodityside = 'R'
            isAscommodityspaced = true
            asprecision = 3
            asdecimalmark = ","
        }

        val result = AmountStyleMapper.toDomain(parsedStyle, "CHF")

        assertEquals(AmountStyle.Position.AFTER, result!!.commodityPosition)
        assertEquals(true, result.isCommoditySpaced)
        assertEquals(3, result.precision)
        assertEquals(",", result.decimalMark)
    }
}
