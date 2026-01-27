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

import net.ktnx.mobileledger.core.common.utils.SimpleDate
import net.ktnx.mobileledger.core.domain.model.*
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

        // Should pass because exactly one line has null amount (auto-balanceable)
        assertTrue(result is ValidationResult.Success)
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

    // Tests for balancePerCurrency

    @Test
    fun `balancePerCurrency returns empty map for transaction with no lines`() {
        val transaction = Transaction(
            date = SimpleDate(2026, 1, 16),
            description = "Test",
            lines = emptyList()
        )

        assertTrue(transaction.balancePerCurrency.isEmpty())
    }

    @Test
    fun `balancePerCurrency calculates sum correctly for single currency`() {
        val transaction = Transaction(
            date = SimpleDate(2026, 1, 16),
            description = "Test",
            lines = listOf(
                TransactionLine(accountName = "Assets:Cash", amount = 100f, currency = ""),
                TransactionLine(accountName = "Expenses:Food", amount = -50f, currency = ""),
                TransactionLine(accountName = "Expenses:Transport", amount = -30f, currency = "")
            )
        )

        assertEquals(1, transaction.balancePerCurrency.size)
        assertEquals(20f, transaction.balancePerCurrency[""]!!, 0.001f)
    }

    @Test
    fun `balancePerCurrency handles multiple currencies independently`() {
        val transaction = Transaction(
            date = SimpleDate(2026, 1, 16),
            description = "Test",
            lines = listOf(
                TransactionLine(accountName = "Assets:USD", amount = 100f, currency = "USD"),
                TransactionLine(accountName = "Assets:EUR", amount = 50f, currency = "EUR"),
                TransactionLine(accountName = "Expenses:Travel", amount = -100f, currency = "USD")
            )
        )

        assertEquals(2, transaction.balancePerCurrency.size)
        assertEquals(0f, transaction.balancePerCurrency["USD"]!!, 0.001f)
        assertEquals(50f, transaction.balancePerCurrency["EUR"]!!, 0.001f)
    }

    @Test
    fun `balancePerCurrency ignores lines with null amounts`() {
        val transaction = Transaction(
            date = SimpleDate(2026, 1, 16),
            description = "Test",
            lines = listOf(
                TransactionLine(accountName = "Assets:Cash", amount = 100f, currency = ""),
                TransactionLine(accountName = "Expenses:Food", amount = null, currency = "")
            )
        )

        assertEquals(1, transaction.balancePerCurrency.size)
        assertEquals(100f, transaction.balancePerCurrency[""]!!, 0.001f)
    }

    // Tests for isBalanced

    @Test
    fun `isBalanced returns true when amounts sum to zero`() {
        val transaction = Transaction(
            date = SimpleDate(2026, 1, 16),
            description = "Test",
            lines = listOf(
                TransactionLine(accountName = "Assets:Cash", amount = 100f),
                TransactionLine(accountName = "Expenses:Food", amount = -100f)
            )
        )

        assertTrue(transaction.isBalanced)
    }

    @Test
    fun `isBalanced returns true when exactly one line has null amount`() {
        val transaction = Transaction(
            date = SimpleDate(2026, 1, 16),
            description = "Test",
            lines = listOf(
                TransactionLine(accountName = "Assets:Cash", amount = 100f),
                TransactionLine(accountName = "Expenses:Food", amount = null)
            )
        )

        assertTrue(transaction.isBalanced)
    }

    @Test
    fun `isBalanced returns false when unbalanced and multiple null amounts`() {
        val transaction = Transaction(
            date = SimpleDate(2026, 1, 16),
            description = "Test",
            lines = listOf(
                TransactionLine(accountName = "Assets:Cash", amount = 100f),
                TransactionLine(accountName = "Expenses:Food", amount = null),
                TransactionLine(accountName = "Expenses:Transport", amount = null)
            )
        )

        assertFalse(transaction.isBalanced)
    }

    @Test
    fun `isBalanced treats near-zero balance as balanced`() {
        val transaction = Transaction(
            date = SimpleDate(2026, 1, 16),
            description = "Test",
            lines = listOf(
                TransactionLine(accountName = "Assets:Cash", amount = 100f),
                TransactionLine(accountName = "Expenses:Food", amount = -100.001f)
            )
        )

        // Should be balanced because difference is within BALANCE_EPSILON (0.005)
        assertTrue(transaction.isBalanced)
    }

    @Test
    fun `isBalanced returns false when balance exceeds epsilon`() {
        val transaction = Transaction(
            date = SimpleDate(2026, 1, 16),
            description = "Test",
            lines = listOf(
                TransactionLine(accountName = "Assets:Cash", amount = 100f),
                TransactionLine(accountName = "Expenses:Food", amount = -99f)
            )
        )

        assertFalse(transaction.isBalanced)
    }

    // Tests for getAutoBalanceAmount

    @Test
    fun `getAutoBalanceAmount returns negative of balance`() {
        val transaction = Transaction(
            date = SimpleDate(2026, 1, 16),
            description = "Test",
            lines = listOf(
                TransactionLine(accountName = "Assets:Cash", amount = 100f, currency = ""),
                TransactionLine(accountName = "Expenses:Food", amount = null, currency = "")
            )
        )

        assertEquals(-100f, transaction.getAutoBalanceAmount("")!!, 0.001f)
    }

    @Test
    fun `getAutoBalanceAmount returns null for multiple empty lines`() {
        val transaction = Transaction(
            date = SimpleDate(2026, 1, 16),
            description = "Test",
            lines = listOf(
                TransactionLine(accountName = "Assets:Cash", amount = 100f, currency = ""),
                TransactionLine(accountName = "Expenses:Food", amount = null, currency = ""),
                TransactionLine(accountName = "Expenses:Transport", amount = null, currency = "")
            )
        )

        assertEquals(null, transaction.getAutoBalanceAmount(""))
    }

    @Test
    fun `getAutoBalanceAmount returns null for no empty lines`() {
        val transaction = Transaction(
            date = SimpleDate(2026, 1, 16),
            description = "Test",
            lines = listOf(
                TransactionLine(accountName = "Assets:Cash", amount = 100f, currency = ""),
                TransactionLine(accountName = "Expenses:Food", amount = -100f, currency = "")
            )
        )

        assertEquals(null, transaction.getAutoBalanceAmount(""))
    }

    // Tests for withAutoBalance

    @Test
    fun `withAutoBalance fills single empty amount per currency`() {
        val transaction = Transaction(
            date = SimpleDate(2026, 1, 16),
            description = "Test",
            lines = listOf(
                TransactionLine(accountName = "Assets:Cash", amount = 100f, currency = ""),
                TransactionLine(accountName = "Expenses:Food", amount = null, currency = "")
            )
        )

        val autoBalanced = transaction.withAutoBalance()

        assertEquals(2, autoBalanced.lines.size)
        assertEquals(-100f, autoBalanced.lines[1].amount!!, 0.001f)
    }

    @Test
    fun `withAutoBalance does not modify when multiple empty amounts exist`() {
        val transaction = Transaction(
            date = SimpleDate(2026, 1, 16),
            description = "Test",
            lines = listOf(
                TransactionLine(accountName = "Assets:Cash", amount = 100f, currency = ""),
                TransactionLine(accountName = "Expenses:Food", amount = null, currency = ""),
                TransactionLine(accountName = "Expenses:Transport", amount = null, currency = "")
            )
        )

        val autoBalanced = transaction.withAutoBalance()

        // Should not modify since there are multiple empty amounts
        assertEquals(null, autoBalanced.lines[1].amount)
        assertEquals(null, autoBalanced.lines[2].amount)
    }

    @Test
    fun `withAutoBalance handles multiple currencies independently`() {
        val transaction = Transaction(
            date = SimpleDate(2026, 1, 16),
            description = "Test",
            lines = listOf(
                TransactionLine(accountName = "Assets:USD", amount = 100f, currency = "USD"),
                TransactionLine(accountName = "Assets:EUR", amount = 50f, currency = "EUR"),
                TransactionLine(accountName = "Expenses:USD", amount = null, currency = "USD"),
                TransactionLine(accountName = "Expenses:EUR", amount = null, currency = "EUR")
            )
        )

        val autoBalanced = transaction.withAutoBalance()

        assertEquals(-100f, autoBalanced.lines[2].amount!!, 0.001f)
        assertEquals(-50f, autoBalanced.lines[3].amount!!, 0.001f)
    }

    @Test
    fun `withAutoBalance does not modify original transaction`() {
        val original = Transaction(
            date = SimpleDate(2026, 1, 16),
            description = "Test",
            lines = listOf(
                TransactionLine(accountName = "Assets:Cash", amount = 100f, currency = ""),
                TransactionLine(accountName = "Expenses:Food", amount = null, currency = "")
            )
        )

        val autoBalanced = original.withAutoBalance()

        // Original should be unchanged
        assertEquals(null, original.lines[1].amount)
        // New transaction should have auto-balanced amount
        assertEquals(-100f, autoBalanced.lines[1].amount!!, 0.001f)
    }

    @Test
    fun `withAutoBalance ignores lines with blank account names`() {
        val transaction = Transaction(
            date = SimpleDate(2026, 1, 16),
            description = "Test",
            lines = listOf(
                TransactionLine(accountName = "Assets:Cash", amount = 100f, currency = ""),
                TransactionLine(accountName = "", amount = null, currency = ""), // blank account name
                TransactionLine(accountName = "Expenses:Food", amount = null, currency = "")
            )
        )

        val autoBalanced = transaction.withAutoBalance()

        // Only one line with non-blank account name has null amount, so it should be filled
        assertEquals(-100f, autoBalanced.lines[2].amount!!, 0.001f)
        // Blank account name line should remain unchanged
        assertEquals(null, autoBalanced.lines[1].amount)
    }

    // Test for validate() using unified BALANCE_EPSILON

    @Test
    fun `validate uses unified BALANCE_EPSILON threshold`() {
        // Transaction with small imbalance within BALANCE_EPSILON (0.005)
        val transaction = Transaction(
            date = SimpleDate(2026, 1, 16),
            description = "Test",
            lines = listOf(
                TransactionLine(accountName = "Assets:Cash", amount = 100f),
                TransactionLine(accountName = "Expenses:Food", amount = -100.003f)
            )
        )

        val result = transaction.validate()

        // Should pass because 0.003 < 0.005 (BALANCE_EPSILON)
        assertTrue(result is ValidationResult.Success)
    }
}
