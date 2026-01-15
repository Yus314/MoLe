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
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.ktnx.mobileledger.async.SendTransactionTask
import net.ktnx.mobileledger.async.TaskCallback
import net.ktnx.mobileledger.db.Profile
import net.ktnx.mobileledger.di.IoDispatcher
import net.ktnx.mobileledger.model.LedgerTransaction

/**
 * Implementation of [TransactionSender] that wraps the existing [SendTransactionTask].
 *
 * This implementation converts the callback-based [SendTransactionTask] into a
 * coroutine-based suspend function, enabling proper integration with ViewModels
 * and easier testing via IoDispatcher injection.
 */
@Singleton
class TransactionSenderImpl @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : TransactionSender {

    override suspend fun send(profile: Profile, transaction: LedgerTransaction, simulate: Boolean): Result<Unit> =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val callback = TaskCallback { error, _ ->
                    if (error == null) {
                        continuation.resume(Result.success(Unit))
                    } else {
                        continuation.resume(Result.failure(Exception(error)))
                    }
                }

                val task = SendTransactionTask(callback, profile, transaction, simulate)

                continuation.invokeOnCancellation {
                    task.interrupt()
                }

                task.start()
            }
        }
}
