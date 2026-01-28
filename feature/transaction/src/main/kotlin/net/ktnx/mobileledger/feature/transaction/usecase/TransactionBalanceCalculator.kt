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

package net.ktnx.mobileledger.feature.transaction.usecase

import net.ktnx.mobileledger.core.domain.model.TransactionLine

/**
 * Calculates transaction balance and auto-balance amounts across currencies.
 *
 * This UseCase encapsulates the business rules for:
 * - Grouping accounts by currency
 * - Calculating balance per currency group
 * - Auto-filling empty amounts when exactly one account is missing per currency
 * - Validating balance constraints
 */
interface TransactionBalanceCalculator {

    /**
     * Input data for balance calculation (UI-layer agnostic)
     */
    data class AccountEntry(
        val accountName: String,
        val amount: Float?,
        val currency: String,
        val comment: String?,
        val isAmountSet: Boolean,
        val isAmountValid: Boolean
    )

    /**
     * Result of balance calculation
     */
    data class BalanceResult(
        val lines: List<TransactionLine>,
        val balancePerCurrency: Map<String, Float>,
        val isBalanced: Boolean
    )

    /**
     * Result for amount hint calculation
     */
    data class AmountHint(
        val entryIndex: Int,
        val hint: String?
    )

    /**
     * Calculate transaction lines with auto-balance applied.
     *
     * Business rules:
     * - Skip entries with blank account names
     * - Group entries by currency
     * - For each currency group, if exactly one entry has no amount set,
     *   auto-fill it with the negative of the sum of other amounts
     * - Amounts within 0.005 of zero are considered balanced
     *
     * @param entries List of account entries from the UI
     * @return BalanceResult with constructed lines and balance info
     */
    fun calculateBalance(entries: List<AccountEntry>): BalanceResult

    /**
     * Calculate amount hints for display in the UI.
     *
     * For each entry without an amount set, calculates what amount
     * would balance the transaction for that currency group.
     *
     * @param entries List of account entries
     * @param formatNumber Function to format numbers for display (locale-aware)
     * @return List of hints indexed by entry position
     */
    fun calculateAmountHints(entries: List<AccountEntry>, formatNumber: (Float) -> String): List<AmountHint>

    /**
     * Validate that the transaction can be balanced.
     *
     * A transaction is balanceable if for each currency group:
     * - The sum of amounts is within 0.005 of zero, OR
     * - Exactly one entry with a non-blank account name has no amount set
     *
     * @param entries List of account entries
     * @return true if balanced or can be auto-balanced
     */
    fun isBalanceable(entries: List<AccountEntry>): Boolean
}
