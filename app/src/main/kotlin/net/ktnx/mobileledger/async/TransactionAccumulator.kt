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
import kotlinx.collections.immutable.toImmutableList
import net.ktnx.mobileledger.domain.model.Transaction
import net.ktnx.mobileledger.service.CurrencyFormatter
import net.ktnx.mobileledger.ui.main.TransactionAccountDisplayItem
import net.ktnx.mobileledger.ui.main.TransactionListDisplayItem
import net.ktnx.mobileledger.utils.Misc
import net.ktnx.mobileledger.utils.SimpleDate

class TransactionAccumulator(
    private val boldAccountName: String?,
    private val accumulateAccount: String?,
    private val currencyFormatter: CurrencyFormatter
) {
    private val list = ArrayList<TransactionListDisplayItem>()
    private val runningTotal = HashMap<String, BigDecimal>()
    private var earliestDate: SimpleDate? = null
    private var latestDate: SimpleDate? = null
    private var lastDate: SimpleDate? = null
    private var transactionCount = 0

    init {
        list.add(TransactionListDisplayItem.Header)
    }

    fun put(transaction: Transaction) {
        put(transaction, transaction.date)
    }

    fun put(transaction: Transaction, date: SimpleDate) {
        transactionCount++

        // first item
        if (earliestDate == null) {
            earliestDate = date
        }
        latestDate = date

        lastDate?.let { last ->
            if (date != last) {
                val showMonth = date.month != last.month || date.year != last.year
                list.add(TransactionListDisplayItem.DateDelimiter(last, showMonth))
            }
        }

        var currentTotal: String? = null
        if (accumulateAccount != null) {
            for (line in transaction.lines) {
                if (line.accountName == accumulateAccount ||
                    isParentAccount(accumulateAccount, line.accountName)
                ) {
                    val currencyKey = line.currency
                    var amt = runningTotal[currencyKey] ?: BigDecimal.ZERO
                    val amount = line.amount ?: 0f
                    var newAmount = BigDecimal.valueOf(amount.toDouble())
                    newAmount = newAmount.setScale(2, RoundingMode.HALF_EVEN)
                    amt = amt.add(newAmount)
                    runningTotal[currencyKey] = amt
                }
            }

            currentTotal = summarizeRunningTotal(runningTotal)
        }
        list.add(
            TransactionListDisplayItem.Transaction(
                id = transaction.ledgerId,
                date = date,
                description = transaction.description,
                comment = transaction.comment,
                accounts = transaction.lines.map { line ->
                    TransactionAccountDisplayItem(
                        accountName = line.accountName,
                        amount = line.amount ?: 0f,
                        currency = line.currency,
                        comment = line.comment,
                        amountStyle = null
                    )
                }.toImmutableList(),
                boldAccountName = boldAccountName,
                runningTotal = currentTotal
            )
        )

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
     * Check if parent is a parent account of child.
     * For hledger, this means child starts with "parent:".
     */
    private fun isParentAccount(parent: String, child: String): Boolean = child.startsWith("$parent:")

    /**
     * Get the accumulated list of transaction items.
     * This method finalizes the list by adding the last date delimiter if needed.
     */
    fun getItems(): List<TransactionListDisplayItem> {
        lastDate?.let { last ->
            val today = SimpleDate.today()
            if (last != today) {
                val showMonth = today.month != last.month || today.year != last.year
                // Only add if not already present
                val hasDelimiter = list.any { item ->
                    item is TransactionListDisplayItem.DateDelimiter && item.date == last
                }
                if (!hasDelimiter) {
                    list.add(TransactionListDisplayItem.DateDelimiter(last, showMonth))
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
