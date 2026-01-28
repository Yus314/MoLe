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
import net.ktnx.mobileledger.core.common.utils.SimpleDate
import net.ktnx.mobileledger.core.domain.model.FutureDates
import net.ktnx.mobileledger.core.domain.model.Transaction
import net.ktnx.mobileledger.domain.usecase.TransactionSender
import net.ktnx.mobileledger.feature.profile.usecase.ObserveCurrentProfileUseCase
import net.ktnx.mobileledger.feature.transaction.usecase.GetFirstTransactionByDescriptionUseCase
import net.ktnx.mobileledger.feature.transaction.usecase.GetTransactionByIdUseCase
import net.ktnx.mobileledger.feature.transaction.usecase.SearchTransactionDescriptionsUseCase
import net.ktnx.mobileledger.feature.transaction.usecase.StoreTransactionUseCase
import net.ktnx.mobileledger.feature.transaction.usecase.TransactionBalanceCalculator
import net.ktnx.mobileledger.service.AppStateService

@HiltViewModel
class TransactionFormViewModel @Inject constructor(
    observeCurrentProfileUseCase: ObserveCurrentProfileUseCase,
    private val searchTransactionDescriptionsUseCase: SearchTransactionDescriptionsUseCase,
    private val storeTransactionUseCase: StoreTransactionUseCase,
    private val getTransactionByIdUseCase: GetTransactionByIdUseCase,
    private val getFirstTransactionByDescriptionUseCase: GetFirstTransactionByDescriptionUseCase,
    private val appStateService: AppStateService,
    private val transactionSender: TransactionSender,
    private val balanceCalculator: TransactionBalanceCalculator
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionFormUiState())
    val uiState: StateFlow<TransactionFormUiState> = _uiState.asStateFlow()

    private val _effects = Channel<TransactionFormEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val currentProfile = observeCurrentProfileUseCase()

    init {
        initializeFromProfile()
    }

    private fun initializeFromProfile() {
        val profile = currentProfile.value
        if (profile != null) {
            _uiState.update {
                it.copy(
                    profileId = profile.id ?: 0,
                    futureDates = profile.futureDates
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
            searchTransactionDescriptionsUseCase(termUpper)
                .onSuccess { suggestions ->
                    _uiState.update { it.copy(descriptionSuggestions = suggestions) }
                }
                .onFailure { e ->
                    logcat { "Failed to lookup suggestions: ${e.message}" }
                    _uiState.update { it.copy(descriptionSuggestions = emptyList()) }
                }
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

        val profile = currentProfile.value ?: run {
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
                    handleTransactionSendSuccess(transaction, profile.id ?: 0)
                },
                onFailure = { exception ->
                    handleTransactionSendFailure(exception.message ?: "送信に失敗しました")
                }
            )
        }
    }

    private suspend fun handleTransactionSendSuccess(transaction: Transaction, profileId: Long) {
        storeTransactionUseCase(transaction, profileId)
            .onSuccess {
                logcat { "Transaction saved to DB" }
                appStateService.signalDataChanged()
                _uiState.update { it.copy(isSubmitting = false, isBusy = false) }
                _effects.send(TransactionFormEffect.TransactionSaved)
            }
            .onFailure { e ->
                logcat { "Failed to save transaction: ${e.message}" }
                _uiState.update { it.copy(isSubmitting = false, isBusy = false) }
                _effects.send(
                    TransactionFormEffect.ShowError(
                        "取引はサーバーに送信されましたが、ローカル保存に失敗しました: ${e.message}"
                    )
                )
            }
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
     * Delegates balance calculation and auto-balance to [TransactionBalanceCalculator].
     */
    private fun constructTransaction(accountRows: List<TransactionAccountRow>): Transaction {
        val state = _uiState.value

        // Convert UI rows to UseCase input
        val entries = accountRows.map { row ->
            TransactionBalanceCalculator.AccountEntry(
                accountName = row.accountName,
                amount = row.amount,
                currency = row.currency,
                comment = row.comment.ifBlank { null },
                isAmountSet = row.isAmountSet,
                isAmountValid = row.isAmountValid
            )
        }

        // Delegate balance calculation to UseCase
        val balanceResult = balanceCalculator.calculateBalance(entries)

        return Transaction(
            id = null,
            ledgerId = 0L,
            date = state.date,
            description = state.description,
            comment = state.transactionComment.ifBlank { null },
            lines = balanceResult.lines
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
            getTransactionByIdUseCase(transactionId)
                .onSuccess { transaction ->
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
                        _effects.send(TransactionFormEffect.ShowError("取引が見つかりません"))
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isBusy = false) }
                    _effects.send(
                        TransactionFormEffect.ShowError("取引の読み込みに失敗しました: ${e.message}")
                    )
                }
        }
    }

    private fun loadFromDescription(description: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, description = description) }
            getFirstTransactionByDescriptionUseCase(description)
                .onSuccess {
                    _uiState.update { it.copy(isBusy = false) }
                    // Note: 見つからない場合はエラーではない（新規入力として扱う）
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isBusy = false) }
                    logcat { "Failed to load from description: ${e.message}" }
                    // サジェスト機能の失敗は致命的ではないのでログのみ
                }
        }
    }

    private fun handleNavigateBack() {
        viewModelScope.launch {
            if (_uiState.value.hasUnsavedChanges) {
                _effects.send(TransactionFormEffect.ShowDiscardChangesDialog)
            } else {
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
