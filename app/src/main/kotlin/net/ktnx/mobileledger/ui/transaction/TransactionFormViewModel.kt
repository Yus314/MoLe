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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.logcat
import net.ktnx.mobileledger.data.repository.ProfileRepository
import net.ktnx.mobileledger.data.repository.TransactionRepository
import net.ktnx.mobileledger.domain.model.Transaction
import net.ktnx.mobileledger.domain.model.TransactionLine
import net.ktnx.mobileledger.domain.usecase.TransactionSender
import net.ktnx.mobileledger.model.FutureDates
import net.ktnx.mobileledger.service.AppStateService
import net.ktnx.mobileledger.utils.SimpleDate

@HiltViewModel
class TransactionFormViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val transactionRepository: TransactionRepository,
    private val appStateService: AppStateService,
    private val transactionSender: TransactionSender
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionFormUiState())
    val uiState: StateFlow<TransactionFormUiState> = _uiState.asStateFlow()

    private val _effects = Channel<TransactionFormEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        initializeFromProfile()
    }

    private fun initializeFromProfile() {
        val profile = profileRepository.currentProfile.value
        if (profile != null) {
            val futureDates = FutureDates.valueOf(profile.futureDates)
            _uiState.update {
                it.copy(
                    profileId = profile.id,
                    futureDates = futureDates
                )
            }
        }
    }

    fun onEvent(event: TransactionFormEvent) {
        when (event) {
            is TransactionFormEvent.UpdateDate -> updateDate(event.date)
            is TransactionFormEvent.UpdateDescription -> updateDescription(event.description)
            is TransactionFormEvent.UpdateTransactionComment -> updateTransactionComment(event.comment)
            TransactionFormEvent.ShowDatePicker -> showDatePicker()
            TransactionFormEvent.DismissDatePicker -> dismissDatePicker()
            TransactionFormEvent.ToggleTransactionComment -> toggleTransactionComment()
            TransactionFormEvent.ToggleSimulateSave -> toggleSimulateSave()
            is TransactionFormEvent.Submit -> submit(event.accountRows)
            TransactionFormEvent.Reset -> reset()
            is TransactionFormEvent.LoadFromTransaction -> loadFromTransaction(event.transactionId)
            is TransactionFormEvent.LoadFromDescription -> loadFromDescription(event.description)
            TransactionFormEvent.NavigateBack -> handleNavigateBack()
            TransactionFormEvent.ConfirmDiscardChanges -> confirmDiscardChanges()
            TransactionFormEvent.ClearError -> clearError()
        }
    }

    fun setProfile(profileId: Long) {
        logcat { "setProfile: profileId=$profileId" }
        _uiState.update { it.copy(profileId = profileId) }
    }

    private fun updateDate(date: SimpleDate) {
        _uiState.update { it.copy(date = date, showDatePicker = false) }
    }

    private fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description) }
        lookupDescriptionSuggestions(description)
    }

    private fun lookupDescriptionSuggestions(term: String) {
        if (term.length < 2) {
            _uiState.update { it.copy(descriptionSuggestions = emptyList()) }
            return
        }

        viewModelScope.launch {
            val termUpper = term.uppercase()
            val containers = transactionRepository.searchByDescription(termUpper)
            val suggestions = containers.mapNotNull { it.description }
            _uiState.update { it.copy(descriptionSuggestions = suggestions) }
        }
    }

    private fun updateTransactionComment(comment: String) {
        _uiState.update { it.copy(transactionComment = comment) }
    }

    private fun showDatePicker() {
        _uiState.update { it.copy(showDatePicker = true) }
    }

    private fun dismissDatePicker() {
        _uiState.update { it.copy(showDatePicker = false) }
    }

    private fun toggleTransactionComment() {
        val wasExpanded = _uiState.value.isTransactionCommentExpanded
        _uiState.update { it.copy(isTransactionCommentExpanded = !wasExpanded) }

        if (!wasExpanded) {
            viewModelScope.launch {
                _effects.send(TransactionFormEffect.RequestFocus(FocusedElement.TransactionComment))
            }
        }
    }

    private fun toggleSimulateSave() {
        _uiState.update { it.copy(isSimulateSave = !it.isSimulateSave) }
    }

    private fun submit(accountRows: List<TransactionAccountRow>) {
        val state = _uiState.value
        if (state.description.isBlank()) return

        val profile = profileRepository.currentProfile.value ?: run {
            viewModelScope.launch {
                _effects.send(TransactionFormEffect.ShowError("プロファイルが選択されていません"))
            }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, isBusy = true) }

        val transaction = constructTransaction(accountRows)

        viewModelScope.launch {
            _effects.send(TransactionFormEffect.HideKeyboard)

            val result = transactionSender.send(profile, transaction, state.isSimulateSave)
            result.fold(
                onSuccess = {
                    handleTransactionSendSuccess(transaction, profile.id)
                },
                onFailure = { exception ->
                    handleTransactionSendFailure(exception.message ?: "送信に失敗しました")
                }
            )
        }
    }

    private suspend fun handleTransactionSendSuccess(transaction: Transaction, profileId: Long) {
        try {
            transactionRepository.storeTransaction(transaction, profileId)
            logcat { "Transaction saved to DB" }
            appStateService.signalDataChanged()
        } catch (e: Exception) {
            logcat { "Failed to save transaction: ${e.message}" }
        }

        _uiState.update { it.copy(isSubmitting = false, isBusy = false) }
        _effects.send(TransactionFormEffect.TransactionSaved)
    }

    private fun handleTransactionSendFailure(errorMessage: String) {
        _uiState.update {
            it.copy(
                isSubmitting = false,
                isBusy = false,
                submitError = errorMessage
            )
        }
        viewModelScope.launch {
            _effects.send(TransactionFormEffect.ShowError(errorMessage))
        }
    }

    /**
     * Construct a domain model Transaction from the form state and account rows.
     *
     * This method handles:
     * - Balance calculation for accounts with empty amounts
     * - Currency grouping for auto-balance
     * - Conversion from UI rows to TransactionLine domain objects
     */
    private fun constructTransaction(accountRows: List<TransactionAccountRow>): Transaction {
        val state = _uiState.value

        // Build transaction lines, tracking balances for auto-balance calculation
        val currencyBalances = mutableMapOf<String, Float>()
        val linesWithEmptyAmount = mutableMapOf<String, MutableList<Int>>() // currency -> indices
        val lines = mutableListOf<TransactionLine>()

        for (row in accountRows) {
            if (row.accountName.isBlank()) continue

            val lineIndex = lines.size
            val amount: Float?

            if (row.isAmountSet && row.isAmountValid) {
                amount = row.amount ?: 0f
                currencyBalances[row.currency] = (currencyBalances[row.currency] ?: 0f) + amount
            } else {
                amount = null
                linesWithEmptyAmount.getOrPut(row.currency) { mutableListOf() }.add(lineIndex)
            }

            lines.add(
                TransactionLine(
                    id = null,
                    accountName = row.accountName.trim(),
                    amount = amount,
                    currency = row.currency,
                    comment = row.comment.ifBlank { null }
                )
            )
        }

        // Auto-balance: set amount for accounts with empty amount (one per currency)
        for ((currency, indices) in linesWithEmptyAmount) {
            if (indices.size == 1) {
                val balance = currencyBalances[currency] ?: 0f
                val idx = indices[0]
                lines[idx] = lines[idx].copy(amount = -balance)
            }
        }

        return Transaction(
            id = null,
            ledgerId = 0L,
            date = state.date,
            description = state.description,
            comment = state.transactionComment.ifBlank { null },
            lines = lines
        )
    }

    private fun reset() {
        _uiState.update {
            TransactionFormUiState(
                profileId = it.profileId,
                futureDates = it.futureDates
            )
        }

        viewModelScope.launch {
            _effects.send(TransactionFormEffect.RequestFocus(FocusedElement.Description))
        }
    }

    private fun loadFromTransaction(transactionId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }

            val transaction = transactionRepository.getTransactionByIdSync(transactionId)

            if (transaction != null) {
                _uiState.update { state ->
                    state.copy(
                        description = transaction.description,
                        transactionComment = transaction.comment ?: "",
                        isBusy = false
                    )
                }
            } else {
                _uiState.update { it.copy(isBusy = false) }
            }
        }
    }

    private fun loadFromDescription(description: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, description = description) }

            val transactionWithAccounts = transactionRepository.getFirstByDescription(description)

            if (transactionWithAccounts != null) {
                _uiState.update { state ->
                    state.copy(
                        isBusy = false
                    )
                }
            } else {
                _uiState.update { it.copy(isBusy = false) }
            }
        }
    }

    private fun handleNavigateBack() {
        if (!_uiState.value.hasUnsavedChanges) {
            viewModelScope.launch {
                _effects.send(TransactionFormEffect.NavigateBack)
            }
        }
    }

    private fun confirmDiscardChanges() {
        viewModelScope.launch {
            _effects.send(TransactionFormEffect.NavigateBack)
        }
    }

    private fun clearError() {
        _uiState.update { it.copy(submitError = null) }
    }

    // Helper function to set description and comment from template
    fun applyTemplateData(description: String, transactionComment: String?, date: SimpleDate?) {
        _uiState.update { state ->
            state.copy(
                description = description,
                transactionComment = transactionComment ?: state.transactionComment,
                date = date ?: state.date
            )
        }
    }
}
