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

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import net.ktnx.mobileledger.dao.AccountDAO
import net.ktnx.mobileledger.dao.AccountValueDAO
import net.ktnx.mobileledger.data.repository.mapper.AccountMapper.toDomain
import net.ktnx.mobileledger.data.repository.mapper.AccountMapper.toEntity
import net.ktnx.mobileledger.di.IoDispatcher
import net.ktnx.mobileledger.domain.model.Account
import net.ktnx.mobileledger.domain.repository.AccountRepository
import net.ktnx.mobileledger.domain.usecase.AppExceptionMapper

/**
 * Implementation of [AccountRepository] that wraps the existing [AccountDAO].
 *
 * This implementation:
 * - Converts LiveData to Flow for reactive data access
 * - Uses ioDispatcher for database operations
 * - Delegates all operations to the underlying DAO
 * - Returns Result<T> for all suspend operations with error handling
 *
 * Thread-safety: All operations are safe to call from any coroutine context.
 */
@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val accountDAO: AccountDAO,
    private val accountValueDAO: AccountValueDAO,
    private val appExceptionMapper: AppExceptionMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AccountRepository {

    // ========================================
    // Query Operations (Flow - observe prefix)
    // ========================================

    override fun observeAllWithAmounts(profileId: Long, includeZeroBalances: Boolean): Flow<List<Account>> =
        accountDAO.getAllWithAmounts(profileId, includeZeroBalances)
            .map { entities -> entities.map { it.toDomain() } }

    override fun observeByNameWithAmounts(profileId: Long, accountName: String): Flow<Account?> =
        accountDAO.getByNameWithAmounts(profileId, accountName)
            .map { it?.toDomain() }

    // ========================================
    // Search Operations (Flow - observe prefix)
    // ========================================

    override fun observeSearchAccountNames(profileId: Long, term: String): Flow<List<String>> =
        accountDAO.lookupNamesInProfileByName(profileId, term.uppercase())
            .map { containers -> AccountDAO.unbox(containers) }

    override fun observeSearchAccountNamesGlobal(term: String): Flow<List<String>> =
        accountDAO.lookupNamesByName(term.uppercase())
            .map { containers -> AccountDAO.unbox(containers) }

    // ========================================
    // Query Operations (suspend - no suffix)
    // ========================================

    override suspend fun getAllWithAmounts(profileId: Long, includeZeroBalances: Boolean): Result<List<Account>> =
        safeCall(appExceptionMapper) {
            withContext(ioDispatcher) {
                accountDAO.getAllWithAmountsSync(profileId, includeZeroBalances)
                    .map { it.toDomain() }
            }
        }

    override suspend fun getByNameWithAmounts(profileId: Long, accountName: String): Result<Account?> =
        safeCall(appExceptionMapper) {
            withContext(ioDispatcher) {
                accountDAO.getByNameWithAmountsSync(profileId, accountName)?.toDomain()
            }
        }

    // ========================================
    // Search Operations (suspend - no suffix)
    // ========================================

    override suspend fun searchAccountNames(profileId: Long, term: String): Result<List<String>> =
        safeCall(appExceptionMapper) {
            withContext(ioDispatcher) {
                AccountDAO.unbox(accountDAO.lookupNamesInProfileByNameSync(profileId, term.uppercase()))
            }
        }

    override suspend fun searchAccountsWithAmounts(profileId: Long, term: String): Result<List<Account>> =
        safeCall(appExceptionMapper) {
            withContext(ioDispatcher) {
                accountDAO.lookupWithAmountsInProfileByNameSync(profileId, term.uppercase())
                    .map { it.toDomain() }
            }
        }

    override suspend fun searchAccountNamesGlobal(term: String): Result<List<String>> = safeCall(appExceptionMapper) {
        withContext(ioDispatcher) {
            AccountDAO.unbox(accountDAO.lookupNamesByNameSync(term.uppercase()))
        }
    }

    // ========================================
    // Mutation Operations
    // ========================================

    override suspend fun storeAccountsAsDomain(accounts: List<Account>, profileId: Long): Result<Unit> =
        safeCall(appExceptionMapper) {
            withContext(ioDispatcher) {
                val generation = accountDAO.getGenerationSync(profileId) + 1

                for (domainAccount in accounts) {
                    val entity = domainAccount.toEntity(profileId)
                    entity.account.generation = generation

                    // Check for existing account to preserve amountsExpanded (not in domain model)
                    val existing = accountDAO.getByNameSync(profileId, domainAccount.name)
                    if (existing != null) {
                        entity.account.amountsExpanded = existing.amountsExpanded
                    }

                    // Insert account
                    entity.account.id = accountDAO.insertSync(entity.account)

                    // Insert amounts
                    for (value in entity.amounts.toList()) {
                        value.accountId = entity.account.id
                        value.generation = generation
                        value.id = accountValueDAO.insertSync(value)
                    }
                }
                accountDAO.purgeOldAccountsSync(profileId, generation)
                accountDAO.purgeOldAccountValuesSync(profileId, generation)
            }
        }

    override suspend fun getCountForProfile(profileId: Long): Result<Int> = safeCall(appExceptionMapper) {
        withContext(ioDispatcher) {
            accountDAO.getCountForProfileSync(profileId)
        }
    }

    override suspend fun deleteAllAccounts(): Result<Unit> = safeCall(appExceptionMapper) {
        withContext(ioDispatcher) {
            accountDAO.deleteAllSync()
        }
    }
}
