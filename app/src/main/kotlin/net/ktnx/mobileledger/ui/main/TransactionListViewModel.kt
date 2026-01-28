/*
 * Copyright © 2026 Damyan Ivanov.
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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.asLog
import logcat.logcat
import net.ktnx.mobileledger.async.TransactionAccumulator
import net.ktnx.mobileledger.core.common.utils.SimpleDate
import net.ktnx.mobileledger.core.domain.model.Profile
import net.ktnx.mobileledger.core.domain.model.Transaction
import net.ktnx.mobileledger.domain.usecase.GetTransactionsUseCase
import net.ktnx.mobileledger.domain.usecase.SearchAccountNamesUseCase
import net.ktnx.mobileledger.feature.profile.usecase.ObserveCurrentProfileUseCase
import net.ktnx.mobileledger.feature.transaction.usecase.TransactionListConverter
import net.ktnx.mobileledger.service.CurrencyFormatter

/**
 * ViewModel for the transaction list tab.
 *
 * Manages transaction list display, filtering, and date navigation.
 * Observes ProfileRepository for current profile changes.
 *
 * Target: ~250 lines (single responsibility)
 */
@HiltViewModel
class TransactionListViewModel @Inject constructor(
    private val observeCurrentProfileUseCase: ObserveCurrentProfileUseCase,
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val searchAccountNamesUseCase: SearchAccountNamesUseCase,
    private val currencyFormatter: CurrencyFormatter,
    private val transactionListConverter: TransactionListConverter
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionListUiState())
    val uiState: StateFlow<TransactionListUiState> = _uiState.asStateFlow()

    val currentProfile: StateFlow<Profile?> = observeCurrentProfileUseCase()

    private val _accountSearchQuery = MutableStateFlow("")

    // Job for managing filter task cancellation
    private var displayedTransactionsFilterJob: Job? = null

    init {
        observeProfileChanges()
        observeAccountSearch()
    }

    private var lastProfileId: Long? = null

    private fun observeProfileChanges() {
        viewModelScope.launch {
            currentProfile.collect { profile ->
                val profileId = profile?.id
                // Clear filter and reload when profile changes
                if (lastProfileId != profileId) {
                    if (lastProfileId != null) {
                        // Profile actually changed, clear filter
                        _uiState.update {
                            it.copy(accountFilter = null)
                        }
                    }
                    lastProfileId = profileId
                    // Load transactions for the new profile
                    if (profileId != null) {
                        loadTransactionsInternal(profileId, null)
                    } else {
                        _uiState.update {
                            it.copy(
                                transactions = persistentListOf(),
                                isLoading = false,
                                error = null,
                                firstTransactionDate = null,
                                lastTransactionDate = null
                            )
                        }
                    }
                }
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeAccountSearch() {
        viewModelScope.launch {
            _accountSearchQuery
                .debounce(300)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.isNotEmpty()) {
                        searchAccountNames(query)
                    } else {
                        _uiState.update {
                            it.copy(accountSuggestions = persistentListOf())
                        }
                    }
                }
        }
    }

    fun onEvent(event: TransactionListEvent) {
        when (event) {
            is TransactionListEvent.SetAccountFilter -> setAccountFilter(event.accountName)
            is TransactionListEvent.ShowAccountFilterInput -> showAccountFilterInput()
            is TransactionListEvent.HideAccountFilterInput -> hideAccountFilterInput()
            is TransactionListEvent.ClearAccountFilter -> clearAccountFilter()
            is TransactionListEvent.GoToDate -> goToDate(event.date)
            is TransactionListEvent.ScrollToTransaction -> scrollToTransaction(event.index)
            is TransactionListEvent.SelectSuggestion -> onSuggestionSelected(event.accountName)
        }
    }

    /**
     * Load transactions for the current profile.
     * This is called when the Transactions tab is selected or externally.
     */
    fun loadTransactions() {
        val profileId = currentProfile.value?.id ?: return
        val accountFilter = _uiState.value.accountFilter
        loadTransactionsInternal(profileId, accountFilter)
    }

    /**
     * Internal method to load transactions with specified profile and filter.
     */
    private fun loadTransactionsInternal(profileId: Long, accountFilter: String?) {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val dbTransactions = getTransactionsUseCase(profileId, accountFilter).getOrThrow()

                // Convert directly to display items without TransactionAccumulator
                // (which has dependency on App.instance)
                val displayItems = convertToDisplayItems(dbTransactions, accountFilter)
                updateDisplayedTransactionsDirectly(displayItems)
            } catch (e: Exception) {
                logcat { "Error loading transactions: ${e.asLog()}" }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error"
                    )
                }
            }
        }
    }

    /**
     * Convert domain model transactions to display items using TransactionListConverter.
     */
    private fun convertToDisplayItems(
        transactions: List<Transaction>,
        accountFilter: String?
    ): List<TransactionListDisplayItem> {
        val items = mutableListOf<TransactionListDisplayItem>()
        items.add(TransactionListDisplayItem.Header)

        // Delegate to UseCase for sorting and grouping
        val conversionResult = transactionListConverter.convert(transactions)

        // Convert domain DisplayItems to UI DisplayItems
        for (item in conversionResult.items) {
            when (item) {
                is TransactionListConverter.DisplayItem.TransactionItem -> {
                    val tx = item.transaction
                    items.add(
                        TransactionListDisplayItem.Transaction(
                            id = tx.ledgerId,
                            date = tx.date,
                            description = tx.description,
                            comment = tx.comment,
                            accounts = tx.lines.map { line ->
                                TransactionAccountDisplayItem(
                                    accountName = line.accountName,
                                    amount = line.amount ?: 0f,
                                    currency = line.currency,
                                    comment = line.comment,
                                    amountStyle = null
                                )
                            }.toImmutableList(),
                            boldAccountName = accountFilter,
                            runningTotal = null
                        )
                    )
                }

                is TransactionListConverter.DisplayItem.DateDelimiter -> {
                    items.add(
                        TransactionListDisplayItem.DateDelimiter(
                            date = item.date,
                            isMonthShown = item.isMonthBoundary
                        )
                    )
                }
            }
        }

        return items
    }

    /**
     * Update displayed transactions directly from a list of display items.
     */
    private fun updateDisplayedTransactionsDirectly(displayItems: List<TransactionListDisplayItem>) {
        // Update date range
        var first: SimpleDate? = null
        var last: SimpleDate? = null
        for (item in displayItems) {
            val date = when (item) {
                is TransactionListDisplayItem.Transaction -> item.date
                is TransactionListDisplayItem.DateDelimiter -> item.date
                else -> null
            }
            if (date != null) {
                if (first == null || date < first) first = date
                if (last == null || date > last) last = date
            }
        }

        val headerText = _uiState.value.headerText.ifEmpty { "----" }
        _uiState.update {
            it.copy(
                transactions = displayItems.toImmutableList(),
                isLoading = false,
                error = null,
                firstTransactionDate = first,
                lastTransactionDate = last,
                headerText = headerText
            )
        }
    }

    private fun setAccountFilter(accountName: String?) {
        _accountSearchQuery.value = accountName ?: ""
        _uiState.update {
            it.copy(
                accountFilter = accountName,
                showAccountFilterInput = !accountName.isNullOrEmpty()
            )
        }
        loadTransactions()
    }

    private fun showAccountFilterInput() {
        _uiState.update { it.copy(showAccountFilterInput = true) }
    }

    private fun hideAccountFilterInput() {
        _uiState.update { it.copy(showAccountFilterInput = false) }
    }

    private fun clearAccountFilter() {
        _accountSearchQuery.value = ""
        _uiState.update {
            it.copy(
                accountFilter = null,
                showAccountFilterInput = false,
                accountSuggestions = persistentListOf()
            )
        }
        loadTransactions()
    }

    private fun goToDate(date: SimpleDate) {
        val transactions = _uiState.value.transactions
        val index = transactions.indexOfFirst { item ->
            when (item) {
                is TransactionListDisplayItem.DateDelimiter -> item.date == date
                is TransactionListDisplayItem.Transaction -> item.date == date
                else -> false
            }
        }
        if (index >= 0) {
            _uiState.update { it.copy(foundTransactionIndex = index) }
        }
    }

    private fun scrollToTransaction(index: Int) {
        _uiState.update { it.copy(foundTransactionIndex = index) }
    }

    private fun onSuggestionSelected(accountName: String) {
        _accountSearchQuery.value = ""
        _uiState.update {
            it.copy(
                accountFilter = accountName,
                accountSuggestions = persistentListOf()
            )
        }
        loadTransactions()
    }

    private fun searchAccountNames(query: String) {
        val profileId = currentProfile.value?.id ?: return
        viewModelScope.launch {
            searchAccountNamesUseCase(profileId, query).onSuccess { names ->
                _uiState.update {
                    it.copy(accountSuggestions = names.take(10).toImmutableList())
                }
            }
        }
    }

    /**
     * Update the header text shown in the transaction list.
     */
    fun updateHeaderText(text: String) {
        _uiState.update { it.copy(headerText = text) }
    }

    /**
     * Update displayed transactions from web sync.
     * Used by RetrieveTransactionsTask to show live updates during sync.
     *
     * Uses viewModelScope.launch instead of Thread for proper lifecycle management
     * and deterministic testing with TestDispatcher.
     */
    fun updateDisplayedTransactionsFromWeb(list: List<Transaction>) {
        // Cancel any previously running filter job
        displayedTransactionsFilterJob?.cancel()

        displayedTransactionsFilterJob = viewModelScope.launch {
            logcat { "entered coroutine (about to examine ${list.size} transactions)" }
            val accNameFilter = _uiState.value.accountFilter

            val acc = TransactionAccumulator(accNameFilter, accNameFilter, currencyFormatter)
            for (tr in list) {
                ensureActive() // Check for cancellation instead of isInterrupted

                if (accNameFilter == null || tr.hasAccountNamed(accNameFilter)) {
                    acc.put(tr, tr.date)
                }
            }

            ensureActive() // Check for cancellation before updating UI

            val items = acc.getItems()
            updateDisplayedTransactionsDirectly(items)
            logcat { "transaction list updated" }
        }
    }
}
