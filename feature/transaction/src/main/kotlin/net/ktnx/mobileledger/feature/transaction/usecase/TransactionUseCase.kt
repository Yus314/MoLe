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

package net.ktnx.mobileledger.feature.transaction.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import net.ktnx.mobileledger.core.domain.model.Transaction
import net.ktnx.mobileledger.core.domain.repository.AccountRepository
import net.ktnx.mobileledger.core.domain.repository.OptionRepository
import net.ktnx.mobileledger.core.domain.repository.TransactionRepository

// ============================================
// Transaction List UseCases
// ============================================

interface ObserveTransactionsUseCase {
    operator fun invoke(profileId: Long, accountFilter: String?): Flow<List<Transaction>>
}

interface GetTransactionsUseCase {
    suspend operator fun invoke(profileId: Long, accountFilter: String?): Result<List<Transaction>>
}

interface SearchAccountNamesUseCase {
    suspend operator fun invoke(profileId: Long, term: String): Result<List<String>>
}

class ObserveTransactionsUseCaseImpl @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ObserveTransactionsUseCase {
    override fun invoke(profileId: Long, accountFilter: String?): Flow<List<Transaction>> =
        transactionRepository.observeTransactionsFiltered(profileId, accountFilter)
}

class GetTransactionsUseCaseImpl @Inject constructor(
    private val observeTransactionsUseCase: ObserveTransactionsUseCase
) : GetTransactionsUseCase {
    override suspend fun invoke(profileId: Long, accountFilter: String?): Result<List<Transaction>> = runCatching {
        observeTransactionsUseCase(profileId, accountFilter).first()
    }
}

class SearchAccountNamesUseCaseImpl @Inject constructor(
    private val accountRepository: AccountRepository
) : SearchAccountNamesUseCase {
    override suspend fun invoke(profileId: Long, term: String): Result<List<String>> =
        accountRepository.searchAccountNames(profileId, term)
}

// ============================================
// Transaction Entry UseCases
// ============================================

/**
 * Search transaction descriptions for autocomplete suggestions.
 */
interface SearchTransactionDescriptionsUseCase {
    suspend operator fun invoke(term: String): Result<List<String>>
}

/**
 * Persist a single transaction for a given profile.
 */
interface StoreTransactionUseCase {
    suspend operator fun invoke(transaction: Transaction, profileId: Long): Result<Unit>
}

interface GetTransactionByIdUseCase {
    suspend operator fun invoke(transactionId: Long): Result<Transaction?>
}

interface GetFirstTransactionByDescriptionUseCase {
    suspend operator fun invoke(description: String): Result<Transaction?>
}

class SearchTransactionDescriptionsUseCaseImpl @Inject constructor(
    private val transactionRepository: TransactionRepository
) : SearchTransactionDescriptionsUseCase {
    override suspend fun invoke(term: String): Result<List<String>> = transactionRepository.searchByDescription(term)
}

class StoreTransactionUseCaseImpl @Inject constructor(
    private val transactionRepository: TransactionRepository
) : StoreTransactionUseCase {
    override suspend fun invoke(transaction: Transaction, profileId: Long): Result<Unit> =
        transactionRepository.storeTransaction(transaction, profileId)
}

class GetTransactionByIdUseCaseImpl @Inject constructor(
    private val transactionRepository: TransactionRepository
) : GetTransactionByIdUseCase {
    override suspend fun invoke(transactionId: Long): Result<Transaction?> =
        transactionRepository.getTransactionById(transactionId)
}

class GetFirstTransactionByDescriptionUseCaseImpl @Inject constructor(
    private val transactionRepository: TransactionRepository
) : GetFirstTransactionByDescriptionUseCase {
    override suspend fun invoke(description: String): Result<Transaction?> =
        transactionRepository.getFirstByDescription(description)
}

// ============================================
// Account Suggestion Lookup
// ============================================

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

// ============================================
// Sync Timestamp UseCases
// ============================================

interface GetLastSyncTimestampUseCase {
    suspend operator fun invoke(profileId: Long): Result<Long?>
}

interface SetLastSyncTimestampUseCase {
    suspend operator fun invoke(profileId: Long, timestamp: Long): Result<Unit>
}

class GetLastSyncTimestampUseCaseImpl @Inject constructor(
    private val optionRepository: OptionRepository
) : GetLastSyncTimestampUseCase {
    override suspend fun invoke(profileId: Long): Result<Long?> = runCatching {
        optionRepository.getLastSyncTimestamp(profileId)
    }
}

class SetLastSyncTimestampUseCaseImpl @Inject constructor(
    private val optionRepository: OptionRepository
) : SetLastSyncTimestampUseCase {
    override suspend fun invoke(profileId: Long, timestamp: Long): Result<Unit> = runCatching {
        optionRepository.setLastSyncTimestamp(profileId, timestamp)
    }
}
