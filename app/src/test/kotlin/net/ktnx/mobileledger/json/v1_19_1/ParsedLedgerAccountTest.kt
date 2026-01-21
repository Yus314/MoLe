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

package net.ktnx.mobileledger.json.v1_19_1

import net.ktnx.mobileledger.json.unified.UnifiedParsedBalance
import net.ktnx.mobileledger.json.unified.UnifiedParsedQuantity
import net.ktnx.mobileledger.json.unified.UnifiedParsedStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [ParsedLedgerAccount] in v1_19_1 API.
 *
 * Tests verify:
 * - getSimpleBalance() conversion
 * - Handling of null balances
 */
class ParsedLedgerAccountTest {

    // ========================================
    // getSimpleBalance tests
    // ========================================

    @Test
    fun `getSimpleBalance returns empty list for null aibalance`() {
        val account = ParsedLedgerAccount().apply {
            aibalance = null
        }

        val balances = account.getSimpleBalance()

        assertTrue(balances.isEmpty())
    }

    @Test
    fun `getSimpleBalance returns empty list for empty aibalance`() {
        val account = ParsedLedgerAccount().apply {
            aibalance = emptyList()
        }

        val balances = account.getSimpleBalance()

        assertTrue(balances.isEmpty())
    }

    @Test
    fun `getSimpleBalance converts single balance`() {
        val account = ParsedLedgerAccount().apply {
            aibalance = listOf(
                UnifiedParsedBalance().apply {
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
                UnifiedParsedBalance().apply {
                    acommodity = "USD"
                    aquantity = UnifiedParsedQuantity().apply {
                        decimalMantissa = 1000
                        decimalPlaces = 2
                    }
                    astyle = UnifiedParsedStyle()
                },
                UnifiedParsedBalance().apply {
                    acommodity = "EUR"
                    aquantity = UnifiedParsedQuantity().apply {
                        decimalMantissa = 2000
                        decimalPlaces = 2
                    }
                    astyle = UnifiedParsedStyle()
                }
            )
        }

        val balances = account.getSimpleBalance()

        assertEquals(2, balances.size)
        assertEquals("USD", balances[0].commodity)
        assertEquals(10.0f, balances[0].amount, 0.01f)
        assertEquals("EUR", balances[1].commodity)
        assertEquals(20.0f, balances[1].amount, 0.01f)
    }

    @Test
    fun `getSimpleBalance handles null quantity`() {
        val account = ParsedLedgerAccount().apply {
            aibalance = listOf(
                UnifiedParsedBalance().apply {
                    acommodity = "USD"
                    aquantity = null
                    astyle = UnifiedParsedStyle()
                }
            )
        }

        val balances = account.getSimpleBalance()

        assertEquals(1, balances.size)
        assertEquals(0f, balances[0].amount, 0.01f)
    }

    @Test
    fun `getSimpleBalance includes style information`() {
        val account = ParsedLedgerAccount().apply {
            aibalance = listOf(
                UnifiedParsedBalance().apply {
                    acommodity = "USD"
                    aquantity = UnifiedParsedQuantity().apply {
                        decimalMantissa = 100
                        decimalPlaces = 2
                    }
                    astyle = UnifiedParsedStyle().apply {
                        ascommodityside = 'L'
                        isAscommodityspaced = true
                    }
                }
            )
        }

        val balances = account.getSimpleBalance()

        assertNotNull(balances[0].amountStyle)
    }

    @Test
    fun `getSimpleBalance handles negative amounts`() {
        val account = ParsedLedgerAccount().apply {
            aibalance = listOf(
                UnifiedParsedBalance().apply {
                    acommodity = "USD"
                    aquantity = UnifiedParsedQuantity().apply {
                        decimalMantissa = -5000
                        decimalPlaces = 2
                    }
                    astyle = UnifiedParsedStyle()
                }
            )
        }

        val balances = account.getSimpleBalance()

        assertEquals(-50.0f, balances[0].amount, 0.01f)
    }
}
