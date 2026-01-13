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

import net.ktnx.mobileledger.db.Profile
import net.ktnx.mobileledger.model.LedgerTransaction

/**
 * Interface for sending transactions to the ledger server.
 *
 * This abstraction enables unit testing of ViewModels without network dependencies.
 * It replaces direct instantiation of [SendTransactionTask] which extends Thread.
 *
 * ## Usage in ViewModel
 *
 * ```kotlin
 * @HiltViewModel
 * class NewTransactionViewModel @Inject constructor(
 *     private val transactionSender: TransactionSender,
 *     // ...
 * ) : ViewModel() {
 *
 *     fun submitTransaction() {
 *         viewModelScope.launch {
 *             val result = transactionSender.send(profile, transaction)
 *             result.fold(
 *                 onSuccess = { handleSuccess() },
 *                 onFailure = { handleError(it) }
 *             )
 *         }
 *     }
 * }
 * ```
 *
 * ## Testing
 *
 * ```kotlin
 * class FakeTransactionSender : TransactionSender {
 *     var shouldSucceed = true
 *     val sentTransactions = mutableListOf<LedgerTransaction>()
 *
 *     override suspend fun send(
 *         profile: Profile,
 *         transaction: LedgerTransaction,
 *         simulate: Boolean
 *     ): Result<Unit> {
 *         sentTransactions.add(transaction)
 *         return if (shouldSucceed) Result.success(Unit)
 *                else Result.failure(Exception("Simulated failure"))
 *     }
 * }
 * ```
 */
interface TransactionSender {

    /**
     * Send a transaction to the ledger server.
     *
     * This is a suspend function that performs the network request on an appropriate
     * dispatcher. It should be called from a coroutine scope (e.g., viewModelScope).
     *
     * @param profile The profile containing server configuration (URL, API version, credentials)
     * @param transaction The fully validated transaction to send
     * @param simulate If true, simulate the send without actually modifying server state.
     *                 Useful for validation/preview purposes. Default is false.
     * @return [Result.success] with Unit if the transaction was accepted by the server,
     *         [Result.failure] with the exception if the send failed (network error,
     *         server rejection, API version mismatch, etc.)
     *
     * @throws kotlinx.coroutines.CancellationException if the coroutine is cancelled
     *         (not wrapped in Result, per standard coroutine conventions)
     */
    suspend fun send(profile: Profile, transaction: LedgerTransaction, simulate: Boolean = false): Result<Unit>
}
