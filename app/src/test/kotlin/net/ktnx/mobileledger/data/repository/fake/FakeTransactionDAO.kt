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

package net.ktnx.mobileledger.data.repository.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import net.ktnx.mobileledger.dao.TransactionDAO
import net.ktnx.mobileledger.db.Transaction
import net.ktnx.mobileledger.db.TransactionWithAccounts

/**
 * Fake implementation of TransactionDAO for unit testing.
 *
 * This class provides an in-memory implementation that allows testing
 * TransactionRepository without a real database.
 *
 * Key features:
 * - In-memory storage with auto-incrementing IDs
 * - LiveData that emits on changes
 * - Thread-safe operations
 */
class FakeTransactionDAO : TransactionDAO() {

    private val transactions = mutableMapOf<Long, Transaction>()
    private val transactionsWithAccounts = mutableMapOf<Long, TransactionWithAccounts>()
    private var nextId = 1L
    private var currentGeneration = mutableMapOf<Long, Long>() // profileId -> generation

    // StateFlow for observing changes
    private val allTransactionsFlow = mutableMapOf<Long, MutableStateFlow<List<TransactionWithAccounts>>>()
    private val filteredTransactionsFlow =
        mutableMapOf<Pair<Long, String?>, MutableStateFlow<List<TransactionWithAccounts>>>()
    private val transactionByIdFlow = mutableMapOf<Long, MutableStateFlow<TransactionWithAccounts?>>()

    // Callbacks for notifying about changes (useful for Flow conversion)
    private val changeListeners = mutableListOf<() -> Unit>()

    fun addChangeListener(listener: () -> Unit) {
        changeListeners.add(listener)
    }

    fun removeChangeListener(listener: () -> Unit) {
        changeListeners.remove(listener)
    }

    private fun notifyChange() {
        updateAllFlows()
        changeListeners.forEach { it() }
    }

    private fun updateAllFlows() {
        // Update per-profile Flow
        allTransactionsFlow.forEach { (profileId, flow) ->
            flow.value = getTransactionsForProfile(profileId)
        }
        // Update filtered Flow
        filteredTransactionsFlow.forEach { (key, flow) ->
            val (profileId, accountName) = key
            flow.value = getFilteredTransactions(profileId, accountName)
        }
        // Update individual transaction Flow
        transactionByIdFlow.forEach { (id, flow) ->
            flow.value = transactionsWithAccounts[id]
        }
    }

    private fun getTransactionsForProfile(profileId: Long): List<TransactionWithAccounts> =
        transactionsWithAccounts.values
            .filter { it.transaction.profileId == profileId }
            .sortedWith(
                compareBy(
                    { it.transaction.year },
                    { it.transaction.month },
                    { it.transaction.day },
                    { it.transaction.ledgerId }
                )
            )

    private fun getFilteredTransactions(profileId: Long, accountName: String?): List<TransactionWithAccounts> =
        transactionsWithAccounts.values
            .filter { twa ->
                twa.transaction.profileId == profileId &&
                    (
                        accountName == null || twa.accounts.any {
                            it.accountName.contains(accountName, ignoreCase = true) && it.amount != 0f
                        }
                        )
            }
            .sortedWith(
                compareBy(
                    { it.transaction.year },
                    { it.transaction.month },
                    { it.transaction.day },
                    { it.transaction.ledgerId }
                )
            )

    // ========================================
    // BaseDAO overrides
    // ========================================

    override fun insertSync(item: Transaction): Long {
        val id = nextId++
        item.id = id
        transactions[id] = item
        notifyChange()
        return id
    }

    override fun updateSync(item: Transaction) {
        if (transactions.containsKey(item.id)) {
            transactions[item.id] = item
            // Update the corresponding TransactionWithAccounts
            transactionsWithAccounts[item.id]?.let { twa ->
                twa.transaction.copyDataFrom(item)
            }
            notifyChange()
        }
    }

    override fun deleteSync(item: Transaction) {
        transactions.remove(item.id)
        transactionsWithAccounts.remove(item.id)
        notifyChange()
    }

    override fun deleteSync(vararg items: Transaction) {
        items.forEach { deleteSync(it) }
    }

    override fun deleteSync(items: List<Transaction>) {
        items.forEach { deleteSync(it) }
    }

    override fun deleteAllSync() {
        transactions.clear()
        transactionsWithAccounts.clear()
        notifyChange()
    }

    // ========================================
    // Query implementations
    // ========================================

    override fun getById(id: Long): Flow<Transaction> {
        val flow = MutableStateFlow(transactions[id])
        return flow.map { it ?: throw NoSuchElementException("Transaction not found: $id") }
    }

    override fun getByIdWithAccounts(transactionId: Long): Flow<TransactionWithAccounts> =
        transactionByIdFlow.getOrPut(transactionId) {
            MutableStateFlow(transactionsWithAccounts[transactionId])
        }.map { it ?: throw NoSuchElementException("Transaction not found: $transactionId") }

    override fun getByIdWithAccountsSync(transactionId: Long): TransactionWithAccounts? =
        transactionsWithAccounts[transactionId]

    override fun lookupDescriptionSync(term: String): List<DescriptionContainer> {
        val termUpper = term.uppercase()
        return transactions.values
            .filter { it.descriptionUpper.contains(termUpper) }
            .distinctBy { it.description }
            .map { tr ->
                DescriptionContainer().apply {
                    description = tr.description
                    ordering = when {
                        tr.descriptionUpper.startsWith(termUpper) -> 1
                        tr.descriptionUpper.contains(":$termUpper") -> 2
                        tr.descriptionUpper.contains(" $termUpper") -> 3
                        else -> 9
                    }
                }
            }
            .sortedWith(compareBy({ it.ordering }, { it.description?.uppercase() }))
    }

    override fun getFirstByDescriptionSync(description: String): TransactionWithAccounts? =
        transactionsWithAccounts.values
            .filter { it.transaction.description == description }
            .maxByOrNull {
                it.transaction.year * 10000 + it.transaction.month * 100 + it.transaction.day
            }

    override fun getFirstByDescriptionHavingAccountSync(
        description: String,
        accountTerm: String
    ): TransactionWithAccounts? = transactionsWithAccounts.values
        .filter { twa ->
            twa.transaction.description == description &&
                twa.accounts.any { it.accountName.contains(accountTerm, ignoreCase = true) }
        }
        .maxByOrNull {
            it.transaction.year * 10000 + it.transaction.month * 100 + it.transaction.day
        }

    override fun getAllForProfileUnorderedSync(profileId: Long): List<Transaction> = transactions.values.filter {
        it.profileId == profileId
    }

    override fun getGenerationPOJOSync(profileId: Long): TransactionGenerationContainer? {
        val generation = currentGeneration[profileId] ?: return null
        return TransactionGenerationContainer(generation)
    }

    override fun getAllWithAccounts(profileId: Long): Flow<List<TransactionWithAccounts>> =
        allTransactionsFlow.getOrPut(profileId) {
            MutableStateFlow(getTransactionsForProfile(profileId))
        }

    override fun getAllWithAccountsFiltered(
        profileId: Long,
        accountName: String?
    ): Flow<List<TransactionWithAccounts>> {
        val key = profileId to accountName
        return filteredTransactionsFlow.getOrPut(key) {
            MutableStateFlow(getFilteredTransactions(profileId, accountName))
        }
    }

    override fun getAllWithAccountsSync(profileId: Long): List<TransactionWithAccounts> =
        getTransactionsForProfile(profileId)

    override fun getAllWithAccountsFilteredSync(profileId: Long, accountName: String?): List<TransactionWithAccounts> =
        getFilteredTransactions(profileId, accountName)

    override fun purgeOldTransactionsSync(profileId: Long, currentGeneration: Long): Int {
        val toRemove = transactions.values
            .filter { it.profileId == profileId && it.generation != currentGeneration }
        toRemove.forEach {
            transactions.remove(it.id)
            transactionsWithAccounts.remove(it.id)
        }
        notifyChange()
        return toRemove.size
    }

    override fun purgeOldTransactionAccountsSync(profileId: Long, currentGeneration: Long): Int {
        // In FakeDAO, accounts are stored with transactions, so this is handled in purgeOldTransactionsSync
        return 0
    }

    override fun deleteAllSync(profileId: Long): Int {
        val toRemove = transactions.values.filter { it.profileId == profileId }
        toRemove.forEach {
            transactions.remove(it.id)
            transactionsWithAccounts.remove(it.id)
        }
        notifyChange()
        return toRemove.size
    }

    override fun deleteByIdSync(transactionId: Long): Int {
        val existed = transactions.containsKey(transactionId)
        transactions.remove(transactionId)
        transactionsWithAccounts.remove(transactionId)
        if (existed) notifyChange()
        return if (existed) 1 else 0
    }

    override fun deleteByIdsSync(transactionIds: List<Long>): Int {
        var count = 0
        transactionIds.forEach { id ->
            if (transactions.containsKey(id)) {
                count++
                transactions.remove(id)
                transactionsWithAccounts.remove(id)
            }
        }
        if (count > 0) notifyChange()
        return count
    }

    override fun getByLedgerId(profileId: Long, ledgerId: Long): Transaction? = transactions.values.find {
        it.profileId == profileId && it.ledgerId == ledgerId
    }

    override fun updateGeneration(transactionId: Long, newGeneration: Long): Int {
        val transaction = transactions[transactionId] ?: return 0
        transaction.generation = newGeneration
        transactionsWithAccounts[transactionId]?.transaction?.generation = newGeneration
        return 1
    }

    override fun updateAccountsGeneration(transactionId: Long, newGeneration: Long): Int {
        val twa = transactionsWithAccounts[transactionId] ?: return 0
        twa.accounts.forEach { it.generation = newGeneration }
        return twa.accounts.size
    }

    override fun getMaxLedgerIdPOJOSync(profileId: Long): LedgerIdContainer? {
        val maxLedgerId = transactions.values
            .filter { it.profileId == profileId }
            .maxOfOrNull { it.ledgerId }
            ?: return null
        return LedgerIdContainer(maxLedgerId)
    }

    // ========================================
    // Test helper methods
    // ========================================

    /**
     * Store a complete TransactionWithAccounts for testing.
     */
    fun storeTestTransaction(twa: TransactionWithAccounts) {
        if (twa.transaction.id == 0L) {
            twa.transaction.id = nextId++
        }
        transactions[twa.transaction.id] = twa.transaction
        transactionsWithAccounts[twa.transaction.id] = twa

        // Update generation tracking
        val profileId = twa.transaction.profileId
        val generation = twa.transaction.generation
        if (generation > (currentGeneration[profileId] ?: 0L)) {
            currentGeneration[profileId] = generation
        }

        notifyChange()
    }

    /**
     * Get all stored transactions for inspection.
     */
    fun getAllStoredTransactions(): List<TransactionWithAccounts> = transactionsWithAccounts.values.toList()

    /**
     * Clear all data for testing.
     */
    fun clearAll() {
        transactions.clear()
        transactionsWithAccounts.clear()
        currentGeneration.clear()
        nextId = 1L
        notifyChange()
    }

    /**
     * Get the current generation for a profile.
     */
    fun getCurrentGeneration(profileId: Long): Long = currentGeneration[profileId] ?: 0L
}
