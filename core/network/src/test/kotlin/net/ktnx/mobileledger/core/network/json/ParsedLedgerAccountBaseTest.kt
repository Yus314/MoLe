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

package net.ktnx.mobileledger.core.network.json

import net.ktnx.mobileledger.core.domain.model.AmountStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [ParsedLedgerAccount] base class.
 */
class ParsedLedgerAccountBaseTest {

    private class TestParsedLedgerAccount(
        private val balances: List<ParsedLedgerAccount.SimpleBalance> = emptyList()
    ) : ParsedLedgerAccount() {
        override fun getSimpleBalance(): List<SimpleBalance> = balances
    }

    @Test
    fun `toDomain converts account with no colons to level 0`() {
        val account = TestParsedLedgerAccount().apply {
            aname = "Assets"
        }

        val domain = account.toDomain()

        assertEquals("Assets", domain.name)
        assertEquals(0, domain.level)
    }

    @Test
    fun `toDomain converts account with one colon to level 1`() {
        val account = TestParsedLedgerAccount().apply {
            aname = "Assets:Cash"
        }

        val domain = account.toDomain()

        assertEquals("Assets:Cash", domain.name)
        assertEquals(1, domain.level)
    }

    @Test
    fun `toDomain converts account with multiple colons to correct level`() {
        val account = TestParsedLedgerAccount().apply {
            aname = "Assets:Bank:Checking"
        }

        val domain = account.toDomain()

        assertEquals("Assets:Bank:Checking", domain.name)
        assertEquals(2, domain.level)
    }

    @Test
    fun `toDomain sets isExpanded to false`() {
        val account = TestParsedLedgerAccount().apply {
            aname = "Assets"
        }

        val domain = account.toDomain()

        assertEquals(false, domain.isExpanded)
    }

    @Test
    fun `toDomain sets isVisible to true`() {
        val account = TestParsedLedgerAccount().apply {
            aname = "Assets"
        }

        val domain = account.toDomain()

        assertEquals(true, domain.isVisible)
    }

    @Test
    fun `toDomain sets id to null`() {
        val account = TestParsedLedgerAccount().apply {
            aname = "Assets"
        }

        val domain = account.toDomain()

        assertNull(domain.id)
    }

    @Test
    fun `toDomain aggregates single balance`() {
        val account = TestParsedLedgerAccount(
            listOf(
                ParsedLedgerAccount.SimpleBalance("USD", 100.50f)
            )
        ).apply {
            aname = "Assets"
        }

        val domain = account.toDomain()

        assertEquals(1, domain.amounts.size)
        assertEquals("USD", domain.amounts[0].currency)
        assertEquals(100.50f, domain.amounts[0].amount, 0.01f)
    }

    @Test
    fun `toDomain aggregates multiple balances of different commodities`() {
        val account = TestParsedLedgerAccount(
            listOf(
                ParsedLedgerAccount.SimpleBalance("USD", 100f),
                ParsedLedgerAccount.SimpleBalance("EUR", 50f)
            )
        ).apply {
            aname = "Assets"
        }

        val domain = account.toDomain()

        assertEquals(2, domain.amounts.size)
        val usd = domain.amounts.find { it.currency == "USD" }
        val eur = domain.amounts.find { it.currency == "EUR" }
        assertEquals(100f, usd?.amount ?: 0f, 0.01f)
        assertEquals(50f, eur?.amount ?: 0f, 0.01f)
    }

    @Test
    fun `toDomain aggregates multiple balances of same commodity`() {
        val account = TestParsedLedgerAccount(
            listOf(
                ParsedLedgerAccount.SimpleBalance("USD", 100f),
                ParsedLedgerAccount.SimpleBalance("USD", 50f)
            )
        ).apply {
            aname = "Assets"
        }

        val domain = account.toDomain()

        assertEquals(1, domain.amounts.size)
        assertEquals("USD", domain.amounts[0].currency)
        assertEquals(150f, domain.amounts[0].amount, 0.01f)
    }

    @Test
    fun `toDomain handles empty balances`() {
        val account = TestParsedLedgerAccount(emptyList()).apply {
            aname = "Assets"
        }

        val domain = account.toDomain()

        assertTrue(domain.amounts.isEmpty())
    }

    @Test
    fun `toDomain handles negative balances`() {
        val account = TestParsedLedgerAccount(
            listOf(
                ParsedLedgerAccount.SimpleBalance("USD", -100f)
            )
        ).apply {
            aname = "Liabilities"
        }

        val domain = account.toDomain()

        assertEquals(-100f, domain.amounts[0].amount, 0.01f)
    }

    @Test
    fun `SimpleBalance constructor with two args sets amountStyle to null`() {
        val balance = ParsedLedgerAccount.SimpleBalance("USD", 100f)

        assertNull(balance.amountStyle)
    }

    @Test
    fun `SimpleBalance constructor with three args includes amountStyle`() {
        val style = AmountStyle(
            commodityPosition = AmountStyle.Position.BEFORE,
            isCommoditySpaced = true,
            precision = 2,
            decimalMark = "."
        )
        val balance = ParsedLedgerAccount.SimpleBalance("USD", 100f, style)

        assertEquals(style, balance.amountStyle)
    }

    @Test
    fun `SimpleBalance is data class with equals`() {
        val balance1 = ParsedLedgerAccount.SimpleBalance("USD", 100f, null)
        val balance2 = ParsedLedgerAccount.SimpleBalance("USD", 100f, null)

        assertEquals(balance1, balance2)
    }

    @Test
    fun `anumpostings default value is 0`() {
        val account = TestParsedLedgerAccount()

        assertEquals(0, account.anumpostings)
    }

    @Test
    fun `aname default value is empty string`() {
        val account = TestParsedLedgerAccount()

        assertEquals("", account.aname)
    }
}
