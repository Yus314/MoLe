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

import net.ktnx.mobileledger.domain.model.Profile
import net.ktnx.mobileledger.domain.model.Transaction
import net.ktnx.mobileledger.model.LedgerTransaction

/**
 * Interface for sending transactions to the ledger server.
 *
 * This abstraction enables unit testing of ViewModels without network dependencies.
 * It replaces direct instantiation of SendTransactionTask which extends Thread.
 */
interface TransactionSender {

    /**
     * Send a transaction to the ledger server using domain model.
     *
     * This is the preferred method for ViewModels to use, as it accepts the domain model
     * Transaction and handles the conversion internally.
     *
     * @param profile The profile containing server configuration (URL, API version, credentials)
     * @param transaction The fully validated domain model transaction to send
     * @param simulate If true, simulate the send without actually modifying server state.
     * @return [Result.success] with Unit if the transaction was accepted by the server,
     *         [Result.failure] with the exception if the send failed
     */
    suspend fun send(profile: Profile, transaction: Transaction, simulate: Boolean = false): Result<Unit>

    /**
     * Send a transaction to the ledger server using legacy LedgerTransaction.
     *
     * @deprecated Use the domain model Transaction overload instead.
     * This method is kept for backward compatibility during migration.
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
    @Deprecated("Use send(Profile, Transaction, Boolean) instead", ReplaceWith("send(profile, transaction, simulate)"))
    suspend fun sendLegacy(profile: Profile, transaction: LedgerTransaction, simulate: Boolean = false): Result<Unit>
}
