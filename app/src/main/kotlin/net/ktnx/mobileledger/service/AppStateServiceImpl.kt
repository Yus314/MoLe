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

import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.ktnx.mobileledger.core.sync.SyncStateNotifier

/**
 * Implementation of [AppStateService] and [SyncStateNotifier].
 *
 * Manages UI-level application state including navigation drawer
 * state and sync information.
 */
@Singleton
class AppStateServiceImpl @Inject constructor() : AppStateService, SyncStateNotifier {

    private val _lastSyncInfo = MutableStateFlow(SyncInfo.EMPTY)
    override val lastSyncInfo: StateFlow<SyncInfo> = _lastSyncInfo.asStateFlow()

    private val _drawerOpen = MutableStateFlow(false)
    override val drawerOpen: StateFlow<Boolean> = _drawerOpen.asStateFlow()

    private val _dataVersion = MutableStateFlow(0L)
    override val dataVersion: StateFlow<Long> = _dataVersion.asStateFlow()

    override fun updateSyncInfo(info: SyncInfo) {
        _lastSyncInfo.value = info
    }

    override fun clearSyncInfo() {
        _lastSyncInfo.value = SyncInfo.EMPTY
    }

    override fun setDrawerOpen(open: Boolean) {
        _drawerOpen.value = open
    }

    override fun toggleDrawer() {
        _drawerOpen.value = !_drawerOpen.value
    }

    override fun signalDataChanged() {
        _dataVersion.value = _dataVersion.value + 1
    }

    // SyncStateNotifier implementation
    override fun notifySyncComplete(date: Date, transactionCount: Int, accountCount: Int) {
        updateSyncInfo(
            SyncInfo(
                date = date,
                transactionCount = transactionCount,
                accountCount = accountCount,
                totalAccountCount = accountCount
            )
        )
    }
}
