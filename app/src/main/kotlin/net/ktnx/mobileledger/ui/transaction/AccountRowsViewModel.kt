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
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import logcat.logcat
import net.ktnx.mobileledger.domain.usecase.AccountSuggestionLookup
import net.ktnx.mobileledger.domain.usecase.DeleteCurrencyUseCase
import net.ktnx.mobileledger.domain.usecase.GetAllCurrenciesUseCase
import net.ktnx.mobileledger.domain.usecase.ObserveCurrentProfileUseCase
import net.ktnx.mobileledger.domain.usecase.SaveCurrencyUseCase
import net.ktnx.mobileledger.domain.usecase.TransactionAccountRowManager
import net.ktnx.mobileledger.domain.usecase.TransactionBalanceCalculator
import net.ktnx.mobileledger.service.CurrencyFormatter
import net.ktnx.mobileledger.service.RowIdGenerator

@HiltViewModel
class AccountRowsViewModel @Inject constructor(
    observeCurrentProfileUseCase: ObserveCurrentProfileUseCase,
    private val getAllCurrenciesUseCase: GetAllCurrenciesUseCase,
    private val saveCurrencyUseCase: SaveCurrencyUseCase,
    private val deleteCurrencyUseCase: DeleteCurrencyUseCase,
    private val currencyFormatter: CurrencyFormatter,
    private val rowIdGenerator: RowIdGenerator,
    private val balanceCalculator: TransactionBalanceCalculator,
    private val rowManager: TransactionAccountRowManager,
    private val suggestionLookup: AccountSuggestionLookup
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountRowsUiState())
    val uiState: StateFlow<AccountRowsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<AccountRowsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var accountSuggestionJob: Job? = null
    private val currentProfile = observeCurrentProfileUseCase()

    init {
        initializeFromProfile()
    }

    private fun initializeFromProfile() {
        val profile = currentProfile.value
        if (profile != null) {
            val defaultCurrency = profile.defaultCommodityOrEmpty
            rowIdGenerator.reset()
            _uiState.update {
                it.copy(
                    showCurrency = profile.showCommodityByDefault,
                    defaultCurrency = defaultCurrency,
                    accounts = listOf(
                        TransactionAccountRow(id = rowIdGenerator.nextId(), currency = defaultCurrency),
                        TransactionAccountRow(id = rowIdGenerator.nextId(), currency = defaultCurrency)
                    )
                )
            }
            recalculateAmountHints()
            loadCurrencies()
        }
    }

    private fun loadCurrencies() {
        viewModelScope.launch {
            getAllCurrenciesUseCase()
                .onSuccess { currencies ->
                    _uiState.update { it.copy(availableCurrencies = currencies.map { c -> c.name }) }
                }
        }
    }

    fun onEvent(event: AccountRowsEvent) {
        when (event) {
            is AccountRowsEvent.UpdateAccountName -> updateAccountName(event.rowId, event.name)
            is AccountRowsEvent.UpdateAmount -> updateAmount(event.rowId, event.amount)
            is AccountRowsEvent.UpdateCurrency -> updateCurrency(event.rowId, event.currency)
            is AccountRowsEvent.UpdateAccountComment -> updateAccountComment(event.rowId, event.comment)
            is AccountRowsEvent.AddAccountRow -> addAccountRow(event.afterRowId)
            is AccountRowsEvent.RemoveAccountRow -> removeAccountRow(event.rowId)
            is AccountRowsEvent.MoveAccountRow -> moveAccountRow(event.fromIndex, event.toIndex)
            is AccountRowsEvent.NoteFocus -> noteFocus(event.rowId, event.element)
            is AccountRowsEvent.ShowCurrencySelector -> showCurrencySelector(event.rowId)
            AccountRowsEvent.DismissCurrencySelector -> dismissCurrencySelector()
            is AccountRowsEvent.AddCurrency -> addCurrency(event.name, event.position, event.gap)
            is AccountRowsEvent.DeleteCurrency -> deleteCurrency(event.name)
            AccountRowsEvent.ToggleCurrency -> toggleCurrency()
            is AccountRowsEvent.ToggleAccountComment -> toggleAccountComment(event.rowId)
            AccountRowsEvent.Reset -> reset()
            is AccountRowsEvent.SetRows -> setRows(event.rows)
        }
    }

    private fun updateAccountName(rowId: Int, name: String) {
        _uiState.update { state ->
            state.copy(
                accounts = rowManager.updateRow(state.accounts, rowId) { it.copy(accountName = name) }
            )
        }
        lookupAccountSuggestions(rowId, name)
        ensureMinimumRows()
        recalculateAmountHints()
    }

    private fun lookupAccountSuggestions(rowId: Int, term: String) {
        logcat { "lookupAccountSuggestions: rowId=$rowId, term='$term'" }

        accountSuggestionJob?.cancel()

        if (!suggestionLookup.isTermValid(term)) {
            logcat { "term too short (${term.length}), clearing suggestions" }
            _uiState.update {
                it.copy(
                    accountSuggestions = emptyList(),
                    accountSuggestionsVersion = it.accountSuggestionsVersion + 1,
                    accountSuggestionsForRowId = null
                )
            }
            return
        }

        accountSuggestionJob = viewModelScope.launch {
            delay(AccountSuggestionLookup.DEFAULT_DEBOUNCE_MS)

            val profileId = currentProfile.value?.id ?: return@launch

            val suggestions = suggestionLookup.search(profileId, term)

            if (isActive) {
                logcat { "got ${suggestions.size} suggestions for row $rowId: ${suggestions.take(3)}" }
                _uiState.update {
                    it.copy(
                        accountSuggestions = suggestions,
                        accountSuggestionsVersion = it.accountSuggestionsVersion + 1,
                        accountSuggestionsForRowId = rowId
                    )
                }
            }
        }
    }

    private fun updateAmount(rowId: Int, amountText: String) {
        _uiState.update { state ->
            val isValid = validateAmount(amountText)
            state.copy(
                accounts = rowManager.updateRow(state.accounts, rowId) {
                    it.copy(amountText = amountText, isAmountValid = isValid)
                }
            )
        }
        recalculateAmountHints()
        ensureMinimumRows()
    }

    private fun validateAmount(amountText: String): Boolean {
        if (amountText.isBlank()) return true
        return try {
            amountText.replace(',', '.').toFloat()
            true
        } catch (e: NumberFormatException) {
            false
        }
    }

    /**
     * Recalculate amount hints for all account rows.
     *
     * Delegates calculation to [TransactionBalanceCalculator].
     */
    private fun recalculateAmountHints() {
        _uiState.update { state ->
            // Convert UI rows to UseCase input
            val entries = state.accounts.map { row ->
                TransactionBalanceCalculator.AccountEntry(
                    accountName = row.accountName,
                    amount = row.amount,
                    currency = row.currency,
                    comment = row.comment.ifBlank { null },
                    isAmountSet = row.isAmountSet,
                    isAmountValid = row.isAmountValid
                )
            }

            // Delegate hint calculation to UseCase
            val hints = balanceCalculator.calculateAmountHints(entries) { amount ->
                currencyFormatter.formatNumber(amount)
            }

            // Apply hints to account rows
            val newAccounts = state.accounts.mapIndexed { idx, row ->
                val hint = hints.find { it.entryIndex == idx }?.hint
                row.copy(amountHint = hint)
            }

            state.copy(accounts = newAccounts)
        }
    }

    private fun updateCurrency(rowId: Int, currency: String) {
        _uiState.update { state ->
            state.copy(
                accounts = rowManager.updateRow(state.accounts, rowId) { it.copy(currency = currency) },
                showCurrencySelector = false,
                currencySelectorRowId = null
            )
        }
        recalculateAmountHints()
    }

    private fun updateAccountComment(rowId: Int, comment: String) {
        _uiState.update { state ->
            state.copy(
                accounts = rowManager.updateRow(state.accounts, rowId) { it.copy(comment = comment) }
            )
        }
    }

    private fun addAccountRow(afterRowId: Int?) {
        val defaultCurrency = _uiState.value.defaultCurrency
        _uiState.update { state ->
            val newRow = TransactionAccountRow(id = rowIdGenerator.nextId(), currency = defaultCurrency)
            state.copy(accounts = rowManager.addRow(state.accounts, afterRowId, newRow))
        }
    }

    private fun removeAccountRow(rowId: Int) {
        _uiState.update { state ->
            state.copy(accounts = rowManager.removeRow(state.accounts, rowId))
        }
        ensureMinimumRows()
    }

    private fun moveAccountRow(fromIndex: Int, toIndex: Int) {
        _uiState.update { state ->
            state.copy(accounts = rowManager.moveRow(state.accounts, fromIndex, toIndex))
        }
    }

    private fun ensureMinimumRows() {
        _uiState.update { state ->
            val defaultCurrency = state.defaultCurrency
            state.copy(
                accounts = rowManager.ensureMinimumRows(state.accounts) {
                    TransactionAccountRow(id = rowIdGenerator.nextId(), currency = defaultCurrency)
                }
            )
        }
    }

    private fun noteFocus(rowId: Int?, element: FocusedElement?) {
        _uiState.update { it.copy(focusedRowId = rowId, focusedElement = element) }
    }

    private fun showCurrencySelector(rowId: Int) {
        _uiState.update {
            it.copy(
                showCurrencySelector = true,
                currencySelectorRowId = rowId
            )
        }
    }

    private fun dismissCurrencySelector() {
        _uiState.update {
            it.copy(
                showCurrencySelector = false,
                currencySelectorRowId = null
            )
        }
    }

    private fun addCurrency(
        name: String,
        position: net.ktnx.mobileledger.core.domain.model.CurrencyPosition,
        gap: Boolean
    ) {
        viewModelScope.launch {
            val currency = net.ktnx.mobileledger.core.domain.model.Currency(
                name = name,
                position = position,
                hasGap = gap
            )
            saveCurrencyUseCase(currency)
            loadCurrencies()
        }
    }

    private fun deleteCurrency(name: String) {
        viewModelScope.launch {
            deleteCurrencyUseCase(name)
            loadCurrencies()
        }
    }

    private fun toggleCurrency() {
        val profile = currentProfile.value ?: return
        val newShowCurrency = !_uiState.value.showCurrency
        val defaultCurrency = if (newShowCurrency) profile.defaultCommodityOrEmpty else ""

        _uiState.update { state ->
            state.copy(
                showCurrency = newShowCurrency,
                defaultCurrency = defaultCurrency,
                accounts = state.accounts.map { row ->
                    row.copy(currency = defaultCurrency)
                }
            )
        }
        recalculateAmountHints()
    }

    private fun toggleAccountComment(rowId: Int) {
        val row = _uiState.value.accounts.find { it.id == rowId }
        val wasExpanded = row?.isCommentExpanded ?: false

        _uiState.update { state ->
            state.copy(
                accounts = state.accounts.map { r ->
                    if (r.id == rowId) r.copy(isCommentExpanded = !r.isCommentExpanded) else r
                }
            )
        }

        if (!wasExpanded) {
            viewModelScope.launch {
                _effects.send(AccountRowsEffect.RequestFocus(rowId, FocusedElement.AccountComment))
            }
        }
    }

    private fun reset() {
        val profile = currentProfile.value
        val defaultCurrency = profile?.defaultCommodityOrEmpty ?: ""

        rowIdGenerator.reset()

        _uiState.update {
            AccountRowsUiState(
                showCurrency = profile?.showCommodityByDefault ?: false,
                defaultCurrency = defaultCurrency,
                accounts = listOf(
                    TransactionAccountRow(id = rowIdGenerator.nextId(), currency = defaultCurrency),
                    TransactionAccountRow(id = rowIdGenerator.nextId(), currency = defaultCurrency)
                ),
                availableCurrencies = it.availableCurrencies
            )
        }
    }

    private fun setRows(rows: List<TransactionAccountRow>) {
        val defaultCurrency = _uiState.value.defaultCurrency

        _uiState.update { state ->
            state.copy(
                accounts = rowManager.setRows(rows) {
                    TransactionAccountRow(id = rowIdGenerator.nextId(), currency = defaultCurrency)
                }
            )
        }
        recalculateAmountHints()
    }
}
