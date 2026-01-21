/*
 * Copyright © 2025 Damyan Ivanov.
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

package net.ktnx.mobileledger.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AmountStyleTest {

    @Test
    fun constructor_setsAllFields() {
        val style = AmountStyle(
            AmountStyle.Position.AFTER,
            true,
            0,
            "."
        )

        assertEquals(AmountStyle.Position.AFTER, style.commodityPosition)
        assertTrue(style.isCommoditySpaced)
        assertEquals(0, style.precision)
        assertEquals(".", style.decimalMark)
    }

    @Test
    fun constructor_withBeforePosition() {
        val style = AmountStyle(
            AmountStyle.Position.BEFORE,
            false,
            2,
            ","
        )

        assertEquals(AmountStyle.Position.BEFORE, style.commodityPosition)
        assertFalse(style.isCommoditySpaced)
        assertEquals(2, style.precision)
        assertEquals(",", style.decimalMark)
    }

    @Test
    fun constructor_withNonePosition() {
        val style = AmountStyle(
            AmountStyle.Position.NONE,
            false,
            3,
            "."
        )

        assertEquals(AmountStyle.Position.NONE, style.commodityPosition)
        assertFalse(style.isCommoditySpaced)
        assertEquals(3, style.precision)
        assertEquals(".", style.decimalMark)
    }

    @Test
    fun serialize_withAfterPosition() {
        val style = AmountStyle(
            AmountStyle.Position.AFTER,
            true,
            0,
            "."
        )

        val serialized = style.serialize()
        assertEquals("AFTER:true:0:.", serialized)
    }

    @Test
    fun serialize_withBeforePosition() {
        val style = AmountStyle(
            AmountStyle.Position.BEFORE,
            false,
            2,
            "."
        )

        val serialized = style.serialize()
        assertEquals("BEFORE:false:2:.", serialized)
    }

    @Test
    fun serialize_withNonePosition() {
        val style = AmountStyle(
            AmountStyle.Position.NONE,
            true,
            4,
            ","
        )

        val serialized = style.serialize()
        assertEquals("NONE:true:4:,", serialized)
    }

    @Test
    fun serialize_withCommaDecimalMark() {
        val style = AmountStyle(
            AmountStyle.Position.AFTER,
            true,
            2,
            ","
        )

        val serialized = style.serialize()
        assertEquals("AFTER:true:2:,", serialized)
    }

    @Test
    fun deserialize_withValidString() {
        val serialized = "AFTER:true:0:."
        val style = AmountStyle.deserialize(serialized)

        assertNotNull(style)
        assertEquals(AmountStyle.Position.AFTER, style!!.commodityPosition)
        assertTrue(style.isCommoditySpaced)
        assertEquals(0, style.precision)
        assertEquals(".", style.decimalMark)
    }

    @Test
    fun deserialize_withBeforePosition() {
        val serialized = "BEFORE:false:2:."
        val style = AmountStyle.deserialize(serialized)

        assertNotNull(style)
        assertEquals(AmountStyle.Position.BEFORE, style!!.commodityPosition)
        assertFalse(style.isCommoditySpaced)
        assertEquals(2, style.precision)
        assertEquals(".", style.decimalMark)
    }

    @Test
    fun deserialize_withNonePosition() {
        val serialized = "NONE:true:3:,"
        val style = AmountStyle.deserialize(serialized)

        assertNotNull(style)
        assertEquals(AmountStyle.Position.NONE, style!!.commodityPosition)
        assertTrue(style.isCommoditySpaced)
        assertEquals(3, style.precision)
        assertEquals(",", style.decimalMark)
    }

    @Test
    fun deserialize_withNullInput() {
        val style = AmountStyle.deserialize(null)
        assertNull(style)
    }

    @Test
    fun deserialize_withEmptyString() {
        val style = AmountStyle.deserialize("")
        assertNull(style)
    }

    @Test
    fun deserialize_withInvalidFormat() {
        val style = AmountStyle.deserialize("INVALID")
        assertNull(style)
    }

    @Test
    fun deserialize_withInvalidParts() {
        val style = AmountStyle.deserialize("AFTER:true")
        assertNull(style)
    }

    @Test
    fun deserialize_withInvalidPosition() {
        val style = AmountStyle.deserialize("INVALID_POS:true:2:.")
        assertNull(style)
    }

    @Test
    fun deserialize_withInvalidPrecision() {
        val style = AmountStyle.deserialize("AFTER:true:invalid:.")
        assertNull(style)
    }

    @Test
    fun serializeDeserialize_roundTrip() {
        val original = AmountStyle(
            AmountStyle.Position.AFTER,
            true,
            0,
            "."
        )

        val serialized = original.serialize()
        val deserialized = AmountStyle.deserialize(serialized)

        assertNotNull(deserialized)
        assertEquals(original.commodityPosition, deserialized!!.commodityPosition)
        assertEquals(original.isCommoditySpaced, deserialized.isCommoditySpaced)
        assertEquals(original.precision, deserialized.precision)
        assertEquals(original.decimalMark, deserialized.decimalMark)
    }

    @Test
    fun serializeDeserialize_roundTripWithComma() {
        val original = AmountStyle(
            AmountStyle.Position.BEFORE,
            false,
            3,
            ","
        )

        val serialized = original.serialize()
        val deserialized = AmountStyle.deserialize(serialized)

        assertNotNull(deserialized)
        assertEquals(original.commodityPosition, deserialized!!.commodityPosition)
        assertEquals(original.isCommoditySpaced, deserialized.isCommoditySpaced)
        assertEquals(original.precision, deserialized.precision)
        assertEquals(original.decimalMark, deserialized.decimalMark)
    }

    @Test
    fun serializeDeserialize_allPositions() {
        for (position in AmountStyle.Position.values()) {
            val original = AmountStyle(position, true, 2, ".")
            val serialized = original.serialize()
            val deserialized = AmountStyle.deserialize(serialized)

            assertNotNull("Failed to deserialize position: $position", deserialized)
            assertEquals(position, deserialized!!.commodityPosition)
        }
    }

    @Test
    fun serializeDeserialize_variousPrecisions() {
        for (precision in 0..5) {
            val original = AmountStyle(
                AmountStyle.Position.AFTER,
                true,
                precision,
                "."
            )
            val serialized = original.serialize()
            val deserialized = AmountStyle.deserialize(serialized)

            assertNotNull("Failed to deserialize precision: $precision", deserialized)
            assertEquals(precision, deserialized!!.precision)
        }
    }

    @Test
    fun serializeDeserialize_spacedVariations() {
        // Test with spaced = true
        val spacedTrue = AmountStyle(AmountStyle.Position.AFTER, true, 2, ".")
        val serializedTrue = spacedTrue.serialize()
        val deserializedTrue = AmountStyle.deserialize(serializedTrue)
        assertNotNull(deserializedTrue)
        assertTrue(deserializedTrue!!.isCommoditySpaced)

        // Test with spaced = false
        val spacedFalse = AmountStyle(AmountStyle.Position.AFTER, false, 2, ".")
        val serializedFalse = spacedFalse.serialize()
        val deserializedFalse = AmountStyle.deserialize(serializedFalse)
        assertNotNull(deserializedFalse)
        assertFalse(deserializedFalse!!.isCommoditySpaced)
    }

    // ========================================
    // formatAccountAmount tests
    // ========================================

    @Test
    fun `formatAccountAmount with currency BEFORE no space`() {
        val style = AmountStyle(AmountStyle.Position.BEFORE, false, 2, ".")
        val result = AmountStyle.formatAccountAmount(100.50f, "USD", style)
        assertEquals("USD100.50", result)
    }

    @Test
    fun `formatAccountAmount with currency BEFORE with space`() {
        val style = AmountStyle(AmountStyle.Position.BEFORE, true, 2, ".")
        val result = AmountStyle.formatAccountAmount(100.50f, "USD", style)
        assertEquals("USD 100.50", result)
    }

    @Test
    fun `formatAccountAmount with currency AFTER no space`() {
        val style = AmountStyle(AmountStyle.Position.AFTER, false, 2, ".")
        val result = AmountStyle.formatAccountAmount(100.50f, "JPY", style)
        assertEquals("100.50JPY", result)
    }

    @Test
    fun `formatAccountAmount with currency AFTER with space`() {
        val style = AmountStyle(AmountStyle.Position.AFTER, true, 2, ".")
        val result = AmountStyle.formatAccountAmount(100.50f, "EUR", style)
        assertEquals("100.50 EUR", result)
    }

    @Test
    fun `formatAccountAmount with NONE position`() {
        val style = AmountStyle(AmountStyle.Position.NONE, false, 2, ".")
        val result = AmountStyle.formatAccountAmount(100.50f, "USD", style)
        assertEquals("100.50", result)
    }

    @Test
    fun `formatAccountAmount with null currency`() {
        val style = AmountStyle(AmountStyle.Position.AFTER, true, 2, ".")
        val result = AmountStyle.formatAccountAmount(100.50f, null, style)
        assertEquals("100.50", result)
    }

    @Test
    fun `formatAccountAmount with empty currency`() {
        val style = AmountStyle(AmountStyle.Position.AFTER, true, 2, ".")
        val result = AmountStyle.formatAccountAmount(100.50f, "", style)
        assertEquals("100.50", result)
    }

    @Test
    fun `formatAccountAmount with zero precision integer amount`() {
        val style = AmountStyle(AmountStyle.Position.AFTER, true, 0, ".")
        val result = AmountStyle.formatAccountAmount(100.0f, "JPY", style)
        assertEquals("100 JPY", result)
    }

    @Test
    fun `formatAccountAmount with zero precision non-integer amount`() {
        val style = AmountStyle(AmountStyle.Position.AFTER, true, 0, ".")
        val result = AmountStyle.formatAccountAmount(100.75f, "JPY", style)
        assertEquals("101 JPY", result)
    }

    @Test
    fun `formatAccountAmount with three decimal places`() {
        val style = AmountStyle(AmountStyle.Position.AFTER, true, 3, ".")
        val result = AmountStyle.formatAccountAmount(100.123f, "BTC", style)
        assertEquals("100.123 BTC", result)
    }

    @Test
    fun `formatAccountAmount with comma decimal mark`() {
        val style = AmountStyle(AmountStyle.Position.AFTER, true, 2, ",")
        val result = AmountStyle.formatAccountAmount(1000.50f, "EUR", style)
        assertEquals("1,000,50 EUR", result)
    }

    @Test
    fun `formatAccountAmount with custom decimal mark`() {
        val style = AmountStyle(AmountStyle.Position.AFTER, true, 2, "'")
        val result = AmountStyle.formatAccountAmount(100.50f, "CHF", style)
        assertEquals("100'50 CHF", result)
    }

    @Test
    fun `formatAccountAmount with negative amount`() {
        val style = AmountStyle(AmountStyle.Position.BEFORE, true, 2, ".")
        val result = AmountStyle.formatAccountAmount(-50.25f, "USD", style)
        assertEquals("USD -50.25", result)
    }

    @Test
    fun `formatAccountAmount with zero amount`() {
        val style = AmountStyle(AmountStyle.Position.AFTER, true, 2, ".")
        val result = AmountStyle.formatAccountAmount(0.0f, "USD", style)
        assertEquals("0.00 USD", result)
    }

    @Test
    fun `formatAccountAmount with large amount includes thousand separators`() {
        val style = AmountStyle(AmountStyle.Position.BEFORE, true, 2, ".")
        val result = AmountStyle.formatAccountAmount(1234567.89f, "USD", style)
        assertTrue(result.contains(","))
        assertTrue(result.startsWith("USD "))
    }

    @Test
    fun `formatAccountAmount with very small amount`() {
        val style = AmountStyle(AmountStyle.Position.AFTER, false, 4, ".")
        val result = AmountStyle.formatAccountAmount(0.0001f, "BTC", style)
        assertEquals("0.0001BTC", result)
    }

    @Test
    fun `formatAccountAmount precision 1`() {
        val style = AmountStyle(AmountStyle.Position.AFTER, true, 1, ".")
        val result = AmountStyle.formatAccountAmount(10.5f, "USD", style)
        assertEquals("10.5 USD", result)
    }

    @Test
    fun `formatAccountAmount precision 4`() {
        val style = AmountStyle(AmountStyle.Position.NONE, false, 4, ".")
        val result = AmountStyle.formatAccountAmount(12.3456f, null, style)
        assertEquals("12.3456", result)
    }

    @Test
    fun `formatAccountAmount precision 5`() {
        val style = AmountStyle(AmountStyle.Position.BEFORE, false, 5, ".")
        val result = AmountStyle.formatAccountAmount(1.23456f, "ETH", style)
        assertEquals("ETH1.23456", result)
    }

    // ========================================
    // Position enum tests
    // ========================================

    @Test
    fun `Position enum has correct values`() {
        val positions = AmountStyle.Position.values()
        assertEquals(3, positions.size)
        assertTrue(positions.contains(AmountStyle.Position.BEFORE))
        assertTrue(positions.contains(AmountStyle.Position.AFTER))
        assertTrue(positions.contains(AmountStyle.Position.NONE))
    }

    @Test
    fun `Position valueOf returns correct enum`() {
        assertEquals(AmountStyle.Position.BEFORE, AmountStyle.Position.valueOf("BEFORE"))
        assertEquals(AmountStyle.Position.AFTER, AmountStyle.Position.valueOf("AFTER"))
        assertEquals(AmountStyle.Position.NONE, AmountStyle.Position.valueOf("NONE"))
    }

    // ========================================
    // serialize edge cases
    // ========================================

    @Test
    fun `serialize with empty decimal mark defaults to period`() {
        val style = AmountStyle(AmountStyle.Position.AFTER, true, 2, "")
        val serialized = style.serialize()
        assertEquals("AFTER:true:2:.", serialized)
    }

    @Test
    fun `serialize with high precision`() {
        val style = AmountStyle(AmountStyle.Position.BEFORE, false, 10, ".")
        val serialized = style.serialize()
        assertEquals("BEFORE:false:10:.", serialized)
    }

    // ========================================
    // Data class equality tests
    // ========================================

    @Test
    fun `equals returns true for identical styles`() {
        val style1 = AmountStyle(AmountStyle.Position.AFTER, true, 2, ".")
        val style2 = AmountStyle(AmountStyle.Position.AFTER, true, 2, ".")
        assertEquals(style1, style2)
    }

    @Test
    fun `equals returns false for different positions`() {
        val style1 = AmountStyle(AmountStyle.Position.AFTER, true, 2, ".")
        val style2 = AmountStyle(AmountStyle.Position.BEFORE, true, 2, ".")
        assertFalse(style1 == style2)
    }

    @Test
    fun `equals returns false for different spacing`() {
        val style1 = AmountStyle(AmountStyle.Position.AFTER, true, 2, ".")
        val style2 = AmountStyle(AmountStyle.Position.AFTER, false, 2, ".")
        assertFalse(style1 == style2)
    }

    @Test
    fun `equals returns false for different precision`() {
        val style1 = AmountStyle(AmountStyle.Position.AFTER, true, 2, ".")
        val style2 = AmountStyle(AmountStyle.Position.AFTER, true, 3, ".")
        assertFalse(style1 == style2)
    }

    @Test
    fun `equals returns false for different decimal mark`() {
        val style1 = AmountStyle(AmountStyle.Position.AFTER, true, 2, ".")
        val style2 = AmountStyle(AmountStyle.Position.AFTER, true, 2, ",")
        assertFalse(style1 == style2)
    }

    @Test
    fun `hashCode is consistent for equal objects`() {
        val style1 = AmountStyle(AmountStyle.Position.AFTER, true, 2, ".")
        val style2 = AmountStyle(AmountStyle.Position.AFTER, true, 2, ".")
        assertEquals(style1.hashCode(), style2.hashCode())
    }

    @Test
    fun `copy creates new instance with modified field`() {
        val original = AmountStyle(AmountStyle.Position.AFTER, true, 2, ".")
        val copied = original.copy(precision = 4)
        assertEquals(4, copied.precision)
        assertEquals(original.commodityPosition, copied.commodityPosition)
        assertEquals(original.isCommoditySpaced, copied.isCommoditySpaced)
        assertEquals(original.decimalMark, copied.decimalMark)
    }

    @Test
    fun `toString contains all fields`() {
        val style = AmountStyle(AmountStyle.Position.AFTER, true, 2, ".")
        val str = style.toString()
        assertTrue(str.contains("AFTER"))
        assertTrue(str.contains("true"))
        assertTrue(str.contains("2"))
        assertTrue(str.contains("."))
    }
}
