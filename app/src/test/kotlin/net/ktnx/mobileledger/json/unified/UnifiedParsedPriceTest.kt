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
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [UnifiedParsedPrice].
 *
 * Tests verify:
 * - Default values
 * - Property accessors
 * - JSON deserialization
 */
class UnifiedParsedPriceTest {

    // ========================================
    // Default values tests
    // ========================================

    @Test
    fun `default tag is NoPrice`() {
        val price = UnifiedParsedPrice()
        assertEquals("NoPrice", price.tag)
    }

    // ========================================
    // Property setter tests
    // ========================================

    @Test
    fun `tag can be set to UnitPrice`() {
        val price = UnifiedParsedPrice()
        price.tag = "UnitPrice"
        assertEquals("UnitPrice", price.tag)
    }

    @Test
    fun `tag can be set to TotalPrice`() {
        val price = UnifiedParsedPrice()
        price.tag = "TotalPrice"
        assertEquals("TotalPrice", price.tag)
    }

    // ========================================
    // JSON deserialization tests
    // ========================================

    @Test
    fun `deserialize NoPrice tag`() {
        val mapper = ObjectMapper()
        val json = """
            {
                "tag": "NoPrice"
            }
        """.trimIndent()

        val price = mapper.readValue(json, UnifiedParsedPrice::class.java)

        assertEquals("NoPrice", price.tag)
    }

    @Test
    fun `deserialize UnitPrice tag`() {
        val mapper = ObjectMapper()
        val json = """
            {
                "tag": "UnitPrice"
            }
        """.trimIndent()

        val price = mapper.readValue(json, UnifiedParsedPrice::class.java)

        assertEquals("UnitPrice", price.tag)
    }

    @Test
    fun `deserialize TotalPrice tag`() {
        val mapper = ObjectMapper()
        val json = """
            {
                "tag": "TotalPrice"
            }
        """.trimIndent()

        val price = mapper.readValue(json, UnifiedParsedPrice::class.java)

        assertEquals("TotalPrice", price.tag)
    }

    @Test
    fun `deserialize ignores unknown properties`() {
        val mapper = ObjectMapper()
        val json = """
            {
                "tag": "NoPrice",
                "unknownField": "value"
            }
        """.trimIndent()

        // Should not throw exception
        val price = mapper.readValue(json, UnifiedParsedPrice::class.java)
        assertEquals("NoPrice", price.tag)
    }
}
