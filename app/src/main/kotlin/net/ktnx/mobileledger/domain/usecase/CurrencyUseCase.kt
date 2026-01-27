/*
 * Use cases for currency data access.
 */
package net.ktnx.mobileledger.domain.usecase

import javax.inject.Inject
import net.ktnx.mobileledger.core.domain.model.Currency
import net.ktnx.mobileledger.core.domain.repository.CurrencyRepository

interface GetAllCurrenciesUseCase {
    suspend operator fun invoke(): Result<List<Currency>>
}

interface SaveCurrencyUseCase {
    suspend operator fun invoke(currency: Currency): Result<Long>
}

interface DeleteCurrencyUseCase {
    suspend operator fun invoke(name: String): Result<Boolean>
}

class GetAllCurrenciesUseCaseImpl @Inject constructor(
    private val currencyRepository: CurrencyRepository
) : GetAllCurrenciesUseCase {
    override suspend fun invoke(): Result<List<Currency>> = currencyRepository.getAllCurrenciesAsDomain()
}

class SaveCurrencyUseCaseImpl @Inject constructor(
    private val currencyRepository: CurrencyRepository
) : SaveCurrencyUseCase {
    override suspend fun invoke(currency: Currency): Result<Long> = currencyRepository.saveCurrency(currency)
}

class DeleteCurrencyUseCaseImpl @Inject constructor(
    private val currencyRepository: CurrencyRepository
) : DeleteCurrencyUseCase {
    override suspend fun invoke(name: String): Result<Boolean> = currencyRepository.deleteCurrencyByName(name)
}
