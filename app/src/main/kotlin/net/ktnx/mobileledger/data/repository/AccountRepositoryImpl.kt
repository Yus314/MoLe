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

package net.ktnx.mobileledger.data.repository

import androidx.lifecycle.asFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import net.ktnx.mobileledger.dao.AccountDAO
import net.ktnx.mobileledger.db.Account
import net.ktnx.mobileledger.db.AccountWithAmounts

/**
 * Implementation of [AccountRepository] that wraps the existing [AccountDAO].
 *
 * This implementation:
 * - Converts LiveData to Flow for reactive data access
 * - Uses Dispatchers.IO for database operations
 * - Delegates all operations to the underlying DAO
 *
 * Thread-safety: All operations are safe to call from any coroutine context.
 */
@Singleton
class AccountRepositoryImpl @Inject constructor(private val accountDAO: AccountDAO) : AccountRepository {

    // ========================================
    // Query Operations
    // ========================================

    override fun getAllWithAmounts(profileId: Long, includeZeroBalances: Boolean): Flow<List<AccountWithAmounts>> =
        accountDAO.getAllWithAmounts(profileId, includeZeroBalances).asFlow()

    override suspend fun getAllWithAmountsSync(
        profileId: Long,
        includeZeroBalances: Boolean
    ): List<AccountWithAmounts> = withContext(Dispatchers.IO) {
        accountDAO.getAllWithAmountsSync(profileId, includeZeroBalances)
    }

    override fun getAll(profileId: Long, includeZeroBalances: Boolean): Flow<List<Account>> =
        accountDAO.getAll(profileId, includeZeroBalances).asFlow()

    override suspend fun getByIdSync(id: Long): Account? = withContext(Dispatchers.IO) {
        accountDAO.getByIdSync(id)
    }

    override fun getByName(profileId: Long, accountName: String): Flow<Account?> =
        accountDAO.getByName(profileId, accountName).asFlow()

    override suspend fun getByNameSync(profileId: Long, accountName: String): Account? = withContext(Dispatchers.IO) {
        accountDAO.getByNameSync(profileId, accountName)
    }

    override fun getByNameWithAmounts(profileId: Long, accountName: String): Flow<AccountWithAmounts?> =
        accountDAO.getByNameWithAmounts(profileId, accountName).asFlow()

    // ========================================
    // Search Operations
    // ========================================

    override fun searchAccountNames(profileId: Long, term: String): Flow<List<String>> =
        accountDAO.lookupNamesInProfileByName(profileId, term.uppercase()).asFlow()
            .map { containers -> AccountDAO.unbox(containers) }

    override suspend fun searchAccountNamesSync(profileId: Long, term: String): List<String> =
        withContext(Dispatchers.IO) {
            AccountDAO.unbox(accountDAO.lookupNamesInProfileByNameSync(profileId, term.uppercase()))
        }

    override suspend fun searchAccountsWithAmountsSync(profileId: Long, term: String): List<AccountWithAmounts> =
        withContext(Dispatchers.IO) {
            accountDAO.lookupWithAmountsInProfileByNameSync(profileId, term.uppercase())
        }

    override fun searchAccountNamesGlobal(term: String): Flow<List<String>> =
        accountDAO.lookupNamesByName(term.uppercase()).asFlow()
            .map { containers -> AccountDAO.unbox(containers) }

    override suspend fun searchAccountNamesGlobalSync(term: String): List<String> = withContext(Dispatchers.IO) {
        AccountDAO.unbox(accountDAO.lookupNamesByNameSync(term.uppercase()))
    }

    // ========================================
    // Mutation Operations
    // ========================================

    override suspend fun insertAccount(account: Account): Long = withContext(Dispatchers.IO) {
        accountDAO.insertSync(account)
    }

    override suspend fun insertAccountWithAmounts(accountWithAmounts: AccountWithAmounts) {
        withContext(Dispatchers.IO) {
            accountDAO.insertSync(accountWithAmounts)
        }
    }

    override suspend fun updateAccount(account: Account) {
        withContext(Dispatchers.IO) {
            accountDAO.updateSync(account)
        }
    }

    override suspend fun deleteAccount(account: Account) {
        withContext(Dispatchers.IO) {
            accountDAO.deleteSync(account)
        }
    }

    // ========================================
    // Sync Operations
    // ========================================

    override suspend fun storeAccounts(accounts: List<AccountWithAmounts>, profileId: Long) {
        withContext(Dispatchers.IO) {
            accountDAO.storeAccountsSync(accounts, profileId)
        }
    }

    override suspend fun getCountForProfile(profileId: Long): Int = withContext(Dispatchers.IO) {
        accountDAO.getCountForProfileSync(profileId)
    }

    override suspend fun deleteAllAccounts() {
        withContext(Dispatchers.IO) {
            accountDAO.deleteAllSync()
        }
    }
}
