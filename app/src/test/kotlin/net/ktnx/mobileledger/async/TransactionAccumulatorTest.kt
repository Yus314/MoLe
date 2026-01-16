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

package net.ktnx.mobileledger.async

import net.ktnx.mobileledger.domain.model.Transaction
import net.ktnx.mobileledger.domain.model.TransactionLine
import net.ktnx.mobileledger.fake.FakeCurrencyFormatter
import net.ktnx.mobileledger.ui.main.TransactionListDisplayItem
import net.ktnx.mobileledger.utils.SimpleDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [TransactionAccumulator].
 *
 * These tests verify that the accumulator correctly calculates running totals
 * and uses the injected [CurrencyFormatter] for formatting.
 */
class TransactionAccumulatorTest {

    private lateinit var currencyFormatter: FakeCurrencyFormatter

    @Before
    fun setup() {
        currencyFormatter = FakeCurrencyFormatter()
    }

    // ========================================
    // Helper methods for creating test data
    // ========================================

    /**
     * Creates a test transaction with the given lines.
     */
    private fun createTransaction(
        ledgerId: Long,
        date: SimpleDate,
        description: String,
        lines: List<TransactionLine>
    ): Transaction = Transaction(
        id = null,
        ledgerId = ledgerId,
        date = date,
        description = description,
        comment = null,
        lines = lines
    )

    /**
     * Creates a simple transaction line.
     */
    private fun createLine(name: String, amount: Float, currency: String = "") = TransactionLine(
        accountName = name,
        amount = amount,
        currency = currency,
        comment = null
    )

    // ========================================
    // T007: Single transaction running total tests
    // ========================================

    @Test
    fun `single transaction calculates running total correctly`() {
        val accumulator = TransactionAccumulator(
            boldAccountName = "Expenses:Food",
            accumulateAccount = "Expenses:Food",
            currencyFormatter = currencyFormatter
        )

        val transaction = createTransaction(
            ledgerId = 1,
            date = SimpleDate(2026, 1, 15),
            description = "Lunch",
            lines = listOf(
                createLine("Expenses:Food", 25.50f),
                createLine("Assets:Cash", -25.50f)
            )
        )

        accumulator.put(transaction)
        val items = accumulator.getItems()

        // Should have: header + transaction + date delimiter
        assertTrue("Should have at least 2 items", items.size >= 2)

        // Find the transaction item
        val transactionItem = items.filterIsInstance<TransactionListDisplayItem.Transaction>().firstOrNull()
        assertNotNull("Should have a transaction item", transactionItem)
        assertEquals("25.50", transactionItem!!.runningTotal)
    }

    @Test
    fun `single transaction with no matching account has null running total`() {
        val accumulator = TransactionAccumulator(
            boldAccountName = null,
            accumulateAccount = null,
            currencyFormatter = currencyFormatter
        )

        val transaction = createTransaction(
            ledgerId = 1,
            date = SimpleDate(2026, 1, 15),
            description = "Lunch",
            lines = listOf(
                createLine("Expenses:Food", 25.50f),
                createLine("Assets:Cash", -25.50f)
            )
        )

        accumulator.put(transaction)
        val items = accumulator.getItems()

        val transactionItem = items.filterIsInstance<TransactionListDisplayItem.Transaction>().firstOrNull()
        assertNotNull(transactionItem)
        assertNull("Running total should be null when no accumulate account", transactionItem!!.runningTotal)
    }

    // ========================================
    // T008: Multiple transactions running total tests
    // ========================================

    @Test
    fun `multiple transactions accumulate running total correctly`() {
        val accumulator = TransactionAccumulator(
            boldAccountName = "Expenses:Food",
            accumulateAccount = "Expenses:Food",
            currencyFormatter = currencyFormatter
        )

        val transaction1 = createTransaction(
            ledgerId = 1,
            date = SimpleDate(2026, 1, 15),
            description = "Lunch",
            lines = listOf(
                createLine("Expenses:Food", 25.00f),
                createLine("Assets:Cash", -25.00f)
            )
        )

        val transaction2 = createTransaction(
            ledgerId = 2,
            date = SimpleDate(2026, 1, 16),
            description = "Dinner",
            lines = listOf(
                createLine("Expenses:Food", 35.50f),
                createLine("Assets:Cash", -35.50f)
            )
        )

        accumulator.put(transaction1)
        accumulator.put(transaction2)
        val items = accumulator.getItems()

        // Items are reversed in getItems(), so most recent comes first after header
        val transactions = items.filterIsInstance<TransactionListDisplayItem.Transaction>()
        assertEquals("Should have 2 transactions", 2, transactions.size)

        // The most recent transaction (dinner) should show the cumulative total: 25.00 + 35.50 = 60.50
        // The first transaction (lunch) should show just 25.00
        // Note: getItems() reverses the order, so newest first
        val runningTotals = transactions.map { it.runningTotal }
        assertTrue("Running totals should contain 60.50", runningTotals.any { it == "60.50" })
        assertTrue("Running totals should contain 25.00", runningTotals.any { it == "25.00" })
    }

    @Test
    fun `negative amounts reduce running total`() {
        val accumulator = TransactionAccumulator(
            boldAccountName = "Assets:Cash",
            accumulateAccount = "Assets:Cash",
            currencyFormatter = currencyFormatter
        )

        val transaction1 = createTransaction(
            ledgerId = 1,
            date = SimpleDate(2026, 1, 15),
            description = "Initial deposit",
            lines = listOf(
                createLine("Assets:Cash", 100.00f),
                createLine("Income:Salary", -100.00f)
            )
        )

        val transaction2 = createTransaction(
            ledgerId = 2,
            date = SimpleDate(2026, 1, 16),
            description = "Withdrawal",
            lines = listOf(
                createLine("Assets:Cash", -30.00f),
                createLine("Expenses:Shopping", 30.00f)
            )
        )

        accumulator.put(transaction1)
        accumulator.put(transaction2)
        val items = accumulator.getItems()

        val transactions = items.filterIsInstance<TransactionListDisplayItem.Transaction>()
        val runningTotals = transactions.map { it.runningTotal }

        // After both transactions: 100.00 - 30.00 = 70.00
        assertTrue("Running totals should contain 70.00", runningTotals.any { it == "70.00" })
        assertTrue("Running totals should contain 100.00", runningTotals.any { it == "100.00" })
    }

    // ========================================
    // T009: Single currency formatting tests
    // ========================================

    @Test
    fun `running total is formatted using injected formatter`() {
        val accumulator = TransactionAccumulator(
            boldAccountName = "Expenses:Food",
            accumulateAccount = "Expenses:Food",
            currencyFormatter = currencyFormatter
        )

        // Use a large number to verify thousand separator formatting
        val transaction = createTransaction(
            ledgerId = 1,
            date = SimpleDate(2026, 1, 15),
            description = "Big purchase",
            lines = listOf(
                createLine("Expenses:Food", 1234.56f),
                createLine("Assets:Cash", -1234.56f)
            )
        )

        accumulator.put(transaction)
        val items = accumulator.getItems()

        val transactionItem = items.filterIsInstance<TransactionListDisplayItem.Transaction>().firstOrNull()
        assertNotNull(transactionItem)
        // FakeCurrencyFormatter uses "#,##0.00" format with Locale.US
        assertEquals("1,234.56", transactionItem!!.runningTotal)
    }

    @Test
    fun `zero amount is formatted correctly`() {
        val accumulator = TransactionAccumulator(
            boldAccountName = "Assets:Cash",
            accumulateAccount = "Assets:Cash",
            currencyFormatter = currencyFormatter
        )

        val transaction1 = createTransaction(
            ledgerId = 1,
            date = SimpleDate(2026, 1, 15),
            description = "In",
            lines = listOf(
                createLine("Assets:Cash", 50.00f),
                createLine("Income:Gift", -50.00f)
            )
        )

        val transaction2 = createTransaction(
            ledgerId = 2,
            date = SimpleDate(2026, 1, 16),
            description = "Out",
            lines = listOf(
                createLine("Assets:Cash", -50.00f),
                createLine("Expenses:Other", 50.00f)
            )
        )

        accumulator.put(transaction1)
        accumulator.put(transaction2)
        val items = accumulator.getItems()

        val transactions = items.filterIsInstance<TransactionListDisplayItem.Transaction>()
        val runningTotals = transactions.map { it.runningTotal }

        // After canceling out: 50 - 50 = 0
        assertTrue("Running totals should contain 0.00", runningTotals.any { it == "0.00" })
    }

    // ========================================
    // T010: Multiple currency formatting tests
    // ========================================

    @Test
    fun `multiple currencies are tracked separately`() {
        val accumulator = TransactionAccumulator(
            boldAccountName = "Assets:Cash",
            accumulateAccount = "Assets:Cash",
            currencyFormatter = currencyFormatter
        )

        val transaction = createTransaction(
            ledgerId = 1,
            date = SimpleDate(2026, 1, 15),
            description = "Multi-currency",
            lines = listOf(
                createLine("Assets:Cash", 100.00f, "USD"),
                createLine("Assets:Cash", 50.00f, "EUR"),
                createLine("Income:Other", -100.00f, "USD"),
                createLine("Income:Other", -50.00f, "EUR")
            )
        )

        accumulator.put(transaction)
        val items = accumulator.getItems()

        val transactionItem = items.filterIsInstance<TransactionListDisplayItem.Transaction>().firstOrNull()
        assertNotNull(transactionItem)

        val runningTotal = transactionItem!!.runningTotal
        assertNotNull(runningTotal)

        // Should contain both currencies (order may vary)
        assertTrue(
            "Running total should contain USD amount",
            runningTotal!!.contains("100.00") && runningTotal.contains("USD")
        )
        assertTrue(
            "Running total should contain EUR amount",
            runningTotal.contains("50.00") && runningTotal.contains("EUR")
        )
    }

    @Test
    fun `default currency (no symbol) is formatted correctly`() {
        val accumulator = TransactionAccumulator(
            boldAccountName = "Assets:Cash",
            accumulateAccount = "Assets:Cash",
            currencyFormatter = currencyFormatter
        )

        val transaction = createTransaction(
            ledgerId = 1,
            date = SimpleDate(2026, 1, 15),
            description = "Default currency",
            lines = listOf(
                createLine("Assets:Cash", 75.25f, ""),
                createLine("Income:Other", -75.25f, "")
            )
        )

        accumulator.put(transaction)
        val items = accumulator.getItems()

        val transactionItem = items.filterIsInstance<TransactionListDisplayItem.Transaction>().firstOrNull()
        assertNotNull(transactionItem)
        assertEquals("75.25", transactionItem!!.runningTotal)
    }

    // ========================================
    // T011: Verify injected formatter is used
    // ========================================

    @Test
    fun `accumulator uses injected formatter not global App instance`() {
        // This test verifies that the injected formatter is actually used
        // by confirming the output matches FakeCurrencyFormatter's format
        val customFormatter = FakeCurrencyFormatter()
        val accumulator = TransactionAccumulator(
            boldAccountName = "Expenses:Test",
            accumulateAccount = "Expenses:Test",
            currencyFormatter = customFormatter
        )

        val transaction = createTransaction(
            ledgerId = 1,
            date = SimpleDate(2026, 1, 15),
            description = "Test",
            lines = listOf(
                createLine("Expenses:Test", 9999.99f),
                createLine("Assets:Cash", -9999.99f)
            )
        )

        accumulator.put(transaction)
        val items = accumulator.getItems()

        val transactionItem = items.filterIsInstance<TransactionListDisplayItem.Transaction>().firstOrNull()
        assertNotNull(transactionItem)

        // FakeCurrencyFormatter formats with US locale: #,##0.00
        // This format would be "9,999.99"
        assertEquals("9,999.99", transactionItem!!.runningTotal)
    }

    @Test
    fun `accumulator with child account includes parent account amounts`() {
        // When accumulateAccount is "Expenses", it should include "Expenses:Food"
        val accumulator = TransactionAccumulator(
            boldAccountName = null,
            accumulateAccount = "Expenses",
            currencyFormatter = currencyFormatter
        )

        val transaction = createTransaction(
            ledgerId = 1,
            date = SimpleDate(2026, 1, 15),
            description = "Test",
            lines = listOf(
                createLine("Expenses:Food", 50.00f),
                createLine("Assets:Cash", -50.00f)
            )
        )

        accumulator.put(transaction)
        val items = accumulator.getItems()

        val transactionItem = items.filterIsInstance<TransactionListDisplayItem.Transaction>().firstOrNull()
        assertNotNull(transactionItem)
        assertEquals("50.00", transactionItem!!.runningTotal)
    }

    // ========================================
    // Additional edge case tests
    // ========================================

    @Test
    fun `getItems returns header as first item`() {
        val accumulator = TransactionAccumulator(
            boldAccountName = null,
            accumulateAccount = null,
            currencyFormatter = currencyFormatter
        )

        val items = accumulator.getItems()
        assertTrue("Should have at least 1 item (header)", items.isNotEmpty())
        assertTrue("First item should be Header", items[0] is TransactionListDisplayItem.Header)
    }

    @Test
    fun `empty accumulator returns only header`() {
        val accumulator = TransactionAccumulator(
            boldAccountName = null,
            accumulateAccount = null,
            currencyFormatter = currencyFormatter
        )

        val items = accumulator.getItems()
        assertEquals("Should have exactly 1 item (header)", 1, items.size)
        assertTrue("First item should be Header", items[0] is TransactionListDisplayItem.Header)
    }

    @Test
    fun `transaction count is tracked correctly`() {
        val accumulator = TransactionAccumulator(
            boldAccountName = null,
            accumulateAccount = null,
            currencyFormatter = currencyFormatter
        )

        repeat(5) { i ->
            val transaction = createTransaction(
                ledgerId = i.toLong() + 1,
                date = SimpleDate(2026, 1, 15 + i),
                description = "Transaction $i",
                lines = listOf(
                    createLine("Expenses:Test", 10.00f),
                    createLine("Assets:Cash", -10.00f)
                )
            )
            accumulator.put(transaction)
        }

        assertEquals(5, accumulator.getTransactionCount())
    }

    @Test
    fun `date range is tracked correctly`() {
        val accumulator = TransactionAccumulator(
            boldAccountName = null,
            accumulateAccount = null,
            currencyFormatter = currencyFormatter
        )

        val date1 = SimpleDate(2026, 1, 15)
        val date2 = SimpleDate(2026, 1, 20)

        // Add transactions in chronological order
        accumulator.put(
            createTransaction(
                1,
                date1,
                "First",
                listOf(createLine("Expenses:Test", 10.00f))
            )
        )
        accumulator.put(
            createTransaction(
                2,
                date2,
                "Second",
                listOf(createLine("Expenses:Test", 10.00f))
            )
        )

        // earliestDate is set from the first transaction
        assertEquals(date1, accumulator.getEarliestDate())
        // latestDate is always updated to the latest transaction added
        assertEquals(date2, accumulator.getLatestDate())
    }
}
