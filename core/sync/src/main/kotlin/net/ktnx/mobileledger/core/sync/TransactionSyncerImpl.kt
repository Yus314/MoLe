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

package net.ktnx.mobileledger.core.sync

import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import net.ktnx.mobileledger.core.common.di.IoDispatcher
import net.ktnx.mobileledger.core.domain.model.Account
import net.ktnx.mobileledger.core.domain.model.Profile
import net.ktnx.mobileledger.core.domain.model.SyncProgress
import net.ktnx.mobileledger.core.domain.model.SyncResult
import net.ktnx.mobileledger.core.domain.model.Transaction
import net.ktnx.mobileledger.core.network.json.ApiNotSupportedException

/**
 * TransactionSyncer implementation that orchestrates the sync process.
 *
 * Delegates the actual work to specialized components:
 * - AccountListFetcher: Fetches accounts via JSON API
 * - TransactionListFetcher: Fetches transactions via JSON API
 * - SyncPersistence: Saves data to database
 * - SyncExceptionMapper: Maps exceptions to user-friendly errors
 *
 * Note: Requires hledger-web v1.32 or later with JSON API support.
 */
@Singleton
class TransactionSyncerImpl @Inject constructor(
    private val accountListFetcher: AccountListFetcher,
    private val transactionListFetcher: TransactionListFetcher,
    private val syncPersistence: SyncPersistence,
    private val syncExceptionMapper: SyncExceptionMapper,
    private val syncStateNotifier: SyncStateNotifier,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : TransactionSyncer {

    private var _lastResult: SyncResult? = null

    override fun sync(profile: Profile): Flow<SyncProgress> = flow {
        val startTime = System.currentTimeMillis()

        try {
            emit(SyncProgress.Starting("接続中..."))

            // Fetch via JSON API (requires hledger-web v1.32+)
            val (accounts, transactions) = fetchViaJsonApi(profile) { progress ->
                emit(progress)
            } ?: throw ApiNotSupportedException(
                "JSON API not available. Please ensure hledger-web v1.32 or later is running."
            )

            // Save to database
            coroutineContext.ensureActive()
            emit(SyncProgress.Indeterminate("データを保存中..."))
            syncPersistence.saveAccountsAndTransactions(profile, accounts, transactions)

            // Update sync info
            syncStateNotifier.notifySyncComplete(
                date = Date(),
                transactionCount = transactions.size,
                accountCount = accounts.size
            )

            // Store result
            val duration = System.currentTimeMillis() - startTime
            _lastResult = SyncResult(
                transactionCount = transactions.size,
                accountCount = accounts.size,
                duration = duration
            )
        } catch (e: Exception) {
            throw syncExceptionMapper.mapToSyncException(e)
        }
    }.flowOn(ioDispatcher)

    override fun getLastResult(): SyncResult? = _lastResult

    /**
     * Attempts to fetch data via JSON API.
     *
     * @return Pair of (accounts, transactions) or null if JSON API is not available
     */
    private suspend fun fetchViaJsonApi(
        profile: Profile,
        onProgress: suspend (SyncProgress) -> Unit
    ): Pair<List<Account>, List<Transaction>>? {
        // Fetch accounts
        coroutineContext.ensureActive()
        onProgress(SyncProgress.Indeterminate("アカウントを取得中..."))
        val accountResult = accountListFetcher.fetch(profile) ?: return null

        // Fetch transactions
        coroutineContext.ensureActive()
        onProgress(SyncProgress.Indeterminate("取引を取得中..."))
        val transactions = transactionListFetcher.fetch(
            profile,
            accountResult.expectedPostingsCount
        ) { current, total ->
            onProgress(SyncProgress.Running(current, total, "取引を処理中..."))
        } ?: return null

        return Pair(accountResult.accounts, transactions)
    }
}
