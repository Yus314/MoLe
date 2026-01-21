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

package net.ktnx.mobileledger.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [TransactionAccount] Room entity.
 */
class TransactionAccountEntityTest {

    @Test
    fun `default values are correct`() {
        val account = TransactionAccount()

        assertEquals(0L, account.id)
        assertEquals(0L, account.transactionId)
        assertEquals(0, account.orderNo)
        assertEquals("", account.accountName)
        assertEquals("", account.currency)
        assertEquals(0f, account.amount, 0.001f)
        assertNull(account.comment)
        assertNull(account.amountStyle)
        assertEquals(0L, account.generation)
    }

    @Test
    fun `all fields can be set and read`() {
        val account = TransactionAccount()

        account.id = 10L
        account.transactionId = 20L
        account.orderNo = 1
        account.accountName = "Expenses:Food"
        account.currency = "USD"
        account.amount = 100.50f
        account.comment = "Groceries"
        account.amountStyle = "1,234.56"
        account.generation = 5L

        assertEquals(10L, account.id)
        assertEquals(20L, account.transactionId)
        assertEquals(1, account.orderNo)
        assertEquals("Expenses:Food", account.accountName)
        assertEquals("USD", account.currency)
        assertEquals(100.50f, account.amount, 0.001f)
        assertEquals("Groceries", account.comment)
        assertEquals("1,234.56", account.amountStyle)
        assertEquals(5L, account.generation)
    }

    @Test
    fun `copyDataFrom copies all fields except id`() {
        val source = TransactionAccount().apply {
            id = 999L
            transactionId = 42L
            orderNo = 2
            accountName = "Assets:Cash"
            currency = "EUR"
            amount = 250.00f
            comment = "Withdrawal"
            amountStyle = "1.234,56"
            generation = 15L
        }

        val target = TransactionAccount()
        target.id = 1L

        target.copyDataFrom(source)

        // id should NOT be copied
        assertEquals(1L, target.id)

        // all other fields should be copied
        assertEquals(42L, target.transactionId)
        assertEquals(2, target.orderNo)
        assertEquals("Assets:Cash", target.accountName)
        assertEquals("EUR", target.currency)
        assertEquals(250.00f, target.amount, 0.001f)
        assertEquals("Withdrawal", target.comment)
        assertEquals("1.234,56", target.amountStyle)
        assertEquals(15L, target.generation)
    }

    @Test
    fun `copyDataFrom handles null currency`() {
        val source = TransactionAccount().apply {
            currency = ""
        }

        val target = TransactionAccount()
        target.currency = "USD"

        target.copyDataFrom(source)

        // Misc.nullIsEmpty should convert empty string to empty string
        assertEquals("", target.currency)
    }

    @Test
    fun `copyDataFrom handles null comment`() {
        val source = TransactionAccount().apply {
            comment = null
        }

        val target = TransactionAccount()
        target.comment = "existing comment"

        target.copyDataFrom(source)

        assertNull(target.comment)
    }

    @Test
    fun `copyDataFrom handles null amountStyle`() {
        val source = TransactionAccount().apply {
            amountStyle = null
        }

        val target = TransactionAccount()
        target.amountStyle = "existing style"

        target.copyDataFrom(source)

        assertNull(target.amountStyle)
    }

    @Test
    fun `amount can be negative`() {
        val account = TransactionAccount()

        account.amount = -500.75f

        assertEquals(-500.75f, account.amount, 0.001f)
    }

    @Test
    fun `amount can be zero`() {
        val account = TransactionAccount()
        account.amount = 100f

        account.amount = 0f

        assertEquals(0f, account.amount, 0.001f)
    }

    @Test
    fun `orderNo can be various values`() {
        val account1 = TransactionAccount()
        account1.orderNo = 0
        assertEquals(0, account1.orderNo)

        val account2 = TransactionAccount()
        account2.orderNo = 1
        assertEquals(1, account2.orderNo)

        val account3 = TransactionAccount()
        account3.orderNo = 100
        assertEquals(100, account3.orderNo)
    }

    @Test
    fun `accountName can contain hierarchical path`() {
        val account = TransactionAccount()

        account.accountName = "Expenses:Food:Groceries:Supermarket"

        assertEquals("Expenses:Food:Groceries:Supermarket", account.accountName)
    }

    @Test
    fun `currency can be unicode symbol`() {
        val account = TransactionAccount()

        account.currency = "€"

        assertEquals("€", account.currency)
    }

    @Test
    fun `currency can be multi-character code`() {
        val account = TransactionAccount()

        account.currency = "JPY"

        assertEquals("JPY", account.currency)
    }
}
