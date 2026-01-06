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

package net.ktnx.mobileledger.model

import net.ktnx.mobileledger.utils.SimpleDate

class TransactionListItem {
    val type: Type
    private var _date: SimpleDate? = null
    val isMonthShown: Boolean
    private var transaction: LedgerTransaction? = null
    val boldAccountName: String?
    val runningTotal: String?

    constructor(date: SimpleDate, monthShown: Boolean) {
        this.type = Type.DELIMITER
        this._date = date
        this.isMonthShown = monthShown
        this.boldAccountName = null
        this.runningTotal = null
    }

    constructor(transaction: LedgerTransaction, boldAccountName: String?, runningTotal: String?) {
        this.type = Type.TRANSACTION
        this.transaction = transaction
        this.boldAccountName = boldAccountName
        this.runningTotal = runningTotal
        this.isMonthShown = false
    }

    constructor() {
        this.type = Type.HEADER
        this.isMonthShown = false
        this.boldAccountName = null
        this.runningTotal = null
    }

    val date: SimpleDate
        get() {
            _date?.let { return it }
            check(type == Type.TRANSACTION) { "Only transaction items have a date" }
            return checkNotNull(transaction) { "Transaction is null" }.requireDate()
        }

    fun getTransaction(): LedgerTransaction {
        check(type == Type.TRANSACTION) {
            "Item type is not ${Type.TRANSACTION}, but $type"
        }
        return checkNotNull(transaction) { "Transaction is null" }
    }

    enum class Type {
        TRANSACTION,
        DELIMITER,
        HEADER;

        companion object {
            @JvmStatic
            fun valueOf(i: Int): Type {
                return when (i) {
                    TRANSACTION.ordinal -> TRANSACTION
                    DELIMITER.ordinal -> DELIMITER
                    HEADER.ordinal -> HEADER
                    else -> throw IllegalStateException("Unexpected value: $i")
                }
            }
        }
    }
}
