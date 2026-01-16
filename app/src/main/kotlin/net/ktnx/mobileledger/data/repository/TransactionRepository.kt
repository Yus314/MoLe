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
import net.ktnx.mobileledger.db.Transaction as DbTransaction
import net.ktnx.mobileledger.db.TransactionWithAccounts
import net.ktnx.mobileledger.domain.model.Transaction

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
 *
 * ## Domain Model Migration (017-domain-model-layer)
 *
 * Query operations now return domain models (Transaction) instead of db entities.
 * Mutation operations still accept db entities for backward compatibility.
 * Use the domain model methods for UI layer consumption.
 */
interface TransactionRepository {

    // ========================================
    // Query Operations (Domain Models)
    // ========================================

    /**
     * Get all transactions with their accounts for a profile.
     *
     * @param profileId The profile ID to filter by.
     * @return Flow emitting the list of domain model transactions whenever data changes.
     */
    fun getAllTransactions(profileId: Long): Flow<List<Transaction>>

    /**
     * Get transactions filtered by account name.
     *
     * @param profileId The profile ID to filter by.
     * @param accountName Optional account name filter. If null, returns all transactions.
     * @return Flow emitting the filtered list of domain model transactions.
     */
    fun getTransactionsFiltered(profileId: Long, accountName: String?): Flow<List<Transaction>>

    /**
     * Get a transaction by its ID.
     *
     * @param transactionId The transaction ID.
     * @return Flow emitting the domain model transaction, or null if not found.
     */
    fun getTransactionById(transactionId: Long): Flow<Transaction?>

    /**
     * Get a transaction by its ID (synchronous version for one-time reads).
     *
     * @param transactionId The transaction ID.
     * @return The domain model transaction, or null if not found.
     */
    suspend fun getTransactionByIdSync(transactionId: Long): Transaction?

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
     * @return The first matching domain model transaction, or null if none found.
     */
    suspend fun getFirstByDescription(description: String): Transaction?

    /**
     * Get the first transaction matching a description and having a specific account.
     *
     * @param description The exact description to match.
     * @param accountTerm The account name to filter by.
     * @return The first matching domain model transaction, or null if none found.
     */
    suspend fun getFirstByDescriptionHavingAccount(description: String, accountTerm: String): Transaction?

    // ========================================
    // Mutation Operations (Domain Models)
    // ========================================

    /**
     * Insert a new transaction using domain model.
     *
     * @param transaction The domain model transaction to insert.
     * @param profileId The profile ID for the transaction.
     * @return The inserted transaction with generated ID.
     */
    suspend fun insertTransaction(transaction: Transaction, profileId: Long): Transaction

    /**
     * Store (insert or update) a transaction using domain model.
     * Uses the transaction's ledgerId to detect duplicates.
     *
     * @param transaction The domain model transaction to store.
     * @param profileId The profile ID for the transaction.
     */
    suspend fun storeTransaction(transaction: Transaction, profileId: Long)

    // ========================================
    // Mutation Operations (DB Entities - Legacy)
    // Note: These are kept for backward compatibility during migration.
    // ========================================

    /**
     * Insert a new transaction with its accounts.
     *
     * @param transaction The db entity transaction to insert.
     */
    suspend fun insertTransaction(transaction: TransactionWithAccounts)

    /**
     * Store (insert or update) a transaction.
     * Uses the transaction's dataHash to detect duplicates.
     *
     * @param transaction The db entity transaction to store.
     */
    suspend fun storeTransaction(transaction: TransactionWithAccounts)

    /**
     * Delete a transaction.
     *
     * @param transaction The db entity transaction to delete.
     */
    suspend fun deleteTransaction(transaction: DbTransaction)

    /**
     * Delete multiple transactions.
     *
     * @param transactions The db entity transactions to delete.
     */
    suspend fun deleteTransactions(transactions: List<DbTransaction>)

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
     * Store multiple transactions from sync using domain models.
     * This will purge old transactions not in the current generation.
     *
     * @param transactions The domain model transactions to store.
     * @param profileId The profile ID for the transactions.
     */
    suspend fun storeTransactionsAsDomain(transactions: List<Transaction>, profileId: Long)

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
