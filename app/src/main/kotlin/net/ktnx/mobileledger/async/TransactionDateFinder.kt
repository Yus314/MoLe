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

import java.util.Collections
import java.util.Locale
import net.ktnx.mobileledger.model.TransactionListItem
import net.ktnx.mobileledger.ui.MainModel
import net.ktnx.mobileledger.utils.Logger
import net.ktnx.mobileledger.utils.SimpleDate

class TransactionDateFinder(
    private val model: MainModel,
    private val date: SimpleDate
) : Thread() {

    override fun run() {
        Logger.debug(
            "go-to-date",
            String.format(Locale.US, "Looking for date %04d-%02d-%02d", date.year, date.month, date.day)
        )
        val transactions = requireNotNull(model.getDisplayedTransactions().value)
        val transactionCount = transactions.size
        Logger.debug(
            "go-to-date",
            String.format(Locale.US, "List contains %d transactions", transactionCount)
        )

        val target = TransactionListItem(date, true)
        var found = Collections.binarySearch(transactions, target, TransactionListItemComparator())
        if (found < 0) {
            found = -1 - found
        }

        model.foundTransactionItemIndex.postValue(found)
    }

    class TransactionListItemComparator : Comparator<TransactionListItem> {
        override fun compare(a: TransactionListItem, b: TransactionListItem): Int {
            val aType = a.type
            if (aType == TransactionListItem.Type.HEADER) return +1
            val bType = b.type
            if (bType == TransactionListItem.Type.HEADER) return -1
            val aDate = a.date
            val bDate = b.date
            val res = aDate.compareTo(bDate)
            if (res != 0) return -res // transactions are reverse sorted by date

            return when {
                aType == TransactionListItem.Type.DELIMITER -> {
                    if (bType == TransactionListItem.Type.DELIMITER) 0 else -1
                }
                bType == TransactionListItem.Type.DELIMITER -> +1
                else -> 0
            }
        }
    }
}
