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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [UnifiedParsedAmount].
 *
 * Tests verify:
 * - Default values
 * - Constructor initialization
 * - JSON deserialization with aliases
 */
class UnifiedParsedAmountTest {

    // ========================================
    // Default values tests
    // ========================================

    @Test
    fun `default acommodity is null`() {
        val amount = UnifiedParsedAmount()
        assertNull(amount.acommodity)
    }

    @Test
    fun `default aquantity is null`() {
        val amount = UnifiedParsedAmount()
        assertNull(amount.aquantity)
    }

    @Test
    fun `default aismultiplier is false`() {
        val amount = UnifiedParsedAmount()
        assertFalse(amount.aismultiplier)
    }

    @Test
    fun `default astyle is null`() {
        val amount = UnifiedParsedAmount()
        assertNull(amount.astyle)
    }

    @Test
    fun `default aprice is null`() {
        val amount = UnifiedParsedAmount()
        assertNull(amount.aprice)
    }

    // ========================================
    // Constructor initialization tests
    // ========================================

    @Test
    fun `can create amount with acommodity`() {
        val amount = UnifiedParsedAmount(acommodity = "USD")
        assertEquals("USD", amount.acommodity)
    }

    @Test
    fun `can create amount with aquantity`() {
        val amount = UnifiedParsedAmount(
            aquantity = UnifiedParsedQuantity(
                decimalMantissa = 10000L,
                decimalPlaces = 2
            )
        )
        assertNotNull(amount.aquantity)
        assertEquals(100.0f, amount.aquantity!!.asFloat(), 0.01f)
    }

    @Test
    fun `can create amount with aismultiplier true`() {
        val amount = UnifiedParsedAmount(aismultiplier = true)
        assertEquals(true, amount.aismultiplier)
    }

    @Test
    fun `can create amount with astyle`() {
        val amount = UnifiedParsedAmount(
            astyle = UnifiedParsedStyle(ascommodityside = 'L')
        )
        assertNotNull(amount.astyle)
    }

    // ========================================
    // Full object tests
    // ========================================

    @Test
    fun `can create complete amount object`() {
        val amount = UnifiedParsedAmount(
            acommodity = "EUR",
            aquantity = UnifiedParsedQuantity(
                decimalMantissa = 5025L,
                decimalPlaces = 2
            ),
            aismultiplier = false,
            astyle = UnifiedParsedStyle(
                ascommodityside = 'R',
                isAscommodityspaced = true,
                asprecision = 2
            )
        )

        assertEquals("EUR", amount.acommodity)
        assertEquals(50.25f, amount.aquantity!!.asFloat(), 0.01f)
        assertFalse(amount.aismultiplier)
        assertEquals('R', amount.astyle!!.ascommodityside)
    }

    // ========================================
    // JSON deserialization tests
    // ========================================

    @Test
    fun `deserialize amount from JSON`() {
        val json = """
            {
                "acommodity": "USD",
                "aquantity": {
                    "decimalMantissa": 10050,
                    "decimalPlaces": 2
                },
                "aismultiplier": false
            }
        """.trimIndent()

        val amount = MoLeJson.decodeFromString<UnifiedParsedAmount>(json)

        assertEquals("USD", amount.acommodity)
        assertEquals(100.50f, amount.aquantity!!.asFloat(), 0.01f)
        assertFalse(amount.aismultiplier)
    }

    @Test
    fun `deserialize amount with aprice field`() {
        val json = """
            {
                "acommodity": "BTC",
                "aquantity": {
                    "decimalMantissa": 100000000,
                    "decimalPlaces": 8
                },
                "aprice": null
            }
        """.trimIndent()

        val amount = MoLeJson.decodeFromString<UnifiedParsedAmount>(json)

        assertEquals("BTC", amount.acommodity)
        assertNull(amount.aprice)
    }

    @Test
    fun `deserialize amount with acost alias for aprice`() {
        val json = """
            {
                "acommodity": "EUR",
                "aquantity": {
                    "decimalMantissa": 1000,
                    "decimalPlaces": 2
                },
                "acost": null
            }
        """.trimIndent()

        val amount = MoLeJson.decodeFromString<UnifiedParsedAmount>(json)

        assertEquals("EUR", amount.acommodity)
        // acost should be aliased to aprice
        assertNull(amount.aprice)
    }

    @Test
    fun `deserialize amount ignores unknown properties`() {
        val json = """
            {
                "acommodity": "GBP",
                "unknownField": "value",
                "anotherUnknown": 123
            }
        """.trimIndent()

        // Should not throw exception
        val amount = MoLeJson.decodeFromString<UnifiedParsedAmount>(json)
        assertEquals("GBP", amount.acommodity)
    }

    @Test
    fun `deserialize amount with style`() {
        val json = """
            {
                "acommodity": "JPY",
                "aquantity": {
                    "decimalMantissa": 1000,
                    "decimalPlaces": 0
                },
                "astyle": {
                    "ascommodityside": "R",
                    "ascommodityspaced": true,
                    "asdecimalmark": ".",
                    "asprecision": 0
                }
            }
        """.trimIndent()

        val amount = MoLeJson.decodeFromString<UnifiedParsedAmount>(json)

        assertEquals("JPY", amount.acommodity)
        assertNotNull(amount.astyle)
        assertEquals('R', amount.astyle!!.ascommodityside)
        assertEquals(0, amount.astyle!!.asprecision)
    }

    @Test
    fun `deserialize amount with aismultiplier true`() {
        val json = """
            {
                "acommodity": "USD",
                "aismultiplier": true
            }
        """.trimIndent()

        val amount = MoLeJson.decodeFromString<UnifiedParsedAmount>(json)

        assertEquals(true, amount.aismultiplier)
    }
}
