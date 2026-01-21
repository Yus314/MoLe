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

package net.ktnx.mobileledger.json.v1_50

import net.ktnx.mobileledger.json.unified.UnifiedParsedAccountData
import net.ktnx.mobileledger.json.unified.UnifiedParsedBalance
import net.ktnx.mobileledger.json.unified.UnifiedParsedBalanceData
import net.ktnx.mobileledger.json.unified.UnifiedParsedQuantity
import net.ktnx.mobileledger.json.unified.UnifiedParsedStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [ParsedLedgerAccount] in v1_50 API.
 *
 * v1_50 uses a different structure with `adata: ParsedAccountData` instead of `aibalance`.
 */
class ParsedLedgerAccountTest {

    @Test
    fun `getSimpleBalance converts single balance from adata`() {
        val account = ParsedLedgerAccount().apply {
            adata = createAccountData(
                listOf(
                    createBalance("USD", 10050, 2)
                ),
                numPostings = 1
            )
        }

        val balances = account.getSimpleBalance()

        assertEquals(1, balances.size)
        assertEquals("USD", balances[0].commodity)
        assertEquals(100.50f, balances[0].amount, 0.01f)
    }

    @Test
    fun `getSimpleBalance converts multiple balances from adata`() {
        val account = ParsedLedgerAccount().apply {
            adata = createAccountData(
                listOf(
                    createBalance("USD", 10000, 2),
                    createBalance("EUR", 5000, 2)
                ),
                numPostings = 5
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
    fun `getSimpleBalance returns empty list when adata is null`() {
        val account = ParsedLedgerAccount().apply {
            adata = null
        }

        val balances = account.getSimpleBalance()

        assertTrue(balances.isEmpty())
    }

    @Test
    fun `getSimpleBalance returns empty list when pdperiods is empty`() {
        val account = ParsedLedgerAccount().apply {
            adata = UnifiedParsedAccountData().apply {
                pdperiods = emptyList()
            }
        }

        val balances = account.getSimpleBalance()

        assertTrue(balances.isEmpty())
    }

    @Test
    fun `getSimpleBalance returns empty list when bdincludingsubs is null`() {
        val account = ParsedLedgerAccount().apply {
            adata = UnifiedParsedAccountData().apply {
                pdperiods = listOf(
                    UnifiedParsedAccountData.PeriodEntry(
                        date = "0000-01-01",
                        balanceData = UnifiedParsedBalanceData().apply {
                            bdincludingsubs = null
                            bdnumpostings = 0
                        }
                    )
                )
            }
        }

        val balances = account.getSimpleBalance()

        assertTrue(balances.isEmpty())
    }

    @Test
    fun `getSimpleBalance handles null aquantity`() {
        val account = ParsedLedgerAccount().apply {
            adata = UnifiedParsedAccountData().apply {
                pdperiods = listOf(
                    UnifiedParsedAccountData.PeriodEntry(
                        date = "0000-01-01",
                        balanceData = UnifiedParsedBalanceData().apply {
                            bdincludingsubs = listOf(
                                UnifiedParsedBalance().apply {
                                    acommodity = "USD"
                                    aquantity = null
                                    astyle = UnifiedParsedStyle()
                                }
                            )
                            bdnumpostings = 1
                        }
                    )
                )
            }
        }

        val balances = account.getSimpleBalance()

        assertEquals(1, balances.size)
        assertEquals("USD", balances[0].commodity)
        assertEquals(0f, balances[0].amount, 0.01f)
    }

    @Test
    fun `getSimpleBalance handles negative amount`() {
        val account = ParsedLedgerAccount().apply {
            adata = createAccountData(
                listOf(createBalance("USD", -5025, 2)),
                numPostings = 1
            )
        }

        val balances = account.getSimpleBalance()

        assertEquals(1, balances.size)
        assertEquals(-50.25f, balances[0].amount, 0.01f)
    }

    @Test
    fun `anumpostings getter returns value from adata`() {
        val account = ParsedLedgerAccount().apply {
            adata = createAccountData(emptyList(), numPostings = 42)
        }

        assertEquals(42, account.anumpostings)
    }

    @Test
    fun `anumpostings getter returns 0 when adata is null`() {
        val account = ParsedLedgerAccount().apply {
            adata = null
        }

        assertEquals(0, account.anumpostings)
    }

    @Test
    fun `anumpostings getter returns 0 when pdperiods is empty`() {
        val account = ParsedLedgerAccount().apply {
            adata = UnifiedParsedAccountData().apply {
                pdperiods = emptyList()
            }
        }

        assertEquals(0, account.anumpostings)
    }

    @Test
    fun `anumpostings setter is no-op`() {
        val account = ParsedLedgerAccount().apply {
            adata = createAccountData(emptyList(), numPostings = 10)
        }

        account.anumpostings = 999

        assertEquals(10, account.anumpostings)
    }

    @Test
    fun `adeclarationinfo field exists`() {
        val account = ParsedLedgerAccount()

        account.adeclarationinfo = null

        assertNull(account.adeclarationinfo)
    }

    private fun createAccountData(balances: List<UnifiedParsedBalance>, numPostings: Int) =
        UnifiedParsedAccountData().apply {
            pdperiods = listOf(
                UnifiedParsedAccountData.PeriodEntry(
                    date = "0000-01-01",
                    balanceData = UnifiedParsedBalanceData().apply {
                        bdincludingsubs = balances
                        bdnumpostings = numPostings
                    }
                )
            )
        }

    private fun createBalance(commodity: String, mantissa: Long, places: Int) = UnifiedParsedBalance().apply {
        acommodity = commodity
        aquantity = UnifiedParsedQuantity().apply {
            decimalMantissa = mantissa
            decimalPlaces = places
        }
        astyle = UnifiedParsedStyle()
    }
}
