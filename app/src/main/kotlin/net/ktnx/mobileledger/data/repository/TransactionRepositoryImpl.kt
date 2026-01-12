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
import java.util.Locale
import net.ktnx.mobileledger.dao.AccountDAO
import net.ktnx.mobileledger.dao.AccountValueDAO
import net.ktnx.mobileledger.dao.TransactionAccountDAO
import net.ktnx.mobileledger.dao.TransactionDAO
import net.ktnx.mobileledger.db.Account
import net.ktnx.mobileledger.db.AccountValue
import net.ktnx.mobileledger.db.Transaction
import net.ktnx.mobileledger.db.TransactionWithAccounts
import net.ktnx.mobileledger.model.LedgerAccount
import net.ktnx.mobileledger.utils.Logger
import net.ktnx.mobileledger.utils.Misc

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
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDAO: TransactionDAO,
    private val transactionAccountDAO: TransactionAccountDAO,
    private val accountDAO: AccountDAO,
    private val accountValueDAO: AccountValueDAO
) : TransactionRepository {

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
            appendTransactionInternal(transaction)
        }
    }

    /**
     * Internal method to append a new transaction.
     * Creates accounts and updates account values as needed.
     */
    private fun appendTransactionInternal(rec: TransactionWithAccounts) {
        val transaction = rec.transaction
        val profileId = transaction.profileId
        transaction.generation = transactionDAO.getGenerationSync(profileId)
        transaction.ledgerId = transactionDAO.getMaxLedgerIdSync(profileId) + 1
        transaction.id = transactionDAO.insertSync(transaction)

        for (trAcc in rec.accounts ?: emptyList()) {
            trAcc.transactionId = transaction.id
            trAcc.generation = transaction.generation
            trAcc.id = transactionAccountDAO.insertSync(trAcc)

            var accName: String? = trAcc.accountName
            while (accName != null) {
                var acc = accountDAO.getByNameSync(profileId, accName)
                if (acc == null) {
                    acc = Account()
                    acc.profileId = profileId
                    acc.name = accName
                    acc.nameUpper = accName.uppercase()
                    acc.parentName = LedgerAccount.extractParentName(accName)
                    acc.level = LedgerAccount.determineLevel(acc.name)
                    acc.generation = trAcc.generation

                    acc.id = accountDAO.insertSync(acc)
                }

                var accVal = accountValueDAO.getByCurrencySync(acc.id, trAcc.currency)
                if (accVal == null) {
                    accVal = AccountValue()
                    accVal.accountId = acc.id
                    accVal.generation = trAcc.generation
                    accVal.currency = trAcc.currency
                    accVal.value = trAcc.amount
                    accVal.id = accountValueDAO.insertSync(accVal)
                } else {
                    accVal.value = accVal.value + trAcc.amount
                    accountValueDAO.updateSync(accVal)
                }

                accName = LedgerAccount.extractParentName(accName)
            }
        }
    }

    override suspend fun storeTransaction(transaction: TransactionWithAccounts) {
        withContext(Dispatchers.IO) {
            storeTransactionInternal(transaction)
        }
    }

    /**
     * Internal method to store a transaction with its accounts.
     * Handles both insert and update cases based on ledger ID.
     */
    private fun storeTransactionInternal(rec: TransactionWithAccounts) {
        var transaction = rec.transaction
        val existing = transactionDAO.getByLedgerId(transaction.profileId, transaction.ledgerId)
        if (existing != null) {
            if (Misc.equalStrings(transaction.dataHash, existing.dataHash)) {
                transactionDAO.updateGenerationWithAccounts(existing.id, rec.transaction.generation)
                return
            }

            existing.copyDataFrom(transaction)
            transactionDAO.updateSync(existing)

            transaction = existing
        } else {
            transaction.id = transactionDAO.insertSync(transaction)
        }

        for (trAcc in rec.accounts ?: emptyList()) {
            trAcc.transactionId = transaction.id
            trAcc.generation = transaction.generation
            val existingAcc = transactionAccountDAO.getByOrderNoSync(trAcc.transactionId, trAcc.orderNo)
            if (existingAcc != null) {
                existingAcc.copyDataFrom(trAcc)
                transactionAccountDAO.updateSync(existingAcc)
            } else {
                trAcc.id = transactionAccountDAO.insertSync(trAcc)
            }
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
            val generation = transactionDAO.getGenerationSync(profileId) + 1

            for (tr in transactions) {
                tr.transaction.generation = generation
                tr.transaction.profileId = profileId
                storeTransactionInternal(tr)
            }

            Logger.debug("Transaction", "Purging old transactions")
            var removed = transactionDAO.purgeOldTransactionsSync(profileId, generation)
            Logger.debug("Transaction", String.format(Locale.ROOT, "Purged %d transactions", removed))

            removed = transactionDAO.purgeOldTransactionAccountsSync(profileId, generation)
            Logger.debug("Transaction", String.format(Locale.ROOT, "Purged %d transaction accounts", removed))
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
