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

package net.ktnx.mobileledger.core.testing.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import net.ktnx.mobileledger.core.domain.model.Account
import net.ktnx.mobileledger.core.domain.repository.AccountRepository

/**
 * Fake AccountRepository for ViewModel testing.
 *
 * Uses domain models (Account) for query operations.
 */
class FakeAccountRepository : AccountRepository {
    // Internal storage using domain models
    private val domainAccounts = mutableMapOf<Long, MutableList<Account>>()

    // Track account names for search operations
    private val accountNames = mutableMapOf<Long, MutableList<String>>()

    private var nextId = 1L

    /**
     * Add a domain model account for testing.
     */
    fun addAccount(profileId: Long, account: Account) {
        val id = account.id ?: nextId++
        val accountWithId = if (account.id == null) account.copy(id = id) else account
        domainAccounts.getOrPut(profileId) { mutableListOf() }.add(accountWithId)
        accountNames.getOrPut(profileId) { mutableListOf() }.add(account.name)
    }

    /**
     * Convenience method to add an account by name only.
     */
    fun addAccount(profileId: Long, name: String) {
        addAccount(
            profileId,
            Account(
                id = null,
                name = name,
                level = name.count { it == ':' },
                isExpanded = true,
                amounts = emptyList()
            )
        )
    }

    // Flow methods (observe prefix)
    override fun observeAllWithAmounts(profileId: Long, includeZeroBalances: Boolean): Flow<List<Account>> {
        val accounts = domainAccounts[profileId] ?: emptyList()
        val filtered = if (includeZeroBalances) {
            accounts
        } else {
            accounts.filter { it.hasAmounts && it.amounts.any { amt -> amt.amount != 0f } }
        }
        return MutableStateFlow(filtered)
    }

    override fun observeByNameWithAmounts(profileId: Long, accountName: String): Flow<Account?> =
        MutableStateFlow(domainAccounts[profileId]?.find { it.name == accountName })

    override fun observeSearchAccountNames(profileId: Long, term: String): Flow<List<String>> = MutableStateFlow(
        accountNames[profileId]?.filter { it.contains(term, ignoreCase = true) } ?: emptyList()
    )

    override fun observeSearchAccountNamesGlobal(term: String): Flow<List<String>> =
        MutableStateFlow(accountNames.values.flatten().filter { it.contains(term, ignoreCase = true) })

    // Suspend methods (no suffix)
    override suspend fun getAllWithAmounts(profileId: Long, includeZeroBalances: Boolean): Result<List<Account>> {
        val accounts = domainAccounts[profileId] ?: emptyList()
        val filtered = if (includeZeroBalances) {
            accounts
        } else {
            accounts.filter { it.hasAmounts && it.amounts.any { amt -> amt.amount != 0f } }
        }
        return Result.success(filtered)
    }

    override suspend fun getByNameWithAmounts(profileId: Long, accountName: String): Result<Account?> =
        Result.success(domainAccounts[profileId]?.find { it.name == accountName })

    override suspend fun searchAccountNames(profileId: Long, term: String): Result<List<String>> =
        Result.success(accountNames[profileId]?.filter { it.contains(term, ignoreCase = true) } ?: emptyList())

    override suspend fun searchAccountsWithAmounts(profileId: Long, term: String): Result<List<Account>> =
        Result.success(domainAccounts[profileId]?.filter { it.name.contains(term, ignoreCase = true) } ?: emptyList())

    override suspend fun searchAccountNamesGlobal(term: String): Result<List<String>> =
        Result.success(accountNames.values.flatten().filter { it.contains(term, ignoreCase = true) })

    override suspend fun storeAccountsAsDomain(accounts: List<Account>, profileId: Long): Result<Unit> {
        domainAccounts[profileId] = accounts.toMutableList()
        accountNames[profileId] = accounts.map { it.name }.toMutableList()
        return Result.success(Unit)
    }

    override suspend fun getCountForProfile(profileId: Long): Result<Int> =
        Result.success(domainAccounts[profileId]?.size ?: 0)

    override suspend fun deleteAllAccounts(): Result<Unit> {
        domainAccounts.clear()
        accountNames.clear()
        return Result.success(Unit)
    }
}
