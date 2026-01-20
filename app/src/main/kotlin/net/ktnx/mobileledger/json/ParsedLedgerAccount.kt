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

import net.ktnx.mobileledger.domain.model.Account
import net.ktnx.mobileledger.domain.model.AccountAmount
import net.ktnx.mobileledger.domain.model.AmountStyle

abstract class ParsedLedgerAccount {
    open var aname: String = ""
    open var anumpostings: Int = 0

    abstract fun getSimpleBalance(): List<SimpleBalance>

    /**
     * Convert to domain model Account.
     *
     * Note: This does NOT create parent accounts. Parent account creation
     * should be handled by the caller (e.g., TransactionSyncerImpl).
     */
    open fun toDomain(): Account {
        val level = aname.count { it == ':' }
        val balances = getSimpleBalance()
        return Account(
            id = null,
            name = aname,
            level = level,
            isExpanded = false,
            isVisible = true,
            amounts = aggregateBalances(balances)
        )
    }

    private fun aggregateBalances(balances: List<SimpleBalance>): List<AccountAmount> = balances
        .groupBy { it.commodity }
        .map { (commodity, items) ->
            AccountAmount(
                currency = commodity,
                amount = items.sumOf { it.amount.toDouble() }.toFloat()
            )
        }

    data class SimpleBalance(var commodity: String, var amount: Float, var amountStyle: AmountStyle? = null) {
        constructor(commodity: String, amount: Float) : this(commodity, amount, null)
    }
}
