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
import kotlinx.coroutines.withContext
import net.ktnx.mobileledger.dao.TransactionDAO
import net.ktnx.mobileledger.db.Transaction
import net.ktnx.mobileledger.db.TransactionWithAccounts

/**
 * Implementation of [TransactionRepository] that wraps the existing [TransactionDAO].
 *
 * This implementation:
 * - Converts LiveData to Flow for reactive data access
 * - Uses Dispatchers.IO for database operations
 * - Delegates all operations to the underlying DAO
 *
 * Thread-safety: All operations are safe to call from any coroutine context.
 */
@Singleton
class TransactionRepositoryImpl @Inject constructor(private val transactionDAO: TransactionDAO) :
    TransactionRepository {

    // ========================================
    // Query Operations
    // ========================================

    override fun getAllTransactions(profileId: Long): Flow<List<TransactionWithAccounts>> =
        transactionDAO.getAllWithAccounts(profileId).asFlow()

    override fun getTransactionsFiltered(profileId: Long, accountName: String?): Flow<List<TransactionWithAccounts>> =
        if (accountName == null) {
            transactionDAO.getAllWithAccounts(profileId).asFlow()
        } else {
            transactionDAO.getAllWithAccountsFiltered(profileId, accountName).asFlow()
        }

    override fun getTransactionById(transactionId: Long): Flow<TransactionWithAccounts?> =
        transactionDAO.getByIdWithAccounts(transactionId).asFlow()

    override suspend fun getTransactionByIdSync(transactionId: Long): TransactionWithAccounts? =
        withContext(Dispatchers.IO) {
            transactionDAO.getByIdWithAccountsSync(transactionId)
        }

    override suspend fun searchByDescription(term: String): List<TransactionDAO.DescriptionContainer> =
        withContext(Dispatchers.IO) {
            transactionDAO.lookupDescriptionSync(term.uppercase())
        }

    override suspend fun getFirstByDescription(description: String): TransactionWithAccounts? =
        withContext(Dispatchers.IO) {
            transactionDAO.getFirstByDescriptionSync(description)
        }

    override suspend fun getFirstByDescriptionHavingAccount(
        description: String,
        accountTerm: String
    ): TransactionWithAccounts? = withContext(Dispatchers.IO) {
        transactionDAO.getFirstByDescriptionHavingAccountSync(description, accountTerm)
    }

    // ========================================
    // Mutation Operations
    // ========================================

    override suspend fun insertTransaction(transaction: TransactionWithAccounts) {
        withContext(Dispatchers.IO) {
            transactionDAO.appendSync(transaction)
        }
    }

    override suspend fun storeTransaction(transaction: TransactionWithAccounts) {
        withContext(Dispatchers.IO) {
            transactionDAO.storeSync(transaction)
        }
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        withContext(Dispatchers.IO) {
            transactionDAO.deleteSync(transaction)
        }
    }

    override suspend fun deleteTransactions(transactions: List<Transaction>) {
        withContext(Dispatchers.IO) {
            transactionDAO.deleteSync(transactions)
        }
    }

    // ========================================
    // Sync Operations
    // ========================================

    override suspend fun storeTransactions(transactions: List<TransactionWithAccounts>, profileId: Long) {
        withContext(Dispatchers.IO) {
            transactionDAO.storeTransactionsSync(transactions, profileId)
        }
    }

    override suspend fun deleteAllForProfile(profileId: Long): Int = withContext(Dispatchers.IO) {
        transactionDAO.deleteAllSync(profileId)
    }

    override suspend fun getMaxLedgerId(profileId: Long): Long? = withContext(Dispatchers.IO) {
        val result = transactionDAO.getMaxLedgerIdSync(profileId)
        if (result == 0L) null else result
    }
}
