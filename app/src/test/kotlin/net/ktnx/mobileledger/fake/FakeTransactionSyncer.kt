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

package net.ktnx.mobileledger.fake

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.ktnx.mobileledger.db.Profile
import net.ktnx.mobileledger.domain.model.SyncError
import net.ktnx.mobileledger.domain.model.SyncException
import net.ktnx.mobileledger.domain.model.SyncProgress
import net.ktnx.mobileledger.domain.model.SyncResult
import net.ktnx.mobileledger.domain.usecase.TransactionSyncer

class FakeTransactionSyncer : TransactionSyncer {
    var shouldSucceed: Boolean = true
    var progressSteps: Int = 5
    var delayPerStepMs: Long = 0
    var errorToThrow: SyncError? = null
    var result: SyncResult = SyncResult(transactionCount = 10, accountCount = 5, duration = 1000)
    var syncCallCount = 0
        private set
    var lastSyncedProfile: Profile? = null
        private set
    private var _lastResult: SyncResult? = null
    var wasCancelled: Boolean = false
        private set
    var cancellationTimeMs: Long = 0
        private set
    private var syncStartTime: Long = 0

    override fun sync(profile: Profile): Flow<SyncProgress> = flow {
        syncCallCount++
        lastSyncedProfile = profile
        syncStartTime = System.currentTimeMillis()

        try {
            emit(SyncProgress.Starting("接続中..."))
            if (delayPerStepMs > 0) delay(delayPerStepMs)

            // Check for cancellation before proceeding
            currentCoroutineContext().ensureActive()

            if (!shouldSucceed) throw SyncException(errorToThrow ?: SyncError.NetworkError())
            for (i in 1..progressSteps) {
                // Check for cancellation at each step - responds to cancellation quickly
                currentCoroutineContext().ensureActive()

                emit(SyncProgress.Running(current = i, total = progressSteps, message = "処理中..."))
                if (delayPerStepMs > 0) delay(delayPerStepMs)
            }
            _lastResult = result
        } catch (e: CancellationException) {
            wasCancelled = true
            cancellationTimeMs = System.currentTimeMillis() - syncStartTime
            throw e
        }
    }

    override fun getLastResult(): SyncResult? = _lastResult

    fun reset() {
        syncCallCount = 0
        lastSyncedProfile = null
        _lastResult = null
        shouldSucceed = true
        errorToThrow = null
        progressSteps = 5
        delayPerStepMs = 0
        wasCancelled = false
        cancellationTimeMs = 0
    }
}
