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

package net.ktnx.mobileledger.core.network.json.unified

import net.ktnx.mobileledger.core.network.json.MoLeJson
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [UnifiedParsedPrice].
 *
 * Tests verify:
 * - Default values
 * - Constructor initialization
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
    // Constructor initialization tests
    // ========================================

    @Test
    fun `can create price with UnitPrice tag`() {
        val price = UnifiedParsedPrice(tag = "UnitPrice")
        assertEquals("UnitPrice", price.tag)
    }

    @Test
    fun `can create price with TotalPrice tag`() {
        val price = UnifiedParsedPrice(tag = "TotalPrice")
        assertEquals("TotalPrice", price.tag)
    }

    // ========================================
    // JSON deserialization tests
    // ========================================

    @Test
    fun `deserialize NoPrice tag`() {
        val json = """
            {
                "tag": "NoPrice"
            }
        """.trimIndent()

        val price = MoLeJson.decodeFromString<UnifiedParsedPrice>(json)

        assertEquals("NoPrice", price.tag)
    }

    @Test
    fun `deserialize UnitPrice tag`() {
        val json = """
            {
                "tag": "UnitPrice"
            }
        """.trimIndent()

        val price = MoLeJson.decodeFromString<UnifiedParsedPrice>(json)

        assertEquals("UnitPrice", price.tag)
    }

    @Test
    fun `deserialize TotalPrice tag`() {
        val json = """
            {
                "tag": "TotalPrice"
            }
        """.trimIndent()

        val price = MoLeJson.decodeFromString<UnifiedParsedPrice>(json)

        assertEquals("TotalPrice", price.tag)
    }

    @Test
    fun `deserialize ignores unknown properties`() {
        val json = """
            {
                "tag": "NoPrice",
                "unknownField": "value"
            }
        """.trimIndent()

        // Should not throw exception
        val price = MoLeJson.decodeFromString<UnifiedParsedPrice>(json)
        assertEquals("NoPrice", price.tag)
    }
}
