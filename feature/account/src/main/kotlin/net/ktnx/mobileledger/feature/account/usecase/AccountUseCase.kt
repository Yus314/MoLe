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

package net.ktnx.mobileledger.feature.account.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import net.ktnx.mobileledger.core.domain.model.Account
import net.ktnx.mobileledger.core.domain.repository.AccountRepository
import net.ktnx.mobileledger.core.domain.repository.PreferencesRepository

// ============================================
// Account Summary UseCases
// ============================================

interface GetAccountsWithAmountsUseCase {
    suspend operator fun invoke(profileId: Long, includeZeroBalances: Boolean): Result<List<Account>>
}

/**
 * Flow-based UseCase for observing accounts with amounts.
 * Emits new list whenever the underlying data changes.
 */
interface ObserveAccountsWithAmountsUseCase {
    operator fun invoke(profileId: Long, includeZeroBalances: Boolean): Flow<List<Account>>
}

interface GetShowZeroBalanceUseCase {
    operator fun invoke(): Boolean
}

interface SetShowZeroBalanceUseCase {
    operator fun invoke(show: Boolean)
}

class GetAccountsWithAmountsUseCaseImpl @Inject constructor(
    private val accountRepository: AccountRepository
) : GetAccountsWithAmountsUseCase {
    override suspend fun invoke(profileId: Long, includeZeroBalances: Boolean): Result<List<Account>> =
        accountRepository.getAllWithAmounts(profileId, includeZeroBalances)
}

class ObserveAccountsWithAmountsUseCaseImpl @Inject constructor(
    private val accountRepository: AccountRepository
) : ObserveAccountsWithAmountsUseCase {
    override fun invoke(profileId: Long, includeZeroBalances: Boolean): Flow<List<Account>> =
        accountRepository.observeAllWithAmounts(profileId, includeZeroBalances)
}

class GetShowZeroBalanceUseCaseImpl @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : GetShowZeroBalanceUseCase {
    override fun invoke(): Boolean = preferencesRepository.getShowZeroBalanceAccounts()
}

class SetShowZeroBalanceUseCaseImpl @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : SetShowZeroBalanceUseCase {
    override fun invoke(show: Boolean) {
        preferencesRepository.setShowZeroBalanceAccounts(show)
    }
}

// ============================================
// Account Hierarchy Resolver
// ============================================

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

/**
 * Implementation of [AccountHierarchyResolver].
 *
 * Resolves account hierarchy relationships using a two-pass algorithm.
 */
class AccountHierarchyResolverImpl @Inject constructor() : AccountHierarchyResolver {

    override fun resolve(accounts: List<Account>): List<AccountHierarchyResolver.ResolvedAccount> {
        if (accounts.isEmpty()) {
            return emptyList()
        }

        // First pass: collect all account names that have children
        val hasSubAccountsSet = HashSet<String>()
        for (account in accounts) {
            val parentName = account.parentName
            if (parentName != null) {
                hasSubAccountsSet.add(parentName)
            }
        }

        // Second pass: create resolved accounts
        return accounts.map { account ->
            AccountHierarchyResolver.ResolvedAccount(
                account = account,
                hasSubAccounts = hasSubAccountsSet.contains(account.name)
            )
        }
    }

    override fun filterZeroBalance(
        accounts: List<AccountHierarchyResolver.ResolvedAccount>,
        showZeroBalance: Boolean
    ): List<AccountHierarchyResolver.ResolvedAccount> {
        if (showZeroBalance || accounts.isEmpty()) {
            return accounts
        }

        // Iteratively remove zero-balance accounts that are not parents of kept accounts
        var result = accounts.toMutableList()
        var removed = true

        while (removed) {
            removed = false
            val newResult = mutableListOf<AccountHierarchyResolver.ResolvedAccount>()
            var previous: AccountHierarchyResolver.ResolvedAccount? = null

            for (current in result) {
                if (previous == null) {
                    previous = current
                    continue
                }

                val shouldKeep = shouldKeepAccount(previous, current)
                if (shouldKeep) {
                    newResult.add(previous)
                } else {
                    removed = true
                }

                previous = current
            }

            // Handle the last account
            if (previous != null) {
                if (hasNonZeroBalance(previous.account)) {
                    newResult.add(previous)
                } else {
                    removed = true
                }
            }

            result = newResult
        }

        return result
    }

    /**
     * Determine if an account should be kept in the filtered list.
     *
     * An account is kept if:
     * - It has a non-zero balance
     * - It is a parent of the next account in the list (preserving hierarchy)
     */
    private fun shouldKeepAccount(
        account: AccountHierarchyResolver.ResolvedAccount,
        nextAccount: AccountHierarchyResolver.ResolvedAccount?
    ): Boolean {
        // Always keep accounts with non-zero balance
        if (hasNonZeroBalance(account.account)) {
            return true
        }

        // Keep parents of the next account (preserving hierarchy)
        if (nextAccount != null && isParentOf(account.account.name, nextAccount.account.name)) {
            return true
        }

        return false
    }

    /**
     * Check if an account has any non-zero amounts.
     */
    private fun hasNonZeroBalance(account: Account): Boolean = account.amounts.any { it.amount != 0f }

    /**
     * Check if parentName is a parent of childName.
     *
     * A parent relationship is determined by the child name starting with
     * the parent name followed by a colon.
     */
    private fun isParentOf(parentName: String, childName: String): Boolean = childName.startsWith("$parentName:")
}
