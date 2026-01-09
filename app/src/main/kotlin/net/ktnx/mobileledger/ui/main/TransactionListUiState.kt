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

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import net.ktnx.mobileledger.model.AmountStyle
import net.ktnx.mobileledger.utils.SimpleDate

/**
 * UI state for the transaction list tab.
 */
data class TransactionListUiState(
    val transactions: ImmutableList<TransactionListDisplayItem> = persistentListOf(),
    val isLoading: Boolean = false,
    val accountFilter: String? = null,
    val showAccountFilterInput: Boolean = false,
    val accountSuggestions: ImmutableList<String> = persistentListOf(),
    val foundTransactionIndex: Int? = null,
    val firstTransactionDate: SimpleDate? = null,
    val lastTransactionDate: SimpleDate? = null,
    val headerText: String = "----"
)

/**
 * Sealed class representing items in the transaction list.
 */
@Stable
sealed class TransactionListDisplayItem {
    /**
     * Header item showing the last update information.
     */
    data object Header : TransactionListDisplayItem()

    /**
     * Date delimiter showing the date for a group of transactions.
     */
    data class DateDelimiter(val date: SimpleDate, val isMonthShown: Boolean) : TransactionListDisplayItem()

    /**
     * Transaction item representing a single transaction.
     */
    @Stable
    data class Transaction(
        val id: Long,
        val date: SimpleDate,
        val description: String,
        val comment: String?,
        val accounts: ImmutableList<TransactionAccountDisplayItem>,
        val boldAccountName: String?,
        val runningTotal: String?
    ) : TransactionListDisplayItem()
}

/**
 * Represents an account row within a transaction.
 */
@Stable
data class TransactionAccountDisplayItem(
    val accountName: String,
    val amount: Float,
    val currency: String,
    val comment: String?,
    val amountStyle: AmountStyle?
)

/**
 * Events from the transaction list tab.
 */
sealed class TransactionListEvent {
    data class SetAccountFilter(val accountName: String?) : TransactionListEvent()
    data object ShowAccountFilterInput : TransactionListEvent()
    data object HideAccountFilterInput : TransactionListEvent()
    data object ClearAccountFilter : TransactionListEvent()
    data class GoToDate(val date: SimpleDate) : TransactionListEvent()
    data class ScrollToTransaction(val index: Int) : TransactionListEvent()
    data class SelectSuggestion(val accountName: String) : TransactionListEvent()
}
