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
import net.ktnx.mobileledger.data.repository.AccountRepository
import net.ktnx.mobileledger.data.repository.CurrencyRepository
import net.ktnx.mobileledger.data.repository.ProfileRepository
import net.ktnx.mobileledger.service.CurrencyFormatter
import net.ktnx.mobileledger.service.RowIdGenerator

@HiltViewModel
class AccountRowsViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val accountRepository: AccountRepository,
    private val currencyRepository: CurrencyRepository,
    private val currencyFormatter: CurrencyFormatter,
    private val rowIdGenerator: RowIdGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountRowsUiState())
    val uiState: StateFlow<AccountRowsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<AccountRowsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var accountSuggestionJob: Job? = null

    init {
        initializeFromProfile()
    }

    private fun initializeFromProfile() {
        val profile = profileRepository.currentProfile.value
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
            val currencies = currencyRepository.getAllCurrencies().map { it.name }
            _uiState.update { it.copy(availableCurrencies = currencies) }
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
                accounts = state.accounts.map { row ->
                    if (row.id == rowId) {
                        row.copy(accountName = name)
                    } else {
                        row
                    }
                }
            )
        }
        lookupAccountSuggestions(rowId, name)
        ensureMinimumRows()
        recalculateAmountHints()
    }

    private fun lookupAccountSuggestions(rowId: Int, term: String) {
        logcat { "lookupAccountSuggestions: rowId=$rowId, term='$term'" }

        accountSuggestionJob?.cancel()

        if (term.length < 2) {
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
            delay(50)

            val profile = profileRepository.currentProfile.value
            logcat { "profileId=${profile?.id}" }

            if (profile == null) {
                logcat { "profile is null, returning" }
                return@launch
            }

            val profileId = profile.id ?: run {
                logcat { "profile has no id, returning" }
                return@launch
            }

            val termUpper = term.uppercase()
            logcat { "querying DB: profileId=$profileId, term='$termUpper'" }
            val suggestions = accountRepository.searchAccountNames(profileId, termUpper)

            if (isActive) {
                logcat { "got ${suggestions.size} suggestions for row $rowId: ${suggestions.take(3)}" }
                _uiState.update {
                    it.copy(
                        accountSuggestions = suggestions,
                        accountSuggestionsVersion = it.accountSuggestionsVersion + 1,
                        accountSuggestionsForRowId = rowId
                    )
                }
            } else {
                logcat { "job cancelled, discarding ${suggestions.size} suggestions" }
            }
        }
    }

    private fun updateAmount(rowId: Int, amountText: String) {
        _uiState.update { state ->
            state.copy(
                accounts = state.accounts.map { row ->
                    if (row.id == rowId) {
                        val isValid = validateAmount(amountText)
                        row.copy(amountText = amountText, isAmountValid = isValid)
                    } else {
                        row
                    }
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

    private fun recalculateAmountHints() {
        _uiState.update { state ->
            val currencyGroups = state.accounts.groupBy { it.currency }
            val newAccounts = state.accounts.map { row ->
                val currencyAccounts = currencyGroups[row.currency] ?: emptyList()
                val accountsWithAmount = currencyAccounts.filter { it.isAmountSet && it.isAmountValid }

                val hint = if (!row.isAmountSet) {
                    val balance = accountsWithAmount.sumOf { it.amount?.toDouble() ?: 0.0 }
                    if (kotlin.math.abs(balance) < 0.005) {
                        "0"
                    } else {
                        currencyFormatter.formatNumber(-balance.toFloat())
                    }
                } else {
                    null
                }

                row.copy(amountHint = hint)
            }
            state.copy(accounts = newAccounts)
        }
    }

    private fun updateCurrency(rowId: Int, currency: String) {
        _uiState.update { state ->
            state.copy(
                accounts = state.accounts.map { row ->
                    if (row.id == rowId) row.copy(currency = currency) else row
                },
                showCurrencySelector = false,
                currencySelectorRowId = null
            )
        }
        recalculateAmountHints()
    }

    private fun updateAccountComment(rowId: Int, comment: String) {
        _uiState.update { state ->
            state.copy(
                accounts = state.accounts.map { row ->
                    if (row.id == rowId) row.copy(comment = comment) else row
                }
            )
        }
    }

    private fun addAccountRow(afterRowId: Int?) {
        val defaultCurrency = _uiState.value.defaultCurrency
        _uiState.update { state ->
            val newRow = TransactionAccountRow(id = rowIdGenerator.nextId(), currency = defaultCurrency)
            val newAccounts = if (afterRowId != null) {
                val index = state.accounts.indexOfFirst { it.id == afterRowId }
                if (index >= 0) {
                    state.accounts.toMutableList().apply {
                        add(index + 1, newRow)
                    }
                } else {
                    state.accounts + newRow
                }
            } else {
                state.accounts + newRow
            }
            state.copy(accounts = updateLastFlags(newAccounts))
        }
    }

    private fun removeAccountRow(rowId: Int) {
        _uiState.update { state ->
            if (state.accounts.size <= 2) return@update state
            val newAccounts = state.accounts.filter { it.id != rowId }
            state.copy(accounts = updateLastFlags(newAccounts))
        }
        ensureMinimumRows()
    }

    private fun moveAccountRow(fromIndex: Int, toIndex: Int) {
        _uiState.update { state ->
            val accounts = state.accounts.toMutableList()
            if (fromIndex in accounts.indices && toIndex in accounts.indices) {
                val item = accounts.removeAt(fromIndex)
                accounts.add(toIndex, item)
            }
            state.copy(accounts = updateLastFlags(accounts))
        }
    }

    private fun updateLastFlags(accounts: List<TransactionAccountRow>): List<TransactionAccountRow> =
        accounts.mapIndexed { index, row ->
            row.copy(isLast = index == accounts.lastIndex)
        }

    private fun ensureMinimumRows() {
        _uiState.update { state ->
            val defaultCurrency = state.defaultCurrency
            val minRows = 2
            if (state.accounts.size < minRows) {
                val additionalRows = (state.accounts.size until minRows).map {
                    TransactionAccountRow(id = rowIdGenerator.nextId(), currency = defaultCurrency)
                }
                state.copy(accounts = updateLastFlags(state.accounts + additionalRows))
            } else {
                state
            }
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

    private fun addCurrency(name: String, position: net.ktnx.mobileledger.domain.model.CurrencyPosition, gap: Boolean) {
        viewModelScope.launch {
            val currency = net.ktnx.mobileledger.domain.model.Currency(
                name = name,
                position = position,
                hasGap = gap
            )
            currencyRepository.saveCurrency(currency)
            loadCurrencies()
        }
    }

    private fun deleteCurrency(name: String) {
        viewModelScope.launch {
            currencyRepository.deleteCurrencyByName(name)
            loadCurrencies()
        }
    }

    private fun toggleCurrency() {
        val profile = profileRepository.currentProfile.value ?: return
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
        val profile = profileRepository.currentProfile.value
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

        val accounts = if (rows.size >= 2) {
            rows
        } else {
            rows + List(2 - rows.size) {
                TransactionAccountRow(id = rowIdGenerator.nextId(), currency = defaultCurrency)
            }
        }

        _uiState.update { state ->
            state.copy(accounts = updateLastFlags(accounts))
        }
        recalculateAmountHints()
    }
}
