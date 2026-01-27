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

import net.ktnx.mobileledger.core.common.utils.SimpleDate
import net.ktnx.mobileledger.core.domain.model.Transaction

/**
 * Converts a list of transactions into a grouped display format.
 *
 * Responsibilities:
 * - Sort transactions by date (newest first)
 * - Insert date delimiters between groups
 * - Detect month boundaries for display
 * - Calculate date range
 */
interface TransactionListConverter {

    /**
     * Represents an item in the converted transaction list.
     */
    sealed class DisplayItem {
        /**
         * A transaction item containing the domain transaction.
         */
        data class TransactionItem(val transaction: Transaction) : DisplayItem()

        /**
         * A date delimiter marking the boundary between date groups.
         *
         * @param date The date for this group
         * @param isMonthBoundary True if this delimiter marks a month/year change
         */
        data class DateDelimiter(val date: SimpleDate, val isMonthBoundary: Boolean) : DisplayItem()
    }

    /**
     * Result of converting transactions to display items.
     *
     * @param items The ordered list of display items (transactions and delimiters)
     * @param firstDate The earliest date in the list (null if empty)
     * @param lastDate The latest date in the list (null if empty)
     */
    data class ConversionResult(
        val items: List<DisplayItem>,
        val firstDate: SimpleDate?,
        val lastDate: SimpleDate?
    )

    /**
     * Convert a list of transactions into grouped display items.
     *
     * Transactions are sorted by date (newest first) and grouped by date.
     * Date delimiters are inserted between groups, with month boundary markers
     * when the month or year changes.
     *
     * @param transactions The transactions to convert
     * @return ConversionResult containing ordered display items and date range
     */
    fun convert(transactions: List<Transaction>): ConversionResult
}
