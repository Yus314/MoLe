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
import kotlinx.coroutines.flow.flowOf
import net.ktnx.mobileledger.core.database.dao.TransactionAccountDAO
import net.ktnx.mobileledger.core.database.entity.TransactionAccount

/**
 * Fake implementation of TransactionAccountDAO for unit testing.
 *
 * This class provides an in-memory implementation that allows testing
 * TransactionRepository without a real database.
 */
class FakeTransactionAccountDAO : TransactionAccountDAO() {

    private val accounts = mutableMapOf<Long, TransactionAccount>()
    private var nextId = 1L

    // ========================================
    // BaseDAO overrides
    // ========================================

    override fun insertSync(item: TransactionAccount): Long {
        val id = nextId++
        item.id = id
        accounts[id] = item
        return id
    }

    override fun updateSync(item: TransactionAccount) {
        if (accounts.containsKey(item.id)) {
            accounts[item.id] = item
        }
    }

    override fun deleteSync(item: TransactionAccount) {
        accounts.remove(item.id)
    }

    override fun deleteSync(items: List<TransactionAccount>) {
        items.forEach { deleteSync(it) }
    }

    override fun deleteAllSync() {
        accounts.clear()
    }

    // ========================================
    // Query implementations
    // ========================================

    override fun getById(id: Long): Flow<TransactionAccount> {
        val account = accounts[id]
        return flowOf(account ?: throw NoSuchElementException("TransactionAccount not found: $id"))
    }

    override fun getByOrderNoSync(transactionId: Long, orderNo: Int): TransactionAccount? = accounts.values.find {
        it.transactionId == transactionId && it.orderNo == orderNo
    }

    // ========================================
    // Test helper methods
    // ========================================

    /**
     * Get all accounts for a transaction.
     */
    fun getAccountsForTransaction(transactionId: Long): List<TransactionAccount> =
        accounts.values.filter { it.transactionId == transactionId }

    /**
     * Get all stored accounts for inspection.
     */
    fun getAllStoredAccounts(): List<TransactionAccount> = accounts.values.toList()

    /**
     * Clear all data for testing.
     */
    fun clearAll() {
        accounts.clear()
        nextId = 1L
    }
}
