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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.ktnx.mobileledger.async.TransactionAccumulator
import net.ktnx.mobileledger.data.repository.AccountRepository
import net.ktnx.mobileledger.data.repository.ProfileRepository
import net.ktnx.mobileledger.data.repository.TransactionRepository
import net.ktnx.mobileledger.db.TransactionWithAccounts
import net.ktnx.mobileledger.model.LedgerTransaction
import net.ktnx.mobileledger.model.TransactionListItem
import net.ktnx.mobileledger.service.CurrencyFormatter
import net.ktnx.mobileledger.utils.Logger
import net.ktnx.mobileledger.utils.SimpleDate

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
    private val profileRepository: ProfileRepository,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val currencyFormatter: CurrencyFormatter
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionListUiState())
    val uiState: StateFlow<TransactionListUiState> = _uiState.asStateFlow()

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
            profileRepository.currentProfile.collect { profile ->
                val profileId = profile?.id
                // Only clear transactions when profile actually changes, not on initial subscription
                if (lastProfileId != null && lastProfileId != profileId) {
                    _uiState.update {
                        it.copy(
                            transactions = persistentListOf(),
                            isLoading = false,
                            error = null,
                            firstTransactionDate = null,
                            lastTransactionDate = null,
                            accountFilter = null // Also clear filter on profile change
                        )
                    }
                }
                lastProfileId = profileId
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
     * This is called when the Transactions tab is selected.
     */
    fun loadTransactions() {
        val profileId = profileRepository.currentProfile.value?.id ?: return
        val accountFilter = _uiState.value.accountFilter

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val dbTransactions = transactionRepository.getTransactionsFiltered(
                    profileId,
                    accountFilter
                ).first()

                // Convert directly to display items without TransactionAccumulator
                // (which has dependency on App.instance)
                val displayItems = convertToDisplayItems(dbTransactions, accountFilter)
                updateDisplayedTransactionsDirectly(displayItems)
            } catch (e: Exception) {
                Logger.debug("TransactionListViewModel", "Error loading transactions", e)
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
     * Convert database transactions to display items without using TransactionAccumulator.
     */
    private fun convertToDisplayItems(
        transactions: List<TransactionWithAccounts>,
        accountFilter: String?
    ): List<TransactionListDisplayItem> {
        val items = mutableListOf<TransactionListDisplayItem>()
        items.add(TransactionListDisplayItem.Header)

        // Sort by date (newest first)
        val sortedTx = transactions.sortedByDescending { tx ->
            SimpleDate(tx.transaction.year, tx.transaction.month, tx.transaction.day)
        }

        var lastDate: SimpleDate? = null
        for (tx in sortedTx) {
            val date = SimpleDate(tx.transaction.year, tx.transaction.month, tx.transaction.day)

            // Add date delimiter if date changed
            if (lastDate != null && date != lastDate) {
                val isMonthShown = date.month != lastDate.month || date.year != lastDate.year
                items.add(TransactionListDisplayItem.DateDelimiter(lastDate, isMonthShown))
            }

            // Add transaction
            items.add(
                TransactionListDisplayItem.Transaction(
                    id = tx.transaction.ledgerId,
                    date = date,
                    description = tx.transaction.description ?: "",
                    comment = tx.transaction.comment,
                    accounts = tx.accounts.map { acc ->
                        TransactionAccountDisplayItem(
                            accountName = acc.accountName,
                            amount = acc.amount,
                            currency = acc.currency ?: "",
                            comment = acc.comment,
                            amountStyle = null
                        )
                    }.toImmutableList(),
                    boldAccountName = accountFilter,
                    runningTotal = null
                )
            )

            lastDate = date
        }

        // Add final date delimiter
        lastDate?.let { last ->
            items.add(TransactionListDisplayItem.DateDelimiter(last, isMonthShown = true))
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
        val profileId = profileRepository.currentProfile.value?.id ?: return
        viewModelScope.launch {
            val suggestions = accountRepository.searchAccountNamesSync(profileId, query).take(10)
            _uiState.update {
                it.copy(accountSuggestions = suggestions.toImmutableList())
            }
        }
    }

    private fun updateDisplayedTransactions(items: List<TransactionListItem>) {
        val displayItems = items.map { item ->
            when (item.type) {
                TransactionListItem.Type.HEADER -> TransactionListDisplayItem.Header

                TransactionListItem.Type.DELIMITER -> TransactionListDisplayItem.DateDelimiter(
                    date = item.date,
                    isMonthShown = item.isMonthShown
                )

                TransactionListItem.Type.TRANSACTION -> {
                    val transaction = item.getTransaction()
                    TransactionListDisplayItem.Transaction(
                        id = transaction.ledgerId,
                        date = transaction.requireDate(),
                        description = transaction.description ?: "",
                        comment = transaction.comment,
                        accounts = transaction.accounts.map { acc ->
                            TransactionAccountDisplayItem(
                                accountName = acc.accountName,
                                amount = if (acc.isAmountSet) acc.amount else 0f,
                                currency = acc.currency ?: "",
                                comment = acc.comment,
                                amountStyle = acc.amountStyle
                            )
                        }.toImmutableList(),
                        boldAccountName = item.boldAccountName,
                        runningTotal = item.runningTotal
                    )
                }
            }
        }

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
    fun updateDisplayedTransactionsFromWeb(list: List<LedgerTransaction>) {
        // Cancel any previously running filter job
        displayedTransactionsFilterJob?.cancel()

        displayedTransactionsFilterJob = viewModelScope.launch {
            Logger.debug(
                "dFilter",
                "entered coroutine (about to examine ${list.size} transactions)"
            )
            val accNameFilter = _uiState.value.accountFilter

            val acc = TransactionAccumulator(accNameFilter, accNameFilter, currencyFormatter)
            for (tr in list) {
                ensureActive() // Check for cancellation instead of isInterrupted

                if (accNameFilter == null || tr.hasAccountNamedLike(accNameFilter)) {
                    tr.date?.let { date -> acc.put(tr, date) }
                }
            }

            ensureActive() // Check for cancellation before updating UI

            val items = acc.getItems()
            updateDisplayedTransactions(items)
            Logger.debug("dFilter", "transaction list updated")
        }
    }
}
