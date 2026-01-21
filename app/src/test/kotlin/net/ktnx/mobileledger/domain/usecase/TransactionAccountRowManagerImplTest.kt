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

import net.ktnx.mobileledger.ui.transaction.TransactionAccountRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [TransactionAccountRowManagerImpl].
 *
 * Tests verify:
 * - Adding rows with position-based insertion
 * - Removing rows with minimum row constraint
 * - Moving rows
 * - Updating rows
 * - Last-flag tracking
 * - Minimum row enforcement
 */
class TransactionAccountRowManagerImplTest {

    private lateinit var manager: TransactionAccountRowManagerImpl

    @Before
    fun setup() {
        manager = TransactionAccountRowManagerImpl()
    }

    // ========================================
    // addRow tests
    // ========================================

    @Test
    fun `addRow appends row when afterRowId is null`() {
        // Given
        val rows = listOf(
            TransactionAccountRow(id = 1, accountName = "Assets:Bank"),
            TransactionAccountRow(id = 2, accountName = "Expenses:Food")
        )
        val newRow = TransactionAccountRow(id = 3, accountName = "Income:Salary")

        // When
        val result = manager.addRow(rows, afterRowId = null, newRow)

        // Then
        assertEquals(3, result.size)
        assertEquals(3, result[2].id)
    }

    @Test
    fun `addRow inserts after specified row`() {
        // Given
        val rows = listOf(
            TransactionAccountRow(id = 1, accountName = "Assets:Bank"),
            TransactionAccountRow(id = 2, accountName = "Expenses:Food")
        )
        val newRow = TransactionAccountRow(id = 3, accountName = "Income:Salary")

        // When
        val result = manager.addRow(rows, afterRowId = 1, newRow)

        // Then
        assertEquals(3, result.size)
        assertEquals(1, result[0].id)
        assertEquals(3, result[1].id) // New row inserted after id=1
        assertEquals(2, result[2].id)
    }

    @Test
    fun `addRow appends when afterRowId not found`() {
        // Given
        val rows = listOf(
            TransactionAccountRow(id = 1, accountName = "Assets:Bank")
        )
        val newRow = TransactionAccountRow(id = 3, accountName = "Income:Salary")

        // When
        val result = manager.addRow(rows, afterRowId = 999, newRow) // Non-existent ID

        // Then
        assertEquals(2, result.size)
        assertEquals(3, result[1].id) // Appended at end
    }

    @Test
    fun `addRow updates last flags`() {
        // Given
        val rows = listOf(
            TransactionAccountRow(id = 1, isLast = true)
        )
        val newRow = TransactionAccountRow(id = 2)

        // When
        val result = manager.addRow(rows, null, newRow)

        // Then
        assertFalse(result[0].isLast) // Previous last is no longer last
        assertTrue(result[1].isLast) // New row is last
    }

    // ========================================
    // removeRow tests
    // ========================================

    @Test
    fun `removeRow removes row by ID`() {
        // Given
        val rows = listOf(
            TransactionAccountRow(id = 1, accountName = "Assets:Bank"),
            TransactionAccountRow(id = 2, accountName = "Expenses:Food"),
            TransactionAccountRow(id = 3, accountName = "Income:Salary")
        )

        // When
        val result = manager.removeRow(rows, rowId = 2)

        // Then
        assertEquals(2, result.size)
        assertEquals(1, result[0].id)
        assertEquals(3, result[1].id)
    }

    @Test
    fun `removeRow does not remove below minimum rows`() {
        // Given
        val rows = listOf(
            TransactionAccountRow(id = 1),
            TransactionAccountRow(id = 2)
        )

        // When
        val result = manager.removeRow(rows, rowId = 1)

        // Then - should return unchanged
        assertEquals(2, result.size)
    }

    @Test
    fun `removeRow updates last flags`() {
        // Given
        val rows = listOf(
            TransactionAccountRow(id = 1, isLast = false),
            TransactionAccountRow(id = 2, isLast = false),
            TransactionAccountRow(id = 3, isLast = true)
        )

        // When
        val result = manager.removeRow(rows, rowId = 3)

        // Then
        assertTrue(result[1].isLast) // ID 2 is now last
    }

    @Test
    fun `removeRow with non-existent ID returns same list with updated flags`() {
        // Given
        val rows = listOf(
            TransactionAccountRow(id = 1),
            TransactionAccountRow(id = 2),
            TransactionAccountRow(id = 3)
        )

        // When
        val result = manager.removeRow(rows, rowId = 999)

        // Then
        assertEquals(3, result.size)
    }

    // ========================================
    // moveRow tests
    // ========================================

    @Test
    fun `moveRow moves row down`() {
        // Given
        val rows = listOf(
            TransactionAccountRow(id = 1),
            TransactionAccountRow(id = 2),
            TransactionAccountRow(id = 3)
        )

        // When
        val result = manager.moveRow(rows, fromIndex = 0, toIndex = 2)

        // Then
        assertEquals(2, result[0].id)
        assertEquals(3, result[1].id)
        assertEquals(1, result[2].id)
    }

    @Test
    fun `moveRow moves row up`() {
        // Given
        val rows = listOf(
            TransactionAccountRow(id = 1),
            TransactionAccountRow(id = 2),
            TransactionAccountRow(id = 3)
        )

        // When
        val result = manager.moveRow(rows, fromIndex = 2, toIndex = 0)

        // Then
        assertEquals(3, result[0].id)
        assertEquals(1, result[1].id)
        assertEquals(2, result[2].id)
    }

    @Test
    fun `moveRow returns unchanged for invalid fromIndex`() {
        // Given
        val rows = listOf(
            TransactionAccountRow(id = 1),
            TransactionAccountRow(id = 2)
        )

        // When
        val result = manager.moveRow(rows, fromIndex = 10, toIndex = 0)

        // Then
        assertEquals(2, result.size)
        assertEquals(1, result[0].id)
    }

    @Test
    fun `moveRow returns unchanged for invalid toIndex`() {
        // Given
        val rows = listOf(
            TransactionAccountRow(id = 1),
            TransactionAccountRow(id = 2)
        )

        // When
        val result = manager.moveRow(rows, fromIndex = 0, toIndex = 10)

        // Then
        assertEquals(2, result.size)
        assertEquals(1, result[0].id)
    }

    @Test
    fun `moveRow updates last flags`() {
        // Given
        val rows = listOf(
            TransactionAccountRow(id = 1, isLast = false),
            TransactionAccountRow(id = 2, isLast = true)
        )

        // When
        val result = manager.moveRow(rows, fromIndex = 1, toIndex = 0)

        // Then
        assertFalse(result[0].isLast) // ID 2 moved to position 0
        assertTrue(result[1].isLast) // ID 1 is now at last position
    }

    // ========================================
    // updateRow tests
    // ========================================

    @Test
    fun `updateRow modifies row by ID`() {
        // Given
        val rows = listOf(
            TransactionAccountRow(id = 1, accountName = "Old"),
            TransactionAccountRow(id = 2, accountName = "Other")
        )

        // When
        val result = manager.updateRow(rows, rowId = 1) { row ->
            row.copy(accountName = "New")
        }

        // Then
        assertEquals("New", result[0].accountName)
        assertEquals("Other", result[1].accountName)
    }

    @Test
    fun `updateRow with non-existent ID returns unchanged`() {
        // Given
        val rows = listOf(
            TransactionAccountRow(id = 1, accountName = "Test")
        )

        // When
        val result = manager.updateRow(rows, rowId = 999) { row ->
            row.copy(accountName = "Changed")
        }

        // Then
        assertEquals("Test", result[0].accountName)
    }

    @Test
    fun `updateRow can update amount text`() {
        // Given
        val rows = listOf(
            TransactionAccountRow(id = 1, amountText = "100.00")
        )

        // When
        val result = manager.updateRow(rows, rowId = 1) { row ->
            row.copy(amountText = "200.00")
        }

        // Then
        assertEquals("200.00", result[0].amountText)
    }

    @Test
    fun `updateRow can update currency`() {
        // Given
        val rows = listOf(
            TransactionAccountRow(id = 1, currency = "USD")
        )

        // When
        val result = manager.updateRow(rows, rowId = 1) { row ->
            row.copy(currency = "EUR")
        }

        // Then
        assertEquals("EUR", result[0].currency)
    }

    // ========================================
    // ensureMinimumRows tests
    // ========================================

    @Test
    fun `ensureMinimumRows adds rows when below minimum`() {
        // Given
        val rows = emptyList<TransactionAccountRow>()
        var nextId = 1

        // When
        val result = manager.ensureMinimumRows(rows) {
            TransactionAccountRow(id = nextId++)
        }

        // Then
        assertEquals(2, result.size) // MIN_ROWS = 2
    }

    @Test
    fun `ensureMinimumRows adds one row when at minimum minus one`() {
        // Given
        val rows = listOf(TransactionAccountRow(id = 1))
        var nextId = 10

        // When
        val result = manager.ensureMinimumRows(rows) {
            TransactionAccountRow(id = nextId++)
        }

        // Then
        assertEquals(2, result.size)
        assertEquals(1, result[0].id)
        assertEquals(10, result[1].id)
    }

    @Test
    fun `ensureMinimumRows returns unchanged when at minimum`() {
        // Given
        val rows = listOf(
            TransactionAccountRow(id = 1),
            TransactionAccountRow(id = 2)
        )
        var generatorCallCount = 0

        // When
        val result = manager.ensureMinimumRows(rows) {
            generatorCallCount++
            TransactionAccountRow(id = 99)
        }

        // Then
        assertEquals(2, result.size)
        assertEquals(0, generatorCallCount) // Generator should not be called
    }

    @Test
    fun `ensureMinimumRows updates last flags`() {
        // Given
        val rows = listOf(TransactionAccountRow(id = 1, isLast = true))

        // When
        val result = manager.ensureMinimumRows(rows) {
            TransactionAccountRow(id = 2)
        }

        // Then
        assertFalse(result[0].isLast)
        assertTrue(result[1].isLast)
    }

    // ========================================
    // setRows tests
    // ========================================

    @Test
    fun `setRows pads to minimum when below`() {
        // Given
        val rows = listOf(TransactionAccountRow(id = 1, accountName = "Assets:Bank"))
        var nextId = 100

        // When
        val result = manager.setRows(rows) {
            TransactionAccountRow(id = nextId++)
        }

        // Then
        assertEquals(2, result.size)
        assertEquals("Assets:Bank", result[0].accountName)
        assertEquals(100, result[1].id)
    }

    @Test
    fun `setRows preserves rows when at or above minimum`() {
        // Given
        val rows = listOf(
            TransactionAccountRow(id = 1, accountName = "Assets:Bank"),
            TransactionAccountRow(id = 2, accountName = "Expenses:Food"),
            TransactionAccountRow(id = 3, accountName = "Income:Salary")
        )

        // When
        val result = manager.setRows(rows) { TransactionAccountRow(id = 99) }

        // Then
        assertEquals(3, result.size)
    }

    @Test
    fun `setRows updates last flags`() {
        // Given
        val rows = listOf(
            TransactionAccountRow(id = 1, isLast = true),
            TransactionAccountRow(id = 2, isLast = true) // Both marked as last incorrectly
        )

        // When
        val result = manager.setRows(rows) { TransactionAccountRow(id = 99) }

        // Then
        assertFalse(result[0].isLast)
        assertTrue(result[1].isLast)
    }

    // ========================================
    // updateLastFlags tests
    // ========================================

    @Test
    fun `updateLastFlags sets only last item as last`() {
        // Given
        val rows = listOf(
            TransactionAccountRow(id = 1, isLast = true),
            TransactionAccountRow(id = 2, isLast = true),
            TransactionAccountRow(id = 3, isLast = false)
        )

        // When
        val result = manager.updateLastFlags(rows)

        // Then
        assertFalse(result[0].isLast)
        assertFalse(result[1].isLast)
        assertTrue(result[2].isLast)
    }

    @Test
    fun `updateLastFlags handles single row`() {
        // Given
        val rows = listOf(TransactionAccountRow(id = 1, isLast = false))

        // When
        val result = manager.updateLastFlags(rows)

        // Then
        assertTrue(result[0].isLast)
    }

    @Test
    fun `updateLastFlags handles empty list`() {
        // Given
        val rows = emptyList<TransactionAccountRow>()

        // When
        val result = manager.updateLastFlags(rows)

        // Then
        assertTrue(result.isEmpty())
    }

    // ========================================
    // TransactionAccountRow computed properties
    // ========================================

    @Test
    fun `isAmountSet returns true when amount text is not blank`() {
        // Given
        val row = TransactionAccountRow(id = 1, amountText = "100.00")

        // Then
        assertTrue(row.isAmountSet)
    }

    @Test
    fun `isAmountSet returns false when amount text is blank`() {
        // Given
        val row = TransactionAccountRow(id = 1, amountText = "")

        // Then
        assertFalse(row.isAmountSet)
    }

    @Test
    fun `isAmountSet returns false when amount text is only whitespace`() {
        // Given
        val row = TransactionAccountRow(id = 1, amountText = "   ")

        // Then
        assertFalse(row.isAmountSet)
    }
}
