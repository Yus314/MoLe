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

import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.ensureActive
import logcat.logcat
import net.ktnx.mobileledger.core.domain.model.Account
import net.ktnx.mobileledger.core.domain.model.Profile
import net.ktnx.mobileledger.core.domain.model.Transaction
import net.ktnx.mobileledger.core.domain.repository.AccountRepository
import net.ktnx.mobileledger.core.domain.repository.OptionRepository
import net.ktnx.mobileledger.data.repository.mapper.AccountMapper.withStateFrom
import net.ktnx.mobileledger.domain.repository.TransactionRepository

/**
 * Implementation of SyncPersistence that saves data using repositories.
 */
@Singleton
class SyncPersistenceImpl @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val optionRepository: OptionRepository
) : SyncPersistence {

    override suspend fun saveAccountsAndTransactions(
        profile: Profile,
        accounts: List<Account>,
        transactions: List<Transaction>
    ) {
        val profileId = profile.id ?: throw IllegalStateException("Cannot sync unsaved profile")

        logcat { "Preparing account list" }
        val accountsWithState = accounts.map { account ->
            coroutineContext.ensureActive()
            // Preserve existing UI state if account exists
            val existing = accountRepository.getByNameWithAmounts(profileId, account.name).getOrNull()
            account.withStateFrom(existing)
        }
        logcat { "Account list prepared. Storing" }
        accountRepository.storeAccountsAsDomain(accountsWithState, profileId)
            .getOrThrow()
        logcat { "Account list stored" }

        logcat { "Storing transaction list" }
        transactionRepository.storeTransactionsAsDomain(transactions, profileId)
            .getOrThrow()
        logcat { "Transactions stored" }

        optionRepository.setLastSyncTimestamp(profileId, Date().time)
    }
}
