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

import javax.inject.Inject
import net.ktnx.mobileledger.ui.templates.TemplateAccountRow

/**
 * Implementation of TemplateAccountRowManager.
 *
 * Provides pure functions for row management with position tracking.
 */
class TemplateAccountRowManagerImpl @Inject constructor() : TemplateAccountRowManager {

    companion object {
        private const val MIN_ROWS = 2
    }

    override fun addRow(rows: List<TemplateAccountRow>, newId: Long): List<TemplateAccountRow> {
        val mutableRows = rows.toMutableList()
        val newRow = TemplateAccountRow(
            id = newId,
            position = mutableRows.size
        )
        mutableRows.add(newRow)
        return mutableRows
    }

    override fun removeRow(rows: List<TemplateAccountRow>, index: Int): List<TemplateAccountRow> {
        if (index !in rows.indices || rows.size <= MIN_ROWS) {
            return rows
        }

        val mutableRows = rows.toMutableList()
        mutableRows.removeAt(index)
        return recalculatePositions(mutableRows)
    }

    override fun moveRow(rows: List<TemplateAccountRow>, fromIndex: Int, toIndex: Int): List<TemplateAccountRow> {
        if (fromIndex !in rows.indices || toIndex !in rows.indices) {
            return rows
        }

        val mutableRows = rows.toMutableList()
        val item = mutableRows.removeAt(fromIndex)
        mutableRows.add(toIndex, item)
        return recalculatePositions(mutableRows)
    }

    override fun updateRow(
        rows: List<TemplateAccountRow>,
        index: Int,
        updater: (TemplateAccountRow) -> TemplateAccountRow
    ): List<TemplateAccountRow> {
        if (index !in rows.indices) {
            return rows
        }

        val mutableRows = rows.toMutableList()
        mutableRows[index] = updater(mutableRows[index])
        return mutableRows
    }

    override fun ensureValidRowState(
        rows: List<TemplateAccountRow>,
        newIdGenerator: () -> Long
    ): List<TemplateAccountRow> {
        val mutableRows = rows.toMutableList()

        // Ensure minimum rows
        while (mutableRows.size < MIN_ROWS) {
            mutableRows.add(
                TemplateAccountRow(
                    id = newIdGenerator(),
                    position = mutableRows.size
                )
            )
        }

        // Ensure at least one empty row exists
        val hasEmptyRow = mutableRows.any { it.isEmpty() }
        if (!hasEmptyRow) {
            mutableRows.add(
                TemplateAccountRow(
                    id = newIdGenerator(),
                    position = mutableRows.size
                )
            )
        }

        // Remove extra empty rows (keep max 1 empty row, unless at positions 0-1)
        val emptyIndices = mutableRows.mapIndexedNotNull { index, row ->
            if (row.isEmpty() && index >= MIN_ROWS) index else null
        }
        if (emptyIndices.size > 1) {
            // Keep only the last empty row
            emptyIndices.dropLast(1).reversed().forEach { index ->
                mutableRows.removeAt(index)
            }
        }

        return recalculatePositions(mutableRows)
    }

    private fun recalculatePositions(rows: MutableList<TemplateAccountRow>): List<TemplateAccountRow> {
        rows.forEachIndexed { i, row ->
            if (row.position != i) {
                rows[i] = row.copy(position = i)
            }
        }
        return rows
    }
}
