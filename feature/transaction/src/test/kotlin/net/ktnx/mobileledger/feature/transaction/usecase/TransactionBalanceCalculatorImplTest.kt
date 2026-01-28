/*
 * Copyright © 2024 Damyan Ivanov.
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

package net.ktnx.mobileledger.feature.transaction.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TransactionBalanceCalculatorImplTest {

    private lateinit var calculator: TransactionBalanceCalculatorImpl

    @Before
    fun setup() {
        calculator = TransactionBalanceCalculatorImpl()
    }

    // ========== calculateBalance tests ==========

    @Test
    fun `calculateBalance with two balanced accounts returns balanced result`() {
        val entries = listOf(
            entry("Assets:Bank", 100f, "USD"),
            entry("Expenses:Food", -100f, "USD")
        )

        val result = calculator.calculateBalance(entries)

        assertEquals(2, result.lines.size)
        assertTrue(result.isBalanced)
        assertEquals(0f, result.balancePerCurrency["USD"] ?: 0f, 0.01f)
    }

    @Test
    fun `calculateBalance auto-fills single empty amount`() {
        val entries = listOf(
            entry("Assets:Bank", 100f, "USD"),
            entry("Expenses:Food", null, "USD", isAmountSet = false)
        )

        val result = calculator.calculateBalance(entries)

        assertEquals(2, result.lines.size)
        assertEquals(-100f, result.lines[1].amount ?: 0f, 0.01f)
        assertTrue(result.isBalanced)
    }

    @Test
    fun `calculateBalance handles multiple currencies independently`() {
        val entries = listOf(
            entry("Assets:Bank:USD", 100f, "USD"),
            entry("Expenses:Food", null, "USD", isAmountSet = false),
            entry("Assets:Bank:EUR", 50f, "EUR"),
            entry("Expenses:Travel", -50f, "EUR")
        )

        val result = calculator.calculateBalance(entries)

        assertEquals(4, result.lines.size)
        // USD auto-balanced
        assertEquals(-100f, result.lines[1].amount ?: 0f, 0.01f)
        // EUR already balanced
        assertEquals(-50f, result.lines[3].amount ?: 0f, 0.01f)
        assertTrue(result.isBalanced)
    }

    @Test
    fun `calculateBalance skips entries with blank account names`() {
        val entries = listOf(
            entry("Assets:Bank", 100f, "USD"),
            entry("", null, "USD", isAmountSet = false),
            entry("Expenses:Food", -100f, "USD")
        )

        val result = calculator.calculateBalance(entries)

        assertEquals(2, result.lines.size)
        assertEquals("Assets:Bank", result.lines[0].accountName)
        assertEquals("Expenses:Food", result.lines[1].accountName)
    }

    @Test
    fun `calculateBalance does not auto-fill when multiple empty amounts exist`() {
        val entries = listOf(
            entry("Assets:Bank", 100f, "USD"),
            entry("Expenses:Food", null, "USD", isAmountSet = false),
            entry("Expenses:Travel", null, "USD", isAmountSet = false)
        )

        val result = calculator.calculateBalance(entries)

        assertEquals(3, result.lines.size)
        assertNull(result.lines[1].amount)
        assertNull(result.lines[2].amount)
        assertFalse(result.isBalanced)
    }

    @Test
    fun `calculateBalance trims account names`() {
        val entries = listOf(
            entry("  Assets:Bank  ", 100f, "USD"),
            entry("Expenses:Food", -100f, "USD")
        )

        val result = calculator.calculateBalance(entries)

        assertEquals("Assets:Bank", result.lines[0].accountName)
    }

    @Test
    fun `calculateBalance handles empty comment as null`() {
        val entries = listOf(
            entry("Assets:Bank", 100f, "USD", comment = ""),
            entry("Expenses:Food", -100f, "USD", comment = "some comment")
        )

        val result = calculator.calculateBalance(entries)

        assertNull(result.lines[0].comment)
        assertEquals("some comment", result.lines[1].comment)
    }

    @Test
    fun `calculateBalance treats near-zero balance as balanced`() {
        val entries = listOf(
            entry("Assets:Bank", 100.001f, "USD"),
            entry("Expenses:Food", -100.003f, "USD")
        )

        val result = calculator.calculateBalance(entries)

        assertTrue(result.isBalanced)
    }

    // ========== calculateAmountHints tests ==========

    @Test
    fun `calculateAmountHints returns hint for empty amount`() {
        val entries = listOf(
            entry("Assets:Bank", 100f, "USD"),
            entry("Expenses:Food", null, "USD", isAmountSet = false)
        )

        val hints = calculator.calculateAmountHints(entries) { it.toInt().toString() }

        assertNull(hints[0].hint)
        assertEquals("-100", hints[1].hint)
    }

    @Test
    fun `calculateAmountHints returns zero hint for balanced transaction`() {
        val entries = listOf(
            entry("Assets:Bank", 100f, "USD"),
            entry("Expenses:Food", -100f, "USD"),
            entry("Expenses:Other", null, "USD", isAmountSet = false)
        )

        val hints = calculator.calculateAmountHints(entries) { it.toString() }

        assertEquals("0", hints[2].hint)
    }

    @Test
    fun `calculateAmountHints handles multiple currencies`() {
        val entries = listOf(
            entry("Assets:Bank:USD", 100f, "USD"),
            entry("Expenses:Food", null, "USD", isAmountSet = false),
            entry("Assets:Bank:EUR", 50f, "EUR"),
            entry("Expenses:Travel", null, "EUR", isAmountSet = false)
        )

        val hints = calculator.calculateAmountHints(entries) { it.toInt().toString() }

        assertEquals("-100", hints[1].hint)
        assertEquals("-50", hints[3].hint)
    }

    @Test
    fun `calculateAmountHints returns null for entries with amount set`() {
        val entries = listOf(
            entry("Assets:Bank", 100f, "USD"),
            entry("Expenses:Food", -50f, "USD")
        )

        val hints = calculator.calculateAmountHints(entries) { it.toString() }

        assertNull(hints[0].hint)
        assertNull(hints[1].hint)
    }

    // ========== isBalanceable tests ==========

    @Test
    fun `isBalanceable returns true for balanced transaction`() {
        val entries = listOf(
            entry("Assets:Bank", 100f, "USD"),
            entry("Expenses:Food", -100f, "USD")
        )

        assertTrue(calculator.isBalanceable(entries))
    }

    @Test
    fun `isBalanceable returns true when one empty amount can auto-balance`() {
        val entries = listOf(
            entry("Assets:Bank", 100f, "USD"),
            entry("Expenses:Food", null, "USD", isAmountSet = false)
        )

        assertTrue(calculator.isBalanceable(entries))
    }

    @Test
    fun `isBalanceable returns false when multiple empty amounts exist`() {
        val entries = listOf(
            entry("Assets:Bank", 100f, "USD"),
            entry("Expenses:Food", null, "USD", isAmountSet = false),
            entry("Expenses:Travel", null, "USD", isAmountSet = false)
        )

        assertFalse(calculator.isBalanceable(entries))
    }

    @Test
    fun `isBalanceable returns true for empty currency group`() {
        val entries = listOf(
            entry("Assets:Bank", null, "USD", isAmountSet = false),
            entry("Expenses:Food", null, "USD", isAmountSet = false)
        )

        assertTrue(calculator.isBalanceable(entries))
    }

    @Test
    fun `isBalanceable handles mixed currencies correctly`() {
        val entries = listOf(
            // USD: balanced
            entry("Assets:Bank:USD", 100f, "USD"),
            entry("Expenses:Food", -100f, "USD"),
            // EUR: can auto-balance
            entry("Assets:Bank:EUR", 50f, "EUR"),
            entry("Expenses:Travel", null, "EUR", isAmountSet = false)
        )

        assertTrue(calculator.isBalanceable(entries))
    }

    @Test
    fun `isBalanceable returns false if any currency group cannot balance`() {
        val entries = listOf(
            // USD: balanced
            entry("Assets:Bank:USD", 100f, "USD"),
            entry("Expenses:Food", -100f, "USD"),
            // EUR: cannot auto-balance (two empty)
            entry("Assets:Bank:EUR", 50f, "EUR"),
            entry("Expenses:Travel", null, "EUR", isAmountSet = false),
            entry("Expenses:Other", null, "EUR", isAmountSet = false)
        )

        assertFalse(calculator.isBalanceable(entries))
    }

    @Test
    fun `isBalanceable ignores entries with blank account names`() {
        val entries = listOf(
            entry("Assets:Bank", 100f, "USD"),
            entry("", null, "USD", isAmountSet = false),
            entry("Expenses:Food", null, "USD", isAmountSet = false)
        )

        // Only one entry with non-blank name has no amount
        assertTrue(calculator.isBalanceable(entries))
    }

    // ========== Helper functions ==========

    private fun entry(
        name: String,
        amount: Float?,
        currency: String,
        isAmountSet: Boolean = amount != null,
        comment: String? = null
    ) = TransactionBalanceCalculator.AccountEntry(
        accountName = name,
        amount = amount,
        currency = currency,
        comment = comment,
        isAmountSet = isAmountSet,
        isAmountValid = true
    )
}
