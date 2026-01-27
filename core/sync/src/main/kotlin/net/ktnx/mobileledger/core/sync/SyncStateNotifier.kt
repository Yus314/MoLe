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

package net.ktnx.mobileledger.core.sync

import java.util.Date

/**
 * Interface for notifying UI layer about sync completion.
 *
 * Implemented by app module's AppStateService to update sync information.
 */
interface SyncStateNotifier {

    /**
     * Notify that sync has completed successfully.
     *
     * @param date Sync completion timestamp
     * @param transactionCount Number of transactions synced
     * @param accountCount Number of accounts synced
     */
    fun notifySyncComplete(date: Date, transactionCount: Int, accountCount: Int)
}
