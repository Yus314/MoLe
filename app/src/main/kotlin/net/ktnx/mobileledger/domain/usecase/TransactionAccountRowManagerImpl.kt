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
import net.ktnx.mobileledger.ui.transaction.TransactionAccountRow

/**
 * Implementation of TransactionAccountRowManager.
 *
 * Provides pure functions for row management with last-flag tracking.
 */
class TransactionAccountRowManagerImpl @Inject constructor() : TransactionAccountRowManager {

    companion object {
        private const val MIN_ROWS = 2
    }

    override fun addRow(
        rows: List<TransactionAccountRow>,
        afterRowId: Int?,
        newRow: TransactionAccountRow
    ): List<TransactionAccountRow> {
        val newAccounts = if (afterRowId != null) {
            val index = rows.indexOfFirst { it.id == afterRowId }
            if (index >= 0) {
                rows.toMutableList().apply {
                    add(index + 1, newRow)
                }
            } else {
                rows + newRow
            }
        } else {
            rows + newRow
        }
        return updateLastFlags(newAccounts)
    }

    override fun removeRow(rows: List<TransactionAccountRow>, rowId: Int): List<TransactionAccountRow> {
        if (rows.size <= MIN_ROWS) {
            return rows
        }
        val newAccounts = rows.filter { it.id != rowId }
        return updateLastFlags(newAccounts)
    }

    override fun moveRow(rows: List<TransactionAccountRow>, fromIndex: Int, toIndex: Int): List<TransactionAccountRow> {
        if (fromIndex !in rows.indices || toIndex !in rows.indices) {
            return rows
        }
        val mutableRows = rows.toMutableList()
        val item = mutableRows.removeAt(fromIndex)
        mutableRows.add(toIndex, item)
        return updateLastFlags(mutableRows)
    }

    override fun updateRow(
        rows: List<TransactionAccountRow>,
        rowId: Int,
        updater: (TransactionAccountRow) -> TransactionAccountRow
    ): List<TransactionAccountRow> = rows.map { row ->
        if (row.id == rowId) updater(row) else row
    }

    override fun ensureMinimumRows(
        rows: List<TransactionAccountRow>,
        newRowGenerator: () -> TransactionAccountRow
    ): List<TransactionAccountRow> {
        if (rows.size >= MIN_ROWS) {
            return rows
        }
        val additionalRows = (rows.size until MIN_ROWS).map { newRowGenerator() }
        return updateLastFlags(rows + additionalRows)
    }

    override fun setRows(
        rows: List<TransactionAccountRow>,
        newRowGenerator: () -> TransactionAccountRow
    ): List<TransactionAccountRow> {
        val accounts = if (rows.size >= MIN_ROWS) {
            rows
        } else {
            rows + List(MIN_ROWS - rows.size) { newRowGenerator() }
        }
        return updateLastFlags(accounts)
    }

    override fun updateLastFlags(rows: List<TransactionAccountRow>): List<TransactionAccountRow> =
        rows.mapIndexed { index, row ->
            row.copy(isLast = index == rows.lastIndex)
        }
}
