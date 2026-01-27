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

import net.ktnx.mobileledger.core.domain.model.Profile
import net.ktnx.mobileledger.core.domain.model.Transaction

/**
 * Interface for sending transactions to the ledger server.
 *
 * This abstraction enables unit testing of ViewModels without network dependencies.
 * It replaces direct instantiation of SendTransactionTask which extends Thread.
 */
interface TransactionSender {

    /**
     * Send a transaction to the ledger server.
     *
     * @param profile The profile containing server configuration (URL, API version, credentials)
     * @param transaction The fully validated domain model transaction to send
     * @param simulate If true, simulate the send without actually modifying server state.
     * @return [Result.success] with Unit if the transaction was accepted by the server,
     *         [Result.failure] with the exception if the send failed
     */
    suspend fun send(profile: Profile, transaction: Transaction, simulate: Boolean = false): Result<Unit>
}
