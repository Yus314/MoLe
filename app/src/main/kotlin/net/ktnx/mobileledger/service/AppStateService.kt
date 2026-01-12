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

package net.ktnx.mobileledger.service

import kotlinx.coroutines.flow.StateFlow

/**
 * UI-level application state management service interface.
 *
 * Manages navigation drawer state and sync information shared across UI.
 * @Singleton scoped to maintain state across Activity transitions.
 *
 * ## Usage in ViewModel
 *
 * ```kotlin
 * @HiltViewModel
 * class MainViewModel @Inject constructor(
 *     private val appStateService: AppStateService
 * ) : ViewModel() {
 *     val lastSyncInfo = appStateService.lastSyncInfo
 *         .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SyncInfo.EMPTY)
 *
 *     val drawerOpen = appStateService.drawerOpen
 *         .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
 *
 *     fun openDrawer() = appStateService.setDrawerOpen(true)
 *     fun closeDrawer() = appStateService.setDrawerOpen(false)
 * }
 * ```
 *
 * ## Usage in Sync Task
 *
 * ```kotlin
 * class RetrieveTransactionsTask @Inject constructor(
 *     private val appStateService: AppStateService
 * ) {
 *     fun onSyncComplete(txCount: Int, accCount: Int, totalAccCount: Int) {
 *         appStateService.updateSyncInfo(
 *             SyncInfo(Date(), txCount, accCount, totalAccCount)
 *         )
 *     }
 * }
 * ```
 */
interface AppStateService {
    /**
     * Last sync information.
     *
     * Returns SyncInfo.EMPTY if no sync has been performed.
     */
    val lastSyncInfo: StateFlow<SyncInfo>

    /**
     * Navigation drawer open/close state.
     */
    val drawerOpen: StateFlow<Boolean>

    /**
     * Update sync information.
     *
     * @param info New sync information
     */
    fun updateSyncInfo(info: SyncInfo)

    /**
     * Clear sync information.
     *
     * Used when switching profiles, etc.
     */
    fun clearSyncInfo()

    /**
     * Set drawer open/close state.
     *
     * @param open true to open, false to close
     */
    fun setDrawerOpen(open: Boolean)

    /**
     * Toggle drawer state.
     */
    fun toggleDrawer()
}
