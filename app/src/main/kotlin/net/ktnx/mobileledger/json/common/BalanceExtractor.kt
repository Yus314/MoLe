/*
 * Copyright © 2024 Damyan Ivanov.
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

package net.ktnx.mobileledger.json.common

/**
 * Interface for extracting balance information from parsed account data.
 *
 * Different API versions have different structures for balance data:
 * - v1_14 ~ v1_40: aibalance property directly on ParsedLedgerAccount
 * - v1_50: adata.firstPeriodBalance.bdincludingsubs structure
 */
interface BalanceExtractor<T, B> {
    /**
     * Extract balance list from the account data.
     * @param account The parsed account data
     * @return List of balance objects, or empty list if none
     */
    fun extractBalances(account: T): List<B>
}
