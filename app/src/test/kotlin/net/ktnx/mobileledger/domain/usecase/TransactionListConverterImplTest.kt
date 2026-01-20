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

package net.ktnx.mobileledger.domain.usecase

import net.ktnx.mobileledger.domain.model.Transaction
import net.ktnx.mobileledger.utils.SimpleDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [TransactionListConverterImpl].
 */
class TransactionListConverterImplTest {

    private lateinit var converter: TransactionListConverterImpl

    @Before
    fun setup() {
        converter = TransactionListConverterImpl()
    }

    private fun transaction(
        id: Long = 1L,
        ledgerId: Long = id,
        date: SimpleDate,
        description: String = "Test Transaction"
    ) = Transaction(
        id = id,
        ledgerId = ledgerId,
        date = date,
        description = description
    )

    // ========== Empty List ==========

    @Test
    fun `convert returns empty result for empty list`() {
        val result = converter.convert(emptyList())

        assertTrue(result.items.isEmpty())
        assertNull(result.firstDate)
        assertNull(result.lastDate)
    }

    // ========== Single Transaction ==========

    @Test
    fun `convert handles single transaction`() {
        val date = SimpleDate(2025, 6, 15)
        val tx = transaction(id = 1, date = date)

        val result = converter.convert(listOf(tx))

        // Should have: TransactionItem + DateDelimiter
        assertEquals(2, result.items.size)

        // First item is the transaction
        val item0 = result.items[0] as TransactionListConverter.DisplayItem.TransactionItem
        assertEquals(tx, item0.transaction)

        // Second item is the date delimiter
        val item1 = result.items[1] as TransactionListConverter.DisplayItem.DateDelimiter
        assertEquals(date, item1.date)
        assertTrue(item1.isMonthBoundary) // Final delimiter always shows month

        // Date range
        assertEquals(date, result.firstDate)
        assertEquals(date, result.lastDate)
    }

    // ========== Sorting ==========

    @Test
    fun `convert sorts transactions by date descending`() {
        val date1 = SimpleDate(2025, 6, 10)
        val date2 = SimpleDate(2025, 6, 15)
        val date3 = SimpleDate(2025, 6, 20)

        val transactions = listOf(
            transaction(id = 1, date = date1, description = "Oldest"),
            transaction(id = 3, date = date3, description = "Newest"),
            transaction(id = 2, date = date2, description = "Middle")
        )

        val result = converter.convert(transactions)

        // Extract transaction descriptions in order
        val descriptions = result.items
            .filterIsInstance<TransactionListConverter.DisplayItem.TransactionItem>()
            .map { it.transaction.description }

        assertEquals(listOf("Newest", "Middle", "Oldest"), descriptions)
    }

    // ========== Date Grouping ==========

    @Test
    fun `convert groups transactions by same date`() {
        val date = SimpleDate(2025, 6, 15)
        val transactions = listOf(
            transaction(id = 1, date = date, description = "First"),
            transaction(id = 2, date = date, description = "Second")
        )

        val result = converter.convert(transactions)

        // Should have: 2 TransactionItems + 1 DateDelimiter
        assertEquals(3, result.items.size)

        val transactionItems = result.items.filterIsInstance<TransactionListConverter.DisplayItem.TransactionItem>()
        assertEquals(2, transactionItems.size)

        val delimiterItems = result.items.filterIsInstance<TransactionListConverter.DisplayItem.DateDelimiter>()
        assertEquals(1, delimiterItems.size)
    }

    @Test
    fun `convert adds delimiter between different dates`() {
        val date1 = SimpleDate(2025, 6, 15)
        val date2 = SimpleDate(2025, 6, 10)

        val transactions = listOf(
            transaction(id = 1, date = date1),
            transaction(id = 2, date = date2)
        )

        val result = converter.convert(transactions)

        // Should have: Tx1 + Delimiter(date1) + Tx2 + Delimiter(date2)
        assertEquals(4, result.items.size)

        assertTrue(result.items[0] is TransactionListConverter.DisplayItem.TransactionItem)
        assertTrue(result.items[1] is TransactionListConverter.DisplayItem.DateDelimiter)
        assertTrue(result.items[2] is TransactionListConverter.DisplayItem.TransactionItem)
        assertTrue(result.items[3] is TransactionListConverter.DisplayItem.DateDelimiter)

        // Verify delimiter dates
        val delim1 = result.items[1] as TransactionListConverter.DisplayItem.DateDelimiter
        assertEquals(date1, delim1.date)

        val delim2 = result.items[3] as TransactionListConverter.DisplayItem.DateDelimiter
        assertEquals(date2, delim2.date)
    }

    // ========== Month Boundary Detection ==========

    @Test
    fun `convert marks month boundary when month changes`() {
        val june = SimpleDate(2025, 6, 15)
        val may = SimpleDate(2025, 5, 20)

        val transactions = listOf(
            transaction(id = 1, date = june),
            transaction(id = 2, date = may)
        )

        val result = converter.convert(transactions)

        // First delimiter (june -> may) should mark month boundary
        val delim1 = result.items[1] as TransactionListConverter.DisplayItem.DateDelimiter
        assertTrue(delim1.isMonthBoundary)
    }

    @Test
    fun `convert marks month boundary when year changes`() {
        val jan2025 = SimpleDate(2025, 1, 15)
        val dec2024 = SimpleDate(2024, 12, 20)

        val transactions = listOf(
            transaction(id = 1, date = jan2025),
            transaction(id = 2, date = dec2024)
        )

        val result = converter.convert(transactions)

        val delim1 = result.items[1] as TransactionListConverter.DisplayItem.DateDelimiter
        assertTrue(delim1.isMonthBoundary)
    }

    @Test
    fun `convert does not mark month boundary for same month different day`() {
        val date1 = SimpleDate(2025, 6, 20)
        val date2 = SimpleDate(2025, 6, 15)
        val date3 = SimpleDate(2025, 6, 10)

        val transactions = listOf(
            transaction(id = 1, date = date1),
            transaction(id = 2, date = date2),
            transaction(id = 3, date = date3)
        )

        val result = converter.convert(transactions)

        // Middle delimiters should NOT be month boundaries
        val delim1 = result.items[1] as TransactionListConverter.DisplayItem.DateDelimiter
        assertFalse(delim1.isMonthBoundary)

        val delim2 = result.items[3] as TransactionListConverter.DisplayItem.DateDelimiter
        assertFalse(delim2.isMonthBoundary)

        // Final delimiter always shows month
        val finalDelim = result.items[5] as TransactionListConverter.DisplayItem.DateDelimiter
        assertTrue(finalDelim.isMonthBoundary)
    }

    // ========== Date Range Calculation ==========

    @Test
    fun `convert calculates correct date range`() {
        val oldest = SimpleDate(2025, 1, 1)
        val middle = SimpleDate(2025, 6, 15)
        val newest = SimpleDate(2025, 12, 31)

        val transactions = listOf(
            transaction(id = 1, date = oldest),
            transaction(id = 2, date = newest),
            transaction(id = 3, date = middle)
        )

        val result = converter.convert(transactions)

        assertEquals(oldest, result.firstDate)
        assertEquals(newest, result.lastDate)
    }

    // ========== Complex Scenario ==========

    @Test
    fun `convert handles complex scenario with multiple dates and months`() {
        val transactions = listOf(
            transaction(id = 1, date = SimpleDate(2025, 6, 20), description = "June 20 #1"),
            transaction(id = 2, date = SimpleDate(2025, 6, 20), description = "June 20 #2"),
            transaction(id = 3, date = SimpleDate(2025, 6, 15), description = "June 15"),
            transaction(id = 4, date = SimpleDate(2025, 5, 10), description = "May 10"),
            transaction(id = 5, date = SimpleDate(2024, 12, 25), description = "Dec 2024")
        )

        val result = converter.convert(transactions)

        // Expected order:
        // Tx June20#1, Tx June20#2, Delimiter(June20, false),
        // Tx June15, Delimiter(June15, false),
        // Tx May10, Delimiter(May10, true - month change),
        // Tx Dec2024, Delimiter(Dec2024, true - final)

        val txItems = result.items.filterIsInstance<TransactionListConverter.DisplayItem.TransactionItem>()
        assertEquals(5, txItems.size)

        val delimItems = result.items.filterIsInstance<TransactionListConverter.DisplayItem.DateDelimiter>()
        assertEquals(4, delimItems.size)

        // Verify date range
        assertEquals(SimpleDate(2024, 12, 25), result.firstDate)
        assertEquals(SimpleDate(2025, 6, 20), result.lastDate)
    }
}
