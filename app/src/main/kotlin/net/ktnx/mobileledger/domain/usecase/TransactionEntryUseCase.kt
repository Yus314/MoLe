/*
 * Use cases for transaction entry and submission flows.
 */
package net.ktnx.mobileledger.domain.usecase

import javax.inject.Inject
import net.ktnx.mobileledger.core.domain.model.Transaction
import net.ktnx.mobileledger.core.domain.repository.TransactionRepository

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
