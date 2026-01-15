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
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import net.ktnx.mobileledger.data.repository.ProfileRepository
import net.ktnx.mobileledger.db.Profile
import net.ktnx.mobileledger.domain.model.SyncException
import net.ktnx.mobileledger.domain.model.SyncProgress
import net.ktnx.mobileledger.domain.model.SyncState
import net.ktnx.mobileledger.domain.usecase.TransactionSyncer
import net.ktnx.mobileledger.service.AppStateService
import net.ktnx.mobileledger.service.BackgroundTaskManager
import net.ktnx.mobileledger.service.SyncInfo
import net.ktnx.mobileledger.service.TaskProgress
import timber.log.Timber

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
    private val transactionSyncer: TransactionSyncer,
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

    // Sync state management
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    private var syncJob: Job? = null

    init {
        observeSyncState()
        observeProfile()
    }

    /**
     * Observe syncState and update UI state accordingly.
     * This replaces the legacy BackgroundTaskManager observation.
     */
    private fun observeSyncState() {
        viewModelScope.launch {
            syncState.collect { state ->
                _uiState.update { uiState ->
                    when (state) {
                        is SyncState.InProgress -> {
                            val progress = state.progress
                            uiState.copy(
                                backgroundTasksRunning = true,
                                isRefreshing = true,
                                backgroundTaskProgress = when (progress) {
                                    is SyncProgress.Running -> progress.progressFraction
                                    else -> 0f
                                }
                            )
                        }

                        else -> uiState.copy(
                            backgroundTasksRunning = false,
                            isRefreshing = false,
                            backgroundTaskProgress = 0f
                        )
                    }
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
            startSync()
        }
    }

    private fun cancelRefresh() {
        cancelSync()
    }

    /**
     * Start synchronization using TransactionSyncer.
     *
     * @param profile The profile to sync. If null, uses the current profile.
     */
    fun startSync(profile: Profile? = null) {
        val syncProfile = profile ?: profileRepository.currentProfile.value
        if (syncProfile == null) {
            Timber.d("No profile to sync")
            return
        }

        // Cancel any existing sync
        syncJob?.cancel()

        syncJob = viewModelScope.launch {
            _syncState.value = SyncState.InProgress(SyncProgress.Starting("同期開始..."))

            try {
                transactionSyncer.sync(syncProfile)
                    .collect { progress ->
                        _syncState.value = SyncState.InProgress(progress)
                    }

                // Sync completed successfully
                val result = transactionSyncer.getLastResult()
                if (result != null) {
                    _syncState.value = SyncState.Completed(result)
                } else {
                    _syncState.value = SyncState.Idle
                }
            } catch (e: SyncException) {
                _syncState.value = SyncState.Failed(e.syncError)
            } catch (e: kotlinx.coroutines.CancellationException) {
                _syncState.value = SyncState.Cancelled
            } catch (e: Exception) {
                _syncState.value = SyncState.Failed(
                    net.ktnx.mobileledger.domain.model.SyncError.UnknownError(
                        message = e.message ?: "予期しないエラーが発生しました",
                        cause = e
                    )
                )
            }
        }
    }

    /**
     * Cancel the current sync operation.
     */
    fun cancelSync() {
        syncJob?.cancel()
        _syncState.value = SyncState.Cancelled
    }

    fun clearSyncState() {
        _syncState.value = SyncState.Idle
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
