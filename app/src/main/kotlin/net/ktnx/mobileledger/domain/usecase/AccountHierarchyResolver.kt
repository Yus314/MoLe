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

import net.ktnx.mobileledger.core.domain.model.Account

/**
 * Resolves account hierarchy relationships and handles zero-balance filtering.
 *
 * Responsibilities:
 * - Determine which accounts have sub-accounts
 * - Filter out zero-balance accounts while preserving hierarchy
 */
interface AccountHierarchyResolver {

    /**
     * An account with resolved hierarchy information.
     *
     * @param account The original account
     * @param hasSubAccounts True if this account has child accounts
     */
    data class ResolvedAccount(
        val account: Account,
        val hasSubAccounts: Boolean
    )

    /**
     * Resolve hierarchy information for a list of accounts.
     *
     * Determines which accounts have sub-accounts by checking if any other account
     * has this account as a parent.
     *
     * @param accounts The accounts to resolve
     * @return List of resolved accounts with hierarchy information
     */
    fun resolve(accounts: List<Account>): List<ResolvedAccount>

    /**
     * Filter out accounts with zero balance while preserving hierarchy.
     *
     * An account is kept if:
     * - It has a non-zero balance
     * - It is a parent of an account that is kept
     *
     * @param accounts The resolved accounts to filter
     * @param showZeroBalance If true, returns all accounts without filtering
     * @return Filtered list of accounts
     */
    fun filterZeroBalance(accounts: List<ResolvedAccount>, showZeroBalance: Boolean): List<ResolvedAccount>
}
