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
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.withContext
import logcat.LogPriority
import logcat.asLog
import logcat.logcat
import net.ktnx.mobileledger.data.repository.AccountRepository
import net.ktnx.mobileledger.data.repository.CurrencyRepository
import net.ktnx.mobileledger.data.repository.ProfileRepository
import net.ktnx.mobileledger.data.repository.TemplateRepository
import net.ktnx.mobileledger.data.repository.TransactionRepository
import net.ktnx.mobileledger.db.TemplateAccount
import net.ktnx.mobileledger.db.TemplateHeader
import net.ktnx.mobileledger.db.TemplateWithAccounts
import net.ktnx.mobileledger.domain.usecase.TransactionSender
import net.ktnx.mobileledger.model.Currency
import net.ktnx.mobileledger.model.FutureDates
import net.ktnx.mobileledger.model.LedgerTransaction
import net.ktnx.mobileledger.model.LedgerTransactionAccount
import net.ktnx.mobileledger.model.MatchedTemplate
import net.ktnx.mobileledger.service.AppStateService
import net.ktnx.mobileledger.service.CurrencyFormatter
import net.ktnx.mobileledger.utils.SimpleDate

@HiltViewModel
class NewTransactionViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val templateRepository: TemplateRepository,
    private val currencyRepository: CurrencyRepository,
    private val currencyFormatter: CurrencyFormatter,
    private val appStateService: AppStateService,
    private val transactionSender: TransactionSender
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewTransactionUiState())
    val uiState: StateFlow<NewTransactionUiState> = _uiState.asStateFlow()

    private val _effects = Channel<NewTransactionEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    // Job for account suggestion lookup (for cancellation during rapid input)
    private var accountSuggestionJob: Job? = null

    init {
        initializeFromProfile()
    }

    private fun initializeFromProfile() {
        val profile = profileRepository.currentProfile.value
        if (profile != null) {
            val defaultCurrency = profile.getDefaultCommodityOrEmpty()
            val futureDates = FutureDates.valueOf(profile.futureDates)
            _uiState.update {
                it.copy(
                    profileId = profile.id,
                    showCurrency = profile.showCommodityByDefault,
                    futureDates = futureDates,
                    accounts = listOf(
                        TransactionAccountRow(id = NewTransactionUiState.nextId(), currency = defaultCurrency),
                        TransactionAccountRow(id = NewTransactionUiState.nextId(), currency = defaultCurrency)
                    )
                )
            }
            recalculateAmountHints()
            loadCurrencies()
        }
    }

    private fun loadCurrencies() {
        viewModelScope.launch {
            val currencies = currencyRepository.getAllCurrenciesSync().map { it.name }
            _uiState.update { it.copy(availableCurrencies = currencies) }
        }
    }

    fun onEvent(event: NewTransactionEvent) {
        when (event) {
            is NewTransactionEvent.UpdateDate -> updateDate(event.date)

            is NewTransactionEvent.UpdateDescription -> updateDescription(event.description)

            is NewTransactionEvent.UpdateTransactionComment -> updateTransactionComment(event.comment)

            NewTransactionEvent.ShowDatePicker -> showDatePicker()

            NewTransactionEvent.DismissDatePicker -> dismissDatePicker()

            is NewTransactionEvent.UpdateAccountName -> updateAccountName(
                event.rowId,
                event.name
            )

            is NewTransactionEvent.UpdateAmount -> updateAmount(event.rowId, event.amount)

            is NewTransactionEvent.UpdateCurrency -> updateCurrency(event.rowId, event.currency)

            is NewTransactionEvent.UpdateAccountComment -> updateAccountComment(
                event.rowId,
                event.comment
            )

            is NewTransactionEvent.AddAccountRow -> addAccountRow(event.afterRowId)

            is NewTransactionEvent.RemoveAccountRow -> removeAccountRow(event.rowId)

            is NewTransactionEvent.MoveAccountRow -> moveAccountRow(event.fromIndex, event.toIndex)

            is NewTransactionEvent.NoteFocus -> noteFocus(event.rowId, event.element)

            is NewTransactionEvent.ShowCurrencySelector -> showCurrencySelector(event.rowId)

            NewTransactionEvent.DismissCurrencySelector -> dismissCurrencySelector()

            is NewTransactionEvent.AddCurrency -> addCurrency(event.name, event.position, event.gap)

            is NewTransactionEvent.DeleteCurrency -> deleteCurrency(event.name)

            NewTransactionEvent.ShowTemplateSelector -> showTemplateSelector()

            NewTransactionEvent.DismissTemplateSelector -> dismissTemplateSelector()

            is NewTransactionEvent.ApplyTemplate -> applyTemplate(event.templateId)

            is NewTransactionEvent.ApplyTemplateFromQr -> applyTemplateFromQr(event.qrText)

            NewTransactionEvent.ToggleCurrency -> toggleCurrency()

            NewTransactionEvent.ToggleTransactionComment -> toggleTransactionComment()

            is NewTransactionEvent.ToggleAccountComment -> toggleAccountComment(event.rowId)

            NewTransactionEvent.ToggleSimulateSave -> toggleSimulateSave()

            NewTransactionEvent.Submit -> submit()

            NewTransactionEvent.Reset -> reset()

            is NewTransactionEvent.LoadFromTransaction -> loadFromTransaction(event.transactionId)

            is NewTransactionEvent.LoadFromDescription -> loadFromDescription(event.description)

            NewTransactionEvent.NavigateBack -> handleNavigateBack()

            NewTransactionEvent.ConfirmDiscardChanges -> confirmDiscardChanges()

            NewTransactionEvent.ClearError -> clearError()
        }
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

        // Cancel previous suggestion job to prevent race conditions during rapid input
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
            // Small delay to debounce rapid input
            delay(50)

            val profileId = _uiState.value.profileId
            logcat { "profileId=$profileId" }

            if (profileId == null) {
                logcat { "profileId is null, returning" }
                return@launch
            }

            val termUpper = term.uppercase()
            logcat { "querying DB: profileId=$profileId, term='$termUpper'" }
            val suggestions = accountRepository.searchAccountNamesSync(profileId, termUpper)

            // Only update if this job is still active (not cancelled by a newer input)
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

    fun setProfile(profileId: Long) {
        logcat { "setProfile: profileId=$profileId" }
        _uiState.update { it.copy(profileId = profileId) }
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
        val defaultCurrency = profileRepository.currentProfile.value?.getDefaultCommodityOrEmpty() ?: ""
        _uiState.update { state ->
            val newRow = TransactionAccountRow(currency = defaultCurrency)
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
        accounts.mapIndexed {
                index,
                row
            ->
            row.copy(isLast = index == accounts.lastIndex)
        }

    private fun ensureMinimumRows() {
        _uiState.update { state ->
            val defaultCurrency = profileRepository.currentProfile.value?.getDefaultCommodityOrEmpty() ?: ""
            val minRows = 2
            if (state.accounts.size < minRows) {
                val additionalRows = (state.accounts.size until minRows).map {
                    TransactionAccountRow(currency = defaultCurrency)
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

    private fun addCurrency(name: String, position: Currency.Position, gap: Boolean) {
        viewModelScope.launch {
            val currency = net.ktnx.mobileledger.db.Currency()
            currency.name = name
            currency.position = position.toString()
            currency.hasGap = gap
            currencyRepository.insertCurrency(currency)
            loadCurrencies()
        }
    }

    private fun deleteCurrency(name: String) {
        viewModelScope.launch {
            val currency = currencyRepository.getCurrencyByNameSync(name)
            if (currency != null) {
                currencyRepository.deleteCurrency(currency)
            }
            loadCurrencies()
        }
    }

    private fun showTemplateSelector() {
        viewModelScope.launch {
            val templates = templateRepository.getAllTemplatesWithAccountsSync()
                .map { TemplateItem(it.header.id, it.header.name, null, it.header.regularExpression) }
            _uiState.update {
                it.copy(
                    showTemplateSelector = true,
                    availableTemplates = templates
                )
            }
        }
    }

    private fun dismissTemplateSelector() {
        _uiState.update { it.copy(showTemplateSelector = false) }
    }

    private fun applyTemplate(templateId: Long) {
        viewModelScope.launch {
            val template = templateRepository.getTemplateWithAccountsSync(templateId)
            if (template != null) {
                applyTemplateWithAccounts(template)
            }
            _uiState.update { it.copy(showTemplateSelector = false) }
        }
    }

    private suspend fun applyTemplateWithAccounts(template: TemplateWithAccounts) {
        val defaultCurrency = profileRepository.currentProfile.value?.getDefaultCommodityOrEmpty() ?: ""

        val newAccounts = template.accounts.map { acc ->
            val currencyName = acc.currency?.let { currencyId ->
                currencyRepository.getCurrencyByIdSync(currencyId)?.name
            } ?: defaultCurrency
            TransactionAccountRow(
                accountName = acc.accountName ?: "",
                amountText = if (acc.amount != null) currencyFormatter.formatNumber(acc.amount!!) else "",
                currency = currencyName,
                comment = acc.accountComment ?: "",
                isAmountValid = true
            )
        }

        _uiState.update { state ->
            state.copy(
                description = template.header.transactionDescription ?: state.description,
                transactionComment = template.header.transactionComment ?: state.transactionComment,
                accounts = updateLastFlags(
                    if (newAccounts.size >= 2) {
                        newAccounts
                    } else {
                        newAccounts + listOf(
                            TransactionAccountRow(currency = defaultCurrency),
                            TransactionAccountRow(currency = defaultCurrency)
                        ).take(2 - newAccounts.size)
                    }
                )
            )
        }
        recalculateAmountHints()
    }

    fun applyTemplateFromQr(qrText: String) {
        viewModelScope.launch {
            val matched = withContext(Dispatchers.IO) {
                findMatchingTemplate(qrText)
            }
            if (matched != null) {
                applyMatchedTemplate(matched)
            }
        }
    }

    private suspend fun findMatchingTemplate(text: String): MatchedTemplate? {
        val templates = templateRepository.getAllTemplatesWithAccountsSync()
        for (twa in templates) {
            val header = twa.header
            val regex = header.regularExpression ?: continue
            try {
                val pattern = Regex(regex)
                val matchResult = pattern.find(text)
                if (matchResult != null) {
                    // Convert kotlin MatchResult to java MatchResult for compatibility
                    val javaPattern = java.util.regex.Pattern.compile(regex)
                    val javaMatcher = javaPattern.matcher(text)
                    if (javaMatcher.find()) {
                        return MatchedTemplate(header, javaMatcher.toMatchResult())
                    }
                }
            } catch (e: Exception) {
                logcat { "Invalid regex in template: $regex - ${e.message}" }
            }
        }
        return null
    }

    private fun applyMatchedTemplate(matched: MatchedTemplate) {
        viewModelScope.launch {
            val template = templateRepository.getTemplateWithAccountsSync(matched.templateHead.id)
                ?: return@launch

            val matchResult = matched.matchResult
            val header = template.header
            val defaultCurrency = profileRepository.currentProfile.value?.getDefaultCommodityOrEmpty() ?: ""

            // Extract header fields from match groups or use static values
            val description = extractFromMatchGroup(
                matchResult,
                header.transactionDescriptionMatchGroup,
                header.transactionDescription
            ) ?: ""

            val comment = extractFromMatchGroup(
                matchResult,
                header.transactionCommentMatchGroup,
                header.transactionComment
            )

            // Extract date from match groups
            val date = extractDate(matchResult, header)

            // Extract account rows from match groups
            val newAccounts = template.accounts.map { acc ->
                extractAccountRow(matchResult, acc, defaultCurrency)
            }

            // Apply to UI state
            _uiState.update { state ->
                state.copy(
                    description = description,
                    transactionComment = comment ?: state.transactionComment,
                    date = date ?: state.date,
                    accounts = updateLastFlags(ensureMinimumAccounts(newAccounts, defaultCurrency))
                )
            }
            recalculateAmountHints()
        }
    }

    // Delegate to TemplateMatchGroupExtractor for testability
    private fun extractFromMatchGroup(
        matchResult: java.util.regex.MatchResult,
        groupNumber: Int?,
        fallback: String?
    ): String? = TemplateMatchGroupExtractor.extractFromMatchGroup(matchResult, groupNumber, fallback)

    private fun parseAmount(amountStr: String?, negate: Boolean): Float? =
        TemplateMatchGroupExtractor.parseAmount(amountStr, negate)

    private fun extractDate(matchResult: java.util.regex.MatchResult, header: TemplateHeader): SimpleDate? =
        TemplateMatchGroupExtractor.extractDate(matchResult, header)

    /**
     * Extract an account row from match groups or static values.
     */
    private suspend fun extractAccountRow(
        matchResult: java.util.regex.MatchResult,
        acc: TemplateAccount,
        defaultCurrency: String
    ): TransactionAccountRow {
        // Account name
        val accountName = extractFromMatchGroup(
            matchResult,
            acc.accountNameMatchGroup,
            acc.accountName
        ) ?: ""

        // Amount - extract from group or use static value
        val amountStr = extractFromMatchGroup(
            matchResult,
            acc.amountMatchGroup,
            acc.amount?.toString()
        )
        val amount = parseAmount(amountStr, acc.negateAmount ?: false)
        val amountText = amount?.let { currencyFormatter.formatNumber(it) } ?: ""

        // Currency - from match group or database lookup
        val currencyName = if (acc.currencyMatchGroup != null && acc.currencyMatchGroup!! > 0) {
            extractFromMatchGroup(matchResult, acc.currencyMatchGroup, null)
        } else {
            acc.currency?.let { currencyRepository.getCurrencyByIdSync(it)?.name }
        } ?: defaultCurrency

        // Account comment
        val comment = extractFromMatchGroup(
            matchResult,
            acc.accountCommentMatchGroup,
            acc.accountComment
        ) ?: ""

        return TransactionAccountRow(
            accountName = accountName,
            amountText = amountText,
            currency = currencyName,
            comment = comment,
            isAmountValid = true
        )
    }

    /**
     * Ensure at least 2 account rows exist.
     */
    private fun ensureMinimumAccounts(
        accounts: List<TransactionAccountRow>,
        defaultCurrency: String
    ): List<TransactionAccountRow> = if (accounts.size >= 2) {
        accounts
    } else {
        accounts + List(2 - accounts.size) {
            TransactionAccountRow(currency = defaultCurrency)
        }
    }

    private fun toggleCurrency() {
        val profile = profileRepository.currentProfile.value ?: return
        val newShowCurrency = !_uiState.value.showCurrency
        val defaultCurrency = if (newShowCurrency) profile.getDefaultCommodityOrEmpty() else ""

        _uiState.update { state ->
            state.copy(
                showCurrency = newShowCurrency,
                accounts = state.accounts.map { row ->
                    row.copy(currency = defaultCurrency)
                }
            )
        }
        recalculateAmountHints()
    }

    private fun toggleTransactionComment() {
        val wasExpanded = _uiState.value.isTransactionCommentExpanded
        _uiState.update { it.copy(isTransactionCommentExpanded = !wasExpanded) }

        // If expanding, request focus on the transaction comment field
        if (!wasExpanded) {
            viewModelScope.launch {
                _effects.send(
                    NewTransactionEffect.RequestFocus(null, FocusedElement.TransactionComment)
                )
            }
        }
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

        // If expanding, request focus on the account comment field
        if (!wasExpanded) {
            viewModelScope.launch {
                _effects.send(NewTransactionEffect.RequestFocus(rowId, FocusedElement.AccountComment))
            }
        }
    }

    private fun toggleSimulateSave() {
        _uiState.update { it.copy(isSimulateSave = !it.isSimulateSave) }
    }

    private fun submit() {
        val state = _uiState.value
        if (!state.isSubmittable) return

        val profile = profileRepository.currentProfile.value ?: run {
            viewModelScope.launch {
                _effects.send(NewTransactionEffect.ShowError("プロファイルが選択されていません"))
            }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, isBusy = true) }

        // Build LedgerTransaction from UI state
        val transaction = constructLedgerTransaction()

        viewModelScope.launch {
            _effects.send(NewTransactionEffect.HideKeyboard)

            val result = transactionSender.send(profile, transaction, state.isSimulateSave)
            result.fold(
                onSuccess = {
                    handleTransactionSendSuccess(transaction)
                },
                onFailure = { exception ->
                    handleTransactionSendFailure(exception.message ?: "送信に失敗しました")
                }
            )
        }
    }

    private suspend fun handleTransactionSendSuccess(transaction: LedgerTransaction) {
        try {
            transactionRepository.storeTransaction(transaction.toDBO())
            logcat { "Transaction saved to DB" }
            appStateService.signalDataChanged()
        } catch (e: Exception) {
            logcat { "Failed to save transaction: ${e.message}" }
        }

        _uiState.update { it.copy(isSubmitting = false, isBusy = false) }
        _effects.send(NewTransactionEffect.TransactionSaved)
        reset()
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
            _effects.send(NewTransactionEffect.ShowError(errorMessage))
        }
    }

    private fun constructLedgerTransaction(): LedgerTransaction {
        val state = _uiState.value
        val profile = profileRepository.currentProfile.value!!

        val transaction = LedgerTransaction(
            0,
            state.date,
            state.description,
            profile
        )
        transaction.comment = state.transactionComment.ifBlank { null }

        // Calculate balance for accounts with empty amounts
        val currencyBalances = mutableMapOf<String, Float>()
        val accountsWithEmptyAmount = mutableMapOf<String, MutableList<LedgerTransactionAccount>>()

        for (row in state.accounts) {
            if (row.accountName.isBlank()) continue

            val acc = LedgerTransactionAccount(row.accountName.trim(), row.currency)
            acc.comment = row.comment.ifBlank { null }

            if (row.isAmountSet && row.isAmountValid) {
                val amount = row.amount ?: 0f
                acc.setAmount(amount)
                currencyBalances[row.currency] =
                    (currencyBalances[row.currency] ?: 0f) + amount
            } else {
                accountsWithEmptyAmount.getOrPut(row.currency) { mutableListOf() }.add(acc)
            }

            transaction.addAccount(acc)
        }

        // Fill in missing amounts
        for ((currency, accounts) in accountsWithEmptyAmount) {
            val balance = currencyBalances[currency] ?: 0f
            if (accounts.size == 1) {
                accounts[0].setAmount(-balance)
            }
        }

        return transaction
    }

    private fun reset() {
        NewTransactionUiState.resetIdCounter()
        val profile = profileRepository.currentProfile.value
        val defaultCurrency = profile?.getDefaultCommodityOrEmpty() ?: ""

        _uiState.update {
            NewTransactionUiState(
                showCurrency = profile?.showCommodityByDefault ?: false,
                accounts = listOf(
                    TransactionAccountRow(id = NewTransactionUiState.nextId(), currency = defaultCurrency),
                    TransactionAccountRow(id = NewTransactionUiState.nextId(), currency = defaultCurrency)
                )
            )
        }

        viewModelScope.launch {
            _effects.send(NewTransactionEffect.RequestFocus(null, FocusedElement.Description))
        }
    }

    private fun loadFromTransaction(transactionId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }

            val transactionWithAccounts = transactionRepository.getTransactionByIdSync(transactionId)

            if (transactionWithAccounts != null) {
                val defaultCurrency = profileRepository.currentProfile.value?.getDefaultCommodityOrEmpty() ?: ""
                val accounts = transactionWithAccounts.accounts?.map { acc ->
                    TransactionAccountRow(
                        accountName = acc.accountName,
                        amountText = if (acc.amount != 0f) currencyFormatter.formatNumber(acc.amount) else "",
                        currency = acc.currency.ifEmpty { defaultCurrency },
                        comment = acc.comment ?: "",
                        isAmountValid = true
                    )
                } ?: emptyList()

                _uiState.update { state ->
                    state.copy(
                        description = transactionWithAccounts.transaction.description,
                        transactionComment = transactionWithAccounts.transaction.comment ?: "",
                        accounts = updateLastFlags(
                            if (accounts.size >= 2) {
                                accounts
                            } else {
                                accounts + listOf(
                                    TransactionAccountRow(currency = defaultCurrency)
                                ).take(2 - accounts.size)
                            }
                        ),
                        isBusy = false
                    )
                }
                recalculateAmountHints()
            } else {
                _uiState.update { it.copy(isBusy = false) }
            }
        }
    }

    private fun loadFromDescription(description: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }

            val transactionWithAccounts = transactionRepository.getFirstByDescription(description)

            if (transactionWithAccounts != null) {
                val defaultCurrency = profileRepository.currentProfile.value?.getDefaultCommodityOrEmpty() ?: ""
                val accounts = transactionWithAccounts.accounts?.map { acc ->
                    TransactionAccountRow(
                        accountName = acc.accountName,
                        amountText = if (acc.amount != 0f) currencyFormatter.formatNumber(acc.amount) else "",
                        currency = acc.currency.ifEmpty { defaultCurrency },
                        comment = acc.comment ?: "",
                        isAmountValid = true
                    )
                } ?: emptyList()

                _uiState.update { state ->
                    state.copy(
                        accounts = updateLastFlags(
                            if (accounts.size >= 2) {
                                accounts
                            } else {
                                accounts + listOf(
                                    TransactionAccountRow(currency = defaultCurrency)
                                ).take(2 - accounts.size)
                            }
                        ),
                        isBusy = false
                    )
                }
                recalculateAmountHints()
            } else {
                _uiState.update { it.copy(isBusy = false) }
            }
        }
    }

    private fun handleNavigateBack() {
        if (_uiState.value.hasUnsavedChanges) {
            // Show confirmation dialog - handled by the UI
            // The actual navigation will happen when ConfirmDiscardChanges is triggered
        } else {
            viewModelScope.launch {
                _effects.send(NewTransactionEffect.NavigateBack)
            }
        }
    }

    private fun confirmDiscardChanges() {
        viewModelScope.launch {
            _effects.send(NewTransactionEffect.NavigateBack)
        }
    }

    private fun clearError() {
        _uiState.update { it.copy(submitError = null) }
    }
}
