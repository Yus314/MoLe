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

package net.ktnx.mobileledger.model

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
}
