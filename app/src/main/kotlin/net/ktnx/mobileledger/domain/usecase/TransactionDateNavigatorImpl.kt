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
import net.ktnx.mobileledger.core.common.utils.SimpleDate

/**
 * Implementation of [TransactionDateNavigator].
 *
 * Performs a linear search through display items to find items matching a date.
 */
class TransactionDateNavigatorImpl @Inject constructor() : TransactionDateNavigator {

    override fun findIndexByDate(items: List<TransactionListConverter.DisplayItem>, targetDate: SimpleDate): Int? =
        items.indexOfFirst { item ->
            when (item) {
                is TransactionListConverter.DisplayItem.DateDelimiter -> item.date == targetDate
                is TransactionListConverter.DisplayItem.TransactionItem -> item.transaction.date == targetDate
            }
        }.takeIf { it >= 0 }
}
