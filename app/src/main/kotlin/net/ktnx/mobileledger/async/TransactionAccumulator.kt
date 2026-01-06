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

package net.ktnx.mobileledger.async

import net.ktnx.mobileledger.model.Data
import net.ktnx.mobileledger.model.LedgerAccount
import net.ktnx.mobileledger.model.LedgerTransaction
import net.ktnx.mobileledger.model.TransactionListItem
import net.ktnx.mobileledger.ui.MainModel
import net.ktnx.mobileledger.utils.Misc
import net.ktnx.mobileledger.utils.SimpleDate
import java.math.BigDecimal
import java.math.RoundingMode

class TransactionAccumulator(
    private val boldAccountName: String?,
    private val accumulateAccount: String?
) {
    private val list = ArrayList<TransactionListItem>()
    private val runningTotal = HashMap<String, BigDecimal>()
    private var earliestDate: SimpleDate? = null
    private var latestDate: SimpleDate? = null
    private var lastDate: SimpleDate? = null
    private var transactionCount = 0

    init {
        list.add(TransactionListItem()) // head item
    }

    fun put(transaction: LedgerTransaction) {
        put(transaction, transaction.requireDate())
    }

    fun put(transaction: LedgerTransaction, date: SimpleDate) {
        transactionCount++

        // first item
        if (earliestDate == null) {
            earliestDate = date
        }
        latestDate = date

        lastDate?.let { last ->
            if (date != last) {
                val showMonth = date.month != last.month || date.year != last.year
                list.add(1, TransactionListItem(last, showMonth))
            }
        }

        var currentTotal: String? = null
        if (accumulateAccount != null) {
            for (acc in transaction.accounts) {
                if (acc.accountName == accumulateAccount ||
                    LedgerAccount.isParentOf(accumulateAccount, acc.accountName)
                ) {
                    val currencyKey = acc.currency ?: ""
                    var amt = runningTotal[currencyKey] ?: BigDecimal.ZERO
                    var newAmount = BigDecimal.valueOf(acc.amount.toDouble())
                    newAmount = newAmount.setScale(2, RoundingMode.HALF_EVEN)
                    amt = amt.add(newAmount)
                    runningTotal[currencyKey] = amt
                }
            }

            currentTotal = summarizeRunningTotal(runningTotal)
        }
        list.add(1, TransactionListItem(transaction, boldAccountName, currentTotal))

        lastDate = date
    }

    private fun summarizeRunningTotal(runningTotal: HashMap<String, BigDecimal>): String {
        val b = StringBuilder()
        for (currency in runningTotal.keys) {
            if (b.isNotEmpty()) {
                b.append('\n')
            }
            if (Misc.emptyIsNull(currency) != null) {
                b.append(currency).append(' ')
            }
            val value = runningTotal[currency]
            b.append(Data.formatNumber(value?.toFloat() ?: 0f))
        }
        return b.toString()
    }

    fun publishResults(model: MainModel) {
        lastDate?.let { last ->
            val today = SimpleDate.today()
            if (last != today) {
                val showMonth = today.month != last.month || today.year != last.year
                list.add(1, TransactionListItem(last, showMonth))
            }
        }

        model.setDisplayedTransactions(list, transactionCount)
        model.firstTransactionDate = earliestDate
        model.lastTransactionDate = latestDate
    }
}
