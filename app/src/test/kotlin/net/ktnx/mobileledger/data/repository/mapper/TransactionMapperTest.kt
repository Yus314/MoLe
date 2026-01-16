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

package net.ktnx.mobileledger.data.repository.mapper

import net.ktnx.mobileledger.db.Transaction as DbTransaction
import net.ktnx.mobileledger.db.TransactionAccount
import net.ktnx.mobileledger.db.TransactionWithAccounts
import net.ktnx.mobileledger.domain.model.Transaction
import net.ktnx.mobileledger.domain.model.TransactionLine
import net.ktnx.mobileledger.utils.SimpleDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TransactionMapperTest {

    @Test
    fun `toDomain maps basic transaction`() {
        val dbTransactionWithAccounts = createDbTransaction(
            id = 123L,
            ledgerId = 456L,
            year = 2026,
            month = 1,
            day = 16,
            description = "Test Transaction",
            comment = "A comment",
            accounts = listOf(
                createDbAccount(1L, "Assets:Cash", 100f, "USD", "Note 1"),
                createDbAccount(2L, "Expenses:Food", -100f, "USD", null)
            )
        )

        val result = TransactionMapper.toDomain(dbTransactionWithAccounts)

        assertEquals(123L, result.id)
        assertEquals(456L, result.ledgerId)
        assertEquals(SimpleDate(2026, 1, 16), result.date)
        assertEquals("Test Transaction", result.description)
        assertEquals("A comment", result.comment)
        assertEquals(2, result.lines.size)

        val line1 = result.lines[0]
        assertEquals(1L, line1.id)
        assertEquals("Assets:Cash", line1.accountName)
        assertEquals(100f, line1.amount)
        assertEquals("USD", line1.currency)
        assertEquals("Note 1", line1.comment)

        val line2 = result.lines[1]
        assertEquals(2L, line2.id)
        assertEquals("Expenses:Food", line2.accountName)
        assertEquals(-100f, line2.amount)
        assertEquals("USD", line2.currency)
        assertNull(line2.comment)
    }

    @Test
    fun `toDomain handles null comment`() {
        val dbTransactionWithAccounts = createDbTransaction(
            id = 1L,
            ledgerId = 1L,
            year = 2026,
            month = 1,
            day = 1,
            description = "Test",
            comment = null,
            accounts = emptyList()
        )

        val result = TransactionMapper.toDomain(dbTransactionWithAccounts)

        assertNull(result.comment)
    }

    @Test
    fun `toDomain handles empty accounts list`() {
        val dbTransactionWithAccounts = createDbTransaction(
            id = 1L,
            ledgerId = 1L,
            year = 2026,
            month = 1,
            day = 1,
            description = "Test",
            comment = null,
            accounts = emptyList()
        )

        val result = TransactionMapper.toDomain(dbTransactionWithAccounts)

        assertEquals(emptyList<TransactionLine>(), result.lines)
    }

    @Test
    fun `toDomain handles null currency as empty string`() {
        val dbTransactionWithAccounts = createDbTransaction(
            id = 1L,
            ledgerId = 1L,
            year = 2026,
            month = 1,
            day = 1,
            description = "Test",
            comment = null,
            accounts = listOf(
                createDbAccount(1L, "Assets:Cash", 100f, null, null)
            )
        )

        val result = TransactionMapper.toDomain(dbTransactionWithAccounts)

        assertEquals("", result.lines[0].currency)
    }

    // Helper functions to create test data

    private fun createDbTransaction(
        id: Long,
        ledgerId: Long,
        year: Int,
        month: Int,
        day: Int,
        description: String,
        comment: String?,
        accounts: List<TransactionAccount>
    ): TransactionWithAccounts = TransactionWithAccounts().apply {
        transaction = DbTransaction().apply {
            this.id = id
            this.ledgerId = ledgerId
            this.year = year
            this.month = month
            this.day = day
            this.description = description
            this.comment = comment
        }
        this.accounts = accounts
    }

    private fun createDbAccount(
        id: Long,
        accountName: String,
        amount: Float,
        currency: String?,
        comment: String?
    ): TransactionAccount = TransactionAccount().apply {
        this.id = id
        this.accountName = accountName
        this.amount = amount
        this.currency = currency ?: ""
        this.comment = comment
    }
}
