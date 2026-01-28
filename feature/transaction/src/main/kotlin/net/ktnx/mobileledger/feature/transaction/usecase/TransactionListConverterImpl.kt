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

package net.ktnx.mobileledger.feature.transaction.usecase

import javax.inject.Inject
import net.ktnx.mobileledger.core.common.utils.SimpleDate
import net.ktnx.mobileledger.core.domain.model.Transaction

/**
 * Implementation of [TransactionListConverter].
 *
 * Converts transactions to a display-ready format with date grouping.
 */
class TransactionListConverterImpl @Inject constructor() : TransactionListConverter {

    override fun convert(transactions: List<Transaction>): TransactionListConverter.ConversionResult {
        if (transactions.isEmpty()) {
            return TransactionListConverter.ConversionResult(
                items = emptyList(),
                firstDate = null,
                lastDate = null
            )
        }

        // Sort by date (newest first)
        val sortedTx = transactions.sortedByDescending { it.date }

        val items = mutableListOf<TransactionListConverter.DisplayItem>()
        var lastDate: SimpleDate? = null
        var firstDate: SimpleDate? = null
        var latestDate: SimpleDate? = null

        for (tx in sortedTx) {
            val date = tx.date

            // Track date range
            if (firstDate == null || date < firstDate) firstDate = date
            if (latestDate == null || date > latestDate) latestDate = date

            // Add date delimiter if date changed
            if (lastDate != null && date != lastDate) {
                val isMonthBoundary = isMonthOrYearChange(date, lastDate)
                items.add(TransactionListConverter.DisplayItem.DateDelimiter(lastDate, isMonthBoundary))
            }

            // Add transaction
            items.add(TransactionListConverter.DisplayItem.TransactionItem(tx))

            lastDate = date
        }

        // Add final date delimiter for the oldest date group
        lastDate?.let { last ->
            items.add(TransactionListConverter.DisplayItem.DateDelimiter(last, isMonthBoundary = true))
        }

        return TransactionListConverter.ConversionResult(
            items = items,
            firstDate = firstDate,
            lastDate = latestDate
        )
    }

    /**
     * Check if two dates have different month or year.
     */
    private fun isMonthOrYearChange(date1: SimpleDate, date2: SimpleDate): Boolean =
        date1.month != date2.month || date1.year != date2.year
}
