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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [TransactionDateNavigatorImpl].
 */
class TransactionDateNavigatorImplTest {

    private lateinit var navigator: TransactionDateNavigatorImpl

    @Before
    fun setup() {
        navigator = TransactionDateNavigatorImpl()
    }

    private fun transaction(date: SimpleDate) = Transaction(
        id = 1L,
        ledgerId = 1L,
        date = date,
        description = "Test"
    )

    private fun txItem(date: SimpleDate) = TransactionListConverter.DisplayItem.TransactionItem(transaction(date))

    private fun delimiterItem(date: SimpleDate, isMonthBoundary: Boolean = false) =
        TransactionListConverter.DisplayItem.DateDelimiter(date, isMonthBoundary)

    // ========== Empty List ==========

    @Test
    fun `findIndexByDate returns null for empty list`() {
        val result = navigator.findIndexByDate(emptyList(), SimpleDate(2025, 6, 15))
        assertNull(result)
    }

    // ========== Find by Transaction ==========

    @Test
    fun `findIndexByDate finds transaction by date`() {
        val targetDate = SimpleDate(2025, 6, 15)
        val items = listOf(
            txItem(SimpleDate(2025, 6, 20)),
            txItem(targetDate),
            txItem(SimpleDate(2025, 6, 10))
        )

        val result = navigator.findIndexByDate(items, targetDate)

        assertEquals(1, result)
    }

    @Test
    fun `findIndexByDate finds first matching transaction`() {
        val targetDate = SimpleDate(2025, 6, 15)
        val items = listOf(
            txItem(SimpleDate(2025, 6, 20)),
            txItem(targetDate), // index 1
            txItem(targetDate), // index 2 (duplicate date)
            txItem(SimpleDate(2025, 6, 10))
        )

        val result = navigator.findIndexByDate(items, targetDate)

        assertEquals(1, result)
    }

    // ========== Find by DateDelimiter ==========

    @Test
    fun `findIndexByDate finds date delimiter`() {
        val targetDate = SimpleDate(2025, 6, 15)
        val items = listOf(
            txItem(SimpleDate(2025, 6, 20)),
            delimiterItem(targetDate),
            txItem(SimpleDate(2025, 6, 10))
        )

        val result = navigator.findIndexByDate(items, targetDate)

        assertEquals(1, result)
    }

    @Test
    fun `findIndexByDate prefers earlier item when both match`() {
        val targetDate = SimpleDate(2025, 6, 15)
        val items = listOf(
            delimiterItem(targetDate), // index 0
            txItem(targetDate) // index 1
        )

        val result = navigator.findIndexByDate(items, targetDate)

        // Should find the first one (delimiter)
        assertEquals(0, result)
    }

    // ========== Not Found ==========

    @Test
    fun `findIndexByDate returns null when date not found`() {
        val items = listOf(
            txItem(SimpleDate(2025, 6, 20)),
            delimiterItem(SimpleDate(2025, 6, 20)),
            txItem(SimpleDate(2025, 6, 10)),
            delimiterItem(SimpleDate(2025, 6, 10))
        )

        val result = navigator.findIndexByDate(items, SimpleDate(2025, 6, 15))

        assertNull(result)
    }

    // ========== Complex Scenario ==========

    @Test
    fun `findIndexByDate works with mixed items`() {
        val items = listOf(
            txItem(SimpleDate(2025, 6, 20)),
            txItem(SimpleDate(2025, 6, 20)),
            delimiterItem(SimpleDate(2025, 6, 20), isMonthBoundary = false),
            txItem(SimpleDate(2025, 6, 15)),
            delimiterItem(SimpleDate(2025, 6, 15), isMonthBoundary = false),
            txItem(SimpleDate(2025, 5, 10)),
            delimiterItem(SimpleDate(2025, 5, 10), isMonthBoundary = true)
        )

        // Find June 15
        assertEquals(3, navigator.findIndexByDate(items, SimpleDate(2025, 6, 15)))

        // Find May 10
        assertEquals(5, navigator.findIndexByDate(items, SimpleDate(2025, 5, 10)))

        // Find June 20 (first item)
        assertEquals(0, navigator.findIndexByDate(items, SimpleDate(2025, 6, 20)))

        // Not found
        assertNull(navigator.findIndexByDate(items, SimpleDate(2025, 7, 1)))
    }
}
