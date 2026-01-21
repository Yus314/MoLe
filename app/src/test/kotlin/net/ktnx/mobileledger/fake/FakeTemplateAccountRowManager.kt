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

package net.ktnx.mobileledger.fake

import net.ktnx.mobileledger.domain.usecase.TemplateAccountRowManager
import net.ktnx.mobileledger.ui.templates.TemplateAccountRow

/**
 * Fake implementation of [TemplateAccountRowManager] for testing.
 * Provides simple, predictable behavior for row manipulation.
 */
class FakeTemplateAccountRowManager : TemplateAccountRowManager {

    override fun addRow(rows: List<TemplateAccountRow>, newId: Long): List<TemplateAccountRow> {
        val newRow = TemplateAccountRow(id = newId, position = rows.size)
        return rows + newRow
    }

    override fun removeRow(rows: List<TemplateAccountRow>, index: Int): List<TemplateAccountRow> {
        if (rows.size <= 2 || index !in rows.indices) return rows
        return rows.filterIndexed { i, _ -> i != index }
            .mapIndexed { i, row -> row.copy(position = i) }
    }

    override fun moveRow(rows: List<TemplateAccountRow>, fromIndex: Int, toIndex: Int): List<TemplateAccountRow> {
        if (fromIndex !in rows.indices || toIndex !in rows.indices) return rows
        val mutableList = rows.toMutableList()
        val item = mutableList.removeAt(fromIndex)
        mutableList.add(toIndex, item)
        return mutableList.mapIndexed { i, row -> row.copy(position = i) }
    }

    override fun updateRow(
        rows: List<TemplateAccountRow>,
        index: Int,
        updater: (TemplateAccountRow) -> TemplateAccountRow
    ): List<TemplateAccountRow> {
        if (index !in rows.indices) return rows
        return rows.mapIndexed { i, row ->
            if (i == index) updater(row) else row
        }
    }

    override fun ensureValidRowState(
        rows: List<TemplateAccountRow>,
        newIdGenerator: () -> Long
    ): List<TemplateAccountRow> {
        var result = rows.toMutableList()

        // Ensure at least 2 rows
        while (result.size < 2) {
            result.add(TemplateAccountRow(id = newIdGenerator(), position = result.size))
        }

        // Ensure last row is empty (simplified logic)
        if (result.isNotEmpty() && !result.last().isEmpty()) {
            result.add(TemplateAccountRow(id = newIdGenerator(), position = result.size))
        }

        return result.mapIndexed { i, row -> row.copy(position = i) }
    }
}
