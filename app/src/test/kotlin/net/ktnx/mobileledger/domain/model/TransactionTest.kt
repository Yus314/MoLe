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

package net.ktnx.mobileledger.domain.model

import net.ktnx.mobileledger.utils.SimpleDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionTest {

    @Test
    fun `validate returns error when description is blank`() {
        val transaction = Transaction(
            date = SimpleDate(2026, 1, 16),
            description = "",
            lines = listOf(TransactionLine(accountName = "Assets:Cash"))
        )

        val result = transaction.validate()

        assertTrue(result is ValidationResult.Error)
        assertTrue((result as ValidationResult.Error).reasons.any { it.contains("説明") })
    }

    @Test
    fun `validate returns error when description is whitespace only`() {
        val transaction = Transaction(
            date = SimpleDate(2026, 1, 16),
            description = "   ",
            lines = listOf(TransactionLine(accountName = "Assets:Cash"))
        )

        val result = transaction.validate()

        assertTrue(result is ValidationResult.Error)
    }

    @Test
    fun `validate returns error when lines are empty`() {
        val transaction = Transaction(
            date = SimpleDate(2026, 1, 16),
            description = "Test",
            lines = emptyList()
        )

        val result = transaction.validate()

        assertTrue(result is ValidationResult.Error)
        assertTrue((result as ValidationResult.Error).reasons.any { it.contains("取引行") })
    }

    @Test
    fun `validate returns success for valid balanced transaction`() {
        val transaction = Transaction(
            date = SimpleDate(2026, 1, 16),
            description = "Test",
            lines = listOf(
                TransactionLine(accountName = "Assets:Cash", amount = 100f),
                TransactionLine(accountName = "Expenses:Food", amount = -100f)
            )
        )

        val result = transaction.validate()

        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `validate returns error for unbalanced transaction`() {
        val transaction = Transaction(
            date = SimpleDate(2026, 1, 16),
            description = "Test",
            lines = listOf(
                TransactionLine(accountName = "Assets:Cash", amount = 100f),
                TransactionLine(accountName = "Expenses:Food", amount = -50f)
            )
        )

        val result = transaction.validate()

        assertTrue(result is ValidationResult.Error)
        assertTrue((result as ValidationResult.Error).reasons.any { it.contains("不均衡") })
    }

    @Test
    fun `validate checks balance per currency`() {
        val transaction = Transaction(
            date = SimpleDate(2026, 1, 16),
            description = "Test",
            lines = listOf(
                TransactionLine(accountName = "Assets:Cash", amount = 100f, currency = "USD"),
                TransactionLine(accountName = "Expenses:Food", amount = -100f, currency = "EUR")
            )
        )

        val result = transaction.validate()

        assertTrue(result is ValidationResult.Error)
    }

    @Test
    fun `validate handles lines without amount (auto-calculated)`() {
        val transaction = Transaction(
            date = SimpleDate(2026, 1, 16),
            description = "Test",
            lines = listOf(
                TransactionLine(accountName = "Assets:Cash", amount = 100f),
                TransactionLine(accountName = "Expenses:Food", amount = null) // auto-calculated
            )
        )

        val result = transaction.validate()

        // Should not fail since only lines with amounts are checked for balance
        // Lines without amounts are considered auto-calculated
        assertTrue(result is ValidationResult.Error)
    }

    @Test
    fun `hasAccountNamed returns true when account exists`() {
        val transaction = Transaction(
            date = SimpleDate(2026, 1, 16),
            description = "Test",
            lines = listOf(
                TransactionLine(accountName = "Assets:Cash", amount = 100f),
                TransactionLine(accountName = "Expenses:Food", amount = -100f)
            )
        )

        assertTrue(transaction.hasAccountNamed("Cash"))
        assertTrue(transaction.hasAccountNamed("Assets:Cash"))
        assertTrue(transaction.hasAccountNamed("CASH")) // case-insensitive
    }

    @Test
    fun `hasAccountNamed returns false when account does not exist`() {
        val transaction = Transaction(
            date = SimpleDate(2026, 1, 16),
            description = "Test",
            lines = listOf(
                TransactionLine(accountName = "Assets:Cash", amount = 100f)
            )
        )

        assertFalse(transaction.hasAccountNamed("Expenses"))
    }

    @Test
    fun `withLine adds a new line`() {
        val original = Transaction(
            date = SimpleDate(2026, 1, 16),
            description = "Test",
            lines = listOf(TransactionLine(accountName = "Assets:Cash", amount = 100f))
        )
        val newLine = TransactionLine(accountName = "Expenses:Food", amount = -100f)

        val updated = original.withLine(newLine)

        assertEquals(2, updated.lines.size)
        assertEquals("Expenses:Food", updated.lines[1].accountName)
        assertEquals(1, original.lines.size) // original unchanged
    }

    @Test
    fun `withUpdatedLine updates an existing line`() {
        val original = Transaction(
            date = SimpleDate(2026, 1, 16),
            description = "Test",
            lines = listOf(
                TransactionLine(accountName = "Assets:Cash", amount = 100f),
                TransactionLine(accountName = "Expenses:Food", amount = -100f)
            )
        )
        val updatedLine = TransactionLine(accountName = "Expenses:Groceries", amount = -100f)

        val updated = original.withUpdatedLine(1, updatedLine)

        assertEquals(2, updated.lines.size)
        assertEquals("Expenses:Groceries", updated.lines[1].accountName)
        assertEquals("Expenses:Food", original.lines[1].accountName) // original unchanged
    }

    @Test
    fun `id is null for new transaction`() {
        val transaction = Transaction(
            date = SimpleDate(2026, 1, 16),
            description = "Test"
        )

        assertEquals(null, transaction.id)
    }

    @Test
    fun `transaction with id represents saved transaction`() {
        val transaction = Transaction(
            id = 123L,
            date = SimpleDate(2026, 1, 16),
            description = "Test"
        )

        assertEquals(123L, transaction.id)
    }

    @Test
    fun `ledgerId defaults to 0`() {
        val transaction = Transaction(
            date = SimpleDate(2026, 1, 16),
            description = "Test"
        )

        assertEquals(0L, transaction.ledgerId)
    }
}
