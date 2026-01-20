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

package net.ktnx.mobileledger.fake

import net.ktnx.mobileledger.domain.model.Transaction
import net.ktnx.mobileledger.domain.usecase.TransactionListConverter

/**
 * Fake implementation of [TransactionListConverter] for testing.
 *
 * Provides controllable behavior for tests:
 * - Configure custom result via [customResult]
 * - Track method calls via [convertCallCount]
 * - Access last input via [lastTransactions]
 * - Reset state between tests via [reset]
 */
class FakeTransactionListConverter : TransactionListConverter {

    /**
     * Custom result to return. If null, delegates to real implementation.
     */
    var customResult: TransactionListConverter.ConversionResult? = null

    /**
     * Number of times [convert] was called.
     */
    var convertCallCount = 0
        private set

    /**
     * The transactions passed to the last [convert] call.
     */
    var lastTransactions: List<Transaction>? = null
        private set

    /**
     * Real implementation for delegation when customResult is null.
     */
    private val realImpl = net.ktnx.mobileledger.domain.usecase.TransactionListConverterImpl()

    override fun convert(transactions: List<Transaction>): TransactionListConverter.ConversionResult {
        convertCallCount++
        lastTransactions = transactions

        return customResult ?: realImpl.convert(transactions)
    }

    /**
     * Reset all state to initial values.
     */
    fun reset() {
        customResult = null
        convertCallCount = 0
        lastTransactions = null
    }

    /**
     * Set a custom empty result.
     */
    fun setEmptyResult() {
        customResult = TransactionListConverter.ConversionResult(
            items = emptyList(),
            firstDate = null,
            lastDate = null
        )
    }
}
