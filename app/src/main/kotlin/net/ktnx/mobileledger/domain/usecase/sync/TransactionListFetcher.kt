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

import net.ktnx.mobileledger.core.domain.model.Profile
import net.ktnx.mobileledger.core.domain.model.Transaction

/**
 * Interface for fetching transaction list from hledger server via JSON API.
 *
 * Returns null if JSON API is not available (e.g., legacy HTML mode configured).
 */
interface TransactionListFetcher {

    /**
     * Fetches transaction list from server using JSON API.
     *
     * @param profile The profile to fetch transactions for
     * @param expectedPostingsCount Expected total postings count for progress calculation
     * @param onProgress Callback for progress updates (current, total)
     * @return List of transactions sorted by date (descending), or null if JSON API is not available
     * @throws Exception on network or parsing errors
     */
    suspend fun fetch(
        profile: Profile,
        expectedPostingsCount: Int,
        onProgress: suspend (Int, Int) -> Unit
    ): List<Transaction>?
}
