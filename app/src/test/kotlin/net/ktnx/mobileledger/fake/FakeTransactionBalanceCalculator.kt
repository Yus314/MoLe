/*
 * Copyright © 2024 Damyan Ivanov.
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

import net.ktnx.mobileledger.domain.model.TransactionLine
import net.ktnx.mobileledger.domain.usecase.TransactionBalanceCalculator

/**
 * Fake implementation of [TransactionBalanceCalculator] for testing.
 *
 * Provides controllable behavior for tests:
 * - Configure custom results via [balanceResult], [hints], [isBalanceableResult]
 * - Track method calls via [calculateBalanceCallCount], [calculateHintsCallCount], [lastEntries]
 * - Reset state between tests via [reset]
 */
class FakeTransactionBalanceCalculator : TransactionBalanceCalculator {

    /**
     * Custom balance result to return. If null, a default result is generated.
     */
    var balanceResult: TransactionBalanceCalculator.BalanceResult? = null

    /**
     * Custom hints to return. If empty, default hints are generated.
     */
    var hints: List<TransactionBalanceCalculator.AmountHint> = emptyList()

    /**
     * Result to return from [isBalanceable].
     */
    var isBalanceableResult = true

    /**
     * Number of times [calculateBalance] was called.
     */
    var calculateBalanceCallCount = 0
        private set

    /**
     * Number of times [calculateAmountHints] was called.
     */
    var calculateHintsCallCount = 0
        private set

    /**
     * Number of times [isBalanceable] was called.
     */
    var isBalanceableCallCount = 0
        private set

    /**
     * The entries passed to the last method call.
     */
    var lastEntries: List<TransactionBalanceCalculator.AccountEntry> = emptyList()
        private set

    override fun calculateBalance(
        entries: List<TransactionBalanceCalculator.AccountEntry>
    ): TransactionBalanceCalculator.BalanceResult {
        calculateBalanceCallCount++
        lastEntries = entries

        return balanceResult ?: TransactionBalanceCalculator.BalanceResult(
            lines = entries.filter { it.accountName.isNotBlank() }.map { entry ->
                TransactionLine(
                    id = null,
                    accountName = entry.accountName,
                    amount = entry.amount,
                    currency = entry.currency,
                    comment = entry.comment
                )
            },
            balancePerCurrency = emptyMap(),
            isBalanced = true
        )
    }

    override fun calculateAmountHints(
        entries: List<TransactionBalanceCalculator.AccountEntry>,
        formatNumber: (Float) -> String
    ): List<TransactionBalanceCalculator.AmountHint> {
        calculateHintsCallCount++
        lastEntries = entries

        return hints.ifEmpty {
            entries.mapIndexed { idx, _ ->
                TransactionBalanceCalculator.AmountHint(idx, null)
            }
        }
    }

    override fun isBalanceable(entries: List<TransactionBalanceCalculator.AccountEntry>): Boolean {
        isBalanceableCallCount++
        lastEntries = entries
        return isBalanceableResult
    }

    /**
     * Reset all state to initial values.
     */
    fun reset() {
        balanceResult = null
        hints = emptyList()
        isBalanceableResult = true
        calculateBalanceCallCount = 0
        calculateHintsCallCount = 0
        isBalanceableCallCount = 0
        lastEntries = emptyList()
    }

    /**
     * Configure a custom balance result with the given lines.
     */
    fun setBalanceResult(
        lines: List<TransactionLine>,
        balancePerCurrency: Map<String, Float> = emptyMap(),
        isBalanced: Boolean = true
    ) {
        balanceResult = TransactionBalanceCalculator.BalanceResult(
            lines = lines,
            balancePerCurrency = balancePerCurrency,
            isBalanced = isBalanced
        )
    }

    /**
     * Configure custom hints for specific indices.
     */
    fun setHints(vararg indexedHints: Pair<Int, String?>) {
        hints = indexedHints.map { (index, hint) ->
            TransactionBalanceCalculator.AmountHint(index, hint)
        }
    }
}
