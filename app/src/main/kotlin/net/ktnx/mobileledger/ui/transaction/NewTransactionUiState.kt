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

import java.util.concurrent.atomic.AtomicInteger
import net.ktnx.mobileledger.utils.SimpleDate

/**
 * UI State for the New Transaction screen in Compose.
 * Represents the complete state of the transaction form including header and account rows.
 */
data class NewTransactionUiState(
    val profileId: Long? = null,
    val date: SimpleDate = SimpleDate.today(),
    val description: String = "",
    val transactionComment: String = "",
    val accounts: List<TransactionAccountRow> = listOf(
        TransactionAccountRow(id = 1),
        TransactionAccountRow(id = 2)
    ),
    val showCurrency: Boolean = false,
    val showComments: Boolean = true,
    val focusedRowId: Int? = null,
    val focusedElement: FocusedElement? = null,

    // UI State
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val showDatePicker: Boolean = false,
    val showTemplateSelector: Boolean = false,
    val showCurrencySelector: Boolean = false,
    val currencySelectorRowId: Int? = null,
    val availableTemplates: List<TemplateItem> = emptyList(),
    val accountSuggestions: List<String> = emptyList(),
    val accountSuggestionsVersion: Int = 0,
    val accountSuggestionsForRowId: Int? = null,
    val descriptionSuggestions: List<String> = emptyList(),
    val isSimulateSave: Boolean = false,
    val isBusy: Boolean = false,
    val validationErrors: Map<ValidationField, String> = emptyMap()
) {
    /**
     * Transaction is submittable when:
     * 1. Description is not empty
     * 2. At least two accounts have names
     * 3. Each account with amount has account name
     * 4. For each currency:
     *    a) Amounts balance to 0, or
     *    b) Exactly one account has empty amount (balance receiver)
     * 5. No invalid amounts
     */
    val isSubmittable: Boolean
        get() {
            // 1. Must have description
            if (description.isBlank()) return false

            val accountsWithName = accounts.filter { it.accountName.isNotBlank() }

            // 2. At least two accounts
            if (accountsWithName.size < 2) return false

            // 3. Each account with amount must have name
            if (accounts.any { it.isAmountSet && it.accountName.isBlank() }) return false

            // 4. Check balance per currency
            val currencyGroups = accounts.groupBy { it.currency }
            for ((currency, currencyAccounts) in currencyGroups) {
                val accountsWithAmount = currencyAccounts.filter { it.isAmountSet && it.isAmountValid }
                val accountsWithNameAndNoAmount = currencyAccounts.filter {
                    it.accountName.isNotBlank() && !it.isAmountSet
                }

                if (accountsWithAmount.isEmpty()) continue

                val balance = accountsWithAmount.sumOf { it.amount?.toDouble() ?: 0.0 }
                val isBalanced = kotlin.math.abs(balance) < 0.005

                if (!isBalanced && accountsWithNameAndNoAmount.size != 1) {
                    return false
                }
            }

            // 5. No invalid amounts
            if (accounts.any { !it.isAmountValid }) return false

            return true
        }

    val hasUnsavedChanges: Boolean
        get() = description.isNotBlank() ||
            transactionComment.isNotBlank() ||
            accounts.any { !it.isEmpty }

    companion object {
        private val idCounter = AtomicInteger(0)

        fun nextId(): Int = idCounter.incrementAndGet()

        fun resetIdCounter() {
            idCounter.set(0)
        }
    }
}

/**
 * Represents a single account row in the transaction form.
 */
data class TransactionAccountRow(
    val id: Int = NewTransactionUiState.nextId(),
    val accountName: String = "",
    val amountText: String = "",
    val amountHint: String? = null,
    val currency: String = "",
    val comment: String = "",
    val isAmountValid: Boolean = true,
    val isLast: Boolean = false
) {
    val isAmountSet: Boolean
        get() = amountText.isNotBlank()

    val amount: Float?
        get() = if (isAmountSet && isAmountValid) {
            try {
                amountText.replace(',', '.').toFloatOrNull()
            } catch (e: NumberFormatException) {
                null
            }
        } else {
            null
        }

    val isEmpty: Boolean
        get() = accountName.isBlank() && amountText.isBlank() && comment.isBlank()
}

/**
 * Template item for quick transaction creation.
 */
data class TemplateItem(val id: Long, val name: String, val description: String? = null, val regex: String? = null)

/**
 * Focused element tracking for keyboard and cursor management.
 */
enum class FocusedElement {
    Description,
    TransactionComment,
    Account,
    Amount,
    AccountComment
}

/**
 * Validation field identifiers.
 */
enum class ValidationField {
    DESCRIPTION,
    DATE,
    ACCOUNT,
    AMOUNT
}

/**
 * Events from UI to ViewModel.
 */
sealed class NewTransactionEvent {
    // Header events
    data class UpdateDate(val date: SimpleDate) : NewTransactionEvent()
    data class UpdateDescription(val description: String) : NewTransactionEvent()
    data class UpdateTransactionComment(val comment: String) : NewTransactionEvent()
    data object ShowDatePicker : NewTransactionEvent()
    data object DismissDatePicker : NewTransactionEvent()

    // Account row events
    data class UpdateAccountName(val rowId: Int, val name: String) : NewTransactionEvent()
    data class UpdateAmount(val rowId: Int, val amount: String) : NewTransactionEvent()
    data class UpdateCurrency(val rowId: Int, val currency: String) : NewTransactionEvent()
    data class UpdateAccountComment(val rowId: Int, val comment: String) : NewTransactionEvent()
    data class AddAccountRow(val afterRowId: Int? = null) : NewTransactionEvent()
    data class RemoveAccountRow(val rowId: Int) : NewTransactionEvent()
    data class MoveAccountRow(val fromIndex: Int, val toIndex: Int) : NewTransactionEvent()

    // Focus management
    data class NoteFocus(val rowId: Int?, val element: FocusedElement?) : NewTransactionEvent()

    // Currency selector
    data class ShowCurrencySelector(val rowId: Int) : NewTransactionEvent()
    data object DismissCurrencySelector : NewTransactionEvent()

    // Template events
    data object ShowTemplateSelector : NewTransactionEvent()
    data object DismissTemplateSelector : NewTransactionEvent()
    data class ApplyTemplate(val templateId: Long) : NewTransactionEvent()
    data class ApplyTemplateFromQr(val qrText: String) : NewTransactionEvent()

    // Toggle visibility
    data object ToggleCurrency : NewTransactionEvent()
    data object ToggleComments : NewTransactionEvent()
    data object ToggleSimulateSave : NewTransactionEvent()

    // Submission
    data object Submit : NewTransactionEvent()
    data object Reset : NewTransactionEvent()

    // Load from existing transaction
    data class LoadFromTransaction(val transactionId: Long) : NewTransactionEvent()
    data class LoadFromDescription(val description: String) : NewTransactionEvent()

    // Navigation
    data object NavigateBack : NewTransactionEvent()
    data object ConfirmDiscardChanges : NewTransactionEvent()

    // Error handling
    data object ClearError : NewTransactionEvent()
}

/**
 * Side effects for one-time UI actions.
 */
sealed class NewTransactionEffect {
    data object NavigateBack : NewTransactionEffect()
    data object TransactionSaved : NewTransactionEffect()
    data class ShowError(val message: String) : NewTransactionEffect()
    data class RequestFocus(val rowId: Int, val element: FocusedElement) : NewTransactionEffect()
    data object HideKeyboard : NewTransactionEffect()
}
