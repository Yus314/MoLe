/*
 * Use cases for Account Summary screen (accounts loading & zero-balance preference).
 */
package net.ktnx.mobileledger.domain.usecase

import javax.inject.Inject
import net.ktnx.mobileledger.domain.model.Account
import net.ktnx.mobileledger.domain.repository.AccountRepository
import net.ktnx.mobileledger.domain.repository.PreferencesRepository

interface GetAccountsWithAmountsUseCase {
    suspend operator fun invoke(profileId: Long, includeZeroBalances: Boolean): Result<List<Account>>
}

interface GetShowZeroBalanceUseCase {
    operator fun invoke(): Boolean
}

interface SetShowZeroBalanceUseCase {
    operator fun invoke(show: Boolean)
}

class GetAccountsWithAmountsUseCaseImpl @Inject constructor(
    private val accountRepository: AccountRepository
) : GetAccountsWithAmountsUseCase {
    override suspend fun invoke(profileId: Long, includeZeroBalances: Boolean): Result<List<Account>> =
        accountRepository.getAllWithAmounts(profileId, includeZeroBalances)
}

class GetShowZeroBalanceUseCaseImpl @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : GetShowZeroBalanceUseCase {
    override fun invoke(): Boolean = preferencesRepository.getShowZeroBalanceAccounts()
}

class SetShowZeroBalanceUseCaseImpl @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : SetShowZeroBalanceUseCase {
    override fun invoke(show: Boolean) {
        preferencesRepository.setShowZeroBalanceAccounts(show)
    }
}
