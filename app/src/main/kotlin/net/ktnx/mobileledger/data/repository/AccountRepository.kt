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

import kotlinx.coroutines.flow.Flow
import net.ktnx.mobileledger.dao.AccountDAO
import net.ktnx.mobileledger.db.Account as DbAccount
import net.ktnx.mobileledger.db.AccountWithAmounts
import net.ktnx.mobileledger.domain.model.Account

/**
 * Repository interface for Account data access.
 *
 * This repository provides:
 * - Reactive access to accounts via Flow
 * - Account lookup and search operations
 * - Batch sync operations for account data
 *
 * ## Usage
 *
 * ```kotlin
 * @HiltViewModel
 * class AccountViewModel @Inject constructor(
 *     private val accountRepository: AccountRepository
 * ) : ViewModel() {
 *     fun loadAccounts(profileId: Long, includeZeroBalances: Boolean) =
 *         accountRepository.getAllWithAmounts(profileId, includeZeroBalances)
 *             .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
 * }
 * ```
 */
interface AccountRepository {

    // ========================================
    // Query Operations (Flow - observe prefix)
    // ========================================

    /**
     * Observe all accounts for a profile with their amounts.
     *
     * @param profileId The profile ID
     * @param includeZeroBalances Whether to include accounts with zero balance
     * @return Flow that emits the account list whenever it changes
     */
    fun observeAllWithAmounts(profileId: Long, includeZeroBalances: Boolean): Flow<List<Account>>

    /**
     * Observe all accounts for a profile (without amounts).
     *
     * @param profileId The profile ID
     * @param includeZeroBalances Whether to include accounts with zero balance
     * @return Flow that emits the account list whenever it changes
     */
    fun observeAll(profileId: Long, includeZeroBalances: Boolean): Flow<List<DbAccount>>

    /**
     * Observe an account by name within a profile.
     *
     * @param profileId The profile ID
     * @param accountName The account name
     * @return Flow that emits the account when it changes
     */
    fun observeByName(profileId: Long, accountName: String): Flow<DbAccount?>

    /**
     * Observe an account by name with its amounts.
     *
     * @param profileId The profile ID
     * @param accountName The account name
     * @return Flow that emits the account with amounts when it changes
     */
    fun observeByNameWithAmounts(profileId: Long, accountName: String): Flow<Account?>

    // ========================================
    // Search Operations (Flow - observe prefix)
    // ========================================

    /**
     * Observe account names matching a term within a profile.
     *
     * @param profileId The profile ID
     * @param term The search term
     * @return Flow that emits matching account names
     */
    fun observeSearchAccountNames(profileId: Long, term: String): Flow<List<String>>

    /**
     * Observe account names matching a term across all profiles.
     *
     * @param term The search term
     * @return Flow that emits matching account names
     */
    fun observeSearchAccountNamesGlobal(term: String): Flow<List<String>>

    // ========================================
    // Query Operations (suspend - no suffix)
    // ========================================

    /**
     * Get all accounts for a profile with their amounts.
     *
     * @param profileId The profile ID
     * @param includeZeroBalances Whether to include accounts with zero balance
     * @return The account list
     */
    suspend fun getAllWithAmounts(profileId: Long, includeZeroBalances: Boolean): List<Account>

    /**
     * Get an account by its ID.
     *
     * @param id The account ID
     * @return The account or null if not found
     */
    suspend fun getById(id: Long): DbAccount?

    /**
     * Get an account by name within a profile.
     *
     * @param profileId The profile ID
     * @param accountName The account name
     * @return The account or null if not found
     */
    suspend fun getByName(profileId: Long, accountName: String): DbAccount?

    /**
     * Get an account by name with its amounts.
     *
     * @param profileId The profile ID
     * @param accountName The account name
     * @return The account with amounts or null if not found
     */
    suspend fun getByNameWithAmounts(profileId: Long, accountName: String): Account?

    // ========================================
    // Search Operations (suspend - no suffix)
    // ========================================

    /**
     * Search for account names matching a term within a profile.
     *
     * @param profileId The profile ID
     * @param term The search term
     * @return List of matching account names
     */
    suspend fun searchAccountNames(profileId: Long, term: String): List<String>

    /**
     * Search for accounts with amounts matching a term within a profile.
     *
     * @param profileId The profile ID
     * @param term The search term
     * @return List of matching accounts with amounts
     */
    suspend fun searchAccountsWithAmounts(profileId: Long, term: String): List<Account>

    /**
     * Search for account names matching a term across all profiles.
     *
     * @param term The search term
     * @return List of matching account names
     */
    suspend fun searchAccountNamesGlobal(term: String): List<String>

    // ========================================
    // Mutation Operations
    // ========================================

    /**
     * Insert an account.
     *
     * @param account The account to insert
     * @return The ID of the inserted account
     */
    suspend fun insertAccount(account: DbAccount): Long

    /**
     * Insert an account with its amounts.
     *
     * @param accountWithAmounts The account with amounts to insert
     */
    suspend fun insertAccountWithAmounts(accountWithAmounts: AccountWithAmounts)

    /**
     * Update an existing account.
     *
     * @param account The account to update
     */
    suspend fun updateAccount(account: DbAccount)

    /**
     * Delete an account.
     *
     * @param account The account to delete
     */
    suspend fun deleteAccount(account: DbAccount)

    // ========================================
    // Sync Operations
    // ========================================

    /**
     * Store a batch of accounts with amounts for a profile.
     *
     * This operation handles generation tracking and purges old data.
     *
     * @param accounts The accounts to store
     * @param profileId The profile ID
     */
    suspend fun storeAccounts(accounts: List<AccountWithAmounts>, profileId: Long)

    /**
     * Store a batch of accounts using domain models.
     *
     * This operation handles generation tracking, db entity conversion, and purges old data.
     * Used by sync operations that work with domain models.
     *
     * @param accounts The domain model accounts to store
     * @param profileId The profile ID
     */
    suspend fun storeAccountsAsDomain(accounts: List<Account>, profileId: Long)

    /**
     * Get the count of accounts for a profile.
     *
     * @param profileId The profile ID
     * @return The account count
     */
    suspend fun getCountForProfile(profileId: Long): Int

    /**
     * Delete all accounts.
     */
    suspend fun deleteAllAccounts()
}
