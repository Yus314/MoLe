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

/**
 * Interface for managing transaction account rows.
 *
 * Provides pure functions for adding, removing, moving, and updating rows
 * while maintaining minimum row requirements and last-row flags.
 */
interface TransactionAccountRowManager {

    /**
     * Adds a new row after the specified row ID, or at the end if null.
     *
     * @param rows Current list of rows
     * @param afterRowId ID of the row to insert after, or null for end
     * @param newRow The new row to add
     * @return Updated list with last flags recalculated
     */
    fun addRow(
        rows: List<TransactionAccountRow>,
        afterRowId: Int?,
        newRow: TransactionAccountRow
    ): List<TransactionAccountRow>

    /**
     * Removes a row by ID.
     * Will not remove if the result would have fewer than 2 rows.
     *
     * @param rows Current list of rows
     * @param rowId ID of the row to remove
     * @return Updated list with last flags recalculated
     */
    fun removeRow(rows: List<TransactionAccountRow>, rowId: Int): List<TransactionAccountRow>

    /**
     * Moves a row from one index to another.
     *
     * @param rows Current list of rows
     * @param fromIndex Source index
     * @param toIndex Destination index
     * @return Updated list with last flags recalculated
     */
    fun moveRow(rows: List<TransactionAccountRow>, fromIndex: Int, toIndex: Int): List<TransactionAccountRow>

    /**
     * Updates a single row by ID.
     *
     * @param rows Current list of rows
     * @param rowId ID of the row to update
     * @param updater Function to transform the row
     * @return Updated list
     */
    fun updateRow(
        rows: List<TransactionAccountRow>,
        rowId: Int,
        updater: (TransactionAccountRow) -> TransactionAccountRow
    ): List<TransactionAccountRow>

    /**
     * Ensures the list has at least the minimum number of rows.
     *
     * @param rows Current list of rows
     * @param newRowGenerator Function to generate new rows if needed
     * @return List with at least 2 rows, with last flags updated
     */
    fun ensureMinimumRows(
        rows: List<TransactionAccountRow>,
        newRowGenerator: () -> TransactionAccountRow
    ): List<TransactionAccountRow>

    /**
     * Sets the rows list, ensuring minimum rows are present.
     *
     * @param rows New list of rows
     * @param newRowGenerator Function to generate new rows if needed
     * @return List with at least 2 rows, with last flags updated
     */
    fun setRows(
        rows: List<TransactionAccountRow>,
        newRowGenerator: () -> TransactionAccountRow
    ): List<TransactionAccountRow>

    /**
     * Updates the isLast flag for all rows based on their position.
     *
     * @param rows Current list of rows
     * @return List with updated isLast flags
     */
    fun updateLastFlags(rows: List<TransactionAccountRow>): List<TransactionAccountRow>
}
