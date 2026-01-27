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

package net.ktnx.mobileledger.domain.usecase

import javax.inject.Inject
import net.ktnx.mobileledger.core.domain.repository.AccountRepository

/**
 * Implementation of AccountSuggestionLookup.
 *
 * Searches account names using the AccountRepository with case-insensitive matching.
 */
class AccountSuggestionLookupImpl @Inject constructor(
    private val accountRepository: AccountRepository
) : AccountSuggestionLookup {

    override suspend fun search(profileId: Long, term: String): List<String> {
        if (!isTermValid(term)) {
            return emptyList()
        }
        val termUpper = term.uppercase()
        return accountRepository.searchAccountNames(profileId, termUpper).getOrElse { emptyList() }
    }

    override fun isTermValid(term: String): Boolean = term.length >= AccountSuggestionLookup.DEFAULT_MIN_TERM_LENGTH
}
