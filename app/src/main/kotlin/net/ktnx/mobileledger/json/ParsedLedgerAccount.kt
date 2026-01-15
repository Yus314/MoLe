/*
 * Copyright © 2021 Damyan Ivanov.
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

package net.ktnx.mobileledger.json

import net.ktnx.mobileledger.async.RetrieveTransactionsTask
import net.ktnx.mobileledger.model.AmountStyle
import net.ktnx.mobileledger.model.LedgerAccount

abstract class ParsedLedgerAccount {
    open var aname: String = ""
    open var anumpostings: Int = 0

    abstract fun getSimpleBalance(): List<SimpleBalance>

    open fun toLedgerAccount(task: RetrieveTransactionsTask, map: HashMap<String, LedgerAccount>): LedgerAccount {
        task.addNumberOfPostings(anumpostings)
        val accName = aname
        val existing = map[accName]
        if (existing != null) {
            throw RuntimeException("Account '$accName' already present")
        }
        val parentName = LedgerAccount.extractParentName(accName)
        val createdParents = ArrayList<LedgerAccount>()
        val parent: LedgerAccount? = if (parentName == null) {
            null
        } else {
            task.ensureAccountExists(parentName, map, createdParents).also {
                it.hasSubAccounts = true
            }
        }
        val acc = LedgerAccount(accName, parent)
        map[accName] = acc

        var lastCurrency: String? = null
        var lastCurrencyAmount = 0f
        var lastAmountStyle: AmountStyle? = null

        for (b in getSimpleBalance()) {
            task.throwIfCancelled()
            val currency = b.commodity
            val amount = b.amount
            val amountStyle = b.amountStyle

            if (currency == lastCurrency) {
                lastCurrencyAmount += amount
                // Keep the first amountStyle found for this currency
            } else {
                if (lastCurrency != null) {
                    acc.addAmount(lastCurrencyAmount, lastCurrency, lastAmountStyle)
                }
                lastCurrency = currency
                lastCurrencyAmount = amount
                lastAmountStyle = amountStyle
            }
        }
        if (lastCurrency != null) {
            acc.addAmount(lastCurrencyAmount, lastCurrency, lastAmountStyle)
        }
        for (p in createdParents) {
            acc.propagateAmountsTo(p)
        }

        return acc
    }

    /**
     * Convert to LedgerAccount without RetrieveTransactionsTask dependency.
     * This method is used by the pure Coroutines implementation of TransactionSyncerImpl.
     *
     * @param map Map of account names to LedgerAccount instances for parent lookup
     * @return The converted LedgerAccount
     */
    open fun toLedgerAccountWithoutTask(map: HashMap<String, LedgerAccount>): LedgerAccount {
        val accName = aname
        val existing = map[accName]
        if (existing != null) {
            throw RuntimeException("Account '$accName' already present")
        }
        val parentName = LedgerAccount.extractParentName(accName)
        val createdParents = ArrayList<LedgerAccount>()
        val parent: LedgerAccount? = if (parentName == null) {
            null
        } else {
            ensureAccountExistsWithoutTask(parentName, map, createdParents).also {
                it.hasSubAccounts = true
            }
        }
        val acc = LedgerAccount(accName, parent)
        map[accName] = acc

        var lastCurrency: String? = null
        var lastCurrencyAmount = 0f
        var lastAmountStyle: AmountStyle? = null

        for (b in getSimpleBalance()) {
            val currency = b.commodity
            val amount = b.amount
            val amountStyle = b.amountStyle

            if (currency == lastCurrency) {
                lastCurrencyAmount += amount
            } else {
                if (lastCurrency != null) {
                    acc.addAmount(lastCurrencyAmount, lastCurrency, lastAmountStyle)
                }
                lastCurrency = currency
                lastCurrencyAmount = amount
                lastAmountStyle = amountStyle
            }
        }
        if (lastCurrency != null) {
            acc.addAmount(lastCurrencyAmount, lastCurrency, lastAmountStyle)
        }
        for (p in createdParents) {
            acc.propagateAmountsTo(p)
        }

        return acc
    }

    private fun ensureAccountExistsWithoutTask(
        accountName: String,
        map: HashMap<String, LedgerAccount>,
        createdAccounts: ArrayList<LedgerAccount>
    ): LedgerAccount {
        map[accountName]?.let { return it }

        val parentName = LedgerAccount.extractParentName(accountName)
        val parentAccount = if (parentName != null) {
            ensureAccountExistsWithoutTask(parentName, map, createdAccounts)
        } else {
            null
        }

        val acc = LedgerAccount(accountName, parentAccount)
        createdAccounts.add(acc)
        map[accountName] = acc
        return acc
    }

    data class SimpleBalance(var commodity: String, var amount: Float, var amountStyle: AmountStyle? = null) {
        constructor(commodity: String, amount: Float) : this(commodity, amount, null)
    }
}
