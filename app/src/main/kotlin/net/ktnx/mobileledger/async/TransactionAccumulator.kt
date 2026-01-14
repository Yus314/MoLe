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

import java.math.BigDecimal
import java.math.RoundingMode
import net.ktnx.mobileledger.model.LedgerAccount
import net.ktnx.mobileledger.model.LedgerTransaction
import net.ktnx.mobileledger.model.TransactionListItem
import net.ktnx.mobileledger.service.CurrencyFormatter
import net.ktnx.mobileledger.utils.Misc
import net.ktnx.mobileledger.utils.SimpleDate

class TransactionAccumulator(
    private val boldAccountName: String?,
    private val accumulateAccount: String?,
    private val currencyFormatter: CurrencyFormatter
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
                list.add(TransactionListItem(last, showMonth))
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
        list.add(TransactionListItem(transaction, boldAccountName, currentTotal))

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
            b.append(currencyFormatter.formatNumber(value?.toFloat() ?: 0f))
        }
        return b.toString()
    }

    /**
     * Get the accumulated list of transaction items.
     * This method finalizes the list by adding the last date delimiter if needed.
     */
    fun getItems(): List<TransactionListItem> {
        lastDate?.let { last ->
            val today = SimpleDate.today()
            if (last != today) {
                val showMonth = today.month != last.month || today.year != last.year
                // Only add if not already present
                val hasDelimiter = list.any { item ->
                    item.type == TransactionListItem.Type.DELIMITER &&
                        item.date == last
                }
                if (!hasDelimiter) {
                    list.add(TransactionListItem(last, showMonth))
                }
            }
        }
        // Return header + reversed items (to show newest first)
        if (list.size <= 1) return list.toList()
        val header = list[0]
        val items = list.subList(1, list.size).reversed()
        return listOf(header) + items
    }

    /**
     * Get the earliest transaction date.
     */
    fun getEarliestDate(): SimpleDate? = earliestDate

    /**
     * Get the latest transaction date.
     */
    fun getLatestDate(): SimpleDate? = latestDate

    /**
     * Get the total transaction count.
     */
    fun getTransactionCount(): Int = transactionCount
}
