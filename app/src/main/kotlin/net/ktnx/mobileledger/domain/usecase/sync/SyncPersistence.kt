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

package net.ktnx.mobileledger.domain.usecase.sync

import net.ktnx.mobileledger.core.domain.model.Account
import net.ktnx.mobileledger.core.domain.model.Profile
import net.ktnx.mobileledger.core.domain.model.Transaction

/**
 * Interface for persisting sync data (accounts and transactions) to storage.
 */
interface SyncPersistence {

    /**
     * Saves accounts and transactions to the database.
     * Preserves existing UI state (expanded/collapsed) for accounts.
     *
     * @param profile The profile to save data for
     * @param accounts The list of accounts to save
     * @param transactions The list of transactions to save
     */
    suspend fun saveAccountsAndTransactions(profile: Profile, accounts: List<Account>, transactions: List<Transaction>)
}
