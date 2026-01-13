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

package net.ktnx.mobileledger.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Date
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import net.ktnx.mobileledger.App
import net.ktnx.mobileledger.async.RetrieveTransactionsTask
import net.ktnx.mobileledger.async.TransactionAccumulator
import net.ktnx.mobileledger.data.repository.AccountRepository
import net.ktnx.mobileledger.data.repository.OptionRepository
import net.ktnx.mobileledger.data.repository.ProfileRepository
import net.ktnx.mobileledger.data.repository.TransactionRepository
import net.ktnx.mobileledger.db.Profile
import net.ktnx.mobileledger.model.LedgerAccount
import net.ktnx.mobileledger.model.LedgerTransaction
import net.ktnx.mobileledger.model.TransactionListItem
import net.ktnx.mobileledger.service.AppStateService
import net.ktnx.mobileledger.service.BackgroundTaskManager
import net.ktnx.mobileledger.service.SyncInfo
import net.ktnx.mobileledger.service.TaskProgress
import net.ktnx.mobileledger.service.TaskState
import net.ktnx.mobileledger.utils.Logger
import net.ktnx.mobileledger.utils.SimpleDate

/**
 * ViewModel for the main screen using StateFlow for Compose compatibility.
 * This replaces the LiveData-based MainModel for Compose UI.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val optionRepository: OptionRepository,
    private val backgroundTaskManager: BackgroundTaskManager,
    private val appStateService: AppStateService
) : ViewModel() {

    // Profile state from ProfileRepository (replaces Data.observeProfile/Data.profiles)
    val currentProfile: StateFlow<Profile?> = profileRepository.currentProfile

    val allProfiles: StateFlow<List<Profile>> = profileRepository.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Task manager state - exposed from BackgroundTaskManager (T026)
    val isTaskRunning: StateFlow<Boolean> = backgroundTaskManager.isRunning

    val taskProgress: StateFlow<TaskProgress?> = backgroundTaskManager.progress

    // Sync info from AppStateService (T029)
    val lastSyncInfo: StateFlow<SyncInfo?> = appStateService.lastSyncInfo

    // Drawer state from AppStateService (T041)
    val drawerOpen: StateFlow<Boolean> = appStateService.drawerOpen

    // Data version from AppStateService - increments when local data changes
    val dataVersion: StateFlow<Long> = appStateService.dataVersion

    private val _mainUiState = MutableStateFlow(MainUiState())
    val mainUiState: StateFlow<MainUiState> = _mainUiState.asStateFlow()

    private val _accountSummaryUiState = MutableStateFlow(AccountSummaryUiState())
    val accountSummaryUiState: StateFlow<AccountSummaryUiState> = _accountSummaryUiState.asStateFlow()

    private val _transactionListUiState = MutableStateFlow(TransactionListUiState())
    val transactionListUiState: StateFlow<TransactionListUiState> = _transactionListUiState.asStateFlow()

    private val _accountSearchQuery = MutableStateFlow("")

    private val _effects = Channel<MainEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val retrieveTransactionsTask = AtomicReference<RetrieveTransactionsTask?>(null)
    private var displayedTransactionsUpdater: TransactionsDisplayedFilter? = null

    var firstTransactionDate: SimpleDate? = null
        private set
    var lastTransactionDate: SimpleDate? = null
        private set

    init {
        loadInitialData()
        observeDataChanges()
        observeAccountSearch()
        observeTaskRunning()
    }

    /**
     * Observe the BackgroundTaskManager's isRunning state and update isRefreshing accordingly.
     * This direct observation approach is more robust than the previous 100ms delay job,
     * which could be cancelled and leave isRefreshing stuck at true.
     */
    private fun observeTaskRunning() {
        viewModelScope.launch {
            backgroundTaskManager.isRunning.collect { running ->
                _mainUiState.update {
                    it.copy(
                        backgroundTasksRunning = running,
                        isRefreshing = running
                    )
                }
            }
        }
    }

    private fun loadInitialData() {
        _accountSummaryUiState.update { it.copy(showZeroBalanceAccounts = App.getShowZeroBalanceAccounts()) }
    }

    private fun observeDataChanges() {
        // These will be connected in the Activity/Fragment lifecycle
        // The Compose UI will collect these StateFlows directly
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
                        _transactionListUiState.update {
                            it.copy(accountSuggestions = persistentListOf())
                        }
                    }
                }
        }
    }

    private fun searchAccountNames(query: String) {
        val profileId = _mainUiState.value.currentProfileId ?: return
        viewModelScope.launch {
            val suggestions = accountRepository.searchAccountNamesSync(profileId, query).take(10)
            _transactionListUiState.update {
                it.copy(accountSuggestions = suggestions.toImmutableList())
            }
        }
    }

    private fun onSuggestionSelected(accountName: String) {
        _accountSearchQuery.value = ""
        _transactionListUiState.update {
            it.copy(
                accountFilter = accountName,
                accountSuggestions = persistentListOf()
            )
        }
        reloadTransactions()
    }

    fun onMainEvent(event: MainEvent) {
        when (event) {
            is MainEvent.SelectTab -> selectTab(event.tab)
            is MainEvent.SelectProfile -> selectProfile(event.profileId)
            is MainEvent.OpenDrawer -> openDrawer()
            is MainEvent.CloseDrawer -> closeDrawer()
            is MainEvent.RefreshData -> refreshData()
            is MainEvent.CancelRefresh -> cancelRefresh()
            is MainEvent.AddNewTransaction -> addNewTransaction()
            is MainEvent.EditProfile -> editProfile(event.profileId)
            is MainEvent.CreateNewProfile -> createNewProfile()
            is MainEvent.NavigateToTemplates -> navigateToTemplates()
            is MainEvent.NavigateToBackups -> navigateToBackups()
            is MainEvent.ClearUpdateError -> clearUpdateError()
            is MainEvent.ReorderProfiles -> reorderProfiles(event.orderedProfiles)
        }
    }

    fun onAccountSummaryEvent(event: AccountSummaryEvent) {
        when (event) {
            is AccountSummaryEvent.ToggleZeroBalanceAccounts -> toggleZeroBalanceAccounts()
            is AccountSummaryEvent.ToggleAccountExpanded -> toggleAccountExpanded(event.accountId)
            is AccountSummaryEvent.ToggleAmountsExpanded -> toggleAmountsExpanded(event.accountId)
            is AccountSummaryEvent.ShowAccountTransactions -> showAccountTransactions(event.accountName)
        }
    }

    fun onTransactionListEvent(event: TransactionListEvent) {
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

    // Main screen functions
    private fun selectTab(tab: MainTab) {
        _mainUiState.update { it.copy(selectedTab = tab) }
        when (tab) {
            MainTab.Accounts -> {
                clearAccountFilter()
            }

            MainTab.Transactions -> {
                // Transactions タブが選択され、まだデータがロードされていない場合にロード
                if (_transactionListUiState.value.transactions.isEmpty() &&
                    !_transactionListUiState.value.isLoading
                ) {
                    reloadTransactions()
                }
            }
        }
    }

    private fun selectProfile(profileId: Long) {
        viewModelScope.launch {
            profileRepository.getProfileByIdSync(profileId)?.let { profile ->
                profileRepository.setCurrentProfile(profile)
                closeDrawer()
            }
        }
    }

    private fun openDrawer() {
        _mainUiState.update { it.copy(isDrawerOpen = true) }
        appStateService.setDrawerOpen(true)
    }

    private fun closeDrawer() {
        _mainUiState.update { it.copy(isDrawerOpen = false) }
        appStateService.setDrawerOpen(false)
    }

    // T042: Add toggleDrawer method
    fun toggleDrawer() {
        appStateService.toggleDrawer()
        _mainUiState.update { it.copy(isDrawerOpen = appStateService.drawerOpen.value) }
    }

    private fun refreshData() {
        // Launch in viewModelScope with yield() to allow the current animation frame
        // to complete before starting the sync task. This prevents freeze caused by
        // recomposition during PullToRefreshBox animation.
        viewModelScope.launch {
            yield()
            scheduleTransactionListRetrieval()
        }
    }

    private fun cancelRefresh() {
        stopTransactionsRetrieval()
    }

    private fun addNewTransaction() {
        val state = _mainUiState.value
        if (state.currentProfileId != null && state.currentProfileCanPost) {
            viewModelScope.launch {
                _effects.send(
                    MainEffect.NavigateToNewTransaction(
                        profileId = state.currentProfileId,
                        theme = state.currentProfileTheme
                    )
                )
            }
        }
    }

    private fun editProfile(profileId: Long) {
        viewModelScope.launch {
            _effects.send(MainEffect.NavigateToProfileDetail(profileId))
        }
    }

    private fun createNewProfile() {
        viewModelScope.launch {
            _effects.send(MainEffect.NavigateToProfileDetail(null))
        }
    }

    private fun navigateToTemplates() {
        viewModelScope.launch {
            _effects.send(MainEffect.NavigateToTemplates)
        }
    }

    private fun navigateToBackups() {
        viewModelScope.launch {
            _effects.send(MainEffect.NavigateToBackups)
        }
    }

    private fun clearUpdateError() {
        _mainUiState.update { it.copy(updateError = null) }
    }

    private fun reorderProfiles(orderedProfiles: List<ProfileListItem>) {
        viewModelScope.launch {
            val profiles = orderedProfiles.mapNotNull { item ->
                profileRepository.getProfileByIdSync(item.id)
            }
            profileRepository.updateProfileOrder(profiles)
        }
    }

    // Account summary functions
    private fun toggleZeroBalanceAccounts() {
        val newValue = !_accountSummaryUiState.value.showZeroBalanceAccounts
        _accountSummaryUiState.update { it.copy(showZeroBalanceAccounts = newValue) }
        App.storeShowZeroBalanceAccounts(newValue)
        reloadAccounts()
    }

    private fun toggleAccountExpanded(accountId: Long) {
        _accountSummaryUiState.update { state ->
            val updatedAccounts = state.accounts.map { item ->
                when (item) {
                    is AccountSummaryListItem.Account -> {
                        if (item.id == accountId) {
                            item.copy(isExpanded = !item.isExpanded)
                        } else {
                            item
                        }
                    }

                    else -> item
                }
            }
            state.copy(accounts = updatedAccounts)
        }
    }

    private fun toggleAmountsExpanded(accountId: Long) {
        _accountSummaryUiState.update { state ->
            val updatedAccounts = state.accounts.map { item ->
                when (item) {
                    is AccountSummaryListItem.Account -> {
                        if (item.id == accountId) {
                            item.copy(amountsExpanded = !item.amountsExpanded)
                        } else {
                            item
                        }
                    }

                    else -> item
                }
            }
            state.copy(accounts = updatedAccounts)
        }
    }

    private fun showAccountTransactions(accountName: String) {
        setAccountFilter(accountName)
        selectTab(MainTab.Transactions)
    }

    // Transaction list functions
    private fun setAccountFilter(accountName: String?) {
        _accountSearchQuery.value = accountName ?: ""
        _transactionListUiState.update {
            it.copy(
                accountFilter = accountName,
                showAccountFilterInput = !accountName.isNullOrEmpty()
            )
        }
        reloadTransactions()
    }

    private fun showAccountFilterInput() {
        _transactionListUiState.update { it.copy(showAccountFilterInput = true) }
    }

    private fun hideAccountFilterInput() {
        _transactionListUiState.update { it.copy(showAccountFilterInput = false) }
    }

    private fun clearAccountFilter() {
        _transactionListUiState.update {
            it.copy(
                accountFilter = null,
                showAccountFilterInput = false
            )
        }
        reloadTransactions()
    }

    private fun goToDate(date: SimpleDate) {
        val transactions = _transactionListUiState.value.transactions
        val index = transactions.indexOfFirst { item ->
            when (item) {
                is TransactionListDisplayItem.DateDelimiter -> item.date == date
                is TransactionListDisplayItem.Transaction -> item.date == date
                else -> false
            }
        }
        if (index >= 0) {
            _transactionListUiState.update { it.copy(foundTransactionIndex = index) }
        }
    }

    private fun scrollToTransaction(index: Int) {
        _transactionListUiState.update { it.copy(foundTransactionIndex = index) }
    }

    // Data loading functions
    fun updateProfile(profile: Profile?) {
        // プロファイルは呼び出し元（MainActivityCompose.onProfileChanged）で既に設定済み。
        // ここでは UI 状態のみを更新する。
        // 注: ユーザーがドロワーからプロファイルを選択した場合は selectProfile() が
        // profileRepository.setCurrentProfile() を呼ぶ。
        _mainUiState.update {
            it.copy(
                currentProfileId = profile?.id,
                currentProfileName = profile?.name ?: "",
                currentProfileTheme = profile?.theme ?: -1,
                currentProfileCanPost = profile?.canPost() ?: false
            )
        }
        // Transactions データをクリア（次回タブ選択時に再ロード）
        _transactionListUiState.update {
            it.copy(
                transactions = persistentListOf(),
                isLoading = false
            )
        }
        reloadAccounts()
    }

    fun updateProfiles(profiles: List<Profile>) {
        _mainUiState.update {
            it.copy(
                profiles = profiles.map { profile ->
                    ProfileListItem(
                        id = profile.id,
                        name = profile.name,
                        theme = profile.theme,
                        canPost = profile.canPost()
                    )
                }
            )
        }
    }

    fun updateBackgroundTaskProgress(progress: RetrieveTransactionsTask.Progress?) {
        val progressState = progress?.state
        val isRunning = progress != null &&
            progressState == RetrieveTransactionsTask.ProgressState.RUNNING
        val isFinished = progress != null &&
            progressState == RetrieveTransactionsTask.ProgressState.FINISHED

        _mainUiState.update {
            it.copy(
                backgroundTasksRunning = progress != null &&
                    progressState != RetrieveTransactionsTask.ProgressState.FINISHED,
                isRefreshing = isRunning,
                backgroundTaskProgress = if (isRunning && progress!!.getTotal() > 0) {
                    progress.getProgress().toFloat() / progress.getTotal()
                } else {
                    0f
                }
            )
        }

        // Reload data when refresh finishes
        if (isFinished) {
            reloadAccounts()
            reloadTransactions()
        }
    }

    fun updateLastUpdateInfo(date: Date?, transactionCount: Int?, accountCount: Int?) {
        _mainUiState.update {
            it.copy(
                lastUpdateDate = date,
                lastUpdateTransactionCount = transactionCount ?: 0,
                lastUpdateAccountCount = accountCount ?: 0
            )
        }
    }

    fun updateHeaderTexts(accountsText: String?, transactionsText: String?) {
        if (accountsText != null) {
            _accountSummaryUiState.update { state ->
                val updatedAccounts = state.accounts.map { item ->
                    when (item) {
                        is AccountSummaryListItem.Header -> AccountSummaryListItem.Header(accountsText)
                        else -> item
                    }
                }
                state.copy(accounts = updatedAccounts, headerText = accountsText)
            }
        }
        if (transactionsText != null) {
            _transactionListUiState.update { state ->
                state.copy(headerText = transactionsText)
            }
        }
    }

    private fun reloadAccounts() {
        val profileId = _mainUiState.value.currentProfileId ?: return
        val showZeroBalances = _accountSummaryUiState.value.showZeroBalanceAccounts

        _accountSummaryUiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                // Get total account count for hybrid display (displayed/total)
                val totalCount = accountRepository.getCountForProfile(profileId)

                val dbAccounts = accountRepository.getAllWithAmountsSync(profileId, showZeroBalances)

                // First pass: build LedgerAccount objects and determine hasSubAccounts
                val accMap = HashMap<String, LedgerAccount>()
                for (dbAcc in dbAccounts) {
                    var parent: LedgerAccount? = null
                    val parentName = dbAcc.account.parentName
                    if (parentName != null) {
                        parent = accMap[parentName]
                    }
                    if (parent != null) {
                        parent.hasSubAccounts = true
                    }
                    val account = LedgerAccount.fromDBO(dbAcc, parent)
                    accMap[dbAcc.account.name] = account
                }

                // Second pass: build the display list with correct hasSubAccounts values
                // Include ALL accounts - visibility will be computed in UI based on parent's isExpanded
                val adapterList = mutableListOf<AccountSummaryListItem>()
                val headerText = _accountSummaryUiState.value.headerText.ifEmpty { "----" }
                adapterList.add(AccountSummaryListItem.Header(headerText))

                for (dbAcc in dbAccounts) {
                    val account = accMap[dbAcc.account.name] ?: continue
                    adapterList.add(
                        AccountSummaryListItem.Account(
                            id = dbAcc.account.id,
                            name = account.name,
                            shortName = account.shortName,
                            level = account.level,
                            amounts = (account.getAmounts() ?: emptyList()).map { amount ->
                                AccountAmount(
                                    amount = amount.amount,
                                    currency = amount.currency ?: "",
                                    formattedAmount = amount.toString()
                                )
                            },
                            parentName = dbAcc.account.parentName,
                            hasSubAccounts = account.hasSubAccounts,
                            isExpanded = account.isExpanded,
                            amountsExpanded = account.amountsExpanded
                        )
                    )
                }

                // Filter zero balance accounts if needed
                val filteredList = if (!showZeroBalances) {
                    removeZeroAccounts(adapterList)
                } else {
                    adapterList
                }

                _accountSummaryUiState.update {
                    it.copy(
                        accounts = filteredList,
                        isLoading = false,
                        headerText = headerText
                    )
                }
                // Account counts are now managed via AppStateService.lastSyncInfo
            } catch (e: Exception) {
                Logger.debug("MainViewModel", "Error loading accounts", e)
                _accountSummaryUiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun removeZeroAccounts(list: MutableList<AccountSummaryListItem>): List<AccountSummaryListItem> {
        var removed = true
        var currentList = list.toMutableList()

        while (removed) {
            var last: AccountSummaryListItem? = null
            removed = false
            val newList = mutableListOf<AccountSummaryListItem>()

            for (item in currentList) {
                if (last == null) {
                    last = item
                    continue
                }

                val isHeader = last is AccountSummaryListItem.Header
                val hasNonZeroBalance = last is AccountSummaryListItem.Account && !last.allAmountsAreZero()
                val isParentOfCurrent = last is AccountSummaryListItem.Account &&
                    item is AccountSummaryListItem.Account &&
                    isParentOf(last.name, item.name)
                if (isHeader || hasNonZeroBalance || isParentOfCurrent) {
                    newList.add(last)
                } else {
                    removed = true
                }

                last = item
            }

            if (last != null) {
                if (last is AccountSummaryListItem.Header ||
                    (last is AccountSummaryListItem.Account && !last.allAmountsAreZero())
                ) {
                    newList.add(last)
                } else {
                    removed = true
                }
            }

            currentList = newList
        }

        return currentList
    }

    private fun isParentOf(parentName: String, childName: String) = childName.startsWith("$parentName:")

    private fun reloadTransactions() {
        val profileId = _mainUiState.value.currentProfileId ?: return
        val accountFilter = _transactionListUiState.value.accountFilter

        _transactionListUiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                val dbTransactions = transactionRepository.getTransactionsFiltered(
                    profileId,
                    accountFilter
                ).first()

                val accumulator = TransactionAccumulator(accountFilter, accountFilter)
                for (tr in dbTransactions) {
                    accumulator.put(LedgerTransaction(tr))
                }

                val transactionItems = accumulator.getItems()
                updateDisplayedTransactions(transactionItems, dbTransactions.size)
            } catch (e: Exception) {
                Logger.debug("MainViewModel", "Error loading transactions", e)
                _transactionListUiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun updateDisplayedTransactions(items: List<TransactionListItem>, count: Int) {
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

        firstTransactionDate = first
        lastTransactionDate = last

        val headerText = _transactionListUiState.value.headerText.ifEmpty { "----" }
        _transactionListUiState.update {
            it.copy(
                transactions = displayItems.toImmutableList(),
                isLoading = false,
                firstTransactionDate = first,
                lastTransactionDate = last,
                headerText = headerText
            )
        }
        // Transaction count is now managed via AppStateService.lastSyncInfo
    }

    fun scheduleTransactionListRetrieval() {
        // Check if a task is already running (lock-free)
        if (retrieveTransactionsTask.get() != null) {
            Logger.debug("db", "Ignoring request for transaction retrieval - already active")
            return
        }

        val profile = profileRepository.currentProfile.value
        if (profile == null) {
            Logger.debug("db", "No current profile, skipping transaction retrieval")
            return
        }

        val task = RetrieveTransactionsTask(
            profile,
            accountRepository,
            transactionRepository,
            optionRepository,
            backgroundTaskManager,
            appStateService
        )

        // CAS (Compare-And-Swap) to atomically set the task
        // If another thread set a task first, this will fail and we skip
        if (!retrieveTransactionsTask.compareAndSet(null, task)) {
            Logger.debug("db", "Another task was started concurrently, skipping")
            return
        }

        Logger.debug("db", "Created a background transaction retrieval task")
        task.start()
    }

    fun stopTransactionsRetrieval() {
        val task = retrieveTransactionsTask.get()
        if (task != null) {
            task.interrupt()
        } else {
            backgroundTaskManager.updateProgress(TaskProgress("cancel", TaskState.FINISHED, "Cancelled"))
        }
    }

    fun transactionRetrievalDone() {
        retrieveTransactionsTask.set(null)
    }

    /**
     * Reload data when local changes are detected.
     * Only reloads transactions if the Transactions tab is currently selected
     * to avoid unnecessary work.
     */
    fun reloadDataAfterChange() {
        // Always reload accounts (they appear in the drawer and Accounts tab)
        reloadAccounts()

        // Only reload transactions if currently viewing the Transactions tab
        // Otherwise, they'll be reloaded when the tab is selected (existing logic)
        if (_mainUiState.value.selectedTab == MainTab.Transactions) {
            reloadTransactions()
        } else {
            // Clear transactions so they'll be reloaded when tab is selected
            _transactionListUiState.update {
                it.copy(transactions = persistentListOf())
            }
        }
    }

    @Synchronized
    fun updateDisplayedTransactionsFromWeb(list: List<LedgerTransaction>) {
        displayedTransactionsUpdater?.interrupt()
        displayedTransactionsUpdater = TransactionsDisplayedFilter(this, list)
        displayedTransactionsUpdater?.start()
    }

    private class TransactionsDisplayedFilter(
        private val viewModel: MainViewModel,
        private val list: List<LedgerTransaction>
    ) : Thread() {

        override fun run() {
            Logger.debug(
                "dFilter",
                "entered synchronized block (about to examine ${list.size} transactions)"
            )
            val accNameFilter = viewModel._transactionListUiState.value.accountFilter

            val acc = TransactionAccumulator(accNameFilter, accNameFilter)
            for (tr in list) {
                if (isInterrupted) {
                    return
                }

                if (accNameFilter == null || tr.hasAccountNamedLike(accNameFilter)) {
                    tr.date?.let { date -> acc.put(tr, date) }
                }
            }

            if (isInterrupted) return

            val items = acc.getItems()
            viewModel.updateDisplayedTransactions(items, list.size)
            Logger.debug("dFilter", "transaction list updated")
        }
    }
}
