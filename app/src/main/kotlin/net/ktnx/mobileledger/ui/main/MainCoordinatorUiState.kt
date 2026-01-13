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

import java.util.Date

/**
 * UI state for the MainCoordinatorViewModel.
 *
 * Contains only UI orchestration state:
 * - Tab selection
 * - Drawer state
 * - Refresh state
 * - Current profile info (for FAB enabled state and navigation)
 * - Background task progress (for progress indicator)
 *
 * Domain-specific state (accounts, transactions) is managed by their
 * respective ViewModels (AccountSummaryViewModel, TransactionListViewModel).
 */
data class MainCoordinatorUiState(
    val selectedTab: MainTab = MainTab.Accounts,
    val isDrawerOpen: Boolean = false,
    val isRefreshing: Boolean = false,
    val backgroundTaskProgress: Float = 0f,
    val backgroundTasksRunning: Boolean = false,
    val lastUpdateDate: Date? = null,
    val lastUpdateTransactionCount: Int = 0,
    val lastUpdateAccountCount: Int = 0,
    val updateError: String? = null,
    val currentProfileId: Long? = null,
    val currentProfileTheme: Int = -1,
    val currentProfileCanPost: Boolean = false
)

/**
 * Events from the main screen coordinator.
 */
sealed class MainCoordinatorEvent {
    data class SelectTab(val tab: MainTab) : MainCoordinatorEvent()
    data object OpenDrawer : MainCoordinatorEvent()
    data object CloseDrawer : MainCoordinatorEvent()
    data object RefreshData : MainCoordinatorEvent()
    data object CancelRefresh : MainCoordinatorEvent()
    data object AddNewTransaction : MainCoordinatorEvent()
    data class EditProfile(val profileId: Long) : MainCoordinatorEvent()
    data object CreateNewProfile : MainCoordinatorEvent()
    data object NavigateToTemplates : MainCoordinatorEvent()
    data object NavigateToBackups : MainCoordinatorEvent()
    data object ClearUpdateError : MainCoordinatorEvent()
}

/**
 * One-shot effects for the main screen coordinator.
 */
sealed class MainCoordinatorEffect {
    data class NavigateToNewTransaction(val profileId: Long, val theme: Int) : MainCoordinatorEffect()
    data class NavigateToProfileDetail(val profileId: Long?) : MainCoordinatorEffect()
    data object NavigateToTemplates : MainCoordinatorEffect()
    data object NavigateToBackups : MainCoordinatorEffect()
    data class ShowError(val message: String) : MainCoordinatorEffect()
}
