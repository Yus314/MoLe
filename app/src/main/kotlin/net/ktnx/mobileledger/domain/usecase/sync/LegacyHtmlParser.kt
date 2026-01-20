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

import net.ktnx.mobileledger.domain.model.Account
import net.ktnx.mobileledger.domain.model.Profile
import net.ktnx.mobileledger.domain.model.Transaction

/**
 * Interface for parsing legacy HTML from hledger-web /journal endpoint.
 *
 * Used as fallback when JSON API is not available.
 */
interface LegacyHtmlParser {

    /**
     * Parses HTML from /journal endpoint to extract accounts and transactions.
     *
     * @param profile The profile to fetch data for
     * @param expectedPostingsCount Expected total postings count for progress calculation
     * @param onProgress Callback for progress updates (current, total)
     * @return Parsed accounts and transactions
     * @throws Exception on network or parsing errors
     */
    suspend fun parse(
        profile: Profile,
        expectedPostingsCount: Int,
        onProgress: suspend (Int, Int) -> Unit
    ): LegacyParseResult
}

/**
 * Result of legacy HTML parsing.
 *
 * @property accounts List of parsed accounts
 * @property transactions List of parsed transactions
 */
data class LegacyParseResult(
    val accounts: List<Account>,
    val transactions: List<Transaction>
)
