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

import net.ktnx.mobileledger.core.common.utils.SimpleDate
import net.ktnx.mobileledger.core.data.mapper.TransactionMapper
import net.ktnx.mobileledger.core.database.entity.Transaction as DbTransaction
import net.ktnx.mobileledger.core.database.entity.TransactionAccount
import net.ktnx.mobileledger.core.database.entity.TransactionWithAccounts
import net.ktnx.mobileledger.core.domain.model.Transaction
import net.ktnx.mobileledger.core.domain.model.TransactionLine
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

    // === toEntity Tests ===

    @Test
    fun `toEntity maps new transaction with null id to entity with id=0`() {
        val domainTransaction = Transaction(
            id = null,
            ledgerId = 789L,
            date = SimpleDate(2026, 3, 15),
            description = "New Transaction",
            comment = "New comment",
            lines = listOf(
                TransactionLine(
                    id = null,
                    accountName = "Assets:Bank",
                    amount = 500f,
                    currency = "EUR",
                    comment = "Line note"
                ),
                TransactionLine(
                    id = null,
                    accountName = "Income:Salary",
                    amount = -500f,
                    currency = "EUR",
                    comment = null
                )
            )
        )
        val profileId = 42L

        val result = TransactionMapper.toEntity(domainTransaction, profileId)

        assertEquals(0L, result.transaction.id)
        assertEquals(789L, result.transaction.ledgerId)
        assertEquals(profileId, result.transaction.profileId)
        assertEquals(2026, result.transaction.year)
        assertEquals(3, result.transaction.month)
        assertEquals(15, result.transaction.day)
        assertEquals("New Transaction", result.transaction.description)
        assertEquals("New comment", result.transaction.comment)
        assertEquals(2, result.accounts.size)
    }

    @Test
    fun `toEntity maps existing transaction with id to entity`() {
        val domainTransaction = Transaction(
            id = 123L,
            ledgerId = 456L,
            date = SimpleDate(2026, 1, 16),
            description = "Existing Transaction",
            comment = null,
            lines = listOf(
                TransactionLine(id = 10L, accountName = "Assets:Cash", amount = 100f, currency = "USD", comment = null)
            )
        )
        val profileId = 1L

        val result = TransactionMapper.toEntity(domainTransaction, profileId)

        assertEquals(123L, result.transaction.id)
        assertEquals(456L, result.transaction.ledgerId)
        assertEquals(profileId, result.transaction.profileId)
        assertEquals("Existing Transaction", result.transaction.description)
        assertNull(result.transaction.comment)
        assertEquals(1, result.accounts.size)
    }

    @Test
    fun `toEntity sets orderNo based on line index`() {
        val domainTransaction = Transaction(
            id = null,
            ledgerId = 0L,
            date = SimpleDate(2026, 1, 1),
            description = "Test",
            lines = listOf(
                TransactionLine(accountName = "Account1"),
                TransactionLine(accountName = "Account2"),
                TransactionLine(accountName = "Account3")
            )
        )
        val profileId = 1L

        val result = TransactionMapper.toEntity(domainTransaction, profileId)

        assertEquals(3, result.accounts.size)
        assertEquals(1, result.accounts[0].orderNo)
        assertEquals(2, result.accounts[1].orderNo)
        assertEquals(3, result.accounts[2].orderNo)
    }

    @Test
    fun `toEntity maps TransactionLine fields correctly`() {
        val domainTransaction = Transaction(
            id = 1L,
            ledgerId = 1L,
            date = SimpleDate(2026, 1, 1),
            description = "Test",
            lines = listOf(
                TransactionLine(
                    id = 50L,
                    accountName = "Assets:Cash",
                    amount = 123.45f,
                    currency = "JPY",
                    comment = "Test comment"
                )
            )
        )
        val profileId = 1L

        val result = TransactionMapper.toEntity(domainTransaction, profileId)

        val account = result.accounts[0]
        assertEquals(50L, account.id)
        assertEquals("Assets:Cash", account.accountName)
        assertEquals(123.45f, account.amount)
        assertEquals("JPY", account.currency)
        assertEquals("Test comment", account.comment)
        assertEquals(1, account.orderNo)
    }

    @Test
    fun `toEntity handles null amount as 0f`() {
        val domainTransaction = Transaction(
            id = 1L,
            ledgerId = 1L,
            date = SimpleDate(2026, 1, 1),
            description = "Test",
            lines = listOf(
                TransactionLine(accountName = "Assets:Cash", amount = null)
            )
        )
        val profileId = 1L

        val result = TransactionMapper.toEntity(domainTransaction, profileId)

        assertEquals(0f, result.accounts[0].amount)
    }

    @Test
    fun `roundTrip preserves transaction data`() {
        val original = Transaction(
            id = 100L,
            ledgerId = 200L,
            date = SimpleDate(2026, 6, 15),
            description = "Round Trip Test",
            comment = "Original comment",
            lines = listOf(
                TransactionLine(
                    id = 1L,
                    accountName = "Assets:Bank",
                    amount = 1000f,
                    currency = "USD",
                    comment = "Line 1"
                ),
                TransactionLine(
                    id = 2L,
                    accountName = "Expenses:Food",
                    amount = -1000f,
                    currency = "USD",
                    comment = null
                )
            )
        )
        val profileId = 5L

        val entity = TransactionMapper.toEntity(original, profileId)
        val roundTripped = TransactionMapper.toDomain(entity)

        assertEquals(original.id, roundTripped.id)
        assertEquals(original.ledgerId, roundTripped.ledgerId)
        assertEquals(original.date, roundTripped.date)
        assertEquals(original.description, roundTripped.description)
        assertEquals(original.comment, roundTripped.comment)
        assertEquals(original.lines.size, roundTripped.lines.size)

        for (i in original.lines.indices) {
            assertEquals(original.lines[i].id, roundTripped.lines[i].id)
            assertEquals(original.lines[i].accountName, roundTripped.lines[i].accountName)
            assertEquals(original.lines[i].amount, roundTripped.lines[i].amount)
            assertEquals(original.lines[i].currency, roundTripped.lines[i].currency)
            assertEquals(original.lines[i].comment, roundTripped.lines[i].comment)
        }
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
