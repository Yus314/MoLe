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

import net.ktnx.mobileledger.utils.SimpleDate

/**
 * Navigates within a transaction list by date.
 *
 * Provides date-based search functionality for transaction display items.
 */
interface TransactionDateNavigator {

    /**
     * Find the index of the first item matching the target date.
     *
     * Searches through the converted display items (from [TransactionListConverter])
     * to find the first transaction or date delimiter matching the specified date.
     *
     * @param items The display items to search through
     * @param targetDate The date to find
     * @return The index of the matching item, or null if not found
     */
    fun findIndexByDate(items: List<TransactionListConverter.DisplayItem>, targetDate: SimpleDate): Int?
}
