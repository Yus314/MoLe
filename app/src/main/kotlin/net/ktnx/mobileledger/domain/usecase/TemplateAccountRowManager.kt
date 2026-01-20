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

import net.ktnx.mobileledger.ui.templates.TemplateAccountRow

/**
 * Interface for managing template account rows.
 *
 * Provides pure functions for adding, removing, moving, and updating rows
 * while maintaining position integrity and minimum row requirements.
 */
interface TemplateAccountRowManager {

    /**
     * Adds a new empty row at the end of the list.
     *
     * @param rows Current list of rows
     * @param newId ID for the new row
     * @return Updated list with the new row
     */
    fun addRow(rows: List<TemplateAccountRow>, newId: Long): List<TemplateAccountRow>

    /**
     * Removes a row at the specified index.
     * Will not remove if the result would have fewer than 2 rows.
     *
     * @param rows Current list of rows
     * @param index Index of the row to remove
     * @return Updated list with positions recalculated
     */
    fun removeRow(rows: List<TemplateAccountRow>, index: Int): List<TemplateAccountRow>

    /**
     * Moves a row from one index to another.
     *
     * @param rows Current list of rows
     * @param fromIndex Source index
     * @param toIndex Destination index
     * @return Updated list with positions recalculated
     */
    fun moveRow(rows: List<TemplateAccountRow>, fromIndex: Int, toIndex: Int): List<TemplateAccountRow>

    /**
     * Updates a single row at the specified index.
     *
     * @param rows Current list of rows
     * @param index Index of the row to update
     * @param updater Function to transform the row
     * @return Updated list
     */
    fun updateRow(
        rows: List<TemplateAccountRow>,
        index: Int,
        updater: (TemplateAccountRow) -> TemplateAccountRow
    ): List<TemplateAccountRow>

    /**
     * Ensures the row list meets all constraints:
     * - At least 2 rows
     * - At least one empty row at the end
     * - No more than one empty row (except at positions 0-1)
     *
     * @param rows Current list of rows
     * @param newIdGenerator Function to generate new IDs for created rows
     * @return Validated list with positions recalculated
     */
    fun ensureValidRowState(rows: List<TemplateAccountRow>, newIdGenerator: () -> Long): List<TemplateAccountRow>
}
