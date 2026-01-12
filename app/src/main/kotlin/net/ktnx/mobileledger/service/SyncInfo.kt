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

/**
 * Data sync result information.
 *
 * @property date Sync completion timestamp
 * @property transactionCount Number of transactions retrieved
 * @property accountCount Number of visible accounts
 * @property totalAccountCount Total number of accounts
 */
data class SyncInfo(val date: Date?, val transactionCount: Int, val accountCount: Int, val totalAccountCount: Int) {
    companion object {
        /**
         * Initial value when no sync has been performed.
         */
        val EMPTY = SyncInfo(
            date = null,
            transactionCount = 0,
            accountCount = 0,
            totalAccountCount = 0
        )
    }

    /**
     * Whether a sync has ever been performed.
     */
    val hasSynced: Boolean
        get() = date != null

    /**
     * Generate summary text for the sync info.
     *
     * @param transactionLabel Label for transactions (e.g., "transactions", "件")
     * @param accountLabel Label for accounts (e.g., "accounts", "アカウント")
     * @return Formatted summary string
     */
    fun formatSummary(transactionLabel: String, accountLabel: String): String =
        "$transactionCount $transactionLabel, $accountCount/$totalAccountCount $accountLabel"
}
