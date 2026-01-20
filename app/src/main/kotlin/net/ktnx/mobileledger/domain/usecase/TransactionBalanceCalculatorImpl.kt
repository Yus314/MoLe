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

package net.ktnx.mobileledger.domain.usecase

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import net.ktnx.mobileledger.domain.model.BalanceConstants
import net.ktnx.mobileledger.domain.model.Transaction
import net.ktnx.mobileledger.domain.model.TransactionLine
import net.ktnx.mobileledger.utils.SimpleDate

/**
 * Implementation of [TransactionBalanceCalculator].
 *
 * This class bridges UI form state (AccountEntry) to domain model (Transaction).
 * Core balance logic is delegated to the Transaction domain model.
 *
 * Responsibilities:
 * - Convert UI entries to TransactionLine
 * - Delegate balance calculation to Transaction.withAutoBalance()
 * - Calculate UI-specific amount hints (requires formatNumber callback)
 */
@Singleton
class TransactionBalanceCalculatorImpl @Inject constructor() : TransactionBalanceCalculator {

    override fun calculateBalance(
        entries: List<TransactionBalanceCalculator.AccountEntry>
    ): TransactionBalanceCalculator.BalanceResult {
        // Convert UI entries to TransactionLine
        val lines = entries
            .filter { it.accountName.isNotBlank() }
            .map { entry ->
                TransactionLine(
                    id = null,
                    accountName = entry.accountName.trim(),
                    amount = if (entry.isAmountSet && entry.isAmountValid) entry.amount else null,
                    currency = entry.currency,
                    comment = entry.comment?.ifBlank { null }
                )
            }

        // Create Transaction and delegate auto-balance to domain model
        val transaction = Transaction(
            date = SimpleDate.today(),
            description = "",
            lines = lines
        )
        val autoBalanced = transaction.withAutoBalance()

        return TransactionBalanceCalculator.BalanceResult(
            lines = autoBalanced.lines,
            balancePerCurrency = autoBalanced.balancePerCurrency,
            isBalanced = autoBalanced.isBalanced
        )
    }

    override fun calculateAmountHints(
        entries: List<TransactionBalanceCalculator.AccountEntry>,
        formatNumber: (Float) -> String
    ): List<TransactionBalanceCalculator.AmountHint> {
        // Group entries by currency with their indices
        val currencyGroups = entries.withIndex().groupBy { it.value.currency }

        return entries.mapIndexed { index, entry ->
            val hint = if (!entry.isAmountSet) {
                // Calculate balance for this currency group
                val currencyEntries = currencyGroups[entry.currency] ?: emptyList()
                val entriesWithAmount = currencyEntries.filter {
                    it.value.isAmountSet && it.value.isAmountValid
                }

                val balance = entriesWithAmount.sumOf {
                    it.value.amount?.toDouble() ?: 0.0
                }

                if (abs(balance) < BalanceConstants.BALANCE_EPSILON) {
                    "0"
                } else {
                    formatNumber(-balance.toFloat())
                }
            } else {
                null
            }

            TransactionBalanceCalculator.AmountHint(index, hint)
        }
    }

    override fun isBalanceable(entries: List<TransactionBalanceCalculator.AccountEntry>): Boolean {
        // Convert UI entries to TransactionLine and delegate to domain model
        val lines = entries
            .filter { it.accountName.isNotBlank() }
            .map { entry ->
                TransactionLine(
                    id = null,
                    accountName = entry.accountName.trim(),
                    amount = if (entry.isAmountSet && entry.isAmountValid) entry.amount else null,
                    currency = entry.currency,
                    comment = entry.comment?.ifBlank { null }
                )
            }

        if (lines.isEmpty()) return true

        val transaction = Transaction(
            date = SimpleDate.today(),
            description = "",
            lines = lines
        )

        return transaction.isBalanced
    }
}
