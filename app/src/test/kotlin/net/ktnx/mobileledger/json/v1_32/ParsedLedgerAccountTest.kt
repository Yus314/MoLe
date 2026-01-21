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

package net.ktnx.mobileledger.json.v1_32

import net.ktnx.mobileledger.json.unified.UnifiedParsedQuantity
import net.ktnx.mobileledger.json.unified.UnifiedParsedStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [ParsedLedgerAccount] in v1_32 API.
 */
class ParsedLedgerAccountTest {

    @Test
    fun `getSimpleBalance converts single balance`() {
        val account = ParsedLedgerAccount().apply {
            aibalance = listOf(
                ParsedBalance().apply {
                    acommodity = "USD"
                    aquantity = UnifiedParsedQuantity().apply {
                        decimalMantissa = 10050
                        decimalPlaces = 2
                    }
                    astyle = UnifiedParsedStyle()
                }
            )
        }

        val balances = account.getSimpleBalance()

        assertEquals(1, balances.size)
        assertEquals("USD", balances[0].commodity)
        assertEquals(100.50f, balances[0].amount, 0.01f)
    }

    @Test
    fun `getSimpleBalance converts multiple balances`() {
        val account = ParsedLedgerAccount().apply {
            aibalance = listOf(
                ParsedBalance().apply {
                    acommodity = "USD"
                    aquantity = UnifiedParsedQuantity().apply {
                        decimalMantissa = 10000
                        decimalPlaces = 2
                    }
                    astyle = UnifiedParsedStyle()
                },
                ParsedBalance().apply {
                    acommodity = "EUR"
                    aquantity = UnifiedParsedQuantity().apply {
                        decimalMantissa = 5000
                        decimalPlaces = 2
                    }
                    astyle = UnifiedParsedStyle()
                }
            )
        }

        val balances = account.getSimpleBalance()

        assertEquals(2, balances.size)
        assertEquals("USD", balances[0].commodity)
        assertEquals(100.00f, balances[0].amount, 0.01f)
        assertEquals("EUR", balances[1].commodity)
        assertEquals(50.00f, balances[1].amount, 0.01f)
    }

    @Test
    fun `getSimpleBalance returns empty list when aibalance is null`() {
        val account = ParsedLedgerAccount().apply {
            aibalance = null
        }

        val balances = account.getSimpleBalance()

        assertTrue(balances.isEmpty())
    }

    @Test
    fun `getSimpleBalance returns empty list when aibalance is empty`() {
        val account = ParsedLedgerAccount().apply {
            aibalance = emptyList()
        }

        val balances = account.getSimpleBalance()

        assertTrue(balances.isEmpty())
    }

    @Test
    fun `getSimpleBalance handles null aquantity`() {
        val account = ParsedLedgerAccount().apply {
            aibalance = listOf(
                ParsedBalance().apply {
                    acommodity = "USD"
                    aquantity = null
                    astyle = UnifiedParsedStyle()
                }
            )
        }

        val balances = account.getSimpleBalance()

        assertEquals(1, balances.size)
        assertEquals("USD", balances[0].commodity)
        assertEquals(0f, balances[0].amount, 0.01f)
    }

    @Test
    fun `getSimpleBalance handles negative amount`() {
        val account = ParsedLedgerAccount().apply {
            aibalance = listOf(
                ParsedBalance().apply {
                    acommodity = "USD"
                    aquantity = UnifiedParsedQuantity().apply {
                        decimalMantissa = -5025
                        decimalPlaces = 2
                    }
                    astyle = UnifiedParsedStyle()
                }
            )
        }

        val balances = account.getSimpleBalance()

        assertEquals(1, balances.size)
        assertEquals(-50.25f, balances[0].amount, 0.01f)
    }

    @Test
    fun `adeclarationinfo field exists`() {
        val account = ParsedLedgerAccount()

        account.adeclarationinfo = null

        // Just verify the field exists and is accessible (v1_32 specific field)
        assertEquals(null, account.adeclarationinfo)
    }
}
