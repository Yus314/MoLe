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

import net.ktnx.mobileledger.core.domain.model.AmountStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [UnifiedParsedLedgerAccount].
 *
 * Tests verify:
 * - Balance extraction from both old (aibalance) and new (adata) formats
 * - Domain model conversion
 * - Level calculation from account name
 */
class UnifiedParsedLedgerAccountTest {

    // ========================================
    // Helper methods
    // ========================================

    private fun createQuantity(mantissa: Long, places: Int): UnifiedParsedQuantity = UnifiedParsedQuantity(
        decimalMantissa = mantissa,
        decimalPlaces = places
    )

    private fun createBalance(commodity: String, mantissa: Long, places: Int): UnifiedParsedBalance =
        UnifiedParsedBalance(
            acommodity = commodity,
            aquantity = createQuantity(mantissa, places)
        )

    // ========================================
    // getSimpleBalance tests - aibalance format (v1_14-v1_40)
    // ========================================

    @Test
    fun `getSimpleBalance returns balances from aibalance`() {
        val account = UnifiedParsedLedgerAccount(
            aname = "Assets:Bank",
            aibalance = listOf(createBalance("USD", 100000, 2))
        )

        val balances = account.getSimpleBalance()

        assertEquals(1, balances.size)
        assertEquals("USD", balances[0].commodity)
        assertEquals(1000.00f, balances[0].amount, 0.01f)
    }

    @Test
    fun `getSimpleBalance returns multiple balances from aibalance`() {
        val account = UnifiedParsedLedgerAccount(
            aname = "Assets:Bank",
            aibalance = listOf(
                createBalance("USD", 100000, 2),
                createBalance("EUR", 50000, 2)
            )
        )

        val balances = account.getSimpleBalance()

        assertEquals(2, balances.size)
        assertTrue(balances.any { it.commodity == "USD" && it.amount == 1000.00f })
        assertTrue(balances.any { it.commodity == "EUR" && it.amount == 500.00f })
    }

    @Test
    fun `getSimpleBalance returns empty list when no balances`() {
        val account = UnifiedParsedLedgerAccount(
            aname = "Assets:Bank",
            aibalance = null
        )

        val balances = account.getSimpleBalance()

        assertTrue(balances.isEmpty())
    }

    // ========================================
    // toDomain tests
    // ========================================

    @Test
    fun `toDomain creates account with correct name`() {
        val account = UnifiedParsedLedgerAccount(
            aname = "Assets:Bank:Checking",
            aibalance = listOf(createBalance("USD", 100000, 2))
        )

        val domain = account.toDomain()

        assertEquals("Assets:Bank:Checking", domain.name)
    }

    @Test
    fun `toDomain calculates level correctly for top level account`() {
        val account = UnifiedParsedLedgerAccount(
            aname = "Assets",
            aibalance = emptyList()
        )

        val domain = account.toDomain()

        assertEquals(0, domain.level)
    }

    @Test
    fun `toDomain calculates level correctly for nested account`() {
        val account = UnifiedParsedLedgerAccount(
            aname = "Assets:Bank:Checking",
            aibalance = emptyList()
        )

        val domain = account.toDomain()

        assertEquals(2, domain.level)
    }

    @Test
    fun `toDomain sets isExpanded to false`() {
        val account = UnifiedParsedLedgerAccount(
            aname = "Assets",
            aibalance = emptyList()
        )

        val domain = account.toDomain()

        assertFalse(domain.isExpanded)
    }

    @Test
    fun `toDomain sets isVisible to true`() {
        val account = UnifiedParsedLedgerAccount(
            aname = "Assets",
            aibalance = emptyList()
        )

        val domain = account.toDomain()

        assertTrue(domain.isVisible)
    }

    @Test
    fun `toDomain converts balances to amounts`() {
        val account = UnifiedParsedLedgerAccount(
            aname = "Assets:Bank",
            aibalance = listOf(
                createBalance("USD", 100000, 2),
                createBalance("EUR", 50000, 2)
            )
        )

        val domain = account.toDomain()

        assertEquals(2, domain.amounts.size)
        assertTrue(domain.amounts.any { it.currency == "USD" && it.amount == 1000.00f })
        assertTrue(domain.amounts.any { it.currency == "EUR" && it.amount == 500.00f })
    }

    @Test
    fun `toDomain aggregates same currency amounts`() {
        val account = UnifiedParsedLedgerAccount(
            aname = "Assets:Bank",
            aibalance = listOf(
                createBalance("USD", 50000, 2),
                createBalance("USD", 50000, 2)
            )
        )

        val domain = account.toDomain()

        assertEquals(1, domain.amounts.size)
        assertEquals("USD", domain.amounts[0].currency)
        assertEquals(1000.00f, domain.amounts[0].amount, 0.01f)
    }

    @Test
    fun `toDomain sets id to null for new account`() {
        val account = UnifiedParsedLedgerAccount(
            aname = "Assets",
            aibalance = emptyList()
        )

        val domain = account.toDomain()

        assertEquals(null, domain.id)
    }

    // ========================================
    // anumpostings tests
    // ========================================

    @Test
    fun `anumpostings returns set value when no adata`() {
        val account = UnifiedParsedLedgerAccount(
            aname = "Assets",
            anumpostings = 5
        )

        assertEquals(5, account.anumpostings)
    }

    // ========================================
    // SimpleBalance tests
    // ========================================

    @Test
    fun `SimpleBalance constructor with two args sets null amountStyle`() {
        val balance = SimpleBalance("USD", 100.0f)

        assertEquals("USD", balance.commodity)
        assertEquals(100.0f, balance.amount, 0.01f)
        assertNull(balance.amountStyle)
    }

    @Test
    fun `SimpleBalance constructor with three args sets amountStyle`() {
        val style = AmountStyle(
            commodityPosition = AmountStyle.Position.NONE,
            isCommoditySpaced = false,
            precision = 2,
            decimalMark = "."
        )
        val balance = SimpleBalance("USD", 100.0f, style)

        assertEquals("USD", balance.commodity)
        assertEquals(100.0f, balance.amount, 0.01f)
        assertEquals(style, balance.amountStyle)
    }
}
