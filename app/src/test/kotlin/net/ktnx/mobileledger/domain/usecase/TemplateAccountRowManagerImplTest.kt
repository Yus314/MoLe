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

import net.ktnx.mobileledger.ui.templates.MatchableValue
import net.ktnx.mobileledger.ui.templates.TemplateAccountRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [TemplateAccountRowManagerImpl].
 *
 * Tests verify:
 * - Adding rows with correct position tracking
 * - Removing rows with minimum row constraint
 * - Moving rows with position recalculation
 * - Updating rows
 * - Ensuring valid row state (minimum rows, empty row management)
 */
class TemplateAccountRowManagerImplTest {

    private lateinit var manager: TemplateAccountRowManagerImpl

    @Before
    fun setup() {
        manager = TemplateAccountRowManagerImpl()
    }

    // ========================================
    // addRow tests
    // ========================================

    @Test
    fun `addRow appends new row to empty list`() {
        // Given
        val rows = emptyList<TemplateAccountRow>()

        // When
        val result = manager.addRow(rows, 100L)

        // Then
        assertEquals(1, result.size)
        assertEquals(100L, result[0].id)
        assertEquals(0, result[0].position)
    }

    @Test
    fun `addRow appends new row with correct position`() {
        // Given
        val rows = listOf(
            TemplateAccountRow(id = 1, position = 0),
            TemplateAccountRow(id = 2, position = 1)
        )

        // When
        val result = manager.addRow(rows, 100L)

        // Then
        assertEquals(3, result.size)
        assertEquals(100L, result[2].id)
        assertEquals(2, result[2].position)
    }

    @Test
    fun `addRow preserves existing rows`() {
        // Given
        val rows = listOf(
            TemplateAccountRow(
                id = 1,
                position = 0,
                accountName = MatchableValue.Literal("Assets:Bank")
            )
        )

        // When
        val result = manager.addRow(rows, 100L)

        // Then
        assertEquals(2, result.size)
        assertEquals("Assets:Bank", result[0].accountName.getLiteralValue())
    }

    // ========================================
    // removeRow tests
    // ========================================

    @Test
    fun `removeRow removes row at index`() {
        // Given
        val rows = listOf(
            TemplateAccountRow(id = 1, position = 0),
            TemplateAccountRow(id = 2, position = 1),
            TemplateAccountRow(id = 3, position = 2)
        )

        // When
        val result = manager.removeRow(rows, 1)

        // Then
        assertEquals(2, result.size)
        assertEquals(1L, result[0].id)
        assertEquals(3L, result[1].id)
    }

    @Test
    fun `removeRow recalculates positions`() {
        // Given
        val rows = listOf(
            TemplateAccountRow(id = 1, position = 0),
            TemplateAccountRow(id = 2, position = 1),
            TemplateAccountRow(id = 3, position = 2)
        )

        // When
        val result = manager.removeRow(rows, 0)

        // Then
        assertEquals(2, result.size)
        assertEquals(0, result[0].position)
        assertEquals(1, result[1].position)
    }

    @Test
    fun `removeRow does not remove below minimum rows`() {
        // Given
        val rows = listOf(
            TemplateAccountRow(id = 1, position = 0),
            TemplateAccountRow(id = 2, position = 1)
        )

        // When
        val result = manager.removeRow(rows, 0)

        // Then - should return unchanged
        assertEquals(2, result.size)
    }

    @Test
    fun `removeRow returns unchanged list for invalid index`() {
        // Given
        val rows = listOf(
            TemplateAccountRow(id = 1, position = 0),
            TemplateAccountRow(id = 2, position = 1),
            TemplateAccountRow(id = 3, position = 2)
        )

        // When
        val result = manager.removeRow(rows, 10) // Invalid index

        // Then
        assertEquals(3, result.size)
    }

    @Test
    fun `removeRow returns unchanged list for negative index`() {
        // Given
        val rows = listOf(
            TemplateAccountRow(id = 1, position = 0),
            TemplateAccountRow(id = 2, position = 1),
            TemplateAccountRow(id = 3, position = 2)
        )

        // When
        val result = manager.removeRow(rows, -1)

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
            TemplateAccountRow(id = 1, position = 0),
            TemplateAccountRow(id = 2, position = 1),
            TemplateAccountRow(id = 3, position = 2)
        )

        // When
        val result = manager.moveRow(rows, 0, 2)

        // Then
        assertEquals(3, result.size)
        assertEquals(2L, result[0].id)
        assertEquals(3L, result[1].id)
        assertEquals(1L, result[2].id)
    }

    @Test
    fun `moveRow moves row up`() {
        // Given
        val rows = listOf(
            TemplateAccountRow(id = 1, position = 0),
            TemplateAccountRow(id = 2, position = 1),
            TemplateAccountRow(id = 3, position = 2)
        )

        // When
        val result = manager.moveRow(rows, 2, 0)

        // Then
        assertEquals(3, result.size)
        assertEquals(3L, result[0].id)
        assertEquals(1L, result[1].id)
        assertEquals(2L, result[2].id)
    }

    @Test
    fun `moveRow recalculates positions correctly`() {
        // Given
        val rows = listOf(
            TemplateAccountRow(id = 1, position = 0),
            TemplateAccountRow(id = 2, position = 1),
            TemplateAccountRow(id = 3, position = 2)
        )

        // When
        val result = manager.moveRow(rows, 0, 2)

        // Then
        assertEquals(0, result[0].position)
        assertEquals(1, result[1].position)
        assertEquals(2, result[2].position)
    }

    @Test
    fun `moveRow returns unchanged for invalid fromIndex`() {
        // Given
        val rows = listOf(
            TemplateAccountRow(id = 1, position = 0),
            TemplateAccountRow(id = 2, position = 1)
        )

        // When
        val result = manager.moveRow(rows, 10, 0)

        // Then
        assertEquals(2, result.size)
        assertEquals(1L, result[0].id)
    }

    @Test
    fun `moveRow returns unchanged for invalid toIndex`() {
        // Given
        val rows = listOf(
            TemplateAccountRow(id = 1, position = 0),
            TemplateAccountRow(id = 2, position = 1)
        )

        // When
        val result = manager.moveRow(rows, 0, 10)

        // Then
        assertEquals(2, result.size)
        assertEquals(1L, result[0].id)
    }

    @Test
    fun `moveRow same position returns same order`() {
        // Given
        val rows = listOf(
            TemplateAccountRow(id = 1, position = 0),
            TemplateAccountRow(id = 2, position = 1)
        )

        // When
        val result = manager.moveRow(rows, 1, 1)

        // Then
        assertEquals(2, result.size)
        assertEquals(1L, result[0].id)
        assertEquals(2L, result[1].id)
    }

    // ========================================
    // updateRow tests
    // ========================================

    @Test
    fun `updateRow modifies row at index`() {
        // Given
        val rows = listOf(
            TemplateAccountRow(
                id = 1,
                position = 0,
                accountName = MatchableValue.Literal("Old")
            ),
            TemplateAccountRow(id = 2, position = 1)
        )

        // When
        val result = manager.updateRow(rows, 0) { row ->
            row.copy(accountName = MatchableValue.Literal("New"))
        }

        // Then
        assertEquals("New", result[0].accountName.getLiteralValue())
    }

    @Test
    fun `updateRow preserves other rows`() {
        // Given
        val rows = listOf(
            TemplateAccountRow(
                id = 1,
                position = 0,
                accountName = MatchableValue.Literal("First")
            ),
            TemplateAccountRow(
                id = 2,
                position = 1,
                accountName = MatchableValue.Literal("Second")
            )
        )

        // When
        val result = manager.updateRow(rows, 0) { row ->
            row.copy(accountName = MatchableValue.Literal("Updated"))
        }

        // Then
        assertEquals("Updated", result[0].accountName.getLiteralValue())
        assertEquals("Second", result[1].accountName.getLiteralValue())
    }

    @Test
    fun `updateRow returns unchanged for invalid index`() {
        // Given
        val rows = listOf(
            TemplateAccountRow(
                id = 1,
                position = 0,
                accountName = MatchableValue.Literal("Test")
            )
        )

        // When
        val result = manager.updateRow(rows, 10) { row ->
            row.copy(accountName = MatchableValue.Literal("Changed"))
        }

        // Then
        assertEquals("Test", result[0].accountName.getLiteralValue())
    }

    @Test
    fun `updateRow can update negateAmount flag`() {
        // Given
        val rows = listOf(
            TemplateAccountRow(id = 1, position = 0, negateAmount = false)
        )

        // When
        val result = manager.updateRow(rows, 0) { row ->
            row.copy(negateAmount = true)
        }

        // Then
        assertTrue(result[0].negateAmount)
    }

    // ========================================
    // ensureValidRowState tests
    // ========================================

    @Test
    fun `ensureValidRowState adds rows to reach minimum`() {
        // Given
        val rows = emptyList<TemplateAccountRow>()
        var nextId = 1L

        // When
        val result = manager.ensureValidRowState(rows) { nextId++ }

        // Then
        assertTrue(result.size >= 2)
    }

    @Test
    fun `ensureValidRowState adds empty row if all rows have content`() {
        // Given
        val rows = listOf(
            TemplateAccountRow(
                id = 1,
                position = 0,
                accountName = MatchableValue.Literal("Assets:Bank"),
                amount = MatchableValue.Literal("100")
            ),
            TemplateAccountRow(
                id = 2,
                position = 1,
                accountName = MatchableValue.Literal("Expenses:Food"),
                amount = MatchableValue.Literal("100")
            )
        )
        var nextId = 100L

        // When
        val result = manager.ensureValidRowState(rows) { nextId++ }

        // Then
        assertTrue(result.size > 2) // Should have added an empty row
        assertTrue(result.any { it.isEmpty() })
    }

    @Test
    fun `ensureValidRowState preserves existing empty row`() {
        // Given
        val rows = listOf(
            TemplateAccountRow(
                id = 1,
                position = 0,
                accountName = MatchableValue.Literal("Assets:Bank")
            ),
            TemplateAccountRow(id = 2, position = 1) // Empty row
        )
        var nextId = 100L

        // When
        val result = manager.ensureValidRowState(rows) { nextId++ }

        // Then
        assertEquals(2, result.size)
    }

    @Test
    fun `ensureValidRowState removes extra empty rows after minimum`() {
        // Given - row at position 1 is empty but within minimum, rows at 2 and 3 are extra
        val rows = listOf(
            TemplateAccountRow(
                id = 1,
                position = 0,
                accountName = MatchableValue.Literal("Assets:Bank")
            ),
            TemplateAccountRow(
                id = 2,
                position = 1,
                accountName = MatchableValue.Literal("Expenses:Food")
            ),
            TemplateAccountRow(id = 3, position = 2), // Extra empty row (index >= MIN_ROWS)
            TemplateAccountRow(id = 4, position = 3) // Another extra empty row
        )
        var nextId = 100L

        // When
        val result = manager.ensureValidRowState(rows) { nextId++ }

        // Then - implementation keeps only 1 empty row at index >= MIN_ROWS
        val emptyRowsAfterMinimum = result.filterIndexed { index, row ->
            row.isEmpty() && index >= 2
        }
        assertEquals(1, emptyRowsAfterMinimum.size)
    }

    @Test
    fun `ensureValidRowState recalculates positions`() {
        // Given
        val rows = listOf(
            TemplateAccountRow(id = 1, position = 5), // Wrong position
            TemplateAccountRow(id = 2, position = 10) // Wrong position
        )
        var nextId = 100L

        // When
        val result = manager.ensureValidRowState(rows) { nextId++ }

        // Then
        assertEquals(0, result[0].position)
        assertEquals(1, result[1].position)
    }

    @Test
    fun `ensureValidRowState keeps minimum empty rows at positions 0-1`() {
        // Given - both rows are empty but at minimum positions
        val rows = listOf(
            TemplateAccountRow(id = 1, position = 0), // Empty at position 0
            TemplateAccountRow(id = 2, position = 1) // Empty at position 1
        )
        var nextId = 100L

        // When
        val result = manager.ensureValidRowState(rows) { nextId++ }

        // Then - should keep both and not remove them
        assertTrue(result.size >= 2)
    }

    // ========================================
    // Edge cases
    // ========================================

    @Test
    fun `operations on empty list do not crash`() {
        // Given
        val emptyList = emptyList<TemplateAccountRow>()

        // When/Then - should not throw
        manager.removeRow(emptyList, 0)
        manager.moveRow(emptyList, 0, 1)
        manager.updateRow(emptyList, 0) { it }
    }

    @Test
    fun `isEmpty correctly identifies empty row`() {
        // Given
        val emptyRow = TemplateAccountRow(id = 1, position = 0)
        val nonEmptyRow = TemplateAccountRow(
            id = 2,
            position = 1,
            accountName = MatchableValue.Literal("Assets:Bank")
        )

        // Then
        assertTrue(emptyRow.isEmpty())
        assertFalse(nonEmptyRow.isEmpty())
    }

    @Test
    fun `row with only amount is not empty`() {
        // Given
        val row = TemplateAccountRow(
            id = 1,
            position = 0,
            amount = MatchableValue.Literal("100.00")
        )

        // Then
        assertFalse(row.isEmpty())
    }

    @Test
    fun `row with only account comment is not empty`() {
        // Given
        val row = TemplateAccountRow(
            id = 1,
            position = 0,
            accountComment = MatchableValue.Literal("Some comment")
        )

        // Then
        assertFalse(row.isEmpty())
    }
}
