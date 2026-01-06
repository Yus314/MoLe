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

package net.ktnx.mobileledger.model

import androidx.lifecycle.LiveData

sealed class AccountListItem private constructor() {
    abstract fun sameContent(other: AccountListItem): Boolean

    val type: Type
        get() = when (this) {
            is Account -> Type.ACCOUNT
            is Header -> Type.HEADER
        }

    fun isAccount(): Boolean = this is Account

    fun toAccount(): Account {
        assert(isAccount())
        return this as Account
    }

    fun isHeader(): Boolean = this is Header

    fun toHeader(): Header {
        assert(isHeader())
        return this as Header
    }

    enum class Type {
        ACCOUNT,
        HEADER
    }

    class Account(val account: LedgerAccount) : AccountListItem() {
        override fun sameContent(other: AccountListItem): Boolean {
            if (other !is Account) return false
            return other.account.hasSubAccounts == account.hasSubAccounts &&
                    other.account.amountsExpanded == account.amountsExpanded &&
                    other.account.isExpanded == account.isExpanded &&
                    other.account.level == account.level &&
                    other.account.getAmountsString() == account.getAmountsString()
        }

        fun allAmountsAreZero(): Boolean = account.allAmountsAreZero()
    }

    class Header(val text: LiveData<String>) : AccountListItem() {
        override fun sameContent(other: AccountListItem): Boolean = true
    }
}
