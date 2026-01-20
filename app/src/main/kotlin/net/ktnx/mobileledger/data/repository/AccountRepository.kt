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
import net.ktnx.mobileledger.domain.model.Account

/**
 * Repository interface for Account data access.
 *
 * This repository provides:
 * - Reactive access to accounts via Flow
 * - Account lookup and search operations
 * - Batch sync operations for account data
 *
 * ## Error Handling
 *
 * All suspend functions return `Result<T>` to handle errors explicitly.
 * Use `result.getOrNull()`, `result.getOrElse {}`, or `result.onSuccess/onFailure` to handle results.
 *
 * ## Usage
 *
 * ```kotlin
 * @HiltViewModel
 * class AccountViewModel @Inject constructor(
 *     private val accountRepository: AccountRepository
 * ) : ViewModel() {
 *     fun loadAccounts(profileId: Long, includeZeroBalances: Boolean) =
 *         accountRepository.observeAllWithAmounts(profileId, includeZeroBalances)
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
     * @return Result containing the account list
     */
    suspend fun getAllWithAmounts(profileId: Long, includeZeroBalances: Boolean): Result<List<Account>>

    /**
     * Get an account by name with its amounts.
     *
     * @param profileId The profile ID
     * @param accountName The account name
     * @return Result containing the account with amounts or null if not found
     */
    suspend fun getByNameWithAmounts(profileId: Long, accountName: String): Result<Account?>

    // ========================================
    // Search Operations (suspend - no suffix)
    // ========================================

    /**
     * Search for account names matching a term within a profile.
     *
     * @param profileId The profile ID
     * @param term The search term
     * @return Result containing list of matching account names
     */
    suspend fun searchAccountNames(profileId: Long, term: String): Result<List<String>>

    /**
     * Search for accounts with amounts matching a term within a profile.
     *
     * @param profileId The profile ID
     * @param term The search term
     * @return Result containing list of matching accounts with amounts
     */
    suspend fun searchAccountsWithAmounts(profileId: Long, term: String): Result<List<Account>>

    /**
     * Search for account names matching a term across all profiles.
     *
     * @param term The search term
     * @return Result containing list of matching account names
     */
    suspend fun searchAccountNamesGlobal(term: String): Result<List<String>>

    // ========================================
    // Mutation Operations
    // ========================================

    /**
     * Store a batch of accounts using domain models.
     *
     * This operation handles generation tracking, db entity conversion, and purges old data.
     * Used by sync operations that work with domain models.
     *
     * @param accounts The domain model accounts to store
     * @param profileId The profile ID
     * @return Result indicating success or failure
     */
    suspend fun storeAccountsAsDomain(accounts: List<Account>, profileId: Long): Result<Unit>

    /**
     * Get the count of accounts for a profile.
     *
     * @param profileId The profile ID
     * @return Result containing the account count
     */
    suspend fun getCountForProfile(profileId: Long): Result<Int>

    /**
     * Delete all accounts.
     *
     * @return Result indicating success or failure
     */
    suspend fun deleteAllAccounts(): Result<Unit>
}
