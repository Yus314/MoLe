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

package net.ktnx.mobileledger.core.testing.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import net.ktnx.mobileledger.core.domain.model.Transaction
import net.ktnx.mobileledger.core.domain.repository.TransactionRepository

/**
 * Fake TransactionRepository for ViewModel testing.
 *
 * Uses domain models (Transaction) for query operations.
 */
class FakeTransactionRepository : TransactionRepository {
    // Internal storage uses domain models
    private val domainTransactions = mutableMapOf<Long, Transaction>()
    private var nextId = 1L

    // Track profile associations
    private val profileMap = mutableMapOf<Long, Long>() // transactionId -> profileId

    /**
     * Add a domain model transaction for testing.
     */
    fun addTransaction(profileId: Long, transaction: Transaction) {
        val id = transaction.id ?: nextId++
        val tx = if (transaction.id == null) transaction.copy(id = id) else transaction
        domainTransactions[id] = tx
        profileMap[id] = profileId
    }

    fun getTransactionsForProfile(profileId: Long): List<Transaction> =
        domainTransactions.values.filter { profileMap[it.id] == profileId }

    // Flow methods (observe prefix)
    override fun observeAllTransactions(profileId: Long): Flow<List<Transaction>> =
        MutableStateFlow(domainTransactions.values.filter { profileMap[it.id] == profileId }.toList())

    override fun observeTransactionsFiltered(profileId: Long, accountName: String?): Flow<List<Transaction>> =
        MutableStateFlow(
            domainTransactions.values.filter { tx ->
                profileMap[tx.id] == profileId &&
                    (accountName == null || tx.lines.any { it.accountName.contains(accountName, true) })
            }.toList()
        )

    override fun observeTransactionById(transactionId: Long): Flow<Transaction?> =
        MutableStateFlow(domainTransactions[transactionId])

    // Suspend methods (no suffix)
    override suspend fun getTransactionById(transactionId: Long): Result<Transaction?> =
        Result.success(domainTransactions[transactionId])

    override suspend fun searchByDescription(term: String): Result<List<String>> = Result.success(
        domainTransactions.values
            .filter { it.description.contains(term, true) }
            .mapNotNull { it.description }
            .distinct()
    )

    override suspend fun getFirstByDescription(description: String): Result<Transaction?> =
        Result.success(domainTransactions.values.find { it.description == description })

    override suspend fun getFirstByDescriptionHavingAccount(
        description: String,
        accountTerm: String
    ): Result<Transaction?> = Result.success(
        domainTransactions.values.find { tx ->
            tx.description == description &&
                tx.lines.any { it.accountName.contains(accountTerm, true) }
        }
    )

    // Domain model mutation methods
    override suspend fun insertTransaction(transaction: Transaction, profileId: Long): Result<Transaction> {
        val id = transaction.id ?: nextId++
        val tx = transaction.copy(id = id)
        domainTransactions[id] = tx
        profileMap[id] = profileId
        return Result.success(tx)
    }

    override suspend fun storeTransaction(transaction: Transaction, profileId: Long): Result<Unit> {
        insertTransaction(transaction, profileId)
        return Result.success(Unit)
    }

    override suspend fun deleteTransactionById(transactionId: Long): Result<Int> {
        val existed = domainTransactions.containsKey(transactionId)
        domainTransactions.remove(transactionId)
        profileMap.remove(transactionId)
        return Result.success(if (existed) 1 else 0)
    }

    override suspend fun deleteTransactionsByIds(transactionIds: List<Long>): Result<Int> {
        var count = 0
        transactionIds.forEach { id ->
            if (domainTransactions.containsKey(id)) {
                count++
            }
            domainTransactions.remove(id)
            profileMap.remove(id)
        }
        return Result.success(count)
    }

    override suspend fun storeTransactionsAsDomain(transactions: List<Transaction>, profileId: Long): Result<Unit> {
        transactions.forEach { tx ->
            val id = tx.id ?: nextId++
            val txWithId = if (tx.id == null) tx.copy(id = id) else tx
            domainTransactions[id] = txWithId
            profileMap[id] = profileId
        }
        return Result.success(Unit)
    }

    override suspend fun deleteAllForProfile(profileId: Long): Result<Int> {
        val toRemove = domainTransactions.values.filter { profileMap[it.id] == profileId }
        toRemove.forEach {
            domainTransactions.remove(it.id)
            profileMap.remove(it.id)
        }
        return Result.success(toRemove.size)
    }

    override suspend fun getMaxLedgerId(profileId: Long): Result<Long?> = Result.success(
        domainTransactions.values
            .filter { profileMap[it.id] == profileId }
            .maxOfOrNull { it.ledgerId }
    )
}
