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
import net.ktnx.mobileledger.dao.TransactionDAO
import net.ktnx.mobileledger.db.Transaction
import net.ktnx.mobileledger.db.TransactionWithAccounts

/**
 * Repository for managing Transaction data.
 *
 * This repository provides:
 * - CRUD operations for transactions
 * - Search and filter operations
 * - Batch operations for sync
 * - Reactive data streams via Flow
 *
 * All transactions are scoped to a profile (profileId).
 *
 * Thread-safety: All suspend functions are safe to call from any coroutine context.
 * Flow emissions are safe for concurrent collectors.
 */
interface TransactionRepository {

    // ========================================
    // Query Operations
    // ========================================

    /**
     * Get all transactions with their accounts for a profile.
     *
     * @param profileId The profile ID to filter by.
     * @return Flow emitting the list of transactions whenever data changes.
     */
    fun getAllTransactions(profileId: Long): Flow<List<TransactionWithAccounts>>

    /**
     * Get transactions filtered by account name.
     *
     * @param profileId The profile ID to filter by.
     * @param accountName Optional account name filter. If null, returns all transactions.
     * @return Flow emitting the filtered list of transactions.
     */
    fun getTransactionsFiltered(profileId: Long, accountName: String?): Flow<List<TransactionWithAccounts>>

    /**
     * Get a transaction by its ID.
     *
     * @param transactionId The transaction ID.
     * @return Flow emitting the transaction, or null if not found.
     */
    fun getTransactionById(transactionId: Long): Flow<TransactionWithAccounts?>

    /**
     * Get a transaction by its ID (synchronous version for one-time reads).
     *
     * @param transactionId The transaction ID.
     * @return The transaction, or null if not found.
     */
    suspend fun getTransactionByIdSync(transactionId: Long): TransactionWithAccounts?

    /**
     * Search transaction descriptions matching a term.
     *
     * @param term The search term.
     * @return List of matching description containers.
     */
    suspend fun searchByDescription(term: String): List<TransactionDAO.DescriptionContainer>

    /**
     * Get the first transaction matching a description.
     * Useful for auto-filling from previous transactions.
     *
     * @param description The exact description to match.
     * @return The first matching transaction, or null if none found.
     */
    suspend fun getFirstByDescription(description: String): TransactionWithAccounts?

    /**
     * Get the first transaction matching a description and having a specific account.
     *
     * @param description The exact description to match.
     * @param accountTerm The account name to filter by.
     * @return The first matching transaction, or null if none found.
     */
    suspend fun getFirstByDescriptionHavingAccount(description: String, accountTerm: String): TransactionWithAccounts?

    // ========================================
    // Mutation Operations
    // ========================================

    /**
     * Insert a new transaction with its accounts.
     *
     * @param transaction The transaction to insert.
     */
    suspend fun insertTransaction(transaction: TransactionWithAccounts)

    /**
     * Store (insert or update) a transaction.
     * Uses the transaction's dataHash to detect duplicates.
     *
     * @param transaction The transaction to store.
     */
    suspend fun storeTransaction(transaction: TransactionWithAccounts)

    /**
     * Delete a transaction.
     *
     * @param transaction The transaction to delete.
     */
    suspend fun deleteTransaction(transaction: Transaction)

    /**
     * Delete multiple transactions.
     *
     * @param transactions The transactions to delete.
     */
    suspend fun deleteTransactions(transactions: List<Transaction>)

    // ========================================
    // Sync Operations
    // ========================================

    /**
     * Store multiple transactions from sync.
     * This will purge old transactions not in the current generation.
     *
     * @param transactions The transactions to store.
     * @param profileId The profile ID for the transactions.
     */
    suspend fun storeTransactions(transactions: List<TransactionWithAccounts>, profileId: Long)

    /**
     * Delete all transactions for a profile.
     *
     * @param profileId The profile ID.
     * @return The number of deleted transactions.
     */
    suspend fun deleteAllForProfile(profileId: Long): Int

    /**
     * Get the maximum ledger ID for a profile.
     * Used for sync operations.
     *
     * @param profileId The profile ID.
     * @return The maximum ledger ID, or null if no transactions exist.
     */
    suspend fun getMaxLedgerId(profileId: Long): Long?
}
