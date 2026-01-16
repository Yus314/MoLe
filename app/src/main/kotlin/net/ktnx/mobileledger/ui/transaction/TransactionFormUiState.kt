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

import net.ktnx.mobileledger.model.FutureDates
import net.ktnx.mobileledger.utils.SimpleDate

/**
 * UI State for transaction form management.
 * Handles date, description, comment, and submission state.
 */
data class TransactionFormUiState(
    val profileId: Long? = null,
    val date: SimpleDate = SimpleDate.today(),
    val description: String = "",
    val transactionComment: String = "",
    val isTransactionCommentExpanded: Boolean = false,
    val showDatePicker: Boolean = false,
    val futureDates: FutureDates = FutureDates.All,
    val descriptionSuggestions: List<String> = emptyList(),

    // Submission state
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val isSimulateSave: Boolean = false,
    val isBusy: Boolean = false,

    // Validation
    val validationErrors: Map<FormValidationField, String> = emptyMap()
) {
    val hasUnsavedChanges: Boolean
        get() = description.isNotBlank() || transactionComment.isNotBlank()

    val isFormValid: Boolean
        get() = description.isNotBlank() && validationErrors.isEmpty()
}

/**
 * Validation field identifiers for form fields.
 */
enum class FormValidationField {
    DESCRIPTION,
    DATE
}

/**
 * Events from UI to TransactionFormViewModel.
 */
sealed class TransactionFormEvent {
    // Form field updates
    data class UpdateDate(val date: SimpleDate) : TransactionFormEvent()
    data class UpdateDescription(val description: String) : TransactionFormEvent()
    data class UpdateTransactionComment(val comment: String) : TransactionFormEvent()

    // Date picker
    data object ShowDatePicker : TransactionFormEvent()
    data object DismissDatePicker : TransactionFormEvent()

    // Toggle visibility
    data object ToggleTransactionComment : TransactionFormEvent()
    data object ToggleSimulateSave : TransactionFormEvent()

    // Submission
    data class Submit(val accountRows: List<TransactionAccountRow>) : TransactionFormEvent()
    data object Reset : TransactionFormEvent()

    // Load from existing transaction
    data class LoadFromTransaction(val transactionId: Long) : TransactionFormEvent()
    data class LoadFromDescription(val description: String) : TransactionFormEvent()

    // Navigation
    data object NavigateBack : TransactionFormEvent()
    data object ConfirmDiscardChanges : TransactionFormEvent()

    // Error handling
    data object ClearError : TransactionFormEvent()
}

/**
 * Side effects for one-time UI actions from TransactionFormViewModel.
 */
sealed class TransactionFormEffect {
    data object NavigateBack : TransactionFormEffect()
    data object TransactionSaved : TransactionFormEffect()
    data class ShowError(val message: String) : TransactionFormEffect()
    data object HideKeyboard : TransactionFormEffect()
    data class RequestFocus(val element: FocusedElement) : TransactionFormEffect()
}
