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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import logcat.logcat
import net.ktnx.mobileledger.dao.AccountDAO
import net.ktnx.mobileledger.dao.AccountValueDAO
import net.ktnx.mobileledger.dao.TransactionAccountDAO
import net.ktnx.mobileledger.dao.TransactionDAO
import net.ktnx.mobileledger.data.repository.mapper.TransactionMapper
import net.ktnx.mobileledger.db.Account
import net.ktnx.mobileledger.db.AccountValue
import net.ktnx.mobileledger.db.Transaction as DbTransaction
import net.ktnx.mobileledger.db.TransactionWithAccounts
import net.ktnx.mobileledger.domain.model.Transaction
import net.ktnx.mobileledger.utils.AccountNameUtils
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
    // Query Operations (Domain Models)
    // ========================================

    override fun getAllTransactions(profileId: Long): Flow<List<Transaction>> =
        transactionDAO.getAllWithAccounts(profileId)
            .map { entities -> TransactionMapper.toDomainList(entities) }

    override fun getTransactionsFiltered(profileId: Long, accountName: String?): Flow<List<Transaction>> =
        if (accountName == null) {
            transactionDAO.getAllWithAccounts(profileId)
        } else {
            transactionDAO.getAllWithAccountsFiltered(profileId, accountName)
        }.map { entities -> TransactionMapper.toDomainList(entities) }

    override fun getTransactionById(transactionId: Long): Flow<Transaction?> =
        transactionDAO.getByIdWithAccounts(transactionId)
            .map { entity -> entity?.let { TransactionMapper.toDomain(it) } }

    override suspend fun getTransactionByIdSync(transactionId: Long): Transaction? = withContext(Dispatchers.IO) {
        transactionDAO.getByIdWithAccountsSync(transactionId)?.let { TransactionMapper.toDomain(it) }
    }

    override suspend fun searchByDescription(term: String): List<TransactionDAO.DescriptionContainer> =
        withContext(Dispatchers.IO) {
            transactionDAO.lookupDescriptionSync(term.uppercase(java.util.Locale.ROOT))
        }

    override suspend fun getFirstByDescription(description: String): Transaction? = withContext(Dispatchers.IO) {
        transactionDAO.getFirstByDescriptionSync(description)?.let { TransactionMapper.toDomain(it) }
    }

    override suspend fun getFirstByDescriptionHavingAccount(description: String, accountTerm: String): Transaction? =
        withContext(Dispatchers.IO) {
            transactionDAO.getFirstByDescriptionHavingAccountSync(description, accountTerm)
                ?.let { TransactionMapper.toDomain(it) }
        }

    // ========================================
    // Mutation Operations (Domain Models)
    // ========================================

    override suspend fun insertTransaction(transaction: Transaction, profileId: Long): Transaction =
        withContext(Dispatchers.IO) {
            val entity = TransactionMapper.toEntity(transaction, profileId)
            appendTransactionInternal(entity)
            // Return the updated domain model with generated ID
            TransactionMapper.toDomain(entity)
        }

    override suspend fun storeTransaction(transaction: Transaction, profileId: Long) {
        withContext(Dispatchers.IO) {
            val entity = TransactionMapper.toEntity(transaction, profileId)
            storeTransactionInternal(entity)
        }
    }

    // ========================================
    // Mutation Operations (DB Entities - Legacy)
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
                    acc.parentName = AccountNameUtils.extractParentName(accName)
                    acc.level = AccountNameUtils.determineLevel(acc.name)
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

                accName = AccountNameUtils.extractParentName(accName)
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

    override suspend fun deleteTransaction(transaction: DbTransaction) {
        withContext(Dispatchers.IO) {
            transactionDAO.deleteSync(transaction)
        }
    }

    override suspend fun deleteTransactions(transactions: List<DbTransaction>) {
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

            logcat { "Purging old transactions" }
            var removed = transactionDAO.purgeOldTransactionsSync(profileId, generation)
            logcat { "Purged $removed transactions" }

            removed = transactionDAO.purgeOldTransactionAccountsSync(profileId, generation)
            logcat { "Purged $removed transaction accounts" }
        }
    }

    override suspend fun storeTransactionsAsDomain(transactions: List<Transaction>, profileId: Long) {
        withContext(Dispatchers.IO) {
            val generation = transactionDAO.getGenerationSync(profileId) + 1

            for (domainTransaction in transactions) {
                val entity = TransactionMapper.toEntity(domainTransaction, profileId)
                entity.transaction.generation = generation
                storeTransactionInternal(entity)
            }

            logcat { "Purging old transactions" }
            var removed = transactionDAO.purgeOldTransactionsSync(profileId, generation)
            logcat { "Purged $removed transactions" }

            removed = transactionDAO.purgeOldTransactionAccountsSync(profileId, generation)
            logcat { "Purged $removed transaction accounts" }
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
