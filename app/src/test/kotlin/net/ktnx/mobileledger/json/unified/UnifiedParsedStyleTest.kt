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

import net.ktnx.mobileledger.json.MoLeJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [UnifiedParsedStyle].
 *
 * Tests verify:
 * - Default values
 * - JSON deserialization for v1_32+ format using kotlinx.serialization
 */
class UnifiedParsedStyleTest {

    // ========================================
    // Default values tests
    // ========================================

    @Test
    fun `default ascommodityside is null character`() {
        val style = UnifiedParsedStyle()
        assertEquals('\u0000', style.ascommodityside)
    }

    @Test
    fun `default isAscommodityspaced is false`() {
        val style = UnifiedParsedStyle()
        assertFalse(style.isAscommodityspaced)
    }

    @Test
    fun `default digitgroups is 0`() {
        val style = UnifiedParsedStyle()
        assertEquals(0, style.digitgroups)
    }

    @Test
    fun `default asdecimalmark is period`() {
        val style = UnifiedParsedStyle()
        assertEquals(".", style.asdecimalmark)
    }

    @Test
    fun `default asprecision is 0`() {
        val style = UnifiedParsedStyle()
        assertEquals(0, style.asprecision)
    }

    @Test
    fun `default asrounding is null`() {
        val style = UnifiedParsedStyle()
        assertNull(style.asrounding)
    }

    // ========================================
    // Constructor tests
    // ========================================

    @Test
    fun `constructor with ascommodityside L`() {
        val style = UnifiedParsedStyle(ascommodityside = 'L')
        assertEquals('L', style.ascommodityside)
    }

    @Test
    fun `constructor with ascommodityside R`() {
        val style = UnifiedParsedStyle(ascommodityside = 'R')
        assertEquals('R', style.ascommodityside)
    }

    @Test
    fun `constructor with isAscommodityspaced true`() {
        val style = UnifiedParsedStyle(isAscommodityspaced = true)
        assertEquals(true, style.isAscommodityspaced)
    }

    @Test
    fun `constructor with digitgroups`() {
        val style = UnifiedParsedStyle(digitgroups = 3)
        assertEquals(3, style.digitgroups)
    }

    @Test
    fun `constructor with asrounding`() {
        val style = UnifiedParsedStyle(asrounding = "HalfUp")
        assertEquals("HalfUp", style.asrounding)
    }

    @Test
    fun `constructor with all fields`() {
        val style = UnifiedParsedStyle(
            ascommodityside = 'L',
            isAscommodityspaced = true,
            digitgroups = 3,
            asdecimalmark = ",",
            asprecision = 2,
            asrounding = "NoRounding"
        )

        assertEquals('L', style.ascommodityside)
        assertEquals(true, style.isAscommodityspaced)
        assertEquals(3, style.digitgroups)
        assertEquals(",", style.asdecimalmark)
        assertEquals(2, style.asprecision)
        assertEquals("NoRounding", style.asrounding)
    }

    // ========================================
    // Kotlin Serialization deserialization tests
    // ========================================

    @Test
    fun `deserialize simple style JSON`() {
        val json = """
            {
                "ascommodityside": "L",
                "ascommodityspaced": true,
                "asdecimalmark": ",",
                "asprecision": 2
            }
        """.trimIndent()

        val style = MoLeJson.decodeFromString<UnifiedParsedStyle>(json)

        assertEquals('L', style.ascommodityside)
        assertEquals(true, style.isAscommodityspaced)
        assertEquals(",", style.asdecimalmark)
        assertEquals(2, style.asprecision)
    }

    @Test
    fun `deserialize ignores unknown properties`() {
        val json = """
            {
                "ascommodityside": "L",
                "unknownField": "value",
                "anotherUnknown": 123
            }
        """.trimIndent()

        // Should not throw exception
        val style = MoLeJson.decodeFromString<UnifiedParsedStyle>(json)
        assertEquals('L', style.ascommodityside)
    }

    @Test
    fun `deserialize style with asrounding`() {
        val json = """
            {
                "ascommodityside": "R",
                "asdecimalmark": ".",
                "asprecision": 2,
                "asrounding": "NoRounding"
            }
        """.trimIndent()

        val style = MoLeJson.decodeFromString<UnifiedParsedStyle>(json)

        assertEquals("NoRounding", style.asrounding)
    }

    @Test
    fun `deserialize handles asprecision as number`() {
        val json = """
            {
                "asprecision": 4
            }
        """.trimIndent()

        val style = MoLeJson.decodeFromString<UnifiedParsedStyle>(json)

        assertEquals(4, style.asprecision)
    }

    // ========================================
    // Serialization round-trip tests
    // ========================================

    @Test
    fun `serialize and deserialize round-trip`() {
        val original = UnifiedParsedStyle(
            ascommodityside = 'L',
            isAscommodityspaced = true,
            asprecision = 2,
            asdecimalmark = ".",
            asrounding = "NoRounding"
        )

        val json = MoLeJson.encodeToString(UnifiedParsedStyle.serializer(), original)
        val deserialized = MoLeJson.decodeFromString<UnifiedParsedStyle>(json)

        assertEquals(original.ascommodityside, deserialized.ascommodityside)
        assertEquals(original.isAscommodityspaced, deserialized.isAscommodityspaced)
        assertEquals(original.asprecision, deserialized.asprecision)
        assertEquals(original.asdecimalmark, deserialized.asdecimalmark)
        assertEquals(original.asrounding, deserialized.asrounding)
    }
}
