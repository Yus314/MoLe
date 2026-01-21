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

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [UnifiedParsedStyle].
 *
 * Tests verify:
 * - Default values
 * - Property accessors
 * - JSON setter behavior for different formats
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
    // asdecimalpoint accessor tests
    // ========================================

    @Test
    fun `asdecimalpoint getter returns first char of asdecimalmark`() {
        val style = UnifiedParsedStyle()
        style.asdecimalmark = ","
        assertEquals(',', style.asdecimalpoint)
    }

    @Test
    fun `asdecimalpoint getter returns period for empty mark`() {
        val style = UnifiedParsedStyle()
        style.asdecimalmark = ""
        assertEquals('.', style.asdecimalpoint)
    }

    @Test
    fun `asdecimalpoint setter updates asdecimalmark`() {
        val style = UnifiedParsedStyle()
        style.asdecimalpoint = ','
        assertEquals(",", style.asdecimalmark)
    }

    // ========================================
    // setAsdecimalpointFromJson tests
    // ========================================

    @Test
    fun `setAsdecimalpointFromJson handles Char input`() {
        val style = UnifiedParsedStyle()
        style.setAsdecimalpointFromJson(',')
        assertEquals(",", style.asdecimalmark)
    }

    @Test
    fun `setAsdecimalpointFromJson handles String input`() {
        val style = UnifiedParsedStyle()
        style.setAsdecimalpointFromJson(",")
        assertEquals(",", style.asdecimalmark)
    }

    @Test
    fun `setAsdecimalpointFromJson handles empty String`() {
        val style = UnifiedParsedStyle()
        style.setAsdecimalpointFromJson("")
        assertEquals(".", style.asdecimalmark)
    }

    @Test
    fun `setAsdecimalpointFromJson handles null`() {
        val style = UnifiedParsedStyle()
        style.setAsdecimalpointFromJson(null)
        assertEquals(".", style.asdecimalmark)
    }

    @Test
    fun `setAsdecimalpointFromJson handles unknown type`() {
        val style = UnifiedParsedStyle()
        style.setAsdecimalpointFromJson(123)
        assertEquals(".", style.asdecimalmark)
    }

    // ========================================
    // setAsprecisionFromJson tests
    // ========================================

    @Test
    fun `setAsprecisionFromJson handles Int input`() {
        val style = UnifiedParsedStyle()
        style.setAsprecisionFromJson(2)
        assertEquals(2, style.asprecision)
    }

    @Test
    fun `setAsprecisionFromJson handles Number input`() {
        val style = UnifiedParsedStyle()
        style.setAsprecisionFromJson(3L)
        assertEquals(3, style.asprecision)
    }

    @Test
    fun `setAsprecisionFromJson handles Map input v1_19_1 format`() {
        val style = UnifiedParsedStyle()
        val map = mapOf("tag" to "Precision", "contents" to 4)
        style.setAsprecisionFromJson(map)
        assertEquals(4, style.asprecision)
    }

    @Test
    fun `setAsprecisionFromJson handles Map without contents`() {
        val style = UnifiedParsedStyle()
        val map = mapOf("tag" to "Precision")
        style.setAsprecisionFromJson(map)
        assertEquals(0, style.asprecision)
    }

    @Test
    fun `setAsprecisionFromJson handles null input`() {
        val style = UnifiedParsedStyle()
        style.setAsprecisionFromJson(null)
        assertEquals(0, style.asprecision)
    }

    @Test
    fun `setAsprecisionFromJson handles unknown type`() {
        val style = UnifiedParsedStyle()
        style.setAsprecisionFromJson("invalid")
        assertEquals(0, style.asprecision)
    }

    // ========================================
    // Property setter tests
    // ========================================

    @Test
    fun `ascommodityside can be set to L`() {
        val style = UnifiedParsedStyle()
        style.ascommodityside = 'L'
        assertEquals('L', style.ascommodityside)
    }

    @Test
    fun `ascommodityside can be set to R`() {
        val style = UnifiedParsedStyle()
        style.ascommodityside = 'R'
        assertEquals('R', style.ascommodityside)
    }

    @Test
    fun `isAscommodityspaced can be set to true`() {
        val style = UnifiedParsedStyle()
        style.isAscommodityspaced = true
        assertEquals(true, style.isAscommodityspaced)
    }

    @Test
    fun `digitgroups can be set`() {
        val style = UnifiedParsedStyle()
        style.digitgroups = 3
        assertEquals(3, style.digitgroups)
    }

    @Test
    fun `asrounding can be set`() {
        val style = UnifiedParsedStyle()
        style.asrounding = "HalfUp"
        assertEquals("HalfUp", style.asrounding)
    }

    // ========================================
    // Jackson deserialization tests
    // ========================================

    @Test
    fun `deserialize simple style JSON`() {
        val mapper = ObjectMapper()
        val json = """
            {
                "ascommodityside": "L",
                "ascommodityspaced": true,
                "asdecimalmark": ",",
                "asprecision": 2
            }
        """.trimIndent()

        val style = mapper.readValue(json, UnifiedParsedStyle::class.java)

        assertEquals('L', style.ascommodityside)
        assertEquals(true, style.isAscommodityspaced)
        assertEquals(",", style.asdecimalmark)
        assertEquals(2, style.asprecision)
    }

    @Test
    fun `deserialize style with asdecimalpoint alias`() {
        val mapper = ObjectMapper()
        val json = """
            {
                "ascommodityside": "R",
                "ascommodityspaced": false,
                "asdecimalpoint": ".",
                "asprecision": 0
            }
        """.trimIndent()

        val style = mapper.readValue(json, UnifiedParsedStyle::class.java)

        assertEquals(".", style.asdecimalmark)
    }

    @Test
    fun `deserialize style with v1_19_1 precision object`() {
        val mapper = ObjectMapper()
        val json = """
            {
                "ascommodityside": "L",
                "ascommodityspaced": false,
                "asdecimalmark": ".",
                "asprecision": {"tag": "Precision", "contents": 3}
            }
        """.trimIndent()

        val style = mapper.readValue(json, UnifiedParsedStyle::class.java)

        assertEquals(3, style.asprecision)
    }

    @Test
    fun `deserialize ignores unknown properties`() {
        val mapper = ObjectMapper()
        val json = """
            {
                "ascommodityside": "L",
                "unknownField": "value",
                "anotherUnknown": 123
            }
        """.trimIndent()

        // Should not throw exception
        val style = mapper.readValue(json, UnifiedParsedStyle::class.java)
        assertEquals('L', style.ascommodityside)
    }
}
