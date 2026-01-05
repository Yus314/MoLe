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

package net.ktnx.mobileledger.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class AmountStyleTest {

    @Test
    public void constructor_setsAllFields() {
        AmountStyle style = new AmountStyle(
                AmountStyle.Position.AFTER,
                true,
                0,
                "."
        );

        assertEquals(AmountStyle.Position.AFTER, style.getCommodityPosition());
        assertTrue(style.isCommoditySpaced());
        assertEquals(0, style.getPrecision());
        assertEquals(".", style.getDecimalMark());
    }

    @Test
    public void constructor_withBeforePosition() {
        AmountStyle style = new AmountStyle(
                AmountStyle.Position.BEFORE,
                false,
                2,
                ","
        );

        assertEquals(AmountStyle.Position.BEFORE, style.getCommodityPosition());
        assertFalse(style.isCommoditySpaced());
        assertEquals(2, style.getPrecision());
        assertEquals(",", style.getDecimalMark());
    }

    @Test
    public void constructor_withNonePosition() {
        AmountStyle style = new AmountStyle(
                AmountStyle.Position.NONE,
                false,
                3,
                "."
        );

        assertEquals(AmountStyle.Position.NONE, style.getCommodityPosition());
        assertFalse(style.isCommoditySpaced());
        assertEquals(3, style.getPrecision());
        assertEquals(".", style.getDecimalMark());
    }

    @Test
    public void serialize_withAfterPosition() {
        AmountStyle style = new AmountStyle(
                AmountStyle.Position.AFTER,
                true,
                0,
                "."
        );

        String serialized = style.serialize();
        assertEquals("AFTER:true:0:.", serialized);
    }

    @Test
    public void serialize_withBeforePosition() {
        AmountStyle style = new AmountStyle(
                AmountStyle.Position.BEFORE,
                false,
                2,
                "."
        );

        String serialized = style.serialize();
        assertEquals("BEFORE:false:2:.", serialized);
    }

    @Test
    public void serialize_withNonePosition() {
        AmountStyle style = new AmountStyle(
                AmountStyle.Position.NONE,
                true,
                4,
                ","
        );

        String serialized = style.serialize();
        assertEquals("NONE:true:4:,", serialized);
    }

    @Test
    public void serialize_withCommaDecimalMark() {
        AmountStyle style = new AmountStyle(
                AmountStyle.Position.AFTER,
                true,
                2,
                ","
        );

        String serialized = style.serialize();
        assertEquals("AFTER:true:2:,", serialized);
    }

    @Test
    public void deserialize_withValidString() {
        String serialized = "AFTER:true:0:.";
        AmountStyle style = AmountStyle.deserialize(serialized);

        assertNotNull(style);
        assertEquals(AmountStyle.Position.AFTER, style.getCommodityPosition());
        assertTrue(style.isCommoditySpaced());
        assertEquals(0, style.getPrecision());
        assertEquals(".", style.getDecimalMark());
    }

    @Test
    public void deserialize_withBeforePosition() {
        String serialized = "BEFORE:false:2:.";
        AmountStyle style = AmountStyle.deserialize(serialized);

        assertNotNull(style);
        assertEquals(AmountStyle.Position.BEFORE, style.getCommodityPosition());
        assertFalse(style.isCommoditySpaced());
        assertEquals(2, style.getPrecision());
        assertEquals(".", style.getDecimalMark());
    }

    @Test
    public void deserialize_withNonePosition() {
        String serialized = "NONE:true:3:,";
        AmountStyle style = AmountStyle.deserialize(serialized);

        assertNotNull(style);
        assertEquals(AmountStyle.Position.NONE, style.getCommodityPosition());
        assertTrue(style.isCommoditySpaced());
        assertEquals(3, style.getPrecision());
        assertEquals(",", style.getDecimalMark());
    }

    @Test
    public void deserialize_withNullInput() {
        AmountStyle style = AmountStyle.deserialize(null);
        assertNull(style);
    }

    @Test
    public void deserialize_withEmptyString() {
        AmountStyle style = AmountStyle.deserialize("");
        assertNull(style);
    }

    @Test
    public void deserialize_withInvalidFormat() {
        AmountStyle style = AmountStyle.deserialize("INVALID");
        assertNull(style);
    }

    @Test
    public void deserialize_withInvalidParts() {
        AmountStyle style = AmountStyle.deserialize("AFTER:true");
        assertNull(style);
    }

    @Test
    public void deserialize_withInvalidPosition() {
        AmountStyle style = AmountStyle.deserialize("INVALID_POS:true:2:.");
        assertNull(style);
    }

    @Test
    public void deserialize_withInvalidPrecision() {
        AmountStyle style = AmountStyle.deserialize("AFTER:true:invalid:.");
        assertNull(style);
    }

    @Test
    public void serializeDeserialize_roundTrip() {
        AmountStyle original = new AmountStyle(
                AmountStyle.Position.AFTER,
                true,
                0,
                "."
        );

        String serialized = original.serialize();
        AmountStyle deserialized = AmountStyle.deserialize(serialized);

        assertNotNull(deserialized);
        assertEquals(original.getCommodityPosition(), deserialized.getCommodityPosition());
        assertEquals(original.isCommoditySpaced(), deserialized.isCommoditySpaced());
        assertEquals(original.getPrecision(), deserialized.getPrecision());
        assertEquals(original.getDecimalMark(), deserialized.getDecimalMark());
    }

    @Test
    public void serializeDeserialize_roundTripWithComma() {
        AmountStyle original = new AmountStyle(
                AmountStyle.Position.BEFORE,
                false,
                3,
                ","
        );

        String serialized = original.serialize();
        AmountStyle deserialized = AmountStyle.deserialize(serialized);

        assertNotNull(deserialized);
        assertEquals(original.getCommodityPosition(), deserialized.getCommodityPosition());
        assertEquals(original.isCommoditySpaced(), deserialized.isCommoditySpaced());
        assertEquals(original.getPrecision(), deserialized.getPrecision());
        assertEquals(original.getDecimalMark(), deserialized.getDecimalMark());
    }

    @Test
    public void serializeDeserialize_allPositions() {
        for (AmountStyle.Position position : AmountStyle.Position.values()) {
            AmountStyle original = new AmountStyle(position, true, 2, ".");
            String serialized = original.serialize();
            AmountStyle deserialized = AmountStyle.deserialize(serialized);

            assertNotNull("Failed to deserialize position: " + position, deserialized);
            assertEquals(position, deserialized.getCommodityPosition());
        }
    }

    @Test
    public void serializeDeserialize_variousPrecisions() {
        for (int precision = 0; precision <= 5; precision++) {
            AmountStyle original = new AmountStyle(
                    AmountStyle.Position.AFTER,
                    true,
                    precision,
                    "."
            );
            String serialized = original.serialize();
            AmountStyle deserialized = AmountStyle.deserialize(serialized);

            assertNotNull("Failed to deserialize precision: " + precision, deserialized);
            assertEquals(precision, deserialized.getPrecision());
        }
    }

    @Test
    public void serializeDeserialize_spacedVariations() {
        // Test with spaced = true
        AmountStyle spacedTrue = new AmountStyle(AmountStyle.Position.AFTER, true, 2, ".");
        String serializedTrue = spacedTrue.serialize();
        AmountStyle deserializedTrue = AmountStyle.deserialize(serializedTrue);
        assertNotNull(deserializedTrue);
        assertTrue(deserializedTrue.isCommoditySpaced());

        // Test with spaced = false
        AmountStyle spacedFalse = new AmountStyle(AmountStyle.Position.AFTER, false, 2, ".");
        String serializedFalse = spacedFalse.serialize();
        AmountStyle deserializedFalse = AmountStyle.deserialize(serializedFalse);
        assertNotNull(deserializedFalse);
        assertFalse(deserializedFalse.isCommoditySpaced());
    }
}
