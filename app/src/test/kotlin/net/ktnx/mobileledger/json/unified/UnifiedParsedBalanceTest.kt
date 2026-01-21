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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [UnifiedParsedBalance].
 *
 * Tests verify:
 * - Default values
 * - Property accessors
 * - JSON deserialization
 */
class UnifiedParsedBalanceTest {

    // ========================================
    // Default values tests
    // ========================================

    @Test
    fun `default aquantity is null`() {
        val balance = UnifiedParsedBalance()
        assertNull(balance.aquantity)
    }

    @Test
    fun `default acommodity is empty string`() {
        val balance = UnifiedParsedBalance()
        assertEquals("", balance.acommodity)
    }

    @Test
    fun `default astyle is null`() {
        val balance = UnifiedParsedBalance()
        assertNull(balance.astyle)
    }

    // ========================================
    // acommodity accessor tests
    // ========================================

    @Test
    fun `acommodity getter returns set value`() {
        val balance = UnifiedParsedBalance()
        balance.acommodity = "USD"
        assertEquals("USD", balance.acommodity)
    }

    @Test
    fun `acommodity getter returns empty for null internal value`() {
        val balance = UnifiedParsedBalance()
        // Default state - internal _acommodity is null
        assertEquals("", balance.acommodity)
    }

    @Test
    fun `acommodity setter stores value`() {
        val balance = UnifiedParsedBalance()
        balance.acommodity = "EUR"
        assertEquals("EUR", balance.acommodity)
    }

    @Test
    fun `acommodity can be overwritten`() {
        val balance = UnifiedParsedBalance()
        balance.acommodity = "USD"
        balance.acommodity = "EUR"
        assertEquals("EUR", balance.acommodity)
    }

    // ========================================
    // aquantity tests
    // ========================================

    @Test
    fun `aquantity can be set`() {
        val balance = UnifiedParsedBalance()
        val quantity = UnifiedParsedQuantity().apply {
            decimalMantissa = 10000L
            decimalPlaces = 2
        }
        balance.aquantity = quantity

        assertNotNull(balance.aquantity)
        assertEquals(100.0f, balance.aquantity!!.asFloat(), 0.01f)
    }

    @Test
    fun `aquantity can be set to null`() {
        val balance = UnifiedParsedBalance()
        balance.aquantity = UnifiedParsedQuantity()
        balance.aquantity = null

        assertNull(balance.aquantity)
    }

    // ========================================
    // astyle tests
    // ========================================

    @Test
    fun `astyle can be set`() {
        val balance = UnifiedParsedBalance()
        val style = UnifiedParsedStyle().apply {
            ascommodityside = 'R'
            isAscommodityspaced = true
        }
        balance.astyle = style

        assertNotNull(balance.astyle)
        assertEquals('R', balance.astyle!!.ascommodityside)
    }

    // ========================================
    // Full object tests
    // ========================================

    @Test
    fun `can create complete balance object`() {
        val balance = UnifiedParsedBalance().apply {
            acommodity = "JPY"
            aquantity = UnifiedParsedQuantity().apply {
                decimalMantissa = 1000L
                decimalPlaces = 0
            }
            astyle = UnifiedParsedStyle().apply {
                ascommodityside = 'R'
                isAscommodityspaced = true
            }
        }

        assertEquals("JPY", balance.acommodity)
        assertEquals(1000f, balance.aquantity!!.asFloat(), 0.01f)
        assertEquals('R', balance.astyle!!.ascommodityside)
    }

    // ========================================
    // JSON deserialization tests
    // ========================================

    @Test
    fun `deserialize balance from JSON`() {
        val mapper = ObjectMapper()
        val json = """
            {
                "acommodity": "USD",
                "aquantity": {
                    "decimalMantissa": 10050,
                    "decimalPlaces": 2
                }
            }
        """.trimIndent()

        val balance = mapper.readValue(json, UnifiedParsedBalance::class.java)

        assertEquals("USD", balance.acommodity)
        assertNotNull(balance.aquantity)
        assertEquals(100.50f, balance.aquantity!!.asFloat(), 0.01f)
    }

    @Test
    fun `deserialize balance ignores unknown properties`() {
        val mapper = ObjectMapper()
        val json = """
            {
                "acommodity": "EUR",
                "unknownField": "value"
            }
        """.trimIndent()

        // Should not throw exception
        val balance = mapper.readValue(json, UnifiedParsedBalance::class.java)
        assertEquals("EUR", balance.acommodity)
    }

    @Test
    fun `deserialize balance with style`() {
        val mapper = ObjectMapper()
        val json = """
            {
                "acommodity": "GBP",
                "aquantity": {
                    "decimalMantissa": 5000,
                    "decimalPlaces": 2
                },
                "astyle": {
                    "ascommodityside": "L",
                    "ascommodityspaced": true,
                    "asdecimalmark": ".",
                    "asprecision": 2
                }
            }
        """.trimIndent()

        val balance = mapper.readValue(json, UnifiedParsedBalance::class.java)

        assertEquals("GBP", balance.acommodity)
        assertNotNull(balance.astyle)
        assertEquals('L', balance.astyle!!.ascommodityside)
    }
}
