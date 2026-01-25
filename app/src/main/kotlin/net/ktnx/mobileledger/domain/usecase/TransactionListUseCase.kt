/*
 * Use cases for transaction list (list/observe transactions, account suggestions).
 */
package net.ktnx.mobileledger.domain.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import net.ktnx.mobileledger.domain.model.Transaction
import net.ktnx.mobileledger.domain.repository.AccountRepository
import net.ktnx.mobileledger.domain.repository.TransactionRepository

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
