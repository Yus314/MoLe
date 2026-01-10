/*
 * Repository Interface Contract: AccountRepository
 * Feature: 008-data-layer-repository
 *
 * This file defines the contract for AccountRepository.
 * Implementation will be in AccountRepositoryImpl.
 */

package net.ktnx.mobileledger.data.repository

import kotlinx.coroutines.flow.Flow
import net.ktnx.mobileledger.db.Account
import net.ktnx.mobileledger.db.AccountWithAmounts
import net.ktnx.mobileledger.model.AccountNameContainer

/**
 * Repository for managing Account data and balances.
 *
 * This repository provides:
 * - CRUD operations for accounts
 * - Balance (AccountValue) management
 * - Search and filter operations
 * - Batch operations for sync
 * - Reactive data streams via Flow
 *
 * All accounts are scoped to a profile (profileId).
 */
interface AccountRepository {

    // ========================================
    // Query Operations
    // ========================================

    /**
     * Get all accounts with their balances for a profile.
     *
     * @param profileId The profile ID to filter by.
     * @param includeZeroBalances Whether to include accounts with zero balance.
     * @return Flow emitting the list of accounts whenever data changes.
     */
    fun getAllAccounts(profileId: Long, includeZeroBalances: Boolean = false): Flow<List<AccountWithAmounts>>

    /**
     * Get an account by its ID.
     *
     * @param accountId The account ID.
     * @return The account, or null if not found.
     */
    suspend fun getAccountById(accountId: Long): Account?

    /**
     * Get an account by its name.
     *
     * @param profileId The profile ID.
     * @param accountName The exact account name.
     * @return Flow emitting the account, or null if not found.
     */
    fun getAccountByName(profileId: Long, accountName: String): Flow<AccountWithAmounts?>

    /**
     * Get an account by its name (synchronous version).
     *
     * @param profileId The profile ID.
     * @param accountName The exact account name.
     * @return The account, or null if not found.
     */
    suspend fun getAccountByNameSync(profileId: Long, accountName: String): Account?

    /**
     * Search account names matching a term.
     *
     * @param profileId The profile ID to search within.
     * @param term The search term.
     * @return Flow emitting matching account names.
     */
    fun searchAccountNames(profileId: Long, term: String): Flow<List<AccountNameContainer>>

    /**
     * Search account names matching a term (synchronous version).
     *
     * @param profileId The profile ID to search within.
     * @param term The search term.
     * @return List of matching account names.
     */
    suspend fun searchAccountNamesSync(profileId: Long, term: String): List<AccountNameContainer>

    /**
     * Search accounts with balances matching a term.
     *
     * @param profileId The profile ID to search within.
     * @param term The search term.
     * @return List of matching accounts with their balances.
     */
    suspend fun searchAccountsWithAmounts(profileId: Long, term: String): List<AccountWithAmounts>

    /**
     * Get the total number of accounts for a profile.
     *
     * @param profileId The profile ID.
     * @return The count of accounts.
     */
    suspend fun getAccountCount(profileId: Long): Int

    // ========================================
    // Mutation Operations
    // ========================================

    /**
     * Insert a new account with its balances.
     *
     * @param account The account to insert.
     * @return The generated ID for the new account.
     */
    suspend fun insertAccount(account: AccountWithAmounts): Long

    /**
     * Update an existing account.
     *
     * @param account The account with updated values.
     */
    suspend fun updateAccount(account: Account)

    /**
     * Delete an account.
     *
     * @param account The account to delete.
     */
    suspend fun deleteAccount(account: Account)

    // ========================================
    // Sync Operations
    // ========================================

    /**
     * Store multiple accounts from sync.
     * This will purge old accounts not in the current generation.
     *
     * @param accounts The accounts to store.
     * @param profileId The profile ID for the accounts.
     */
    suspend fun storeAccounts(accounts: List<AccountWithAmounts>, profileId: Long)
}
