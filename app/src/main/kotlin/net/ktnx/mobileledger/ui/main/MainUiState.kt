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

import java.util.Date

/**
 * Main tab selection for the main screen.
 */
enum class MainTab {
    Accounts,
    Transactions
}

/**
 * UI state for the main screen containing navigation, profile, and background task information.
 */
data class MainUiState(
    val currentProfileId: Long? = null,
    val currentProfileName: String = "",
    val currentProfileTheme: Int = -1,
    val currentProfileCanPost: Boolean = false,
    val profiles: List<ProfileListItem> = emptyList(),
    val selectedTab: MainTab = MainTab.Accounts,
    val isDrawerOpen: Boolean = false,
    val isRefreshing: Boolean = false,
    val lastUpdateDate: Date? = null,
    val lastUpdateTransactionCount: Int = 0,
    val lastUpdateAccountCount: Int = 0,
    val backgroundTaskProgress: Float = 0f,
    val backgroundTasksRunning: Boolean = false,
    val updateError: String? = null
)

/**
 * Simplified profile item for display in the navigation drawer.
 */
data class ProfileListItem(val id: Long, val name: String, val theme: Int, val canPost: Boolean)

/**
 * One-shot effects for the main screen.
 */
sealed class MainEffect {
    data class NavigateToNewTransaction(val profileId: Long, val theme: Int) : MainEffect()
    data class NavigateToProfileDetail(val profileId: Long?) : MainEffect()
    data object NavigateToTemplates : MainEffect()
    data object NavigateToBackups : MainEffect()
    data class ShowError(val message: String) : MainEffect()
}

/**
 * Events from the main screen.
 */
sealed class MainEvent {
    data class SelectTab(val tab: MainTab) : MainEvent()
    data class SelectProfile(val profileId: Long) : MainEvent()
    data object OpenDrawer : MainEvent()
    data object CloseDrawer : MainEvent()
    data object RefreshData : MainEvent()
    data object CancelRefresh : MainEvent()
    data object AddNewTransaction : MainEvent()
    data class EditProfile(val profileId: Long) : MainEvent()
    data object CreateNewProfile : MainEvent()
    data object NavigateToTemplates : MainEvent()
    data object NavigateToBackups : MainEvent()
    data object ClearUpdateError : MainEvent()
}
