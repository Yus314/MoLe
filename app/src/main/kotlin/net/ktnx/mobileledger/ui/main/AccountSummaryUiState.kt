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

package net.ktnx.mobileledger.ui.main

/**
 * UI state for the account summary tab.
 */
data class AccountSummaryUiState(
    val accounts: List<AccountSummaryListItem> = emptyList(),
    val showZeroBalanceAccounts: Boolean = true,
    val isLoading: Boolean = false,
    val headerText: String = "----",
    val error: String? = null
)

/**
 * Sealed class representing items in the account summary list.
 */
sealed class AccountSummaryListItem {
    /**
     * Header item showing the last update information.
     */
    data class Header(val text: String) : AccountSummaryListItem()

    /**
     * Account item representing a single account with its balance.
     */
    data class Account(
        val id: Long,
        val name: String,
        val shortName: String,
        val level: Int,
        val amounts: List<AccountAmount>,
        val parentName: String? = null,
        val hasSubAccounts: Boolean = false,
        val isExpanded: Boolean = true,
        val amountsExpanded: Boolean = false
    ) : AccountSummaryListItem() {
        /**
         * Returns true if all amounts are zero.
         */
        fun allAmountsAreZero(): Boolean = amounts.all { it.amount == 0f }
    }

    companion object {
        /**
         * Remove zero balance accounts from the list, keeping parent accounts
         * if they have non-zero children.
         */
        fun removeZeroAccounts(list: List<AccountSummaryListItem>): List<AccountSummaryListItem> {
            var removed = true
            var currentList = list.toMutableList()

            while (removed) {
                var last: AccountSummaryListItem? = null
                removed = false
                val newList = mutableListOf<AccountSummaryListItem>()

                for (item in currentList) {
                    if (last == null) {
                        last = item
                        continue
                    }

                    val isHeader = last is Header
                    val hasNonZeroBalance = last is Account && !last.allAmountsAreZero()
                    val isParentOfCurrent = last is Account &&
                        item is Account &&
                        isParentOf(last.name, item.name)
                    if (isHeader || hasNonZeroBalance || isParentOfCurrent) {
                        newList.add(last)
                    } else {
                        removed = true
                    }

                    last = item
                }

                if (last != null) {
                    if (last is Header ||
                        (last is Account && !last.allAmountsAreZero())
                    ) {
                        newList.add(last)
                    } else {
                        removed = true
                    }
                }

                currentList = newList
            }

            return currentList
        }

        private fun isParentOf(parentName: String, childName: String): Boolean = childName.startsWith("$parentName:")
    }
}

/**
 * Represents an amount with its currency.
 */
data class AccountAmount(val amount: Float, val currency: String, val formattedAmount: String)

/**
 * Events from the account summary tab.
 */
sealed class AccountSummaryEvent {
    data object ToggleZeroBalanceAccounts : AccountSummaryEvent()
    data class ToggleAccountExpanded(val accountId: Long) : AccountSummaryEvent()
    data class ToggleAmountsExpanded(val accountId: Long) : AccountSummaryEvent()
    data class ShowAccountTransactions(val accountName: String) : AccountSummaryEvent()
}

/**
 * Side effects from AccountSummaryViewModel.
 * Effects are one-shot actions that happen once (navigation, showing dialogs).
 */
sealed class AccountSummaryEffect {
    /**
     * Request to show transactions filtered by account name.
     * MainScreen should handle this by switching to Transactions tab with filter applied.
     */
    data class ShowAccountTransactions(val accountName: String) : AccountSummaryEffect()
}
