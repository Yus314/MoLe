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
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import net.ktnx.mobileledger.async.RetrieveTransactionsTask
import net.ktnx.mobileledger.data.repository.AccountRepository
import net.ktnx.mobileledger.data.repository.OptionRepository
import net.ktnx.mobileledger.data.repository.ProfileRepository
import net.ktnx.mobileledger.data.repository.TransactionRepository
import net.ktnx.mobileledger.db.Profile
import net.ktnx.mobileledger.service.AppStateService
import net.ktnx.mobileledger.service.BackgroundTaskManager
import net.ktnx.mobileledger.service.SyncInfo
import net.ktnx.mobileledger.service.TaskProgress
import net.ktnx.mobileledger.service.TaskState
import net.ktnx.mobileledger.utils.Logger

/**
 * ViewModel for coordinating the main screen UI.
 *
 * Responsibilities:
 * - Tab selection
 * - Drawer state
 * - Refresh orchestration (starting/stopping sync tasks)
 * - Navigation effects
 *
 * Domain-specific logic is delegated to:
 * - ProfileSelectionViewModel: Profile selection and management
 * - AccountSummaryViewModel: Account list display
 * - TransactionListViewModel: Transaction list display
 */
@HiltViewModel
class MainCoordinatorViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val optionRepository: OptionRepository,
    private val backgroundTaskManager: BackgroundTaskManager,
    private val appStateService: AppStateService
) : ViewModel() {

    // Delegate state to BackgroundTaskManager
    val isTaskRunning: StateFlow<Boolean> = backgroundTaskManager.isRunning
    val taskProgress: StateFlow<TaskProgress?> = backgroundTaskManager.progress

    // Delegate state to AppStateService
    val lastSyncInfo: StateFlow<SyncInfo?> = appStateService.lastSyncInfo
    val drawerOpen: StateFlow<Boolean> = appStateService.drawerOpen
    val dataVersion: StateFlow<Long> = appStateService.dataVersion

    private val _uiState = MutableStateFlow(MainCoordinatorUiState())
    val uiState: StateFlow<MainCoordinatorUiState> = _uiState.asStateFlow()

    private val _effects = Channel<MainCoordinatorEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val retrieveTransactionsTask = AtomicReference<RetrieveTransactionsTask?>(null)

    init {
        observeTaskRunning()
        observeProfile()
    }

    private fun observeTaskRunning() {
        viewModelScope.launch {
            backgroundTaskManager.isRunning.collect { running ->
                _uiState.update {
                    it.copy(
                        backgroundTasksRunning = running,
                        isRefreshing = running
                    )
                }
            }
        }
    }

    private fun observeProfile() {
        viewModelScope.launch {
            profileRepository.currentProfile.collect { profile ->
                _uiState.update {
                    it.copy(
                        currentProfileId = profile?.id,
                        currentProfileTheme = profile?.theme ?: -1,
                        currentProfileCanPost = profile?.canPost() ?: false
                    )
                }
            }
        }
    }

    fun onEvent(event: MainCoordinatorEvent) {
        when (event) {
            is MainCoordinatorEvent.SelectTab -> selectTab(event.tab)
            is MainCoordinatorEvent.OpenDrawer -> openDrawer()
            is MainCoordinatorEvent.CloseDrawer -> closeDrawer()
            is MainCoordinatorEvent.RefreshData -> refreshData()
            is MainCoordinatorEvent.CancelRefresh -> cancelRefresh()
            is MainCoordinatorEvent.AddNewTransaction -> addNewTransaction()
            is MainCoordinatorEvent.EditProfile -> editProfile(event.profileId)
            is MainCoordinatorEvent.CreateNewProfile -> createNewProfile()
            is MainCoordinatorEvent.NavigateToTemplates -> navigateToTemplates()
            is MainCoordinatorEvent.NavigateToBackups -> navigateToBackups()
            is MainCoordinatorEvent.ClearUpdateError -> clearUpdateError()
        }
    }

    private fun selectTab(tab: MainTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    private fun openDrawer() {
        _uiState.update { it.copy(isDrawerOpen = true) }
        appStateService.setDrawerOpen(true)
    }

    private fun closeDrawer() {
        _uiState.update { it.copy(isDrawerOpen = false) }
        appStateService.setDrawerOpen(false)
    }

    fun toggleDrawer() {
        appStateService.toggleDrawer()
        _uiState.update { it.copy(isDrawerOpen = appStateService.drawerOpen.value) }
    }

    private fun refreshData() {
        viewModelScope.launch {
            yield()
            scheduleTransactionListRetrieval()
        }
    }

    private fun cancelRefresh() {
        stopTransactionsRetrieval()
    }

    private fun addNewTransaction() {
        val state = _uiState.value
        if (state.currentProfileId != null && state.currentProfileCanPost) {
            viewModelScope.launch {
                _effects.send(
                    MainCoordinatorEffect.NavigateToNewTransaction(
                        profileId = state.currentProfileId,
                        theme = state.currentProfileTheme
                    )
                )
            }
        }
    }

    private fun editProfile(profileId: Long) {
        viewModelScope.launch {
            _effects.send(MainCoordinatorEffect.NavigateToProfileDetail(profileId))
        }
    }

    private fun createNewProfile() {
        viewModelScope.launch {
            _effects.send(MainCoordinatorEffect.NavigateToProfileDetail(null))
        }
    }

    private fun navigateToTemplates() {
        viewModelScope.launch {
            _effects.send(MainCoordinatorEffect.NavigateToTemplates)
        }
    }

    private fun navigateToBackups() {
        viewModelScope.launch {
            _effects.send(MainCoordinatorEffect.NavigateToBackups)
        }
    }

    private fun clearUpdateError() {
        _uiState.update { it.copy(updateError = null) }
    }

    fun scheduleTransactionListRetrieval() {
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

    fun reloadDataAfterChange() {
        appStateService.signalDataChanged()
    }

    fun updateProfile(profile: Profile?) {
        _uiState.update {
            it.copy(
                currentProfileId = profile?.id,
                currentProfileTheme = profile?.theme ?: -1,
                currentProfileCanPost = profile?.canPost() ?: false
            )
        }
    }

    fun updateBackgroundTaskProgress(progress: RetrieveTransactionsTask.Progress?) {
        val progressState = progress?.state
        val isRunning = progress != null &&
            progressState == RetrieveTransactionsTask.ProgressState.RUNNING

        _uiState.update {
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
    }

    fun updateLastUpdateInfo(date: java.util.Date?, transactionCount: Int?, accountCount: Int?) {
        _uiState.update {
            it.copy(
                lastUpdateDate = date,
                lastUpdateTransactionCount = transactionCount ?: 0,
                lastUpdateAccountCount = accountCount ?: 0
            )
        }
    }
}
