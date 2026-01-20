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

/**
 * Interface for looking up account name suggestions.
 *
 * Provides search functionality with minimum term length validation.
 */
interface AccountSuggestionLookup {

    companion object {
        const val DEFAULT_MIN_TERM_LENGTH = 2
        const val DEFAULT_DEBOUNCE_MS = 50L
    }

    /**
     * Search for account name suggestions matching the given term.
     *
     * @param profileId The profile ID to search within
     * @param term The search term (case-insensitive)
     * @return List of matching account names, or empty list if term is too short
     */
    suspend fun search(profileId: Long, term: String): List<String>

    /**
     * Check if the search term meets minimum length requirements.
     *
     * @param term The search term to validate
     * @return true if term is long enough for search
     */
    fun isTermValid(term: String): Boolean
}
