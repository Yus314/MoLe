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

package net.ktnx.mobileledger.ui.transaction

/**
 * UI State for account rows management.
 * Handles account row CRUD, amount calculation, currency selection, and focus management.
 */
data class AccountRowsUiState(
    val accounts: List<TransactionAccountRow> = listOf(
        TransactionAccountRow(id = AccountRowsUiState.nextId()),
        TransactionAccountRow(id = AccountRowsUiState.nextId())
    ),
    val showCurrency: Boolean = false,
    val defaultCurrency: String = "",

    // Focus management
    val focusedRowId: Int? = null,
    val focusedElement: FocusedElement? = null,

    // Currency selector
    val showCurrencySelector: Boolean = false,
    val currencySelectorRowId: Int? = null,
    val availableCurrencies: List<String> = emptyList(),

    // Account suggestions
    val accountSuggestions: List<String> = emptyList(),
    val accountSuggestionsVersion: Int = 0,
    val accountSuggestionsForRowId: Int? = null
) {
    /**
     * Returns true if the account rows are submittable:
     * 1. At least two accounts have names
     * 2. Each account with amount has account name
     * 3. For each currency:
     *    a) Amounts balance to 0, or
     *    b) Exactly one account has empty amount (balance receiver)
     * 4. No invalid amounts
     */
    val isBalanced: Boolean
        get() {
            val accountsWithName = accounts.filter { it.accountName.isNotBlank() }

            // At least two accounts
            if (accountsWithName.size < 2) return false

            // Each account with amount must have name
            if (accounts.any { it.isAmountSet && it.accountName.isBlank() }) return false

            // Check balance per currency
            val currencyGroups = accounts.groupBy { it.currency }
            for ((_, currencyAccounts) in currencyGroups) {
                val accountsWithAmount = currencyAccounts.filter { it.isAmountSet && it.isAmountValid }
                val accountsWithNameAndNoAmount = currencyAccounts.filter {
                    it.accountName.isNotBlank() && !it.isAmountSet
                }

                if (accountsWithAmount.isEmpty()) continue

                val balance = accountsWithAmount.sumOf { it.amount?.toDouble() ?: 0.0 }
                val isBalancedCurrency = kotlin.math.abs(balance) < 0.005

                if (!isBalancedCurrency && accountsWithNameAndNoAmount.size != 1) {
                    return false
                }
            }

            // No invalid amounts
            if (accounts.any { !it.isAmountValid }) return false

            return true
        }

    val hasAccountChanges: Boolean
        get() = accounts.any { !it.isEmpty }

    companion object {
        private val idCounter = java.util.concurrent.atomic.AtomicInteger(0)

        fun nextId(): Int = idCounter.incrementAndGet()

        fun resetIdCounter() {
            idCounter.set(0)
        }
    }
}

/**
 * Events from UI to AccountRowsViewModel.
 */
sealed class AccountRowsEvent {
    // Account row updates
    data class UpdateAccountName(val rowId: Int, val name: String) : AccountRowsEvent()
    data class UpdateAmount(val rowId: Int, val amount: String) : AccountRowsEvent()
    data class UpdateCurrency(val rowId: Int, val currency: String) : AccountRowsEvent()
    data class UpdateAccountComment(val rowId: Int, val comment: String) : AccountRowsEvent()

    // Account row management
    data class AddAccountRow(val afterRowId: Int? = null) : AccountRowsEvent()
    data class RemoveAccountRow(val rowId: Int) : AccountRowsEvent()
    data class MoveAccountRow(val fromIndex: Int, val toIndex: Int) : AccountRowsEvent()

    // Focus management
    data class NoteFocus(val rowId: Int?, val element: FocusedElement?) : AccountRowsEvent()

    // Currency selector
    data class ShowCurrencySelector(val rowId: Int) : AccountRowsEvent()
    data object DismissCurrencySelector : AccountRowsEvent()
    data class AddCurrency(
        val name: String,
        val position: net.ktnx.mobileledger.model.Currency.Position,
        val gap: Boolean
    ) : AccountRowsEvent()
    data class DeleteCurrency(val name: String) : AccountRowsEvent()

    // Toggle visibility
    data object ToggleCurrency : AccountRowsEvent()
    data class ToggleAccountComment(val rowId: Int) : AccountRowsEvent()

    // Reset state
    data object Reset : AccountRowsEvent()

    // Set rows from template or external source
    data class SetRows(val rows: List<TransactionAccountRow>) : AccountRowsEvent()
}

/**
 * Side effects for one-time UI actions from AccountRowsViewModel.
 */
sealed class AccountRowsEffect {
    data class RequestFocus(val rowId: Int?, val element: FocusedElement) : AccountRowsEffect()
    data object HideKeyboard : AccountRowsEffect()
}
